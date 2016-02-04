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
 * Interface for deciding if values should be used from a {@link CommandCache}.
 * This encapuslates 2 aspects of getting values from a cache:
 * <ol>
 *    <li>The parameters T and F represent the ability to translate values. If your
 *    client does not need or want to use the same DTO as the cahce you can use this
 *    interface to perform a translation. If you do indeed want to use the same
 *    DTO then T and F are the same type.</li>
 *    <li>If you want to avoid using a cached value entirely then you can return
 *    a straight null from {@link #afterCacheGet(Optional)}. We use an Optional here
 *    so that you can differentiate between a null value versus the cache not having
 *    the key/value at all.</li>
 * </ol>
 */
@FunctionalInterface
public interface CacheGetHandler<F, T>
{
   /**
    * This gets called after {@link CommandCache#getCache(Object)}. You
    * can use this method to examine the value returned from cache, and if
    * desired, modify it before being used. You can also prevent using it
    * entirely by returning null, which will force a remote command execution.
    * @return Value to return to client, or null if you wish not cache it.
    */
   Optional<T> afterCacheGet(Optional<F> value);
}
