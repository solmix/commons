package org.solmix.service.versioncontrol.support;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuildIterator;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.CorruptObjectException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.internal.JGitText;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.service.versioncontrol.FileDiffer;
import org.solmix.service.versioncontrol.Revision;
import org.solmix.service.versioncontrol.RevisionDifferItem;
import org.solmix.service.versioncontrol.VersionController;



public class GitVersionController implements VersionController
{
    private static Logger LOG = LoggerFactory.getLogger(GitVersionController.class);
	private static final String GIT_DIR = ".git";
	private static final int DEFAULT_PAGE_SIZE = -1;
	private Set<String> binaryExtSet=null;
	private Thread mapPersisterThread;
	private Long fileCount = 0L;
	private Long zipFileCount = 0L;
	private final Map<String, Repository> repositoryMap = new HashMap<String, Repository>();
	private final Map<String, PairLong> fileInfoMap = new TreeMap<String,PairLong>();
	private String extMonitoredInArchive;
	
    private String getGitDir(final String gitDirBase) {
		return gitDirBase + File.separator + GIT_DIR;
	}
    @Override
    public boolean createRepository(String gitDirBase) {
    	File git = new File(getGitDir(gitDirBase));
		MapPersister mapPersister = new MapPersister(fileInfoMap, gitDirBase); 
		mapPersister.init(!git.exists());
		mapPersisterThread = new Thread(mapPersister, "VC-MapPersister");
		mapPersisterThread.start();
		
		if (git.exists()){
			File gitLoc = new File(git.getPath()+File.separator+"index.loc");
			if (gitLoc.exists())
				gitLoc.delete();
		}
		
		if (gitDirExists(gitDirBase))
			return true;
		
		if (gitDirBase == null)
			return false;
		final File f = new File(gitDirBase);
		if (!gitDirExists(gitDirBase)) {
			Utils.recursiveCreateDir(gitDirBase);
			if (!f.exists() && !f.mkdir()) {
				LOG.error("Failure creating repository git Dir folder: " + gitDirBase);
				return false;
			}
		}

		final Repository repository = openRepository(gitDirBase);
		if (repository == null)
			return false;
		try {
			repository.create();
		} catch (Exception e) {
			LOG.error("Error creating repository " + e.getMessage(), e);
			return false;
		}
		closeRepositoryTransaction(gitDirBase);
		return true;
    }
    
    public Repository openRepository(final String gitDirBase) {

		final String gitDir = getGitDir(gitDirBase);
		Repository repository = repositoryMap.get(gitDirBase);
		if (repository != null)
			return repository;
		final RepositoryBuilder builder = new RepositoryBuilder();

		final File fGitDir = new File(gitDir);

		try {
			// scan up the file system tree
			builder.setGitDir(fGitDir).readEnvironment().findGitDir();
			repository = builder.build();
		} catch (IOException e) {
			LOG.error("Error opening repository " + e.getMessage(), e);
			return null;
		}
		repositoryMap.put(gitDirBase, repository);
		return repository;
	}
    
    public void closeRepositoryTransaction(final String gitDir) {
		Repository r = repositoryMap.get(gitDir);
		if (r == null)
			return;
		repositoryMap.remove(gitDir);
		r.close();
	}
    
    public void closeRepositoryTransaction(final Repository repository) {
		final String gitDir = repository.getDirectory().getAbsolutePath();
		Repository r = repositoryMap.get(gitDir);
		if (r == null)
			return;
		repositoryMap.remove(gitDir);
		r.close();
	}
    
    private DirCache call(final String pathForGit, final UpdateInfo updateInfo,
			boolean addBody, InputStream is, long size, long lastModified,
			FileMode fileMode) {
		return call(pathForGit, null, updateInfo, addBody, is, size, lastModified, fileMode, null);
	}
	
