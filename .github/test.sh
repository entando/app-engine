#!/bin/bash
#
# Step A of the CI gate — build the reactor and run the tests (with coverage).
#
# This step NEVER fails on a test failure (-fae + -Dmaven.test.failure.ignore=true):
# the whole reactor is exercised so (a) the scan step always has something to
# analyse and (b) the gate step can evaluate every module's reports. A non-zero
# exit here therefore means a BUILD error (compile / plugin / dependency) — NOT a
# test failure. Test failures are surfaced later by the "Unit test gate" step.
#
set -uo pipefail

SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"

# --- keep the reactor version consistent with the build job ---
PARENT_VERSION=$(mvn help:evaluate -Dexpression=project.parent.version -q -DforceStdout)
if [[ "$PARENT_VERSION" == *"-PR"* ]]; then
  echo "~> Parent PR version detected ($PARENT_VERSION), purging parent dependency cache"
  mvn dependency:purge-local-repository \
    -DmanualInclude=org.entando:entando-maven-root \
    -DreResolve=false \
    -DactTransitively=false
fi

mvn versions:set -DnewVersion="$ARTIFACT_VERSION"

if $SKIP_TESTS; then
  echo "~> SKIP_TESTS=true — skipping test execution."
  echo "### [SKIP] Build & tests — skipped (SKIP_TESTS=true)" >> "$SUMMARY"
  exit 0
fi

# -fae: fail at end (keep exercising the reactor after a failing module).
# -Dmaven.test.failure.ignore=true: a failing/flaky test must not stop the build,
# so the scan step (run with if: always()) always executes and the test gate is
# re-enforced by .github/gate.sh.
mvn -B -fae -Dmaven.test.failure.ignore=true \
  -Ppre-deployment-verification \
  org.jacoco:jacoco-maven-plugin:prepare-agent \
  verify \
  org.jacoco:jacoco-maven-plugin:report
RC=$?

if [ "$RC" -ne 0 ]; then
  echo "::error title=Build failed::Compilation/plugin/dependency error in the reactor (exit $RC). NOTE: test failures alone do NOT fail this step — check the 'Unit test gate' step for those."
  {
    echo "### [FAIL] Build & tests — BUILD ERROR"
    echo ""
    echo "Maven exited with code \`$RC\` **before** tests could complete — this is a compilation, plugin or dependency error, **not** a test failure."
    echo "See the **Build & run tests** step log for the failing module."
  } >> "$SUMMARY"
  exit "$RC"
fi

echo "### [OK] Build & tests — completed (results evaluated by the Unit test gate)" >> "$SUMMARY"
exit 0
