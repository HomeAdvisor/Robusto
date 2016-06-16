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
package com.homeadvisor.robusto.spring;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.homeadvisor.robusto.ClientConfiguration;
import com.homeadvisor.robusto.spring.config.SpringCommandProperties;
import com.homeadvisor.robusto.spring.config.SpringThreadPoolProperties;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.validation.DataBinder;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.List;

/**
 * Encapsulates additional config options that make sense for the Spring client.
 */
public class SpringClientConfiguration extends ClientConfiguration
{
   private final static Logger LOG = LoggerFactory.getLogger(SpringClientConfiguration.class);

   private Environment environment;

   private String configPrefix;

   /**
    * Initializes using a Spring {@link StandardEnvironment}.
    * @deprecated Adding for backwards compatibility. Please use
    * {@link #SpringClientConfiguration(Environment,String)} instead.
    */
   public SpringClientConfiguration()
   {
      environment = new StandardEnvironment();
      configPrefix = "robusto";
   }

   /**
    * Initializes using the provided Spring {@link Environment}.
    * @param environment Spring Environment.
    */
   public SpringClientConfiguration(Environment environment, String configPrefix)
   {
      this.environment = environment;
      this.configPrefix = configPrefix;
   }

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
    * Default HTTP client connection timeout in ms (default 2000).
    */
   private int connectTimeout = 2000;

   /**
    * Default HTTP client request timeout in ms (default 2000).
    */
   private int requestTimeout = 2000;

   /**
    * Determine if all requests and subsequent responses should be logged at
    * the DEBUG logging level (default is false). The logging will include
    * headers and entity bodies for both requests and responses, so use with
    * caution as log files may fill up fast.
    */
   private boolean httpLoggingDebug = true;

   /**
    * Determine if the client should log the time it takes to get a response
    * at debug level.
    * Default is true.
    */
   private boolean responseTimingDebug = true;

   protected String getConfigPrefix()
   {
      return configPrefix;
   }

   protected void setConfigPrefix(String configPrefix)
   {
      this.configPrefix = configPrefix;
   }

   //
   // Overrides for base API client configuration; check for settings in the
   // environment first and fallback to base class as default.
   //

   /**
    * Creates a new {@link SpringCommandProperties} for the given command name
    * which will delegate all lookups to the environment.
    * @param name Name of the command group.
    * @return A {@link HystrixCommandProperties.Setter} for the given command
    * name which can be used when building {@link
    * com.homeadvisor.robusto.ApiCommand.Builder#withHystrixCommandProperties(HystrixCommandProperties.Setter) ApiCommands}.
    */
   @Override
   protected HystrixCommandProperties.Setter buildCustomCommandProperties(String name)
   {
      return new SpringCommandProperties(
            environment,
            configPrefix,
            HystrixCommandKey.Factory.asKey(name))
            .createSetter();
   }

   @Override
   protected HystrixThreadPoolProperties.Setter buildCustomThreadPoolProperties(String name)
   {
      return new SpringThreadPoolProperties(
            environment,
            configPrefix,
            HystrixThreadPoolKey.Factory.asKey(name))
            .createSetter();
   }

   @Override
   public int getNumRetries()
   {
      return getProperty(getConfigPrefix() + ".client.numRetries", super.getNumRetries());
   }

   @Override
   public int getNumRetries(String name)
   {
      return getProperty(getConfigPrefix() + ".client.command." + name.toLowerCase() + ".numRetries", getNumRetries());
   }

   @Override
   public int getHystrixHealthNumFailures()
   {
      return getProperty(getConfigPrefix() + ".client.healthCheck.hystrix.minFailures", super.getHystrixHealthNumFailures());
   }

   @Override
   public boolean isCacheEnabled(String cacheName)
   {
      return getProperty(getConfigPrefix() + ".client.cache." + cacheName + ".enabled", super.isCacheEnabled(cacheName));
   }

   @Override
   public String getCacheType(String cacheName)
   {
      return getProperty(getConfigPrefix() + ".client.cache." + cacheName + ".type", super.getCacheType(cacheName));
   }

   @Override
   public String getCacheConfig(String cacheName)
   {
      return getProperty(getConfigPrefix() + ".client.cache." + cacheName + ".config", super.getCacheConfig(cacheName));
   }

