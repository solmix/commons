package org.solmix.service.filetrack.support;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.commons.exec.Os;
import org.solmix.service.filetrack.ChangeEvent;
import org.solmix.service.filetrack.EventActionsEnum;
import org.solmix.service.filetrack.TrackedDirInfo;
import org.solmix.service.versioncontrol.support.Utils;

public class Watcher implements Runnable {
	public static final String FAMILY_UNIX = "unix";
	/** JPathWatch watcher service **/
	private final WatchService watcher;

	/** Global filter for watcher **/
	private String filter;

	private static Logger log = LoggerFactory.getLogger(Watcher.class);

	/** Queue for sending recieved file system events **/
	private final BlockingQueue<ChangeEvent> senderQueue;

	/**
	 * Map which holds a list of paths being watched Used to sync current watch list
	 * with update requests
	 **/
	private final Map<String, WatchKey> pathWatchKeyMap = new HashMap<String, WatchKey>();

	/**
	 * When an event occurs it gives the Watch key information. This map is used to
	 * identify the file full path and attributes.
	 **/
	private final Map<WatchKey, TrackedDirInfo> watchKeyInfoMap = new HashMap<WatchKey, TrackedDirInfo>();

	/**
	 * Last event is kept to
	 */
	ChangeEvent prevEvent;

	private boolean _stop = false;

	/**
	 * Constructor
	 * 
	 * @param senderQueue
	 * @throws IOException
	 */
	public Watcher(BlockingQueue<ChangeEvent> senderQueue) throws IOException {
		this.senderQueue = senderQueue;
		watcher = FileSystems.getDefault().newWatchService();
		prevEvent = null;
	}

	/**
	 * Map of directories to watch. The watcher will sync current watched
	 * directories with this list.
	 * 
	 * @param newDirsMap
	 */
	public void setWatchedDirs(Map<String, TrackedDirInfo> newDirsMap) {
		final Collection<String> remove = new HashSet<String>(this.pathWatchKeyMap.keySet());
		addUpdateWatchedDirs(newDirsMap, remove);
		removeWatchedDirs(remove);

	}

	public void removeWatchedDirs(final Collection<String> remove) {
		// remove old dirs
		Set<String> keySet = new HashSet<String>(pathWatchKeyMap.keySet());
		for (String path : remove) {
			removeWatchedFolder(path);
			if (Os.isFamily(FAMILY_UNIX)) {
				for (String s : keySet) {
					if (s.startsWith(path))
						removeWatchedFolder(path);
				}
			}
		}
	}

	private void removeWatchedFolder(String path) {
		final WatchKey key = pathWatchKeyMap.get(path);
		if (key == null)
			return;
		try {
			key.cancel();
		} catch (NullPointerException e) {
			log.debug("Folder removed before watch removed");
		}
		pathWatchKeyMap.remove(path);
		watchKeyInfoMap.remove(key);
		if (log.isInfoEnabled())
			log.info("Removing folder from watch: " + path);
	}

	public void addUpdateWatchedDirs(Map<String, TrackedDirInfo> newDirsMap) {
		this.addUpdateWatchedDirs(newDirsMap, null);
	}

	protected void addUpdateWatchedDirs(Map<String, TrackedDirInfo> dirsMap, final Collection<String> remove) {
		final Map<String, TrackedDirInfo> newDirsMap = addLinuxRecursiveDirs(dirsMap);

		final Collection<String> exists = new ArrayList<String>();
		// update existing dirs
		for (String path : newDirsMap.keySet()) {
			if (pathWatchKeyMap.containsKey(path)) {
				final WatchKey watchKey = pathWatchKeyMap.get(path);
				final TrackedDirInfo info = watchKey == null ? null : watchKeyInfoMap.get(watchKey);
				if (info != null) {
					final String filter = newDirsMap.get(path) == null ? null : newDirsMap.get(path).getFilter();
					final boolean recursive = newDirsMap.get(path) == null ? false : newDirsMap.get(path).isRecursive();
					info.setFilter(filter);
					info.setRecursive(recursive);
					log.info("Updating folder to watch: " + path);
				}
				exists.add(path);
			}
		}

		for (String path : exists) {
			if (remove != null)
				remove.remove(path);
			newDirsMap.remove(path);
		}

		// add new dirs
		for (String path : newDirsMap.keySet()) {
			final TrackedDirInfo info = newDirsMap.get(path);
			addWatchedFolder(path, info);
		}
	}

