package com.pixalate.android.blocking;

import android.content.Context;

/**
 * Allows for custom implementations of fetching blocking parameters, if the default
 * implementation is not suitable for your use case.
 */
public interface BlockingStrategy {
    /**
     * Gets the device ID associated with the device.
     * This method is not meant to be called directly by user code.
     * @param context  App context
     * @param callback The callback containing the fetched device ID, or null if none found.
     */
    default void getDeviceID(Context context, BlockingStrategyCallback callback) {
        callback.done( null );
    }

    /**
     * Gets the IPv4 address associated with the device.
     * This method is not meant to be called directly by user code.
     * @param context App context
     * @param callback The callback containing the fetched IPv4 address, or null if none found.
     */
    default void getIPv4 ( Context context, BlockingStrategyCallback callback ) {
        callback.done( null );
    }

//    /**
//     * Gets the IPv6 address associated with the device.
//     * This method is not meant to be called directly by user code.
//     * @param context App context
//     * @param callback The callback containing the fetched IPv6 address, or null if none found.
//     */
//    default void getIPv6 ( Context context, BlockingStrategyCallback callback ) {
//        callback.done( null );
//    }

    /**
     * Gets the browser user agent associated with the device.
     * This method is not meant to be called directly by user code.
     * @param context App context
     * @param callback The callback containing the fetched user agent, or null if none found.
     */
    default void getUserAgent ( Context context, BlockingStrategyCallback callback ) {
        callback.done( null );
    }
}
