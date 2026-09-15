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

The test suite runs under the `pre-deployment-verification` Maven profile — without it, surefire
is skipped and no tests execute.

### Quick commands

To execute all the tests:

```
mvn clean test -Ppre-deployment-verification
```

To execute a specific test:

```
mvn clean test -Ppre-deployment-verification -pl <module-name> -Dtest=<test-class-name>
```

### Reactor test runner (`run-reactor-tests.sh`)

To run the tests of one or more reactor modules, the repo ships a helper at the project root that
wraps the Maven invocation, chooses the right build strategy, and prints a per-module PASS/FAIL
summary (logs are saved under `test-results/`).

```
# interactive module picker (UP/DOWN move, SPACE select, a=all, n=none, ENTER confirm, q quit)
./run-reactor-tests.sh

# non-interactive: test only the given module(s)
./run-reactor-tests.sh engine cms-plugin

# test every module, no prompt
ASSUME_YES=1 ./run-reactor-tests.sh
```

When a **subset** of modules is selected, the script first builds the selected modules and their
upstream dependencies **without** tests (`install -DskipTests`), then runs the tests for the
selected modules only (no `-am`, so dependencies are not re-tested). Selecting all modules runs the
whole reactor in a single pass.

Environment overrides:

| Variable | Default | Effect |
| :-- | :-- | :-- |
| `PROFILE` | `pre-deployment-verification` | Maven profile that enables the tests |
| `MVN_OPTS` | _(empty)_ | extra Maven options, e.g. `-o` for offline |
| `ASSUME_YES` | `0` | skip the picker and test every module |
| `DRY_RUN` | `0` | print the Maven command(s) without running them |

In a non-interactive shell (CI or a pipe) the picker is skipped automatically and all modules are
tested.

