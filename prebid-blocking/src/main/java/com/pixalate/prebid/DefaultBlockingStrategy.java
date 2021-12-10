package com.pixalate.prebid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.JsonReader;


import androidx.ads.identifier.AdvertisingIdClient;
import androidx.ads.identifier.AdvertisingIdInfo;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.concurrent.ExecutionException;

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

    public DefaultBlockingStrategy ( long cacheTTL ) {
        this.cacheTTL = cacheTTL;
    }

    public long getCacheTTL () {
        return cacheTTL;
    }

    public void setCacheTTL ( long cacheTTL ) {
        this.cacheTTL = cacheTTL;
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
                getDeviceIDImpl( context, ( String result ) -> {
                    PixalateBlocking.LogDebug( "Fetched deviceID: " + result );
                    if( result != null ) {
                        cachedDeviceID = result;
                        nextDeviceIDFetchTime = now + cacheTTL;
                    }
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching device ID..." );
            getDeviceIDImpl( context, callback );
        }
    }

    public void getDeviceIDImpl ( Context context, BlockingStrategyCallback callback ) {
        if( AdvertisingIdClient.isAdvertisingIdProviderAvailable( context ) ) {
            ListenableFuture<AdvertisingIdInfo> infoFuture = AdvertisingIdClient.getAdvertisingIdInfo( context );
            try {
                AdvertisingIdInfo info = infoFuture.get();
            } catch( ExecutionException | InterruptedException e ) {
                PixalateBlocking.LogDebug( "Failed to fetch device ID:" );
                PixalateBlocking.LogDebug( e.getMessage() );
            }
        }
    }

    @Override
    public final void getIPv4 ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking IPv4 cache..." );
            long now = new Date().getTime();
            if( nextIPv4FetchTime > now && cachedIPv4 != null ) {
                PixalateBlocking.LogDebug( "Using cached IPv4: " + cachedIPv4 );
                callback.done( cachedIPv4 );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching IPv4..." );
                getIPv4Impl( context, ( String result ) -> {
                    PixalateBlocking.LogDebug( "Fetched IPv4: " + result );
                    if( result != null ) {
                        cachedIPv4 = result;
                        nextIPv4FetchTime = now + cacheTTL;
                    }
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching IPv4..." );
            getIPv4Impl( context, callback );
        }
    }

    public void getIPv4Impl ( Context context, BlockingStrategyCallback callback ) {
        try {
            String ipEndpoint = "https://get-ipv4.adrta.com";
            URL url = new URL( ipEndpoint );

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod( "GET" );

            int connStatus = connection.getResponseCode();

            if( connStatus != 200 ) {
                throw new PixalateBlocking.RequestErrorException( connStatus, "Failed to fetch IP address" );
            }

            InputStream in = connection.getInputStream();

            JsonReader reader = new JsonReader( new InputStreamReader( in, StandardCharsets.UTF_8 ) );

            reader.beginObject();

            String ip = null;

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

            callback.done( ip );

            return;
        } catch ( MalformedURLException exc ) {
            PixalateBlocking.LogInfo( "Failed to create IP URL." );
        } catch ( IOException | PixalateBlocking.RequestErrorException exc ) {
            PixalateBlocking.LogError( "Failed to fetch IP Address" );
        }

        callback.done( null );
    }

    @Override
    public final void getIPv6 ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking IPv6 cache..." );
            long now = new Date().getTime();
            if( nextIPv6FetchTime > now && cachedIPv6 != null ) {
                PixalateBlocking.LogDebug( "Using cached IPv6: " + cachedIPv6 );
                callback.done( cachedIPv6 );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching IPv6..." );
                getIPv6Impl( context, ( String result ) -> {
                    PixalateBlocking.LogDebug( "Fetched IPv6: " + result );
                    if( result != null ) {
                        cachedIPv6 = result;
                        nextIPv6FetchTime = now + cacheTTL;
                    }
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching IPv6..." );
            getIPv6Impl( context, callback );
        }
    }

    public void getIPv6Impl ( Context context, BlockingStrategyCallback callback ) {
        callback.done( null );
    }

    @Override
    public final void getUserAgent ( Context context, BlockingStrategyCallback callback ) {
        if( cacheTTL > 0 ) {
            PixalateBlocking.LogDebug( "Checking user agent cache..." );
            long now = new Date().getTime();
            if( nextUserAgentFetchTime > now && cachedUserAgent != null ) {
                PixalateBlocking.LogDebug( "Using cached UserAgent: " + cachedUserAgent );
                callback.done( cachedUserAgent );
            } else {
                PixalateBlocking.LogDebug( "Cache missed, fetching user agent..." );
                getUserAgentImpl( context, ( String result ) -> {
                    PixalateBlocking.LogDebug( "Fetched user agent: " + result );
                    if( result != null ) {
                        cachedUserAgent = result;
                        nextUserAgentFetchTime = now + cacheTTL;
                    }
                });
            }
        } else {
            PixalateBlocking.LogDebug( "Cache is disabled, fetching user agent..." );
            getUserAgentImpl( context, callback );
        }
    }

    public void getUserAgentImpl ( Context context, BlockingStrategyCallback callback ) {
        callback.done( null );
    }
}
