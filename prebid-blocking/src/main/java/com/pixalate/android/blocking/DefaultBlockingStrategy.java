package com.pixalate.android.blocking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.JsonReader;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

/**
 * Provides some default strategires for common use-cases.
 * Collects Device ID from the Telephony service or the Android ID, depending
 * on support level.
 */
public class DefaultBlockingStrategy implements BlockingStrategy {

    private long nextDeviceIDFetchTime;
    private long nextIPv4FetchTime;
    private long nextIPv6FetchTime;
    private long nextUserAgentFetchTime;

    private String cachedDeviceID;
    private String cachedIPv4;
    private String cachedIPv6;
    private String cachedUserAgent;

    private long cacheTTL;
    private int requestTimeout = -1;

    public DefaultBlockingStrategy ( long cacheTTL ) {
        this.cacheTTL = cacheTTL;
    }

    public DefaultBlockingStrategy ( long cacheTTL, int requestTimeout ) {
        this.cacheTTL = cacheTTL;

        this.setRequestTimeout( requestTimeout );
    }

    public long getCacheTTL () {
        return cacheTTL;
    }
    public void setCacheTTL ( long cacheTTL ) {
        this.cacheTTL = cacheTTL;
    }

    public int getRequestTimeout() {
        return Math.max( requestTimeout, 0 );
    }
    public void setRequestTimeout( int requestTimeout ) {
        if( requestTimeout < 0 ) throw new InvalidParameterException( "Request timeout cannot be less than 0." );
        this.requestTimeout = requestTimeout;
    }

    @Override
    public final void getDeviceID ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking device ID cache..." );
            long now = new Date().getTime();
            if( nextDeviceIDFetchTime > now && cachedDeviceID != null ) {
                PixalateBlocking.LogDebug( "Using cached deviceID: " + cachedDeviceID );
                callback.done( cachedDeviceID );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching device ID..." );
                getDeviceIDImpl( context, result -> {
                    PixalateBlocking.LogDebug( "Fetched deviceID: " + result );
                    if( result != null ) {
                        cachedDeviceID = result;
                        nextDeviceIDFetchTime = now + cacheTTL;
                    }
                    callback.done( result );
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching device ID..." );
            getDeviceIDImpl( context, callback );
        }
    }

    @SuppressLint( { "MissingPermission", "HardwareIds" } )
    public void getDeviceIDImpl ( Context context, BlockingStrategyCallback callback ) {
        try {
            Class<?> client = context.getClassLoader().loadClass( "com.google.android.gms.ads.identifier.AdvertisingIdClient" );

            Method getAdvertisingIdInfo = client.getMethod( "getAdvertisingIdInfo", Context.class );

            Object infoResult = getAdvertisingIdInfo.invoke( null, context );

            if( infoResult != null ) {
                String adId = (String) infoResult.getClass().getMethod( "getId" ).invoke( infoResult );

                if( adId != null ) {
                    callback.done( adId );
                    return;
                }
            }
        } catch( ClassNotFoundException e ) {
            PixalateBlocking.LogDebug( "GMS not enabled for this app, unable to fetch GMS ad ID." );
        } catch( Exception e ) {
            PixalateBlocking.LogDebug( "Failed to fetch GMS device ID: " + e.getMessage() );
        }

        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ) {
            try {
                String deviceId = ( (TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE ) ).getDeviceId();

                if( deviceId != null ) {
                    callback.done( deviceId );
                    return;
                }
            } catch( SecurityException ignored ) {
            }
        }

        try {
            MessageDigest m = MessageDigest.getInstance( "MD5" );
            String aid = Settings.Secure.getString( context.getContentResolver(), Settings.Secure.ANDROID_ID );
            if( aid != null ) {
                m.update( aid.getBytes(), 0, aid.length() );
                String digestedAndroidId = new BigInteger( 1, m.digest() ).toString( 16 );

                callback.done( digestedAndroidId );
                return;
            }
        } catch( NoSuchAlgorithmException ignored ) {}

        callback.done( null );
    }

