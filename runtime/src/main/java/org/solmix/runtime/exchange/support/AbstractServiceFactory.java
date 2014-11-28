/**
 * Copyright (c) 2014 The Solmix Project
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

package org.solmix.runtime.exchange.support;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

import org.solmix.runtime.Container;
import org.solmix.runtime.bean.ConfiguredBeanProvider;
import org.solmix.runtime.exchange.Service;
import org.solmix.runtime.exchange.ServiceFactoryListener;
import org.solmix.runtime.exchange.event.ServiceFactoryEvent;
import org.solmix.runtime.exchange.serialize.Serialization;

/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2014年11月21日
 */

public abstract class AbstractServiceFactory {

    // private static final Logger LOG =
    // LoggerFactory.getLogger(AbstractServiceFactory.class);

    private Container container;

    private Service service;

    private Serialization serialization;

    protected boolean serializeSetted;

    private final CopyOnWriteArrayList<ServiceFactoryListener> listeners 
                      = new CopyOnWriteArrayList<ServiceFactoryListener>();

    public abstract Service create();

    /**   */
    public Container getContainer() {
        return container;
    }

    public void pulishEvent(ServiceFactoryEvent event) {
        for (ServiceFactoryListener listener : listeners) {
            listener.onHandle(event);
        }
    }
    
    public void pulishEvent(int type ,Object ... args ) {
        ServiceFactoryEvent event = new ServiceFactoryEvent(type,this,args);
        for (ServiceFactoryListener listener : listeners) {
            listener.onHandle(event);
        }
    }

    /**   */
    public void setContainer(Container container) {
        this.container = container;
        if (container == null) {
            return;
        }
        ConfiguredBeanProvider cp = container.getExtension(ConfiguredBeanProvider.class);
        if (cp != null) {
            Collection<? extends ServiceFactoryListener> ls = cp.getBeansOfType(ServiceFactoryListener.class);
            for (ServiceFactoryListener l : ls) {
                listeners.addIfAbsent(l);
            }
        }
    }

    /**   */
    public Serialization getSerialization() {
        return getSerialization(true);

    }

    /**   */
    public Serialization getSerialization(boolean create) {
        if (serialization == null && create) {
            serialization = defaultSerialization();
        }
        return serialization;
    }

    /**
     * @return
     */
    protected Serialization defaultSerialization() {
        return null;
    }

    /**   */
    public void setSerialization(Serialization serialization) {
        this.serialization = serialization;
        serializeSetted = serialization != null;
    }

    public Service getService() {
        return service;
    }

    protected void setService(Service service) {
        this.service = service;
    }
}