#!/bin/bash

# Spring Security 6.x OAuth2 Migration Script for Entando Engine
# This script migrates OAuth2 classes from Spring Security 5.x to 6.x structure

echo "Starting Spring Security 6.x OAuth2 migration..."

# Navigate to engine directory
cd "$(dirname "$0")/engine/src" || exit 1

# 1. Migrate OAuth2AccessToken imports and references
echo "Migrating OAuth2AccessToken..."
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.common\.OAuth2AccessToken;/import org.springframework.security.oauth2.core.OAuth2AccessToken;/g' {} \;
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.common\.DefaultOAuth2AccessToken;/import org.springframework.security.oauth2.core.OAuth2AccessToken;/g' {} \;

# 2. Migrate OAuth2RefreshToken imports
echo "Migrating OAuth2RefreshToken..."
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.common\.OAuth2RefreshToken;/import org.springframework.security.oauth2.core.OAuth2RefreshToken;/g' {} \;
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.common\.DefaultOAuth2RefreshToken;/import org.springframework.security.oauth2.core.OAuth2RefreshToken;/g' {} \;

# 3. Migrate OAuth2Authentication imports
echo "Migrating OAuth2Authentication..."
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.provider\.OAuth2Authentication;/import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;/g' {} \;

# 4. Migrate ClientDetailsService imports
echo "Migrating ClientDetailsService..."
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.provider\.ClientDetailsService;/import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;/g' {} \;
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.provider\.ClientDetails;/import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;/g' {} \;

# 5. Migrate TokenStore imports
echo "Migrating TokenStore..."
find . -name "*.java" -exec sed -i 's/import org\.springframework\.security\.oauth2\.provider\.token\.TokenStore;/import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;/g' {} \;

# 6. Update class references in code
echo "Updating class references..."
find . -name "*.java" -exec sed -i 's/DefaultOAuth2AccessToken/OAuth2AccessToken/g' {} \;
find . -name "*.java" -exec sed -i 's/DefaultOAuth2RefreshToken/OAuth2RefreshToken/g' {} \;
find . -name "*.java" -exec sed -i 's/OAuth2Authentication/OAuth2Authorization/g' {} \;
find . -name "*.java" -exec sed -i 's/ClientDetailsService/RegisteredClientRepository/g' {} \;
find . -name "*.java" -exec sed -i 's/ClientDetails/RegisteredClient/g' {} \;
find . -name "*.java" -exec sed -i 's/TokenStore/OAuth2AuthorizationService/g' {} \;

echo "Spring Security 6.x OAuth2 migration completed!"
echo "Note: This script handles the basic import and class name migrations."
echo "Manual code review and adjustments may be needed for method calls and constructors."