	protected Map<String, TrackedDirInfo> addLinuxRecursiveDirs(Map<String, TrackedDirInfo> newDirsMap) {
		if (!Os.isFamily(FAMILY_UNIX) || newDirsMap == null || newDirsMap.size() <= 0)
			return newDirsMap;

		Map<String, TrackedDirInfo> map = new HashMap<String, TrackedDirInfo>(newDirsMap);

		for (Map.Entry<String, TrackedDirInfo> entry : newDirsMap.entrySet()) {
			TrackedDirInfo folder = entry.getValue();
			if (folder.isRecursive()) {
				DirsMapAdder callable = new DirsMapAdder(map, folder.getFilter());
				final File dir = new File(entry.getKey());
				if (log.isDebugEnabled())
					log.debug("Directory is to be monitored recursively on unix " + dir.getPath());
				FileWalker.walkDfs(dir, callable);
			}
		}
		return map;
	}

	public void addWatchedFolder(String path, final TrackedDirInfo info) {
		final Path p = createPathObj(path);
		final boolean recursive = info == null ? null : info.isRecursive();
		final WatchKey wk = initPathWatch(p, recursive);
		pathWatchKeyMap.put(path, wk);
		info.setPath(p);
		watchKeyInfoMap.put(wk, info);
		log.info("Adding folder to watch: " + path);
	}

	private Path createPathObj(String path) {
		if (path == null || path.length() <= 0)
			return null;
		return Paths.get(path);
	}

	/**
	 * 
	 * @return
	 */
	public String getFilter() {
		return filter;
	}

	private WatchKey initPathWatch(final Path path, boolean recursive) {
		WatchKey key = null;
		// Prevent UnsupportedOperationException from being raised
		if (Os.isFamily(FAMILY_UNIX))
			recursive = false;
		try {
//			WatchEvent.Modifier exModifier = recursive ? com.sun.nio.file.ExtendedWatchEventModifier.FILE_TREE
//					: com.sun.nio.file.ExtendedWatchEventModifier.ACCURATE;
			key = path.register(
							watcher, new Kind<?>[] { StandardWatchEventKinds.ENTRY_CREATE,
									StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY });

		} catch (UnsupportedOperationException uox) {
			System.err.println("file watching not supported!");
			// handle this error here
		} catch (IOException iox) {
			System.err.println("I/O errors");
			// handle this error here
		}
		return key;
	}

	private String oldPath = null;

	public static EventActionsEnum convert(Kind<?> kind) {
		if (kind == null)
			return null;
		if (kind == StandardWatchEventKinds.ENTRY_CREATE)
			return EventActionsEnum.CREATE;
		if (kind == StandardWatchEventKinds.ENTRY_DELETE)
			return EventActionsEnum.DELETE;
		if (kind == StandardWatchEventKinds.ENTRY_MODIFY)
			return EventActionsEnum.MODIFY;
		return null;
	}

