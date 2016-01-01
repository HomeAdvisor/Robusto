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
package com.homeadvisor.robusto.cache;

import java.util.HashMap;
import java.util.Optional;

/**
 * Implementation of {@link CommandCache} backed by a Java HashMap. There is no
 * timeout or expiration, this is really just the simplest form of caching.
 */
public class HashMapCommandCache<K,F,T> extends CommandCache<K,F,T>
{
   /**
    * Underlying mechanism for storing cache data.
    */
   private HashMap<K,F> cache = new HashMap<>();

   public HashMapCommandCache(String cacheName, CommandCacheConfig cacheConfig)
   {
      super(cacheName, cacheConfig);
   }

   @Override
   protected Optional<F> doGetCache(K key)
   {
      return cache.containsKey(key) ? Optional.ofNullable(cache.get(key)) : null;
   }

   @Override
   protected boolean doPutCache(K key, F value)
   {
      cache.put(key, value);
      return true;
   }

   @Override
   public void emptyCache()
   {
      cache.clear();
   }

   @Override
   public String dumpCache()
   {
      StringBuilder output = new StringBuilder("Contents of cache ").append(getName()).append("\n").append("\n");

      cache.forEach( (key, value) -> output.append(key).append(" : ").append(value).append("\n") );

      return output.toString();
   }
}
