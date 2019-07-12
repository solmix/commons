package org.solmix.exchange.scheme;

/**
 * Secure web socket scheme.
 */
class SecureWebSocketScheme extends WebSocketScheme {

    SecureWebSocketScheme() {
        super("wss", 443);
    }

}