    @Override
    public final void getIPv4 ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking IPv4 address cache..." );
            long now = new Date().getTime();
            if( nextIPv4FetchTime > now && cachedIPv4 != null ) {
                PixalateBlocking.LogDebug( "Using cached IPv4 address: " + cachedIPv4 );
                callback.done( cachedIPv4 );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching IPv4 address..." );
                getIPv4Impl( context, ( result ) -> {
                    PixalateBlocking.LogDebug( "Fetched IPv4 address: " + result );
                    if( result != null ) {
                        cachedIPv4 = result;
                        nextIPv4FetchTime = now + cacheTTL;
                    }

                    callback.done( result );
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching IPv4 address..." );
            getIPv4Impl( context, callback );
        }
    }

    public void getIPv4Impl ( Context context, BlockingStrategyCallback callback ) {
        String ip = null;

        try {
            String ipEndpoint = "https://get-ipv4.adrta.com";
            URL url = new URL( ipEndpoint );

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod( "GET" );
            if( requestTimeout > 0 ) {
                connection.setConnectTimeout( requestTimeout );
            }

            int connStatus = connection.getResponseCode();

            if( connStatus != 200 ) {
                throw new PixalateBlocking.HTTPException( connStatus, "Failed to fetch IP address" );
            }

            InputStream in = connection.getInputStream();

            JsonReader reader = new JsonReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );

            reader.beginObject();

            while( reader.hasNext() ) {
                String name = reader.nextName();

                if( "ip".equals( name ) ) {
                    ip = reader.nextString();
                    reader.close();
                    break;
                } else {
                    reader.skipValue();
                }
            }

            in.close();
        } catch ( MalformedURLException exc ) {
            PixalateBlocking.LogInfo( "Failed to create IP URL." );
        } catch ( IOException | PixalateBlocking.HTTPException exc ) {
            PixalateBlocking.LogError( "Failed to fetch IP Address" );
        } finally {
            callback.done( ip );
        }
    }

//    @Override
//    public final void getIPv6 ( Context context, BlockingStrategyCallback callback ) {
//        if( cacheTTL > 0 ) {
//            PixalateBlocking.LogDebug( "Checking IPv6 address cache..." );
//            long now = new Date().getTime();
//            if( nextIPv6FetchTime > now && cachedIPv6 != null ) {
//                PixalateBlocking.LogDebug( "Using cached IPv6 address: " + cachedIPv6 );
//                callback.done( cachedIPv6 );
//            } else {
//                PixalateBlocking.LogDebug( "Cache missed, fetching IPv6 address..." );
//                getIPv6Impl( context, ( result ) -> {
//                    PixalateBlocking.LogDebug( "Fetched IPv6 address: " + result );
//                    if( result != null ) {
//                        cachedIPv6 = result;
//                        nextIPv6FetchTime = now + cacheTTL;
//                    }
//
//                    callback.done( result );
//                });
//            }
//        } else {
//            PixalateBlocking.LogDebug( "Cache is disabled, fetching IPv6 address..." );
//            getIPv6Impl( context, callback );
//        }
//    }
//
//    public void getIPv6Impl ( Context context, BlockingStrategyCallback callback ) {
//        callback.done( null );
//    }

    @Override
    public final void getUserAgent ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking user agent cache..." );
            long now = new Date().getTime();
            if( nextUserAgentFetchTime > now && cachedUserAgent != null ) {
                PixalateBlocking.LogDebug( "Using cached user agent: " + cachedIPv6 );
                callback.done( cachedUserAgent );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching user agent..." );
                getUserAgentImpl( context, ( String result ) -> {
                    PixalateBlocking.LogDebug( "Fetched user agent: " + result );
                    if( result != null ) {
                        cachedUserAgent = result;
                        nextUserAgentFetchTime = now + cacheTTL;
                    }

                    callback.done( result );
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching user agent..." );
            getUserAgentImpl( context, callback );
        }
    }

    public void getUserAgentImpl ( @NonNull Context context, @NonNull BlockingStrategyCallback callback ) {
        callback.done( null );
    }
}
