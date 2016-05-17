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

package org.solmix.karaf.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;

/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年7月10日
 */
@Command(scope = "download", name = "mvn", description = "downlaod mvn resource ")
@Service
public class UrlDownloadCommand extends AbstractDownloadCommand implements Action
{

    @Argument(index = 0, name = "url", description = "resource URL.", required = true, multiValued = false)
    String url;


    @Override
    public Object execute() throws Exception {
        String fileName = getDownLoadedFile(url);
        downLoadFile(url, fileName);
        return null;
    }

}
