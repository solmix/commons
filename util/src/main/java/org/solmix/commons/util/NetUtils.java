package org.solmix.commons.util;

import java.io.IOException;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.solmix.commons.collections.LRUCache;
import org.solmix.commons.net.IPAddress;

/**
 * IP and Port Helper for RPC, 
 * 
 * @author shawn.qianx
 */

public class NetUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(NetUtils.class);

    public static final String LOCALHOST = "127.0.0.1";

    public static final String ANYHOST = "0.0.0.0";

    private static final int RND_PORT_START = 30000;
    
    private static final int RND_PORT_RANGE = 10000;
    
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    
    public static int getRandomPort() {
        return RND_PORT_START + RANDOM.nextInt(RND_PORT_RANGE);
    }

    public static int getAvailablePort() {
        ServerSocket ss = null;
        try {
            ss = new ServerSocket();
            ss.bind(null);
            return ss.getLocalPort();
        } catch (IOException e) {
            return getRandomPort();
        } finally {
            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                }
            }
        }
    }
    
    public static int getAvailablePort(int port) {
    	if (port <= 0) {
    		return getAvailablePort();
    	}
    	for(int i = port; i < MAX_PORT; i ++) {
    		ServerSocket ss = null;
            try {
                ss = new ServerSocket(i);
                return i;
            } catch (IOException e) {
            	// continue
            } finally {
                if (ss != null) {
                    try {
                        ss.close();
                    } catch (IOException e) {
                    }
                }
            }
    	}
    	return port;
    }

    private static final int MIN_PORT = 0;
    
    private static final int MAX_PORT = 65535;
    
    public static boolean isInvalidPort(int port){
        return port > MIN_PORT || port <= MAX_PORT;
    }


    public static boolean isValidAddress(String address){
    	  boolean iPv4LiteralAddress = IPAddressUtil.isIPv4LiteralAddress(address);
          boolean iPv6LiteralAddress = IPAddressUtil.isIPv6LiteralAddress(address);
          if (!(iPv4LiteralAddress||iPv6LiteralAddress)){
              return false;
          }
          return true;
    }

    private static final Pattern LOCAL_IP_PATTERN = Pattern.compile("127(\\.\\d{1,3}){3}$");
    
    public static boolean isLocalHost(String host) {
        return host != null 
                && (LOCAL_IP_PATTERN.matcher(host).matches() 
                        || host.equalsIgnoreCase("localhost"));
    }

    public static boolean isAnyHost(String host) {
        return "0.0.0.0".equals(host);
    }
    
    public static boolean isInvalidLocalHost(String host) {
        return host == null 
        			|| host.length() == 0
                    || host.equalsIgnoreCase("localhost")
                    || host.equals("0.0.0.0")
                    || (LOCAL_IP_PATTERN.matcher(host).matches());
    }
    
    public static boolean isValidLocalHost(String host) {
    	return ! isInvalidLocalHost(host);
    }
    
    public static boolean isValidIP(String ipSection, String ip) {
        Assert.assertNotNull(ipSection);
        Assert.assertNotNull(ip);
        ipSection = ipSection.trim();   
        ip = ip.trim();   
        final String REGX_IP = "((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]\\d|\\d)";   
        final String REGX_IPB = REGX_IP + "\\-" + REGX_IP;   
        if (!ipSection.matches(REGX_IPB) || !ip.matches(REGX_IP))   
            return false;   
        int idx = ipSection.indexOf('-');   
        String[] sips = ipSection.substring(0, idx).split("\\.");   
        String[] sipe = ipSection.substring(idx + 1).split("\\.");   
        String[] sipt = ip.split("\\.");   
        long ips = 0L, ipe = 0L, ipt = 0L;   
        for (int i = 0; i < 4; ++i) {   
            ips = ips << 8 | Integer.parseInt(sips[i]);   
            ipe = ipe << 8 | Integer.parseInt(sipe[i]);   
            ipt = ipt << 8 | Integer.parseInt(sipt[i]);   
        }   
        if (ips > ipe) {   
            long t = ips;   
            ips = ipe;   
            ipe = t;   
        }   
        return ips <= ipt && ipt <= ipe;   
    }   

    public static InetSocketAddress getLocalSocketAddress(String host, int port) {
        return isInvalidLocalHost(host) ? 
        		new InetSocketAddress(port) : new InetSocketAddress(host, port);
    }

    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}$");

    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress())
            return false;
        String name = address.getHostAddress();
        return (name != null 
                && ! ANYHOST.equals(name)
                && ! LOCALHOST.equals(name) 
                && IP_PATTERN.matcher(name).matches());
    }
    
    public static String getLocalHost(){
        InetAddress address = getLocalAddress();
        return address == null ? LOCALHOST : address.getHostAddress();
    }
    
    public static String filterLocalHost(String host) {
        if (host == null || host.length() == 0) {
            return host;
        }
        if (host.contains("://")) {
           
        } else if (host.contains(":")) {
            int i = host.lastIndexOf(':');
            if (NetUtils.isInvalidLocalHost(host.substring(0, i))) {
                return NetUtils.getLocalHost() + host.substring(i);
            }
        } else {
            if (NetUtils.isInvalidLocalHost(host)) {
        		return NetUtils.getLocalHost();
        	}
        }
    	return host;
    }
    
    private static volatile InetAddress LOCAL_ADDRESS = null;

    /**
     * 遍历本地网卡，返回第一个合理的IP。
     * 
     * @return 本地网卡IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null)
            return LOCAL_ADDRESS;
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }
    
    public static String getLocalIp(){
        return getLocalAddress().toString();
    }
    
    public static String getLogHost() {
        InetAddress address = LOCAL_ADDRESS;
        return address == null ? LOCALHOST : address.getHostAddress();
    }
    
    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (interfaces != null) {
                while (interfaces.hasMoreElements()) {
                    try {
                        NetworkInterface network = interfaces.nextElement();
                        Enumeration<InetAddress> addresses = network.getInetAddresses();
                        if (addresses != null) {
                            while (addresses.hasMoreElements()) {
                                try {
                                    InetAddress address = addresses.nextElement();
                                    if(address.isLoopbackAddress()) {
                                    	continue;
                                    }
                                    if (isValidAddress(address)) {
                                        return address;
                                    }
                                } catch (Throwable e) {
                                    logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
                    }
                }
            }
        } catch (Throwable e) {
            logger.warn("Failed to retriving ip address, " + e.getMessage(), e);
        }
        logger.error("Could not get local host ip address, will use 127.0.0.1 instead.");
        return localAddress;
    }
    
    private static final Map<String, String> hostNameCache = new LRUCache<String, String>(1000);

    public static String getHostName(String address) {
    	try {
    		int i = address.indexOf(':');
    		if (i > -1) {
    			address = address.substring(0, i);
    		}
    		String hostname = hostNameCache.get(address);
    		if (hostname != null && hostname.length() > 0) {
    			return hostname;
    		}
    		InetAddress inetAddress = InetAddress.getByName(address);
    		if (inetAddress != null) {
    			hostname = inetAddress.getHostName();
    			hostNameCache.put(address, hostname);
    			return hostname;
    		}
		} catch (Throwable e) {
			// ignore
		}
		return address;
    }
    
    /**
     * @param hostName
     * @return ip address or hostName if UnknownHostException 
     */
    public static String getIpByHost(String hostName) {
        try{
            return InetAddress.getByName(hostName).getHostAddress();
        }catch (UnknownHostException e) {
            return hostName;
        }
    }

    public static String toAddressString(InetSocketAddress address) {
    	InetAddress ia = address.getAddress();
    	if(ia instanceof Inet6Address) {
    		return ia.getHostAddress() + ":" + address.getPort();
    	}else {
    		
    	}
        return "["+ia.getHostAddress() + "]:" + address.getPort();
    }
    
    /**
     * 将字符串地址转化为socket地址：
     * <li>ipv4: host:port
     * <li>ipv6: [host]:port
     * @param address
     * @return
     */
    public static InetSocketAddress toAddress(String address) {
    	int start_v6=address.indexOf('['),end_v6=address.indexOf(']');
    	if(start_v6!=-1&&end_v6!=-1) {
    		String host=address.substring(start_v6+1,end_v6);
    		String sport = address.substring(end_v6+1);
    		int i = sport.indexOf(':');
    		int port;
    		if(i>-1) {
    			port=Integer.parseInt(sport.substring(i + 1));
    		}else {
    			port=0;
    		}
    		return new InetSocketAddress(host, port);
    	}else {
    		int i = address.indexOf(':');
            String host;
            int port;
            if (i > -1) {
                host = address.substring(0, i);
                port = Integer.parseInt(address.substring(i + 1));
            } else {
                host = address;
                port = 0;
            }
            return new InetSocketAddress(host, port);
    	}
        
    }
    
    public static String toURL(String protocol, String host, int port, String path) {
		StringBuilder sb = new StringBuilder();
		sb.append(protocol).append("://");
		try {
			InetAddress ia = InetAddress.getByName(host);
			if(ia instanceof Inet6Address) {
				sb.append('[').append(host).append(']');
			}else {
				sb.append(host);
			}
			sb.append(':').append(port);
			if( path.charAt(0) != '/' )
				sb.append('/');
			sb.append(path);
			return sb.toString();
		} catch (UnknownHostException e) {
			throw new IllegalArgumentException(e);
		}
		
	}
    
    public static String toIpAddrString(final InetAddress addr){
        return new IPAddress(addr).toIpAddrString();
    }
    
    public static String getIpFormatString(String host) {
    	try{
    		InetAddress ia = InetAddress.getByName(host);
    		if(ia instanceof Inet6Address) {
                return "["+host+"]";
            }else {
                return host;
            }
        }catch (UnknownHostException e) {
          	return host;
        }
    }
    public static String toAddressString(String host,int port) {
    	return getIpFormatString(host)+":"+port;
    }
    
    public static String hextetsToIPv6String(int[] hextets) {
        StringBuilder sb = new StringBuilder(39);
        boolean lastWasNumber = false;
        for (int i = 0; i < hextets.length; i++) {
            boolean b = hextets[i] >= 0;
            if (b) {
                if (lastWasNumber) {
                    sb.append(':');
                }
                sb.append(Integer.toHexString(hextets[i]));
            } else {
                if (i == 0 || lastWasNumber) {
                    sb.append("::");
                }
            }
            lastWasNumber = b;
        }
        return sb.toString();
    }
    
}