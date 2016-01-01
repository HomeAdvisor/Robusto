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

/**
 * Interface for deciding if values should be put into a {@link CommandCache}.
 * This allows clients a way to examine or modify a value before caching it,
 * and if desired, avoid the cache put entirely.
 */
public interface CachePutHandler<V>
{
   V beforeCachePut(V value);
}
