package org.solmix.commons.util;

import org.junit.Assert;
import org.junit.Test;

public class NetUtilsTest {

	@Test
	public void isValidAddress() throws Exception {
//	        boolean valid=NetUtils.isValidAddress("192.168.0.100");
//
//	        Assert.assertTrue(valid);
	}

	@Test
	public void toAddress() throws Exception {
		NetUtils.toAddress("[111::1]:3307");
		NetUtils.toAddress("[111::1]");
		NetUtils.toAddress("127.0.0.1:5675");
	}

	@Test
	public void toURL() throws Exception {
		String url = NetUtils.toURL("http", "111::1", 1231, "/ads");
		Assert.assertEquals(url, "http://[111::1]:1231/ads");
		
		String url2 = NetUtils.toURL("http", "127.0.0.1", 1231, "/ads");
		Assert.assertEquals(url2, "http://127.0.0.1:1231/ads");
	}
}
