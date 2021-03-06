/*
 *  Copyright 2012 The Solmix Project
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

/**
 * 
 * @author solmix.f@gmail.com
 * @version $Id$ 2013-11-3
 */

public class ClassLoaderUtils {

	public static class ClassLoaderHolder {
		ClassLoader loader;

		ClassLoaderHolder(ClassLoader c) {
			loader = c;
		}

		public void reset() {
			ClassLoaderUtils.setThreadContextClassloader(loader);
		}
	}

	public static ClassLoaderHolder setThreadContextClassloader(final ClassLoader newLoader) {
		return AccessController.doPrivileged(new PrivilegedAction<ClassLoaderHolder>() {
			@Override
			public ClassLoaderHolder run() {
				ClassLoader l = Thread.currentThread().getContextClassLoader();
				Thread.currentThread().setContextClassLoader(newLoader);
				return new ClassLoaderHolder(l);
			}
		});
	}

	public static URL getResource(ClassLoader loader, String resourceName) {
		return getResource(loader, resourceName);
	}

	public static URL getResource(ClassLoader loader, String resourceName, Class<?> callingClass) {
		URL url = null;
		if (loader != null) {
			url = loader.getResource(resourceName);
			if (url != null)
				return url;
			else
				return getResource(resourceName, callingClass);
		} else {
			return getResource(resourceName, callingClass);
		}
	}

	public static Enumeration<URL> getResources(ClassLoader loader, String resourceName) throws IOException {
		return getResources(loader, resourceName, null);
	}

	public static Enumeration<URL> getResources(ClassLoader loader, String resourceName, Class<?> callingClass)
			throws IOException {
		Enumeration<URL> url = null;
		if (loader != null) {
			url = loader.getResources(resourceName);
			if (url != null)
				return url;
			else
				return getResources(resourceName, callingClass);
		} else {
			return getResources(resourceName, callingClass);
		}
	}

	/**
	 * Load a given resource.
	 * <p/>
	 * This method will try to load the resource using the following methods (in
	 * order):
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>From ClassLoaderUtils.class.getClassLoader()
	 * <li>callingClass.getClassLoader()
	 * </ul>
	 * 
	 * @param resourceName The name of the resource to load
	 * @param callingClass The Class object of the calling object
	 */
	public static URL getResource(String resourceName, Class<?> callingClass) {
		URL url = Thread.currentThread().getContextClassLoader().getResource(resourceName);
		if (url == null && resourceName.startsWith("/")) {
			// certain classloaders need it without the leading /
			url = Thread.currentThread().getContextClassLoader().getResource(resourceName.substring(1));
		}

		ClassLoader cluClassloader = ClassLoaderUtils.class.getClassLoader();
		if (cluClassloader == null) {
			cluClassloader = ClassLoader.getSystemClassLoader();
		}
		if (url == null) {
			url = cluClassloader.getResource(resourceName);
		}
		if (url == null && resourceName.startsWith("/")) {
			// certain classloaders need it without the leading /
			url = cluClassloader.getResource(resourceName.substring(1));
		}

		if (url == null && callingClass != null) {
			ClassLoader cl = callingClass.getClassLoader();

			if (cl != null) {
				url = cl.getResource(resourceName);
			}
			if (url == null)
				url = callingClass.getResource(resourceName);
		}

		if ((url == null) && (resourceName != null) && (resourceName.charAt(0) != '/')) {
			return getResource('/' + resourceName, callingClass);
		}

		return url;
	}

	public static Enumeration<URL> getResources(String resourceName, Class<?> callingClass) throws IOException {
		Enumeration<URL> url = Thread.currentThread().getContextClassLoader().getResources(resourceName);
		if (url == null && resourceName.startsWith("/")) {
			// certain classloaders need it without the leading /
			url = Thread.currentThread().getContextClassLoader().getResources(resourceName.substring(1));
		}

		ClassLoader cluClassloader = ClassLoaderUtils.class.getClassLoader();
		if (cluClassloader == null) {
			cluClassloader = ClassLoader.getSystemClassLoader();
		}
		if (url == null) {
			url = cluClassloader.getResources(resourceName);
		}
		if (url == null && resourceName.startsWith("/")) {
			// certain classloaders need it without the leading /
			url = cluClassloader.getResources(resourceName.substring(1));
		}

		if (url == null && callingClass != null) {
			ClassLoader cl = callingClass.getClassLoader();

			if (cl != null) {
				url = cl.getResources(resourceName);
			}
		}

		if ((url == null) && (resourceName != null) && (resourceName.charAt(0) != '/')) {
			return getResources('/' + resourceName, callingClass);
		}

		return url;
	}

