package com.pixalate.mobile;

import android.content.Context;
import android.os.Bundle;

import com.pixalate.prebid.BlockingStatusListener;
import com.pixalate.prebid.DefaultBlockingStrategy;
import com.pixalate.prebid.PixalateBlocking;
import com.pixalate.prebid.BlockingConfig;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_main );
        Toolbar toolbar = findViewById( R.id.toolbar );
        setSupportActionBar( toolbar );

        BlockingConfig config = new BlockingConfig.Builder( "my-api-key" )
            .setRequestTimeout( 3000 ) // set the max amount of time to wait before aborting a blocking request.
            .setBlockingThreshold( 0.75 ) // set the blocking threshold. A range from 0.75-0.9 is recommended. See the API documentation for more info.
            .setTTL( 1000 * 60 * 60 * 7 ) // set the TTL of the response cache, or set to 0 to disable the cache.
            .setBlockingStrategy( new DefaultBlockingStrategy( 1000 * 60 * 60 * 24 ) )
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
        } );
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