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
package com.homeadvisor.robusto.spring;

import com.homeadvisor.robusto.*;

import com.homeadvisor.robusto.cache.CommandCache;
import com.homeadvisor.robusto.spring.interceptor.AcceptHeaderInterceptor;
import com.homeadvisor.robusto.spring.interceptor.RequestResponseLogInterceptor;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;

/**
 * Base class for API clients that uses Spring Rest for invoking HTTP commands.
 */
@Component
public abstract class SpringRestClient extends AbstractApiClient
{
   private final static Logger LOG = LoggerFactory.getLogger(SpringRestClient.class);

   /**
    * A set of delimeters to use when building useful command names. See {@link
    * #buildCommandGroupName()}. These delimeters will not show up in the final
    * command name, and will be used to identity characters that will made capital.
    */
   private final static HashSet<Character> SERVICE_NAME_DELIMIETERS = new HashSet<>();

   /**
    * A set of method names to ignore when trying to build a useful command
    * name. See {@link #buildCommandGroupName()}. This is populated during
    * {@link #setup()} and you can add more names if needed. By default it
    * will ignore Thread.getStackTrace(), SpringRestClient.buildCommandGroupName,
    * and any of the restCommand helper methods.
    */
   private final static HashSet<String> IGNORED_METHODS_FOR_COMMAND_NAME = new HashSet<>();

   /**
    * Used to issue HTTP requests. Lazily intialized first time it is needed.
    */
   private RestTemplate restTemplate;

   /**
    * Container for client configuration. Can be overridden by child classes tp
    * plug in metrics from different sources.
    */
   protected SpringClientConfiguration config;

   /**
    * Sets up the RestTemplate for making HTTP calls.
    */
   @PostConstruct
   protected void setup() throws Exception
   {
      SERVICE_NAME_DELIMIETERS.add('-');
      SERVICE_NAME_DELIMIETERS.add('.');
      SERVICE_NAME_DELIMIETERS.add('_');

      IGNORED_METHODS_FOR_COMMAND_NAME.add("getStackTrace");
      IGNORED_METHODS_FOR_COMMAND_NAME.add("buildCommandGroupName");
      IGNORED_METHODS_FOR_COMMAND_NAME.add("restCommand");

      if(restTemplate == null)
      {
         try
         {
            int connectTimeout = getConfiguration().getConnectTimeout();
            int requestTimeout = getConfiguration().getRequestTimeout();

            LOG.info("Initializing RestTemplate with connect/read timeouts {}/{} ms", connectTimeout, requestTimeout);

            //
            // Update timeouts for underlying HTTP client
            //

            SimpleClientHttpRequestFactory scrf = new SimpleClientHttpRequestFactory();
            scrf.setConnectTimeout(connectTimeout);
            scrf.setReadTimeout(requestTimeout);

            //
            // Wrap the default request factory in a BufferingClientHttpRequestFactory
            // which allows us to read response bodies multiple times. This is needed
            // because some interceptors will need to consume the body before the final
            // response gets to the caller.
            //

            restTemplate = new RestTemplate(new BufferingClientHttpRequestFactory(scrf));

            //
            // Add some standard interceptors for all clients
            //

            restTemplate.getInterceptors().add(new AcceptHeaderInterceptor(getSpringConfiguration().getDefaultAcceptTypes()));
            restTemplate.getInterceptors().add(new RequestResponseLogInterceptor(getConfiguration().isHttpLoggingDebug()));
         }
         catch(Exception e)
         {
            LOG.error("Failed to initialize RestTemplate", e);
         }
      }
   }

   @PreDestroy
   protected void close()
   {
      // Nothing to do
   }

   /**
    * Utility method for building new {@link ApiCommand}s with most
    * of the boiler plate already setup. This method will <em>NOT</em>
    * perform any command caching.
    * @param uriProvider Provider getting service URIs.
    * @param callback The callback to execute.
    * @return Result of remote method call.
    */
   public <T> ApiCommand.Builder<T> restCommand(
         UriProvider<T> uriProvider,
         SpringInstanceCallback<T> callback)
   {
      return restCommand(uriProvider, callback, null);
   }

   /**
    * Utility method for building new {@link ApiCommand}s with most
    * of the boiler plate already setup. This method will <em>NOT</em>
    * perform any command caching.
    * @param uriProvider Provider getting service URIs.
    * @param callback The callback to execute.
    * @param listener Optional spring retry listener to intercept retries.
    * @return Result of remote method call.
    */
   public <T> ApiCommand.Builder<T> restCommand(
         UriProvider<T> uriProvider,
         SpringInstanceCallback<T> callback,
         RetryListener listener)
   {
      return restCommand(uriProvider, callback, listener, null, null);
   }