By default the logging output in tests is minimized. See [Logging](#logging) below for how to get
verbose/`DEBUG` output, both for the running webapp and for test runs (they work differently).

## Logging

Logging is configured via `engine/src/main/resources/base.xml` (logback) and driven by two environment
variables:
- `ROOT_LOG_LEVEL` — the root logger level. Defaults to `DEBUG` when running the webapp; overridden to
  `WARN` when running tests (see `pom.xml` surefire configuration).
- `LOG_LEVEL` — the console (`STDOUT`) appender threshold. Defaults to `WARN`, regardless of
  `ROOT_LOG_LEVEL`.

To run the webapp locally with `DEBUG` logs printed to the console:

```
cd webapp/
LOG_LEVEL=DEBUG mvn package jetty:run-war -Pjetty-local -Dspring.profiles.active=swagger -DskipTests -DskipLicenseDownload -Pderby -Pkeycloak
```

`ROOT_LOG_LEVEL` does not need to be set for this, since it already defaults to `DEBUG` outside of tests;
`LOG_LEVEL` is the variable that actually gates what reaches the console.

Test runs are different: `entando-engine`'s test-jar ships `logback-test.xml`, which every other module
picks up on its test classpath. It hardcodes `<root level="INFO">` (with explicit per-package `DEBUG`
overrides only for a couple of Spring test loggers), so **`ROOT_LOG_LEVEL`/`LOG_LEVEL` have no effect on
test runs** — only on the running webapp. To get `DEBUG` output from a test run, point Logback at a
throwaway config instead:

```
cat > /tmp/logback-debug.xml <<'EOF'
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder><pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern></encoder>
    </appender>
    <root level="DEBUG">
        <appender-ref ref="STDOUT"/>
    </root>
</configuration>
EOF
mvn clean test -Ppre-deployment-verification -pl <module-name> -Dtest=<test-class-name> \
    -DargLine=-Dlogback.configurationFile=/tmp/logback-debug.xml
```

(Alternatively, add a one-off `<logger name="<package>" level="DEBUG"/>` line to
`engine/src/test/resources/logback-test.xml` before running the test — cheaper for a quick, throwaway
check, but remember to revert it since it's a shared test resource.)

## Environment Variables List
| Group         | Name                                                   | Value [default]                                        | Description                                                                                   |
|:--------------|:-------------------------------------------------------|:-------------------------------------------------------|:----------------------------------------------------------------------------------------------|
| CDS           | CDS_ENABLED                                            | true, [false]                                          | Enable Content Delivery Server                                                                |
|               | CDS_PUBLIC_URL                                         | 	http://YOUR-APP-NAME-cds.YOUR-HOST-NAME/YOUR-TENANT-ID	 |                                                                                               | 
|               | 	CDS_PRIVATE_URL                                        | http://YOUR-TENANT-ID-cds-service:8080                 |                                                                                               |	
|               | 	CDS_PATH                                               | 	/api/v1	                                                |                                                                                               |	
| Keycloak/TLS  | KEYCLOAK_AUTH_URL                                      | https://YOUR-HOST-NAME/auth                            |                                                                                               |
|               | 	SPRING_SECURITY_OAUTH2_CLIENT_PROVIDER_OIDC_ISSUER_URI | https://YOUR-HOST-NAME/auth/realms/entando             |                                                                                               |
|               | 	ENTANDO_APP_USE_TLS	                                    |                                                        | protocol for the redirect to keycloak login                                                   |
|               | 	ENTANDO_APP_ENGINE_EXTERNAL_PORT                       |                                                        | to force the port to use                                                                      |			
| Redis server  | REDIS_ACTIVE                                           | true, [false]                                          | to activate Redis cache management                                                            |
|               | 	REDIS_ADDRESS                                          | URL [redis://localhost:6379]                           | 	Redis host address                                                                            ||
|               | REDIS_ADDRESSES                                        |                                                        | for HA, insert the comma separated list of nodes                                              |
|               | REDIS_MASTER_NAME                                      | [mymaster]                                             | To specify the name of the master node                                                        |
|               | REDIS_SESSION_ACTIVE	                                   | true, [false]                                          | 	enable storing of HTTP sessions in the Redis cluster, REDIS_ACTIVE has to be "true" too.      |
|               | REDIS_PASSWORD                                         |                                                        |                                                                                               | 	
|               | REDIS_USE_SENTINEL_EVENTS                              | [true], false                                          | when Redis is active and Redis addresses is set, use Sentinel Monitoring                      |
|               | REDIS_IO_THREAD_POOL_SIZE                              | Integer, [8]	                                           | to mitigate errors caused by missing front-end cache refresh		                                  |
| Solr          | SOLR_ACTIVE	                                            | true, false                                            | to activate Solr search                                                                       |
|               | SOLR_ADDRESS	                                           | [http://localhost:8983/solr]                           | Solr host address                                                                             |
|               | SOLR_CORE                                              | string, [entando]                                      | name of collection                                                                            |
|               | advancedSearch                                         | true, false                                            | To add the Solr config page to the CMS menu                                                   |
| Tomcat server | AGENT_ENABLED                                          | true, [false]                                          | if true, adds the agent options to tomcat                                                     |
|               | AGENT_OPTS                                             | javaagent:~/YOUR-JARFILE.jar, [empty]                  | the jar file with the agent options to use                                                    |
|               | TOMCAT_MAX_POST_SIZE                                   | Enter a value in bytes, [209,715,200 bytes]            | to configure connector maxPostSize                                                            | 
|               | FILE_UPLOAD_MAX_SIZE                                   | Enter a value in bytes, [52,428,800 bytes]             | to configure the application upload limit		                                                     |
| MISC          |                                                        |                                                        |                                                                                               |			
|               | ENTANDO_BUNDLE_CLI_ETC                                 | ${ENTANDO_BUNDLE_CLI_ETC}/hub/credentials              | Credentials/parameters saved within JSON files under this path for ent bundle add hub command			 |
|               | ENTANDO_APP_ENGINE_HEALTH_CHECK_TYPE                   | db.migration.strategy                                  | [auto], skip, disabled, generate_sql                                                          | Liquibase strategy 			
|               | LOG_CONFIG_FILE_PATH                                   |                                                        | to use the logback composable feature                                                         | 			
|               | ENTANDO_DOCKER_REGISTRY_OVERRIDE                       |                                                        | Deprecated-for v1 bundles, to propagate to CM for plugins                                     |
| Feature Flags | ENTANDO_FEATURE_FLAGS                                  | comma-separated list of tags                           | Enable experimental features. Example: `CACHE_PIPELINE,HEADLESS_WIDGET_CONFIG`                |
|               |                                                        |                                                        | Available flags: `CACHE_PIPELINE`, `HEADLESS_WIDGET_CONFIG`                                   |
|               | ENTANDO_FF_DEEP_DEBUG                                  | comma-separated list of tags                           | Enable deep debug logging for specific components. Example: `service-reload`                  |
