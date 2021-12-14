package com.pixalate.mobile;

import android.os.Bundle;

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

        BlockingConfig config = new BlockingConfig.Builder("j14cHPTJTtpIaGJM3k1A7Fvuue6Ywyp4")
            .setBlockingThreshold(0.75)
            .build();

        PixalateBlocking.initialize(this, config);

        PixalateBlocking.setLogLevel( PixalateBlocking.LogLevel.DEBUG );

        PixalateBlocking.requestBlockStatus(new BlockingStatusListener() {
            @Override
            public void onBlock () {

            }

            @Override
            public void onAllow () {
                // load your ads here!
            }

            @Override
            public void onError ( int errorCode, String message ) {
                // load your ads here, too.
            }
        });
    }
}