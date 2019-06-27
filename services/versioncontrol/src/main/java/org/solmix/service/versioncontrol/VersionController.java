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
    
    /**
     * 提交指定路径的文件到版本库中
     * 
     * @param gitDir 版本库根路径
     * @param path 文件路径
     * @param commitMessage 提交信息
     * @return
     */
    String addChange(final String gitDir, final String path, String commitMessage);
    
    /**
     * 提交指定路径的文件到版本库中
     * @param gitDir
     * @param path
     * @param oldPath
     * @param commitMessage
     * @param performCommit 是否提交
     * @return
     */
    String addChange(final String gitDir, final String path, String oldPath, String commitMessage, boolean performCommit);
    
    /**
     * 提交指定路径的文件到版本库中
     * @param gitDir
     * @param paths
     * @param commitMessage
     * @param performCommit 是否提交
     * @return
     */
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
    
    /**
     * 指定版本id下的修改信息
     * 
     * @param revId
     * @param gitDir
     * @param filepaths
     * @return
     * @throws IOException
     */
    public Collection<RevisionDifferItem> getChangeInfo(final String revId, final String gitDir,  final Collection<String> filepaths) throws IOException;
    
    /**
     * 获取某个目录下的版本控制状态
     * 
     * @param dirPath
     * @param items
     * @return
     */
    public String getStatus(String dirPath, Collection<RevisionDifferItem> items);
    
    /**
     * 对于在jar压缩文件中文件的监控，压缩文件中的文件名不包括在其中的，不版本控制
     * 
     * @param filteredExt 默认："properties;config;cfg;class;mf;list;jar;xml"
     */
    public void setExtMonitoredInArchive(String filteredExt);

}
