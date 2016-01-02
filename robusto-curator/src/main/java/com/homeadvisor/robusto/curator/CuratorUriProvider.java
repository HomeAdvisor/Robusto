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
package com.homeadvisor.robusto.curator;

import com.google.common.base.Throwables;
import com.homeadvisor.robusto.ApiCommand;
import com.homeadvisor.robusto.RemoteServiceCallback;
import com.homeadvisor.robusto.RetryableApiCommandException;
import com.homeadvisor.robusto.UriProvider;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.ServiceProvider;
import org.apache.curator.x.discovery.details.ServiceProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;

/**
 * Implementation of {@link UriProvider} backed by curator service discovery.
 * Every invoccation of an {@link ApiCommand} will result in a service lookup
 * and potentially a different service instance. The instance that is returned
 * by each lookup is completely dependent on the state of the Service Provider,
 * including instance states, provider strategy, etc.
 */
public class CuratorUriProvider<T> implements UriProvider<T>
{
   private final static Logger LOG = LoggerFactory.getLogger(CuratorUriProvider.class);

   private final ServiceProvider serviceProvider;

   private final String serviceName;

   public CuratorUriProvider(ServiceProvider template)
   {
      this(template, null);
   }

   public CuratorUriProvider(ServiceProvider template, String name)
   {
      serviceProvider = template;
      serviceName = name;
   }

   @Override
   public T execute(RemoteServiceCallback<T> callback)
   {
      ServiceInstance instance = null;

      try
      {
         instance = serviceProvider.getInstance();

         if(instance == null)
         {
            if(serviceName == null)
            {
               throw new RetryableApiCommandException("No available instances were found");
            }
            else
            {
               throw new RetryableApiCommandException("No available instances were found for service " + serviceName);
            }
         }

         LOG.debug("Using curator service instance {}", instance.getId());

         return callback.run(instance.buildUriSpec());
      }
      catch (Exception e)
      {
         if(instance != null &&
               (e instanceof RetryableApiCommandException || e instanceof SocketTimeoutException))
         {
            LOG.debug("Exception {} is being noted as error on instance", e.getCause());
            serviceProvider.noteError(instance);
         }

         Throwables.propagate(e);

         //
         // Will never reach this, but need to make compiler happy
         //

         return null;
      }
   }
}
