#!/bin/bash

echo "=== Jakarta EE 10 Namespace Migration for Engine Module ==="
echo "Starting migration of javax.* imports to jakarta.* in engine module..."

cd engine/

# Backup original files
echo "Creating backup..."
find . -name "*.java" -exec cp {} {}.pre-jakarta \; 2>/dev/null

# Count files before migration
echo "Files to migrate:"
echo "  - Servlet API: $(find . -name "*.java" -exec grep -l "import javax\.servlet\." {} \; | wc -l) files"
echo "  - Annotation API: $(find . -name "*.java" -exec grep -l "import javax\.annotation\." {} \; | wc -l) files"
echo "  - Validation API: $(find . -name "*.java" -exec grep -l "import javax\.validation\." {} \; | wc -l) files"
echo "  - EL API: $(find . -name "*.java" -exec grep -l "import javax\.el\." {} \; | wc -l) files"
echo "  - JAX-RS API: $(find . -name "*.java" -exec grep -l "import javax\.ws\.rs\." {} \; | wc -l) files"
echo "  - JAXB API: $(find . -name "*.java" -exec grep -l "import javax\.xml\.bind\." {} \; | wc -l) files"

echo ""
echo "Performing namespace migration..."

# Servlet API migration
echo "  → Migrating javax.servlet.* to jakarta.servlet.*"
find . -name "*.java" -exec sed -i 's/import javax\.servlet\./import jakarta.servlet./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.servlet\./jakarta.servlet./g' {} \;

# Annotation API migration
echo "  → Migrating javax.annotation.* to jakarta.annotation.*"
find . -name "*.java" -exec sed -i 's/import javax\.annotation\./import jakarta.annotation./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.annotation\./jakarta.annotation./g' {} \;

# Validation API migration
echo "  → Migrating javax.validation.* to jakarta.validation.*"
find . -name "*.java" -exec sed -i 's/import javax\.validation\./import jakarta.validation./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.validation\./jakarta.validation./g' {} \;

# EL API migration
echo "  → Migrating javax.el.* to jakarta.el.*"
find . -name "*.java" -exec sed -i 's/import javax\.el\./import jakarta.el./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.el\./jakarta.el./g' {} \;

# JAX-RS API migration
echo "  → Migrating javax.ws.rs.* to jakarta.ws.rs.*"
find . -name "*.java" -exec sed -i 's/import javax\.ws\.rs\./import jakarta.ws.rs./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.ws\.rs\./jakarta.ws.rs./g' {} \;

# JAXB API migration
echo "  → Migrating javax.xml.bind.* to jakarta.xml.bind.*"
find . -name "*.java" -exec sed -i 's/import javax\.xml\.bind\./import jakarta.xml.bind./g' {} \;
find . -name "*.java" -exec sed -i 's/javax\.xml\.bind\./jakarta.xml.bind./g' {} \;

# Count files after migration
echo ""
echo "Migration completed! Files affected:"
echo "  - Total Java files processed: $(find . -name "*.java" | wc -l)"
echo "  - Files with jakarta.servlet imports: $(find . -name "*.java" -exec grep -l "import jakarta\.servlet\." {} \; | wc -l)"
echo "  - Files with jakarta.annotation imports: $(find . -name "*.java" -exec grep -l "import jakarta\.annotation\." {} \; | wc -l)"
echo "  - Files with jakarta.validation imports: $(find . -name "*.java" -exec grep -l "import jakarta\.validation\." {} \; | wc -l)"

echo ""
echo "=== Migration Summary ==="
echo "Namespace migration completed for engine module"
echo "Backup files created with .pre-jakarta extension"
echo "Ready for compilation testing"