   /**
    * Utility method for building new {@link ApiCommand}s with most
    * of the boiler plate already setup. This method <em>MAY</em> do
    * command caching, but only if both cacheKey and commandCache
    * are not null.
    * @param uriProvider Provider getting service URIs.
    * @param callback The callback to execute.
    * @param listener Optional spring retry listener to intercept retries.
    * @param cacheKey Optional key to use for the CommandCache.
    * @param commandCache Optional CommandCache to use if desired.
    * @return Result of remote method call.
    */
   public <T> ApiCommand.Builder<T> restCommand(
         UriProvider<T> uriProvider,
         SpringInstanceCallback<T> callback,
         RetryListener listener,
         Object cacheKey,
         CommandCache<?,?,?> commandCache
         )
   {
      return ApiCommand.<T>builder()
            .withHystrixCommandProperties(HystrixCommandProperties.Setter()
                  .withCircuitBreakerEnabled(getConfiguration().getHystrixCircuitBreakerEnabled())
                  .withCircuitBreakerSleepWindowInMilliseconds(getConfiguration().getHystrixCircuitBreakerSleep())
                  .withMetricsRollingPercentileBucketSize(getConfiguration().getHystrixMetricsWindowSize())
                  .withExecutionTimeoutInMilliseconds(getConfiguration().getHystrixTimeout()))
            .withHystrixThreadProperties(HystrixThreadPoolProperties.Setter()
                  .withCoreSize(getConfiguration().getHystrixNumthreads()))
            .withNumberOfRetries(getConfiguration().getNumRetries())
            .withUriProvider(uriProvider)
            .withCommandGroup(buildCommandGroupName())
            .withRetryListener(listener)
            .withRemoteServiceCallback(callback)
            .withCommandCache(commandCache, cacheKey);
   }

   /**
    * Return a fully intialized {@link RestTemplate} that can be used to
    * invoke remote HTTP commands.
    * @return Fully initialized RestTemplate.
    */
   public RestTemplate getRestTemplate()
   {
      return restTemplate;
   }

   /**
    * Overridden to return a {@link SpringClientConfiguration}.
    * @return Spring client specific configuration data.
    */
   @Override
   protected ClientConfiguration getConfiguration()
   {
      if(config == null)
      {
         LOG.info("Creating new SpringClientConfiguration");
         config = new SpringClientConfiguration();
      }
      return config;
   }

   /**
    * Helper method to save callers the trouble of casting the configuration
    * class.
    * @return Fully initialized SpringClientConfiguration data.
    */
   protected SpringClientConfiguration getSpringConfiguration()
   {
      return (SpringClientConfiguration)getConfiguration();
   }

   /**
    * Hook for building command group names for ApiCommand, which are used
    * for both Hystrix command group and thread pool. You can either override
    * this method in your own client, or call {@link
    * com.homeadvisor.robusto.ApiCommand.Builder#withCommandGroup(String)}
    * with your desired name.
    * <br/><br/>
    * This implementation uses a combination of getServiceName() + the calling
    * function to build a meaningful name. It will delegate both the service
    * and method names to {@link #capitalizeName(String)} to format them nicely.
    */
   protected String buildCommandGroupName()
   {
      //
      // Convert service name to camel case string with any punctuation.
      // The first character becomes captialized, and then we use hypens,
      // underscores, and periods to find other characters to capitalize.
      //

      String prefix = capitalizeName(getServiceName());

      String suffix = "";

      for(StackTraceElement ste : Thread.currentThread().getStackTrace())
      {
         if(!IGNORED_METHODS_FOR_COMMAND_NAME.contains(ste.getMethodName()))
         {
            suffix = "." + capitalizeName(ste.getMethodName());
            break;
         }
      }

      return prefix + suffix;
   }

   /**
    * This takes an input string and does two things to it: capitalize the first
    * character and any other that follows one of the characters in SERVICE_NAME_DELIMIETERS;
    * and it removes any character in that matches the SERVICE_NAME_DELIMIETERS set. As an
    * example, the service name my-cool-service would convert to MyCoolService.
    * @param input Name to convert
    * @return Converted name
    */
   private String capitalizeName(String input)
   {
      StringBuilder sb = new StringBuilder();

      boolean capitalizeNext = true;
      for(int i = 0 ; i < input.length(); i++)
      {
         if(capitalizeNext)
         {
            sb.append(String.valueOf(input.charAt(i)).toUpperCase());
            capitalizeNext = false;
         }
         else
         {
            if(SERVICE_NAME_DELIMIETERS.contains(input.charAt(i)))
            {
               capitalizeNext = true;
            }
            else
            {
               sb.append(input.charAt(i));
            }
         }
      }

      return sb.toString().intern();
   }
}
