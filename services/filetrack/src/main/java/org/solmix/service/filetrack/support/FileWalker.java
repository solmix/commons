package org.solmix.service.filetrack.support;

import java.io.File;

import org.solmix.service.filetrack.Callable;

public class FileWalker {
	

	public static void walkDfs(File dir, Callable callable) {
		File[] files = dir.listFiles();
		if (files == null) return;
		for (File f : files) {
			boolean passFilter = callable.passFilter(f);
			if (passFilter) 
				callable.preOrder(f);
			if (f.isDirectory() && callable.walkSubdirs(f)){
				walkDfs(f, callable);
			}
			if (passFilter) 
				callable.postOrder(f);
		}
	}
}

