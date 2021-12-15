package com.pixalate.android.blocking;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests the BlockingConfig and BlockingConfig.Builder classes.
 */
@RunWith( AndroidJUnit4.class )
public class BlockingConfigTest {
    @Test
    public void reasonableBuilderInputsShouldReflectInBuiltObject () {
        BlockingConfig.Builder builder;
        BlockingConfig config;

        builder = new BlockingConfig.Builder( "some-api-key" );
        config = builder.build();
        assertEquals( "some-api-key", config.getApiKey() );

        builder = new BlockingConfig.Builder( "" ).setBlockingThreshold(0.5);
        config = builder.build();
        assertEquals( 0.5, config.getBlockingThreshold(), 0 );

        builder = new BlockingConfig.Builder( "" ).setTTL( 91210 );
        config = builder.build();
        assertEquals( 91210, config.getTTL() );

        builder = new BlockingConfig.Builder( "" ).setRequestTimeout( 5040 );
        config = builder.build();
        assertEquals( 5040, config.getRequestTimeout() );

        BlockingStrategy strat = new BlockingStrategy() {};
        builder = new BlockingConfig.Builder( "" ).setBlockingStrategy( strat );
        config = builder.build();
        assertEquals( strat, config.getBlockingStrategy() );
    }

    @Test
    public void timeoutShouldBeClampedToZeroIfNegative () {
        BlockingConfig config = new BlockingConfig.Builder( "" )
                .setRequestTimeout(-100)
                .build();

        assertEquals( config.getRequestTimeout(), 0 );
    }

    @Test(expected = IllegalArgumentException.class)
    public void ttlShouldThrowWhenLessThanZero () {
        BlockingConfig config = new BlockingConfig.Builder( "" )
                .setTTL(-100)
                .build();
    }
}
