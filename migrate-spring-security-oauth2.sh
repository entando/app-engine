#!/bin/bash

echo "Migrating Java source imports from javax to jakarta..."

# Update servlet imports
find . -name "*.java" -type f -exec sed -i 's/import javax\.servlet\./import jakarta.servlet./g' {} +
find . -name "*.java" -type f -exec sed -i 's/import javax\.servlet\.http\./import jakarta.servlet.http./g' {} +
find . -name "*.java" -type f -exec sed -i 's/import javax\.servlet\.jsp\./import jakarta.servlet.jsp./g' {} +

# Update validation imports
find . -name "*.java" -type f -exec sed -i 's/import javax\.validation\./import jakarta.validation./g' {} +

# Update xml.bind imports  
find . -name "*.java" -type f -exec sed -i 's/import javax\.xml\.bind\./import jakarta.xml.bind./g' {} +

# Update ws.rs imports
find . -name "*.java" -type f -exec sed -i 's/import javax\.ws\.rs\./import jakarta.ws.rs./g' {} +

# Update annotation imports
find . -name "*.java" -type f -exec sed -i 's/import javax\.annotation\./import jakarta.annotation./g' {} +

echo "Java source import migration completed."
