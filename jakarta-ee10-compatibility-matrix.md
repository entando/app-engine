# Jakarta EE 10 Compatibility Matrix for Entando App Engine

**Generated**: September 22, 2025
**Project**: Entando App Engine v7.5.0
**Current State**: Java EE 8 (javax.* namespace)
**Target**: Jakarta EE 10 (jakarta.* namespace)

## Executive Summary

- **Total Modules**: 14 (1 parent + 13 sub-modules)
- **Test Files**: 578 test files with 3,346 test annotations
- **Critical Dependencies**: 23 major libraries requiring updates
- **Risk Level**: HIGH (Major framework upgrade required)

## Core Framework Dependencies

### Spring Ecosystem (CRITICAL - Major Version Jump Required)

| Component | Current Version | Target Version | Compatibility | Risk Level | Notes |
|-----------|----------------|----------------|---------------|------------|-------|
| Spring Framework | 5.3.39 | 6.2.1 | ❌ Breaking | CRITICAL | Major version jump, requires Jakarta EE namespace |
| Spring Security | 5.8.16 | 6.3.1 | ❌ Breaking | CRITICAL | Depends on Spring 6.x |
| Spring Security OAuth2 | 2.5.2.RELEASE | 6.3.1 | ❌ Breaking | CRITICAL | Major architectural changes |
| Spring Data Redis | 2.5.12 | 3.2.1 | ❌ Breaking | HIGH | Requires Spring 6.x |
| Spring Data Commons | 2.5.3 | 3.2.1 | ❌ Breaking | HIGH | Requires Spring 6.x |
| Spring Data REST | 3.5.6 | 4.2.1 | ❌ Breaking | HIGH | Requires Spring 6.x |
| Spring Session Data Redis | 2.7.4 | 3.2.1 | ❌ Breaking | HIGH | Requires Spring 6.x |
| Spring HATEOAS | 1.5.6 | 2.2.1 | ❌ Breaking | MEDIUM | API changes expected |

### Web & Servlet Stack

| Component | Current Version | Target Version | Compatibility | Risk Level | Notes |
|-----------|----------------|----------------|---------------|------------|-------|
| Servlet API | javax.servlet-api 4.0.1 | jakarta.servlet-api 6.0.0 | ❌ Breaking | CRITICAL | Namespace change required |
| JSP API | jsp-api 2.2 | jakarta.servlet.jsp-api 3.1.1 | ❌ Breaking | HIGH | Namespace + version jump |
| EL API | javax.el-api 3.0.0 | jakarta.el-api 5.0.1 | ❌ Breaking | HIGH | Namespace + version jump |
| Annotation API | javax.annotation-api 1.3.2 | jakarta.annotation-api 2.1.1 | ❌ Breaking | HIGH | Namespace change |
| Tomcat Servlet API | 10.0.8 | 10.1.33 | ⚠️ Partial | MEDIUM | Already Jakarta EE 9, needs 10 |

### Struts2 & MVC

| Component | Current Version | Target Version | Compatibility | Risk Level | Notes |
|-----------|----------------|----------------|---------------|------------|-------|
| Struts2 Core | 6.7.4 | 6.7.4+ | ✅ Compatible | LOW | Should work with Jakarta EE 10 |
| Struts2 Spring Plugin | 6.7.4 | 6.7.4+ | ⚠️ Depends | MEDIUM | Requires Spring 6 compatibility testing |
| Struts2 Tiles Plugin | 6.7.4 | 6.7.4+ | ✅ Compatible | LOW | No direct Jakarta EE dependency |
| Struts2 JSON Plugin | 6.7.4 | 6.7.4+ | ✅ Compatible | LOW | No direct Jakarta EE dependency |

### Validation & Data Binding

| Component | Current Version | Target Version | Compatibility | Risk Level | Notes |
|-----------|----------------|----------------|---------------|------------|-------|
| Hibernate Validator | 6.0.20.Final | 8.0.1.Final | ❌ Breaking | HIGH | Major version jump for Jakarta EE 10 |
| Validation API | javax.validation 2.0.1.Final | jakarta.validation-api 3.0.2 | ❌ Breaking | HIGH | Namespace change |
| Jackson Core | 2.16.2 | 2.18.1 | ✅ Compatible | LOW | Should work with Jakarta EE 10 |
| Jackson Databind | 2.16.2 | 2.18.1 | ✅ Compatible | LOW | Should work with Jakarta EE 10 |

### API & Documentation

| Component | Current Version | Target Version | Compatibility | Risk Level | Notes |
|-----------|----------------|----------------|---------------|------------|-------|
| JAX-RS API | jakarta.ws.rs-api 2.1.6 | jakarta.ws.rs-api 3.1.0 | ⚠️ Minor | MEDIUM | Already Jakarta, needs version update |
| JAXB API | javax.xml.bind 2.3.1 | jakarta.xml.bind-api 4.0.0 | ❌ Breaking | HIGH | Namespace + major version |
| SpringFox Swagger2 | 2.10.5 | SpringDoc 2.x | ❌ Breaking | HIGH | SpringFox deprecated, migrate to SpringDoc |

## Module-Specific Analysis

### Core Modules

| Module | Java EE Dependencies | Jakarta Ready | Risk Level | Migration Effort |
|--------|---------------------|---------------|------------|------------------|
| **engine** | Heavy (servlet, annotation, validation) | ❌ No | CRITICAL | HIGH - Core module with extensive Spring usage |
| **webapp** | Heavy (servlet, JSP, EL) | ❌ No | HIGH | HIGH - Main assembly with all integrations |
| **admin-console** | Medium (servlet, JSP) | ❌ No | HIGH | MEDIUM - Admin interface dependencies |
| **portal-ui** | Medium (servlet, filters) | ❌ No | HIGH | MEDIUM - UI components and filters |

