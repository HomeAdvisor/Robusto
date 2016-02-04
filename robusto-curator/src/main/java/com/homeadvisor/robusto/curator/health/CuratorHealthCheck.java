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
package com.homeadvisor.robusto.curator.health;

import com.homeadvisor.robusto.health.HealthCheck;
import com.homeadvisor.robusto.health.HealthCheckResult;
import com.homeadvisor.robusto.health.Status;
import org.apache.curator.x.discovery.ServiceProvider;

/**
 * Health check handler that bases health off of curator service discovery.
 * The health check reports down if there is not a minimum number of
 * available service instances.
 */
public class CuratorHealthCheck implements HealthCheck
{
   /**
    * Curator service discovery interface used to look up service instances.
    */
   private ServiceProvider serviceProvider;

   /**
    * The minimum number of service instances required for this health check
    * to report healthy.
    */
   private int minNumberInstances;

   public CuratorHealthCheck(ServiceProvider serviceProvider, int minNumberInstances)
   {
      this.serviceProvider = serviceProvider;
      this.minNumberInstances = minNumberInstances;
   }

   @Override
   public HealthCheckResult doCheck()
   {
      try
      {
         int numInstances = serviceProvider.getAllInstances().size();
         return numInstances >= minNumberInstances
               ? new HealthCheckResult(Status.HEALTHY)
               : new HealthCheckResult(Status.UNHEALTHY, "Number of available server instances too low [Found: " + numInstances + ", Required: " + minNumberInstances + "]");
      }
      catch(Exception e)
      {
         return new HealthCheckResult(Status.UNKNOWN, "Could not get number of available server instances: " + e.getMessage());
      }
   }
}
