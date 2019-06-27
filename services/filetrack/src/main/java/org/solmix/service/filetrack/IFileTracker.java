package org.solmix.service.filetrack;

import java.util.Collection;

public interface IFileTracker
{

    void start();
    
    void stop();
    
    void addListener(ChangeListener listener);
    
    
    void removeListener(ChangeListener listener);
    
    TrackerStatus getStatus();
    
    void setMaxDiffSize(final Long sizeInKb);

	void setAppDataDir(String path);

	void removeTrackedDirs(Collection<String> monitoredPaths);

	/**
	 * 添加跟踪目录
	 * <li>目录为null或者空，目录不是绝对路径，目录不是以/或者～/开头，目录不是盘符(:)开头，则添加basePath
	 * @param basePath
	 * @param trackerInfos
	 * @param listener
	 * @return
	 */
	Collection<String> addTrackedDirs(String basePath, Collection<TrackerInfo> trackerInfos, ChangeListener listener);
}
