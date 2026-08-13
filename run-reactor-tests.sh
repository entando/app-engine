#!/usr/bin/env bash
#
# run-reactor-tests.sh
# ---------------------------------------------------------------------------
# Runs the unit/integration tests of the reactor's inner modules with Maven.
#
#   1. reports the Maven version and the JVM in use;
#   2. lets you pick the module(s) to test from an interactive list
#      (UP/DOWN arrows to move, SPACE to select/deselect, ENTER to confirm);
#   3. runs the tests and collects a per-module PASS/FAIL summary.
#
# Usage:
#   ./run-reactor-tests.sh                     # interactive module picker
#   ./run-reactor-tests.sh engine cms-plugin   # non-interactive: given module(s)
#
# Environment overrides:
#   PROFILE=pre-deployment-verification    # Maven profile enabling the tests
#   MVN_OPTS="-o"                          # extra Maven options (e.g. offline)
#   ASSUME_YES=1                           # skip the picker, test every module
#   DRY_RUN=1                              # print the mvn command, do not run it
# ---------------------------------------------------------------------------

set -u -o pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_ROOT" || { echo "ERROR: cannot cd to $PROJECT_ROOT"; exit 1; }

PROFILE="${PROFILE:-pre-deployment-verification}"
MVN_OPTS="${MVN_OPTS:-}"
MVN_BIN="$(command -v mvn || true)"

TS="$(date +%Y%m%d-%H%M%S)"
RESULTS_DIR="$PROJECT_ROOT/test-results"
LOG_FILE="$RESULTS_DIR/reactor-tests-$TS.log"

c_bold=$'\033[1m'; c_green=$'\033[0;32m'; c_red=$'\033[0;31m'
c_yellow=$'\033[0;33m'; c_cyan=$'\033[0;36m'; c_off=$'\033[0m'
hr() { printf '%s\n' "----------------------------------------------------------------------"; }
say() { printf '%s\n' "$*"; }

# --- preconditions ---------------------------------------------------------
if [[ -z "$MVN_BIN" ]]; then
    say "${c_red}ERROR:${c_off} 'mvn' was not found on the PATH."; exit 127
fi
if [[ ! -f "$PROJECT_ROOT/pom.xml" ]]; then
    say "${c_red}ERROR:${c_off} no pom.xml found in $PROJECT_ROOT (not a reactor root)."; exit 1
fi

# --- reactor modules (in pom.xml order) ------------------------------------
mapfile -t MODULES < <(grep -oE "<module>[^<]+</module>" pom.xml | sed 's/<[^>]*>//g')
if [[ "${#MODULES[@]}" -eq 0 ]]; then
    say "${c_red}ERROR:${c_off} no <module> entries found in pom.xml."; exit 1
fi

# --- report Maven & JVM ----------------------------------------------------
hr
say "${c_bold}Entando App Engine — reactor test runner${c_off}"
hr
say "${c_bold}Maven / JVM in use:${c_off}"
# 'mvn -version' prints the Maven version, the Java version/vendor and the JVM home.
"$MVN_BIN" -version
say ""
say "JAVA_HOME    : ${JAVA_HOME:-<not set>}"
say "Project root : $PROJECT_ROOT"
say "Test profile : -P$PROFILE"
say "Extra opts   : ${MVN_OPTS:-<none>}"
hr

