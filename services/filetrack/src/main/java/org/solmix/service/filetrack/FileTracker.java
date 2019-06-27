package org.solmix.service.filetrack;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.service.filetrack.support.BaselineAdder;
import org.solmix.service.filetrack.support.ChangeEventProcessor;
import org.solmix.service.filetrack.support.FileWalker;
import org.solmix.service.filetrack.support.Watcher;
import org.solmix.service.versioncontrol.support.Utils;

public class FileTracker implements IFileTracker,ChangeListener {
	private static Logger LOG = LoggerFactory.getLogger(FileTracker.class);
	private static FileTracker instance = new FileTracker();
	private TrackerStatus status = TrackerStatus.STOPPED;

	private ChangeEventProcessor changeEventProcessor;
	private Watcher watcher;

	private Collection<ChangeListener> listeners;
	private Map<String, ChangeListener> folderListenerMap = new HashMap<String, ChangeListener>();

	private String appDataDir;
	private Long maxDiffSize=5l;
	public static FileTracker getInstance() {
		return instance;
	}

	@Override
	public void setAppDataDir(String path) {
		this.appDataDir=path;
		if(this.changeEventProcessor!=null) {
			this.changeEventProcessor.setVersionContorlDir(path);
		}
	}

	private String replaceSysVariable(String path) {
		int pos = -1;
		if (path.startsWith("%") && (pos = path.indexOf('%', 1)) > 0) {
			final String sysPropName = path.substring(0, pos);
			final String pathSuffix = path.substring(pos, path.length());
			final String sysProp = System.getProperty(sysPropName);
			return sysProp + pathSuffix;
		}
		return path;
	}

	private String getFullPath(final String installDir, final String relPath) {
		if (relPath == null || relPath.length() <= 0)
			return installDir;
		File f = new File(relPath);
		final boolean isAbsolute = f.isAbsolute() || relPath.charAt(1) == ':' || relPath.charAt(0) == '/'
				|| relPath.startsWith("~/");
		final String basePath = isAbsolute ? "" : installDir + File.separator;
		return basePath + relPath;
	}

	private Map<String, TrackedDirInfo> getMonitoredFolderMap(String installPath,
			Collection<TrackerInfo> configDefDtos) {

		Map<String, TrackedDirInfo> dirsMap = new HashMap<String, TrackedDirInfo>();

		for (TrackerInfo configDef : configDefDtos) {
			String dirName = configDef.getPath();
			dirName = replaceSysVariable(dirName);
			//完整路径，处理相对路径和绝对路径
			if (installPath != null)
				dirName = getFullPath(installPath, dirName);
			if (LOG.isDebugEnabled())
				LOG.debug("Adding monitored directory: " + dirName);
			final File dir = new File(dirName);
			if (!dir.isDirectory()) {
				if (LOG.isDebugEnabled())
					LOG.debug(dirName + " is not a directory. Skipping...");
			}
			dirsMap.put(dirName, new TrackedDirInfo(null, configDef.isRecursive(), configDef.getFilter()));
		}
		return dirsMap;
	}

	@Override
	public Collection<String> addTrackedDirs(String basePath, Collection<TrackerInfo> trackerInfos,
			ChangeListener listener) {

		final Map<String, TrackedDirInfo> dirsMap = getMonitoredFolderMap(basePath, trackerInfos);
		//新添加监控时，创建第一版基线
		generateBaselineEvents(dirsMap);
		if (listener != null) {
			for (final String dir : dirsMap.keySet())
				folderListenerMap.put(dir, listener);
		}
		//添加监控目录
		watcher.addUpdateWatchedDirs(dirsMap);
		return dirsMap.keySet();
	}

	private void generateBaselineEvents(Map<String, TrackedDirInfo> dirsMap) {
		// Building version control baseline for watched files
		for (String path : dirsMap.keySet()) {
			LOG.debug("generateBaselineEvents for {}", path);
			TrackedDirInfo info = dirsMap.get(path);
			BaselineAdder callable = new BaselineAdder(watcher, info.isRecursive(), info.getFilter());
			FileWalker.walkDfs(new File(path), callable);
		}
		watcher.simulateEvent(EventActionsEnum.REGISTER_COMPLETE, null, null);
	}

