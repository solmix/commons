package org.solmix.service.filetrack.support;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.service.filetrack.ChangeListener;
import org.solmix.service.filetrack.event.ChangeEvent;
import org.solmix.service.filetrack.event.EventActionsEnum;
import org.solmix.service.versioncontrol.ChangedType;
import org.solmix.service.versioncontrol.RevisionDifferItem;
import org.solmix.service.versioncontrol.VersionController;
import org.solmix.service.versioncontrol.support.GitVersionController;

public class ChangeEventProcessor implements Runnable {

    private static Logger LOG = LoggerFactory.getLogger(ChangeEventProcessor.class);
    private String versionContorlDir;
    private long maxDiffSize;
    private final BlockingQueue<ChangeEvent> messageQueue;
    private VersionController versionController;
    private ChangeListener changeListener;
    private boolean stop = false;
    public ChangeEventProcessor() {
    	versionController  = new GitVersionController();
    	messageQueue = new ArrayBlockingQueue<ChangeEvent>(1000, true);;
    }
    
   
    
    public BlockingQueue<ChangeEvent> getMessageQueue() {
		return messageQueue;
	}



	private void updateDeleteList(List<ChangeEvent> deleteList,
			final ChangeEvent event) {
		boolean improves = false;
		boolean represented = false;
		
		ChangeEvent updatedEvent = new ChangeEvent(event);
		
		File f = new File (event.getFullPath());
		File parentF = null;
		do {
			String parent = f.getParent();
			if (parent == null || parent.length() <= 0)
				break;
			parentF = new File(parent);
			if (!parentF.exists())
				f = parentF;
			else
				break;
		} while (true);
		updatedEvent.setFullPath(f.getPath());
		
		Collection<ChangeEvent> tmpDelList = new ArrayList<ChangeEvent> (deleteList);
		for (ChangeEvent root: tmpDelList) {
			if (root.getFullPath().startsWith(updatedEvent.getFullPath())) {
				improves = true;
				deleteList.remove(root);
			}
			if (updatedEvent.getFullPath().startsWith(root.getFullPath())) {
				represented = true;
				break;
			}
		}
		if (!represented || improves) 
			deleteList.add(updatedEvent);
		if (LOG.isDebugEnabled()){
			String s = "\nPath recieved: "+event.getFullPath();
			for (ChangeEvent item: deleteList)
				s += "\n" + item.getFullPath();
			LOG.debug("rootSet items:" + s);
		}
	}
    
