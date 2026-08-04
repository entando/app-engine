#!/usr/bin/env bash
#
# Extensive test suite + assertion harness for AdvContentSearchController
#   GET /api/plugins/advcontentsearch/contents
#   GET /api/plugins/advcontentsearch/facetedcontents
#
# The controller has NO @RestAccessControl -> guest-accessible. A Bearer token
# switches results to that user's visibility. Endpoints are only live when Solr
# is enabled (@SolrActive(true)); if Solr is OFF the happy-path asserts will
# report FAIL with HTTP 404 (that's the signal, not a bug in the test).
#
# Usage:
#   bash test_advcontentsearch.sh [-u|--base-url URL] [-h|--help]
#   HOST=http://localhost:8080 CONTEXT=entando-de-app TOKEN=<jwt> \
#       bash test_advcontentsearch.sh
#   bash test_advcontentsearch.sh --base-url https://my.host/entando-de-app
#   # See --help for details. Exit code is non-zero if any assertion fails.
# ---------------------------------------------------------------------------

HOST="${HOST:-http://localhost:8080}"
CONTEXT="${CONTEXT:-entando-de-app}"
TOKEN="${TOKEN:-PASTE_JWT_HERE}"
AUTH=(-H "Authorization: Bearer $TOKEN")
VERBOSE="${VERBOSE:-0}"   # VERBOSE=1 prints the response body for every test

usage() {
  cat <<'EOF'
Usage: test_advcontentsearch.sh [-u|--base-url URL] [-h|--help]

Runs the AdvContentSearch REST assertion suite against a running instance.

Options:
  -u, --base-url URL   Base URL of the instance under test, up to the web-app context
                       (e.g. https://my.host/entando-de-app). Overrides HOST/CONTEXT.
                       "/api/plugins/advcontentsearch" is appended automatically -- do NOT
                       include /api yourself (a trailing /api is tolerated if you do).
  -h, --help           Show this help and exit.

Environment variables (still honoured):
  HOST      default http://localhost:8080  (used to build the base URL when --base-url is absent)
  CONTEXT   default entando-de-app         (   "        "     "    "    "                       )
  TOKEN     Bearer JWT for the authenticated cases (A3/A4/L6); those are skipped without it
  VERBOSE=1 print the response body of every request

Examples:
  test_advcontentsearch.sh
  test_advcontentsearch.sh --base-url https://demo.entando.org/entando-de-app
  HOST=http://localhost:8081 test_advcontentsearch.sh
EOF
}

# --- command-line arguments -------------------------------------------------
BASE_URL=""
while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help)     usage; exit 0 ;;
    -u|--base-url)
      [[ -z "${2:-}" ]] && { printf 'error: %s requires a value\n\n' "$1" >&2; usage >&2; exit 2; }
      BASE_URL="$2"; shift 2 ;;
    --base-url=*)  BASE_URL="${1#*=}"; shift ;;
    -*)            printf "error: unknown option '%s'\n\n" "$1" >&2; usage >&2; exit 2 ;;
    *)             printf "error: unexpected argument '%s'\n\n" "$1" >&2; usage >&2; exit 2 ;;
  esac
done

# An explicit --base-url wins; otherwise fall back to the HOST/CONTEXT default.
# The arg is the instance URL up to the web-app context (no /api needed); this script
# appends /api/plugins/advcontentsearch. A trailing /api is tolerated if the user adds it.
if [[ -n "$BASE_URL" ]]; then
  BASE_URL="${BASE_URL%/}"        # strip any trailing slash
  BASE_URL="${BASE_URL%/api}"     # tolerate a trailing /api so we never build /api/api
  BASE="$BASE_URL/api/plugins/advcontentsearch"
else
  BASE="$HOST/$CONTEXT/api/plugins/advcontentsearch"
fi

# Auth tests are skipped (not failed) unless a real token is supplied.
TOKEN_OK=1; [[ -z "$TOKEN" || "$TOKEN" == "PASTE_JWT_HERE" ]] && TOKEN_OK=0

PASS=0; FAIL=0; SKIP=0; declare -a FAILURES; declare -a SKIPPED

skip() { SKIP=$((SKIP+1)); SKIPPED+=("$1"); printf '  \033[33mSKIP\033[0m              %s\n' "$1"; }

