# Entando app-engine

This multi-module Maven project contains all the Entando core modules needed to build the app-engine war file.

To run the war file locally:

```
mvn clean install -DskipLicenseDownload
cd webapp/
mvn package jetty:run-war -Pjetty-local -Dspring.profiles.active=swagger -DskipTests -DskipLicenseDownload -Pderby -Pkeycloak
```

The application will be available at http://localhost:8080/entando-de-app/

More information are available on [webapp README](webapp/README.md).

## Optional Plugins

The Content Scheduler, Content Workflow, and Web Dynamic Form plugins are disabled by default. They can be included in the webapp by activating the corresponding Maven profiles during the build: `contentscheduler`, `contentworkflow`, `webdynamicform`.

## Testing

To execute all the tests:

```
mvn clean test -Ppre-deployment-verification
```

To execute a specific test:

```
mvn clean test -Ppre-deployment-verification -pl <module-name> -Dtest=<test-class-name>
```

By default the logging output in tests is minimized.
The general log level is controlled by the variable `ROOT_LOG_LEVEL`, that in tests is set to `WARN` by default.

## Environment Variables List
| Group | Name | Value [default] | Description |
| :-- | :-- | :-- | :---------------- |
|CDS  | CDS_ENABLED | true, [false] | Enable Content Delivery Server |
|| CDS_PUBLIC_URL |	http://YOUR-APP-NAME-cds.YOUR-HOST-NAME/YOUR-TENANT-ID	| | 
||	CDS_PRIVATE_URL | http://YOUR-TENANT-ID-cds-service:8080 | |	
||	CDS_PATH |	/api/v1	||	
| Keycloak/TLS | KEYCLOAK_AUTH_URL | https://YOUR-HOST-NAME/auth|
||	SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI| https://YOUR-HOST-NAME/auth/realms/entando ||
|| 	ENTANDO_APP_USE_TLS	| | protocol for the redirect to keycloak login |
||	ENTANDO_APP_ENGINE_EXTERNAL_PORT | | to force the port to use |			
|Redis server | REDIS_ACTIVE | true, [false] | to activate Redis cache management |
||	REDIS_ADDRESS |URL [redis://localhost:6379]|	Redis host address||
|| REDIS_ADDRESSES | | for HA, insert the comma separated list of nodes |
|| REDIS_MASTER_NAME | [mymaster] | To specify the name of the master node |
|| REDIS_SESSION_ACTIVE	| true, [false] |	enable storing of HTTP sessions in the Redis cluster, REDIS_ACTIVE has to be "true" too.
|| REDIS_PASSWORD  |  |  | 	
|| REDIS_USE_SENTINEL_EVENTS | [true], false | when Redis is active and Redis addresses is set, use Sentinel Monitoring
|| REDIS_IO_THREAD_POOL_SIZE | Integer, [8]	| to mitigate errors caused by missing front-end cache refresh		
|Solr | SOLR_ACTIVE	| true, false | to activate Solr search
|| SOLR_ADDRESS	|[http://localhost:8983/solr] | Solr host address
|| SOLR_CORE | string, [entando] | name of collection  
|| advancedSearch | true, false | To add the Solr config page to the CMS menu
| Tomcat server | AGENT_ENABLED | true, [false] | if true, adds the agent options to tomcat 
|| AGENT_OPTS | javaagent:~/YOUR-JARFILE.jar, [empty] | the jar file with the agent options to use 
|| TOMCAT_MAX_POST_SIZE | Enter a value in bytes, [209,715,200 bytes] | to configure connector maxPostSize | 
|| FILE_UPLOAD_MAX_SIZE | Enter a value in bytes, [52,428,800 bytes] | to configure the application upload limit		
| MISC  |  |  | |			
|| ENTANDO_BUNDLE_CLI_ETC | ${ENTANDO_BUNDLE_CLI_ETC}/hub/credentials | Credentials/parameters saved within JSON files under this path for ent bundle add hub command			
|| ENTANDO_APP_ENGINE_HEALTH_CHECK_TYPE | db.migration.strategy | [auto], skip, disabled, generate_sql | Liquibase strategy 			
|| LOG_CONFIG_FILE_PATH | | to use the logback composable feature | 			
|| ENTANDO_DOCKER_REGISTRY_OVERRIDE |  | Deprecated-for v1 bundles, to propagate to CM for plugins |
| Feature Flags | ENTANDO_FEATURE_FLAGS | comma-separated list of tags | Enable experimental features. Example: `CACHE_PIPELINE,HEADLESS_WIDGET_CONFIG` |
|| | | Available flags: `CACHE_PIPELINE`, `HEADLESS_WIDGET_CONFIG` |
|| ENTANDO_FF_DEEP_DEBUG | comma-separated list of tags | Enable deep debug logging for specific components. Example: `service-reload` |
