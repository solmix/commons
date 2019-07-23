package org.solmix.runtime.security;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class AllowAllTrustManager implements X509TrustManager
{

    /**
     * Empty certificate sequence.
     */
    private static final X509Certificate[] EMPTY_CERTS = new X509Certificate[0];

    /**
     * Null implementation.
     *
     * @param certs    the supplied certs (ignored)
     * @param authType the supplied type (ignored)
     */
    public void checkServerTrusted( final X509Certificate[] certs, final String authType )
    {
    }

    /**
     * Null implementation.
     *
     * @param certs    the supplied certs (ignored)
     * @param authType the supplied type (ignored)
     */
    public void checkClientTrusted( final X509Certificate[] certs, final String authType )
    {
    }

    /**
     * Null implementation.
     *
     * @return an empty certificate array
     */
    public X509Certificate[] getAcceptedIssuers()
    {
        return EMPTY_CERTS;
    }
}