### Plugin Modules

| Plugin | Primary Dependencies | Jakarta Ready | Risk Level | Migration Effort |
|--------|---------------------|---------------|------------|------------------|
| **keycloak-plugin** | Spring Security, Servlet | ❌ No | CRITICAL | HIGH - OAuth/Security integration |
| **cms-plugin** | JAX-RS, Validation, Servlet | ❌ No | HIGH | HIGH - Content management APIs |
| **mail-plugin** | Spring, Validation | ❌ No | MEDIUM | MEDIUM - Email functionality |
| **redis-plugin** | Spring Data Redis, Session | ❌ No | HIGH | MEDIUM - Session management |
| **seo-plugin** | Servlet, Validation | ❌ No | MEDIUM | MEDIUM - SEO features |
| **versioning-plugin** | Spring, Servlet | ❌ No | MEDIUM | MEDIUM - Content versioning |
| **cds-plugin** | Servlet, JAX-RS | ❌ No | MEDIUM | MEDIUM - Content delivery |
| **solr-plugin** | Spring, Servlet | ❌ No | MEDIUM | MEDIUM - Search functionality |
| **contentscheduler-plugin** | Spring, Quartz | ❌ No | MEDIUM | MEDIUM - Job scheduling |

## Third-Party Library Impact

### High Risk Libraries (Require Major Updates)
- **Spring Framework**: Complete ecosystem migration needed
- **Hibernate Validator**: Major version jump (6.x → 8.x)
- **SpringFox**: Migration to SpringDoc required
- **Spring Security OAuth2**: Architecture changes

### Medium Risk Libraries (Version Updates)
- **JAX-RS**: Already Jakarta, needs version update
- **JAXB**: Namespace change required
- **Tomcat**: Minor version update for Jakarta EE 10

### Low Risk Libraries (Should be Compatible)
- **Jackson**: Modern versions support Jakarta EE
- **Struts2**: Framework is Jakarta EE 10 compatible
- **Apache Commons**: No direct Jakarta EE dependencies
- **Logging (Logback/SLF4J)**: No direct Jakarta EE dependencies

## Testing Impact Analysis

### Current Test Coverage
- **Total Test Files**: 578
- **Test Annotations**: 3,346 (@Test, @IntegrationTest, @SpringBootTest)
- **Test Structure**: Standard Maven test structure (src/test/java)

### Testing Migration Requirements

| Test Category | Count (Estimated) | Migration Effort | Risk Level |
|---------------|------------------|------------------|------------|
| Unit Tests | ~2,500 | LOW | Tests should mostly work after namespace migration |
| Integration Tests | ~600 | HIGH | Spring context configuration changes required |
| Web Tests | ~200 | HIGH | Servlet/MockMvc configuration updates needed |
| Security Tests | ~46 | CRITICAL | Spring Security 6 has major changes |

### Test Dependencies Requiring Updates
- **Spring Test**: 5.3.39 → 6.2.1
- **Spring Boot Test**: If used, requires 3.x
- **MockMvc**: Spring 6 changes
- **Security Test**: Spring Security 6 changes

## Migration Timeline Estimates

### Phase 1: Core Framework (4 weeks)
- Spring Framework 6.2 migration
- Basic compilation fixes
- Core module updates

### Phase 2: Namespace Migration (4 weeks)
- Automated javax → jakarta conversion
- Manual verification and fixes
- Compilation error resolution

### Phase 3: Plugin Updates (4 weeks)
- Individual plugin migrations
- Plugin integration testing
- Cross-plugin compatibility verification

### Phase 4: Testing & Validation (4 weeks)
- Test framework updates
- Integration test fixes
- End-to-end validation

## Risk Mitigation Strategies

### Critical Risks
1. **Spring Framework Breaking Changes**
   - Create compatibility layer where possible
   - Extensive integration testing
   - Phased rollout approach

2. **Test Suite Failures**
   - Update test frameworks in parallel
   - Maintain test coverage metrics
   - Create new tests for Jakarta EE 10 features

3. **Plugin Incompatibilities**
   - Test plugins independently
   - Maintain plugin isolation
   - Create fallback mechanisms

### Recommended Approach
1. **Parallel Development**: Maintain current branch while developing migration
2. **Incremental Testing**: Test each module independently
3. **Rollback Plan**: Maintain ability to revert at any phase
4. **Documentation**: Document all breaking changes and workarounds

## Success Metrics

### Technical Metrics
- ✅ All 578 test files pass
- ✅ All 14 modules compile successfully
- ✅ All 9 plugins function correctly
- ✅ Performance regression < 5%
- ✅ Memory usage regression < 10%

### Functional Metrics
- ✅ All REST APIs maintain compatibility
- ✅ Web interface functions correctly
- ✅ Authentication/authorization works
- ✅ Plugin activation/deactivation works
- ✅ Database operations function correctly

---

**Legend**:
- ✅ Compatible: No migration required
- ⚠️ Partial: Minor updates required
- ❌ Breaking: Major migration required

**Risk Levels**:
- **CRITICAL**: Project-blocking issues
- **HIGH**: Significant development effort required
- **MEDIUM**: Moderate effort with some risk
- **LOW**: Minimal effort, low risk

**Next Steps**: Begin with Phase 1 (Core Framework Migration) after stakeholder approval.