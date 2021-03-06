
package org.solmix.runtime.helper;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public final class HttpHeaderHelper {
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_ID = "Content-ID";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
    public static final String COOKIE = "Cookie";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CHUNKED = "chunked";
    public static final String CONNECTION = "Connection";
    public static final String CLOSE = "close";
    public static final String AUTHORIZATION = "Authorization";
    private static final String UTF_8 = Charset.forName("UTF-8").name();
    
    private static Map<String, String> internalHeaders = new HashMap<String, String>();
    private static ConcurrentHashMap<String, String> encodings = new ConcurrentHashMap<String, String>();
    private static Pattern charsetPattern = Pattern.compile("\"|'");
    
    static {
        internalHeaders.put("Accept-Encoding", "accept-encoding");
        internalHeaders.put("Content-Encoding", "content-encoding");
        internalHeaders.put("Content-Type", "content-type");
        internalHeaders.put("Content-ID", "content-id");
        internalHeaders.put("Content-Transfer-Encoding", "content-transfer-encoding"); 
        internalHeaders.put("Transfer-Encoding", "transfer-encoding");
        internalHeaders.put("Connection", "connection");
        internalHeaders.put("authorization", "Authorization");
        internalHeaders.put("soapaction", "SOAPAction");
        internalHeaders.put("accept", "Accept");
        internalHeaders.put("content-length", "Content-Length");
    }
    
    private HttpHeaderHelper() {
        
    }
    
    public static List<String> getHeader(Map<String, List<String>> headerMap, String key) {
        return headerMap.get(getHeaderKey(key));
    }
    
    public static String getHeaderKey(String key) {
        if (internalHeaders.containsKey(key)) {
            return internalHeaders.get(key);
        } else {
            return key;
        }
    }
    
    public static String findCharset(String contentType) {
        if (contentType == null) {
            return null;
        }
        int idx = contentType.indexOf("charset=");
        if (idx != -1) {
            String charset = contentType.substring(idx + 8);
            if (charset.indexOf(";") != -1) {
                charset = charset.substring(0, charset.indexOf(";")).trim();
            }
            if (charset.charAt(0) == '\"') {
                charset = charset.substring(1, charset.length() - 1);
            }
            return charset;
        }
        return null;
    }
    public static String mapCharset(String enc) {
        return mapCharset(enc, UTF_8);
    }    
    
    //helper to map the charsets that various things send in the http Content-Type header 
    //into something that is actually supported by Java and the Stax parsers and such.
    public static String mapCharset(String enc, String deflt) {
        if (enc == null) {
            return deflt;
        }
        //older versions of tomcat don't properly parse ContentType headers with stuff
        //after charset="UTF-8"
        int idx = enc.indexOf(";");
        if (idx != -1) {
            enc = enc.substring(0, idx);
        }
        // Charsets can be quoted. But it's quite certain that they can't have escaped quoted or
        // anything like that.
        enc = charsetPattern.matcher(enc).replaceAll("").trim();
        if ("".equals(enc)) {
            return deflt;
        }
        String newenc = encodings.get(enc);
        if (newenc == null) {
            try {
                newenc = Charset.forName(enc).name();
            } catch (IllegalCharsetNameException icne) {
                return null;
            } catch (UnsupportedCharsetException uce) {
                return null;
            }
            String tmpenc = encodings.putIfAbsent(enc, newenc);
            if (tmpenc != null) {
                newenc = tmpenc;
            }
        }
        return newenc;
    }
}
