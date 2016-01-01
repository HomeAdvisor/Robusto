/*
 * Copyright 2015 HomeAdvisor, Inc.
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

import com.homeadvisor.robusto.cache.CommandCacheConfig;

/**
 * Encapsulates all the standard config options that make sense for all clients.
 * Goal is to provide sensible default values and prefixes and allow child classes
 * to plug in their own configuration providers as needed, as well as different
 * strategies for naming configuration options or additional confugration options
 * that make sense for their technology stack.
 */
public class ClientConfiguration
{
   /**
    * HTTP client connection timeout in ms (default 2000).
    */
   private int connectTimeout = 2000;

   /**
    * HTTP client request timeout in ms (default 2000).
    */
   private int requestTimeout = 2000;

   /**
    * Overall Hystrix/Retry timeout in ms (default 8000). Must be big enough to
    * encompass all connetion and retry attempts.
    */
   private int hystrixTimeout = 8000;

   /**
    * Number of threads to use in the Hystrix thread pool (default is 10).
    */
   private int hystrixNumthreads = 10;

   /**
    * Determine if the Hystrix circuit breaker logic should be used (default
    * is true). If this is disabled, Hystrix will not reject commands after
    * any number of failures and you lose one of the main benefits of the
    * library. In other words, override with caution.
    */
   private boolean hystrixCircuitBreakerEnabled = true;

   /**
    * Amount of time a circuit stays open after it trips in ms (default is
    * 5000). During this time after a circuit trips due to too many failures
    * any command in the pool is automatically rejected.
    */
   private int hystrixCircuitBreakerSleep = 5000;

   /**
    * Size of the window for capturing metrics in ms (default is 10000).
    */
   private int hystrixMetricsWindowSize = 10000;

   /**
    * Number of retries for retryable exceptions (default is 3). Note that this
    * includes the initial attempt, so a value of 3 really means the initial
    * attempt plus 2 retries.
    */
   private int numRetries = 3;

   /**
    * Determine if all requests and subsequent responses should be logged at
    * the DEBUG logging level (default is false). The logging will include
    * headers and entity bodies for both requests and responses, so use with
    * caution as log files may fill up fast.
    */
   private boolean httpLoggingDebug = false;

   /**
    * Defines if a command cache is enabled (default is true).
    */
   private boolean cacheEnabled = true;

   /**
    * Defines the type of {@link CommandCacheConfig}
    * to use (default is null, for no caching).
    */
   private String cacheType = null;

   /**
    * Defines the default {@link CommandCacheConfig}
    * configuration (default is null, which means no command caching). This class
    * uses the same config string for *all* command caches.
    */
   private String cacheConfig = null;

   //
   // Getters and setters
   //

   public int getConnectTimeout()
   {
      return connectTimeout;
   }

   public void setConnectTimeout(int connectTimeout)
   {
      this.connectTimeout = connectTimeout;
   }

   public int getRequestTimeout()
   {
      return requestTimeout;
   }

   public void setRequestTimeout(int requestTimeout)
   {
      this.requestTimeout = requestTimeout;
   }

   public int getHystrixTimeout()
   {
      return hystrixTimeout;
   }

   public void setHystrixTimeout(int hystrixTimeout)
   {
      this.hystrixTimeout = hystrixTimeout;
   }

   public int getHystrixNumthreads()
   {
      return hystrixNumthreads;
   }

   public void setHystrixNumthreads(int hystrixNumthreads)
   {
      this.hystrixNumthreads = hystrixNumthreads;
   }

   public int getHystrixCircuitBreakerSleep()
   {
      return hystrixCircuitBreakerSleep;
   }

   public void setHystrixCircuitBreakerEnabled(boolean hystrixCircuitBreakerEnabled)
   {
      this.hystrixCircuitBreakerEnabled = hystrixCircuitBreakerEnabled;
   }

   public boolean getHystrixCircuitBreakerEnabled()
   {
      return hystrixCircuitBreakerEnabled;
   }

   public void setHystrixCircuitBreakerSleep(int hystrixCircuitBreakerSleep)
   {
      this.hystrixCircuitBreakerSleep = hystrixCircuitBreakerSleep;
   }

   public int getHystrixMetricsWindowSize()
   {
      return hystrixMetricsWindowSize;
   }

   public void setHystrixMetricsWindowSize(int hystrixMetricsWindowSize)
   {
      this.hystrixMetricsWindowSize = hystrixMetricsWindowSize;
   }

   public int getNumRetries()
   {
      return numRetries;
   }

   public void setNumRetries(int numRetries)
   {
      this.numRetries = numRetries;
   }

   public boolean isHttpLoggingDebug()
   {
      return httpLoggingDebug;
   }

   public void setHttpLoggingDebug(boolean httpLoggingDebug)
   {
      this.httpLoggingDebug = httpLoggingDebug;
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
}
