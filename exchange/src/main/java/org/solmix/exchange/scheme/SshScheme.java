package org.solmix.exchange.scheme;

/**
 * Secure shell scheme.
 */
class SshScheme extends HttpScheme {

    SshScheme() {
        super("ssh", 22);
    }

    SshScheme(String name, int port) {
        super(name, port);
    }
}
