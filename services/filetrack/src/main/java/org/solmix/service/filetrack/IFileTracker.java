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

	Collection<String> addTrackedDirs(String basePath, Collection<TrackerInfo> trackerInfos, ChangeListener listener);
}