	public void run() {
		HandleRenameFromNonMonitoredFolder handleUnmonitoredRename = new HandleRenameFromNonMonitoredFolder();

		// wait for events in infinite loop
		while (!_stop) {
			// take() will block until a file has been created/deleted
			WatchKey signalledKey;
			try {
				signalledKey = watcher.take();
			} catch (InterruptedException ix) {
				// we'll ignore being interrupted
				continue;
			} catch (ClosedWatchServiceException cwse) {
				// other thread closed watch service
				log.info("watch service closed, terminating.");
				// System.out.println("watch service closed, terminating.");
				break;
			}
			// get list of events from key
			List<WatchEvent<?>> list = signalledKey.pollEvents();

			// VERY IMPORTANT! call reset() AFTER pollEvents() to allow the
			// key to be reported again by the watch service
			signalledKey.reset();

			final TrackedDirInfo info = watchKeyInfoMap.get(signalledKey);
			final Path p = info == null ? null : info.getPath();
			final String path = p == null ? null : p.toString();
			final String filter = info == null ? null : info.getFilter();

			// print what has happened
			for (WatchEvent<?> e : list) {
				final Kind<?> kind = e.kind();
				final EventActionsEnum event = convert(kind);
				final Path context = (Path) e.context();
				final String fileName = context == null ? null : context.toString();
				final String fullPath = path + File.separator + fileName;
				if (log.isDebugEnabled())
					log.debug("Recieved event: " + event + " " + fullPath);

				if (event == null) {
					//其他的不处理
					continue;
				}

				// this is only for directories (checked in doLinuxDirAdd)
				if ((EventActionsEnum.RENAME.equals(event) || EventActionsEnum.CREATE.equals(event)) && info.isRecursive()) {
					if (doLinuxDirAdd(filter, fullPath)) {
						// rename of folder is treated as add delete, even though it is identified as
						// rename.
						// TODO: treat as rename, not delete/add.
						if (EventActionsEnum.RENAME.equals(event) && oldPath != null) {
							handleUnmonitoredRename.setPath(null);
							removeWatchedFolder(oldPath);
							onEvent(EventActionsEnum.DELETE, filter, oldPath);
						}
						oldPath = null;
						continue;
					}
				}

				if (EventActionsEnum.DELETE.equals(event))
					removeWatchedFolder(fullPath);

				if (EventActionsEnum.RENAME.equals(event) && oldPath != null) {
					onEvent(event, filter, fullPath, oldPath);
					handleUnmonitoredRename.setPath(null);
				} else
					onEvent(event, filter, fullPath);
					oldPath = null;
			}
			if (path != null) {
				handleUnmonitoredRename.setPath(oldPath);
				Thread t = new Thread(handleUnmonitoredRename);
				t.start();
			}
		}
	}

	private class HandleRenameFromNonMonitoredFolder implements Runnable {
		private String path;

		public HandleRenameFromNonMonitoredFolder() {
			super();
			path = null;
		}

		public void setPath(String path) {
			this.path = path;
			log.debug("Potential rename from monitored to non-monitored. " + path);
		}

