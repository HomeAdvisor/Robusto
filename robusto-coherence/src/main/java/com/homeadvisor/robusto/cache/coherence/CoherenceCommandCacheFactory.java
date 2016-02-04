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

import com.homeadvisor.robusto.cache.CommandCacheFactory;

import java.util.Map;

/**
 *  Creates a new instance of a {@link CoherenceCommandCache}.
 */
public class CoherenceCommandCacheFactory implements CommandCacheFactory<CoherenceCommandCache>
{
   @Override
   public CoherenceCommandCache create(String name, Map<String, Object> config)
   {
      //
      // Need to use the name from the config if provided, so buil the config
      // first. Backup is to just use the client cache name.
      //

      CoherenceCommandCacheConfig newConfg = CoherenceCommandCacheConfig.fromMap(config);

      return new CoherenceCommandCache(
            config.getOrDefault("name", name).toString(),
            newConfg);
   }
}
