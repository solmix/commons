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
package org.solmix.karaf.command;

import java.util.Map;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.support.table.ShellTable;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.solmix.runtime.extension.ExtensionInfo;
import org.solmix.runtime.extension.ExtensionRegistry;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年10月19日
 */
@Command(scope = "rt", name = "list-ext", description = "Displays a list of all loaded extensions.")
@Service
public class ExtensionListCommand implements Action
{

    @Override
    public Object execute() throws Exception {
        Map<String, ExtensionInfo> registries=  ExtensionRegistry.getRegisteredExtensions();

        ShellTable table = new ShellTable();
        table.column("Bundle");
        table.column("Class");
        table.column("Interface");
        table.emptyTableText("no defiend interface");
        for(ExtensionInfo info :registries.values()){
            if(info!=null){
                Class<?> clazze=info.getClassObject();
                String bundleId ="N/A";
                if(clazze!=null){
                    Bundle bundle= FrameworkUtil.getBundle(clazze);
                    if(bundle!=null){
                        bundleId=bundle.getBundleId()+"";
                    }
                }
                table.addRow().addContent(bundleId,info.getClassname(),info.getInterfaceName());
            }
        }
        table.print(System.out, true);
        return null;
    }

}
