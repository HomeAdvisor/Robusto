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
package com.homeadvisor.robusto.health;

/**
 * Represents the result of a single {@link HealthCheck} check.
 */
public class HealthCheckResult
{
   /**
    * State of the health check.
    */
   public Status status = Status.HEALTHY;

   /**
    * Optional description to elaborate on the status enum.
    */
   public String description;

   /**
    * Convenience constructor to set all required fields.
    * @param status Status of the check.
    */
   public HealthCheckResult(Status status)
   {
      this.status = status;
   }

   /**
    * Convenience constructor to set all fields.
    * @param status Status of the check.
    * @param description Optional descriptipn to elaborate on the status.
    */
   public HealthCheckResult(Status status, String description)
   {
      this.status = status;
      this.description = description;
   }
}
