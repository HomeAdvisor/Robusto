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
package com.homeadvisor.robusto;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.retry.backoff.FixedBackOffPolicy;

import java.util.Random;

/**
 * Unit test for {@link ApiCommand}.
 */
@RunWith(PowerMockRunner.class)
public class ApiCommandTest extends TestCase
{
   /**
    * Tests that the ApiCommand.Builder prevents building an ApiCommand when
    * no UriProvider is provided.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMissingUriProvider()
   {
      ApiCommand command = ApiCommand.builder()
            .withRemoteServiceCallback(new SuccessfulRemoteCallback())
            .build();
   }

   /**
    * Tests that the ApiCommand.Builder prevents building an ApiCommand when
    * no RemoteServiceCallback is provided.
    */
   @Test(expected = IllegalArgumentException.class)
   public void testMissingRemoteServiceCallback()
   {
      ApiCommand command = ApiCommand.builder()
            .withUriProvider(new ConstantUriProvider<>(""))
            .build();
   }

   /**
    * Tests that UriProvider failures are retried and eventually succeed.
    */
   @Test
   public void testUriProviderFailures()
   {
      ApiCommand command = ApiCommand.builder()
            .withUriProvider(new FlakyUriProvider(0.5))
            .withRemoteServiceCallback(new SuccessfulRemoteCallback())
            .withNumberOfRetries(5)
            .withBackoffPolicy(createOneSecondBackoffPolicy())
            .withCommandTimeoutInMilliseconds(10000)
            .build();

      Object result = command.execute();

      assertNotNull(result);
   }
   /**
    * Tests that RemoteServiceCallback failures are retried and eventually succeed.
    */
   @Test
   public void testRemoteCallbackFailuresFailures()
   {
      ApiCommand command = ApiCommand.builder()
            .withUriProvider(new ConstantUriProvider<>(""))
            .withRemoteServiceCallback(new FlakyRemoteServiceCallback(0.5,true))
            .withNumberOfRetries(5)
            .withBackoffPolicy(createOneSecondBackoffPolicy())
            .withCommandTimeoutInMilliseconds(10000)
            .build();

      Object result = command.execute();

      assertNotNull(result);
   }

   //
   // Helper classes
   //

   /**
    * Simple DTO to mock returns from remote calls.
    */
   private class DummyDto
   {

   }

   /**
    * Simple RemoteServiceCallback that always returns a new DummyDto.
    */
   private class SuccessfulRemoteCallback implements RemoteServiceCallback
   {
      @Override
      public Object run(String url)
      {
         return new DummyDto();
      }
   }

   /**
    * Simple RemoteServiceCallback that always throws a {@link RetryableApiCommandException}.
    */
   private class RetryableRemoteCallback implements RemoteServiceCallback
   {
      @Override
      public Object run(String url)
      {
         throw new RetryableApiCommandException("");
      }
   }

   /**
    * Simple RemoteServiceCallback that always throws a {@link NonRetryableApiCommandException}.
    */
   private class NonRetryableRemoteCallback implements RemoteServiceCallback
   {
      @Override
      public Object run(String url)
      {
         throw new NonRetryableApiCommandException("");
      }
   }

   /**
    * Simple RemoteServiceCallback that times outs to mock remote service
    * timeouts.
    */
   private class TimeoutRemoteCallback implements RemoteServiceCallback
   {
      private final long millis;

      public TimeoutRemoteCallback(long millis)
      {
         this.millis = millis;
      }

      @Override
      public Object run(String url)
      {
         try
         {
            Thread.sleep(millis);
         }
         catch(Exception e)
         {
            // Dont care
         }

         return null;
      }
   }

   /**
    * Implemenation of {@link RemoteServiceCallback} that fails at a certain percentage.
    */
   private class FlakyRemoteServiceCallback implements RemoteServiceCallback
   {
      private final double pct;

      private final boolean retryable;

      public FlakyRemoteServiceCallback()
      {
         this(.5, true);
      }

      public FlakyRemoteServiceCallback(double pct)
      {
         this(pct, true);
      }

      public FlakyRemoteServiceCallback(double pct, boolean retryable)
      {
         this.pct = pct;
         this.retryable = retryable;
      }

      @Override
      public Object run(String url)
      {
         if(new Random().nextDouble() >= pct)
         {
            return new DummyDto();
         }
         else
         {
            if(retryable)
            {
               throw new RetryableApiCommandException("Failed to lookup URI - will retry");
            }
            else
            {
               throw new NonRetryableApiCommandException("Failed to lookup URI - will NOT retry");
            }
         }
      }
   }

   /**
    * Implementation of UriProvider that always throws a {@link RetryableApiCommandException}
    * 50% of the time.
    */
   private class FlakyUriProvider implements UriProvider
   {
      private final double pct;

      public FlakyUriProvider()
      {
         this(.5);
      }

      public FlakyUriProvider(double pct)
      {
         this.pct = pct;
      }

      @Override
      public Object execute(RemoteServiceCallback callback)
      {
         if(new Random().nextDouble() >= pct)
         {
            return callback.run("http://fakehost:1234");
         }
         else
         {
            throw new RetryableApiCommandException("Failed to lookup URI");
         }
      }
   }

   /**
    * Create a {@link FixedBackOffPolicy} of 1 second. The default is
    * already 1 second in that class, but want to protect against that
    * changing in future iterations.
    * @return New FixedBackOffPolicy with 1 second backoff.
    */
   public FixedBackOffPolicy createOneSecondBackoffPolicy()
   {
      FixedBackOffPolicy policy = new FixedBackOffPolicy();
      policy.setBackOffPeriod(1000);
      return policy;
   }
}
