# Robusto API Client Changelog

## October 27, 2016

* CHANGE: The default Hystrix threadpool size is now 5 instead of 10. This can be overridden globally using the config name hystrix.threadpool.default.coreSize, or per command using standard configuration naming conventions.
* FIX: The default ObjectMapper in SpringClientConfiguration now sets DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES instead of DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES. This was a typo in the original commit.
