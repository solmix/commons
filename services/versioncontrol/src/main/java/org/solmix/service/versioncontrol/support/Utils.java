package org.solmix.service.versioncontrol.support;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Utils {
	private static Logger logger = LoggerFactory.getLogger(Utils.class);

	public static Properties getPropertiesFromClasspath(Class<?> clazz,
			String propFileName) throws IOException {
		Properties props = new Properties();
		InputStream inputStream = null;
		try {
			inputStream = clazz.getClassLoader().getResourceAsStream(propFileName);

			if (inputStream == null) {
				throw new FileNotFoundException("property file '"
						+ propFileName + "' not found in the classpath");
			}

			props.load(inputStream);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
		return props;
	}

	
	public static boolean recursiveCreateDir(final String dir) {
		if (dir == null || dir.length() == 0)
			return false;
		String path=dir;
		boolean success = true;
		if(!path.endsWith(File.separator)) {
			path=path+File.separator;
		}

		final StringBuffer sb = new StringBuffer(path.length());
		Character c;
		for (int i = 0; i < path.length(); i++) {
			c = path.charAt(i);
			if (c == File.separatorChar) {
				if(sb.length()==0) {
					sb.append(File.separatorChar);
					continue;
				}
				final File f = new File(sb.toString());
				if (!f.exists() || (f.exists() && f.isFile()))
					success = f.mkdir();

				if (!success) {
					logger.error("Failed to create directory: " + sb.toString()
							+ " for file: " + path);
					break;
				}
			}
			sb.append(c);
		}
		return success;
	}

	public static boolean writeToFile(final File f, final byte[] content,
			final boolean create) {

		boolean success = true;

		FileOutputStream fop = null;
		try {
			if (!f.exists() && create) {
				success = recursiveCreateDir(f.getCanonicalPath());
				if (!success) {
					logger.error("Error creating file: " + f.getPath());
					return false;
				}
				success = f.createNewFile();
			}

			if (!success)
				return success;

			fop = new FileOutputStream(f, false);
			fop.write(content);
			fop.flush();
		} catch (IOException e) {
			logger.error(
					"Error writing to file: " + f.getPath() + " "
							+ e.getMessage(), e);
			return false;
		} finally {
			try {
				if (fop != null)
					fop.close();
			} catch (IOException e) {
			}
		}
		return true;
	}

	
	public static String getParentPath(String path) {
		if (path == null) {
			return null;
		}

		int index = path.lastIndexOf('/');
		if (index == -1) {
			index = path.lastIndexOf('\\');
		}
		if (index == -1) {
			return null;
		}

		String parent = path.substring(0, index);
		if (parent.length() == 0) {
			return null;
		}
		return parent;
	}

	/**
	 * Calculate the file name for a path.
	 * 
	 * @param fullPath
	 * @return
	 */
	public static String fileName(String path) {
		if (path == null) {
			return null;
		}

		int index = path.lastIndexOf('/');
		if (index == -1) {
			index = path.lastIndexOf('\\');
		}
		if (index == -1) {
			return null;
		}

		String name = path.substring(index + 1);
		if (name.length() == 0) {
			return null;
		}
		return name;
	}

	public static String getFileExtension(String name) {
		if (name == null)
			return null;
		int k = name.lastIndexOf(".");
		String ext = null;
		if (k != -1)
			ext = name.substring(k + 1, name.length());
		return ext;
	}

	public static String getHashHexDigest(InputStream fis) throws IOException {
		byte[] buf = new byte[1024];
		MessageDigest md5digest = null;
		try {
			md5digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("MD5 Algorithm seems to be missing!?!?");
		}
		int n;
		do {
			n = fis.read(buf);
			if (n > 0) {
				md5digest.update(buf, 0, n);
			}
		} while (n != -1);
		byte[] md5bytes = md5digest.digest();
		String md5hex = "";
		for (int i = 0; i < md5bytes.length; i++) {
			md5hex += Integer.toString((md5bytes[i] & 0xff) + 0x100, 16 /* radix */
			).substring(1);
		}
		return md5hex;
	}

	public static String formatPath(final String path) {
		final String replaceSlash = path.replace("\\", "/");
		String formattedPath = replaceSlash.replace("//", "/");
//		if (formattedPath.startsWith("/"))
//			formattedPath = formattedPath.substring(1, formattedPath.length());
		// for windows, make sure disk name is lowercase
		formattedPath = formatWinDrive(formattedPath);
		return formattedPath;
	}

	public static String formatWinDrive(String path) {
		String s=path;
		if (path.charAt(1)==':')
			s = path.substring(0, 1).toLowerCase() + path.substring(1);
		return s;
	}

	public static String getMd5Stream(InputStream is){
		String md5String = null;
		try {
			md5String = getHashHexDigest(is);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
		if (md5String == null || md5String.length() <=0){
			logger.error("MD5 String null or empty.");
		}
		return md5String;
	}

	public static String getRoot(String path) {
		File f = new File (path);
		while (f.getParentFile() != null)
			f = f.getParentFile();
		String s = f.getName();
		String t = f.getPath();
		if (s.length() == 0 && t.length() > 0){
			int i = t.indexOf(File.separator, 1);
			if (i > 0)
				s = t.substring(0, i);
			else 
				s = t;
		}
		return s;
	}
	public static boolean isZipArchive(String filename) {
		if ((new File(filename).isDirectory()))
			return false;
		return filename.endsWith(".jar");
	} 
	public static String getZipMethodName(long method) {
		switch ((int)method) {
			case ZipEntry.DEFLATED:
				return "DEFLATED";
			case ZipEntry.STORED:
				return "STORED";
			default:
				logger.debug("Unknown Zip method encountered " + method);
				return "UNKNOWN";
		}
	}
}
