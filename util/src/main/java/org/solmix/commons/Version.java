/**
 * Copyright 2016 The Solmix Project
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
package org.solmix.commons;

import java.io.IOException;
import java.io.InputStream;
import java.security.CodeSource;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 版本信息工具类
 * 
 * @author solmix.f@gmail.com
 */
public final class Version {

	private static final Logger LOG = LoggerFactory.getLogger(Version.class);

	private Version() {
	}

	public static String readFromMaven(String groupId, String artifactId) {
		return readFromMaven(Version.class, groupId, artifactId);
	}

	/**
	 * 通过jar中打包的maven信息获取版本号.
	 * 
	 * @param groupId
	 *            Maven groupid
	 * @param artifactId
	 *            Maven artifactId
	 * @return 返回版本号
	 */
	public static String readFromMaven(Class<?> baseCalss, String groupId, String artifactId) {
		String propPath = "/META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties";
		InputStream is = baseCalss.getResourceAsStream(propPath);
		if (is == null) {
			is = Version.class.getResourceAsStream(propPath);
		}
		if (is != null) {
			Properties properties = new Properties();
			try {
				properties.load(is);
				String version = properties.getProperty("version");
				if (version != null) {
					return version;
				}
			} catch (IOException e) {
				// ignore
			} finally {
				IOUtils.closeQuietly(is);
			}
		}

		return "undetermined (please report this as bug)";
	}

	/**
	 * 根据Class获取版本号,
	 * <li>查找MANIFEST.MF规范中的实现版本
	 * <li>根据jar命名规则确定版本号
	 * 
	 * @param cls
	 *            class
	 * @param defaultVersion
	 *            找不到确定的版本号时的默认值
	 * @return
	 */
	public static String getVersion(Class<?> cls, String defaultVersion) {
		try {
			// 首先查找MANIFEST.MF规范中的版本号
			String version = cls.getPackage().getImplementationVersion();
			if (version == null || version.length() == 0) {
				version = cls.getPackage().getSpecificationVersion();
			}
			if (version == null || version.length() == 0) {
				// 如果规范中没有版本号，基于jar包名获取版本号
				CodeSource codeSource = cls.getProtectionDomain().getCodeSource();
				if (codeSource == null) {
					LOG.info("No codeSource for class " + cls.getName() + " when getVersion, use default version "
							+ defaultVersion);
				} else {
					String file = codeSource.getLocation().getFile();
					if (file != null && file.length() > 0 && file.endsWith(".jar")) {
						file = file.substring(0, file.length() - 4);
						int i = file.lastIndexOf('/');
						if (i >= 0) {
							file = file.substring(i + 1);
						}
						i = file.indexOf("-");
						if (i >= 0) {
							file = file.substring(i + 1);
						}
						while (file.length() > 0 && !Character.isDigit(file.charAt(0))) {
							i = file.indexOf("-");
							if (i >= 0) {
								file = file.substring(i + 1);
							} else {
								break;
							}
						}
						version = file;
					}
				}
			}
			// 返回版本号，如果为空返回缺省版本号
			return version == null || version.length() == 0 ? defaultVersion : version;
		} catch (Throwable e) { // 防御性容错
			// 忽略异常，返回缺省版本号
			LOG.error("return default version, ignore exception " + e.getMessage(), e);
			return defaultVersion;
		}
	}

	/**
	 * Constant identifying the 1.3.x JVM (JDK 1.3).
	 */
	public static final int JAVA_13 = 0;

	/**
	 * Constant identifying the 1.4.x JVM (J2SE 1.4).
	 */
	public static final int JAVA_14 = 1;

	/**
	 * Constant identifying the 1.5 JVM (Java 5).
	 */
	public static final int JAVA_15 = 2;

	/**
	 * Constant identifying the 1.6 JVM (Java 6).
	 */
	public static final int JAVA_16 = 3;

	/**
	 * Constant identifying the 1.7 JVM (Java 7).
	 */
	public static final int JAVA_17 = 4;

	/**
	 * Constant identifying the 1.8 JVM (Java 8).
	 */
	public static final int JAVA_18 = 5;

	/**
	 * Constant identifying the 1.9 JVM (Java 9).
	 */
	public static final int JAVA_19 = 6;

	private static final String javaVersion;

	private static final int majorJavaVersion;

	static {
		javaVersion = System.getProperty("java.version");
		// version String should look like "1.4.2_10"
		if (javaVersion.contains("1.9.")) {
			majorJavaVersion = JAVA_19;
		} else if (javaVersion.contains("1.8.")) {
			majorJavaVersion = JAVA_18;
		} else if (javaVersion.contains("1.7.")) {
			majorJavaVersion = JAVA_17;
		} else {
			// else leave 1.6 as default (it's either 1.6 or unknown)
			majorJavaVersion = JAVA_16;
		}
	}

	/**
	 * Return the full Java version string, as returned by
	 * {@code System.getProperty("java.version")}.
	 * 
	 * @return the full Java version string
	 * @see System#getProperty(String)
	 */
	public static String getJavaVersion() {
		return javaVersion;
	}

	/**
	 * Get the major version code. This means we can do things like
	 * {@code if (getMajorJavaVersion() >= JAVA_17)}.
	 * 
	 * @return a code comparable to the {@code JAVA_XX} codes in this class
	 * @see #JAVA_16
	 * @see #JAVA_17
	 * @see #JAVA_18
	 * @see #JAVA_19
	 */
	public static int getMajorJavaVersion() {
		return majorJavaVersion;
	}

}
