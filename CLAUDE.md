# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is the Entando App Engine - a multi-module Maven project that builds the core CMS and application platform. The project uses Java 11 with Spring Framework, Struts2, and various plugins for extended functionality.

## Build and Development Commands

### Core Build Commands
- `mvn clean install -DskipLicenseDownload` - Clean build all modules, skipping license downloads for speed
- `mvn clean test -Ppre-deployment-verification` - Run all tests with pre-deployment verification profile
- `mvn clean test -Ppre-deployment-verification -pl <module-name> -Dtest=<test-class-name>` - Run specific test in specific module

### Local Development Server
```bash
# Basic local development with Derby database
mvn clean package jetty:run-war -Pjetty-local -Pderby -DskipLicenseDownload

# With Keycloak authentication
mvn clean package jetty:run-war -Pjetty-local -Pderby -Pkeycloak -DskipLicenseDownload

# With external PostgreSQL
mvn clean package jetty:run-war -Pjetty-local -Ppostgresql -DskipDatabaseImage=true \
  -Dportdb.url=jdbc:postgresql://localhost:5432/portdb -Dportdb.username=dbuser -Dportdb.password=password \
  -Dservdb.url=jdbc:postgresql://localhost:5432/servdb -Dservdb.username=dbuser -Dservdb.password=password
```

The application runs at: http://localhost:8080/entando-de-app/

### Testing and Quality
- `mvn verify -Ppre-deployment-verification` - Full verification including tests and quality checks
- Add `-Dspring.profiles.active=swagger` to enable Swagger UI at `/api/swagger-ui.html`
- Checkstyle is configured but currently skipped (`checkstyle.skip=true` in root pom)

## Project Structure

### Module Architecture
- `engine/` - Core engine with business logic and APIs
- `webapp/` - Main web application module that assembles everything
- `admin-console/` - Administrative interface
- `portal-ui/` - Frontend portal components
- Plugin modules:
  - `keycloak-plugin/` - OAuth2/OIDC authentication
  - `cms-plugin/` - Content Management System
  - `mail-plugin/` - Email functionality
  - `redis-plugin/` - Redis cache integration
  - `cds-plugin/` - Content Delivery Server
  - `seo-plugin/` - SEO optimization
  - `versioning-plugin/` - Content versioning
  - `contentscheduler-plugin/` - Content scheduling (disabled by default, enable with `-Pcontentscheduler`)
  - `solr-plugin/` - Search functionality

### Key Source Packages
- `org.entando.entando.web.*` - REST API controllers
- `org.entando.entando.aps.*` - Core business services  
- `org.entando.entando.ent.*` - Enterprise features

### Configuration
- Spring XML configurations in `src/main/resources/spring/`
- Database configurations use profiles: derby, postgresql, mysql
- Test database: Derby in-memory by default
- Environment variables for runtime configuration (see README.md for full list)

## Development Guidelines

### Technology Stack
- Java 11 (source and target)
- Spring Framework 5.3.x
- Spring Security 5.8.x  
- Struts2 6.7.x
- Jackson 2.12.x for JSON
- Maven for build management
- Derby/PostgreSQL/MySQL for databases
- Liquibase for database migrations (v7.0.0+)

### Database Migrations
- Starting from v7.0.0, uses Liquibase for schema changes
- For upgrades from 6.3.2 to 7.0.0, manual upgrade scripts are in `upgrade/` folder
- Health check strategy controlled by `ENTANDO_APP_ENGINE_HEALTH_CHECK_TYPE`

### Docker Support
- Dockerfiles available for different containers: `Dockerfile.wildfly`, `Dockerfile.tomcat`, `Dockerfile.eap`
- fabric8 plugin for Docker operations: `mvn docker:run -Pwildfly,postgresql`

### Profiles and Features
- Profile activation controls which features are included
- Content Scheduler Plugin requires explicit activation via Maven profile
- Various database profiles for different RDBMS support
- `build-ootb-widgets` profile for building JavaScript widgets with Node.js

### Extension Points
- Plugin architecture allows extending functionality through separate modules
- Each plugin has its own Spring configuration and can be independently activated
- REST APIs follow `/api/` path convention with Swagger documentation support