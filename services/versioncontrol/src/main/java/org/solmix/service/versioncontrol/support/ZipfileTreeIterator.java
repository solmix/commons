package org.solmix.service.versioncontrol.support;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.WorkingTreeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZipfileTreeIterator extends ExtendedTreeIterator {
	static Logger log = LoggerFactory.getLogger(ZipfileTreeIterator.class);
	protected ZipFile zipFile = null;
	protected ZipFileEntry currentEntry;
	private Enumeration<? extends ZipEntry> zipEntries; 
	protected ZipFileEntry[] entries;
	protected static Set<String> extensionsForMd5 = null;	
	private Set<String> includedExtensions = null;

	public ZipFile getZipFile(){
		return zipFile;
	}

	/**
	 * Create a new iterator to traverse the work tree and its children.
	 *
	 * @param repo
	 *            the repository whose working tree will be scanned.
	 */
	public ZipfileTreeIterator(ZipfileTreeIterator p, ZipFileEntry[] entries, ZipFile zf, Set<String> filterExt) {
		super(p);
		this.zipFile = zf;
		this.entries = entries;
		this.includedExtensions = filterExt;
		init (entries);
	}
	
	public void closeStream() throws IOException{
		if (zipFile != null)
			zipFile.close();
	}
	
	/**
	 * Create a new iterator to traverse the work tree and its children.
	 *
	 * @param repo
	 *            the repository whose working tree will be scanned.
	 */
	public ZipfileTreeIterator(Repository repo, String path) {
		super("", repo.getConfig().get(WorkingTreeOptions.KEY));
		initBase(path);
		initRootIterator(repo);
	}

	private void initBase(String path) {
		File file = new File(path);
		if (!file.exists())
			return;
		Map<String, ZipFileEntry> dirsMap = new HashMap<String, ZipFileEntry>();
		
		try {
			zipFile = new ZipFile(file);
			zipEntries = zipFile.entries();
			Collection<ZipFileEntry> entCol = new ArrayList<ZipFileEntry>();
			while (zipEntries.hasMoreElements()){
				ZipEntry zEntry = zipEntries.nextElement();
				String zipName = zEntry.getName();
				if (!passedFilter(zipName)){
					if (log.isDebugEnabled())
						log.debug("Entry filtered out: "+zipName);
					continue;
				}
				ZipFileEntry entry = new ZipFileEntry(zEntry, zipFile, isBinary(zipName));
				addFileCount();
				File f = new File(zipName);
				String parent = f.getParent();
				if (zEntry.isDirectory()){
					if (dirsMap.get(zEntry.getName()) != null)
						continue;
					dirsMap.put(zEntry.getName(), entry);
				}
				if (parent == null || parent.length() <=0)
					entCol.add(entry);
				else {
					ZipFileEntry containingDir = dirsMap.get(parent);
					if (containingDir != null)
						containingDir.addSubentry(entry);
					else{
						String [] pathParts = zEntry.getName().split("/");
						Collection<ZipFileEntry> currLevelCol = entCol;
						ZipFileEntry lastLevel = null;
						String currPath = null;
						for (String s:pathParts){
							currPath = currPath == null ? s : currPath+s;
							if (currPath.equals(zEntry.getName())){
								lastLevel.addSubentry(entry);
								break;
							}
							currPath += "/";
								
							ZipFileEntry found = null;
							if (currLevelCol != null){
								for (ZipFileEntry ent: currLevelCol){
									if (ent.getName().equals(s)){
										found = ent;
										break;
									}
								}	
							}
							if (found != null){
								currLevelCol = found.getEntryCol();
								lastLevel = found;
								found = null;
							} else{
								ZipFileEntry newDir = new ZipFileEntry(currPath);
								if (lastLevel == null)
									entCol.add(newDir);
								else
									lastLevel.addSubentry(newDir);
								dirsMap.put(currPath, newDir);
								lastLevel = newDir;
							}
						}
					}
				}
			}
			
			
			File currFile = file;
			ZipFileEntry curr = null;
			while (currFile != null && !currFile.getPath().equals(File.separator)){
				ZipFileEntry parent = new ZipFileEntry(currFile.getPath());
				if (curr == null)
					parent.subtreeEntries=entCol;
				else
					parent.addSubentry(curr);
				curr = parent;
				currFile = currFile.getParentFile();
			}
			
			init(new Entry[]{curr});
		} catch (ZipException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		}
	}
	
	@Override
	public AbstractTreeIterator createSubtreeIterator(ObjectReader reader)
			throws IncorrectObjectTypeException, IOException {
		ZipFileEntry [] subtreeEntries = ((ZipFileEntry)current()).getEntries();
		((ZipFileEntry)current()).clearSubEntries(); // memory cleanup
		return new ZipfileTreeIterator(this, subtreeEntries , zipFile, includedExtensions);
	}
	
	public Long zipFileCount = 0L;
	
	public Long getFileCount(){
		return zipFileCount;
	}
	
	public void addFileCount(){
		zipFileCount++;
	}

	/**
	 * Wrapper for a standard Java IO file
	 */
	public static class ZipFileEntry extends ExtendedFileEntry {

		private ZipEntry zipEntry;
		private ZipFile zipFile;
		private Collection<ZipFileEntry> subtreeEntries = new ArrayList<ZipFileEntry>();
		protected boolean closeStream = false;
		
		public ZipFileEntry(ZipEntry zipEntry, ZipFile zipFile, boolean isBinary){
			this.zipEntry = zipEntry;
			this.zipFile = zipFile;
			if (zipEntry.isDirectory())
				mode = FileMode.TREE;
			else{
				mode = FileMode.REGULAR_FILE;
//				addFileCount();
			}
			length = zipEntry.getSize();
			lastModified = zipEntry.getTime();
			String relPath = zipEntry.getName();
			File f = new File(relPath);
			name = f.getName();
			this.isBinary = isBinary;
			if (isBinary){
				getMd5();
				if (md5String != null && md5String.length() >0)
					length = md5String.length();
			}
		}

		/**
		 * Constructor for dummy folder Entry
		 * @param relPath
		 */
		public ZipFileEntry(String relPath){
			this.mode = FileMode.TREE;
			this.lastModified = -1;
			File f = new File(relPath);
			name = f.getName().length() == 0 ? Utils.getRoot(f.getPath()): f.getName();
			this.length = f.length();
			this.isBinary = false;
		}
										
		public void addSubentry(ZipFileEntry zipFileEntry) {
			subtreeEntries.add(zipFileEntry);
		}
		
		public ZipFileEntry[] getEntries(){
			return subtreeEntries.toArray(new ZipFileEntry[subtreeEntries.size()]);
		}

		public void clearSubEntries(){
			subtreeEntries.clear();
		}
		
		public Collection<ZipFileEntry> getEntryCol(){
			return subtreeEntries;
		}
		
		@Override
		public InputStream openInputStream() throws IOException {
			if (zipEntry == null)
				return null;
			InputStream in;
			if (md5String != null && md5String.length() > 0){
				closeStream = true;
				in = new ByteArrayInputStream(md5String.getBytes());
				return new InputStreamWrapper(in, false);
			}
			in = this.zipFile.getInputStream(zipEntry);
			return new InputStreamWrapper(in, true);
		}
	
	}

	public Set<String> getFilteredExtensions() {
		return includedExtensions;
	}

	public void setFilteredExtensions(Set<String> ext) {
		this.includedExtensions = ext;
	}

	public void setIncludedExtensions(String extString) {
		if (extString == null || extString.length() <= 0)
			return;
		
		String[] ext = extString.split(";");
		if (ext == null || ext.length <= 0)
			return;
		
		this.includedExtensions = new HashSet<String>();
		for (String s: ext)
			this.includedExtensions.add(s);
	}

	protected boolean passedFilter(String ext){
		return includedExtensions == null || includedExtensions.size() <=0 || includedExtensions.contains(ext);
	}
}

