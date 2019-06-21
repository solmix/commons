package org.solmix.service.versioncontrol.support;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapPersister  implements Runnable {

    private static Logger LOG = LoggerFactory.getLogger(MapPersister.class);
    
	private Map<String, PairLong> map = new TreeMap<String, PairLong>();
	private String file;
	private static final String FILE_NAME = "mapfile";
	private static final long SLEEP_INERVAL = 1000*60*5;
	
	MapPersister(Map<String, PairLong> map, String directory){
		this.map = map;
		this.file = directory+File.separator+FILE_NAME;
	}

	public void init(boolean eraseExisting){
		loadMap(eraseExisting);
	}
	
	public void run() {
		while (true){
			try {
				Thread.sleep(SLEEP_INERVAL);
				saveMap();
			} catch (InterruptedException e) {
				saveMap();
				return;
			}
		}

	}

	private void saveMap() {
		FileOutputStream fos = null;
		ObjectOutputStream out = null;
		try {
			if (LOG.isDebugEnabled())
				LOG.debug("persisting map start");
			fos = new FileOutputStream(file);
			out = new ObjectOutputStream(fos);
			//Map<String, PairLong> mapCopy = new TreeMap<String, PairLong>(map);
			out.writeObject(map);
			out.close();
			if (LOG.isDebugEnabled())
				LOG.debug("persisting map end");
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private void loadMap(boolean eraseExisting) {
		File f = new File(file);
		if (eraseExisting && f.exists())
			f.delete();
		if (!f.exists()){
			Utils.recursiveCreateDir(f.getParent());
			try {
				f.createNewFile();
			} catch (IOException e) {
				LOG.error(e.getMessage() + " " + f.getPath(), e);
			}
			return;
		}
			
		if (f.length() <= 0)
			return;
		FileInputStream fis = null;
		ObjectInputStream in = null;
		try {
			fis = new FileInputStream(file);
			in = new ObjectInputStream(fis);
			Map<String, PairLong> loadedMap = (Map<String, PairLong>) in.readObject();
			map.clear();
			map.putAll(loadedMap);
			in.close();
		} catch (IOException e) {
			LOG.error(e.getMessage(), e);
		} catch (ClassNotFoundException e) {
			LOG.error(e.getMessage(), e);
		}		
	}

}