	private DirCache call(final String pathForGit, final String oldPath, final UpdateInfo updateInfo,
			boolean isDelete, InputStream is, long size, long lastModified,
			FileMode fileMode, ObjectId id) {
		try {
			final String formattedPath = Utils.formatPath(pathForGit);

			if (oldPath != null && oldPath.length() > 0){
				updateInfo.addTreeFilter(FollowFilter.create(formattedPath,null));
				updateInfo.addTreeFilter(FollowFilter.create(Utils.formatPath(oldPath),null));
			}
			else
				updateInfo.addTreeFilter(PathFilter.create(formattedPath));

			if (!isDelete) {
				final DirCacheBuilder builder = updateInfo.getBuilder();
				DirCacheEntry entry = new DirCacheEntry(formattedPath);
				entry.setLength(size);
				entry.setLastModified(lastModified);
				entry.setFileMode(fileMode);
				if (id != null)
					entry.setObjectId(id);
				else
					entry.setObjectId(updateInfo.getInserter().insert(Constants.OBJ_BLOB, size,is)); 
				builder.add(entry);
			}
		} catch (IOException e) {
			throw new JGitInternalException(JGitText.get().exceptionCaughtDuringExecutionOfAddCommand, e);
		}
		return updateInfo.getDirCache();
	}
    
    private DirCache commitRenameFile(final String gitDir, final String path, final String oldPath,
			String commitMessage){
		final Repository repository = openRepository(gitDir);
		if (repository == null) {
			LOG.error("Repository missing or coruppeted in " + gitDir);
			return null;
		}
		UpdateInfo uInfo = new UpdateInfo(repository);
		try {
			final File file = new File(path);
			
			ObjectId id = getLastObjectId( oldPath, repository);
			if (id == null){
				LOG.error("Rename aborted, object id is null for old path " + oldPath);
				return null;
			}
			uInfo.init();
			DirCache dc2 = call(oldPath, uInfo, true, (InputStream) null, 0,
					0, (FileMode) null);
			uInfo.commit();
			uInfo.init();
			DirCache dc = call(path, oldPath, uInfo, false, null, file.length(),
					file.lastModified(), FileMode.REGULAR_FILE, id);
			uInfo.commit();
			if (dc == null)
				LOG.warn("Return value null for rename call create phase");
			if (dc2 == null)
				LOG.warn("Return value null for rename call delete phase");
			uInfo.release();
	
			return dc;
		} catch (AmbiguousObjectException e2) {
			LOG.error(e2.getMessage(), e2);
		} catch (IOException e2) {
			LOG.error(e2.getMessage(), e2);
		}catch (NoWorkTreeException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			closeRepositoryTransaction(gitDir);
		}
		return null;
	}
    
    private ObjectId getLastObjectId(String path, Repository repository) throws AmbiguousObjectException, IOException{

		final RevWalk rw = new RevWalk(repository);
		final ObjectId objHead = repository.resolve("HEAD");
		if (objHead == null) {
			LOG.error("Repository missing or coruppeted - HEAD not found");
			return null;
		}
		rw.markStart(rw.parseCommit(objHead));		
		TreeFilter pathFilter = PathFilter.create(Utils.formatPath(path));
		rw.setTreeFilter(pathFilter);// , TreeFilter.ANY_DIFF));
		for (RevCommit c : rw) {
			final FileDiffer[] diffs = getChangeInfo(c, repository,
					pathFilter, true);
			if (diffs == null || diffs.length <=0)
				continue;
			ObjectId[] ids = diffs[0].getBlobs();
			if (ids == null || ids.length <=0 )
				return null;
			if (ids.length > 1 && !ids[1].equals(ObjectId.zeroId()))
				return ids[1];
			if (!ids[0].equals(ObjectId.zeroId()))
				return ids[0];
			return null;
		}
		rw.dispose();
		return null;
	}
    
	private FileDiffer[] getChangeInfo(final RevCommit c,
			final Repository repository, final TreeFilter pathFilter,
			final boolean changesOnly) throws IOException {
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- FileDiff[] getChangeInfo start");
		TreeWalk tw = new TreeWalk(repository);
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- FileDiff[] getChangeInfo after treewalk create");
		if (pathFilter != null)
			tw.setFilter(AndTreeFilter.create(pathFilter, TreeFilter.ANY_DIFF));
		else
			tw.setFilter(TreeFilter.ANY_DIFF);
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- getChangeInfo before compute");
		FileDiffer[] diffs = FileDiffer.compute(tw, c, null, true);
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- getChangeInfo after compute");
		if ((diffs == null || diffs.length <= 0) && changesOnly) {
			return null;
		}
		return diffs;
	}
    
