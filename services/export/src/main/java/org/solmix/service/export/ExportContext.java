/*
 * Copyright 2014 The Solmix Project
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
package org.solmix.service.export;

import java.util.Set;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年7月28日
 */

public interface ExportContext
{
    /** 添加一个值。 */
    void put(String key, Object value);

    /** 取得指定值。 */
    Object get(String key);

    /** 删除一个值。 */
    void remove(String key);

    /** 判断是否包含指定的键。 */
    boolean containsKey(String key);

    /** 取得所有key的集合。 */
    Set<String> keySet();
}
