package com.homeadvisor.robusto.spring;

import com.homeadvisor.robusto.CommandContext;
import org.springframework.web.client.RestTemplate;

@FunctionalInterface
public interface RestTemplateInvoker<T>
{
   T run(CommandContext ctx, RestTemplate restTemplate, String url);
}
