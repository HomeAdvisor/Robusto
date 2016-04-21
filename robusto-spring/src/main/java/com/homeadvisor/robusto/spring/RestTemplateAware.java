package com.homeadvisor.robusto.spring;

import org.springframework.web.client.RestTemplate;

public interface RestTemplateAware
{
   void setRestTemplate(RestTemplate template);
}
