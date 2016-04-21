package com.homeadvisor.robusto.spring;

import org.springframework.web.client.RestTemplate;

public class RestTemplateCallback<T> extends SpringInstanceCallback<T> implements RestTemplateAware
{
   private RestTemplateInvoker<T> invoker;

   private RestTemplate restTemplate;

   public RestTemplateCallback(RestTemplateInvoker<T> invoker)
   {
      this.invoker = invoker;
   }

   @Override
   public void setRestTemplate(RestTemplate restTemplate)
   {
      this.restTemplate = restTemplate;
   }

   @Override
   public T runWithUrl(String url)
   {
      return invoker.run(getContext(), restTemplate, url);
   }
}
