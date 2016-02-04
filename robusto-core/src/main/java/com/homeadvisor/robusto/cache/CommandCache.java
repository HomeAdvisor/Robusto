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

import java.util.Optional;

/**
 * Abstract class for defining basic command caching. This provides the bare
 * minimum operations and leaves core caching operations (get and put) up to
 * actual implementations.
 * <br/>
 * The type parameters should be interpreted as follows:
 * <ul>
 *    <li>K - Type of the key</li>
 *    <li>F - The type used from the underlying cache mechanism</li>
 *    <li>T - The type returned to client after a cache hit</li>
 * </ul>
 * <br/>
 * The types F and T exist to allow any registered {@link CacheGetHandler} to
 * optionally translate the type from the underlying cace into a different type
 * that the client wants. One example of this with the Coherence implemntation.
 * The coherence cache may use very heavy VO objects that should not be passed
 * around, and the client chooses to use a DTO that is some subset of the VOs
 * returned from the coherence cache.
 */
public abstract class CommandCache<K,F,T>
{
   /**
    * Unique name for this cache.
    */
   private final String cacheName;

   /**
    * Configuration to use for setting up the underlying cache.
    */
   private final CommandCacheConfig cacheConfig;

   /**
    * Optional handler for intercepting cache gets, and allowing you to
    * modify the return type if needed.
    */
   private CacheGetHandler<F,T> cacheGetHandler = null;

   /**
    * Optional handler for intercepting cache puts.
    */
   private CachePutHandler<F> cachePutHandler = null;

   public CommandCache(String cacheName, CommandCacheConfig cacheConfig)
   {
      this.cacheName = cacheName;
      this.cacheConfig = cacheConfig;
   }

   /**
    * Set a {@link CacheGetHandler} to be invoked after {@link CommandCache#getCache(Object)}
    * returns a value.
    * @param handler The handler.
    */
   public void setCacheGetHandler(CacheGetHandler<F,T> handler)
   {
      this.cacheGetHandler = handler;
   }

   public CacheGetHandler<F,T> getCacheGetHandler()
   {
      return cacheGetHandler;
   }

   /**
    * Set a {@link CachePutHandler} to be invoked before {@link CommandCache#putCache(Object, Object)}
    * caches a value.
    * @param handler The handler.
    */
   public void setCachePutHandler(CachePutHandler<F> handler)
   {
      this.cachePutHandler = handler;
   }

   public CachePutHandler<F> getCachePutHandler()
   {
      return cachePutHandler;
   }

   /**
    * A unique String that identifies this cache. A single client may manage
    * multiple caches so each one needs its own name.
    * @return Cache name.
    */
   public String getName()
   {
      return cacheName;
   }

   /**
    * Provides access to the underlying cache config.
    * @return Cache config.
    */
   public CommandCacheConfig getConfig()
   {
      return cacheConfig;
   }

   /**
    * Public interface for getting values from the underlying cache. This does
    * the work of passing values through any registered {@link CacheGetHandler}
    * after calling {@link #doGetCache(Object)}.
    * @param key Cache key
    * @return Result of cache get
    */
   public Optional<T> getCache(K key)
   {
      //
      // Lookup the value from the underlying cache
      //

      Optional<F> cacheValue = doGetCache(key);

      //
      // Prepare to translate the result, if applicable. If no handler
      // is present F and T *must* be the same type so we can safely
      // cast between the two.
      //

      if(getCacheGetHandler() != null && cacheValue != null)
      {
          return getCacheGetHandler().afterCacheGet(cacheValue);
      }
      else
      {
         if(cacheValue !=null && cacheValue.isPresent())
         {
            return Optional.ofNullable((T) cacheValue.get());
         }
         else
         {
            return null;
         }
      }
   }

   /**
    * Public interface for putting values into the underlying cache. This does
    * the work of passing the value through any registered {@link CachePutHandler}
    * prior to calling {@link #doPutCache(Object, Object)}. If the put handler
    * returns null, this method simply returns true and skips caching entirely.
    * @param key Cache key
    * @param value Value to cache
    * @return True if successful, false if any error occur from underlying cache
    * put.
    */
   public boolean putCache(K key, F value)
   {
      //
      // Some caches may wish to avoid puts
      //

      if(getConfig().isPutEnabled() == false)
      {
         return true;
      }

      if(getCachePutHandler() != null)
      {
         value = getCachePutHandler().beforeCachePut(value);
      }

      return value == null ? true : doPutCache(key, value);
   }

   /**
    * Gets a value from the cache for a the given key if it exists.
    * @param key Key to use for lookup.
    * @return Value from cache, or null if the key is not present.
    */
   protected abstract Optional<F> doGetCache(K key);

   /**
    * Put a key/value pair into the cache.
    * @param key Key
    * @param value Value
    * @return True if the operation succeeded, false otherwise.
    */
   protected abstract boolean doPutCache(K key, F value);

   /**
    * Allows clients to purge all entries from the cache.
    */
   public abstract void emptyCache();

   /**
    * Dumps all the key/value pairs in the cache to a human readable format.
    * @return List of key/value pairs.
    */
   public abstract String dumpCache();
}
