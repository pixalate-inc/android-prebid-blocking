package com.pixalate.mobile;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubView;
import com.pixalate.prebid.BlockingStatusListener;
import com.pixalate.prebid.DefaultBlockingStrategy;
import com.pixalate.prebid.PixalateBlocking;
import com.pixalate.prebid.BlockingConfig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    static final String TAG = "PixalateBlockingExample";

    private MoPubView adView;

    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        Toolbar toolbar = findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );

        MoPub.initializeSdk( this, new SdkConfiguration.Builder( "b195f8dd8ded45fe847ad89ed1d016da" )
            .withLogLevel( MoPubLog.LogLevel.INFO ).build(), () -> Log.d( TAG, "Finished initializing MoPub SDK" ) );

        adView = findViewById( R.id.adview );
//        adView.setAutorefreshEnabled( false );
        adView.setAdUnitId( "b195f8dd8ded45fe847ad89ed1d016da" );

        BlockingConfig config = new BlockingConfig.Builder( "my-api-key" )
            .build();

        PixalateBlocking.initialize( this, config );

        PixalateBlocking.requestBlockStatus( new BlockingStatusListener() {
            @Override
            public void onBlock () {

            }

            @Override
            public void onAllow () {

            }

            @Override
            public void onError ( int errorCode, String message ) {

            }
        });
    }

    static class TestBlockingStrategy extends DefaultBlockingStrategy {

        public TestBlockingStrategy ( long cacheTTL ) {
            super( cacheTTL );
        }

        @Override
        public String getIPv4Impl ( Context context ) {
            return null;
        }
    }
}