package org.solmix.exchange;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.solmix.exchange.util.IPVersion;


public class URLTest {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@Test
	public void test() {
		URL url =URL.from("http://localhost:8080/a/b/c?x1=qw&x2=y2");
		Assert.assertEquals("http", url.getScheme());
		Assert.assertEquals("localhost", url.getDecodedHost());
		Assert.assertEquals(Integer.valueOf(8080), url.getPort());
		Assert.assertEquals("qw", url.getQueryParams().get("x1").get(0));
		Assert.assertEquals(IPVersion.IPV4, url.getProtocolVersion());
		Assert.assertEquals("x1=qw&x2=y2", url.getQuery());
		
		
	}

}
