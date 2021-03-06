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
package org.solmix.runtime.helper;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.solmix.commons.util.StringUtils;
import org.solmix.commons.util.SystemPropertyAction;


/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$  2015年8月6日
 */

public class ClasspathScanner
{
    public static final String ALL_FILES = "**/*";
    public static final String ALL_CLASS_FILES = ALL_FILES + ".class";
    public static final String WILDCARD = "*";
    public static final String CLASSPATH_URL_SCHEME = "classpath:";
    
    static final ClasspathScanner HELPER;
    static {
        HELPER = getClasspathScanner();
    }
    
    // Default packages list to ignore during classpath scanning 
    static final String[] PACKAGES_TO_SKIP = {"org.solmix.runtime"}; 

    
    protected ClasspathScanner() {
    }    

    private static ClasspathScanner getClasspathScanner() { 
        boolean useSpring = true;
        String s = SystemPropertyAction.getPropertyOrNull("org.solmix.runtime.springClassPathScanner");
        if (!StringUtils.isEmpty(s)) {
            useSpring = "1".equals(s) || Boolean.parseBoolean(s);
        }
        if (useSpring) {
            try {
                return new SpringClasspathScanner();
            } catch (Throwable ex) {
                // ignore
            }
        }
        return new ClasspathScanner();
    }
    
    /**
     * Scans list of base packages for all classes marked with specific annotations. 
     * @param basePackage base package 
     * @param annotations annotations to discover
     * @return all discovered classes grouped by annotations they belong too 
     * @throws IOException class metadata is not readable 
     * @throws ClassNotFoundException class not found
     */
    @SafeVarargs
    public static Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClasses(
        String basePackage, Class< ? extends Annotation > ... annotations) 
        throws IOException, ClassNotFoundException {
        return findClasses(Collections.singletonList(basePackage), 
                           Collections.unmodifiableList(Arrays.asList(annotations)));
    }
    
    /**
     * Scans list of base packages for all classes marked with specific annotations. 
     * @param basePackages list of base packages 
     * @param annotations annotations to discover
     * @return all discovered classes grouped by annotations they belong too 
     * @throws IOException class metadata is not readable 
     * @throws ClassNotFoundException class not found
     */
    @SafeVarargs
    public static Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClasses(
        Collection< String > basePackages, Class< ? extends Annotation > ... annotations) 
        throws IOException, ClassNotFoundException {
        return findClasses(basePackages, Collections.unmodifiableList(Arrays.asList(annotations)));
    }
    
    /**
     * Scans list of base packages for all classes marked with specific annotations. 
     * @param basePackages list of base packages 
     * @param annotations annotations to discover
     * @return all discovered classes grouped by annotations they belong too 
     * @throws IOException class metadata is not readable 
     * @throws ClassNotFoundException class not found
     */
    public static Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClasses(
        Collection< String > basePackages, List<Class< ? extends Annotation > > annotations) 
        throws IOException, ClassNotFoundException {
        return findClasses(basePackages, annotations, null);
        
    }

    public static Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClasses(
        Collection< String > basePackages, 
        List<Class< ? extends Annotation > > annotations,
        ClassLoader loader) throws IOException, ClassNotFoundException {
        return HELPER.findClassesInternal(basePackages, annotations, loader);
    }
    
    protected Map< Class< ? extends Annotation >, Collection< Class< ? > > > findClassesInternal(
        Collection< String > basePackages, 
        List<Class< ? extends Annotation > > annotations,
        ClassLoader loader) 
        throws IOException, ClassNotFoundException {
        return Collections.emptyMap();
    }
    
    /**
     * Scans list of base packages for all resources with the given extension. 
     * @param basePackage base package 
     * @param extension the extension matching resources needs to have
     * @return list of all discovered resource URLs 
     * @throws IOException resource is not accessible
     */
    public static List<URL> findResources(String basePackage, String extension) 
        throws IOException {
        return findResources(basePackage, extension, null);
    }
    
    /**
     * Scans list of base packages for all resources with the given extension. 
     * @param basePackage base package 
     * @param extension the extension matching resources needs to have
     * @return list of all discovered resource URLs 
     * @throws IOException resource is not accessible
     */
    public static List<URL> findResources(String basePackage, String extension, ClassLoader loader) 
        throws IOException {
        return findResources(Collections.singletonList(basePackage), extension, loader);
    }
    
    /**
     * Scans list of base packages for all resources with the given extension. 
     * @param basePackages list of base packages 
     * @param extension the extension matching resources needs to have
     * @return list of all discovered resource URLs 
     * @throws IOException resource is not accessible
     */
    public static List<URL> findResources(Collection<String> basePackages, String extension) 
        throws IOException {
        return findResources(basePackages, extension, null);
    }
    
    public static List<URL> findResources(Collection<String> basePackages, String extension,
                                          ClassLoader loader) 
        throws IOException {
        return HELPER.findResourcesInternal(basePackages, extension, loader);
    }
    
    public static Set<String> parsePackages(final String packagesAsCsv) {        
        final String[] values = StringUtils.split(packagesAsCsv, ",");
        final Set<String> basePackages = new HashSet<String>(values.length);
        for (final String value : values) {
            final String trimmed = value.trim();
            if (trimmed.equals(WILDCARD)) {
                basePackages.clear();
                basePackages.add(trimmed);
                break;
            } else if (trimmed.length() > 0) {
                basePackages.add(trimmed);
            }
        }
        
        return basePackages;
    }
    
    protected List<URL> findResourcesInternal(Collection<String> basePackages, 
                                              String extension,
                                              ClassLoader loader) 
        throws IOException {
        return Collections.emptyList();
    }
    
}
