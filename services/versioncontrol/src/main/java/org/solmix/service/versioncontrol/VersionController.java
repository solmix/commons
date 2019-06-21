package org.solmix.service.versioncontrol;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

public interface VersionController
{
    /**
     * 创建git仓库
     * 
     * @param gitDir
     * @return
     */
    boolean createRepository(final String gitDir);
    
    /**
     * 重命名
     * 
     * @param gitDir
     * @param oldPath
     * @param newPath
     * @param message
     * @return
     */
    String[] doRename(final String gitDir, final String oldPath, final String newPath, final String message);
    
    String addChange(final String gitDir, final String path, String commitMessage);
    
    String addChange(final String gitDir, final String path, String oldPath, String commitMessage, boolean performCommit);
    
    String addChange(final String gitDir, final Collection<String> paths,    String commitMessage, boolean performCommit);
    
    String commit(final String gitDir, String commitMessage);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, final String gitDir, final boolean changesOnly);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, final String gitDir, final boolean changesOnly, int pageSize);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, final String gitDir, final boolean changesOnly, String revision, int pageSize);     
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate, Long endDate);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate,   Long endDate, int pageSize);
    
    public List<Revision> getRevisionsForPath(final Collection<String> filepaths, String gitDir, boolean changesOnly, Long startDate,   Long endDate, String revision, int pageSize);
    
    public FileDiffer[] compareRevisionToHead(final String revId, final String gitDir, final String filepath);
   
    public byte[] getFileByRevision(final String gitDir, final String revId);
    
    public String getDiff(final String gitDir, final RevisionDifferItem revItem1, final RevisionDifferItem revItem2);
    
    public String getLastChangeDiff(String gitDir, RevisionDifferItem revItem);
    
    /**
     * 检查文件下是否已经存在git库
     * @param gitDirBase
     * @return
     */
    public boolean gitDirExists(String gitDirBase);
    
    public Collection<RevisionDifferItem> getChangeInfo(final String revId, final String gitDir,  final Collection<String> filepaths) throws IOException;
    
    public String getStatus(String dirPath, Collection<RevisionDifferItem> items);
    
    public void setExtMonitoredInArchive(String filteredExt);

}
