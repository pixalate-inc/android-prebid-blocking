package com.pixalate.prebid;

/**
 * Interface for responses from requestBlockStatus.
 */
public interface BlockingStatusListener {
    /**
     * Method that is called if it is determined that the traffic is invalid.
     */
    void onBlock ();

    /**
     * Method that is called if it is determined that the traffic is valid.
     */
    void onAllow ();

    /**
     * Method that is called if something goes wrong with the request, for eg. incorrect login details, or a timeout.
     * @param errorCode The code of the error.
     * @param message The message of the error.
     */
    void onError ( int errorCode, String message );
}
