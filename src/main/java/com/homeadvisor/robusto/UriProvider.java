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
 * Interface for getting a remote service URI. Allows a client to use different
 * service discovery mechanisms (curator, config values, etc). The execute method
 * may be invoked multiple times for the same request if retryable failures are
 * encountered, so it is up to each implementer to handle using different URIs
 * if appropriate.
 */
public interface UriProvider<T>
{
   /**
    * Main method for calling the remote service. This method should handle
    * lookup of the remote service and invoking the supplied callback.
    * @return Result of remote callback.
    */
   T execute(RemoteServiceCallback<T> callback);
}