# assert <expected> <label> <curl-args...>   (GET, params via --data-urlencode)
#   <expected> is a regex alternation of acceptable codes, e.g. "200" or "400|422"
assert()     { _run 1 "$@"; }
# assert_raw <expected> <label> <curl-args...>  (query string sent VERBATIM; for
#   malformed-encoding / method / negotiation tests)
assert_raw() { _run 0 "$@"; }

_run() {
  local useG="$1" expected="$2" label="$3"; shift 3
  local out code body url lastline

  # Percent-encode '[' and ']' in the NAME part of every --data-urlencode arg.
  # curl url-encodes only the value, leaving the parameter name verbatim; Tomcat
  # rejects raw brackets in the query string with its own HTTP 400 (Jetty tolerates
  # them). Sending filters%5B0%5D.attribute keeps the suite portable across both
  # containers -- Spring's data binder decodes it back to filters[0].attribute.
  local -a cargs=()
  local prev="" a name value
  for a in "$@"; do
    if [[ "$prev" == "--data-urlencode" && "$a" == *"="* ]]; then
      name="${a%%=*}"; value="${a#*=}"
      name="${name//\[/%5B}"; name="${name//\]/%5D}"
      cargs+=("$name=$value")
    else
      cargs+=("$a")
    fi
    prev="$a"
  done

  # %{url_effective} is captured so VERBOSE can show the exact endpoint curl hit
  # (with the encoded query string). -g / --globoff: don't treat [ ] as curl globs.
  if [[ "$useG" == "1" ]]; then
    out=$(curl -sS -g -G -m 30 -w $'\n%{http_code}\t%{url_effective}' "${cargs[@]}" 2>/dev/null)
  else
    out=$(curl -sS -g    -m 30 -w $'\n%{http_code}\t%{url_effective}' "${cargs[@]}" 2>/dev/null)
  fi
  lastline="${out##*$'\n'}"; body="${out%$'\n'*}"
  code="${lastline%%$'\t'*}"; url="${lastline#*$'\t'}"
  if [[ "$code" =~ ^(${expected})$ ]]; then
    PASS=$((PASS+1)); printf '  \033[32mPASS\033[0m [%s]        %s\n' "$code" "$label"
  else
    FAIL=$((FAIL+1)); FAILURES+=("$label — want:$expected got:$code")
    printf '  \033[31mFAIL\033[0m [got %s want %s] %s\n' "$code" "$expected" "$label"
    printf '       body: %s\n' "$(printf '%s' "$body" | tr -d '\n' | head -c 300)"
  fi
  if [[ "$VERBOSE" == "1" ]]; then
    printf '       URL:  %s\n' "$url"
    printf '       body: %s\n' "$(printf '%s' "$body" | head -c 800)"
  fi
}

section() { printf '\n\033[1m########## %s ##########\033[0m\n' "$1"; }

# ===========================================================================
section "LEGIT PAYLOADS (real-world valid requests, expect 200)"
# ---------------------------------------------------------------------------
# L1. Latest 10 published EVENTS, newest first
assert 200 "L1 latest 10 events, newest first" "$BASE/contents" \
  --data-urlencode "filters[0].attribute=typeCode" --data-urlencode "filters[0].operator=eq" --data-urlencode "filters[0].value=EVN" \
  --data-urlencode "sort=created" --data-urlencode "direction=DESC" \
  --data-urlencode "page=1" --data-urlencode "pageSize=10"

# L2. Full-text search for "entando" including attachment text, English
assert 200 "L2 full-text incl. attachments (en)" "$BASE/contents" \
  --data-urlencode "text=entando" --data-urlencode "searchOption=all" \
  --data-urlencode "includeAttachments=true" --data-urlencode "lang=en"

# L3. Faceted search by category (returns facet occurrences for navigation)
assert 200 "L3 faceted-by-category counts" "$BASE/facetedcontents" \
  --data-urlencode "csvCategories=general" --data-urlencode "pageSize=25"

# L4. Articles OR Events in one result set (doubleFilters = OR-group)
assert 200 "L4 articles OR events (doubleFilters)" "$BASE/contents" \
  --data-urlencode "doubleFilters[0][0].attribute=typeCode" --data-urlencode "doubleFilters[0][0].operator=eq" --data-urlencode "doubleFilters[0][0].value=ART" \
  --data-urlencode "doubleFilters[0][1].attribute=typeCode" --data-urlencode "doubleFilters[0][1].operator=eq" --data-urlencode "doubleFilters[0][1].value=EVN" \
  --data-urlencode "sort=modified" --data-urlencode "direction=DESC"

