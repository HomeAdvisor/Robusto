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
package com.homeadvisor.robusto.spring.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Implementation of {@link ClientHttpRequestInterceptor} that records the time it
 * takes to get a response from the remote service.
 */
@Order(0)
public class ResponseTimeInterceptor implements ClientHttpRequestInterceptor
{
   private static final Logger LOG = LoggerFactory.getLogger(ResponseTimeInterceptor.class);

   private final boolean useDebug;

   public ResponseTimeInterceptor(boolean useDebug)
   {
      this.useDebug = useDebug;
   }

   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
   {
      long startTime = System.currentTimeMillis();

      ClientHttpResponse response = execution.execute(request, body);

      long endTime = System.currentTimeMillis();

      if(useDebug)
      {
         LOG.debug("Request for {} took {} ms", request.getURI().toString(), endTime - startTime);
      }
      else
      {
         LOG.info("Request for {} took {} ms", request.getURI().toString(), endTime - startTime);
      }

      return response;
   }
}
