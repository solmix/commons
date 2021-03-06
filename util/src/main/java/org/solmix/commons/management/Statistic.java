/*
 * Copyright 2012 The Solmix Project
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

package org.solmix.commons.management;

/**
 * 
 * @author Administrator
 * @version 110035 2011-8-15
 */

public interface Statistic
{

    public enum UpdateMode
    {
        VALUE , DIFFERENCE , COUNTER , MAXIMUM , MINIMUM
    }

    void increment();

    void updateValue(long value);

    long getValue();

    long getUpdateCount();

    void reset();
}
