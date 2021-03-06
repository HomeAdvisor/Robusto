/*
 * Copyright 2016 HomeAdvisor, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.homeadvisor.robusto;

import com.homeadvisor.robusto.cache.CommandCache;
import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestContext;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;

/**
 * Top level command for calling a remote API service. Commands should be
 * built using the provided {@link ApiCommand.Builder}
 * interface then invoked using one of the 3 Hystrix base methods:
 *
 * <ul>
 *    <li>run() for synchronous execution</li>
 *    <li>queue() for asynchrounous execution</li>
 *    <li>observe() for reactive execution</li>
 * </ul>
 * A sample use case using SpringRestClient and a constant URI provider:
 *
 * <pre>
 *
 * ApiCommand<CustomDTO> command = restCommand(
 *   new ConstantUriProvider("http://somehost.com/"),
 *   new SpringInstanceCallback<CustomDTO>()
 *   {
 *   @Override
 *      public CustomDTO runWithUrl(String url)
 *      {
 *         return getRestTemplate().getForObject(
 *            UriComponentsBuilder
 *               .fromUriString(url)
 *               .pathSegment("path").pathSegment("to").pathSegment("resource")
 *               .build()
 *               .toUriString(),
 *            CustomDTO.class
 *         );
 *      }
 *   }
 * ).build();
 *
 * // Now you can execute the command using one of the 3 Hystrix methods:
 *
 * CustomDTO = command.execute();
 * Future<CustomDTO> = command.queue();
 * Observable<CustomDTO> = command.observe();
 * </pre>
 *
 * Note that various builders above have more configuration than is shown.
 * Suitable defaults are provided but can be overridden as needed.
 */
public class ApiCommand<T> extends HystrixCommand<T> implements CommandContext
{
   private static final Logger LOG = LoggerFactory.getLogger(ApiCommand.class);

   /**
    * The Hystrix Request Context.
    */
   private HystrixRequestContext hystrixContext;

   /**
    * Used to find URLs of remote services.
    */
   protected final UriProvider<T> uriProvider;

   /**
    * Encompasses the command to be executed against a remote service.
    */
   protected final RemoteServiceCallback<T> remoteServiceCallback;

   /**
    * Spring Retry template which handles retrying failures and backoff policies.
    */
   protected final RetryTemplate retryTemplate;

   /**
    * Provides an interface for looking up values prior to calling remote
    * commands, as well as persisting the results of calls. This is an optional
    * field, and when null, means the remote command is always executed.
    * <br/>
    * This is different from {@link HystrixRequestCache} in that it persists the
    * results of a call for later use.
    */
   protected final CommandCache commandCache;

   /**
    * When a CommandCache is provided, this is the key that will be used for
    * both looking up and storing values. If this is null, then no command
    * caching is used.
    */
   protected final Object cacheKey;

   /**
    * Do not set this directly. It will be set by ApiCommandExecutionHook
    * to ensure correlation IDs get copied from calling threads to Hystrix
    * threads.
    * @deprecated This will be removed in future release. Please use the more
    * generic attributes to pass data between threads.
    */
   public String correlationId = null;

   /**
    * Logical name of this command. Used to name/configure Hystrix thread pools and
    * command settings.
    */
   private final String commandName;

   /**
    * Underlying map of variables from the builder class. Need to hold on to
    * these because we cannot initialize the HystrixRequestVariableDefault
    * until run() is called, because that's when the HystrixRequestContext
    * is initialized.
    */
   private final ConcurrentHashMap<String, Object> attributes;

   /**
    * Optional interceptor to fire if we end up looking up in the command
    * cache. Similar concept to the retryInterceptor Function.
    */
   private final Function<Supplier<Optional<T>>, Optional<T>> cacheInterceptor;

