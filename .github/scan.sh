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

# --- coverage-report guard ---------------------------------------------------
# The scanner treats a missing coverage report as "0% covered" rather than as an
# error, so a pipeline that fails to produce one fails the quality gate on
# "Coverage on New Code" and blames the code. That reads as a code problem and
# sends the author looking in the wrong place. gate.sh already refuses to pass
# when no surefire report exists; this is the same check for coverage. It only
# warns - the scan must still run so Sonar is always updated - but it names the
# real cause up front.
COVERAGE_REPORTS=$(find . -type f -path '*/target/site/jacoco*/jacoco.xml' 2>/dev/null | wc -l)
if [ "$COVERAGE_REPORTS" -eq 0 ]; then
  echo "::warning title=No coverage reports::No jacoco.xml was found, so Sonar will record 0% coverage on new code regardless of how well tested it is. Check that the 'Build & run tests' step ran the tests and reached the verify phase."
  {
    echo "### [WARN] Sonar quality gate — no coverage reports found"
    echo ""
    echo "No \`jacoco.xml\` exists under any module's \`target/site/jacoco*/\`, so this analysis"
    echo "carries **no coverage data** and any \"Coverage on New Code\" failure below reflects the"
    echo "missing report, **not** untested code. Verify that the tests ran and reached \`verify\`."
  } >> "$SUMMARY"
else
  echo "~> $COVERAGE_REPORTS JaCoCo XML report(s) found:"
  find . -type f -path '*/target/site/jacoco*/jacoco.xml' 2>/dev/null | sed 's/^/     /'
fi

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
