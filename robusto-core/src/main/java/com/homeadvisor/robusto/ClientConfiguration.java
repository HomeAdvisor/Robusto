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

import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Encapsulates all the standard config options that make sense for all clients.
 * Goal is to provide sensible default values and allow child classes
 * to plug in their own configuration providers as needed, as well as different
 * strategies for naming configuration options or additional confugration options
 * that make sense for their technology stack.
 * <br/><br/>
 *
 */
public class ClientConfiguration
{
   /**
    * Number of command failures before the {@link com.homeadvisor.robusto.health.hystrix.HystrixHealthCheck}
    * reports unhealthy. Default is 1.
    */
   private int hystrixHealthFailures = 1;

   /**
    * Number of retries for retryable exceptions (default is 3). Note that this
    * includes the initial attempt, so a value of 3 really means the initial
    * attempt plus 2 retries.
    */
   private int numRetries = 3;

   /**
    * Defines if a command cache is enabled (default is true).
    */
   private boolean cacheEnabled = true;

   /**
    * Defines the type of {@link com.homeadvisor.robusto.cache.CommandCacheConfig}
    * to use (default is null, for no caching).
    */
   private String cacheType = null;

   /**
    * Defines the default {@link com.homeadvisor.robusto.cache.CommandCacheConfig}
    * configuration (default is null, which means no command caching). This class
    * uses the same config string for *all* command caches.
    */
   private String cacheConfig = null;

   private Map<String, HystrixCommandProperties.Setter> commandPropertiesMap = new HashMap<>();

   private Map<String, HystrixThreadPoolProperties.Setter> threadPropertiesMap = new HashMap<>();

   //
   // Getters and setters
   //

   /**
    * @deprecated Use {@link #getHystrixCommandProperties(String)} instead.
    */
   @Deprecated
   public int getHystrixTimeout()
   {
      return 8000;
   }

   public void setHystrixTimeout(int hystrixTimeout)
   {

   }

   /**
    * @deprecated Use {@link #getHystrixThreadPoolProperties(String)} instead.
    */
   @Deprecated
   public int getHystrixNumthreads()
   {
      return 4;
   }

   public void setHystrixNumthreads(int hystrixNumthreads)
   {

   }

   /**
    * @deprecated Use {@link #getHystrixCommandProperties(String)} instead.
    */
   @Deprecated
   public int getHystrixCircuitBreakerSleep()
   {
      return 5000;
   }

   public void setHystrixCircuitBreakerEnabled(boolean hystrixCircuitBreakerEnabled)
   {

   }

   /**
    * @deprecated Use {@link #getHystrixCommandProperties(String)} instead.
    */
   @Deprecated
   public boolean getHystrixCircuitBreakerEnabled()
   {
      return true;
   }

   public void setHystrixCircuitBreakerSleep(int hystrixCircuitBreakerSleep)
   {

   }

   /**
    * @deprecated Use {@link #getHystrixCommandProperties(String)} instead.
    */
   @Deprecated
   public int getHystrixMetricsWindowSize()
   {
      return 10000;
   }

   public void setHystrixMetricsWindowSize(int hystrixMetricsWindowSize)
   {

   }

   public int getHystrixHealthNumFailures()
   {
      return 1;
   }

   public int getNumRetries()
   {
      return numRetries;
   }

   /**
    * Allow number of retries to vary per command.
    * @param name Logical command name.
    * @return Number of retries for the given command.
    */
   public int getNumRetries(String name)
   {
      return numRetries;
   }

   public void setNumRetries(int numRetries)
   {
      this.numRetries = numRetries;
   }

   public boolean isCacheEnabled(String cacheName)
   {
      return cacheEnabled;
   }

   public void setCacheEnabled(String cacheName, boolean cacheEnabled)
   {
      this.cacheEnabled = cacheEnabled;
   }

   public String getCacheType(String cacheName)
   {
      return cacheType;
   }

