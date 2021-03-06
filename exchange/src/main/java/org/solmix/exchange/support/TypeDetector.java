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

package org.solmix.exchange.support;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.solmix.runtime.Container;
import org.solmix.runtime.bean.ConfiguredBeanProvider;
import org.solmix.runtime.bean.ConfiguredBeanProvider.BeanLoaderListener;
import org.solmix.runtime.extension.DefaultExtensionLoader;

/**
 * 根据URI查找适配的传输层实现.
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2015年1月5日
 */

public class TypeDetector<T> {

    private final Map<String, T> map;

    private final Set<String> loaded;

    private final Class<T> cls;

    private final ConfiguredBeanProvider provider;

    public TypeDetector(Container container, Map<String, T> map,
        Set<String> loaded, Class<T> clz) {
        this.map = map;
        this.loaded = loaded;
        cls = clz;
        provider = container.getExtension(ConfiguredBeanProvider.class);
    }

    public T detectInstanceForType(String type) {
        if (provider == null) {
            return null;
        }
        T instance = loadConfiguredBean(type);
        return instance;
    }

   

    public T detectInstanceForURI(String uri) {
        if (provider == null) {
            return null;
        }
        T t = detectInstanceWithPrefix(uri);
        if (t == null) {
            loadAll();
            t = detectInstanceWithPrefix(uri);
        }
        return t;
    }

    private T detectInstanceWithPrefix(String uri) {
        for (T t : map.values()) {
            if (hasPrefix(uri, getPrefixes(t))) {
                return t;
            }
        }
        return null;
    }
   
    private boolean hasPrefix(String uri, Set<String> prefixes) {
        if (prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (uri.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    
    private Set<String> getPrefixes(T t) {
        Set<String> prefixes = null;
        if (t instanceof TypeDetectSupport) {
            TypeDetectSupport tds = (TypeDetectSupport) t;
            prefixes = tds.getUriPrefixes();
        }
        if (prefixes == null) {
            prefixes = Collections.emptySet();
        }
        return prefixes;
    }

    private void loadAll() {
        provider.loadBeansOfType(cls, new BeanLoaderListener<T>() {

            @Override
            public boolean loadBean(String name, Class<? extends T> type) {
                return !loaded.contains(name);
            }

            @Override
            public boolean beanLoaded(String name, T bean) {
                loaded.add(name);
                if (!map.containsKey(name)) {
                    registerBean(name, bean);
                } 
                return map.containsKey(name);
            }
        });
    }


    private T loadConfiguredBean(final String type) {
        provider.loadBeansOfType(cls, new BeanLoaderListener<T>() {

            @Override
            public boolean loadBean(String name, Class<? extends T> clazz) {
                return !loaded.contains(name)
                    && type.equals(DefaultExtensionLoader.extensionName(clazz));
            }

            @Override
            public boolean beanLoaded(String name, T bean) {
                loaded.add(name);
                if (!map.containsKey(type)) {
                    registerBean(type, bean);
                } 
                return map.containsKey(type);
            }
        });
        return map.get(type);
    }

    @SuppressWarnings("unchecked")
    private void registerBean(String name, T bean) {
        if (bean instanceof TypeDetectSupport) {
            TypeDetectSupport tds = (TypeDetectSupport) bean;
            if (tds.getTransportTypes() != null) {
                for (String tt : tds.getTransportTypes()) {
                    if (!map.containsKey(tt)) {
                        map.put(tt, bean);
                    }
                }
            }
        } else {
            Method m =null;
            try {
                 m = bean.getClass().getMethod("getSupportTypes",new Class[0]);
            } catch (Exception ex) {/**ignore*/}
            if (m != null) {
                
                Collection<String> types=null;
                try {
                    types = (Collection<String>) m.invoke(bean);
                } catch (Exception e) {/**ignore*/}
                if(types!=null){
                    for (String s : types) {
                        if (!map.containsKey(s)) {
                            map.put(s, bean);
                        }
                    }
                }
            } else {
                if (!map.containsKey(name)) {
                    map.put(name, bean);
                }
            }
        }
    }
}
