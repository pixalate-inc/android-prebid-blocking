package com.pixalate.android.blocking;

/**
 * Callback to pass the blocking parameter result back from the strategy.
 */
public interface BlockingStrategyCallback {
    void done ( String result );
}
