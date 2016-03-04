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
package com.homeadvisor.robusto.spring;

import com.homeadvisor.robusto.NonRetryableApiCommandException;
import com.homeadvisor.robusto.RemoteServiceCallback;
import com.homeadvisor.robusto.RetryableApiCommandException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

/**
 * Extension of {@link RemoteServiceCallback} that handles Spring classes
 * of exceptions and wraps them in RetryableApiCommandException or
 * NonRetryableApiCommandException types.
 */
public abstract class SpringInstanceCallback<T> implements RemoteServiceCallback<T>
{
   private String commandName;

   public SpringInstanceCallback()
   {
      this.commandName = "";
   }

   public SpringInstanceCallback(String commandName)
   {
      this.commandName = commandName;
   }

   /**
    * This delagates actual execution to {@link #runWithUrl(String)} and handles any
    * Spring Rest exceptions, wrapping them as necessary. Default behavior is to throw
    * a {@link RetryableApiCommandException} for 5xx errors and timeouts, and {@link
    * NonRetryableApiCommandException} for all others. If you need different behavior
    * your best bet is to write your own {@link RemoteServiceCallback} and/or use
    * the various flavors of {@link org.springframework.web.client.RestTemplate} that
    * take let you provide {@link ResponseErrorHandler}.
    * @param url The URL of the remote service.
    * @return Result of the remote call, or potentially throws an exception if an
    * error occurs calling the remote service.
    */
   @Override
   public T run(String url)
   {
      T response = null;

      try
      {
         response = runWithUrl(url);
      }
      catch (HttpStatusCodeException hsce)
      {
         if (hsce.getStatusCode()         == HttpStatus.REQUEST_TIMEOUT ||
             hsce.getStatusCode().value() >= 500)
         {
            throw new RetryableApiCommandException("Remote server error: " + hsce.getMessage(), hsce);
         }
         else
         {
            throw new NonRetryableApiCommandException("Local client error: " + hsce.getMessage(), hsce);
         }
      }

      return response;
   }

   protected void setCommandName(String commandName)
   {
      this.commandName = commandName;
   }

   protected String getCommandName()
   {
      return commandName;
   }

   /**
    * This is the method users must implement to execute the desired HTTP
    * call using Spring RestTemplate. The URL provided is the base URL of the
    * remote service so you must add on any path and query parameters before
    * executing. If you are using {@link SpringRestClient} then this is the
    * method where you should use {@link SpringRestClient#getRestTemplate()}
    * to execute your HTTP call.
    * @param url Base URL of remote service.
    * @return Result of remote HTTP call.
    */
   public abstract T runWithUrl(String url);
}
