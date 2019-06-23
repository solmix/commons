package org.solmix.service.filetrack;

import java.io.File;

public interface Callable {
	/**
	 * Whether to act on a node
	 * @param f current node
	 * @return true to act on node, false to skip
	 */
	boolean passFilter(File f);
	
	/**
	 * Stopping condition (directory tree filter)
	 * @param f current node
	 * @return true to search children, false to stop
	 */
	boolean walkSubdirs(File f);
	
	/**
	 * Action to be performed on node before descending to children
	 * @param f current node
	 */
	void preOrder(File f);

	/**
	 * Action to be performed on node after ascending from children
	 * @param f current node
	 */
	void postOrder(File f);
}
