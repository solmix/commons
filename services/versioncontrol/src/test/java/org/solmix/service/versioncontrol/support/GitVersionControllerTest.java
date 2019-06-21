package org.solmix.service.versioncontrol.support;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.solmix.commons.util.FileUtils;
import org.solmix.service.versioncontrol.Revision;
import org.solmix.test.TestUtils;

public class GitVersionControllerTest {

	@BeforeClass
	public static void setUpBeforeClass()  {
		TestUtils.load();
//		System.out.println(TestUtils.basedir);
	}

	@Test
	public void test() throws IOException {
		GitVersionController gvc  = new GitVersionController();
		String gitBase = TestUtils.destdir().getAbsolutePath();
		FileUtils.delFolder(gitBase + File.separator + ".git");
		boolean success=gvc.createRepository(gitBase);
		Assert.assertTrue(success);
		
		String cdir =gitBase + File.separator + "renametest";
		Utils.recursiveCreateDir(cdir);
		String cfile=cdir+File.separator+"configfile";
		FileUtils.createNewFile(cfile, "#file content");
		gvc.addChange(gitBase, cfile, "add dir");
		List<Revision> revs=gvc.getRevisionsForPath(Collections.singletonList(cdir), gitBase, true);
		Assert.assertTrue(revs!=null);
	}

}
