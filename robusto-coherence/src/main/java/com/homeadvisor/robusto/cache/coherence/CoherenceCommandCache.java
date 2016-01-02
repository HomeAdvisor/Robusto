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
package com.homeadvisor.robusto.cache.coherence;

import com.homeadvisor.robusto.cache.CommandCache;
import com.homeadvisor.robusto.cache.CommandCacheConfig;
import com.tangosol.net.CacheFactory;
import com.tangosol.net.NamedCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Extension of {@link CommandCache} that uses Oracle Coherence as the
 * underlying cache mechanism.
 * </br>
 * <br/>
 * <em>Important!</em> This, by default, disables cache puts because we assume
 * the remote service cache
 */
public class CoherenceCommandCache<K,F,T> extends CommandCache<K,F,T>
{
   private final static Logger LOG = LoggerFactory.getLogger(CoherenceCommandCache.class);

   /**
    * Limit the number of entries we will dump.
    */
   private final static int MAX_DUMP_SIZE = 500;

   /**
    * Underlying coherence cache handle.
    */
   private final NamedCache cache;

   public CoherenceCommandCache(NamedCache cache, CommandCacheConfig config)
   {
      super(cache.getCacheName(), config);
      this.cache = cache;
      config.setPutEnabled(false);
   }

   public CoherenceCommandCache(String cache, CommandCacheConfig config)
   {
      this(CacheFactory.getCache(cache), config);
   }

   @Override
   protected Optional<F> doGetCache(K key)
   {
      if(cache.containsKey(key))
      {
         return Optional.ofNullable((F)cache.get(key));
      }
      return null;
   }

   @Override
   protected boolean doPutCache(Object key, Object value)
   {
      try
      {
         cache.put(key, value);
         return true;
      }
      catch (Exception e)
      {
         LOG.warn("Failed cache key {} in cache {}", key, getName(), e);
         return false;
      }
   }

   @Override
   public void emptyCache()
   {
      cache.clear();
   }

   @Override
   public String dumpCache()
   {
      //
      // Some of these caches may have really big max sizes and dumping all
      // contents could take a while, so limit the number returned
      //

      try
      {
         StringBuilder output = new StringBuilder();

         if(cache.size() >= MAX_DUMP_SIZE)
         {
            output.append("Cache size {} > {}, cache dump is disabled for performance reasons.", cache.size(), MAX_DUMP_SIZE);
            return output.toString();
         }

         cache.keySet().forEach(key -> output.append(key.toString()).append(" : ").append(cache.get(key)).append("\n"));

         return output.toString();
      }
      catch(Exception e)
      {
         return "Error Dumping Cache : " + e.getMessage();
      }
   }
}
