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

## Content Scheduler Plugin

The Content Scheduler Plugin is disabled by default. It can be included in the webapp activating the `contentscheduler` Maven profile during the build.

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
