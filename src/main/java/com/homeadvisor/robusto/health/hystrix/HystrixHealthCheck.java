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
package com.homeadvisor.robusto.health.hystrix;

import com.homeadvisor.robusto.health.HealthCheckResult;

import com.homeadvisor.robusto.health.Status;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;
import com.homeadvisor.robusto.health.HealthCheck;

/**
 * Implemenation of {@link HealthCheck} that bases health off of
 * a hystrix command pool.
 */
public class HystrixHealthCheck implements HealthCheck
{
   /**
    * The minimum number of failures in a bucket required to report unhealthy.
    */
   private int minNumFailures;

   /**
    * Command group key uses to lookup Hystrix metrics. Should likely be the
    * same as service name.
    */
   private String key;

   public HystrixHealthCheck(String key, int minNumFailures)
   {
      this.key            = key;
      this.minNumFailures = minNumFailures;
   }

   @Override
   public HealthCheckResult doCheck()
   {
      //
      // Lookup the command group using the provided key. These may be null if
      // no ApiCommands have been created and executed so no need to panic.
      //

      try
      {
         HystrixCommandKey cmdKey = HystrixCommandKey.Factory.asKey(key);

         if (cmdKey == null)
         {
            return new HealthCheckResult(Status.HEALTHY);
         }

         HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(cmdKey);

         if (metrics == null)
         {
            return new HealthCheckResult(Status.HEALTHY);
         }

         //
         // As this point we should have viable metrics.
         //

         int windowSize = metrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get() / 1000;

         long failCount = metrics.getRollingCount(HystrixRollingNumberEvent.FAILURE);

         if (failCount >= minNumFailures)
         {
            return new HealthCheckResult(Status.UNHEALTHY, "Command has " + failCount + " failures in last " + windowSize + " seconds");
         }
         else
         {
            return new HealthCheckResult(Status.HEALTHY);
         }
      }
      catch (Exception e)
      {
         return new HealthCheckResult(Status.UNKNOWN, "Could not get Hystrix metrics for command key " + key + ": " + e.getMessage());
      }
   }
}