	private String getCommonPath(String ePath, String fullPath) {
		if (ePath == null || fullPath == null || ePath.length() <=0 || fullPath.length() <= 0)
			return null;
		int min = Math.min(ePath.length(), fullPath.length());
		int index = 0;
		for (int i=0; i<min; i++){
			if (ePath.charAt(i) != fullPath.charAt(i))
				break;
			index++;
		}
		if (index < 2)
			return null;
	
		String common = ePath.substring(0, index);
		if (common.endsWith(File.separator)){
			common = common.substring(0, common.length()-1);
		} else {
			File f = new File(common);
			common = f.getParent();
		}

		return common;
	}

    
	@Override
	public void run() {
		final boolean success = versionController.createRepository(versionContorlDir);
	if (!success) {
		LOG.error("Failure creating version control repository for git Dir: " + versionContorlDir);
		return;
	}
	
	EventActionsEnum action = null;
	String path = null;
	List<ChangeEvent> deleteList = new LinkedList<ChangeEvent>();
	ChangeEvent lastCreateDirEvent = null;
	LOG.info("ChangeEventProcessor started");
	Collection<String> registerFiles = new LinkedList<String>();
	Collection<String> batchCreateFiles = new LinkedList<String>();
	long lastDummyTime = 0L;
	
	while (!stop){
		try {
			final ChangeEvent event = messageQueue.take();
			action = event.getType();
			if (LOG.isDebugEnabled())
				LOG.debug("Event taken "+action.protocolValue()+ " " + event.getFullPath());
			
			if (EventActionsEnum.SKIPPED.equals(action))
				continue;
				
			if (EventActionsEnum.REGISTER_COMPLETE.equals(action)){
				LOG.info("Register complete!");
				continue;
			}
			
			File f = event.getFullPath() == null ? null : new File(event.getFullPath());
			String ePath = event.getFullPath();
			
			ChangeEvent nextEvent = messageQueue.peek();
			if (nextEvent == null) {
				if (action == EventActionsEnum.DELETE) Thread.sleep(500);
				else if (action == EventActionsEnum.MODIFY) Thread.sleep(200);
				else if (action == EventActionsEnum.CREATE || action == EventActionsEnum.REGISTER) Thread.sleep(100); 
				nextEvent = messageQueue.peek();
			}
			
			
			if (nextEvent != null && EventActionsEnum.SKIPPED.equals(nextEvent.getType())){
				lastDummyTime = nextEvent.getTimestamp();
				nextEvent.setFullPath(event.getFullPath());
				nextEvent.setTimestamp(event.getTimestamp());
				nextEvent.setType(event.getType());
				nextEvent.setOldFullPath(event.getOldFullPath());
				continue;
			}

			if (LOG.isDebugEnabled())
				LOG.debug("Next evet: "+(nextEvent == null ? null : nextEvent.getType()));

			if (EventActionsEnum.REGISTER.equals(action)){
				boolean commit = nextEvent == null 
					|| !EventActionsEnum.REGISTER.equals(nextEvent.getType())
					/*|| ArchiveUtils.isZipArchive(nextEvent.getFullPath())
					|| ArchiveUtils.isZipArchive(ePath)*/;
				registerFiles.add(event.getFullPath());
				if (commit && registerFiles.size() > 0){
//					if (msg == null)
//						msg = "dummy message";
					String msg = addFileBatch(registerFiles, EventActionsEnum.REGISTER);
					if (msg != null){
						String revId = versionController.commit(getVersionControlDir(), msg);
						LOG.info("Commited register, id: "+revId);
					}
					//processCommit(registerFiles, msg);
					registerFiles.clear();
				}
				lastDummyTime = 0L;
				continue;
			}
			
			if (EventActionsEnum.RENAME.equals(action) && event.getOldFullPath() == null){
				action = EventActionsEnum.CREATE;
				event.setType(EventActionsEnum.CREATE);
			}
			
			if (nextEvent == null
				|| ((nextEvent.getTimestamp() - event.getTimestamp() > 500) && nextEvent.getTimestamp() - lastDummyTime > 500)) {
				// Linux solution - set of delete events
				lastDummyTime = 0L;
				if (EventActionsEnum.DELETE.equals(action))
					updateDeleteList(deleteList, event);
				fireFolderDelete(deleteList);
				
				// fire create event for directory if any has bee saved:
				if (lastCreateDirEvent != null){
					if (action.equals(EventActionsEnum.CREATE) 
							&& contained(ePath, lastCreateDirEvent.getFullPath())){
						addToDirCopy(event, batchCreateFiles);
					}
					if (batchCreateFiles.size() > 0)
						fireNClearCreateDirEvent(lastCreateDirEvent, batchCreateFiles);
					lastCreateDirEvent = null;
					continue;
				} 
				// if it's a directory, we don't want to proecess directory events independantly
				if (f.isDirectory()){
					if (LOG.isDebugEnabled())
						LOG.debug("Directory event discarded");
					continue;
				}
				
				// if it's delete, current event was already added to the rootSet, and then processed.
				if (!action.equals(EventActionsEnum.DELETE) )
					processEvent(event);

				continue;
			}
			if (EventActionsEnum.MODIFY.equals(action)
				&& event.equals(nextEvent)) {
				LOG.debug("Duplicate MODIFY events detected, skipping");
				continue;
			}
			if (EventActionsEnum.DELETE.equals(action)) {
				if (EventActionsEnum.CREATE.equals(nextEvent.getType())
					&& event.getFullPath().equals(nextEvent.getFullPath())) {
					// Win XP and Linux generate a DELETE-CREATE sequence for the MOVE operation. 
					// If the next event is CREATE and it's on the same file, replace both with a 
					// MODIFY event
					nextEvent.setType(EventActionsEnum.MODIFY);
					if (LOG.isDebugEnabled())
						LOG.debug("overwrite sequence detected (DELETE+CREATE), replacing with MODIFY");
					continue; // drop delete-create and leave a modify (for overwrite sequences)
				}
				if (EventActionsEnum.DELETE.equals(nextEvent.getType())) {
					updateDeleteList(deleteList, event);
					continue;
				}
				updateDeleteList(deleteList, event);
			}

			if (EventActionsEnum.CREATE.equals(action) || EventActionsEnum.MODIFY.equals(action)) {
				if (EventActionsEnum.MODIFY.equals(nextEvent.getType())
					&& event.getFullPath().equals(nextEvent.getFullPath())) {
					if (LOG.isDebugEnabled())
						LOG.debug("Dropping modify from create/modify sequence.");
					nextEvent.setType(EventActionsEnum.CREATE);
					continue; // make create/modify into single create
				}
				if (EventActionsEnum.CREATE.equals(nextEvent.getType()) || EventActionsEnum.MODIFY.equals(nextEvent.getType())) {
					if (f.isDirectory()){
						if (lastCreateDirEvent != null){
							if (contained(ePath, lastCreateDirEvent.getFullPath())){
								fireFolderDelete(deleteList);
								continue;
							} else {
								if (batchCreateFiles.size() > 0)
									fireNClearCreateDirEvent(lastCreateDirEvent, batchCreateFiles);
							}
						}							
						lastCreateDirEvent = event;
						if (LOG.isDebugEnabled())
							LOG.debug("Directory saved for recursive copy: "+event.getFullPath());
						fireFolderDelete(deleteList);
						continue;
					} else {
						if (lastCreateDirEvent != null && contained(ePath, lastCreateDirEvent.getFullPath())){
							addToDirCopy(event, batchCreateFiles);
							continue;
						} else{
							if (EventActionsEnum.MODIFY.equals(action)){
								String commonPath = getCommonPath(ePath, nextEvent.getFullPath());
								if (commonPath != null){
									lastCreateDirEvent = new ChangeEvent(EventActionsEnum.MODIFY, commonPath, lastCreateDirEvent == null ? event.getTimestamp() : lastCreateDirEvent.getTimestamp());
									addToDirCopy(event, batchCreateFiles);
									fireFolderDelete(deleteList);
									if (LOG.isDebugEnabled())
										LOG.debug("Generating common modify directory: "+commonPath);
									continue;
								}
									
							}
							
							if (batchCreateFiles.size() > 0)
								fireNClearCreateDirEvent(lastCreateDirEvent, batchCreateFiles);
							lastCreateDirEvent = null;
						}
					}
				} else  if (lastCreateDirEvent != null){
					// fire create event for directory if any has bee saved:
					if (batchCreateFiles.size() > 0)
						fireNClearCreateDirEvent(lastCreateDirEvent, batchCreateFiles);
					lastCreateDirEvent = null;
				}
			}

			fireFolderDelete(deleteList);
			
			// if it's delete, current event was already added to the rootSet, and then processed.
			if (!action.equals(EventActionsEnum.DELETE))
				processEvent(event);

		} catch (InterruptedException ex) {
			if (!stop)
				LOG.error(ex.getMessage(), ex);
			break;
		} catch (Exception e){
			LOG.error(e.getMessage(), e);

		}
	}
	LOG.info("ChangeEventProcessor stopped. Last Event: "+action+" "+path+ ". Stop flag:"+stop);
}
	private String addFileBatch(Collection<String> paths, EventActionsEnum action){
		LOG.info("addFiles: - multiple file " + action.protocolValue());
		final String message = String.valueOf(System.currentTimeMillis());
		final String gitDir = getVersionControlDir();
		String res = versionController.addChange(gitDir, paths, message, false);
		
		return res;
	}
	
