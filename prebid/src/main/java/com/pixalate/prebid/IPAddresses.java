package com.pixalate.prebid;

import androidx.annotation.Nullable;
import java.util.Objects;

/**
 * Immutable container for IP Address results.
 */
public class IPAddresses {
    /**
     * The IPv4 address, if it exists.
     */
    private final String ipv4;

    /**
     * The IPv6 address, if it exists.
     */
    private final String ipv6;

    /**
     *
     * @param ipv4 The IPv4 address, if applicable.
     * @param ipv6 The IPv6 address, if applicable.
     */
    public IPAddresses ( String ipv4, String ipv6 ) {
        this.ipv4 = ipv4;
        this.ipv6 = ipv6;
    }

    /**
     * Gets the IPv4 address.
     * @return The IPv4 address.
     */
    public String getIpv4 () {
        return ipv4;
    }

    /**
     * Gets the IPv6 address.
     * @return The IPv6 address.
     */
    public String getIpv6 () {
        return ipv6;
    }

    @Override
    public boolean equals ( @Nullable Object o ) {
        if( this == o ) return true;
        if( o == null || getClass() != o.getClass() ) return false;

        IPAddresses other = (IPAddresses) o;

        return Objects.equals( other.ipv4, ipv4 ) && Objects.equals( other.ipv6, ipv6 );
    }

    @Override
    public int hashCode () {
        int result = 1;

        result = 31 * result + (ipv4 == null ? 0 : ipv4.hashCode() );
        result = 31 * result + (ipv6 == null ? 0 : ipv6.hashCode() );

        return result;
    }
}