   /**
    * Mechanism that allows you to wrap the command execution in your own code,
    * for example to perform timing or logging operations. The interceptor will
    * be passed the {@link RemoteServiceCallback#run(String)} from each iteration
    * of the main command retry loop (including the first invocation). Your
    * interceptor should simply execute the Supplier and return its result, along
    * with any other operations you perform.
    * <br/><br/>
    * Warning! Your operations (logging, timing, etc) will be part of the main
    * Hystrix command execution, so make sure you account for them when setting
    * command timeouts. Try to keep your operations as quick as possible and
    * consider offloading them to asynchronous mechanisms whenever possible.
    */
   private final Function<Supplier<T>, T> retryInterceptor;

   /**
    * Initialize a new ApiCommand from a Builder. The builder will handle
    * validation of parameters.
    * @param builder Builder
    */
   public ApiCommand(Builder<T> builder)
   {
      super(Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(builder.commandGroup))
            .andCommandKey(HystrixCommandKey.Factory.asKey(builder.commandGroup))
            .andCommandPropertiesDefaults(builder.hystrixCommandProperties)
            .andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(builder.commandGroup))
            .andThreadPoolPropertiesDefaults(builder.hystrixThreadProperties)
      );

      this.uriProvider            = builder.uriProvider;
      this.remoteServiceCallback  = builder.remoteServiceCallback;
      this.retryTemplate          = builder.retryTemplate;
      this.commandCache           = builder.commandCache;
      this.cacheKey               = builder.cacheKey;
      this.cacheInterceptor       = builder.cacheInterceptor;
      this.retryInterceptor       = builder.retryInterceptor;

      //
      // Setup the fields that satisfy CommandContext
      //

      commandName = builder.commandGroup;
      attributes = builder.attributes;
      this.remoteServiceCallback.setContext(this);

      //
      // Add a Spring retry listener to handle logging of failures and retries
      //

      this.retryTemplate.registerListener(new ApiCommandLogger(this));
   }

   /**
    * This is the core of calling API services. The provided RemoteServiceCallback
    * is invoked inside a spring retry container. If a {@link CommandCache} has been
    * provided, this first attempts to lookup the values
    * @return Result of invoking {@link UriProvider#execute(RemoteServiceCallback)}.
    * @throws Exception
    */
   @Override
   protected T run() throws Exception
   {
      hystrixContext = HystrixRequestContext.initializeContext();

      try
      {
         return retryTemplate.execute(
               context ->
               {
                  Optional<T> cacheResult = null;

                  //
                  // Attempt cache lookup if applicable, and return the result
                  //

                  if(shouldUseCache())
                  {
                     LOG.debug("Attempting lookup of key {} from command cache", cacheKey.toString());

                     if(cacheInterceptor != null)
                     {
                        cacheResult = cacheInterceptor.apply(() -> commandCache.getCache(cacheKey));
                     }
                     else
                     {
                        cacheResult = commandCache.getCache(cacheKey);
                     }

                     if(cacheResult != null)
                     {
                        //
                        // Ready to return cached result
                        //

                        LOG.debug("Command cache hit, returning result from cache [isPresent = {}]", cacheResult.isPresent());

                        return cacheResult.get();
                     }
                     else
                     {
                        LOG.debug("Command cache miss, will call remote service");
                     }
                  }

                  //
                  // Command caching is either disabled or there was a miss
                  // so we have to invoke the remote call.
                  //

                  T result = null;

                  if(retryInterceptor != null)
                  {
                     result = retryInterceptor.apply(() -> uriProvider.execute(remoteServiceCallback));
                  }
                  else
                  {
                     result = uriProvider.execute(remoteServiceCallback);
                  }

                  //
                  // Put the result in the cache if applicable. This is wrapped
                  // in its own try/catch because failure to put into a cache
                  // should not cause overall command failures.
                  //

                  try
                  {
                     if(shouldUseCache())
                     {
                        LOG.debug("Putting result into command cache for key {}", cacheKey.toString());

                        commandCache.putCache(cacheKey, result);
                     }
                  }
                  catch(Exception e)
                  {
                     LOG.warn("Failed to put result into cache, command will still return normally", e);
                  }

                  //
                  // Now we can return the remote call result
                  //

                  return result;
               });
      }
      finally
      {
         hystrixContext.shutdown();
      }
   }

   /**
    * Get the command name that was set from the {@link Builder#withCommandGroup(String)}.
    * @return Command name.
    */
   public String getCommandName()
   {
      return commandName;
   }

   /**
    * Looks up the value associated with the given key from the attributes.
    * @param key Key to lookup in command data map.
    * @return Value associated with the given key, or null if none was set.
    */
   public Object getCommandAttribute(String key)
   {
      Object val = attributes.getOrDefault(key, null);

      LOG.debug("Returning attribute {}/{} for command {}", key, val, commandName);

      return val;
   }

   /**
    * Sets the given key/value pair for the underlying command attributes. Will
    * overwrite existing key if it exists.
    * @param key Key name to use.
    * @param val Value to associate with the key.
    */
   public void setCommandAttribute(String key, Object val)
   {
      if(key != null && val != null)
      {
         LOG.debug("Setting attribute {}/{} for command {}", key, val, commandName);
         attributes.put(key, val);
      }
      else
      {
         LOG.debug("Not setting attribute {}/{} for command {} due to NULL", key, val, commandName);
      }
   }

   /**
    * Removes the given key, if it exists, from the underlying command attributes.
    * this is a no-op if the key doesn't exist.
    * @param key Key to remove.
    */
   public void removeCommandAttribute(String key)
   {
      LOG.debug("Removing attribute {} for command {}", key, commandName);
      attributes.remove(key);
   }

   /**
    * Basically a clone of Throwables.getRootCause(), used to get the innermost
    * exception of a chained exception.
    * @param throwable
    * @return
    */
   private String unwrapException(Throwable throwable) {
      Throwable cause;
      while ((cause = throwable.getCause()) != null) {
         throwable = cause;
      }
      return throwable.getMessage();
   }

   /**
    * Helper method to determine if command caching should be used before and
    * after the command is executed.
    * @return True if command caching should be used to get and put values.
    */
   private boolean shouldUseCache()
   {
      return commandCache != null && cacheKey != null && commandCache.getConfig().isEnabled();
   }

   /**
    * Return a new {@link Builder} for constructing a new ApiCommand.
    * @return Builder
    */
   public static <T> Builder<T> builder()
   {
      return new Builder<T>();
   }

   /**
    * Builder class for fluently creating new {@link ApiCommand} objects.
    */
   public static class Builder<T>
   {
      //
      // Two required parameters are defaulted to null
      //

      protected UriProvider<T> uriProvider = null;

      protected RemoteServiceCallback<T> remoteServiceCallback = null;

      //
      // Settings for Hystrix (optional)
      //

      protected HystrixCommandProperties.Setter hystrixCommandProperties
            = HystrixCommandProperties.Setter()
            .withFallbackEnabled(false);

      protected HystrixThreadPoolProperties.Setter hystrixThreadProperties
            = HystrixThreadPoolProperties.Setter()
            .withCoreSize(4);

      protected String commandGroup = null;

      //
      // Spring retry settings (optional)
      //

      protected RetryTemplate retryTemplate = new RetryTemplate();

      protected int numberOfRetries = 3;

      protected BackOffPolicy backoffPolicy = null;

      protected Map<Class<? extends Throwable>, Boolean> exceptionMap = new HashMap<>();

      //
      // Command cache settings (optional)
      //

      protected CommandCache<?, ?, ?> commandCache = null;

      protected Object cacheKey = null;

      //
      // This map allows you to pass data from the calling thread to the
      // hystrix thread, via HystrixRequestVariable. Not to be confused
      // with HystrixCommandProperties. This will get set onto the ApiCommand's
      // HystrixRequestVariable.
      //

      protected ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();

      //
      // Interceptors for command execution (optional)
      //

      protected Function<Supplier<Optional<T>>, Optional<T>> cacheInterceptor = null;

      protected Function<Supplier<T>, T> retryInterceptor = null;

      public Builder()
      {

      }

      //
      // Builder methods
      //

      /**
       * <b>Required.</b> The {@link UriProvider} that will handle invoking the
       * remote request.
       * @param uriProvider
       * @return Builder
       */
      public Builder<T> withUriProvider(UriProvider<T> uriProvider)
      {
         this.uriProvider = uriProvider;
         return this;
      }

      /**
       * <b>Required.</b> The callback to execute when the ApiCommand is run.
       * @param remoteServiceCallback
       * @return Builder
       */
      public Builder<T> withRemoteServiceCallback(RemoteServiceCallback<T> remoteServiceCallback)
      {
         this.remoteServiceCallback = remoteServiceCallback;
         return this;
      }

      /**
       * <b>Optional.</b> Set of Hystrix Command Properties to use for the ApiCommand.
       * If not specified, normal Hystrix defaults will be used.
       * @param hystrixCommandProperties
       * @return Builder
       */
      public Builder<T> withHystrixCommandProperties(HystrixCommandProperties.Setter hystrixCommandProperties)
      {
         this.hystrixCommandProperties = hystrixCommandProperties;
         return this;
      }

      /**
       * <b>Optional.</b> Set of Hystrix Thread Pool Properties to use for the ApiCommand.
       * If not specified, normal Hystrix defaults will be used.
       * @param hystrixThreadProperties
       * @return Builder
       */
      public Builder<T> withHystrixThreadProperties(HystrixThreadPoolProperties.Setter hystrixThreadProperties)
      {
         this.hystrixThreadProperties = hystrixThreadProperties;
         return this;
      }

      /**
       * <i>Optional.</i> Defines a command group name for Hystrix thread pool
       * and metrics purposes. Default is just the name "ApiCommand".
       * @param commandGroup
       * @return Builder
       */
      public Builder<T> withCommandGroup(String commandGroup)
      {
         this.commandGroup = commandGroup;
         return this;
      }

      /**
       * <i>Optional.</i> Sets the number of retries. Default is 3. Note that
       * the first attempt counts as a retry so a value of 3 really means the
       * first attempt plus two retries.
       * @param numberOfRetries
       * @return
       */
      public Builder<T> withNumberOfRetries(int numberOfRetries)
      {
         this.numberOfRetries = numberOfRetries;
         return this;
      }

      /**
       * <i>Optional.</i> Spring backoff policy. Default is exponential backoff
       * with initial value of 500 milliseconds.
       * @param backoffPolicy
       * @return Builder
       */
      public Builder<T> withBackoffPolicy(BackOffPolicy backoffPolicy)
      {
         this.backoffPolicy = backoffPolicy;
         return this;
      }

      /**
       * <i>Optional.</i> Adds the given {@link RetryListener} to the Spring
       * retry template. This allows you to intercept failures to do things
       * like custom logging, persistence, etc. By default no listeners are
       * registered. Can be called as many times as needed. The order of
       * execution of listeners is up to Spring.
       * @param listener
       * @return
       */
      public Builder<T> withRetryListener(RetryListener listener)
      {
         if(listener != null)
         {
            retryTemplate.registerListener(listener);
         }

         return this;
      }

      /**
       * <i>Optional.</i> Set a command cache and key that can be used to lookup
       * values prior to remote calls being executed, and subsequently
       * storing the results of remote calls after they return.
       * @param commandCache CommandCache to use for lookup and persistence.
       * @param cacheKey Cache key
       * @return
       */
      public Builder<T> withCommandCache(CommandCache<?,?,?> commandCache, Object cacheKey)
      {
         this.commandCache = commandCache;
         this.cacheKey = cacheKey;
         return this;
      }

      /**
       * <i>Optional.</i> Can be called multiple times to define which
       * exception hierachies should be retried and not. This is passed
       * straight to Spring Retry. By default anything extending Throwable
       * will be retried, and anything extending NonRetryableApiCommandException
       * will <b>not</b> be retried.
       * @param t Some class that is rooted in Throwable
       * @param b Whether or not instances of type t should be retried.
       * @return Builder
       */
      public Builder<T> withExceptionRetryPolicy(Class<? extends Throwable> t, boolean b)
      {
         exceptionMap.put(t, Boolean.valueOf(b));
         return this;
      }

      //
      // The following methods are depracated and will be removed in a future
      // release. Please use the more direct with
      //
      /**
       * <i>Optional.</i> Command timeout in milliseconds. Default is 8000.
       * @param timeoutInMilliseconds
       * @return Builder
       * @deprecated Use {@link #withHystrixCommandProperties(HystrixCommandProperties.Setter)}.
       */
      public Builder<T> withCommandTimeoutInMilliseconds(int timeoutInMilliseconds)
      {
         this.hystrixCommandProperties.withExecutionTimeoutInMilliseconds(timeoutInMilliseconds);
         return this;
      }


      /**
       * <i>Optional.</i> Sets the number of threads for this command pool.
       * Default is 10.
       * @param numberOfThreads
       * @return Builder
       * @deprecated Use {@link #withHystrixThreadProperties(HystrixThreadPoolProperties.Setter)}.
       */
      public Builder<T> withNumberOfThreads(int numberOfThreads)
      {
         this.hystrixThreadProperties.withCoreSize(numberOfThreads);
         return this;
      }

      /**
       * <i>Optional.</i> Set the size (in ms) of the Hystrix rolling window.
       * Hystrix collects metrics about command failures and successes in
       * "buckets" that are the size specified. Default is 10000 ms.
       * @param rollingWindowMetricLengthMs
       * @return Builder
       * @deprecated Use {@link #withHystrixCommandProperties(HystrixCommandProperties.Setter)}.
       */
      public Builder<T> withRollingWindowMetricLengthMs(int rollingWindowMetricLengthMs)
      {
         this.hystrixCommandProperties.withMetricsRollingPercentileWindowInMilliseconds(rollingWindowMetricLengthMs);
         return this;
      }

      /**
       * <i>Optional.</i> Set the length of time (in ms) that a circuit remains
       * open after tripping. During this time all commands will be rejected
       * immediately. Default is 5000 ms.
       * @param circuitBreakerSleepWindow
       * @return Builder
       * @deprecated Use {@link #withHystrixCommandProperties(HystrixCommandProperties.Setter)}.
       */
      public Builder<T> withCircuitBreakerSleepWindow(int circuitBreakerSleepWindow)
      {
         this.hystrixCommandProperties.withCircuitBreakerSleepWindowInMilliseconds(circuitBreakerSleepWindow);
         return this;
      }

      /**
       * <i>Optional.</i> Set whether or not to use circuit breaker logic for
       * this command. When true, the Hystrix will open the circuit to fail
       * commands for a breif period of time when too many failures occur in
       * a small window of time. Default is true.
       * @param circuitBreakerEnabled
       * @return Builder
       * @deprecated Use {@link #withHystrixCommandProperties(HystrixCommandProperties.Setter)}.
       */
      public Builder<T> withCircuitBreakerEnabled(boolean circuitBreakerEnabled)
      {
         this.hystrixCommandProperties.withCircuitBreakerEnabled(circuitBreakerEnabled);
         return this;
      }

      /**
       * <i>Optional.</i> Adds a new key/value pair to the underyling {@link HystrixRequestVariable}.
       * This object maintains a ConcurrentHashMap and allows you to pass arbitrary
       * key/value pairs from the calling thread and make them available inside the
       * {@link RemoteServiceCallback} on the Hystrix thread.
       * @param key Key to lookup variable later.
       * @param val Value to set for the given key.
       * @return Builder
       */
      public Builder<T> withCommandVariable(String key, Object val)
      {
         attributes.put(key, val);
         return this;
      }

      /**
       * <i>Optional.</i> This mechanism is used to intercept every invocation of the
       * {@link CommandCache} operations (get and put). It gives you a chance to
       * do anything you like such as logging, timing, etc. Make sure that whatever
       * you do, it does not require a lot of time as it counts towards the overall
       * Hystrix retry time.
       * @param cacheInterceptor A Function that will be passed in the the CommandCache
       *                         that needs executing. It is the responsibility of the
       *                         Function to execute the supplier and return its result
       *                         in addition to anything else that it needs to do (logging,
       *                         timing, etc).
       * @return Builder
       */
      public Builder<T> withCacheInterceptor(Function<Supplier<Optional<T>>, Optional<T>> cacheInterceptor)
      {
         this.cacheInterceptor = cacheInterceptor;
         return this;
      }

      /**
       * <i>Optional.</i> This mechanism is used to intercept every invocation of the
       * main command retry loop, including the initial try. It gives you a chance to
       * do anything you like before or after the execution of the {@link UriProvider}
       * such as logging, timing, etc. Make sure that whatever you do, it does not
       * require a lot of time as it counts towards the overall Hystrix retry time.
       * @param retryInterceptor A Function that will be passed in the the UriProvider
       *                         that needs executing. It is the responsibility of the
       *                         Function to execute the supplier and return its result
       *                         in addition to anything else that it needs to do (logging,
       *                         timing, etc).
       * @return Builder
       */
      public Builder<T> withRetryInterceptor(Function<Supplier<T>, T> retryInterceptor)
      {
         this.retryInterceptor = retryInterceptor;
         return this;
      }

      //
      // Create a new ApiCommand
      //

      /**
       * Create a new ApiCommand from this builder. Performs some basic null
       * checking and may throw IllegalArgumentException if any required fields
       * have not been set.
       * @return New ApiCommand
       * @throws IllegalArgumentException If any required fields are null.
       */
      public ApiCommand<T> build()
      {
         if(this.uriProvider == null)
         {
            throw new IllegalArgumentException("UriProvider cannot be null");
         }

         if(this.remoteServiceCallback == null)
         {
            throw new IllegalArgumentException("RemoteServiceCallback cannot be null");
         }

         if(this.numberOfRetries <= 0)
         {
            LOG.warn("Number of retries cannot be less than or equal to zero! Please don't do that. Setting to 1 for now, but please fix this.");
            this.numberOfRetries = 1;
         }

         //
         // Initialize any optional fields that have not been set
         //

         if(commandGroup == null)
         {
            commandGroup = "ApiCommand";
         }

         if(backoffPolicy == null)
         {
            ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
            backOffPolicy.setInitialInterval(500L);
            retryTemplate.setBackOffPolicy(backOffPolicy);
         }

         //
         // Setup standard failure exception classes (all Throwables are retried
         // except for NonRetryableApiCommandException).
         //

         exceptionMap.put(NonRetryableApiCommandException.class, false);
         exceptionMap.put(Throwable.class,                       true);

         //
         // Set remaining fields as needed
         //

         SimpleRetryPolicy srp = new SimpleRetryPolicy(numberOfRetries, exceptionMap, true);
         retryTemplate.setRetryPolicy(srp);

         return new ApiCommand<>(this);
      }
   }

   /**
    * Implementation of Spring {@link RetryListener} that logs failures
    * for ApiCommands.
    */
   private class ApiCommandLogger implements RetryListener
   {
      private final ApiCommand apiCommand;

      public ApiCommandLogger(ApiCommand apiCommand)
      {
         this.apiCommand = apiCommand;
      }

      @Override
      public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback)
      {
         return true;
      }

      @Override
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable)
      {

      }

      @Override
      public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable)
      {
         String reason = throwable == null ? "Unknown" : unwrapException(throwable);
         LOG.warn(
               "Command {} has failed [attempt = {}, reason = {}]",
               apiCommand.getCommandKey().name(),
               context.getRetryCount() + 1,
               reason);
         }
   }
}