    @Override
    public String[] doRename(String gitDir, String oldPath, String newPath, String message) {
    	DirCache dc = commitRenameFile(gitDir, newPath, oldPath, message);
		final String ret = dc == null ? "" : commit(gitDir, message);
		return new String[] {ret};
    }

    @Override
    public String addChange(String gitDir, String path, String commitMessage) {
    	return addChange(gitDir, path, null, commitMessage, true);
    }

    @Override
    public String addChange(String gitDir, String path, String oldPath, String commitMessage, boolean performCommit) {
    	if (!addToMap(path) && oldPath == null)
			return null;
			
		File file = new File(path);
		
		boolean retVal = false;
		final String comment = commitMessage + ";" + file.lastModified() + ";"
				+ file.length()+ ";"+ file.getName();
		if (/*file.isDirectory() ||*/ !file.exists()) {
//			return commitDeleteFile(gitDir, path, comment);
			retVal = commitChangeFile(gitDir, Arrays.asList(path), true);
			deleteFromInfoMap(path);
		}
		
		// handle rename
		else if (oldPath != null){
			retVal = commitRenameFile(gitDir, path, oldPath, comment) != null;
			deleteFromInfoMap(oldPath);
		}
		
		// 
		else 
			retVal = commitChangeFile(gitDir,  Arrays.asList(path), false);
		
		if (retVal && performCommit)
			return commit(gitDir, comment);
		
		return retVal ?  comment : null;
    }
    
    private boolean commitChangeFile(final String gitDir, final Collection<String> paths,
			boolean setUpdateFlag) {
		final Repository repository = openRepository(gitDir);

		AddCommand addFile = null;
		String currRoot = null;
		
		AddCommand add = null;
		
		/**
		 * VERY IMPORTANT!!!!!!!!
		 * int the following loop the addCommand objects are called and then released before 
		 * the next object is created. If not, a potential out of memory can occur if many
		 * addCommand objects are held in memory simultaniosly!!!
		 */
		
		for (String path: paths){
			final String formattedPath = Utils.formatPath(path);
			String root = Utils.getRoot(formattedPath);
			String zipPath = formattedPath;
			// for linux add backslash to root.
			if (path.startsWith("/")){
				root = "/"+root;
				zipPath = "/"+zipPath;
			}
			
			if (Utils.isZipArchive(formattedPath)) {
				if (add!= null  && add instanceof AddAllCommand)
					callAdd(add);
				final ZipfileTreeIterator zipTreeIterator = new ZipfileTreeIterator(repository, zipPath);
				zipTreeIterator.setIncludedExtensions(getExtMonitoredInArchive());
				add = initAddcommand(new AddAllCommand(repository), zipTreeIterator);
			} else {
				if (add != null && add instanceof AddAllCommand)
					callAdd(add);
				if (addFile == null || !root.equals(currRoot)){
					addFile = initAddcommand(new AddCommand(repository), new ExtendedTreeIterator(repository, root));
					currRoot = root;
				}
				add = addFile;
			}
			add.addFilepattern(formattedPath);
			add.setUpdate(setUpdateFlag);
		}
		
		if (add != null  && add != addFile)
			callAdd(add);
		if (addFile != null)
			callAdd(addFile);

		return true;

	}
    public String getExtMonitoredInArchive() {
		return extMonitoredInArchive;
	}

	public void setExtMonitoredInArchive(String extMonitoredInArchive) {
		this.extMonitoredInArchive = extMonitoredInArchive;
	}
	private AddCommand initAddcommand(AddCommand addCommand,
			ExtendedTreeIterator fileTreeIterator) {
		fileTreeIterator.setExtensionsForMd5(binaryExtSet);
		addCommand.setWorkingTreeIterator(fileTreeIterator);
		return addCommand;
	}

	public boolean isBinary(String path) {
		if (binaryExtSet == null || binaryExtSet.size() <=0 || path == null || path.length() <= 0)
			return false;
		if (binaryExtSet.contains(Utils.getFileExtension(Utils.fileName(path))))
			return true;
		return false;
	}
    private boolean callAdd(AddCommand add) {
		boolean res = false;
		try {
			res = add.call() != null;
		} catch (GitAPIException e) {
			LOG.error(e.getMessage(), e);
		}
		if (add instanceof AddAllCommand){
			WorkingTreeIterator it = ((AddAllCommand)add).getWorkingTreeIterator();
			if (it instanceof ZipfileTreeIterator){
				ZipfileTreeIterator zipIt = (ZipfileTreeIterator)it;
				addZipFileCount(zipIt.getFileCount());
				try {
					zipIt.closeStream();
				} catch (IOException e) {
					LOG.error(e.getMessage(), e);
				}
			}
		}

		return res;
	}
    
