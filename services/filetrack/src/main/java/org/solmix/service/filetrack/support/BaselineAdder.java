package org.solmix.service.filetrack.support;

import java.io.File;

import org.solmix.service.filetrack.Callable;
import org.solmix.service.filetrack.EventActionsEnum;

/**
 * 添加基线
 */
public class BaselineAdder implements Callable {
	Watcher watcher;
	boolean recursive;
	String filter;
	EventActionsEnum eventType;
	

	public BaselineAdder(Watcher watcher, boolean recursive, String filter) {
		this(watcher, recursive, filter, EventActionsEnum.REGISTER);
	}

	public BaselineAdder(Watcher watcher, boolean recursive, String filter, EventActionsEnum eventType) {
		this.watcher = watcher;
		this.recursive = recursive;
		this.filter = filter;
		this.eventType = eventType;
	}
	/**
	 * Files are later filtered by watcher before actually being added to senderQueue;
	 */
	public boolean passFilter(File f) {
		return f.exists() && f.isFile();
	}

	public boolean walkSubdirs(File f) {
		return recursive;
	}

	/**
	 * Adding the events at preorder so version control can apply them
	 */
	public void preOrder(File f) {
		watcher.simulateEvent(eventType, filter, f.getAbsolutePath());
	}

	public void postOrder(File f) {
	}

}

