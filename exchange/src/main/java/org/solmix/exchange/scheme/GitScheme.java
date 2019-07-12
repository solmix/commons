package org.solmix.exchange.scheme;

/**
 * Git scheme.
 */
class GitScheme extends HttpScheme {

    GitScheme() {
        super("git", 443);
    }

    GitScheme(String name, int port) {
        super(name, port);
    }

}
