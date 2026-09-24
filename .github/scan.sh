#!/bin/bash
#
# Step B of the CI gate — submit the SonarCloud analysis and BLOCK on the
# quality gate of the Compute Engine task THIS run creates.
#
# The analysis always runs and always waits for the gate result. The gate is
# enforced on pull requests, tags and direct pushes. On a push that is the merge
# commit of a PR into the pushed branch the PR was already gated, so the result
# is reported as a warning and never fails the step.
#
# Wired in the workflow with `if: always()` so a failed test/build step can
# never prevent the scan (requirement: always update Sonar regardless of test
# results). `sonar.qualitygate.wait=true` ties the pass/fail decision to this
# run's ceTaskId (recorded in target/sonar/report-task.txt), so:
#   - a red quality gate exits non-zero where the gate is enforced -> the job fails;
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

# --- gate enforcement --------------------------------------------------------
# Not enforced only when the pushed commit is the merge commit of a PR merged into
# the pushed branch. A failed lookup leaves the gate enforced.
GATE_ENFORCED=true
MERGED_PR=""
if [ "${GITHUB_EVENT_NAME:-}" = "push" ] && [[ "${GITHUB_REF:-}" == refs/heads/* ]]; then
  MERGED_PR=$(gh api "repos/${GITHUB_REPOSITORY:-}/commits/${GITHUB_SHA:-}/pulls" \
    --jq "[.[] | select(.merged_at != null
                        and .merge_commit_sha == \"${GITHUB_SHA:-}\"
                        and .base.ref == \"${GITHUB_REF_NAME:-}\")][0].number // empty" \
    2>/dev/null)
  LOOKUP_RC=$?
  if [ "$LOOKUP_RC" -ne 0 ]; then
    MERGED_PR=""
    echo "::warning title=PR lookup failed::Could not determine whether ${GITHUB_SHA:-HEAD} is a PR merge commit (gh exit $LOOKUP_RC); the quality gate is enforced."
  elif [ -n "$MERGED_PR" ]; then
    GATE_ENFORCED=false
    echo "~> Merge commit of PR #$MERGED_PR (already gated on the PR): the quality gate result is reported, not enforced."
  else
    echo "~> Direct push to ${GITHUB_REF_NAME:-the branch} (no merged PR for ${GITHUB_SHA:-HEAD}): the quality gate is enforced."
  fi
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

# --- no analysis submitted -> stale-analysis guard ----------------------------
# Fails whether or not the gate is enforced: without a fresh analysis the branch
# baseline that PR analyses compare against goes stale.
if [ -z "$RT" ]; then
  echo "::error title=Sonar scan did not run::No report-task.txt was produced — refusing to treat the quality gate as passed (stale-analysis guard)."
  {
    echo "### [FAIL] Sonar quality gate — SCAN DID NOT RUN"
    echo ""
    echo "No \`report-task.txt\` was produced, so there is **no fresh analysis** to gate on. Failing rather than passing on a possibly stale server-side result."
  } >> "$SUMMARY"
  exit "$([ "$SONAR_RC" -ne 0 ] && echo "$SONAR_RC" || echo 1)"
fi

# --- classify the gate result -------------------------------------------------
#   PASSED       the scanner reported the gate as passed
#   NONE         the scanner reported FAILED and SonarCloud says NONE: no New Code
#                period, no condition evaluated (a SonarCloud setting, not code)
#   FAILED       the scanner reported FAILED (a red gate)
#   UNAVAILABLE  the analysis was submitted but no gate result came back
#                (timeout or Compute Engine error)
if grep -q "QUALITY GATE STATUS: FAILED" "$SCAN_LOG"; then
  RESULT=FAILED
elif [ "$SONAR_RC" -ne 0 ]; then
  RESULT=UNAVAILABLE
else
  RESULT=PASSED
fi

if [ "$RESULT" = "FAILED" ]; then
  CE_URL=$(grep -E '^ceTaskUrl=' "$RT" | head -1 | cut -d= -f2-)
  ANALYSIS_ID=""
  QG_STATUS=""
  if [ -n "$CE_URL" ]; then
    ANALYSIS_ID=$(curl -fsS -u "${SONAR_TOKEN:-}:" "$CE_URL" 2>/dev/null | jq -r '.task.analysisId // empty' 2>/dev/null || true)
  fi
  if [ -n "$ANALYSIS_ID" ]; then
    QG_STATUS=$(curl -fsS -u "${SONAR_TOKEN:-}:" \
      "${SONAR_URL:-https://sonarcloud.io}/api/qualitygates/project_status?analysisId=$ANALYSIS_ID" 2>/dev/null \
      | jq -r '.projectStatus.status // empty' 2>/dev/null || true)
  fi
  [ "$QG_STATUS" = "NONE" ] && RESULT=NONE
fi

# --- report -------------------------------------------------------------------
if $GATE_ENFORCED; then
  LEVEL=error; TAG=FAIL; SUFFIX=""
else
  LEVEL=warning; TAG=WARN; SUFFIX=" (not enforced — merge of PR #$MERGED_PR, already gated on the PR)"
fi

case "$RESULT" in
  PASSED)
    {
      echo "### [OK] Sonar quality gate — PASSED${SUFFIX}"
      echo ""
      [ -n "$DASH" ] && echo "[View the analysis on SonarCloud]($DASH)"
    } >> "$SUMMARY"
    echo "~> Quality gate PASSED for this run's analysis. ${DASH}"
    exit 0
    ;;
  NONE)
    echo "::${LEVEL} title=Sonar quality gate NOT COMPUTED::No New Code period is defined for '${GITHUB_REF_NAME:-this branch}' on SonarCloud, so no quality gate condition could be evaluated. This is a SonarCloud project setting (Administration > New Code), not a code problem. Details: ${DASH:-see the scan log}"
    {
      echo "### [${TAG}] Sonar quality gate — NOT COMPUTED (no New Code period)${SUFFIX}"
      echo ""
      [ -n "$DASH" ] && echo "[View the analysis on SonarCloud]($DASH)"
      echo ""
      echo "The quality gate status is \`NONE\`: every gate condition is on new code, and SonarCloud"
      echo "has no New Code period for this branch, so nothing was evaluated. The scanner reports this"
      echo "as FAILED. Fix it in SonarCloud under **Administration > New Code**; the code is not at fault."
    } >> "$SUMMARY"
    $GATE_ENFORCED && exit 1
    exit 0
    ;;
  FAILED)
    echo "::${LEVEL} title=Sonar quality gate FAILED::The SonarCloud quality gate did not pass for this run's analysis${SUFFIX}. Details: ${DASH:-see the scan log}"
    {
      echo "### [${TAG}] Sonar quality gate — FAILED${SUFFIX}"
      echo ""
      [ -n "$DASH" ] && echo "[View the failing quality gate on SonarCloud]($DASH)"
      echo ""
      echo "Failing conditions (from the scan log):"
      echo '```'
      grep -iE 'QUALITY GATE STATUS|condition|new coverage|duplicated|reliability|security|maintainability' "$SCAN_LOG" | tail -n 30 || true
      echo '```'
    } >> "$SUMMARY"
    $GATE_ENFORCED && exit "$([ "$SONAR_RC" -ne 0 ] && echo "$SONAR_RC" || echo 1)"
    exit 0
    ;;
  UNAVAILABLE)
    echo "::${LEVEL} title=Sonar quality gate result UNAVAILABLE::The analysis was submitted but the scanner returned no gate result (exit $SONAR_RC: timeout or Compute Engine error)${SUFFIX}. Details: ${DASH:-see the scan log}"
    {
      echo "### [${TAG}] Sonar quality gate — RESULT UNAVAILABLE (scanner exit $SONAR_RC)${SUFFIX}"
      echo ""
      echo "The analysis was submitted, but no gate result came back (wait timeout or Compute Engine error). See the scan log."
      [ -n "$DASH" ] && echo "[View the analysis on SonarCloud]($DASH)"
    } >> "$SUMMARY"
    $GATE_ENFORCED && exit "$SONAR_RC"
    exit 0
    ;;
esac