# ---------------------------------------------------------------------------
# Interactive multi-select checklist.
# Fills the global array SELECTED with the chosen module names.
# ---------------------------------------------------------------------------
SELECTED=()
select_modules() {
    local -a items=("$@")
    local n=${#items[@]}
    local -a checked
    local i cursor=0
    for ((i = 0; i < n; i++)); do checked[i]=0; done

    printf '%s\n' "${c_bold}Select the module(s) to test:${c_off}"
    printf '%s\n' "  ${c_cyan}UP/DOWN${c_off} move   ${c_cyan}SPACE${c_off} select/deselect   ${c_cyan}a${c_off} all   ${c_cyan}n${c_off} none   ${c_cyan}ENTER${c_off} confirm   ${c_cyan}q${c_off} quit"
    printf '\033[?25l'                          # hide cursor
    # shellcheck disable=SC2064
    trap 'printf "\033[?25h"' RETURN            # restore cursor when function returns

    local first=1 key rest mark pointer line
    while true; do
        [[ $first -eq 0 ]] && printf '\033[%dA' "$n"   # move up to redraw
        first=0
        for ((i = 0; i < n; i++)); do
            mark=" "; [[ ${checked[i]} -eq 1 ]] && mark="x"
            if [[ $i -eq $cursor ]]; then
                pointer="${c_yellow}>${c_off}"
                line=$(printf '%s [%s] %s%s%s' "$pointer" "$mark" "$c_bold" "${items[i]}" "$c_off")
            else
                pointer=" "
                line=$(printf '%s [%s] %s' "$pointer" "$mark" "${items[i]}")
            fi
            printf '\r\033[2K%s\n' "$line"      # clear line, then print
        done

        IFS= read -rsn1 key
        if [[ $key == $'\033' ]]; then          # escape sequence (arrow keys)
            IFS= read -rsn2 -t 0.05 rest || rest=""
            key+="$rest"
        fi
        case "$key" in
            $'\033[A' | k) ((cursor = (cursor - 1 + n) % n)) ;;   # up
            $'\033[B' | j) ((cursor = (cursor + 1) % n)) ;;       # down
            ' ')           checked[cursor]=$((1 - checked[cursor])) ;;
            a | A)         for ((i = 0; i < n; i++)); do checked[i]=1; done ;;
            n | N)         for ((i = 0; i < n; i++)); do checked[i]=0; done ;;
            '' | $'\n' | $'\r') break ;;                         # ENTER -> confirm
            q | Q | $'\033') printf '\033[?25h'; return 1 ;;     # quit / bare ESC
        esac
    done

    SELECTED=()
    for ((i = 0; i < n; i++)); do
        [[ ${checked[i]} -eq 1 ]] && SELECTED+=("${items[i]}")
    done
    return 0
}

# --- decide the selection --------------------------------------------------
if [[ "$#" -gt 0 ]]; then
    # explicit module names on the command line -> non-interactive
    SELECTED=("$@")
elif [[ "${ASSUME_YES:-0}" == "1" || ! -t 0 || ! -t 1 ]]; then
    # no TTY (CI/pipe) or ASSUME_YES -> test everything, no prompt
    SELECTED=("${MODULES[@]}")
    say "Non-interactive run: testing all ${#MODULES[@]} modules."
else
    if ! select_modules "${MODULES[@]}"; then
        say ""; say "Aborted by user. No tests were run."; exit 0
    fi
fi

if [[ "${#SELECTED[@]}" -eq 0 ]]; then
    say ""; say "No module selected. No tests were run."; exit 0
fi

# --- validate selected names -----------------------------------------------
for m in "${SELECTED[@]}"; do
    found=0
    for known in "${MODULES[@]}"; do [[ "$m" == "$known" ]] && found=1 && break; done
    if [[ $found -eq 0 ]]; then
        say "${c_red}ERROR:${c_off} '$m' is not a reactor module. Known: ${MODULES[*]}"; exit 1
    fi
done

# --- selection mode: whole reactor vs a subset -----------------------------
# NOTE: for a subset we deliberately DO NOT test with '-am'. '-am' ("also make")
# would run the *test* phase of every upstream dependency module too, i.e. test
# far more than the user picked. Instead we build the deps without tests first
# (below) and then test only the selected modules.
SUBSET=0
MODULE_CSV=""
if [[ "${#SELECTED[@]}" -lt "${#MODULES[@]}" ]]; then
    SUBSET=1
    MODULE_CSV="$(IFS=,; echo "${SELECTED[*]}")"
fi
DEPS_LOG="$RESULTS_DIR/reactor-deps-$TS.log"

hr
say "${c_bold}About to test:${c_off} ${SELECTED[*]}"
say "Log file: $LOG_FILE"
hr

mkdir -p "$RESULTS_DIR"

