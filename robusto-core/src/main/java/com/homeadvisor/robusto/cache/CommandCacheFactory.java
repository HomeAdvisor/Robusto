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
 * Simple interface for defining how to create new subclasses of {@link CommandCache}.
 */
public interface CommandCacheFactory<T extends CommandCache>
{
   /**
    * Create a new instance of a subclass of CommandCache.
    * @param name The name of cache the client is referring too.
    * @param config Map of config values that make sense for this cache type.
    * @return New cache of type T.
    */
   T create(String name, Map<String, Object> config);
}
