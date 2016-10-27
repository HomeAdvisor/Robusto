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

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;

/**
 * Extension of {@link HystrixThreadPoolProperties} that delegates all property
 * lookups to a Spring {@link Environment} instead of archauis. All values retrieved
 * from the environment are wrapped in the proper {@link HystrixProperty} class and will
 * return the default values from the parent class when no match is found in the
 * environment.
 * <br/><br/>
 * To configure each thread pool in your app, you'll need to know the thread pool
 * name. The structure of configs is as follows:
 * <br/><br/>
 * <code>hystrix.threadpool.[thread-pool-key].[property-name]=...</code>
 * <br/><br/>
 * Where thread-pool-key comes from {@link com.homeadvisor.robusto.ApiCommand} and
 * property-name is one of the available members of HystrixThreadPoolProperties.
 * <br/><br/>
 * This class can be used in 2 ways. You can register it as the "global" Hystrix
 * properties provider via {@link com.netflix.hystrix.strategy.HystrixPlugins}
 * or you can use {@link #createSetter()} to use it with individual {@link
 * com.homeadvisor.robusto.ApiCommand}s. This latter strategy is preferred
 * if your application is going to have multiple clients that dont all need or
 * want to use the Spring environment as the provider.
 */
public class SpringThreadPoolProperties extends HystrixThreadPoolProperties
{
   private final static Logger LOG = LoggerFactory.getLogger(SpringThreadPoolProperties.class);

   private final String configPrefix;

   private final Environment environment;

   private final String commandName;

   /**
    * Create a new HystrixThreadPoolProperties that is backed by Spring environment.
    * Given a configuration prefix and thread key, this class cna lookup all available
    * Hystrix thread pool properties from the Spring environment.
    * @param environment Spring environment.
    * @param configPrefix Configuration prefix. Useful when your application has multiple
    *                     clients that each need to be configured indivud
    * @param key
    */
   public SpringThreadPoolProperties(Environment environment, String configPrefix, HystrixThreadPoolKey key)
   {
      super(key);
      this.environment = environment;
      this.configPrefix = configPrefix + ".client.threadpool.";
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
            .withCoreSize(coreSize().get())
            .withKeepAliveTimeMinutes(keepAliveTimeMinutes().get())
            .withMaxQueueSize(maxQueueSize().get())
            .withMetricsRollingStatisticalWindowBuckets(metricsRollingStatisticalWindowBuckets().get())
            .withMetricsRollingStatisticalWindowInMilliseconds(metricsRollingStatisticalWindowInMilliseconds().get())
            .withQueueSizeRejectionThreshold(queueSizeRejectionThreshold().get());
   }

   @Override
   public HystrixProperty<Integer> coreSize()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".coreSize",
                  Integer.class,
                  environment.getProperty("hystrix.threadpool.default.coreSize", Integer.class, 5)));
   }

   @Override
   public HystrixProperty<Integer> keepAliveTimeMinutes()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".keepAliveTimeMinutes",
                  Integer.class,
                  super.keepAliveTimeMinutes().get()));
   }

   @Override
   public HystrixProperty<Integer> maxQueueSize()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".maxQueueSize",
                  Integer.class,
                  super.maxQueueSize().get()));
   }

   @Override
   public HystrixProperty<Integer> queueSizeRejectionThreshold()
   {
      return HystrixProperty.Factory.asProperty(
            environment.getProperty(
                  configPrefix + commandName + ".queueSizeRejectionThreshold",
                  Integer.class,
                  super.queueSizeRejectionThreshold().get()));
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
}
