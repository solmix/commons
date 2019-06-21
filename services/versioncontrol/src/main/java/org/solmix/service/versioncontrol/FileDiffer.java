package org.solmix.service.versioncontrol;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.EditList;
import org.eclipse.jgit.diff.MyersDiff;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileDiffer {
	static Logger logger = LoggerFactory.getLogger(FileDiffer.class);

	private final RevCommit commit;

	private final DiffEntry diffEntry;

	private static ObjectId[] trees(final RevCommit commit) {
		final ObjectId[] r = new ObjectId[commit.getParentCount() + 1];
		for (int i = 0; i < r.length - 1; i++) {
			RevTree tree = commit.getParent(i).getTree();
			if (tree == null)
				return null;
			r[i] = tree.getId();
		}
		r[r.length - 1] = commit.getTree().getId();
		return r;
	}

	public static FileDiffer[] compute(final TreeWalk walk, final RevCommit commit)
			throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		final ArrayList<FileDiffer> r = new ArrayList<FileDiffer>();

		ObjectId[] ids = null;
		if (commit.getParentCount() > 0) {
			ids = trees(commit);
		}
		if (ids != null) {
			walk.reset(ids);
		} else {
			walk.reset();
			walk.addTree(new EmptyTreeIterator());
			walk.addTree(commit.getTree());
		}
		List<DiffEntry> entries = DiffEntry.scan(walk);

		for (DiffEntry entry : entries) {
			final FileDiffer d = new FileDiffer(commit, entry);
			r.add(d);
		}

		final FileDiffer[] tmp = new FileDiffer[r.size()];
		r.toArray(tmp);
		return tmp;
	}

	public static FileDiffer[] compute(final TreeWalk walk, final RevCommit commit, RevCommit commitComp,
			boolean recursive)
			throws MissingObjectException, IncorrectObjectTypeException, CorruptObjectException, IOException {
		final ArrayList<FileDiffer> r = new ArrayList<FileDiffer>();

		final RevCommit commit1 = commit;// commit == null ? commitComp :
											// commit;
		final RevCommit commit2 = commitComp;// commit == null ? null :
												// commitComp;
		if (commit1 != null && commit2 != null) {
			walk.reset();
			walk.addTree(commit1.getTree());
			walk.addTree(commit2.getTree());
		} else if (commit1.getParentCount() > 0) {
			walk.reset(trees(commit1));
			if (commit2 != null)
				walk.addTree(commit2.getTree());
		} else {
			walk.reset();
			walk.addTree(new EmptyTreeIterator());
			walk.addTree(commit.getTree());
			if (commit2 != null)
				walk.addTree(commit2.getTree());
		}
		walk.setRecursive(recursive);

		if (walk.getTreeCount() <= 2) {
			List<DiffEntry> entries = DiffEntry.scan(walk);
			for (DiffEntry entry : entries) {
				final FileDiffer d = new FileDiffer(commit1, entry);
				r.add(d);
			}
		} else { // DiffEntry does not support walks with more than two trees
			final int nTree = walk.getTreeCount();
			final int myTree = nTree - 1;
			while (walk.next()) {
				// if (matchAnyParent(walk, myTree))
				// continue;

				final FileDifferForMerges d = new FileDifferForMerges(commit1);
				d.path = walk.getPathString();
				int m0 = 0;
				for (int i = 0; i < myTree; i++)
					m0 |= walk.getRawMode(i);
				final int m1 = walk.getRawMode(myTree);
				d.change = ChangeType.MODIFY;
				if (m0 == 0 && m1 != 0)
					d.change = ChangeType.ADD;
				else if (m0 != 0 && m1 == 0)
					d.change = ChangeType.DELETE;
				else if (m0 != m1 && walk.idEqual(0, myTree))
					d.change = ChangeType.MODIFY; // there is no
													// ChangeType.TypeChanged
				d.blobs = new ObjectId[nTree];
				d.modes = new FileMode[nTree];
				for (int i = 0; i < nTree; i++) {
					d.blobs[i] = walk.getObjectId(i);
					d.modes[i] = walk.getFileMode(i);
				}
				r.add(d);
			}

		}

		final FileDiffer[] tmp = new FileDiffer[r.size()];
		r.toArray(tmp);
		return tmp;
	}


	/**
	 * Creates a textual diff together with meta information. TODO So far this works
	 * only in case of one parent commit.
	 * 
	 * @param d         the StringBuilder where the textual diff is added to
	 * @param db        the Repo
	 * @param diffFmt   the DiffFormatter used to create the textual diff
	 * @param gitFormat if false, do not show any source or destination prefix, and
	 *                  the paths are calculated relative to the eclipse project,
	 *                  otherwise relative to the git repository
	 * @throws IOException
	 */
	public void outputDiff(final StringBuilder d, final Repository db, final DiffFormatter diffFmt, boolean gitFormat)
			throws IOException {
		if (gitFormat) {
			diffFmt.setRepository(db);
			diffFmt.format(diffEntry);
			return;
		}

		ObjectReader reader = db.newObjectReader();
		try {
			outputEclipseDiff(d, db, reader, diffFmt);
		} finally {
			reader.close();
		}
	}

	/**
	 * Same as above, except blobs and modes supplied by caller.
	 * 
	 * @param d
	 * @param db
	 * @param diffFmt
	 * @param gitFormat
	 * @param blobs
	 * @param modes
	 * @throws IOException
	 */
	public static void outputDiff(final StringBuilder d, final Repository db, final DiffFormatter diffFmt, 
			final ObjectId[] blobs, final FileMode[] modes, final String filePath) throws IOException {
		
		ObjectReader reader = db.newObjectReader();
		try {
			outputEclipseDiff(d, db, reader, diffFmt, blobs, modes, filePath);
		} finally {
			reader.close();
		}
	}

	private void outputEclipseDiff(final StringBuilder d, final Repository db, final ObjectReader reader,
			final DiffFormatter diffFmt) throws IOException {
		outputEclipseDiff(d, db, reader, diffFmt, getBlobs(), getModes(), getProjectRelativePath(db, getPath()));
	}

	private static void outputEclipseDiff(final StringBuilder d, final Repository db, final ObjectReader reader,
			final DiffFormatter diffFmt, final ObjectId[] blobs, final FileMode[] modes, final String path)
			throws IOException {

		if (!(blobs.length == 2))
			throw new UnsupportedOperationException("Not supported yet if the number of parents is different from one"); //$NON-NLS-1$

		String projectRelativePath = path;
		d.append("diff --git ").append(projectRelativePath).append(" ")
				.append(projectRelativePath).append("\n"); 
		final ObjectId id1 = blobs[0];
		final ObjectId id2 = blobs[1];
		final FileMode mode1 = modes[0];
		final FileMode mode2 = modes[1];

		if (id1.equals(ObjectId.zeroId())) {
			d.append("new file mode " + mode2).append("\n"); 
		} else if (id2.equals(ObjectId.zeroId())) {
			d.append("deleted file mode " + mode1).append("\n"); 
		} else if (!mode1.equals(mode2)) {
			d.append("old mode " + mode1); 
			d.append("new mode " + mode2).append("\n"); 
		}
		d.append("index ").append(reader.abbreviate(id1).name()). 
				append("..").append(reader.abbreviate(id2).name()). 
				append(mode1.equals(mode2) ? " " + mode1 : "").append("\n"); 
		if (id1.equals(ObjectId.zeroId()))
			d.append("--- /dev/null\n");
		else {
			d.append("--- ");
			d.append(path);
			d.append("\n"); 
		}

		if (id2.equals(ObjectId.zeroId()))
			d.append("+++ /dev/null\n"); 
		else {
			d.append("+++ "); 
			d.append(path);
			d.append("\n"); 
		}

		final RawText a = getRawText(id1, reader);
		final RawText b = getRawText(id2, reader);
		EditList editList = MyersDiff.INSTANCE.diff(RawTextComparator.DEFAULT, a, b);
		diffFmt.format(editList, a, b);
	}

	private String getProjectRelativePath(Repository db, String repoPath) {
		return repoPath;
	}

	private static RawText getRawText(ObjectId id, ObjectReader reader) throws IOException {
		if (id.equals(ObjectId.zeroId()))
			return new RawText(new byte[] {});
		try {
			ObjectLoader ldr = reader.open(id, Constants.OBJ_BLOB);
			return new RawText(ldr.getCachedBytes(Integer.MAX_VALUE));
		} catch (MissingObjectException e) {
			logger.warn(e.getMessage());
			return new RawText(new byte[] {});
		}
	}

	public RevCommit getCommit() {
		return commit;
	}

	public String getPath() {
		if (ChangeType.DELETE.equals(diffEntry.getChangeType()))
			return diffEntry.getOldPath();
		return diffEntry.getNewPath();
	}

	public ChangeType getChange() {
		return diffEntry.getChangeType();
	}

	public ObjectId[] getBlobs() {
		List<ObjectId> objectIds = new ArrayList<ObjectId>();
		if (diffEntry.getOldId() != null)
			objectIds.add(diffEntry.getOldId().toObjectId());
		if (diffEntry.getNewId() != null)
			objectIds.add(diffEntry.getNewId().toObjectId());
		return objectIds.toArray(new ObjectId[] {});
	}

	public FileMode[] getModes() {
		List<FileMode> modes = new ArrayList<FileMode>();
		if (diffEntry.getOldMode() != null)
			modes.add(diffEntry.getOldMode());
		if (diffEntry.getOldMode() != null)
			modes.add(diffEntry.getOldMode());
		return modes.toArray(new FileMode[] {});
	}

	FileDiffer(final RevCommit c, final DiffEntry entry) {
		diffEntry = entry;
		commit = c;
	}

	private static class FileDifferForMerges extends FileDiffer {
		private String path;

		private ChangeType change;

		private ObjectId[] blobs;

		private FileMode[] modes;

		private FileDifferForMerges(final RevCommit c) {
			super(c, null);
		}

		@Override
		public String getPath() {
			return path;
		}

		@Override
		public ChangeType getChange() {
			return change;
		}

		@Override
		public ObjectId[] getBlobs() {
			return blobs;
		}

		@Override
		public FileMode[] getModes() {
			return modes;
		}
	}
}
