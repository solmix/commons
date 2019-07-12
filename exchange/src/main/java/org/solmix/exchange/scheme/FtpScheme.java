package org.solmix.exchange.scheme;

/**
 * FTP scheme.
 */
class FtpScheme extends HttpScheme {

    FtpScheme() {
        super("ftp", 21);
    }

    FtpScheme(String name, int port) {
        super(name, port);
    }

}