    private boolean addToMap(String path){
		File file = new File(path);
		long lastModified = file.lastModified();
		long fileSize = file.length();
		PairLong info = fileInfoMap.get(path);
		if (info != null && info.getKey().equals(lastModified) && info.getValue().equals(fileSize) && file.exists()){
			LOG.info("Date and size unchanged, skipping - "+path);
			return false;
		}
		fileInfoMap.put(path, new PairLong(lastModified, fileSize));
		return true;
	}

    private void deleteFromInfoMap(String toRemove) {
		PairLong o =fileInfoMap.remove(toRemove);
		if (o != null)
			return;

		String path = toRemove.endsWith(File.pathSeparator) ? toRemove : toRemove + File.separator;
		o =fileInfoMap.remove(path);
		if (o != null)
			return;

		// deal with case of folder delete
		Collection<String> toDelete = new LinkedList<String>();
		for (String s: fileInfoMap.keySet())
			if(s.startsWith(path) && !(new File(s).exists()))
				toDelete.add(s);
		for (String s: toDelete)
			fileInfoMap.remove(s);
	}
    
    @Override
    public String addChange(String gitDir, Collection<String> paths, String commitMessage, boolean performCommit) {
    	boolean retVal = false;
		final Collection<String> changedFiles = new ArrayList<String>();
		for (String path: paths) {
			retVal = addToMap(path) ;
			if (retVal)
				changedFiles.add(path);
		}
		if (changedFiles.size() <= 0)
			return null;
		
		retVal = commitChangeFile(gitDir, changedFiles, false);
			
		if (retVal && performCommit)
			return commit(gitDir, "multiple files changed.");
			
		return retVal == false? null : String.valueOf(retVal);
    }

    @Override
    public String commit(String gitDir, String commitMessage) {
    	if (LOG.isDebugEnabled())
    		LOG.debug("---BENCHMARK--- commit start");
		final Repository repository = openRepository(gitDir);
		String id = null;
		final RevCommit rc = doCommit(repository, commitMessage);
		id = rc == null ? null : ObjectId.toString(rc.getId());
		closeRepositoryTransaction(repository);
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- commit end");
		return id;
    }
    
