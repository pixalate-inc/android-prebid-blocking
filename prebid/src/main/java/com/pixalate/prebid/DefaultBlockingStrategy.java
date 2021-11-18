package com.pixalate.prebid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.JsonReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
    public final String getDeviceID ( Context context ) {
        String deviceID;
        if( cacheTTL > 0 ) {
            long now = new Date().getTime();
            if( nextDeviceIDFetchTime > now && cachedDeviceID != null ) {
                deviceID = cachedDeviceID;
            } else {
                deviceID = getDeviceIDImpl( context );
                if( deviceID != null ) {
                    cachedDeviceID = deviceID;
                    nextDeviceIDFetchTime = now + cacheTTL;
                }
            }
        } else {
            deviceID = getDeviceIDImpl( context );
        }

        return deviceID;
    }

    @SuppressLint( { "MissingPermission", "HardwareIds" } )
    public String getDeviceIDImpl ( Context context ) {
        if( Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ) {
            try {
                return ( (TelephonyManager) context.getSystemService( Context.TELEPHONY_SERVICE ) ).getDeviceId();
            } catch( SecurityException ignored ) {
            }
        }

        return Settings.Secure.getString( context.getContentResolver(), Settings.Secure.ANDROID_ID );
    }

    @Override
    public final String getIPv4 ( Context context ) {
        String ipv4;
        if( cacheTTL > 0 ) {
            long now = new Date().getTime();
            if( nextIPv4FetchTime > now && cachedIPv4 != null ) {
                ipv4 = cachedIPv4;
            } else {
                ipv4 = getIPv4Impl( context );
                if( ipv4 != null ) {
                    cachedIPv4 = ipv4;
                    nextIPv4FetchTime = now + cacheTTL;
                }
            }
        } else {
            ipv4 = getIPv4Impl( context );
        }

        return ipv4;
    }

    public String getIPv4Impl ( Context context ) {
        try {
            String ipEndpoint = "https://get-ipv4.adrta.com";
            URL url = new URL( ipEndpoint );

            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

            connection.setRequestMethod( "GET" );
//            connection.setConnectTimeout(  );

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

            return ip;
        } catch ( MalformedURLException exc ) {
            PixalateBlocking.LogInfo( "Failed to create IP URL." );
        } catch ( IOException | PixalateBlocking.RequestErrorException exc ) {
            PixalateBlocking.LogError( "Failed to fetch IP Address" );
        }

        return null;
    }

    @Override
    public final String getIPv6 ( Context context ) {
        String ipv6;
        if( cacheTTL > 0 ) {
            long now = new Date().getTime();
            if( nextIPv6FetchTime > now && cachedIPv6 != null ) {
                ipv6 = cachedIPv6;
            } else {
                ipv6 = getIPv6Impl( context );
                if( ipv6 != null ) {
                    cachedIPv6 = ipv6;
                    nextIPv6FetchTime = now + cacheTTL;
                }
            }
        } else {
            ipv6 = getIPv6Impl( context );
        }

        return ipv6;
    }

    public String getIPv6Impl ( Context context ) {
        return null;
    }

    @Override
    public final String getUserAgent ( Context context ) {
        String userAgent;
        if( cacheTTL > 0 ) {
            long now = new Date().getTime();
            if( nextUserAgentFetchTime > now && cachedUserAgent != null ) {
                userAgent = cachedUserAgent;
            } else {
                userAgent = getUserAgentImpl( context );
                if( userAgent != null ) {
                    cachedUserAgent = userAgent;
                    nextUserAgentFetchTime = now + cacheTTL;
                }
            }
        } else {
            userAgent = getUserAgentImpl( context );
        }

        return userAgent;
    }

    public String getUserAgentImpl ( Context context ) {
        return null;
    }
}
