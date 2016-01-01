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

import com.netflix.hystrix.exception.HystrixBadRequestException;

/**
 * Type of exception that is not retryable, such as illegal arguments,
 * authentication/authorization problems, etc. These types of exceptions
 * will NOT be retried by the framework and will not count towards the health
 * of a curator service instance or Hystrix circuit.
 * <br/><br/>
 * This class extends {@link HystrixBadRequestException} to prevent triggering
 * open Hystrix circuits, and implements {@link IServiceInstanceRequestException}
 * to prevent marking curator service instances as bad.
 */
public class NonRetryableApiCommandException extends HystrixBadRequestException
{
   public NonRetryableApiCommandException(String msg)
   {
      super(msg);
   }

   public NonRetryableApiCommandException(String msg, Throwable t)
   {
      super(msg, t);
   }
}