	private void fireFolderDelete(List<ChangeEvent> deleteList) {
		for (ChangeEvent root: deleteList) {
			if (LOG.isDebugEnabled())
				LOG.debug("fireFolderDelete: " + root.getFullPath());
			processEvent(root);
		}
		deleteList.clear();
	}
	

	private String addChange(ChangeEvent event){
		EventActionsEnum action = event.getType();
		String path = event.getFullPath();
		final String oldPath = EventActionsEnum.RENAME.equals(action) ? event.getOldFullPath() : null;
		final Long time = event.getTimestamp();
		final String message = String.valueOf(time);
		if (LOG.isDebugEnabled())
			LOG.debug("addChange: " + action.protocolValue() + " "+ path);
		final String gitDir = getVersionControlDir();
		String res = versionController.addChange(gitDir, path, oldPath, message, false);
		
		return res;
	}
	
	private void processEvent(ChangeEvent event) {
		String res = addChange(event);
		if (res == null)
			return;
		
		processCommit(event, res);
	}
	
	private void processCommit(ChangeEvent event, final String message) {
		EventActionsEnum action = event.getType();
		String path = event.getFullPath();
		final String gitDir = getVersionControlDir();
		String revId = versionController.commit(gitDir, message);
		String[] revIds = revId == null ? null : new String[]{revId};
		event.setRevisionId(revId);
		
		Collection<RevisionDifferItem> items = null;
		if (!EventActionsEnum.RENAME.equals(action)) {
			items = revIds == null ? null : getDiffItems(revIds, Arrays.asList(path));
			if ((items == null || items.size() <= 0) ) {
				return;
			}
		}
		// adjust action 
		boolean folderCreate = EventActionsEnum.CREATE.equals(action) && (new File(event.getFullPath()).isDirectory());

		EventActionsEnum adjustedAction = folderCreate ? action : getAdjustedAction(items, action);
		if (!adjustedAction.equals(action)){
			LOG.info("Event changed from "+action+" to "+ adjustedAction );
			action = adjustedAction;
			event.setType(action);
		}
		
		// set the diff on modify events
		
		if (EventActionsEnum.MODIFY.equals(action) || folderCreate) {
			String diff = getDiff(path, items);
			if (diff == null)
				return;
			event.setDiff(diff);
		}
		
		// log event
		
		if (LOG.isInfoEnabled()){
			final String oldPathMsg = event.getOldFullPath() == null ? "" : " Old path: " + event.getOldFullPath();
			LOG.info("Event processed and fired: revId:"+  event.getRevisionId() + " " + event.getType() + " "+ event.getFullPath() + " " + oldPathMsg);
		}
		if (LOG.isDebugEnabled() && event.getDiff() != null)
			LOG.debug("Diff: "+event.getDiff());
		
		// fire event
		
		fireEvent(event);
	}
	
