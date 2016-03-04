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
package com.homeadvisor.robusto.spring.config;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Extension of {@link HystrixCommandProperties} that delegates all property
 * lookups to a Spring {@link Environment} instead of archauis. All values retrieved
 * from the environment are wrapped in the proper {@link HystrixProperty} class and will
 * return the default values from the parent class when no match is found in the
 * environment.
 * <br/><br/>
 * To configure each command in your app, you'll need to know the command key
 * name. The structure of configs is as follows:
 * <br/><br/>
 * <code>hystrix.command.[command-key].[property-name]=...</code>
 * <br/><br/>
 * Where command-key comes from {@link com.homeadvisor.robusto.ApiCommand} and
 * property-name is one of the available members of HystrixCommandProperties.
 * <br/><br/>
 * This class can be used in 2 ways. You can register it as the "global" Hystrix
 * properties provider via {@link com.netflix.hystrix.strategy.HystrixPlugins}
 * or you can use {@link #createSetter()} to use it with individual {@link
 * com.homeadvisor.robusto.ApiCommand}s. This latter strategy is preferred
 * if your application is going to have multiple clients that dont all need or
 * want to use the Spring environment as the provider.
 */
public class SpringCommandProperties extends HystrixCommandProperties
{
   private final static Logger LOG = LoggerFactory.getLogger(SpringCommandProperties.class);

   private final String configPrefix;

   private final Environment environment;

   private final String commandName;

   public SpringCommandProperties(Environment environment, String configPrefix, HystrixCommandKey key)
   {
      super(key);
      this.environment = environment;
      this.configPrefix = configPrefix + ".client.command.";
      this.commandName = key.name().toLowerCase();
   }

   /**
    * Use this to override specific instances of {@link com.homeadvisor.robusto.ApiCommand}s
    * instead of registering this class with
    * {@link com.netflix.hystrix.strategy.HystrixPlugins#registerPropertiesStrategy(HystrixPropertiesStrategy)}
    * @return Instance of a Setter.
    */
   public Setter createSetter()
   {
      return Setter()
            .withCircuitBreakerEnabled(circuitBreakerEnabled().get())
            .withCircuitBreakerErrorThresholdPercentage(circuitBreakerErrorThresholdPercentage().get())
            .withCircuitBreakerForceClosed(circuitBreakerForceClosed().get())
            .withCircuitBreakerForceOpen(circuitBreakerForceOpen().get())
            .withCircuitBreakerRequestVolumeThreshold(circuitBreakerRequestVolumeThreshold().get())
            .withCircuitBreakerSleepWindowInMilliseconds(circuitBreakerSleepWindowInMilliseconds().get())
            .withExecutionIsolationSemaphoreMaxConcurrentRequests(executionIsolationSemaphoreMaxConcurrentRequests().get())
            .withExecutionIsolationStrategy(executionIsolationStrategy().get())
            .withExecutionIsolationThreadInterruptOnTimeout(executionIsolationThreadInterruptOnTimeout().get())
            .withExecutionTimeoutEnabled(executionTimeoutEnabled().get())
            .withExecutionTimeoutInMilliseconds(executionTimeoutInMilliseconds().get())
            .withFallbackEnabled(fallbackEnabled().get())
            .withFallbackIsolationSemaphoreMaxConcurrentRequests(fallbackIsolationSemaphoreMaxConcurrentRequests().get())
            .withMetricsHealthSnapshotIntervalInMilliseconds(metricsHealthSnapshotIntervalInMilliseconds().get())
            .withMetricsRollingPercentileBucketSize(metricsRollingPercentileBucketSize().get())
            .withMetricsRollingPercentileEnabled(metricsRollingPercentileEnabled().get())
            .withMetricsRollingPercentileWindowBuckets(metricsRollingStatisticalWindowBuckets().get())
            .withMetricsRollingPercentileWindowInMilliseconds(metricsRollingPercentileWindowInMilliseconds().get())
            .withMetricsRollingStatisticalWindowBuckets(metricsRollingStatisticalWindowBuckets().get())
            .withMetricsRollingStatisticalWindowInMilliseconds(metricsRollingStatisticalWindowInMilliseconds().get())
            .withRequestCacheEnabled(requestCacheEnabled().get())
            .withRequestLogEnabled(requestLogEnabled().get());
   }

