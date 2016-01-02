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
package com.homeadvisor.robusto.health.codahale;

import com.codahale.metrics.health.HealthCheck;
import com.homeadvisor.robusto.health.HealthCheckAwareClient;
import com.homeadvisor.robusto.health.HealthCheckResult;
import com.homeadvisor.robusto.health.Status;

import java.util.Map;

/**
 * Implementation of {@link HealthCheckAwareClient} that reports health using
 * Codahale's {@link HealthCheck}.
 */
public class CodahaleClientHealthCheck extends HealthCheck implements HealthCheckAwareClient
{
   @Override
   protected Result check()
   {
      //
      // Get the parent results first
      //

      boolean isUnhealthy = false;

      StringBuilder msg = new StringBuilder();

      try
      {
         Map<String, HealthCheckResult> results = doAllChecks();

         //
         // Convert results into Codahale objects. Any result that isnt HEALTHY
         // gets converted into a Codahale UNHEALTHY object.
         //

         for(Map.Entry<String, HealthCheckResult> entry : results.entrySet())
         {
            msg.append(entry.getKey())
                  .append(" [").append(entry.getValue().status).append("]")
                  .append(entry.getValue().description != null ? " - " + entry.getValue().description : "")
                  .append("; ");


            if(entry.getValue().status != Status.HEALTHY)
            {
               isUnhealthy = true;
            }
         }
      }
      catch(Exception e)
      {
         return Result.unhealthy("Failed to check health: " + e.getMessage());
      }

      if(isUnhealthy)
      {
         return Result.unhealthy(msg.toString());
      }
      else
      {
         return Result.healthy(msg.toString());
      }
   }


}