# L5. Entity-attribute filter + full-text, Italian, relevance-boosted term
assert 200 "L5 entity-attr + boosted full-text (it)" "$BASE/facetedcontents" \
  --data-urlencode "lang=it" \
  --data-urlencode "filters[0].entityAttr=title" --data-urlencode "filters[0].operator=like" --data-urlencode "filters[0].value=corso" \
  --data-urlencode "filters[1].fullText=true" --data-urlencode "filters[1].value=formazione" --data-urlencode "filters[1].relevancy=5"

# L6. Date-range browse (created after a date), authenticated
if (( TOKEN_OK )); then
  assert 200 "L6 created-after date range (auth)" "$BASE/contents" "${AUTH[@]}" \
    --data-urlencode "filters[0].attribute=created" --data-urlencode "filters[0].operator=gt" \
    --data-urlencode "filters[0].type=date" --data-urlencode "filters[0].value=2020-01-01 00:00:00" \
    --data-urlencode "sort=created" --data-urlencode "direction=ASC"
else
  skip "L6 created-after date range (auth) — no TOKEN"
fi

# ===========================================================================
section "A. SMOKE / HAPPY PATH"
assert 200 "A1 contents guest defaults"          "$BASE/contents"
assert 200 "A2 facetedcontents guest defaults"   "$BASE/facetedcontents"
if (( TOKEN_OK )); then
  assert 200 "A3 contents authenticated"         "$BASE/contents"        "${AUTH[@]}"
  assert 200 "A4 facetedcontents authenticated"  "$BASE/facetedcontents" "${AUTH[@]}"
else
  skip "A3 contents authenticated — no TOKEN"
  skip "A4 facetedcontents authenticated — no TOKEN"
fi

section "B. PAGINATION & SORTING (expect 200)"
assert 200 "B1 page1 size10"        "$BASE/contents" --data-urlencode "page=1" --data-urlencode "pageSize=10"
assert 200 "B2 page2 size5"         "$BASE/contents" --data-urlencode "page=2" --data-urlencode "pageSize=5"
assert 200 "B3 sort=created DESC"   "$BASE/contents" --data-urlencode "sort=created"  --data-urlencode "direction=DESC"
assert 200 "B4 sort=modified ASC"   "$BASE/contents" --data-urlencode "sort=modified" --data-urlencode "direction=ASC"
assert 200 "B5 sort=descr ASC"      "$BASE/contents" --data-urlencode "sort=descr"    --data-urlencode "direction=ASC"
assert 200 "B6 pageSize=0"          "$BASE/contents" --data-urlencode "pageSize=0"
assert 200 "B7 pageSize=1000"       "$BASE/contents" --data-urlencode "pageSize=1000"

section "C. METADATA FILTERS (expect 200)"
assert 200 "C1 eq typeCode EVN"     "$BASE/contents" --data-urlencode "filters[0].attribute=typeCode" --data-urlencode "filters[0].operator=eq"  --data-urlencode "filters[0].value=EVN"
assert 200 "C2 not typeCode EVN"    "$BASE/contents" --data-urlencode "filters[0].attribute=typeCode" --data-urlencode "filters[0].operator=not" --data-urlencode "filters[0].value=EVN"
assert 200 "C3 like descr"          "$BASE/contents" --data-urlencode "filters[0].attribute=descr"    --data-urlencode "filters[0].operator=like" --data-urlencode "filters[0].value=news"
assert 200 "C4 gt created (date)"   "$BASE/contents" --data-urlencode "filters[0].attribute=created"  --data-urlencode "filters[0].operator=gt" --data-urlencode "filters[0].type=date" --data-urlencode "filters[0].value=2000-01-01 00:00:00"
assert 200 "C5 lt created (date)"   "$BASE/contents" --data-urlencode "filters[0].attribute=created"  --data-urlencode "filters[0].operator=lt" --data-urlencode "filters[0].type=date" --data-urlencode "filters[0].value=2100-12-31 23:59:59"
assert 200 "C6 multi-filter+order"  "$BASE/contents" \
  --data-urlencode "filters[0].attribute=created"  --data-urlencode "filters[0].order=DESC" \
  --data-urlencode "filters[1].attribute=typeCode" --data-urlencode "filters[1].operator=eq" --data-urlencode "filters[1].value=EVN" \
  --data-urlencode "pageSize=20"

