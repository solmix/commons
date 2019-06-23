package org.solmix.service.filetrack;

import java.nio.file.Path;

public class TrackedDirInfo {
	private Path path;
	private boolean recursive;
	private String filter;

	public Path getPath() {
		return path;
	}

	public void setPath(Path watchKey) {
		path = watchKey;
	}

	public boolean isRecursive() {
		return recursive;
	}

	public void setRecursive(boolean recursive) {
		this.recursive = recursive;
	}

	public String getFilter() {
		return filter;
	}

	public void setFilter(String filter) {
		this.filter = filter;
	}

	public TrackedDirInfo(Path path, boolean recursive, String filter) {
		super();
		this.path = path;
		this.recursive = recursive;
		this.filter = filter;
	}

	public TrackedDirInfo(TrackedDirInfo info) {
		this(info.path, info.recursive, info.filter);
	}
}
