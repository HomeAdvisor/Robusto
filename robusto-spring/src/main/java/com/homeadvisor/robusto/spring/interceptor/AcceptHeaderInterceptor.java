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

import org.apache.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import java.util.List;

/**
 * Implementation of {@link ClientHttpRequestInterceptor} that sets the
 * proper accept headers on all requests. See {@link org.springframework.http.MediaType}
 * for proper formatting of values.
 * <br/><br/>
 * Note that if the passed in list is null or empty, the default Spring implementation
 * is used for accept headers.
 */
public class AcceptHeaderInterceptor implements ClientHttpRequestInterceptor
{
   private final List<String> mediaTypes;

   public AcceptHeaderInterceptor(List<String> mediaTypes)
   {
      this.mediaTypes  = mediaTypes;
   }

   @Override
   public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException
   {
      if(mediaTypes != null && mediaTypes.size() > 0)
      {
         request.getHeaders().put(HttpHeaders.ACCEPT, mediaTypes);
      }

      return execution.execute(request, body);
   }
}
