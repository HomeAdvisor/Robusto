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
package com.homeadvisor.robusto.cache;

import java.util.Map;

/**
 * Contains basic configuration parameters applicable to all caches. Specific
 * client caches may extend this to provide their own parameters that make
 * sense.
 */
public class CommandCacheConfig
{
   protected static final String CONFIG_ENABLED = "enabled";

   protected static final String CONFIG_PUT_ENABLED = "putEnabled";

   /**
    * Indicate if this cache should be used at all.
    */
   private boolean enabled = true;

   /**
    * A flag indicating if the cache should do the cache put after the the
    * remote service has returned a value (default is true). Some implementations
    * may wish to avoid cache puts for various reasons, for example with
    * distributed caches where the client and server are using the same cache
    * and a cache miss will result in the remote service call causing the cache
    * to be populated.
    */
   private boolean putEnabled = true;

   public boolean isEnabled()
   {
      return enabled;
   }

   public void setEnabled(boolean enabled)
   {
      this.enabled = enabled;
   }

   public boolean isPutEnabled()
   {
      return putEnabled;
   }

   public void setPutEnabled(boolean putEnabled)
   {
      this.putEnabled = putEnabled;
   }

   /**
    * Creates a new CommandCacheConfig from the given map of config values. Only
    * values this class cares about will be used, otehr will be ignored.
    * @param configMap Map of config key/value pairs.
    * @return New CommandCacheConfig.
    */
   public static CommandCacheConfig fromMap(Map<String, Object> configMap)
   {
      CommandCacheConfig newConfig = new CommandCacheConfig();

      newConfig.setEnabled(Boolean.valueOf(configMap.getOrDefault(CONFIG_ENABLED, "true").toString()));
      newConfig.setPutEnabled(Boolean.valueOf(configMap.getOrDefault(CONFIG_PUT_ENABLED, "true").toString()));

      return newConfig;
   }
}
