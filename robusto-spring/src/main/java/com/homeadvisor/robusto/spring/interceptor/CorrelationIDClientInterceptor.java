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
package com.homeadvisor.robusto.spring.interceptor;

import com.homeadvisor.correlation.Correlation;
import com.homeadvisor.correlation.CorrelationID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

/**
 * Implementation of {@link ClientHttpRequestInterceptor} that adds a
 * correlation ID to the HTTP headers of a client request. The correlation
 * ID will only be added if one as been set in {@link com.homeadvisor.correlation.CorrelationID}.
 */
public class CorrelationIDClientInterceptor implements ClientHttpRequestInterceptor
{
   private final static Logger LOG = LoggerFactory.getLogger(CorrelationIDClientInterceptor.class);

   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
   {
      if(CorrelationID.getId() != null)
      {
         LOG.debug("Adding correlation ID {} to request {}", CorrelationID.getId(), request.getURI().toString());
         request.getHeaders().add(Correlation.HTTP_HEADER_NAME, CorrelationID.getId());
      }
      else
      {
         LOG.debug("No correlation ID found");
      }

      return execution.execute(request, body);
   }
}
