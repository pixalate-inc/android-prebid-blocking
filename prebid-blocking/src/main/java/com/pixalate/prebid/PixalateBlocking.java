package com.pixalate.prebid;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.JsonReader;
import android.util.Log;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import javax.net.ssl.HttpsURLConnection;

public final class PixalateBlocking {
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

    public enum BlockingMode {
        DEFAULT,
        ALWAYS_BLOCK,
        NEVER_BLOCK
    }

    static final String TAG = "PixalateBlocking";

    private static final String baseFraudURL = "https://dev-api.pixalate.com/api/v2/hosts/rpc/suspect?";

    static LogLevel logLevel = LogLevel.INFO;

    static int backoffCount = 5;


    PixalateBlocking () {}

    /**
     * Sets the level to which debug statements should be logged to the console.
     *
     * @param level The LogLevel to use.
     */
    public static void setLogLevel ( LogLevel level ) {
        logLevel = level;
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

    static HashMap<BlockingCacheParameters,BlockingResult> cachedResults = new HashMap<>();

    static BlockingConfig globalConfig;
    static boolean initialized;
    static WeakReference<Context> context;

    public static void initialize ( Context context, BlockingConfig config ) {
        setGlobalConfig( config );
        PixalateBlocking.context = new WeakReference<>( context );

        initialized = true;
    }

    public static void setGlobalConfig ( BlockingConfig config ) {
        if( config == null ) throw new InvalidParameterException( "Passed config cannot be null." );
        globalConfig = config;
    }

    @SuppressWarnings( "unused" )
    public static BlockingConfig getGlobalConfig () {
        return globalConfig;
    }

    /**
     * Requests a block status using the blocking strategy. If anything goes wrong with the request, eg. incorrect login details, it will
     * return an onError result in the listener. Otherwise, it will use the set threshold to compare probabilities and return a positive
     * or negative result in onAllow and onBlock respectively.
     *
     * @param listener   The listener will be called with the results of the request.
     */
    public static void requestBlockStatus ( BlockingStatusListener listener ) throws IllegalStateException {
        requestBlockStatus( BlockingMode.DEFAULT, listener );
    }

    /**
     * Requests a block status using the blocking strategy and the selected blocking mode.
     * It will then compare the result to the selected threshold, and call the appropriate method on the passed listener.
     * If an error occurs with the request, {@link com.pixalate.prebid.BlockingStatusListener#onError onError} will be called.
     * or negative result in onAllow and onBlock respectively.
     *
     * @param mode       The BlockingMode to utilize.
     * @param listener   The listener will be called with the results of the request.
     */
    public static void requestBlockStatus ( BlockingMode mode, BlockingStatusListener listener ) throws IllegalStateException {
        SendPreBidBlockingRequestTask task = new SendPreBidBlockingRequestTask( globalConfig.getTTL(), globalConfig.getBlockingThreshold(), listener );

        if( !initialized ) {
            throw new IllegalStateException( "You must set the global blocking config using `Pixalate.initialize` before requesting block status." );
        }

        Context ctx = context.get();

        if( ctx == null ) {
            throw new IllegalStateException( "Context is null, cannot proceed." );
        }

        task.execute( new BlockingRequestParameters( ctx, globalConfig.getApiKey(), globalConfig.getRequestTimeout(), mode, globalConfig.getBlockingStrategy() ) );
    }


    static class BlockingResult {
        String message = null;
        int errorCode = -1;
        double probability = -1;

        BlockingCacheParameters parameters;



        long time;

        public boolean hasError () {
            return errorCode > -1;
        }
    }

    static class RequestErrorException extends Exception {
        public int status;

        public RequestErrorException ( int status, String message ) {
            super( message );
            this.status = status;
        }
    }

    static class BlockingRequestParameters {
        WeakReference<Context> context;
        BlockingStrategy strategy;
        BlockingMode mode;
        String apiKey;
        int timeout;

        public BlockingRequestParameters ( Context context, String apiKey, int timeout, BlockingMode mode, BlockingStrategy strategy ) {
            this.context = new WeakReference<>( context );
            this.strategy = strategy;
            this.mode = mode;
            this.apiKey = apiKey;
            this.timeout = timeout;
        }
    }

    static class BlockingCacheParameters {
        final String ipv4;
        final String ipv6;
        final String deviceId;
        final String userAgent;
        final BlockingMode mode;

        public BlockingCacheParameters ( String deviceId, String ipv4, String ipv6, String userAgent, BlockingMode mode ) {
            this.deviceId = deviceId;
            this.ipv4 = ipv4;
            this.ipv6 = ipv6;
            this.userAgent = userAgent;
            this.mode = mode;
        }

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


    static class SendPreBidBlockingRequestTask extends AsyncTask<BlockingRequestParameters,Integer,BlockingResult> {

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
            Context context = param.context.get();

            int timeout      = param.timeout;
            String apiKey    = param.apiKey;
            String deviceId  = param.strategy.getDeviceID( context );
            String ipv4      = param.strategy.getIPv4( context );
            String ipv6      = param.strategy.getIPv6( context );
            String userAgent = param.strategy.getUserAgent( context );

            BlockingMode mode = param.mode;

            BlockingCacheParameters cacheParams = new BlockingCacheParameters( deviceId, ipv4, ipv6, userAgent, mode );

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

            if( mode != BlockingMode.DEFAULT ) {
                LogDebug( "Using custom blocking mode: " + mode );
                BlockingResult result = new BlockingResult();
                result.parameters = cacheParams;
                result.probability = mode == BlockingMode.ALWAYS_BLOCK ? 1 : 0;
                return result;
            }

            for( int i = 0; i < backoffCount; i++ ) {
                try {
                    URL url = new URL( buildUrl( deviceId, ipv4, ipv6, userAgent ) );

                    LogDebug( "Sent URL: " + url.toString() );

                    HttpsURLConnection connection = null;

                    try {
                        connection = (HttpsURLConnection) url.openConnection();

                        connection.setRequestMethod( "GET" );
                        connection.setConnectTimeout( timeout );
                        if( apiKey != null ) connection.setRequestProperty( "X-Api-Key", apiKey );

                        int connStatus = connection.getResponseCode();

                        // There shouldn't be any need for redirect management w/ this api.
                        if( connStatus != 200 ) {
                            throw new RequestErrorException( connStatus, "HTTPS Error" );
                        }

                        InputStream in = connection.getInputStream();

                        JsonReader reader = new JsonReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );

                        reader.beginObject();

                        BlockingResult result = new BlockingResult();
                        result.parameters = cacheParams;

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
                    } finally {
                        if( connection != null ) connection.disconnect();
                    }
                } catch( Exception e ) {
                    BlockingResult result = new BlockingResult();

                    result.errorCode = 500;
                    result.message = "An unknown error occurred when attempting to send the request.";

                    if( e instanceof SocketTimeoutException ) {
                        result.errorCode = 408;
                        result.message = "The request timed out.";
                        return result;
                    }

                    if( e instanceof RequestErrorException ) {
                        result.errorCode = ( (RequestErrorException) e ).status;

                        if( result.errorCode == 401 || result.errorCode == 403 ) {
                            result.message = "Incorrect authentication details.";
                        }

                        // @todo: there are perhaps other statuses we need to cover for
                        return result;
                    }

                    if( i == backoffCount - 1 ) {
                        LogInfo( result.message );
                        LogError( Log.getStackTraceString( e ) );
                        return result;
                    } else {
                        try {
                            Thread.sleep( ( (int) Math.round( Math.pow( 2, i + 1 ) ) * 1000 ) );
                        } catch( InterruptedException ignored ) {
                        }
                    }
                }
            }

            return null;
        }

        private static String buildUrl ( String deviceId, String ipv4, String ipv6, String userAgent ) {
            Uri.Builder uri = Uri.parse( baseFraudURL )
                .buildUpon();

            if( ipv4 != null ) {
                uri.appendQueryParameter( "ip", ipv4 );
            }

            if( ipv6 != null ) {
                uri.appendQueryParameter( "ip", ipv4 );
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
                LogDebug( String.format( "Got blocking result:\nStatus: %s\nError: %s\nProbability: %s", result.errorCode, result.message, result.probability ) );

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

