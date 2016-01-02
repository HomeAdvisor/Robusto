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
package com.homeadvisor.robusto.cache.guava;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.homeadvisor.robusto.cache.CommandCache;
import com.homeadvisor.robusto.cache.CommandCacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of {@link CommandCache} backed by a Guava cache.
 */
public class GuavaCommandCache<K,F,T> extends CommandCache<K,F,T>
{
   private final static Logger LOG = LoggerFactory.getLogger(GuavaCommandCache.class);

   private final Cache<K,F> cache;

   /**
    * Constructor. Initializes the cache with the provided config.
    * @param name Cache name.
    * @param config CommandCacheConfig
    */
   public GuavaCommandCache(String name, GuavaCommandCacheConfig config)
   {
      super(name, config);
      cache = initCache();
   }

   /**
    * Constructs the backing cache.
    */
   private Cache initCache()
   {
      return CacheBuilder.newBuilder()
            .maximumSize(getGuavaConfig().getMaxSize())
            .expireAfterAccess(getGuavaConfig().getExpiration(), TimeUnit.SECONDS)
            .recordStats()
            .build();
   }

   @Override
   protected Optional<F> doGetCache(K key)
   {
      return Optional.ofNullable(cache.getIfPresent(key));
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
      cache.invalidateAll();
   }

   @Override
   public String dumpCache()
   {
      StringBuilder output = new StringBuilder("Cache ").append(getName()).append("\n\n");

      cache.asMap().forEach( (k, v) -> output.append(k).append(" : ").append(v.toString()).append("\n") );

      return output.toString();
   }

   /**
    * Exposes the Guava cache stats for this cache.
    * @return String of stats for this cache.
    */
   public String getStats()
   {
      return new StringBuilder(getName()).append(" : ").append(cache.stats().toString()).toString();
   }

   /**
    * Helper method so we dont always have to cast the {@link CommandCacheConfig}
    * to the type we expect it to be.
    */
   private GuavaCommandCacheConfig getGuavaConfig()
   {
      return (GuavaCommandCacheConfig)getConfig();
   }
}
