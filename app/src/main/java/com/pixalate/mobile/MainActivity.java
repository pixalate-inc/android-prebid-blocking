package com.pixalate.mobile;

import android.os.Bundle;
import android.util.Log;

import com.mopub.common.MoPub;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubView;
import com.pixalate.android.blocking.BlockingStatusListener;
import com.pixalate.android.blocking.PixalateBlocking;
import com.pixalate.android.blocking.BlockingConfig;

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
                .withLogLevel( MoPubLog.LogLevel.INFO ).build(), () -> {

            BlockingConfig config = new BlockingConfig.Builder("your-api-key")
                    .setBlockingThreshold(0.75)
                    .build();

            PixalateBlocking.initialize(this, config);

            PixalateBlocking.setLogLevel( PixalateBlocking.LogLevel.DEBUG );

            PixalateBlocking.requestBlockStatus(new BlockingStatusListener() {
                @Override
                public void onBlock () {
                    Log.d( TAG, "Blocked ad load." );
                }

                @Override
                public void onAllow () {
                    // load your ads here!
                    adView.loadAd();
                }

                @Override
                public void onError ( int errorCode, String message ) {
                    // load your ads here, too.
                    adView.loadAd();
                }
            });
        });

        adView = findViewById( R.id.adview );
//        adView.setAutorefreshEnabled( false );
        adView.setAdUnitId( "b195f8dd8ded45fe847ad89ed1d016da" );


    }
}