# --- run the tests ---------------------------------------------------------
if [[ "${DRY_RUN:-0}" == "1" ]]; then
    say "${c_yellow}DRY_RUN:${c_off} would execute:"
    if [[ "$SUBSET" -eq 1 ]]; then
        say "  [1/2 build deps, no tests] $MVN_BIN $MVN_OPTS -pl $MODULE_CSV -am install -DskipTests"
        say "  [2/2 test selected only ]  $MVN_BIN $MVN_OPTS -P$PROFILE -fae -pl $MODULE_CSV test"
    else
        say "  $MVN_BIN $MVN_OPTS -P$PROFILE -fae test"
    fi
    exit 0
fi

START_EPOCH="$(date +%s)"
set -f
if [[ "$SUBSET" -eq 1 ]]; then
    # Phase 1: compile+install the selected modules AND their upstream deps, WITHOUT
    # running tests (-DskipTests still builds the test-jars downstream tests need),
    # so phase 2 can resolve every dependency from the local repo.
    say "${c_bold}[1/2] Building selected modules + upstream dependencies (no tests)...${c_off}"
    say "      deps log: $DEPS_LOG"
    hr
    # shellcheck disable=SC2086
    "$MVN_BIN" $MVN_OPTS -pl "$MODULE_CSV" -am install -DskipTests 2>&1 | tee "$DEPS_LOG"
    DEPS_STATUS="${PIPESTATUS[0]}"
    if [[ "$DEPS_STATUS" -ne 0 ]]; then
        set +f
        hr
        say "${c_red}${c_bold}Dependency build failed — cannot run the selected tests.${c_off}"
        say "  See $DEPS_LOG"
        exit "$DEPS_STATUS"
    fi
    # Phase 2: run tests for the SELECTED modules only (no -am, so deps are NOT re-tested).
    say ""
    say "${c_bold}[2/2] Running tests for the selected module(s) only: ${SELECTED[*]}${c_off}"
    hr
    # shellcheck disable=SC2086
    "$MVN_BIN" $MVN_OPTS -P"$PROFILE" -fae -pl "$MODULE_CSV" test 2>&1 | tee "$LOG_FILE"
    MVN_STATUS="${PIPESTATUS[0]}"
else
    say "${c_bold}Running tests for all reactor modules...${c_off}"
    hr
    # shellcheck disable=SC2086
    "$MVN_BIN" $MVN_OPTS -P"$PROFILE" -fae test 2>&1 | tee "$LOG_FILE"
    MVN_STATUS="${PIPESTATUS[0]}"
fi
set +f
ELAPSED=$(( $(date +%s) - START_EPOCH ))

# --- collect results -------------------------------------------------------
hr
say "${c_bold}Result summary${c_off}  (elapsed: $((ELAPSED/60))m $((ELAPSED%60))s)"
hr
say "${c_bold}Per-module (reactor summary):${c_off}"
if grep -q "Reactor Summary" "$LOG_FILE"; then
    sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" \
        | sed -n '/Reactor Summary/,/^\[INFO\] -\{20,\}$/p' \
        | grep -E "SUCCESS|FAILURE|SKIPPED" | sed -E 's/^\[INFO\] /  /'
else
    say "  (no multi-module reactor summary — single module selected, or the build stopped early;"
    say "   see the test totals and the BUILD status below)"
fi

say ""
say "${c_bold}Test totals (surefire, per module):${c_off}"
sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" \
    | grep -E "Tests run: [0-9]+, Failures: [0-9]+, Errors: [0-9]+, Skipped: [0-9]+" \
    | grep -vE " -- in | <<< " | sed 's/^\[[A-Z]*\] /  /' | sed 's/^/  /' || true

FAILED_LINES="$(sed 's/\x1b\[[0-9;]*m//g' "$LOG_FILE" | grep -E "<<< (FAILURE|ERROR)!" || true)"
if [[ -n "$FAILED_LINES" ]]; then
    say ""; say "${c_red}${c_bold}Failing/errored test classes:${c_off}"
    printf '%s\n' "$FAILED_LINES" | sed 's/^/  /'
fi

hr
if [[ "$MVN_STATUS" -eq 0 ]]; then
    say "${c_green}${c_bold}BUILD SUCCESS — all selected module tests passed.${c_off}"
else
    say "${c_red}${c_bold}BUILD FAILURE — see failures above and the full log:${c_off}"
    say "  $LOG_FILE"
    say "  Surefire reports: <module>/target/surefire-reports/"
fi
hr
exit "$MVN_STATUS"
