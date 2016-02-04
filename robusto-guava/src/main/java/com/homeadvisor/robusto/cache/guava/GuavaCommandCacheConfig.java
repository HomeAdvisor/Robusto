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
package com.homeadvisor.robusto.cache.guava;

import com.homeadvisor.robusto.cache.CommandCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Extension of {@link CommandCacheConfig} that adds additional configuration
 * applicable to Guava caches.
 */
public class GuavaCommandCacheConfig extends CommandCacheConfig
{
   private final static Logger LOG = LoggerFactory.getLogger(GuavaCommandCacheConfig.class);

   private final static String CONFIG_MAX_VALUE = "maxValue";

   private final static String CONFIG_EXPIRATION = "expiration";

   /**
    * Time before individual entries should expire (in seconds).
    */
   private int expiration = 300;

   /**
    * Maximum number of entries allowed in the cache.
    */
   private int maxSize = 1000;

   /**
    * Initialize this cache config using default values.
    */
   public GuavaCommandCacheConfig()
   {

   }

   /**
    * Initialize this cache config using custom values.
    */
   public GuavaCommandCacheConfig(int expiration, int maxSize)
   {
      this.expiration = expiration;
      this.maxSize    = maxSize;
   }

   public int getExpiration()
   {
      return expiration;
   }

   public void setExpiration(int expiration)
   {
      this.expiration = expiration;
   }

   public int getMaxSize()
   {
      return maxSize;
   }

   public void setMaxSize(int maxSize)
   {
      this.maxSize = maxSize;
   }

   /**
    * Static method to create a new GuavaCommandCacheConfig from a map of config
    * values. This will pull out the applicable config values and ignore any
    * others.
    * @param configMap Map of config keys and values.
    * @return New GuavaCommandCacheConfig.
    */
   public static GuavaCommandCacheConfig fromMap(Map<String, Object> configMap)
   {
      GuavaCommandCacheConfig newConfig = new GuavaCommandCacheConfig();

      try
      {
         newConfig.setEnabled(Boolean.valueOf(configMap.getOrDefault(CommandCacheConfig.CONFIG_ENABLED, "true").toString()));
         newConfig.setPutEnabled(Boolean.valueOf(configMap.getOrDefault(CommandCacheConfig.CONFIG_PUT_ENABLED, "true").toString()));
         newConfig.setExpiration(Integer.getInteger(configMap.getOrDefault(CONFIG_EXPIRATION, "300").toString()));
         newConfig.setMaxSize(Integer.getInteger(configMap.getOrDefault(CONFIG_MAX_VALUE, "1000").toString()));
      }
      catch(Exception e)
      {
         LOG.error("Error parsing GuavaCommandCacheConfig, cache may contain some default settings");
      }

      return newConfig;
   }
}