section "D. ENTITY-ATTRIBUTE FILTERS (expect 200)"
assert 200 "D1 entityAttr like (en)"  "$BASE/contents"        --data-urlencode "filters[0].entityAttr=title" --data-urlencode "filters[0].operator=like" --data-urlencode "filters[0].value=abc" --data-urlencode "lang=en"
assert 200 "D2 entityAttr date gt"    "$BASE/facetedcontents" --data-urlencode "filters[0].entityAttr=date"  --data-urlencode "filters[0].operator=gt" --data-urlencode "filters[0].type=date" --data-urlencode "filters[0].value=2020-01-01 00:00:00"
# D3/D4: boolean-like nested in a Composite. entityAttr = full composite path joined with '_'
# (e.g. promo_active, or outer_inner_flag when doubly nested). Indexed iff that composite child is
# flagged searchable=true (inherited from the content type; no environment flag). The demo app has no
# such attribute, so these document the URL form and stay 200 (empty payload), like the postman set.
assert 200 "D3 nested boolean (path)" "$BASE/contents"        --data-urlencode "filters[0].entityAttr=promo_active" --data-urlencode "filters[0].type=boolean" --data-urlencode "filters[0].operator=eq" --data-urlencode "filters[0].value=true"
assert 200 "D4 nested ThreeState none" "$BASE/contents"       --data-urlencode "filters[0].entityAttr=promo_confirmed" --data-urlencode "filters[0].type=string" --data-urlencode "filters[0].operator=eq" --data-urlencode "filters[0].value=none"

section "E. FULL-TEXT SEARCH (expect 200)"
assert 200 "E1 text=entando"                 "$BASE/contents"        --data-urlencode "text=entando"
assert 200 "E2 text exact"                   "$BASE/facetedcontents" --data-urlencode "text=entando" --data-urlencode "searchOption=exact"
assert 200 "E3 text all-words"               "$BASE/contents"        --data-urlencode "text=open source" --data-urlencode "searchOption=all"
assert 200 "E4 text incl. attachments"       "$BASE/contents"        --data-urlencode "text=manual" --data-urlencode "includeAttachments=true"
assert 200 "E5 text lang=it"                 "$BASE/contents"        --data-urlencode "text=guida" --data-urlencode "lang=it"
assert 200 "E6 full-text via filter"         "$BASE/contents"        --data-urlencode "filters[0].fullText=true" --data-urlencode "filters[0].value=entando" --data-urlencode "filters[0].searchOption=exact"

section "F. CATEGORIES (expect 200)"
assert 200 "F1 single csv cat"      "$BASE/facetedcontents" --data-urlencode "csvCategories=cat1"
assert 200 "F2 csv OR-set"          "$BASE/facetedcontents" --data-urlencode "csvCategories=cat1,cat2,cat3"
assert 200 "F3 repeated csv param"  "$BASE/facetedcontents" --data-urlencode "csvCategories=cat1" --data-urlencode "csvCategories=cat2"

section "G. DOUBLE FILTERS & RELEVANCY (expect 200)"
assert 200 "G1 doubleFilters OR"    "$BASE/contents" \
  --data-urlencode "doubleFilters[0][0].attribute=typeCode" --data-urlencode "doubleFilters[0][0].operator=eq" --data-urlencode "doubleFilters[0][0].value=EVN" \
  --data-urlencode "doubleFilters[0][1].attribute=typeCode" --data-urlencode "doubleFilters[0][1].operator=eq" --data-urlencode "doubleFilters[0][1].value=ART"
assert 200 "G2 relevancy boost"     "$BASE/contents" --data-urlencode "text=entando" --data-urlencode "filters[0].fullText=true" --data-urlencode "filters[0].value=news" --data-urlencode "filters[0].relevancy=5"

