package org.solmix.service.filetrack.support;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.service.filetrack.Callable;
import org.solmix.service.filetrack.TrackedDirInfo;

public class WatchedFolderAdder implements Callable {
	Watcher watcher = null;
	String filter = null;
	private static Logger log = LoggerFactory.getLogger(WatchedFolderAdder.class);

	public WatchedFolderAdder(Watcher watcher, String filter) {
		this.watcher = watcher;
		this.filter = filter;
	}

	public boolean passFilter(File f) {
		return f.isDirectory();
	}
	// Arbitrarily deciding to walk preorder

	public void preOrder(File f) {
		if (log != null)
			log.debug("Adding "+f.getAbsolutePath()+" to watcher");
		watcher.addWatchedFolder(f.getAbsolutePath(), new TrackedDirInfo(null, true, filter));
	}

	public void postOrder(File f) {}

	public boolean walkSubdirs(File f) { return true; } // No stopping condition
}