	private String getDiff(String path,
			final Collection<RevisionDifferItem> items) {
		String diff;
//		if (LOG.isDebugEnabled())
//			LOG.debug("---BENCHMARK--- getDiff start");
		File f = new File(path);
		boolean folder = (f!=null && f.isDirectory());
		
		if (items == null || items.size() <=0)
			diff = null;
		else if (items.size() == 1 && !folder){
			RevisionDifferItem item = items.iterator().next();
			diff = versionController.getLastChangeDiff(getVersionControlDir(),item);
			// remove verbose diff info
			if (diff != null){
				int pos = diff.indexOf("@@ ");
				if (pos > 0 && diff.length() > pos+3){
					diff = path + System.getProperty("line.separator") + diff.substring(pos);
					//diff = item.getId() + ".."+item.getPrevVersionId() + '\n' + diff;
				}
			}
		}
		// For directories, instead of a diff, retrieving a status string
		else 
			diff = versionController.getStatus(path, items);
		// if nothing has changed, no reason to fire rename...
		if (diff == null){
			if (LOG.isDebugEnabled())
				LOG.debug("No diff detected for: "+ path);
			return null;
		}
		
		// TODO: this is temporary: (1) maxDiffSize is long - either change this or adjust method (2) calculation is not accurate (?).
		if (diff.length() > maxDiffSize / 2){
			diff = diff.substring(0, (int)maxDiffSize /2);
			diff += " (Diff truncated due to length...)";
		}
//		if (LOG.isDebugEnabled())
//			LOG.debug("---BENCHMARK--- getDiff end");

		return diff;
	}
	