   //
   // Spring Client specific settings
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
      return getProperty(getConfigPrefix() + ".client.defaultAcceptTypes", defaultAcceptTypes);
   }

   public boolean isResponseTimingDebug()
   {
      return getProperty(getConfigPrefix() + ".client.responseTimingDebug", responseTimingDebug);
   }

   public int getConnectTimeout()
   {
      return getProperty(getConfigPrefix() + ".client.connectTimeout", connectTimeout);
   }

   public void setConnectTimeout(int connectTimeout)
   {
      this.connectTimeout = connectTimeout;
   }

   public int getRequestTimeout()
   {
      return getProperty(getConfigPrefix() + ".client.requestTimeout", requestTimeout);
   }

   public void setRequestTimeout(int requestTimeout)
   {
      this.requestTimeout = requestTimeout;
   }

   public boolean isHttpLoggingDebug()
   {
      return getProperty(getConfigPrefix() + ".client.httpLoggingDebug", httpLoggingDebug);
   }

   public void setHttpLoggingDebug(boolean httpLoggingDebug)
   {
      this.httpLoggingDebug = httpLoggingDebug;
   }

   /**
    * Provide a way to get connect timeouts per command.
    * @param name Logical command name. See {@link SpringRestClient#buildCommandGroupName()}.
    * @return Connect timeout for that command, or the default.
    */
   public int getConnectTimeout(String name)
   {
      return getProperty(getConfigPrefix() + ".client.command." + name.toLowerCase() + ".connectTimeout", getConnectTimeout());
   }

   /**
    * Provide a way to get request timeouts per command.
    * @param name Logical command name. See {@link SpringRestClient#buildCommandGroupName()}.
    * @return Request timeout for that command, or the default.
    */
   public int getRequestTimeout(String name)
   {
      return getProperty(getConfigPrefix() + ".client.command." + name.toLowerCase() + ".requestTimeout", getRequestTimeout());
   }

   /**
    * Allows customizing Jackson {@link ObjectMapper} per command. This just
    * returns the same ObjectMapper as {@link #buildDefaultJacksonObjectMapper()}.
    * @param commandName Command name.
    * @return Configured ObjectMapper.
    */
   public ObjectMapper buildCustomJacksonObjectMapper(String commandName)
   {
      return buildDefaultJacksonObjectMapper();
   }

   /**
    * Build a default Jackson ObjectMapper. The default implementation is to
    * include non-null, ignore uknown properties on deserialization, and use
    * the date format yyyy-MM-dd'T'HH:mm:ss.SSSZ.
    * @return A Jackson ObjectMapper.
    */
   protected ObjectMapper buildDefaultJacksonObjectMapper()
   {
      /* Cant use unless Spring 4.x is being used throughout
      return new Jackson2ObjectMapperBuilder()
            .serializationInclusion(JsonInclude.Include.NON_NULL)
            .failOnUnknownProperties(false)
            .dateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"))
            .featuresToEnable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME)
            .annotationIntrospector(AnnotationIntrospector.pair(
                  new JacksonAnnotationIntrospector(),
                  new JaxbAnnotationIntrospector()))
            .build();
            */
      ObjectMapper mapper = new ObjectMapper();
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
      mapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
      mapper.enable(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME);
      mapper.registerModule(new JodaModule());

      AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
      AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
      mapper.setAnnotationIntrospector(AnnotationIntrospector.pair(primary, secondary));

      return mapper;
   }

   //
   // These are utility methods for getting config values from the Spring
   // environment.
   //

   /**
    * Utility method to get a property from the environment.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public String getProperty(String name, String defaultValue)
   {
      try
      {
         return environment.getProperty(name, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as a Boolean.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public Boolean getProperty(String name, Boolean defaultValue)
   {
      try
      {
         return environment.getProperty(name, Boolean.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as an Integer.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public Integer getProperty(String name, Integer defaultValue)
   {
      try
      {
         return environment.getProperty(name, Integer.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as an Integer.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public Long getProperty(String name, Long defaultValue)
   {
      try
      {
         return environment.getProperty(name, Long.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as a Float.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public Float getProperty(String name, Float defaultValue)
   {
      try
      {
         return environment.getProperty(name, Float.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as a Double.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public Double getProperty(String name, Double defaultValue)
   {
      try
      {
         return environment.getProperty(name, Double.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   /**
    * Utility method to get a property from the environment as a List.
    * @param name Property name
    * @param defaultValue Default value if not found.
    * @return Value of property if set, or default value if not set.
    */
   public <T> List<T> getProperty(String name, List<T> defaultValue)
   {
      try
      {
         return environment.getProperty(name, List.class, defaultValue);
      }
      catch (Exception e)
      {
         LOG.warn("Error getting value for property {}", name, e);
         return defaultValue;
      }
   }

   private class EnvironmentPropertValues implements PropertyValues
   {
      private final Environment environment;

      private final String prefix;

      public EnvironmentPropertValues(Environment environment, String prefix)
      {
         this.environment = environment;
         this.prefix = prefix;
      }

      @Override
      public PropertyValue[] getPropertyValues()
      {
         return new PropertyValue[0];
      }

      @Override
      public PropertyValue getPropertyValue(String propertyName)
      {
         if(contains(propertyName))
         {
            return new PropertyValue(propertyName, environment.getProperty(prefix + "." + propertyName));
         }

         return null;
      }

      @Override
      public PropertyValues changesSince(PropertyValues old)
      {
         return null;
      }

      @Override
      public boolean contains(String propertyName)
      {
         return environment.getProperty(prefix + "." + propertyName) != null;
      }

      @Override
      public boolean isEmpty()
      {
         return false;
      }
   }
}
