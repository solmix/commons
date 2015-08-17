/**
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

package org.solmix.exchange;

import org.solmix.exchange.interceptor.InterceptorProvider;
import org.solmix.exchange.model.ProtocolInfo;

/**
 * 对协议的绑定，主要功能：
 * <li>提供协议处理的切面
 * <li>提供消息创建逻辑
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年10月15日
 */

public interface Protocol extends InterceptorProvider {

    Message createMessage();

    Message createMessage(Message m);

    ProtocolInfo getProtocolInfo();
}