   public void setCacheType(String cacheName, String cacheType)
   {
      this.cacheType = cacheType;
   }

   public String getCacheConfig(String cacheName)
   {
      return cacheConfig;
   }

   public void setCacheConfig(String cacheName, String cacheConfig)
   {
      this.cacheConfig = cacheConfig;
   }

   /**
    * Get the hystrix command properties for the given command name. By default
    * every command will get the same default settings, unless extending clients
    * override the behavior of {@link #buildCustomCommandProperties(String)}.
    * @param name Command name
    * @return Hystrix command properties to use for the given command name,
    * which may be the default.
    */
   public HystrixCommandProperties.Setter getHystrixCommandProperties(String name)
   {
      if(!commandPropertiesMap.containsKey(name))
      {
         commandPropertiesMap.put(name, buildCustomCommandProperties(name));
      }

      //
      // We shouldn't need the default here, but in the interest of trying
      // to avoid null properties we use it anyway.
      //

      return commandPropertiesMap.getOrDefault(name, buildDefaultCommandProperties());
   }

   /**
    * Extension point for building default Hystrix command properties.
    * @return New command properties with reasonable defaults. Command timeout
    * is 8000ms, fallback is disabled, circuit break sleep window is 5000ms, and
    * metrics reporting window size is 10000ms. Execution strategy is THREAD, which
    * should not be overridden unless you know what you're doing.
    */
   protected HystrixCommandProperties.Setter buildDefaultCommandProperties()
   {
      return HystrixCommandProperties.Setter()
            .withCircuitBreakerEnabled(true)
            .withCircuitBreakerSleepWindowInMilliseconds(5000)
            .withExecutionIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD)
            .withExecutionTimeoutInMilliseconds(8000)
            .withFallbackEnabled(false) //dont override unles you also override ApiCommand to have a fallback
            .withMetricsRollingPercentileBucketSize(10000)
            .withMetricsRollingPercentileEnabled(true);
   }

   /**
    * Extension point for building custom Hystrix command properties for a command name. The
    * default implementation is just delegate to {@link #buildDefaultCommandProperties()}.
    * @param name Name of the command group.
    * @return Command settings for the given command name.
    */
   protected HystrixCommandProperties.Setter buildCustomCommandProperties(String name)
   {
      return buildDefaultCommandProperties();
   }

   /**
    * Get the hystrix threadpool properties for the given command name. By default
    * every command will get the same default settings, unless extending clients
    * override the behavior of {@link #buildCustomThreadPoolProperties(String)}.
    * @param name Command name
    * @return Hystrix thread pool properties to use for the given command name,
    * which may be the default.
    */
   public HystrixThreadPoolProperties.Setter getHystrixThreadPoolProperties(String name)
   {
      if(!threadPropertiesMap.containsKey(name))
      {
         threadPropertiesMap.put(name, buildCustomThreadPoolProperties(name));
      }

      //
      // We shouldn't need the default here, but in the interest of trying
      // to avoid null properties we use it anyway.
      //

      return threadPropertiesMap.getOrDefault(name, buildDefaultThreadPoolProperties());
   }

   /**
    * Returns a reasonable default
    * @return for Hystrix thread pool settings. Size is 4 and rolling stats
    * window size is 10000ms.
    */
   protected HystrixThreadPoolProperties.Setter buildDefaultThreadPoolProperties()
   {
      return HystrixThreadPoolProperties.Setter()
            .withCoreSize(5)
            .withMetricsRollingStatisticalWindowInMilliseconds(10000);
   }

   /**
    * Extension point for building custom Hystrix thread pools for a command
    * name. The default implementation is just delegate to {@link #buildDefaultThreadPoolProperties()}.
    * @param name Name of the command group.
    * @return Thread pool settings for the given command.
    */
   protected HystrixThreadPoolProperties.Setter buildCustomThreadPoolProperties(String name)
   {
      return buildDefaultThreadPoolProperties();
   }
}
