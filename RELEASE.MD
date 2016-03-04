Mar 4, 2016

* Can now set your choice of HTTP factory in SpringRestClient
* Autowire Spring Environment into SpringRestClient
* Can now configure number of retries and Hystrix command and thread pool setting per ApiCommand
* Can now configure Spring connect and request timeouts per ApiCommand
* Spring interceptors are now ordered using AnnotationAwareOrderComparator (as long as you add them using SpringRestClient.getInterceptors())
* Added new ResponseTimeInterceptor to Spring package to log total request/response time (in ms)
