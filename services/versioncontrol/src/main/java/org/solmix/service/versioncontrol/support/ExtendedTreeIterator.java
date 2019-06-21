package org.solmix.service.versioncontrol.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator.FileEntry;
import org.eclipse.jgit.treewalk.WorkingTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtendedTreeIterator extends WorkingTreeIterator {
	private static Logger logger = LoggerFactory.getLogger(ExtendedTreeIterator.class);

	/**
	 * the starting directory. This directory should correspond to the root of
	 * the repository.
	 */
	protected File directory;

	/**
	 * the file system abstraction which will be necessary to perform certain
	 * file system operations.
	 */
	protected FS fs;
	
	protected Set<String> extensionsForMd5 = null;

	public ExtendedTreeIterator(Repository repo, String root) {
		super("", 
				repo.getConfig().get(WorkingTreeOptions.KEY));

		directory = new File(root+File.separator);
		this.fs = repo.getFS();
		Entry e = new ExtendedFileEntry(new File(root), fs, false);
		init(new Entry[]{e});
		//init(entries());
		initRootIterator(repo);
	}

	protected ExtendedTreeIterator(final ExtendedTreeIterator p, final File root, FS fs) {
		super(p);
		directory = root;
		this.fs = fs;
		init(entries());
	}
	
	/**
	 * Following two constructors are just passthough for the ZipfileTreeIterator
	 */
	protected ExtendedTreeIterator(final WorkingTreeIterator p) {
		super(p);
	}
	
	protected ExtendedTreeIterator(final String prefix,
			WorkingTreeOptions options) {
		super(prefix, options);
	}
	
	@Override
	public AbstractTreeIterator createSubtreeIterator(final ObjectReader reader)
			throws IncorrectObjectTypeException, IOException {
		final Entry e = current();
/*		if (e instanceof ZipFileEntry){
			return new ZipfileTreeIterator(this, ((ZipFileEntry) e).getFile().getPath());
		}
*/		return new ExtendedTreeIterator(this, ((ExtendedFileEntry) e).getFile(), fs);
	}

	private Entry[] entries() {
		String s = directory.getPath();
		final File[] all;
		if (s.length() ==2 && s.charAt(1)==':')
			all = (new File(s+File.separator)).listFiles();
		else
			all = directory.listFiles();
		if (all == null)
			return EOF;
		final Entry[] r = new Entry[all.length];
		for (int i = 0; i < r.length; i++)
			r[i] = new ExtendedFileEntry(all[i], fs, isBinary(all[i].getName()));
		return r;
	}

	public Set<String> getExtensionsForMd5() {
		return extensionsForMd5;
	}

	public void setExtensionsForMd5(Set<String> extensions) {
		extensionsForMd5 = extensions;
	}
	
	protected boolean isBinary(String path) {
		if (extensionsForMd5 == null || extensionsForMd5.size() <=0 || path == null || path.length() <= 0)
			return false;
		if (extensionsForMd5.contains(Utils.getFileExtension(Utils.fileName(path))))
			return true;
		return false;
	}
	
	/**
	 * Wrapper for a standard Java IO file
	 */
	static public class ExtendedFileEntry extends Entry {
		protected File file;

		protected FileMode mode;

		protected long length = -1;

		protected long lastModified;
		
		protected String md5String;
		
		protected boolean isBinary;
		
		protected String name;

		public ExtendedFileEntry(final File f, FS fs, boolean isBinary) {
			file = f;
			this.isBinary = isBinary;
			
			if (f.isDirectory()) {
/*				if (new File(f, Constants.DOT_GIT).isDirectory())
					mode = FileMode.GITLINK;
				else
*/				mode = FileMode.TREE;
			} else if (fs.canExecute(file))
				mode = FileMode.EXECUTABLE_FILE;
			else
				mode = FileMode.REGULAR_FILE;
			
			length = file.length();
			
			name = file.getName().length() == 0 ? Utils.getRoot(file.getPath()): file.getName();
			
			if (isBinary){
				getMd5();
				if (md5String != null && md5String.length() >0)
					length = md5String.length();
			}
				
		}
		
		public ExtendedFileEntry(){};
		
		@Override
		public FileMode getMode() {
			return mode;
		}

		@Override
		public String getName() {
			return name;
		}

		@Override
		public long getLength() {
			return length;
		}

		protected void getMd5() {
			try{
				InputStream is = openInputStream();
				md5String = Utils.getMd5Stream(is);
				is.close();
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
				md5String = "";
			}
		}
		
		@Override
		public long getLastModified() {
			if (lastModified == 0)
				lastModified = file.lastModified();
			return lastModified;
		}

		@Override
		public InputStream openInputStream() throws IOException {
			if (md5String != null && md5String.length() > 0)
				return new ByteArrayInputStream(md5String.getBytes());
			return new FileInputStream(file);
		}

		/**
		 * Get the underlying file of this entry.
		 *
		 * @return the underlying file of this entry
		 */
		public File getFile() {
			return file;
		}
	}

	/**
	 * @return The root directory of this iterator
	 */
	public File getDirectory() {
		return directory;
	}

	/**
	 * @return The location of the working file. This is the same as {@code new
	 *         File(getDirectory(), getEntryPath())} but may be faster by
	 *         reusing an internal File instance.
	 */
	public File getEntryFile() {
		return ((FileEntry) current()).getFile();
	}


	
/*	@Override
	protected void init(final Entry[] list) {
		for (int i = 0; i < list.length; i++) {
			final Entry e = list[i];
			if (e == null)
				continue;
			final String name = e.getName();
			if (ArchiveUtils.isZipArchive(name)){
				list[i] = new ZipfileTreeIterator.ZipFileEntry(name);
			}
		}
		super.init(list);
	}*/
}
