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

import com.homeadvisor.robusto.ClientConfiguration;
import org.springframework.http.MediaType;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates additional config options that make sense for the Spring client.
 */
public class SpringClientConfiguration extends ClientConfiguration
{
   /**
    * Determine if correlation IDs should be added to outbound requests (default
    * is true).
    * @deprecated Will be removed in future release.
    */
   private boolean correlationEnabled = true;

   /**
    * Default list of Accept-Type headers to put on outbound requests (default
    * is only application/json). Spring Rest sets a number of accept types by
    * default, including JSON, XML, HTML, etc. so this sets a default of JSON
    * only, which should be useful in most cases where a DTO is going to be
    * serialized anyway.
    */
   private List<String> defaultAcceptTypes = Collections.singletonList(MediaType.APPLICATION_JSON_VALUE);

   /**
    * Number of command failures before the {@link com.homeadvisor.robusto.health.hystrix.HystrixHealthCheck}
    * reports unhealthy.
    */
   private int hystrixHealthFailures = 1;

   //
   // Getters
   //

   /**
    * @deprecated Will be removed in future release.
    */
   @Deprecated
   public boolean isCorrelationEnabled()
   {
      return correlationEnabled;
   }

   public List<String> getDefaultAcceptTypes()
   {
      return defaultAcceptTypes;
   }

   public int getHystrixHealthNumFailures()
   {
      return hystrixHealthFailures;
   }
}
