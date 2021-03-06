/*
 * Copyright 2013 The Solmix Project
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.gnu.org/licenses/ 
 * or see the FSF site: http://www.fsf.org. 
 */
package org.solmix.commons.util;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2014年11月14日
 */

public class FilesTest extends Assert{

	@Test
	public void getFileSize() throws IOException {
		URL url = FilesTest.class.getResource("./bean/Bean1.class");
		File f = new File(url.getFile());
		if (f.exists()) {
			String s = FileUtils.getFileSize(f);
			assertNotNull(s);
		}
	}
	
	@Test
	public void getFiles() throws IOException {
		List<String> a = new ArrayList<String>();
		a.add("aaa");
		System.out.println(StringUtils.arrayToString(a.toArray(new String[0])));
	}
}
