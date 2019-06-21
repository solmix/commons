package org.solmix.service.versioncontrol.support;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.LinkedList;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddAllCommand extends AddCommand {
	/**
	 * Executes the {@code Add} command. Each instance of this class should only
	 * be used for one invocation of the command. Don't call this method twice
	 * on an instance.
	 *
	 * @return the DirCache after Add
	 */
    private static Logger log = LoggerFactory.getLogger(AddAllCommand.class);

	private Collection<String> zfilepatterns;

	private WorkingTreeIterator zworkingTreeIterator;

	/**
	 *
	 * @param repo
	 */
	public AddAllCommand(Repository repo) {
		super(repo);
		zfilepatterns = new LinkedList<String>();
	}

	/**
	 * @param filepattern
	 *            File to add content from. Also a leading directory name (e.g.
	 *            dir to add dir/file1 and dir/file2) can be given to add all
	 *            files in the directory, recursively. Fileglobs (e.g. *.c) are
	 *            not yet supported.
	 * @return {@code this}
	 */
	public AddCommand addFilepattern(String filepattern) {
		checkCallable();
		zfilepatterns.add(filepattern);
		return this;
	}	
	
	/**
	 * Allow clients to provide their own implementation of a FileTreeIterator
	 * @param f
	 * @return {@code this}
	 */
	public AddCommand setWorkingTreeIterator(WorkingTreeIterator f) {
		zworkingTreeIterator = f;
		return this;
	}


	/**
	 * Allow clients to provide their own implementation of a FileTreeIterator
	 * @param f
	 * @return {@code this}
	 */
	public WorkingTreeIterator getWorkingTreeIterator() {
		return zworkingTreeIterator;
	}

	public DirCache call() throws NoFilepatternException {
		if (zfilepatterns.isEmpty())
			throw new NoFilepatternException(JGitText.get().atLeastOnePatternIsRequired);
		checkCallable();
		DirCache dc = null;
		boolean addAll = false;
		if (zfilepatterns.contains("."))
			addAll = true;

		ObjectInserter inserter = repo.newObjectInserter();
		try {
			dc = repo.lockDirCache();
			DirCacheIterator c;

			DirCacheBuilder builder = dc.builder();
			final TreeWalk tw = new TreeWalk(repo);
			tw.addTree(new DirCacheBuildIterator(builder));
			if (zworkingTreeIterator == null)
				zworkingTreeIterator = new FileTreeIterator(repo);
			tw.addTree(zworkingTreeIterator);
			tw.setRecursive(true);
			if (!addAll)
				tw.setFilter(PathFilterGroup.createFromStrings(zfilepatterns));

			String lastAddedFile = null;

			while (tw.next()) {
				String path = tw.getPathString();
				if (log.isDebugEnabled())
					log.debug("Iterating file: "+path);

				WorkingTreeIterator f = tw.getTree(1, WorkingTreeIterator.class);
				if (tw.getTree(0, DirCacheIterator.class) == null &&
						f != null && f.isEntryIgnored()) {
					// file is not in index but is ignored, do nothing
				}
				// In case of an existing merge conflict the
				// DirCacheBuildIterator iterates over all stages of
				// this path, we however want to add only one
				// new DirCacheEntry per path.
				else if (!(path.equals(lastAddedFile))) {
					if (!(isUpdate() && tw.getTree(0, DirCacheIterator.class) == null)) {
						c = tw.getTree(0, DirCacheIterator.class);
						if (f != null) { // the file exists
							long sz = f.getEntryLength();
							DirCacheEntry entry = new DirCacheEntry(path);
							if (c == null || c.getDirCacheEntry() == null
									|| !c.getDirCacheEntry().isAssumeValid()) {
								entry.setLength(sz);
								entry.setLastModified(f.getEntryLastModified());
								entry.setFileMode(f.getEntryFileMode());

								InputStream in = f.openEntryStream();
								try {
									entry.setObjectId(inserter.insert(
											Constants.OBJ_BLOB, sz, in));
								} finally {
									in.close();
								}

								builder.add(entry);
								lastAddedFile = path;
							} else {
								builder.add(c.getDirCacheEntry());
							}

						} /*else if (!isUpdate()){
							builder.add(c.getDirCacheEntry());
						}*/
					}
				}
			}
			inserter.flush();
			builder.commit();
			setCallable(false);
		} catch (IOException e) {
			throw new JGitInternalException(
					JGitText.get().exceptionCaughtDuringExecutionOfAddCommand, e);
		} finally {
			inserter.close();
			if (dc != null)
				dc.unlock();
		}

		return dc;
	}

}
