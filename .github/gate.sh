#!/bin/bash
#
# Step C of the CI gate — the TEST gate.
#
# Wired in the workflow with `if: always()` so it evaluates the test results
# even when the scan step already failed the job on a red quality gate (and
# vice-versa). Fails on any real surefire/failsafe failure or error. Flaky tests
# that passed on retry are recorded as <flakyFailure> with failures="0"/
# errors="0", so they do NOT trip this gate.
#
set -uo pipefail

SUMMARY="${GITHUB_STEP_SUMMARY:-/dev/null}"

if $SKIP_TESTS; then
  echo "~> SKIP_TESTS=true — nothing to gate."
  echo "### [SKIP] Unit test gate — skipped (SKIP_TESTS=true)" >> "$SUMMARY"
  exit 0
fi

.github/github-tools/mvn.test.report.generate || true

# ---------------------------------------------------------------------------
# Surface FLAKY tests (informational — does NOT fail the gate).
#
# With rerunFailingTestsCount=1 (pom.xml), a test that fails then passes on
# retry is recorded by surefire as <flakyFailure>/<flakyError> with
# failures="0"/errors="0". Those pass the gate by design, but we report them
# here so intermittent tests don't stay invisible in a green build.
# ---------------------------------------------------------------------------
FLAKY_REPORTS=$(find . -type f \( -path '*/surefire-reports/*.xml' -o -path '*/failsafe-reports/*.xml' \) -print0 2>/dev/null \
  | xargs -0 -r grep -lE '<flakyFailure|<flakyError' 2>/dev/null)

if [ -n "$FLAKY_REPORTS" ]; then
  # one "class<TAB>method" line per flaky testcase, de-duplicated
  FLAKY_LIST=$(printf '%s\n' "$FLAKY_REPORTS" | while IFS= read -r f; do
    [ -z "$f" ] && continue
    awk '
      /<testcase / {
        name=""; cls=""; flagged=0
        if (match($0, /name="[^"]*"/))      name = substr($0, RSTART+6,  RLENGTH-7)
        if (match($0, /classname="[^"]*"/)) cls  = substr($0, RSTART+11, RLENGTH-12)
      }
      /<flakyFailure|<flakyError/ { if (!flagged) { print cls "\t" name; flagged=1 } }
    ' "$f"
  done | sort -u)

  FLAKY_COUNT=$(printf '%s\n' "$FLAKY_LIST" | grep -cve '^$')
  echo "::warning title=Flaky tests detected::${FLAKY_COUNT} test(s) failed then passed on retry — see the job summary (the gate is NOT failed)."
  {
    echo "### [WARN] Flaky tests — ${FLAKY_COUNT} (failed once, passed on retry — gate NOT failed)"
    echo ""
    echo "| Test class | Method |"
    echo "|---|---|"
    printf '%s\n' "$FLAKY_LIST" | while IFS=$'\t' read -r cls name; do
      [ -z "${cls}${name}" ] && continue
      echo "| \`${cls:-?}\` | \`${name:-?}\` |"
    done
    echo ""
    echo "> These failed on the first attempt and passed on a retry (\`rerunFailingTestsCount=1\`). They do **not** fail the pipeline, but flag intermittent tests worth investigating."
  } >> "$SUMMARY"
fi

# reports with at least one real failure or error (flaky retries carry failures="0")
FAILED_REPORTS=$(find . -type f \( -path '*/surefire-reports/*.xml' -o -path '*/failsafe-reports/*.xml' \) -print0 2>/dev/null \
  | xargs -0 -r grep -lE 'failures="[1-9][0-9]*"|errors="[1-9][0-9]*"' 2>/dev/null)

if [ -n "$FAILED_REPORTS" ]; then
  echo "::error title=Unit test gate FAILED::One or more tests failed. See the table in the job summary and the offending modules below."
  {
    echo "### [FAIL] Unit test gate — FAILED"
    echo ""
    echo "| Test suite | Module (report path) | Failures | Errors |"
    echo "|---|---|---:|---:|"
  } >> "$SUMMARY"

  # one row per failing report, with the suite name and counts pulled from the XML
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    line=$(grep -oE '<testsuite [^>]*>' "$f" | head -1)
    name=$(printf '%s' "$line" | sed -nE 's/.*[[:space:]]name="([^"]*)".*/\1/p')
    fails=$(printf '%s' "$line" | sed -nE 's/.*[[:space:]]failures="([0-9]+)".*/\1/p')
    errs=$(printf '%s' "$line" | sed -nE 's/.*[[:space:]]errors="([0-9]+)".*/\1/p')
    mod=$(printf '%s' "$f" | sed -E 's#/target/(surefire|failsafe)-reports/.*##')
    echo "| \`${name:-?}\` | \`${mod:-$f}\` | ${fails:-?} | ${errs:-?} |" >> "$SUMMARY"
    # per-run log annotations naming the failing test classes
    echo "::error file=$f::Failing suite ${name:-?} (failures=${fails:-?}, errors=${errs:-?})"
  done <<< "$FAILED_REPORTS"

  echo "" >> "$SUMMARY"
  echo "> [INFO] The Sonar analysis was still submitted — see the **Sonar quality gate** step." >> "$SUMMARY"
  exit 1
fi

# Presence guard: a -fae compile-skip produces NO xml for the skipped modules,
# which would otherwise look like "no failures". Require at least one report.
REPORT_COUNT=$(find . -type f \( -path '*/surefire-reports/*.xml' -o -path '*/failsafe-reports/*.xml' \) 2>/dev/null | wc -l)
if [ "$REPORT_COUNT" -eq 0 ]; then
  echo "::error title=Unit test gate FAILED::No surefire/failsafe reports were produced — tests did not run (likely a compile failure). See the 'Build & run tests' step."
  {
    echo "### [FAIL] Unit test gate — NO TESTS RAN"
    echo ""
    echo "No surefire/failsafe reports were found, so tests never executed (likely a compile failure). See the **Build & run tests** step."
  } >> "$SUMMARY"
  exit 1
fi

echo "### [OK] Unit test gate — PASSED ($REPORT_COUNT report files scanned)" >> "$SUMMARY"
echo "All tests passed ($REPORT_COUNT report files scanned) and the Sonar scan was submitted."
exit 0
