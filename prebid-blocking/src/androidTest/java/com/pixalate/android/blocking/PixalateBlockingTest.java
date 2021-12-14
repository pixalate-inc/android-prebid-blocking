package com.pixalate.android.blocking;

import static com.pixalate.android.blocking.PixalateBlocking.*;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Tests the PixalateBlocking class.
 */
@RunWith( AndroidJUnit4.class )
public class PixalateBlockingTest {

    @Before
    public void before () throws IllegalAccessException, NoSuchFieldException {
        Field initialized = PixalateBlocking.class.getDeclaredField( "initialized" );
        initialized.setAccessible( true );
        initialized.set( null, false );

        Field logLevel = PixalateBlocking.class.getDeclaredField( "logLevel" );
        logLevel.setAccessible( true );
        logLevel.set( null, LogLevel.INFO );

        Field globalConfig = PixalateBlocking.class.getDeclaredField( "globalConfig" );
        globalConfig.setAccessible( true );
        globalConfig.set( null, null );

        Field context = PixalateBlocking.class.getDeclaredField( "context" );
        context.setAccessible( true );
        context.set( null, null );

        Field cachedResults = PixalateBlocking.class.getDeclaredField( "cachedResults" );
        cachedResults.setAccessible( true );
        cachedResults.set( null, null );

        Field executor = PixalateBlocking.class.getDeclaredField( "executor" );
        executor.setAccessible( true );
        executor.set( null, null );

        Field queue = PixalateBlocking.class.getDeclaredField( "queue" );
        queue.setAccessible( true );
        queue.set( null, new ArrayBlockingQueue<>( 4 ) );
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateWhenRequestingBeforeInitialization () {
        requestBlockStatus(new BlockingStatusListener() {
            @Override
            public void onBlock() {

            }

            @Override
            public void onAllow() {

            }

            @Override
            public void onError(int errorCode, String message) {

            }
        });
    }

    @Test(expected = IllegalStateException.class)
    public void illegalStateWhenUpdatingGlobalConfigBeforeInitialization () {
        updateGlobalConfig( new BlockingConfig.Builder( "" ).build() );
    }

    @Test
    public void settingLogLevelShouldChangeLogLevel () {
        setLogLevel( LogLevel.DEBUG );
        assertEquals( getLogLevel(), LogLevel.DEBUG );
        setLogLevel( LogLevel.WARNING );
        assertEquals( getLogLevel(), LogLevel.WARNING );
        setLogLevel( LogLevel.ERROR );
        assertEquals( getLogLevel(), LogLevel.ERROR );
        setLogLevel( LogLevel.INFO );
        assertEquals( getLogLevel(), LogLevel.INFO );
        setLogLevel( LogLevel.NONE );
        assertEquals( getLogLevel(), LogLevel.NONE );
    }

    @Test
    public void logLevelShouldDefaultToInfo () {
        assertEquals( getLogLevel(), LogLevel.INFO );
    }

    @Test
    public void initializingShouldSetTheGlobalConfig () {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        BlockingConfig config = new BlockingConfig.Builder( "" )
                .build();

        PixalateBlocking.initialize( appContext, config );

        assertEquals( config, PixalateBlocking.getGlobalConfig() );
    }

    public void updatingTheGlobalConfigShouldSetTheGlobalConfig () {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();

        BlockingConfig config1 = new BlockingConfig.Builder( "" )
                .build();

        BlockingConfig config2 = new BlockingConfig.Builder( "" )
                .build();

        PixalateBlocking.initialize( appContext, config1 );

        PixalateBlocking.updateGlobalConfig( config2 );

        assertEquals( config2, PixalateBlocking.getGlobalConfig() );
    }
}