package org.solmix.service.watch;


public interface IFileWatcher
{

    void start();
    
    void stop();
    
    void addListener(ChangeListener listener);
    
    
    void removeListener(ChangeListener listener);
    
    WatcherStatus getStatus();
    
    void setMaxDiffSize(final Long sizeInKb);
}
