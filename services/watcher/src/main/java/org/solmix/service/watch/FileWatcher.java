package org.solmix.service.watch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileWatcher implements IFileWatcher
{
    private static Logger LOG = LoggerFactory.getLogger(FileWatcher.class);
    private static FileWatcher instance = new FileWatcher();
    
    public static FileWatcher getInstance() {
        return instance;
    }
    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stop() {
        // TODO Auto-generated method stub

    }

    @Override
    public void addListener(ChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public void removeListener(ChangeListener listener) {
        // TODO Auto-generated method stub

    }

    @Override
    public WatcherStatus getStatus() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setMaxDiffSize(Long sizeInKb) {
        // TODO Auto-generated method stub

    }

}
