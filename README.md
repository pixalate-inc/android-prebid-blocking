Pixalate Pre-Bid Fraud Blocking SDK for Android
===

- [Pixalate Pre-Bid Fraud Blocking SDK for Android](#pixalate-pre-bid-fraud-blocking-sdk-for-android)
  - [Installation & Integration](#installation--integration)
    - [Maven Central](#maven-central)
  - [Authentication & Configuration](#authentication--configuration)
    - [Blocking Strategies](#blocking-strategies)
      - [Device ID](#device-id)
      - [IPv4 Address](#ipv4-address)
      - [IPv6 Address](#ipv6-address)
      - [User Agent](#user-agent)
      - [Caching](#caching)
    - [Custom Blocking Strategies](#custom-blocking-strategies)
  - [Blocking Ads](#blocking-ads)
    - [Testing Responses](#testing-responses)
    - [Logging](#logging)

The Pixalate Pre-Bid Blocking SDK gives easy access to pixalate's pre-bid fraud blocking APIs.

## Installation & Integration

### Maven Central

The latest version of the pre-built SDK is available in [Maven Central](http://example.com/XXXXXXX__PLACEHOLDER), and can be integrated into your project's `build.gradle`:

```gradle
dependencies {
  implementation 'com.pixalate.android:prebid:1.0.0
}
```

Or in your `pom.xml`:

```xml
<dependency>
  <groupId>com.pixalate.android</groupId>
  <artifactId>mobile</artifactId>
  <version>1.0.0</version>
  <type>aar</type>
</dependency>
```

## Authentication & Configuration

To use the Pixalate Blocking SDK, you must first initializing it by calling `PixalateBlocking.initialize()`, passing a configuration object.

The configuration object lets you override the default behavior of the SDK.

An example configuration is below.

```java
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingThreshold( 0.75 ) // set the blocking threshold. A value in the range 0.75 - 0.9 is recommended.
    .setTTL( 1000 * 60 * 60 * 7 ) // set the TTL of the response cache in milliseconds, or set to 0 to disable the cache.
    .setRequestTimeout( 3000 ) // set the max amount of time to wait before aborting a blocking request. 
    .build();

PixalateBlocking.initialize(this, config);
```

Parameter Name | Description | Default Value
---------------|-------------|-------------:
blockingThreshold | The probability threshold at which blocking should occur.<br/>Normal range is anywhere from 0.75-0.9. | 0.75
ttl               | How long results should be cached before making another request. | 8 hours
requestTimeout    | How long requests are allowed to run before aborting. In the rare case of a network issue, this will help ensure the Pixalate SDK is not a bottleneck to running your ads. | 2 seconds
blockingStrategy | The blocking strategy used to retrieve device parameters such as device id and IP address | DefaultBlockingStrategy

### Blocking Strategies

Pixalate provides some default behavior for collecting both the device ID and IPv4 address of the host device for blocking purposes.

#### Device ID

If the device uses API version 28 or lower, this strategy will attempt to retrieve the device ID from `Telephony.getDeviceId()`. If this fails, it will return the value of `Settings.Secure.ANDROID_ID`.

#### IPv4 Address

The SDK will retrieve the external IPv4 address of the device by utilizing a Pixalate endpoint.  

#### IPv6 Address

The pre-bid fraud API will support IPv6 soon, and default support for IPv6 will be integrated into the SDK at that time. 

#### User Agent

Although the pre-bid fraud API supports passing browser user agents, the concept of a user agent is nebulous when in an app context. For this reason, the default blocking strategy does not utilize user agents.

#### Caching

By default, the blocking strategy will mirror the TTL of the global config. This value can be overridden by passing a new DefaultBlockingStrategy object to the BlockingConfig.Builder:

```java
BlockingConfig config = new BlockingConfig.Builder("my-api-key")
    .setBlockingStrategy(new DefaultBlockingStrategy(1000 * 60 * 5)) // set the blocking strategy TTL to 5 minutes.
```

### Custom Blocking Strategies

If the default behavior is not working for your use case, you would like more control over how you retrieve the blocking parameters, or if you want to add or remove included parameters, you can create your own BlockingStrategy.

Below is a contrived example of how to go about implementing such a strategy. As it only implements getIPv4 and returns null for the other methods, IPv4 is the only parameter that will be included in requests.

```java
static class CustomBlockingStrategy extends BlockingStrategy {
    @Override
    public String getDeviceID (Context context) {
        return null;
    }

    @Override
    public String getIPv4 (Context context) {
        // The strategy implementations are executed in a background thread, so it is OK 
        // to use blocking operations such as HttpsURLConnection.

        URL url = new URL("some-ipv4-source-url");

        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        int connStatus = connection.getResponseCode();

        String ipv4 = /* do something with the response... */;

        return ipv4;
    }

    @Override
    public String getIPv6 (Context context) {
        return null;
    }

    @Override
    public String getUserAgent (Context context) {
        return null;
    }
}
```

The simplest method is to extend DefaultBlockingStrategy, which carries over the default behavior, including caching.

When extending the DefaultBlockingStrategy, make sure to override the `-Impl` variety methods so as to preserve caching behavior.

```java
static class TestBlockingStrategy extends DefaultBlockingStrategy {
    public TestBlockingStrategy (long cacheTTL) {
        super(cacheTTL);
    }

    @Override
    public String getIPv4Impl (Context context) {
        return null;
    }
}
```

---
**NOTE:** Default caching behavior for blocking parameters is implemented in  `DefaultBlockingStrategy`. If you implement your own blocking strategy using `BlockingStrategy`, you will need to manage your own caching of parameters. Blocking request caching and TTL is always managed by the SDK, and is unaffected by the blocking strategy.

---

## Blocking Ads

Once the SDK is set up, you can implement it into your ad loading logic. The SDK is framework and approach-agnostic.

The basic pattern for performing a block request is as follows:

```java
PixalateBlocking.requestBlockStatus(this, new BlockingStatusListener () {
    @Override
    public void onAllow () {
      /* Load your ads here! */
    }

    @Override
    public void onBlock () {
      /* Log the event or do nothing! */
    }

    @Override
    public void onError (int errorCode, String message) {
      /* In the case of an unexpected error, it is recommended to
         default to loading ads. */
    }
});
```

### Testing Responses

During development, it may be helpful to test both blocked and unblocked behavior. You can accomplish this using the alternate overload for `Pixalate.requestBlockStatus` that includes a `BlockingMode` parameter. You can pass `BlockingMode.DEFAULT` to use normal behavior, `BlockingMode.ALWAYS_BLOCK` to simulate a blocked response, or `BlockingMode.NEVER_BLOCK` to simulate a non-blocked response:

```java
PixalateBlocking.requestBlockStatus(this, PixalateBlocking.BlockingMode.ALWAYS_BLOCK, new BlockingStatusListener () {
    /* ... */
});
```

These debug responses execute normally up until actually executing the API call, and so can be used to test custom blocking strategies as well.

### Logging

The SDK supports multiple logging levels which can provide additional context when debugging. The current level can be set through `Pixalate.setLogLevel`, and defaults to `INFO`. Logging can be disabled entirely by setting the level to `NONE`.

```java
PixalateBlocking.setLogLevel( PixalateBlocking.LogLevel.DEBUG );
```