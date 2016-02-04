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
 * Creates a new instance of a {@link HashMapCommandCache}.
 */
public class HashMapCommandCacheFactory implements CommandCacheFactory<HashMapCommandCache>
{
   @Override
   public HashMapCommandCache create(String name, Map<String, Object> config)
   {
      return new HashMapCommandCache(name, new CommandCacheConfig());
   }
}
