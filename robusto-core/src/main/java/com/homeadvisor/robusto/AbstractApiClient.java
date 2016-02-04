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
import com.homeadvisor.robusto.cache.CommandCacheConfig;
import com.homeadvisor.robusto.cache.CommandCacheFactory;
import com.homeadvisor.robusto.cache.HashMapCommandCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Base class for all API clients.
 */
public abstract class AbstractApiClient
{
   private final static Logger LOG = LoggerFactory.getLogger(AbstractApiClient.class);

   /**
    * Allows clients to manage multiple caches, each with its own name and
    * configuration.
    */
   protected Map<String, CommandCache> cacheMap = new HashMap<>();

   /**
    * Returns the name of the service of the remote API. This name can be
    * used in many ways by implementing clients, such as service discovery,
    * thread pool naming, etc.
    *
    * @return Name of the service this client communicates with.
    */
   public abstract String getServiceName();

   /**
    * By default we just use service name as the prefix to make naming
    * a little more meaningful for properties, but the user can override
    * this if they want.
    *
    * @return
    */
   protected String getConfigPrefix()
   {
      return getServiceName();
   }

   /**
    * Provide a mechanism for clients to get configuration data. Default is
    * simply return a new {@link ClientConfiguration}.
    *
    * @return Client configuration data.
    */
   protected ClientConfiguration getConfiguration()
   {
      return new ClientConfiguration();
   }

   /**
    * Returns a {@link CommandCache}, creating and adding it to the cache map
    * the first time the cache is requested. This will delegate to
    * {@link #createCache(String)} to lazily init the cache the first time
    * it is needed.
    * @param name Cache name
    */
   protected <K,F,T> CommandCache<K,F,T> getCache(String name)
   {
      if (cacheMap.containsKey(name) == false)
      {
         //
         // Try to build a cache from config values...this ensures config values
         // always take presedence over hard coded methods. If no cache config
         // is present then delegate cache building to createCache(), which may
         // be overridden by clients as needed.
         //

         String type   = getConfiguration().getCacheType(name);
         String config = getConfiguration().getCacheConfig(name);

         try
         {
            if (type != null && type.length() > 0 && config != null && config.length() > 0)
            {
               LOG.info("Attempting to create cache {} from config {}", name, config);
               cacheMap.put(name, createCacheFromConfig(name, type, config));
               return cacheMap.get(name);
            }
         }
         catch(Exception e)
         {
            LOG.warn("Error creating cache {} from config, will use default behavior", name, e);
         }

         cacheMap.put(name, createCache(name));

         //
         // Finally, check if this cache has been explicitly enabled or disabled.
         // Doing this as a separate check from type and config above so that
         // we can set enabled to true/false without having to specifiy the type
         // and config with it (i.e. we can leave type and config unspecified and
         // get default cache creation behavior and then set the enabled flag on
         // its own).
         //

         if(cacheMap.get(name) != null)
         {
            cacheMap.get(name).getConfig().setEnabled(getConfiguration().isCacheEnabled(name));
         }
      }

      return cacheMap.get(name);
   }

   /**
    * Returns null, indicating no caching will be performed. Clients should
    * override this method to get cache behavior specific to their needs.
    * @param name Name of cache to create.
    */
   protected <K,F,T> CommandCache<K,F,T> createCache(String name)
   {
      return null;
   }

   /**
    * Delegates to {@link #createCacheFromConfig(String, String, String)}, passing the given
    * cache name along with the type and config obtained from {@link ClientConfiguration#getCacheType(String)}
    * and {@link ClientConfiguration#getCacheConfig(String)}, respectively.
    * @param name Cache name
    * @return Fully intiailized CommandCache
    */
   protected <K,F,T>CommandCache<K,F,T> createCacheFromConfig(String name)
   {
      return createCacheFromConfig(
            name,
            getConfiguration().getCacheType(name),
            getConfiguration().getCacheConfig(name));
   }

   /**
    * Creates a new CommandCache with the given name, type, and config. The type
    * should be the fully qualified {@link CommandCacheFactory} class and config
    * should be of the following format:
    * <br/>
    * <br/>
    * <pre>config1=value1,config2=value2</pre>
    * <br/>
    * <br/>
    * This method should not be overridden. It is simply a utility method from invoking
    * cache factories, allowing you to provide the config String from any source
    * you like (as long as it adheres to the structure laid out above).
    * @param name Name of command cache.
    * @param type Fully qualified cache factory class name that can build a cache.
    * @param config String representing comma-separated key/value pairs that can
    *               be used to intialize the command cache.
    * @return New CommandCache, or null if an error occurs.
    */
   protected <K,F,T> CommandCache<K,F,T> createCacheFromConfig(String name, String type, String config)
   {
      LOG.info("Creating command cache {} using {} and config {}", name, type, config);

      try
      {
         //
         // The config value pairs are everything after the semi-colon to the end
         //

         StringTokenizer configTokens = new StringTokenizer(config, ",");

         Map<String, Object> configMap = new HashMap<>();

         while(configTokens.hasMoreElements())
         {
            String token = configTokens.nextToken();

            configMap.put(
                  token.substring(0, token.indexOf('=')),   // config name
                  token.substring(token.indexOf('=') + 1)); // config value
         }

         //
         // Try to create the factory class using reflection
         //

         Class clazz = Class.forName(type);

         CommandCacheFactory factory = (CommandCacheFactory)clazz.newInstance();

         //
         // Invoke the builder method to get a concrete command cache with config
         //

         return factory.create(name, configMap);
      }
      catch(Exception e)
      {
         LOG.error("Error configuring cache {}, this cache will not be used", name, e);
         return null;
      }
   }

   /**
    * Utility method to consistently log API failures. Extending clients can override
    * if they don't like the format. By default this implementation will log the throwable
    * message along with the command group name. The log level will be ERROR for
    * {@link RetryableApiCommandException}s and WARN for all others.
    * @param logger Logger to send message to.
    * @param command ApiCommand that was executed or being constructed (may be null).
    * @param throwable The exception to be logged.
    */
   protected void logCommandException(Logger logger, ApiCommand command, Throwable throwable)
   {
      StringBuilder sb = new StringBuilder("Exception executing command ");

      //
      // Log either the command group or service name to provide some context
      //

      if(command != null)
      {
         sb.append(command.getCommandGroup().name());
      }
      else
      {
         sb.append("[unknown command for service ").append(getServiceName()).append("]");
      }

      //
      // Add the throwable message
      //

      sb.append("; ").append(throwable.getMessage());

      //
      // Ready to log
      //

      if(throwable instanceof RetryableApiCommandException)
      {
         logger.error(sb.toString(), throwable);
      }
      else
      {
         logger.warn(sb.toString());
      }
   }
}
