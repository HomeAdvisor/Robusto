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
package com.homeadvisor.robusto.cache.coherence;

import com.homeadvisor.robusto.cache.CommandCacheConfig;

import java.util.Map;

/**
 * Extension of {@link CommandCacheConfig} that adds additional configuration
 * applicable to Coherence caches.
 */
public class CoherenceCommandCacheConfig extends CommandCacheConfig
{
   private final static String CONFIG_CACHE_NAME = "name";

   /**
    * This refers to the name of coherence cache to connect to, and can be
    * different from the name the client uses to refer to the command cache.
    * The goal is to allow the coherence cache to change names without
    * requiring a code change,
    */
   private String coherenceCacheName;

   /**
    * Construct a new CoherenceCommandCacheConfig with the given coherence
    * cache name.
    */
   public CoherenceCommandCacheConfig(String coherenceCacheName)
   {
      this.coherenceCacheName = coherenceCacheName;
   }

   public String getCoherenceCacheName()
   {
      return coherenceCacheName;
   }

   /**
    * Static method to create a new CoherenceCommandCacheConfig from a map of config
    * values. This will pull out the applicable config values and ignore any
    * others. Important: this config sets putEnabled = false by default, so you will
    * need to override it if you want that behavior.
    * @param configMap Map of config keys and values.
    * @return New CoherenceCommandCacheConfig.
    * @throws IllegalArgumentException If the required config name isn't present.
    */
   public static CoherenceCommandCacheConfig fromMap(Map<String, Object> configMap)
   {
      boolean putEnabled = Boolean.valueOf(configMap.getOrDefault(CommandCacheConfig.CONFIG_PUT_ENABLED, "false").toString());

      if(configMap.containsKey(CONFIG_CACHE_NAME))
      {
         CoherenceCommandCacheConfig newConfig = new CoherenceCommandCacheConfig(configMap.get(CONFIG_CACHE_NAME).toString());
         newConfig.setPutEnabled(putEnabled);
         return newConfig;
      }

      throw new IllegalArgumentException("Config is missing required parameter " + CONFIG_CACHE_NAME);
   }
}
