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
package com.homeadvisor.robusto.health;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Interface for clients that wish to provide a measure of health. Provides
 * some default behavior, but implementing clients may override if they need.
 */
public interface HealthCheckAwareClient
{
   /**
    * List of healthchecks to execute to determine overall health.
    */
   List<HealthCheck> checks = new ArrayList<>();

   //
   // Interface methods
   //

   /**
    * Add a new {@link HealthCheck} to be executed as part of the overall
    * client health. HealthChecks will be executed in the order in which they
    * are registered.
    * @param check
    */
   default void registerCheck(HealthCheck check)
   {
      checks.add(check);
   }

   /**
    * Main method for determining client health. Default implementation is to
    * simply execute all registered handlers and combine them into a single
    * list of results.
    * @return List of individual results.
    */
   default Map<String, HealthCheckResult> doAllChecks()
   {
      Map<String, HealthCheckResult> results = new TreeMap<>();

      for(HealthCheck check : checks)
      {
         results.put(check.getClass().getSimpleName(), check.doCheck());
      }

      return results;
   }
}
