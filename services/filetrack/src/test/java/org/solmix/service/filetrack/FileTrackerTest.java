package org.solmix.service.filetrack;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.commons.util.FileUtils;
import org.solmix.service.versioncontrol.support.GitVersionController;
import org.solmix.test.TestUtils;

public class FileTrackerTest {
	private static Logger LOG = LoggerFactory.getLogger(FileTrackerTest.class);
	private static final String TEST_FILE="testfile.for.git.txt";
	private static GitVersionController gvc;
	private static FileTracker tracker;
	private static String gitBase;
	@BeforeClass
	public static void setUpBeforeClass()  {
		TestUtils.load();
		gitBase = TestUtils.destdir().getAbsolutePath();
		tracker = FileTracker.getInstance();
		FileUtils.delFolder(gitBase);
		tracker.setAppDataDir(gitBase);
		tracker.start();
	}
	
	@AfterClass
	public static void tearDown() {
		if(tracker!=null) {
			tracker.stop();
		}
		String base = TestUtils.basedir().getAbsolutePath();
		String cfile=base+File.separator+TEST_FILE;
		File f = new File(cfile);
		if(f.exists()) {
			f.delete();
		}
	}
	@Test
	public void skip() {
		
	}
	@Test
	public void test() throws IOException, InterruptedException {
		
		String base = TestUtils.basedir().getAbsolutePath();
		String cfile=base+File.separator+TEST_FILE;
		FileUtils.createNewFile(cfile, "#file content");
		
		TrackerInfo i = new TrackerInfo();
		//监控的是目录，而不是文件
		i.setPath(base);
		i.setFilter(".*\\.txt");
		final CountDownLatch latch = new CountDownLatch(1);
		tracker.addTrackedDirs(gitBase, Collections.singletonList(i), new ChangeListener() {
			
			@Override
			public void onChange(ChangeEvent event) {
				latch.countDown();
				LOG.debug(event.getDiff());
			}
		});
		Thread.sleep(2000);
		FileWriter writer = new FileWriter(cfile);
		try {
			IOUtils.write("asdas", writer);
			writer.flush();
		} finally {
			IOUtils.closeQuietly(writer);
		}
		try {
			latch.await();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
}
