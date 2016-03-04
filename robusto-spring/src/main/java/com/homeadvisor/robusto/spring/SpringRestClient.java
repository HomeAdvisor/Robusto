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
package com.homeadvisor.robusto.spring;

import com.homeadvisor.robusto.*;

import com.homeadvisor.robusto.cache.CommandCache;
import com.homeadvisor.robusto.spring.config.SpringCommandProperties;
import com.homeadvisor.robusto.spring.config.SpringThreadPoolProperties;
import com.homeadvisor.robusto.spring.interceptor.AcceptHeaderInterceptor;
import com.homeadvisor.robusto.spring.interceptor.RequestResponseLogInterceptor;
import com.homeadvisor.robusto.spring.interceptor.ResponseTimeInterceptor;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;
import org.springframework.core.env.Environment;
import org.springframework.http.client.*;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.stream.Collectors;

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
    * This is the default RestTemplate used for all commands when no command
    * specific timeouts have been specified. See {@link SpringClientConfiguration#getConnectTimeout(String) connect}
    * and {@link SpringClientConfiguration#getRequestTimeout(String) request} timeouts.
    */
   private RestTemplate defaultRestTemplate;

   /**
    * Reference to the Spring {@link Environment} for this application.
    */
   private Environment environment;

   /**
    * To allow multiple settings per command, we're taking the approach of maintaining
    * multiple RestTemplate that are keyed by command name. The default RestTemplate
    * should be used when no cutomization is needed for a command.
    */
   private final Map<String, RestTemplate> restTemplateMap = new HashMap<>();

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
      IGNORED_METHODS_FOR_COMMAND_NAME.add("getRestTemplate");

      try
      {
         defaultRestTemplate = createRestTemplate(
                  "default",
                  getSpringConfiguration().getConnectTimeout(),
                  getSpringConfiguration().getRequestTimeout()
         );

         defaultRestTemplate.setInterceptors(createInterceptors().stream().collect(Collectors.toList()));
      }
      catch(Exception e)
      {
         LOG.error("Failed to initialize default RestTemplate", e);
      }
   }

   /**
    * Extension point for plugging in different HTTP factories.
    * @return Default is a {@link BufferingClientHttpRequestFactory}
    */
   protected ClientHttpRequestFactory createHttpFactory(
         int connectTimeout,
         int requestTimeout)
   {
      SimpleClientHttpRequestFactory scrf = new SimpleClientHttpRequestFactory();
      scrf.setConnectTimeout(connectTimeout);
      scrf.setReadTimeout(requestTimeout);

      //
      // Wrap the default request factory in a BufferingClientHttpRequestFactory
      // which allows us to read response bodies multiple times. This is needed
      // because some interceptors will need to consume the body before the final
      // response gets to the caller.
      //

      return new BufferingClientHttpRequestFactory(scrf);
   }

   /**
    * Creates the proper set of interceptors for this class. Extending classes that override this
    * should consider calling this method first and then adding their
    * own interceptors afterwards. This is because this method returns a TreeSet
    * with interceptors ordered using the Spring AnnotationAwareOrderComparator so that
    * interceptors are executed in a desired order.
    * @return Set of client HTTP interceptors ordered using AnnotationAwareOrderComparator.
    */
   protected Set<ClientHttpRequestInterceptor> createInterceptors()
   {
      Set<ClientHttpRequestInterceptor> interceptors = new TreeSet<>(new AnnotationAwareOrderComparator());

      interceptors.add(new AcceptHeaderInterceptor(getSpringConfiguration().getDefaultAcceptTypes()));
      interceptors.add(new RequestResponseLogInterceptor(getSpringConfiguration().isHttpLoggingDebug()));
      interceptors.add(new ResponseTimeInterceptor(getSpringConfiguration().isResponseTimingDebug()));

      return interceptors;
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
    * are not null. Also uses the default command name as defined by
    * {@link #buildCommandGroupName()}.
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
      String commandName = buildCommandGroupName();
      return restCommand(uriProvider, callback, listener, cacheKey, commandCache, commandName);
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
    * @param commandName Name to use for the hystrix command.
    * @return Result of remote method call.
    */
   public <T> ApiCommand.Builder<T> restCommand(
         UriProvider<T> uriProvider,
         SpringInstanceCallback<T> callback,
         RetryListener listener,
         Object cacheKey,
         CommandCache<?,?,?> commandCache,
         String commandName
   )
   {
      callback.setCommandName(commandName);
      return ApiCommand.<T>builder()
            .withHystrixCommandProperties(getConfiguration().getHystrixCommandProperties(commandName))
            .withHystrixThreadProperties(getConfiguration().getHystrixThreadPoolProperties(commandName))
            .withNumberOfRetries(getConfiguration().getNumRetries(commandName))
            .withUriProvider(uriProvider)
            .withCommandGroup(capitalizeName(getServiceName()) + "." + commandName)
            .withRetryListener(listener)
            .withRemoteServiceCallback(callback)
            .withCommandCache(commandCache, cacheKey);
   }

   /**
    * Return a fully intialized {@link RestTemplate} that can be used to
    * invoke remote HTTP commands. This is configured with default timeouts.
    * @return Fully initialized RestTemplate.
    */
   public RestTemplate getRestTemplate()
   {
      return defaultRestTemplate;
   }

   /**
    * Return a fully intialized {@link RestTemplate} that can be used to
    * invoke remote HTTP commands. This RestTemplate will be customized with
    * connect and request timeouts for the given command if appropriate, or
    * the default RestTemplate will be returned.
    * @return Fully initialized RestTemplate.
    */
   public RestTemplate getRestTemplate(String commandName)
   {
      //
      // See if the configuration specifies connect and request timeouts for the
      // given command that differ from the defaults. If so, then lazily intialize
      // a new RestTemplate with those values and use that. If the timesouts are
      // the same as the default then we just return the default RestTemplate.
      //

      if(   getSpringConfiguration().getConnectTimeout(commandName) != getSpringConfiguration().getConnectTimeout()
         || getSpringConfiguration().getRequestTimeout(commandName) != getSpringConfiguration().getRequestTimeout())
      {
         if(!restTemplateMap.containsKey(commandName))
         {
            try
            {
               RestTemplate restTemplate = createRestTemplate(
                     commandName,
                     getSpringConfiguration().getConnectTimeout(commandName),
                     getSpringConfiguration().getRequestTimeout(commandName));

               restTemplate.setInterceptors(createInterceptors().stream().collect(Collectors.toList()));

               restTemplateMap.put(
                     commandName,
                     restTemplate);
            }
            catch (Exception e)
            {
               LOG.error("Unable to intialize new RestTemplate for command {}, default will be used", commandName, e);
            }
         }

         //
         // Shouldnt need the default value for the get() here unless something
         // went haywire above.
         //

         return restTemplateMap.getOrDefault(commandName, defaultRestTemplate);
      }

      return defaultRestTemplate;
   }

   /**
    * Creates a new {@link RestTemplate} with the given connect and request timeouts.
    * The request factory used is specified by {@link #createHttpFactory(int, int)}.
    * @param connectTimeout Connect timeout.
    * @param requestTimeout Request timeout.
    * @return New RestTemplate.
    */
   protected RestTemplate createRestTemplate(String commandName, int connectTimeout, int requestTimeout)
   {
      LOG.info(
            "Creating new RestTemplate for {} command with connect/read timeouts of {}/{}",
            commandName,
            connectTimeout,
            requestTimeout);

      RestTemplate customRestTemplate =
            new RestTemplate(
                  createHttpFactory(
                        connectTimeout,
                        requestTimeout));

      return customRestTemplate;
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
         config = new SpringClientConfiguration(environment, getConfigPrefix());
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

      String suffix = "";

      for(StackTraceElement ste : Thread.currentThread().getStackTrace())
      {
         if(!IGNORED_METHODS_FOR_COMMAND_NAME.contains(ste.getMethodName()))
         {
            suffix = capitalizeName(ste.getMethodName());
            break;
         }
      }

      return suffix;
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

   /**
    * By default we just use service name as the prefix to make naming
    * a little more meaningful for properties, but the use can override
    * this if they want. This is really useful if your application has
    * multiple clients and you want to configure them individually.
    * @return A string config prefix.
    */
   protected String getConfigPrefix()
   {
      return getServiceName();
   }

   @Autowired
   public void setEnvironment(Environment environment)
   {
      this.environment = environment;
   }

   protected Environment getEnvironment()
   {
      return environment;
   }
}