	private EventActionsEnum getAdjustedAction(Collection<RevisionDifferItem> items, EventActionsEnum action){
		if (!EventActionsEnum.REGISTER.equals(action) && !EventActionsEnum.CREATE.equals(action) 
				||items == null || items.size() <=0)
			return action;
			
		if (items.size() == 1){
			
			RevisionDifferItem item = items.iterator().next();
			// check if the file already exists in version control, and is in fact just being changed
			// this can happen on restart, if files changed while the agent was down, or if a mistaken
			// create event is sent (seems to happen for editing in linux vi):		
			if (item.getChangeType().equals(ChangedType.MODIFY)){
				return EventActionsEnum.MODIFY;
			}
			
		} else { // jar file
			boolean isCreate = true;
			for (RevisionDifferItem item: items){
				if (!item.getChangeType().equals(ChangedType.ADD)){
					isCreate = false;
					break;
				}
			}
			// TODO: if !isCreate but and the previous commit deleted the jar, should be create, not modify
			if (!isCreate) {
				return EventActionsEnum.MODIFY;
			}
		}
		return action;
	}

	
	/**
	 * Get the {@link RevisionDifferItem} objects representing the file entries in the version control.
	 * @param revIds
	 * @param filePaths
	 * @return
	 */
	private Collection<RevisionDifferItem> getDiffItems(
			String[] revIds, final Collection<String> filePaths) {
		final Collection<RevisionDifferItem> items = new ArrayList<RevisionDifferItem>();
		for (final String id: revIds)
		{
			try{
				final Collection<RevisionDifferItem> revItems = versionController.getChangeInfo(id, getVersionControlDir(), filePaths);
				if (revItems != null && revItems.size() > 0){
					//if (revItems.size() > 1)
					//	LOG.warn("More than one diff item in revision");
					items.addAll(revItems);
				}
			} catch (IOException e) {
				LOG.error(e.getMessage(), e);
			}
		}
		return items;
	}

	
	private boolean contained(String s1, String s2){
		if (!s1.startsWith(s2))
			return false;
		
		// exclude the case where there are different directories with a identical prefix (...\aaa and ...\aaa-copy)
		File f ;
		String tmp = s1;
		while (tmp != null && tmp.length() >0){
			f = new File(tmp);
			if (tmp.equals(s2))
				return true;
			tmp = f.getParent();
		}
		return false;
	}

	private void addToDirCopy(ChangeEvent e, Collection<String> batchCreateFiles) {
		String path = e.getFullPath();
		File f = new File(e.getFullPath());
		if (f.isDirectory()){
			if (LOG.isDebugEnabled())
				LOG.debug("Skipping directory "+path);
			return;
		}
		batchCreateFiles.add(path);
		//return  addChange(event) != null;
	}

	private void fireNClearCreateDirEvent(ChangeEvent lastCreateDirEvent, Collection<String> paths) {
		if (LOG.isDebugEnabled())
			LOG.debug("fireNClearCreateDirEvent: "+lastCreateDirEvent.getFullPath());
		final String gitDir = getVersionControlDir();
		String msg = lastCreateDirEvent.getFullPath()+"\n";
		String res = versionController.addChange(gitDir, paths, msg, false);
		paths.clear();
		if (res != null)
			processCommit(lastCreateDirEvent, msg);
	}


	public void setStop(boolean b) {
		this.stop = b;
	}
	private void fireEvent(ChangeEvent event) {
		changeListener.onChange(event);
	}
	
	public void onDestroy(){
		changeListener=null;
	}

	public String getVersionControlDir() {
		return versionContorlDir;
	}

	public void setVersionContorlDir(String gitDir) {
		this.versionContorlDir = gitDir;
	}

	
	
	
	public long getMaxDiffSize() {
		return maxDiffSize;
	}

	/** 
	 * Set max diff size in KB
	 * @param maxDiffSize - max size in KB
	 */
	public void setMaxDiffSize(long maxDiffSize) {
		this.maxDiffSize = maxDiffSize * 1024;
	}


}
