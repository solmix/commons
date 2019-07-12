package org.solmix.exchange.scheme;

/**
 * Secure HTTP scheme.
 */
class SecureHttpScheme extends HttpScheme {

    SecureHttpScheme() {
        super("https", 443);
    }

}
