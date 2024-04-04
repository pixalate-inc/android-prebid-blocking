Pixalate Pre-Bid Fraud Blocking SDK for Android
===

- [Pixalate Pre-Bid Fraud Blocking SDK for Android](#pixalate-pre-bid-fraud-blocking-sdk-for-android)
  - [Installation & Integration](#installation--integration)
    - [Maven Central](#maven-central)
  - [Authentication & Basic Configuration](#authentication--basic-configuration)
  - [Blocking Ads](#blocking-ads)
    - [Testing Responses](#testing-responses)
  - [Logging](#logging)
  - [Advanced Configuration](#advanced-configuration)
    - [Blocking Strategies](#blocking-strategies)
      - [Device ID](#device-id)
      - [IPv4 Address](#ipv4-address)
      - [User Agent](#user-agent)
      - [Parameter Caching](#parameter-caching)
    - [Custom Blocking Strategies](#custom-blocking-strategies)
      - [Overriding DefaultBlockingStategy](#overriding-defaultblockingstategy)
      - [Creating a Strategy From Scratch](#creating-a-strategy-from-scratch)

The Pixalate Pre-Bid Blocking SDK gives easy access to Pixalate's Ad Fraud API.

## Installation & Integration

### Maven Central

The latest version of the pre-built SDK is available in [Maven Central](https://mvnrepository.com/artifact/com.pixalate.android/prebid-blocking), and can be integrated into your project's `build.gradle`, or directly in your `pom.xml`.

```gradle
// build.gradle
dependencies {
  implementation 'com.pixalate.android:prebid-blocking:0.1.2
}
```

```xml
<!-- pom.xml -->
<dependency>
  <groupId>com.pixalate.android</groupId>
  <artifactId>prebid-blocking</artifactId>
  <version>0.1.2</version>
  <type>aar</type>
</dependency>
```

## Authentication & Basic Configuration

To use the Pixalate Blocking SDK, you must first initialize it. You can do this by calling `PixalateBlocking.initialize()` and passing in a configuration object with your API key.

```java
// in your app initialization code, such as MainActivity.java
// A sample configuration & initialization -- the values chosen for this example
// are not meaningful.
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingThreshold( 0.75 ) // set the com.pixalate.android.blocking threshold. A value in the range 0.75 - 0.9 is recommended.
    .setTTL( 1000 * 60 * 60 * 7 ) // set the TTL of the response cache in milliseconds, or set to 0 to disable the cache.
    .setRequestTimeout( 3000 ) // set the max amount of time to wait before aborting a com.pixalate.android.blocking request. 
    .build();

PixalateBlocking.initialize(this, config);
```

The configuration builder allows you to override the default configuration values:

Parameter Name    | Description | Default Value 
------------------|-------------|---------------:
blockingThreshold | The probability threshold at which blocking should occur.<br/>Normal range is anywhere from 0.75-0.9. | 0.75
ttl               | How long results should be cached before making another request. | 8 hours
requestTimeout    | How long requests are allowed to run before aborting. In the rare case of a network issue, this will help ensure the Pixalate SDK is not a bottleneck to running your ads. <br/>**Important Note:** This timeout applies to the entire request *including* strategy execution, not just the Pixalate API request. | 2 seconds
blockingStrategy | The blocking strategy used to retrieve device parameters such as device id and IP address | DefaultBlockingStrategy


## Blocking Ads

Once the SDK is set up, you can implement it into your ad loading logic. The SDK is framework and approach-agnostic.

The basic pattern for performing a block request is as follows. All listener methods are optional, but at the very least you should implement `onAllow`.

```java
// The most basic blocking request, displaying 
// all possible interface implementations.
// You only need to implement the methods you need for your use case.
PixalateBlocking.requestBlockStatus(new BlockingStatusListener () {
    @Override
    public void onAllow () {
      /* Load your ads here! */
    }

    @Override
    public void onBlock () {
      /* Log the event, or otherwise modify your app behavior accordingly. */
    }

    @Override
    public void onError (int errorCode, String message) {
      /* In the case of an unexpected error, it is recommended to
          loading ads here as well. */
    }
});
```

The SDK will retrieve the external IPv6 address of the device by utilizing a Pixalate endpoint. 

During development, it may be helpful to test both blocked and unblocked behavior. You can accomplish this using the alternate overload for `Pixalate.requestBlockStatus` that includes a `BlockingMode` parameter. You can pass `BlockingMode.DEFAULT` to use normal behavior, `BlockingMode.ALWAYS_BLOCK` to simulate a blocked response, or `BlockingMode.NEVER_BLOCK` to simulate a non-blocked response:


```java
// Pass the blocking mode as the first parameter to simulate different blocking conditions.
PixalateBlocking.requestBlockStatus(
  PixalateBlocking.BlockingMode.ALWAYS_BLOCK, 
  new BlockingStatusListener () { /* ... */ }
);
```

Debug mode requests execute normally except that they do not perform a real API call, and so can be used to test custom blocking strategies as well.

## Logging

The SDK supports multiple logging levels which can provide additional context when debugging. The current level can be set through `Pixalate.setLogLevel`, and defaults to `INFO`. Logging can be disabled entirely by setting the level to `NONE`.

```java
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingStrategy(new DefaultBlockingStrategy(1000 * 60 * 5)) // set the com.pixalate.android.blocking strategy TTL to 5 minutes.
```

## Advanced Configuration

### Blocking Strategies

Below is a contrived example of how to go about implementing such a strategy. `BlockingStrategy` will return `null` by default, so you only need to override the methods you intend to implement strategies for.

```java
static class CustomBlockingStrategy extends BlockingStrategy {
    @Override
    public String getIPv4 (Context context, BlockingStrategyCallback callback) {
        // The strategy implementations are executed in a background thread, so it is OK 
        // to use com.pixalate.android.blocking operations such as HttpsURLConnection.

If your app uses Google Play Services, this strategy will read the device's [Advertising ID](https://support.google.com/googleplay/android-developer/answer/6048248?hl=en). Otherwise, it will attempt to retrieve the device ID from `Telephony.getDeviceId()`, a method only valid on older phones. If this fails, it will return the value of `Settings.Secure.ANDROID_ID` hashed using MD5. Check out the [Fraud API documentation]() for more information about possible values.

#### IPv4 Address

The SDK will retrieve the external IPv4 address of the device by utilizing a Pixalate endpoint.  

#### User Agent

Although the pre-bid fraud API supports passing browser user agents, the concept of a user agent is nebulous when in an app context. For this reason, the default blocking strategy does not utilize user agents.

        callback.done( ipv4 );
    }
}
```

### Custom Blocking Strategies

If you have an alternate use case that the default strategies are not providing, you would like more control over how you retrieve the blocking parameters, or if you want to add or remove included parameters, you can create your own blocking strategy.

#### Overriding DefaultBlockingStategy

The simplest method is to extend `DefaultBlockingStrategy`, which carries over all default behavior, including caching.

When extending the DefaultBlockingStrategy, make sure to override the `-Impl` variety methods rather than the base methods so as to preserve caching behavior.

```java
// TestBlockingStrategy.java
static class TestBlockingStrategy extends DefaultBlockingStrategy {
    @Override
    public String getIPv4Impl (Context context, BlockingStrategyCallback callback) {
        callback.done(null);
    }
}

// Then, in your initialization code, pass your modified strategy
// into the setBlockingStrategy builder method.
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingStrategy(new TestBlockingStrategy())
    .build();
```

#### Creating a Strategy From Scratch

To create a custom strategy from scratch, you must extend the `BlockingStrategy` interface. All of the methods have default implementations returning null, meaning you only need to override the strategies you want to provide values for.

```java
// CustomBlockingStrategy.java

// A contrived custom strategy only implementing the IPv4 parameter --
// all other parameters will be null by default.
static class CustomBlockingStrategy extends BlockingStrategy {
    @Override
    public String getIPv4 (Context context, BlockingStrategyCallback callback) {
        // The strategy implementations are executed in a background thread, so it is OK 
        // to use blocking operations such as HttpsURLConnection.

The basic pattern for performing a block request is as follows. All listener methods are optional, but at the very least you should implement `onAllow`.

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        int connStatus = connection.getResponseCode();

        String ipv4 = /* do something with the response to extract IPv4... */;

        callback.done( ipv4 );
    }
}
```

```java
// Then, in your initialization code, pass your modified strategy
// into the setBlockingStrategy builder method.
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingStrategy(new CustomBlockingStrategy())
    .build();
```

**Important note:** To keep the core functionality as implementation agnostic as possible, default strategy caching behavior is self-contained within the `DefaultBlockingStrategy` class. If you implement your own blocking strategy from scratch using the `BlockingStrategy` interface, you will need to manage your own caching of parameters. The caching of API responses is always managed by the SDK, and is unaffected by the blocking strategy.

### Logging

The SDK supports multiple logging levels which can provide additional context when debugging. The current level can be set through `Pixalate.setLogLevel`, and defaults to `INFO`. Logging can be disabled entirely by setting the level to `NONE`.

```java
PixalateBlocking.setLogLevel( PixalateBlocking.LogLevel.DEBUG );
```
