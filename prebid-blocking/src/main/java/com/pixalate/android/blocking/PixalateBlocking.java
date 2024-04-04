package com.pixalate.android.blocking;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.JsonReader;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

public final class PixalateBlocking {

    static final String TAG = "PixalateBlocking";

    private static final String baseFraudURL = "https://fraud-api.pixalate.com/api/v2/fraud?";

    static LogLevel logLevel = LogLevel.INFO;

    static HashMap<BlockingCacheParameters,BlockingResult> cachedResults;
    static BlockingConfig globalConfig;
    static boolean initialized;
    static WeakReference<Context> context;
    static Executor executor;
    final static ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 4 );

    PixalateBlocking () {}

    /**
     * Sets the level to which debug statements should be logged to the console.
     * @param level The LogLevel to use.
     */
    public static void setLogLevel ( LogLevel level ) {
        logLevel = level;
    }

    /**
     * Gets the current log level.
     * @return The current log level.
     */
    public static LogLevel getLogLevel () {
        return logLevel;
    }

    /**
     * Updates the global configuration with a new value. You should not normally need to use this.
     * @param config The config to update with.
     */
    public static void updateGlobalConfig ( BlockingConfig config ) {
        if( !initialized ) {
            throw new IllegalStateException( "You must set the global com.pixalate.android.blocking config using `Pixalate.initialize` before updating the configuration." );
        }

        globalConfig = config;

        executor = new ThreadPoolExecutor( 2, 4, Math.max(globalConfig.getRequestTimeout(),1000), TimeUnit.MILLISECONDS, queue );
    }

    /**
     * Returns the currently set global configuration.
     * @return The active global configuration, or null if the SDK has not been initialized.
     */
    public static BlockingConfig getGlobalConfig () {
        return globalConfig;
    }

    static void LogInfo ( String message ) {
        if( logLevel.includes( LogLevel.INFO ) ) {
            Log.i( TAG, message );
        }
    }

    static void LogError ( String message ) {
        if( logLevel.includes( LogLevel.ERROR ) ) {
            Log.e( TAG, message );
        }
    }

    static void LogWarning ( String message ) {
        if( logLevel.includes( LogLevel.WARNING ) ) {
            Log.w( TAG, message );
        }
    }

    static void LogDebug ( String message ) {
        if( logLevel.includes( LogLevel.DEBUG ) ) {
            Log.d( TAG, message );
        }
    }

    /**
     * Initializes the Pixalate Pre-bid Blocking SDK.
     * @param context An application context.
     * @param config The blocking config to initialize with.
     */
    public static void initialize ( Context context, BlockingConfig config ) {
        PixalateBlocking.context = new WeakReference<>( context );

        initialized = true;

        cachedResults = new HashMap<>();
        updateGlobalConfig( config );
    }

    /**
     * Requests a block status using the com.pixalate.android.blocking strategy. If anything goes wrong with the request, eg. incorrect login details, it will
     * return an onError result in the listener. Otherwise, it will use the set threshold to compare probabilities and return a positive
     * or negative result in onAllow and onBlock respectively.
     *
     * @param listener   The listener will be called with the results of the request.
     */
    public static void requestBlockStatus ( BlockingStatusListener listener ) throws IllegalStateException {
        requestBlockStatus( BlockingMode.DEFAULT, listener );
    }

    /**
     * Requests a block status using the com.pixalate.android.blocking strategy and the selected com.pixalate.android.blocking mode.
     * It will then compare the result to the selected threshold, and call the appropriate method on the passed listener.
     * If an error occurs with the request, {@link com.pixalate.android.blocking.BlockingStatusListener#onError onError} will be called.
     * or negative result in onAllow and onBlock respectively.
     *
     * @param mode       The BlockingMode to utilize.
     * @param listener   The listener will be called with the results of the request.
     */
    public static void requestBlockStatus ( BlockingMode mode, BlockingStatusListener listener ) throws IllegalStateException {
        if( !initialized ) {
            throw new IllegalStateException( "You must set the global com.pixalate.android.blocking config using `Pixalate.initialize` before requesting block status." );
        }

        final Context ctx = context.get();

        if( ctx == null ) {
            throw new IllegalStateException( "Context is null, cannot proceed." );
        }

        Handler handler = new Handler();
        CountDownLatch latch = new CountDownLatch( 3 );

        final BlockingStrategy strategy = globalConfig.getBlockingStrategy();
        final BlockingCacheParameters cacheParams = new BlockingCacheParameters();

        executor.execute( () -> {
            strategy.getDeviceID(ctx, result -> {
                cacheParams.deviceId = result;
                latch.countDown();
            });
        });

        executor.execute( () -> {
            strategy.getIPv4( ctx, result -> {
                cacheParams.ipv4 = result;
                latch.countDown();
            });
        });

        executor.execute( () -> {
            strategy.getIPv6( ctx, result -> {
                cacheParams.ipv6 = result;
                latch.countDown();
            });
        });

        executor.execute( () -> {
            strategy.getUserAgent( ctx, result -> {
                cacheParams.userAgent = result;
                latch.countDown();
            });
        });

        SendPreBidBlockingRequestTask task = new SendPreBidBlockingRequestTask( globalConfig.getTTL(), globalConfig.getBlockingThreshold(), listener );
        task.execute( new BlockingRequestParameters( latch, cacheParams, globalConfig.getApiKey(), globalConfig.getRequestTimeout(), mode ) );
    }

    /**
     * Available log granularity levels. Set the global Pixalate log level by calling {@link PixalateBlocking#setLogLevel(LogLevel)}.
     */
    public enum LogLevel {
        NONE( 0 ),
        INFO( 1 ),
        ERROR( 2 ),
        WARNING( 3 ),
        DEBUG( 4 );

        private final int severity;

        LogLevel ( int severity ) {
            this.severity = severity;
        }

        public boolean includes ( LogLevel other ) {
            return this.severity >= other.severity;
        }
    }

    /**
     * Various blocking modes for testing. You can pass this into the {@link PixalateBlocking#requestBlockStatus(BlockingMode, BlockingStatusListener)}
     * overload to test your ad loads given guaranteed blocking states.
     */
    public enum BlockingMode {
        DEFAULT,
        ALWAYS_BLOCK,
        NEVER_BLOCK
    }

    /**
     * An exception wrapper around a non-OK HTTP response code.
     */
    public static class HTTPException extends Exception {
        public final int errorCode;

        public HTTPException ( int errorCode, String message ) {
            super( message );
            this.errorCode = errorCode;
        }
    }

    /**
     * Occurs when the strategy takes too long to execute and times out the request.
     */
    public static class StrategyTimeoutException extends Exception {
        StrategyTimeoutException ( String message ) {
            super( message );
        }
    }

    private static class BlockingResult {
        String message = null;
        int errorCode = -1;
        double probability = -1;

        BlockingCacheParameters parameters;



        long time;

        public boolean hasError () {
            return errorCode > -1;
        }
    }

    private static class BlockingRequestParameters {
        BlockingCacheParameters cacheParams;
        BlockingMode mode;
        String apiKey;
        int timeout;
        final CountDownLatch latch;

        public BlockingRequestParameters( CountDownLatch latch, BlockingCacheParameters params, String apiKey, int timeout, BlockingMode mode ) {
            this.latch = latch;
            this.cacheParams = params;
            this.mode = mode;
            this.apiKey = apiKey;
            this.timeout = timeout;
        }
    }

    private static class BlockingCacheParameters {
        String ipv4;
        String ipv6;
        String deviceId;
        String userAgent;
        BlockingMode mode;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BlockingCacheParameters parameters = (BlockingCacheParameters) o;
            return Objects.equals(ipv4,parameters.ipv4) &&
                Objects.equals(ipv6, parameters.ipv6) &&
                Objects.equals(deviceId,parameters.deviceId) &&
                Objects.equals(userAgent,parameters.userAgent) &&
                Objects.equals(mode, parameters.mode);
        }

        @Override
        public int hashCode() {
            int result = 1;

            result = 31 * result + (ipv4 == null ? 0 : ipv4.hashCode());
            result = 31 * result + (ipv6 == null ? 0 : ipv6.hashCode());
            result = 31 * result + (deviceId == null ? 0 : deviceId.hashCode());
            result = 31 * result + (userAgent == null ? 0 : userAgent.hashCode());
            result = 31 * result + (mode == null ? 0 : mode.hashCode());

            return result;
        }
    }

    private static class SendPreBidBlockingRequestTask extends AsyncTask<BlockingRequestParameters,Integer,BlockingResult> {

        BlockingStatusListener listener;
        double threshold;
        long ttl;

        public SendPreBidBlockingRequestTask ( long ttl, double blockingThreshold, BlockingStatusListener listener ) {
            this.threshold = blockingThreshold;
            this.listener = listener;
            this.ttl = ttl;
        }

        @Override
        protected BlockingResult doInBackground ( BlockingRequestParameters... parameters ) {
            BlockingRequestParameters param = parameters[ 0 ];

            int timeout = param.timeout;
            boolean hasTimeout = timeout > 0;

            long now = System.currentTimeMillis();

            String apiKey = param.apiKey;
            BlockingCacheParameters cacheParams = param.cacheParams;

            try {
                boolean success = param.latch.await( timeout, TimeUnit.MILLISECONDS );
                if( !success ) {
                    timeout = 0;
                }
            } catch (InterruptedException ignored) {}

            if( globalConfig.getTTL() > 0 ) {
                BlockingResult result = cachedResults.get( cacheParams );

                if( result != null ) {
                    if( result.time > new Date().getTime() ) {
                        LogDebug( "Using cached results." );

                        return result;
                    } else {
                        cachedResults.remove( cacheParams );
                    }
                }
            }

            BlockingMode mode = param.mode;
            if( mode != BlockingMode.DEFAULT ) {
                LogDebug( "Using custom com.pixalate.android.blocking mode: " + mode );
                BlockingResult result = new BlockingResult();
                result.parameters = cacheParams;
                result.probability = mode == BlockingMode.ALWAYS_BLOCK ? 1 : 0;
                return result;
            }

            HttpsURLConnection connection = null;

            try {
                timeout -= System.currentTimeMillis() - now;

                if( hasTimeout && timeout <= 0 ) {
                    throw new StrategyTimeoutException( "Timeout exceeded while executing strategy, aborting the request. If this is occurring too often, try bumping up the requestTimeout in the global config." );
                }

                LogDebug( "Remaining timeout after strategies: " + timeout );

                URL url = new URL( buildUrl(cacheParams.deviceId, cacheParams.ipv4, cacheParams.ipv6, cacheParams.userAgent ) );

                LogDebug( "Sent URL: " + url.toString() );

                connection = (HttpsURLConnection) url.openConnection();

                connection.setRequestMethod( "GET" );
                if( hasTimeout ) {
                    connection.setConnectTimeout(timeout);
                }

                if( apiKey != null ) connection.setRequestProperty( "X-Api-Key", apiKey );

                int connStatus = connection.getResponseCode();

                BlockingResult result = new BlockingResult();
                result.parameters = cacheParams;

                if( connStatus != 200 ) {
                    result.errorCode = connStatus;
                    result.message = connection.getResponseMessage();
                    return result;
                }

                InputStream in = connection.getInputStream();

                JsonReader reader = new JsonReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );

                reader.beginObject();

                while( reader.hasNext() ) {
                    String name = reader.nextName();

                    switch( name ) {
                        case "status":
                            result.errorCode = reader.nextInt();
                            break;
                        case "message":
                            result.message = reader.nextString();
                            break;
                        case "probability":
                            result.probability = reader.nextDouble();
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }

                reader.endObject();

                return result;
            } catch( Exception e ) {
                BlockingResult result = new BlockingResult();

                result.errorCode = 500;
                result.message = "An error occurred while attempting to send the request: " + e.getMessage();

                if( e instanceof SocketTimeoutException ||
                    e instanceof StrategyTimeoutException ) {
                    result.errorCode = 408;
                    result.message = e.getMessage();
                    return result;
                }

                LogInfo( result.message );
                LogError( Log.getStackTraceString( e ) );
                return result;
            } finally {
                if( connection != null ) connection.disconnect();
            }
        }

        private static String buildUrl ( String deviceId, String ipv4, String ipv6, String userAgent ) {
            Uri.Builder uri = Uri.parse( baseFraudURL )
                .buildUpon();

            if( ipv4 != null ) {
                uri.appendQueryParameter( "ip", ipv4 );
            }

            if( ipv6 != null ) {
                uri.appendQueryParameter( "ip", ipv6 );
            }

            if( userAgent != null ) {
                uri.appendQueryParameter( "userAgent", userAgent );
            }

            if( deviceId != null ) {
                uri.appendQueryParameter( "deviceId", deviceId );
            }

            return uri.build().toString();
        }

        @Override
        protected void onPostExecute ( BlockingResult result ) {
            if( result.hasError() ) {
                // don't cache errors
                LogError( String.format( "Error getting data: %s %s", result.errorCode, result.message ) );
                listener.onError( result.errorCode, result.message );
            } else {
                LogDebug( String.format( "Got com.pixalate.android.blocking result:\nStatus: %s\nError: %s\nProbability: %s", result.errorCode, result.message, result.probability ) );

                if( ttl > 0 ) {
                    LogDebug( String.format( "Caching result for %sms", ttl ) );
                    result.time = new Date().getTime() + ttl;
                    cachedResults.put( result.parameters, result );
                }

                if( result.probability > threshold ) {
                    listener.onBlock();
                } else {
                    listener.onAllow();
                }
            }
        }
    }

}

