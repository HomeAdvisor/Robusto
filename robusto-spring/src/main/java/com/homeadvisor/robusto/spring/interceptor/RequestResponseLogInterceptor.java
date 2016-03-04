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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Implementation of {@link ClientHttpRequestInterceptor} that logs every
 * request and response.
 */
@Order(1000)
public class RequestResponseLogInterceptor implements ClientHttpRequestInterceptor
{
   private static final Logger LOG = LoggerFactory.getLogger(RequestResponseLogInterceptor.class);

   private final boolean useDebugLevel;

   public RequestResponseLogInterceptor(boolean useDebugLevel)
   {
      this.useDebugLevel = useDebugLevel;
   }

   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
   {
      ClientHttpResponse response = execution.execute(request, body);

      log(request, body, response);

      return response;
   }

   /**
    * Performs actual logging of request and response objects.
    * @param request Request object.
    * @param body Request body.
    * @param response Response object.
    */
   private void log(HttpRequest request, byte[] body, ClientHttpResponse response)
   {
      StringBuilder logMsg = new StringBuilder();

      try
      {
         //
         // Log request
         //

         logMsg.append("\n\n").append("--- Request ---\n");
         logMsg.append(request.getMethod()).append(" ").append(request.getURI().toString()).append("\n").append("\n");
         request.getHeaders().forEach((name, list) -> logMsg.append(name).append(": ").append(StringUtils.collectionToDelimitedString(list, ";")).append("\n"));
         logMsg.append("\n").append(new String(body));

         //
         // Log response
         //

         logMsg.append("--- Response ---\n");
         logMsg.append(response.getRawStatusCode()).append(" ").append(response.getStatusText()).append("\n").append("\n");
         response.getHeaders().forEach((name, list) -> logMsg.append(name).append(": ").append(StringUtils.collectionToDelimitedString(list, ";")).append("\n"));

         BufferedReader br = new BufferedReader(new InputStreamReader(response.getBody()));
         logMsg.append("\n");
         br.lines().forEach((line) -> logMsg.append(line).append("\n"));

         response.getBody().reset();
      }
      catch(Exception e)
      {
         if(!useDebugLevel)
         {
            LOG.error("Error logging request and response; message content may not be complete [Error is {}]", e.getMessage());
         }
      }

      if(useDebugLevel)
      {
         LOG.debug(logMsg.toString());
      }
      else
      {
         LOG.info(logMsg.toString());
      }
   }
}
