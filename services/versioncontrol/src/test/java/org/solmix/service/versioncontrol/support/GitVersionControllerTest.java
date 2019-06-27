package org.solmix.service.versioncontrol.support;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.solmix.commons.util.FileUtils;
import org.solmix.service.versioncontrol.Revision;
import org.solmix.service.versioncontrol.RevisionDifferItem;
import org.solmix.test.TestUtils;

public class GitVersionControllerTest {

	private static final String TEST_FILE="testfile.for.git.txt";
	private static GitVersionController gvc;
	private static  String GIT_BASE="";
	@BeforeClass
	public static void setUpBeforeClass()  {
		TestUtils.load();
		gvc  = new GitVersionController();
		GIT_BASE = TestUtils.destdir().getAbsolutePath();
		FileUtils.delFolder(GIT_BASE );
		boolean success=gvc.createRepository(GIT_BASE);
		Assert.assertTrue(success);
		boolean exist=gvc.gitDirExists(GIT_BASE);
		Assert.assertTrue(exist);
	}
	@AfterClass
	public static void tearDown()  {
		String base = TestUtils.basedir().getAbsolutePath();
		String cfile=base+File.separator+TEST_FILE;
		File f = new File(cfile);
		if(f.exists()) {
			f.delete();
		}
	}

	@Test
	public void test() throws IOException, InterruptedException {
		String base = TestUtils.basedir().getAbsolutePath();
		String cfile=base+File.separator+TEST_FILE;
		FileUtils.createNewFile(cfile, "#file content");
		gvc.addChange(GIT_BASE, cfile, "add baseline");
		FileWriter writer = new FileWriter(cfile);
		try {
			IOUtils.write("#file content123", writer);
			writer.flush();
		} finally {
			IOUtils.closeQuietly(writer);
		}
		gvc.addChange(GIT_BASE, cfile, "modify file");
		File f = new File(cfile);
		f.delete();
		String revId=gvc.addChange(GIT_BASE, cfile, "delete file");
		List<Revision> revs=gvc.getRevisionsForPath(Collections.singletonList(cfile), GIT_BASE, true);
		Assert.assertTrue(revs.size()==3);
		
		Collection<RevisionDifferItem> differs=	gvc.getChangeInfo(revId,GIT_BASE,Collections.singletonList(cfile));
		Assert.assertTrue(differs.size()==1);
		
		String status=gvc.getStatus(base, differs);
		Assert.assertTrue(status.contains(TEST_FILE));
	}
	
	//支持jar解压后比较
	@Test
	public void testZip() throws IOException, InterruptedException {
		String base = TestUtils.basedir().getAbsolutePath();
		String cfile=base+File.separator+"/src/test/resources/for.git.zip.test.jar";
		gvc.addChange(GIT_BASE, cfile, "add baseline");
		
		List<Revision> revs=gvc.getRevisionsForPath(Collections.singletonList(cfile), GIT_BASE, true);
		Assert.assertTrue(revs.size()==1);
		
	}

}
