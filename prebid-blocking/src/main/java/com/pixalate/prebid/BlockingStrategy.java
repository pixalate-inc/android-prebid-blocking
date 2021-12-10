package com.pixalate.prebid;

import android.content.Context;

/**
 * An interface
 */
public interface BlockingStrategy {
    /**
     * Gets the device ID of the device.
     * @param context App context
     * @param callback The callback containing the device ID, or null if none found
     */
    void getDeviceID ( Context context, BlockingStrategyCallback callback );

    /**
     * Gets the IPv4 address associated with the device.
     * @param context App context
     * @param callback The callback containing the IPv4 address, or null if none found
     */
    void getIPv4 ( Context context, BlockingStrategyCallback callback );

    /**
     * Gets the IPv6 address associated with the device.
     * @param context App context
     * @param callback The callback containing the IPv6 address, or null if none found
     */
    void getIPv6 ( Context context, BlockingStrategyCallback callback );

    /**
     * Gets the browser user agent string of the device.
     * @param context App context
     * @param callback The callback containing the user agent, or null if none found
     */
    void getUserAgent ( Context context, BlockingStrategyCallback callback );
}