	@Override
	public void removeTrackedDirs(Collection<String> monitoredPaths) {

		for (final String dir : monitoredPaths)
			folderListenerMap.remove(dir);
		watcher.removeWatchedDirs(monitoredPaths);
	}

	@Override
	public void start() {
		synchronized (status) {
			LOG.info("Entered FileMonitor.start()");
			if (status == TrackerStatus.STOPPED) {
				changeEventProcessor = new ChangeEventProcessor(this);
				changeEventProcessor.setVersionContorlDir(this.appDataDir);
				changeEventProcessor.setMaxDiffSize(this.maxDiffSize);
				Thread rocessorThread = new Thread(changeEventProcessor, "File-Change-Listener");
				rocessorThread.start();
				try {
					watcher = new Watcher(changeEventProcessor.getMessageQueue());
				} catch (IOException e) {
					LOG.error("Error ceate Watcher",e);
				}
				Thread t = new Thread(watcher, "File-Watcher");
				t.start();
				status = TrackerStatus.STARTED;
			}
		}

	}

	@Override
	public void stop() {
		synchronized (status) {
			LOG.info("Entered FileMonitor.stop()");
			if (status != TrackerStatus.STOPPED) {
				watcher.stopWatching();
				changeEventProcessor.setStop(true);
				status = TrackerStatus.STOPPED;
			}
		}

	}

	@Override
	public void addListener(ChangeListener listener) {
		if (listeners == null)
			listeners = new LinkedList<ChangeListener>();
		listeners.add(listener);

	}

	@Override
	public void removeListener(ChangeListener listener) {
		if (listeners != null)
			listeners.remove(listener);

	}

	@Override
	public TrackerStatus getStatus() {
		return status;
	}

	@Override
	public void setMaxDiffSize(Long sizeInKb) {
		this.maxDiffSize=sizeInKb;
		if(this.changeEventProcessor!=null) {
			this.changeEventProcessor.setMaxDiffSize(this.maxDiffSize);
		}

	}
	
	@Override
	public void onChange(ChangeEvent item) {
		/* ignore registration events */
		if (EventActionsEnum.REGISTER.equals(item.getType()))
			return;

		if (LOG.isDebugEnabled())
			LOG.debug("---FILE-TRACKER--- onChange event start");
		/* mapped listeners listen on specific folders */
		if (folderListenerMap.size() > 0) {
			String evPath = EventActionsEnum.RENAME == item.getType() ? item.getOldFullPath() : item.getFullPath();
			ChangeListener listener = null;

			String path = evPath;
			File f = path == null ? null : new File(path);
			if (f == null) {
				LOG.error("FileMonitor onChange(EventMessage item) - Path is null for event: " + item.getType()
						+ ", old path: " + item.getOldFullPath() + ", new path: " + item.getFullPath());
				return;
			}
			boolean suspectedDir = Utils.getFileExtension(f.getName()) == null;
			// if the event is on a folder, check the path first
			if (!suspectedDir)
				path = f == null ? null : f.getParent();
			do {
				if (path == null)
					break;
				listener = path == null ? null : folderListenerMap.get(path);
				if (listener == null) {
					path = f.getParent();
					f = path == null ? null : new File(path);
				}
			} while (listener == null);
			if (listener != null)
				listener.onChange(item);
			else {
				// check if the event is in a directory under any being listened to. Case of
				// directory delete.
				if (suspectedDir && EventActionsEnum.DELETE.equals(item.getType())) {
					Set<ChangeListener> lSet = new HashSet<ChangeListener>();
					for (Map.Entry<String, ChangeListener> entry : folderListenerMap.entrySet()) {
						if (entry.getKey().startsWith(evPath))
							lSet.add(entry.getValue());
					}
					if (lSet.size() > 0) {
						for (ChangeListener l : lSet)
							l.onChange(item);
					} else
						LOG.warn("No listener mapped for file event: " + evPath);
				} else
					LOG.warn("No listener mapped for file event: " + evPath);
			}
		}

		if (LOG.isDebugEnabled())
			LOG.debug("---FILE-TRACKER--- onChange before fire");

		/* general liteners listen for all events */
		if (listeners != null) {
			for (ChangeListener listener : listeners)
				listener.onChange(item);
		}
		if (LOG.isDebugEnabled())
			LOG.debug("---FILE-TRACKER--- onChange event fired");
	}

}
