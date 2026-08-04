#!/bin/bash
#
# Step B of the CI gate — submit the SonarCloud analysis and BLOCK on the
# quality gate of the Compute Engine task THIS run creates.
#
# Wired in the workflow with `if: always()` so a failed test/build step can
# never prevent the scan (requirement: always update Sonar regardless of test
# results). `sonar.qualitygate.wait=true` ties the pass/fail decision to this
# run's ceTaskId (recorded in target/sonar/report-task.txt), so:
#   - a red quality gate exits non-zero -> the job fails;
#   - the decision can never be satisfied by a stale server-side analysis.
#
set -uo pipefail

SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"
SCAN_LOG="sonar-scan.log"

if $SKIP_SCANS; then
  echo "~> SKIP_SCANS=true — skipping Sonar scan."
  echo "### [SKIP] Sonar quality gate — skipped (SKIP_SCANS=true)" >> "$SUMMARY"
  exit 0
fi

# Make this step self-sufficient when Step A was skipped (SKIP_TESTS) and the
# poms were not version-set yet. Harmless (and quiet) when already set.
mvn versions:set -DnewVersion="$ARTIFACT_VERSION" -q || true

mvn -B org.sonarsource.scanner.maven:sonar-maven-plugin:5.0.0.4389:sonar \
  -Dsonar.verbose=true \
  -Dsonar.qualitygate.wait=true \
  -Dsonar.qualitygate.timeout=600 \
  ${SONAR_PROJECT_KEY:+-Dsonar.projectKey="$SONAR_PROJECT_KEY"} \
  ${SONAR_ORG:+-Dsonar.organization="$SONAR_ORG"} \
  2>&1 | tee "$SCAN_LOG"
SONAR_RC="${PIPESTATUS[0]}"

# Dashboard URL of THIS run's analysis (if the scan got far enough to write it).
RT=$(find . -path '*/target/sonar/report-task.txt' 2>/dev/null | head -n1)
DASH=""
[ -n "$RT" ] && DASH=$(grep -E '^dashboardUrl=' "$RT" | head -1 | cut -d= -f2-)

# --- case 1: the scan did not run / produced no analysis -> stale-analysis guard
if [ -z "$RT" ]; then
  echo "::error title=Sonar scan did not run::No report-task.txt was produced — refusing to treat the quality gate as passed (stale-analysis guard)."
  {
    echo "### [FAIL] Sonar quality gate — SCAN DID NOT RUN"
    echo ""
    echo "No \`report-task.txt\` was produced, so there is **no fresh analysis** to gate on. Failing rather than passing on a possibly stale server-side result."
  } >> "$SUMMARY"
  exit "$([ "$SONAR_RC" -ne 0 ] && echo "$SONAR_RC" || echo 1)"
fi

# --- case 2: the analysis ran but the quality gate is RED
if grep -q "QUALITY GATE STATUS: FAILED" "$SCAN_LOG" || [ "$SONAR_RC" -ne 0 ]; then
  echo "::error title=Sonar quality gate FAILED::The SonarCloud quality gate did not pass for this run's analysis. Details: ${DASH:-see the scan log}"
  {
    echo "### [FAIL] Sonar quality gate — FAILED"
    echo ""
    [ -n "$DASH" ] && echo "[View the failing quality gate on SonarCloud]($DASH)"
    echo ""
    echo "Failing conditions (from the scan log):"
    echo '```'
    grep -iE 'QUALITY GATE STATUS|condition|new coverage|duplicated|reliability|security|maintainability' "$SCAN_LOG" | tail -n 30 || true
    echo '```'
  } >> "$SUMMARY"
  exit "$([ "$SONAR_RC" -ne 0 ] && echo "$SONAR_RC" || echo 1)"
fi

# --- case 3: analysis ran and the quality gate passed
{
  echo "### [OK] Sonar quality gate — PASSED"
  echo ""
  [ -n "$DASH" ] && echo "[View the analysis on SonarCloud]($DASH)"
} >> "$SUMMARY"
echo "~> Quality gate PASSED for this run's analysis. ${DASH}"
exit 0