		public void run() {
			if (path == null)
				return;

			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				return;
			}
			if (path != null) {
				onEvent(EventActionsEnum.DELETE, filter, path);
				log.debug("Detected rename from monitored to non-monitored. " + path);
				oldPath = null;
			}

		}
	}

	private boolean doLinuxDirAdd(String filter, String path) {
		if (!Os.isFamily(FAMILY_UNIX))
			return false;

		final File f = new File(path);

		if (!f.exists() || !f.isDirectory())
			return false;

		if (pathWatchKeyMap.containsKey(path))
			return false;

		addFolderRecursive(path, filter);
		return true;
	}

	/**
	 * Start recursively monitoring a directory. Includes adding the tree to
	 * watcher, simulating CREATE events for all files in tree and consequently
	 * adding their data to version control and reporting their creation.
	 * 
	 * @param path
	 * @param filter
	 * @return
	 */
	public void addFolderRecursive(String path, String filter) {
		// Adding folders
		log.info("addFolderRecursive(" + path + ", " + filter + ")");
		addWatchedFolder(path, new TrackedDirInfo(null, true, filter));
		WatchedFolderAdder callable = new WatchedFolderAdder(this, filter);
		FileWalker.walkDfs(new File(path), callable);
		// Simulating the events on files
		BaselineAdder fileAdder = new BaselineAdder(this, true, filter, EventActionsEnum.CREATE);
		simulateEvent(EventActionsEnum.CREATE, filter, path);
		FileWalker.walkDfs(new File(path), fileAdder);
	}

	/**
	 * Stop watcher
	 * 
	 * @return true if successful
	 */
	public Boolean stopWatching() {
		_stop = true;
		if (watcher != null) {
			// stop watching
			try {
				watcher.close();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			// release resources
			return true;
		} else
			return false;
	}

	public boolean passedFilter(String filePath, EventActionsEnum eventAction, String filter) {
		// Check whether this is a temp file. If so, ignore the event
		final File f = new File(filePath);

		// if it's a directory (or suspected directory in the case of delete) allow all
		// rename/delete events
		if (f.exists() && f.isDirectory() /*
											 * && (EventActionsEnum.RENAME.equals(eventAction) ||
											 * EventActionsEnum.CREATE.equals(eventAction))
											 */
				|| EventActionsEnum.DELETE.equals(eventAction) && Utils.getFileExtension(f.getName()) == null)
			return true;

		// check for existence/directory in non-delete cases
		if (!EventActionsEnum.DELETE.equals(eventAction) && (!f.exists() || f.isDirectory())) {
			if (log.isDebugEnabled())
				log.debug("Filtered - " + filePath + " This is a directory or non-existent file. Skipping this event.");
			return false;
		}
		// check against exclude filter
		final String fileName = f.getName();

		if (filter == null || fileName.matches(filter))
			return true;

		if (log.isDebugEnabled())
			log.debug("Filtered - " + filePath + " - File does not match filter. Skipping this event.");

		return false;
	}

	private void onEvent(EventActionsEnum eventAction, final String filter, final String filePath) {
		onEvent(eventAction, filter, filePath, null);
	}

	private void onEvent(EventActionsEnum eventAction, final String filter, final String filePath,
			final String oldFilePath) {

		String path = filePath;
		String oldPath = oldFilePath;
		//已处理完成的
		if (EventActionsEnum.REGISTER_COMPLETE.equals(eventAction)) {
			path = null;
		} else if (!passedFilter(path, eventAction, filter)) {
			//处理没通过filter的
			if (oldPath == null) {
				if(log.isDebugEnabled())
					log.debug("path and target didn't pass filter {}" , path);
				eventAction = EventActionsEnum.SKIPPED;
			} else if (!passedFilter(oldPath, EventActionsEnum.DELETE, filter)) {
				if(log.isDebugEnabled())
					log.debug("source and target didn't pass filter " + path + " " + oldPath);
				eventAction = EventActionsEnum.SKIPPED;
			} else {
				// rename from passed filter to filtered - change event to delete.
				path = oldPath;
				eventAction = EventActionsEnum.DELETE;
				log.info("Event changed from RENAME to " + eventAction);
			}
		} else if (oldPath != null && !passedFilter(oldPath, EventActionsEnum.DELETE, filter)) {
			// rename from filtered to passed filter - change event to create.
			eventAction = EventActionsEnum.CREATE;
			log.info("Event changed from RENAME to " + eventAction);
		}
		final ChangeEvent currEvent;

		try {
			final Long timestamp = Calendar.getInstance().getTimeInMillis();
			if (EventActionsEnum.RENAME.equals(eventAction))
				currEvent = new ChangeEvent(eventAction, path, oldPath, timestamp);
			else
				currEvent = new ChangeEvent(eventAction, path, timestamp);
			senderQueue.put(currEvent);
			// Current event is now reference for further events
			prevEvent = currEvent;
		} catch (InterruptedException e) {
			log.error("Queue action interrupted " + e.getMessage(), e);
		}
	}

	/**
	 * Simulate a file system event. May be used to create baseline. Assumes no
	 * RENAME actions
	 * 
	 * @param eventAction
	 * @param filter
	 * @param filePath
	 */
	public void simulateEvent(EventActionsEnum eventAction, final String filter, final String filePath) {
		if (log.isDebugEnabled())
			log.debug("Simulating " + eventAction.name() + " event for " + filePath + " with filter " + filter);
		onEvent(eventAction, filter, filePath);
	}

}
