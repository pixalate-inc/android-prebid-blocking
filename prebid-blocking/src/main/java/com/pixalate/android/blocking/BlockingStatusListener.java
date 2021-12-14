package com.pixalate.android.blocking;

/**
 * Interface for responses from requestBlockStatus.
 */
public interface BlockingStatusListener {
    /**
     * Method that is called if it is determined that the traffic is invalid.
     */
    default void onBlock () {}

    /**
     * Method that is called if it is determined that the traffic is valid.
     */
    default void onAllow () {}

    /**
     * Method that is called if something goes wrong with the request, for eg. incorrect login details, or a timeout.
     * @param errorCode The code of the error.
     * @param message The message of the error.
     */
    default void onError ( int errorCode, String message ) {}
}