	/**
	 * Load a class with a given name.
	 * <p/>
	 * It will try to load the class in the following order:
	 * <ul>
	 * <li>From Thread.currentThread().getContextClassLoader()
	 * <li>Using the basic Class.forName()
	 * <li>From ClassLoaderUtils.class.getClassLoader()
	 * <li>From the callingClass.getClassLoader()
	 * </ul>
	 * 
	 * @param className    The name of the class to load
	 * @param callingClass The Class object of the calling object
	 * @throws ClassNotFoundException If the class cannot be found anywhere.
	 */
	public static Class<?> loadClass(String className, Class<?> callingClass) throws ClassNotFoundException {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			if (cl != null) {
				return cl.loadClass(className);
			}
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return loadClass2(className, callingClass);
	}

	public static Class<?> loadClass(String className, ClassLoader loader) throws ClassNotFoundException {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			if (cl != null) {
				return cl.loadClass(className);
			}
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return loadClass2(className, loader);
	}

	private static Class<?> loadClass2(String className, ClassLoader loader) throws ClassNotFoundException {
		try {
			return loader.loadClass(className);
		} catch (ClassNotFoundException ex) {
			try {
				return Class.forName(className);
			} catch (ClassNotFoundException e) {
				if (ClassLoaderUtils.class.getClassLoader() != null) {
					return ClassLoaderUtils.class.getClassLoader().loadClass(className);
				}
			}
			throw ex;
		}
	}

	public static <T> Class<? extends T> loadClass(String className, Class<?> callingClass, Class<T> type)
			throws ClassNotFoundException {
		try {
			ClassLoader cl = Thread.currentThread().getContextClassLoader();

			if (cl != null) {
				return cl.loadClass(className).asSubclass(type);
			}
		} catch (ClassNotFoundException e) {
			// ignore
		}
		return loadClass2(className, callingClass).asSubclass(type);
	}

	private static Class<?> loadClass2(String className, Class<?> callingClass) throws ClassNotFoundException {
		try {
			return Class.forName(className);
		} catch (ClassNotFoundException ex) {
			try {
				if (ClassLoaderUtils.class.getClassLoader() != null) {
					return ClassLoaderUtils.class.getClassLoader().loadClass(className);
				}
			} catch (ClassNotFoundException exc) {
				if (callingClass != null && callingClass.getClassLoader() != null) {
					return callingClass.getClassLoader().loadClass(className);
				}
			}
			throw ex;
		}
	}

	public static ClassLoader getDefaultClassLoader() {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = ClassLoaderUtils.class.getClassLoader();
			if (cl == null) {
				// getClassLoader() returning null indicates the bootstrap ClassLoader
				try {
					cl = ClassLoader.getSystemClassLoader();
				} catch (Throwable ex) {
					// Cannot access system ClassLoader - oh well, maybe the caller can live with
					// null...
				}
			}
		}
		return cl;
	}

	/**
	 * First form current thread context classLoader. Second form
	 * class.getClassLoader
	 * 
	 * @param class
	 * @return
	 */
	public static ClassLoader getClassLoader(Class<?> cls) {
		ClassLoader cl = null;
		try {
			cl = Thread.currentThread().getContextClassLoader();
		} catch (Throwable ex) {
			// Cannot access thread context ClassLoader - falling back to system class
			// loader...
		}
		if (cl == null) {
			// No thread context class loader -> use class loader of this class.
			cl = cls.getClassLoader();
		}
		return cl;
	}

	/**
	 * Return ClassLoader with custom path entry
	 * 
	 * @param entries
	 * @return
	 */
	public static ClassLoader getCustomClassloader(Collection<String> entries) {
		List<URL> urls = new ArrayList<URL>();
		File file;

		if (entries != null) {
			for (String classPathEntry : entries) {
				file = new File(classPathEntry);
				if (!file.exists()) {
					throw new RuntimeException("class path is not exist " + classPathEntry);
				}

				try {
					urls.add(file.toURI().toURL());
				} catch (MalformedURLException e) {
					throw new RuntimeException("class path is valid " + classPathEntry);
				}
			}
		}
		ClassLoader parent = getDefaultClassLoader();
		URLClassLoader ucl = new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
		return ucl;
	}
}