section "H. VALIDATION ERRORS (expect 400)"
assert 400 "H1 page=0 (110)"             "$BASE/contents" --data-urlencode "page=0"
assert 400 "H2 page=-1 (110)"            "$BASE/contents" --data-urlencode "page=-1"
assert 400 "H3 pageSize=-5 (112)"        "$BASE/contents" --data-urlencode "pageSize=-5"
assert 400 "H4 direction bad (102)"      "$BASE/contents" --data-urlencode "direction=SIDEWAYS"
assert 400 "H5 sort invalid (100)"       "$BASE/contents" --data-urlencode "sort=notARealField"
assert 400 "H6 filter attr invalid(101)" "$BASE/contents" --data-urlencode "filters[0].attribute=bogusAttr" --data-urlencode "filters[0].value=x"
assert 400 "H7 operator invalid (103)"   "$BASE/contents" --data-urlencode "filters[0].attribute=typeCode" --data-urlencode "filters[0].operator=bananas" --data-urlencode "filters[0].value=EVN"
# H8: date-format validation only runs for attributes in getDateFilterKeys(), which
# returns an empty list for this controller, so an invalid date is NOT rejected -> 200.
assert 200 "H8 invalid date not validated" "$BASE/contents" --data-urlencode "filters[0].attribute=created" --data-urlencode "filters[0].operator=gt" --data-urlencode "filters[0].type=date" --data-urlencode "filters[0].value=not-a-date"

section "I. SECURITY / ROBUSTNESS"
# sanitized -> should succeed (200), NOT 500
assert 200 "I1 lucene reserved chars"    "$BASE/facetedcontents" --data-urlencode 'text=cat:dog AND *:*'
assert 200 "I2 all reserved chars"       "$BASE/facetedcontents" --data-urlencode 'text=+ - && || ! ( ) { } [ ] ^ " ~ * ? : \'
assert 200 "I3 query-injection attempt"  "$BASE/facetedcontents" --data-urlencode 'text=") OR (1=1'
assert 200 "I4 sql-ish value injection"  "$BASE/contents"        --data-urlencode "filters[0].attribute=descr" --data-urlencode "filters[0].operator=like" --data-urlencode "filters[0].value=' OR '1'='1"
assert 200 "I5 xss payload in text"      "$BASE/facetedcontents" --data-urlencode 'text=<script>alert(1)</script>'
assert 200 "I6 leading wildcard"         "$BASE/facetedcontents" --data-urlencode 'text=*ento'
assert 200 "I7 very long input (5k)"     "$BASE/facetedcontents" --data-urlencode "text=$(printf 'A%.0s' {1..5000})"
assert 200 "I8 unicode / emoji"          "$BASE/facetedcontents" --data-urlencode 'text=日本語 😀 café'
assert 200 "I9 sparse high filter index" "$BASE/contents"        --data-urlencode "filters[99].attribute=typeCode" --data-urlencode "filters[99].operator=eq" --data-urlencode "filters[99].value=EVN"
# Malformed percent-encoding is handled at the servlet-container layer, and the outcome
# is container-specific: Jetty rejects it at query parse (400/500 BadMessageException),
# while Tomcat tolerates it (decodes/ignores the bad token) and the request reaches the
# app, returning 200. Any of these is acceptable here -- the point is that a bad %-escape
# must not hang or produce an unhandled crash; accept 200|400|500 for portability.
assert_raw "200|400|500" "I10 malformed encoding %7%"  "$BASE/facetedcontents?text=%7%"
assert_raw "200|400|500" "I11 malformed encoding %ZZ"  "$BASE/contents?filters[0].value=%ZZ"

section "J. CONTENT NEGOTIATION / METHOD"
assert_raw 405 "J1 POST not allowed"     -X POST "$BASE/contents"
assert     406 "J2 Accept xml unsupported" "$BASE/contents" -H "Accept: application/xml"
assert_raw 404 "J3 unmapped path"        "$BASE/unknownpath"

# ===========================================================================
printf '\n\033[1m==================== SUMMARY ====================\033[0m\n'
printf '  Passed: \033[32m%d\033[0m   Failed: \033[31m%d\033[0m   Skipped: \033[33m%d\033[0m   Total: %d\n' \
  "$PASS" "$FAIL" "$SKIP" "$((PASS+FAIL+SKIP))"
if (( SKIP > 0 )); then
  printf '\n  Skipped:\n'
  for s in "${SKIPPED[@]}"; do printf '    \033[33m•\033[0m %s\n' "$s"; done
fi
if (( FAIL > 0 )); then
  printf '\n  Failures:\n'
  for f in "${FAILURES[@]}"; do printf '    \033[31m✗\033[0m %s\n' "$f"; done
fi
exit $(( FAIL > 0 ? 1 : 0 ))