   @Override
   public HystrixProperty<Boolean> circuitBreakerEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerEnabled",
                  Boolean.class,
                  super.circuitBreakerEnabled().get()));
   }

   @Override
   public HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerErrorThresholdPercentage",
                  Integer.class,
                  super.circuitBreakerErrorThresholdPercentage().get()));
   }

   @Override
   public HystrixProperty<Boolean> circuitBreakerForceClosed()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerEnabled",
                  Boolean.class,
                  super.circuitBreakerEnabled().get()));
   }

   @Override
   public HystrixProperty<Boolean> circuitBreakerForceOpen()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerForceOpen",
                  Boolean.class,
                  super.circuitBreakerForceOpen().get()));
   }

   @Override
   public HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerRequestVolumeThreshold",
                  Integer.class,
                  super.circuitBreakerRequestVolumeThreshold().get()));
   }

   @Override
   public HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".circuitBreakerSleepWindowInMilliseconds",
                  Integer.class,
                  super.circuitBreakerSleepWindowInMilliseconds().get()));
   }

   @Override
   public HystrixProperty<Integer> executionIsolationSemaphoreMaxConcurrentRequests()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionIsolationSemaphoreMaxConcurrentRequests",
                  Integer.class,
                  super.executionIsolationSemaphoreMaxConcurrentRequests().get()));
   }

   @Override
   public HystrixProperty<ExecutionIsolationStrategy> executionIsolationStrategy()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionIsolationStrategy",
                  ExecutionIsolationStrategy.class,
                  ExecutionIsolationStrategy.THREAD));
   }

   @Override
   public HystrixProperty<Boolean> executionIsolationThreadInterruptOnTimeout()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionIsolationThreadInterruptOnTimeout",
                  Boolean.class,
                  super.executionIsolationThreadInterruptOnTimeout().get()));
   }

   @Override
   public HystrixProperty<String> executionIsolationThreadPoolKeyOverride()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionIsolationThreadPoolKeyOverride",
                  String.class,
                  super.executionIsolationThreadPoolKeyOverride().get()));
   }

   @Override
   public HystrixProperty<Integer> executionTimeoutInMilliseconds()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionTimeoutInMilliseconds",
                  Integer.class,
                  8000));
   }

   @Override
   public HystrixProperty<Boolean> executionTimeoutEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".executionTimeoutEnabled",
                  Boolean.class,
                  super.executionTimeoutEnabled().get()));
   }

   @Override
   public HystrixProperty<Integer> fallbackIsolationSemaphoreMaxConcurrentRequests()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".fallbackIsolationSemaphoreMaxConcurrentRequests",
                  Integer.class,
                  super.fallbackIsolationSemaphoreMaxConcurrentRequests().get()));
   }

   @Override
   public HystrixProperty<Boolean> fallbackEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".fallbackEnabled",
                  Boolean.class,
                  false));
   }

   @Override
   public HystrixProperty<Integer> metricsHealthSnapshotIntervalInMilliseconds()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsHealthSnapshotIntervalInMilliseconds",
                  Integer.class,
                  super.metricsHealthSnapshotIntervalInMilliseconds().get()));
   }

   @Override
   public HystrixProperty<Integer> metricsRollingPercentileBucketSize()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingPercentileBucketSize",
                  Integer.class,
                  super.metricsRollingPercentileBucketSize().get()));
   }

   @Override
   public HystrixProperty<Boolean> metricsRollingPercentileEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingPercentileEnabled",
                  Boolean.class,
                  super.metricsRollingPercentileEnabled().get()));
   }

   @Override
   public HystrixProperty<Integer> metricsRollingPercentileWindowInMilliseconds()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingPercentileWindowInMilliseconds",
                  Integer.class,
                  super.metricsRollingPercentileWindowInMilliseconds().get()));
   }

   @Override
   public HystrixProperty<Integer> metricsRollingPercentileWindowBuckets()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingPercentileWindowBuckets",
                  Integer.class,
                  super.metricsRollingPercentileWindowBuckets().get()));
   }

   @Override
   public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingStatisticalWindowInMilliseconds",
                  Integer.class,
                  super.metricsRollingStatisticalWindowInMilliseconds().get()));
   }

   @Override
   public HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".metricsRollingStatisticalWindowBuckets",
                  Integer.class,
                  super.metricsRollingStatisticalWindowBuckets().get()));
   }

   @Override
   public HystrixProperty<Boolean> requestCacheEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".requestCacheEnabled",
                  Boolean.class,
                  super.requestCacheEnabled().get()));
   }

   @Override
   public HystrixProperty<Boolean> requestLogEnabled()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".requestLogEnabled",
                  Boolean.class,
                  super.requestLogEnabled().get()));
   }
}
