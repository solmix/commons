package org.solmix.service.filetrack.support;

import java.io.File;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.service.filetrack.Callable;
import org.solmix.service.filetrack.TrackedDirInfo;

public class DirsMapAdder implements Callable {

	private static Logger log = LoggerFactory.getLogger(DirsMapAdder.class);
	private Map<String, TrackedDirInfo> dirsMap;
	private String filter = null;

	public DirsMapAdder(Map<String, TrackedDirInfo> dirsMap, String filter) {
		this.dirsMap = dirsMap;
		this.filter = filter;
	}

	public boolean passFilter(File f) {
		return f.isDirectory();
	}
	// Arbitrarily deciding to walk preorder

	public void preOrder(File f) {
		if (log != null)
			log.debug("Adding "+f.getAbsolutePath()+" to dirsMap");
		dirsMap.put(f.getAbsolutePath(), new TrackedDirInfo(null, true, filter));
	}

	public void postOrder(File f) {}

	public boolean walkSubdirs(File f) { return true; } // No stopping condition
}