	public RevCommit doCommit(final Repository repository, String message) {
		 Git git = null;
		try {
			git = new Git(repository);
			CommitCommand commit = git.commit();

			commit.setMessage(message);
			final RevCommit c = commit.call();
			return c;
		} catch (Exception e) {
			LOG.error("Error in commit" + e.getMessage(), e);
		}finally {
			if(git!=null) {
				git.close();
			}
		}
		return null;
	}
	
    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, null, null,
				null, DEFAULT_PAGE_SIZE);
    }

    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, int pageSize) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, null, null,
				null, pageSize);
    }

    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, String revision, int pageSize) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, null, null,
				revision, pageSize);
    }

    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, startDate,
				null, null, DEFAULT_PAGE_SIZE);
    }

    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate, Long endDate) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, startDate,
				endDate, null, DEFAULT_PAGE_SIZE);
    }

    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate, Long endDate,
        int pageSize) {
    	return getRevisionsForPath(filepaths, gitDir, changesOnly, startDate,
				endDate, null, pageSize);
    }

    
    private TreeFilter getTreeFilter(final Collection<String> filepaths) {
		TreeFilter pathFilter = null;
		if (filepaths != null && filepaths.size() > 0) {
			Collection<String> paths = new ArrayList<String>();
			for (final String filepath : filepaths) {
				final String path = filepath == null ? null
						: Utils.formatPath(filepath);
				if (path != null && path.length() > 0)
					paths.add(path);
			}
			if (paths.size() > 0)
				pathFilter = PathFilterGroup.createFromStrings(paths);
		}
		return pathFilter;
	}
    
    @Override
    public List<Revision> getRevisionsForPath(Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate, Long endDate,
        String revision, int pageSize) {
    	List<Revision> commitList = new ArrayList<Revision>();
		final Repository repository = openRepository(gitDir);
		if (repository == null) {
			LOG.error("Repository missing or coruppeted in " + gitDir);
			return commitList;
		}

		final RevWalk rw = new RevWalk(repository);
		try {
			final ObjectId objHead = repository.resolve(revision != null ? revision : "HEAD");
			if (objHead == null) {
				LOG.error("Repository missing or coruppeted - HEAD not found in " + gitDir);
				return commitList;
			}
			rw.markStart(rw.parseCommit(objHead));
			
			TreeFilter pathFilter = getTreeFilter(filepaths);
			
			if (pathFilter != null)
				rw.setTreeFilter(pathFilter);// , TreeFilter.ANY_DIFF));
			else
				rw.setTreeFilter(TreeFilter.ALL);
			
			int pageCounter = 0;

			for (RevCommit c : rw) {

				if (pageSize > 0 && pageCounter >= pageSize) {
					break;
				}

				if (!isCommitInDateRange(c, startDate, endDate)) {
					continue;
				}

				final FileDiffer[] diffs = getChangeInfo(c, repository,
						pathFilter, changesOnly);
				if (diffs == null)
					continue;
				final Revision info = new Revision(c);
				for (FileDiffer fd : diffs) {
					info.addDiff(fd);
				}
				commitList.add(info);
			}
			rw.dispose();
		} catch (AmbiguousObjectException e2) {
			LOG.error(e2.getMessage(), e2);
		} catch (IOException e2) {
			LOG.error(e2.getMessage(), e2);
		} finally {
			closeRepositoryTransaction(gitDir);
		}
		return commitList;
    }
    
    private boolean isCommitInDateRange(RevCommit commit, Long startDate,
			Long endDate) {
		long time = commit.getCommitTime() * 1000;
		boolean inRange = true;
		inRange &= startDate == null ? true : time >= startDate;
		inRange &= endDate == null ? true : time <= endDate;
		return inRange;
	}

    @Override
    public FileDiffer[] compareRevisionToHead(String revId, String gitDir, String filepath) {
    	final Repository repository = openRepository(gitDir);
		if (repository == null) {
			LOG.error("Repository missing or coruppeted in " + gitDir);
			return null;
		}

		final String path = filepath == null ? null : Utils.formatPath(filepath);
		RevWalk rw = null;
		try {
			final ObjectId objHead = repository.resolve("HEAD");
			if (objHead == null) {
				LOG.error("Repository missing or coruppeted - HEAD not found in "
						+ gitDir);
				return null;
			}

			final ObjectId objRev = ObjectId.fromString(revId);
			if (objRev == null) {
				LOG.error("Revision to compare not found in " + gitDir);
				return null;
			}

			rw = new RevWalk(repository);
			RevCommit head = rw.parseCommit(objHead);
			RevCommit comparedRev = rw.parseCommit(objRev);

			TreeWalk tw = new TreeWalk(repository);
			if (path != null && path.length() > 0)
				tw.setFilter(AndTreeFilter.create(PathFilter.create(path),
						TreeFilter.ANY_DIFF));
			else
				tw.setFilter(TreeFilter.ANY_DIFF);
			
			return FileDiffer.compute(tw, head, comparedRev, true);

		} catch (AmbiguousObjectException e2) {
			LOG.error(e2.getMessage(), e2);
		} catch (IOException e2) {
			LOG.error(e2.getMessage(), e2);
		} finally {
			if (rw != null)
				rw.dispose();
			closeRepositoryTransaction(gitDir);
		}
		return null;
    }

    @Override
    public byte[] getFileByRevision(String gitDir, String revId) {
    	final Repository repository = openRepository(gitDir);
		if (repository == null) {
			LOG.error("Repository missing or coruppeted in " + gitDir);
			return null;
		}
		final ObjectId objId = ObjectId.fromString(revId);

		final ObjectLoader loader;
		byte[] bytes = null;
		try {
			loader = repository.open(objId, Constants.OBJ_BLOB);
			bytes = loader.getCachedBytes(Integer.MAX_VALUE);
		} catch (MissingObjectException e) {
			LOG.error("Error retreiving revision: " + e.getMessage(), e);
		} catch (IOException e) {
			LOG.error("Error retreiving revision: " + e.getMessage(), e);
		} finally {
			closeRepositoryTransaction(gitDir);
		}

		return bytes;
    }

    @Override
    public String getDiff(String gitDir, RevisionDifferItem revItem1, RevisionDifferItem revItem2) {
    	if (revItem1.getFileModeBits() == null
				|| revItem2.getFileModeBits() == null) {
			LOG.error("File mode null");
			return null;
		}
		final String[] ids = new String[] { revItem1.getId(), revItem2.getId() };
		final FileMode fileMode1 = FileMode
				.fromBits(revItem1.getFileModeBits());
		final FileMode fileMode2 = FileMode
				.fromBits(revItem2.getFileModeBits());

		final FileMode[] modes = new FileMode[] { fileMode1, fileMode2 };
		final String diff = outputDiff(gitDir, ids, modes, revItem1.getPath());
		return diff;
    }

    @Override
    public String getLastChangeDiff(String gitDir, RevisionDifferItem revItem) {
    	if (revItem == null) {
			LOG.error("Revision item null");
			return null;
		}

		if (revItem.getFileModeBits() == null) {
			LOG.error("File mode null");
			return null;
		}
				
		final String[] ids = new String[] { revItem.getPrevVersionId(),
				revItem.getId() };
		final FileMode fileMode1 = FileMode.fromBits(revItem.getFileModeBits());
		final FileMode fileMode2 = FileMode.fromBits(revItem
				.getPrevVersionFileModeBits());
		final FileMode[] modes = new FileMode[] { fileMode2, fileMode1 };
		final String diff = outputDiff(gitDir, ids, modes, revItem.getPath());
		if (isBinary(revItem.getPath()) && diff != null && diff.length() > 0)
			return revItem.getPath() + "\n@@ 0,0 0,0 @@\nBinary file changed";
		return diff;
    }
    
    public String outputDiff(final String gitDir, final String[] blobIds,
			final FileMode[] fileModes, final String path) {
		if (blobIds == null || blobIds.length < 2) {
			LOG.error("Number of object ids to compare is smaller than 2");
			return null;
		}

		final StringBuilder sb = new StringBuilder();
		final Repository repository = openRepository(gitDir);

		try {
			final ObjectId[] objIds = new ObjectId[2];
			objIds[0] = ObjectId.fromString(blobIds[0]);
			objIds[1] = ObjectId.fromString(blobIds[1]);

			final DiffFormatter formatter = new DiffFormatter(
					new BufferedOutputStream(new ByteArrayOutputStream() {

						@Override
						public synchronized void write(byte[] b, int off,
								int len) {
							super.write(b, off, len);
							sb.append(toString());

							reset();
						}

					})) {
			};

			if (LOG.isDebugEnabled())
				LOG.debug("---BENCHMARK--- before outputDiff");
			FileDiffer.outputDiff(sb, repository, formatter, objIds, fileModes,
					path);
			if (LOG.isDebugEnabled())
				LOG.debug("---BENCHMARK--- after outputDiff");
			formatter.flush();
			return sb.toString();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} finally {
			closeRepositoryTransaction(gitDir);
		}
		return null;
	}

    @Override
    public boolean gitDirExists(String gitDirBase) {
    	final String dir = getGitDir(gitDirBase);
		final File f = new File(dir);
		return f.exists() && f.isDirectory();
    }

    @Override
    public Collection<RevisionDifferItem> getChangeInfo(String revId, String gitDir, Collection<String> filepaths) throws IOException {
    	List<RevisionDifferItem> commitList = new ArrayList<RevisionDifferItem>();
		final Repository repository = openRepository(gitDir);
		if (repository == null) {
			LOG.error("Repository missing or corrupted in " + gitDir);
			return commitList;
		}

		final RevWalk rw = new RevWalk(repository);
		try {
			final ObjectId objHead = repository.resolve(/*
														 * revId != null ? revId
														 * :
														 */"HEAD");
			if (objHead == null) {
				LOG.error("Repository missing or coruppeted - HEAD not found in "
						+ gitDir);
				return commitList;
			}
			rw.markStart(rw.parseCommit(objHead));

			TreeFilter pathFilter = getTreeFilter(filepaths);

			if (pathFilter != null)
				rw.setTreeFilter(AndTreeFilter.create(pathFilter,
						TreeFilter.ALL));// , TreeFilter.ANY_DIFF));
			else
				rw.setTreeFilter(TreeFilter.ALL);

			if (LOG.isDebugEnabled())
				LOG.debug("---BENCHMARK--- getChangeInfo before loop");
			for (RevCommit c : rw) {
				final String cId = c.getId().toString();
				if (!cId.contains(revId))
					continue;
				final FileDiffer[] diffs = getChangeInfo(c, repository,
						pathFilter, true);
				if (diffs == null)
					break;
				for (FileDiffer fd : diffs) {
					commitList.add(new RevisionDifferItem(fd));
				}
				break;
			}
			if (LOG.isDebugEnabled())
				LOG.debug("---BENCHMARK--- getChangeInfo after loop");

			rw.dispose();
		} catch (AmbiguousObjectException e2) {
			LOG.error(e2.getMessage(), e2);
		} catch (IOException e2) {
			LOG.error(e2.getMessage(), e2);
		} finally {
			closeRepositoryTransaction(gitDir);
		}
		return commitList;
    }

    @Override
    public String getStatus(String dirPath, Collection<RevisionDifferItem> items) {
    	if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- getStatus start");
		int offset = Utils.formatPath(dirPath).length() + 1;
		String status = dirPath+"\n";
		for (RevisionDifferItem item : items) {
			status += String.format("%s\t%s\n", item.getChangeType().name()
					.charAt(0), item.getPath().substring(offset));
		}
		if (LOG.isDebugEnabled())
			LOG.debug("---BENCHMARK--- getStatus end");
		return status;
    }

   
    public Long getFileCount() {
		return fileCount;
	}

	public void setFileCount(Long fileCount) {
		this.fileCount = fileCount;
	}

	public void addFileCount(Long fileCount) {
		this.fileCount += fileCount;
	}


	public Long getZipFileCount() {
		return zipFileCount;
	}

	public void setZipFileCount(Long zipFileCount) {
		this.zipFileCount = zipFileCount;
	}

	public void addZipFileCount(Long zipFileCount) {
		this.zipFileCount += zipFileCount;
		if (LOG.isDebugEnabled())
			LOG.debug("Added: "+zipFileCount + " total: "+this.zipFileCount);
	}

    private class UpdateInfo {
		private Repository repository;
		private ObjectInserter inserter; 
		private DirCacheBuilder builder;
		private DirCache dirCache;
		private Collection<TreeFilter> filters = new ArrayList<TreeFilter>();
		
		public UpdateInfo(Repository repository) {
			this.repository = repository;
		}

		public void init() throws NoWorkTreeException, CorruptObjectException, IOException{
			inserter = repository.newObjectInserter();
			dirCache = repository.lockDirCache();
			builder = dirCache.builder();
		}

		public void addTreeFilter(TreeFilter filter){
			filters.add(filter);
		}

		private TreeFilter getFilter(){
			if (filters.size() <= 0)
				return null;
			
			if (filters.size() == 1){
				return filters.iterator().next();
			}
			else {
				TreeFilter [] filterArr = new TreeFilter[filters.size()];
				int i = 0;
				for (TreeFilter f: filters){
					filterArr[i]=f;
					i++;
				}
				return AndTreeFilter.create(filterArr);
			}
		}
		
		public void commit() throws IOException{
			final TreeWalk tw = new TreeWalk(repository);
			//tw.reset();
			tw.addTree(new DirCacheBuildIterator(builder));
			tw.setRecursive(true);
			TreeFilter tf = getFilter();
			if (tf != null)
				tw.setFilter(tf);
			while (tw.next()) { // this is necessary for the cache to
								// actually register the change correctly
			}
			inserter.flush();
			builder.commit(); 
		}
		
		public void release(){
			inserter.close();
			if (dirCache != null)
				dirCache.unlock();
		}
		
		public DirCacheBuilder getBuilder(){
			return builder;
		}
		
		public ObjectInserter getInserter(){
			return inserter;
		}

		public DirCache getDirCache() {
			return dirCache;
		}
	}
	
	public void onDestroy(){
		if (mapPersisterThread != null)
			mapPersisterThread.interrupt();
	}
}
