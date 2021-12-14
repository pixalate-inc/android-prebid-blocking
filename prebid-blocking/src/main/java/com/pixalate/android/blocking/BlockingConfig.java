package com.pixalate.android.blocking;

/**
 * Global configuration for the Pixalate SDK's com.pixalate.android.blocking behavior.
 */
public final class BlockingConfig {
    private String apiKey;
    private long ttl;
    private double blockingThreshold;
    private int requestTimeout;
    private BlockingStrategy blockingStrategy;

    /**
     * Returns the configured Pixalate API key.
     * @return The com.pixalate.android.blocking threshold.
     */
    public String getApiKey () {
        return apiKey;
    }

    /**
     * Returns the configured com.pixalate.android.blocking threshold, the value at or above which will trigger a block response.
     * @return The com.pixalate.android.blocking threshold.
     */
    public double getBlockingThreshold () {
        return blockingThreshold;
    }

    /**
     * Returns the configured com.pixalate.android.blocking strategy, which provides critical device values for determining block probability.
     * @return The com.pixalate.android.blocking strategy.
     */
    public BlockingStrategy getBlockingStrategy () {
        return blockingStrategy;
    }

    /**
     * Returns the configured TTL for cached com.pixalate.android.blocking requests.
     * @return The TTL.
     */
    public long getTTL() {
        return ttl;
    }

    /**
     * Returns the configured time to wait for a request to return.
     * @return The TTL.
     */
    public int getRequestTimeout () {
        return requestTimeout;
    }

    /**
     * Helper class for building PixalateConfig objects.
     */
    public static final class Builder {
        private final String apiKey;

        private int requestTimeout;

        private double blockingThreshold;
        private long ttl;

        private BlockingStrategy blockingStrategy;

        public Builder ( String apiKey ) {
            this.apiKey = apiKey;
            this.blockingThreshold = 0.75f;
            this.requestTimeout = 2000;
            this.ttl = 1000 * 60 * 60 * 8;
        }

        /**
         * The comparison value to be used as a measure of whether to allow the traffic, or block it.
         * @param blockingThreshold The threshold, from 0.1 to 1, that sets the maximum allowable IVT probability.
         * @return This builder instance for chaining purposes.
         */
        public Builder setBlockingThreshold ( double blockingThreshold ) {
            if( blockingThreshold < 0.1 || blockingThreshold > 1 ) {
                throw new IllegalArgumentException( "The com.pixalate.android.blocking threshold must be between 0.1 and 1, inclusive." );
            }

            this.blockingThreshold = blockingThreshold;

            return this;
        }

        /**
         * The maximum time a request can take, including any strategy executions -- anything beyond this will count as a failed attempt and call the error listener.
         * @param timeout The timeout value in milliseconds.
         * @return This builder instance for chaining purposes.
         */
        public Builder setRequestTimeout ( int timeout ) {
            if( timeout < 0 ) timeout = 0;
            this.requestTimeout = timeout;

            return this;
        }

        /**
         * The maximum time a cached result should be stored in the cache.
         * A value of 0 disables the cache.
         * @param ttl The cache age value in milliseconds.
         * @return This builder instance for chaining purposes.
         */
        public Builder setTTL ( long ttl ) {
            if( ttl < 0 ) throw new IllegalArgumentException( "The ttl must be greater than or equal to 0." );
            this.ttl = ttl;

            return this;
        }

        /**
         * The strategy to use for retrieving important com.pixalate.android.blocking parameters.
         * Defaults to an implementation that provides the most common use case.
         * @param strategy The strategy to implement.
         * @return This builder instance for chaining purposes.
         */
        public Builder setBlockingStrategy ( BlockingStrategy strategy ) {
            this.blockingStrategy = strategy;
            return this;
        }

        /**
         * Build the config.
         * @return The built config.
         */
        public BlockingConfig build () {
            BlockingConfig config = new BlockingConfig();
            config.apiKey = apiKey;
            config.blockingThreshold = blockingThreshold;
            config.requestTimeout = requestTimeout;
            config.ttl = ttl;

            if( this.blockingStrategy != null ) {
                config.blockingStrategy = blockingStrategy;
                if( blockingStrategy instanceof DefaultBlockingStrategy ) {
                    DefaultBlockingStrategy defaultBlockingStrategy = (DefaultBlockingStrategy) blockingStrategy;
                    if( defaultBlockingStrategy.getRequestTimeout() < 0 ) {
                        defaultBlockingStrategy.setRequestTimeout( requestTimeout );
                    }
                }
            } else {
                config.blockingStrategy = new DefaultBlockingStrategy( ttl );
            }

            return config;
        }
    }
}
