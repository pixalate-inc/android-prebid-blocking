package com.pixalate.prebid;

import android.content.Context;

/**
 * An interface
 */
public interface BlockingStrategy {
    /**
     * Gets the device ID of the device.
     * @return The device ID of the device, or null.
     * @param context App context
     */
    String getDeviceID ( Context context );

    /**
     * Gets the IPv4 address associated with the device.
     * @return The found IPv4 address, or null.
     * @param context App context
     */
    String getIPv4 ( Context context );

    /**
     * Gets the IPv6 address associated with the device.
     * @return The found IPv6 address, or null.
     * @param context App context
     */
    String getIPv6 ( Context context );

    /**
     * Gets the browser user agent string of the device.
     * @return The user agent of the device, or null of none found.
     * @param context App context
     */
    String getUserAgent ( Context context );
}
