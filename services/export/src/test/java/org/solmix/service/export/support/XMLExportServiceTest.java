/*
 * Copyright 2015 The Solmix Project
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
package org.solmix.service.export.support;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.solmix.service.export.ExportService;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年9月9日
 */

public class XMLExportServiceTest
{
    @Test
    public void resultTest() throws IOException{
        File f = new File("target/xmlTest.xml");
        OutputStream outs = new FileOutputStream(f);
        XMLExportService export = new XMLExportService();
        MappedExportContext context= new MappedExportContext();
        context.put(ExportService.XML_TAG_NAME, "对象标示");
        export.setContext(context);
        Map<Object,Object > map = new HashMap<Object,Object>();
        map.put("一个标示", "一个值");
        List< Map<Object,Object >> l = new ArrayList< Map<Object,Object >>();
        l.add(map);
        export.exportResultSet(l, outs);
        outs.flush();
        Assert.assertNotSame(0, f.length());
        if(f.exists())
            f.delete();
    }
}
