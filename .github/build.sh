#!/bin/bash
set -e

mvn -B clean

(mvn org.codehaus.mojo:license-maven-plugin:2.5.0:aggregate-download-licenses &> ./license-maven-plugin.log) &
BKMVNPID="$!"

# Check if parent has PR version and purge if needed
PARENT_VERSION=$(mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout)
if [[ "$PARENT_VERSION" == *"-PR"* ]]; then
  echo "~> Parent PR version detected ($PARENT_VERSION), purging parent dependency cache"
  mvn dependency:purge-local-repository \
    -DmanualInclude=org.entando:entando-maven-root \
    -DreResolve=false \
    -DactTransitively=false
fi

mvn versions:set -DnewVersion="$ARTIFACT_VERSION"

mvn -B package;

echo ""
echo "~> Waiting for license download completion"
wait "$BKMVNPID" || true

if grep -q "BUILD SUCCESS" ./license-maven-plugin.log; then
  echo "~> License download completed with success"
else
  echo "::error::License download terminated with error"
  exit 99
fi
