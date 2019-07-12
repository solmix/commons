package org.solmix.exchange.scheme;

import org.solmix.commons.util.FileUtils;
import org.solmix.exchange.URL;

/**
 * HTTP scheme.
 */
class HttpScheme extends AbstractScheme {

    HttpScheme() {
        super("http", 80);
    }

    HttpScheme(String name, int port) {
        super(name, port);
    }

    @Override
    public URL normalize(URL url) {
        String host = url.getHost();
        if (host != null) {
            host = host.toLowerCase();
        }
        return URL.builder()
                .scheme(url.getScheme())
                .userInfo(url.getUserInfo())
                .host(host, url.getProtocolVersion())
                .port(url.getPort())
                .path(FileUtils.normalizePath(url.getPath()))
                .query(url.getQuery())
                .fragment(url.getFragment())
                .build();
    }
}
