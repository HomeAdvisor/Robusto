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
package com.homeadvisor.robusto;

/**
 * Implementation of {@link UriProvider} that returns the same URI for every
 * invoccation of a {@link ApiCommand}. Suitable when using virtual IPs or DNS
 * aliases, or when a remote service is always tied to a specific address/IP.
 */
public class ConstantUriProvider<T> implements UriProvider<T>
{
   private final String uri;

   public ConstantUriProvider(String uri)
   {
      this.uri = uri;
   }

   @Override
   public T execute(RemoteServiceCallback<T> callback)
   {
      return callback.run(uri);
   }
}
