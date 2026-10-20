#!/bin/bash
#
# Classifies the branch push that triggered this run by the commits it adds to
# the branch's first-parent history.
#
# Prints on stdout:
#   PUSH_ORIGIN=pr-merge|direct|unknown
#   PUSH_HEAD_PR=<PR landed by the head commit, empty unless pr-merge>
#   PUSH_MERGED_PRS=<comma-separated PRs landed by this push>
#   PUSH_DIRECT_COMMITS=<comma-separated short SHAs no merged PR accounts for>
#
#   pr-merge  every added first-parent commit was landed by a PR merged into this
#             branch within this push (merge, squash or rebase merge)
#   direct    at least one added commit was not, or the push rewrote history
#   unknown   the push could not be resolved; callers treat it as direct
#
set -uo pipefail

MAX_CHAIN=50
ZERO_SHA=0000000000000000000000000000000000000000

repo="${GITHUB_REPOSITORY:-}"
branch="${GITHUB_REF_NAME:-}"
after="${GITHUB_SHA:-}"
before=$(jq -r '.before // empty' "${GITHUB_EVENT_PATH:-/dev/null}" 2>/dev/null || true)
forced=$(jq -r '.forced // false' "${GITHUB_EVENT_PATH:-/dev/null}" 2>/dev/null || echo false)

emit() {
  printf 'PUSH_ORIGIN=%s\nPUSH_HEAD_PR=%s\nPUSH_MERGED_PRS=%s\nPUSH_DIRECT_COMMITS=%s\n' "$1" "${2:-}" "${3:-}" "${4:-}"
  exit 0
}
note() { echo "~> push-origin: $*" >&2; }

[ -n "$repo" ] && [ -n "$branch" ] && [ -n "$after" ] || { note "missing GITHUB_* context"; emit unknown; }

if [ "$forced" = "true" ]; then
  note "forced push"
  emit direct "" "" "${after:0:9}"
fi

# --- first-parent chain of the commits this push adds, head first ------------
chain=()
if [ -z "$before" ] || [ "$before" = "$ZERO_SHA" ]; then
  note "branch creation: only the head commit is classified"
  chain=("$after")
else
  cmp=$(gh api "repos/$repo/compare/$before...$after" 2>/dev/null) || { note "compare API failed"; emit unknown; }
  status=$(jq -r '.status // empty' <<<"$cmp")
  case "$status" in
    ahead) ;;
    diverged) note "history rewritten ($before is not an ancestor of $after)"; emit direct "" "" "${after:0:9}" ;;
    *) note "compare status '$status'"; emit unknown ;;
  esac
  total=$(jq -r '.total_commits' <<<"$cmp")
  listed=$(jq -r '.commits | length' <<<"$cmp")
  [ "$total" = "$listed" ] || { note "compare lists $listed of $total commits"; emit unknown; }

  declare -A first_parent
  while read -r sha parent; do
    first_parent[$sha]="$parent"
  done < <(jq -r '.commits[] | "\(.sha) \(.parents[0].sha // "")"' <<<"$cmp")

  cur="$after"
  while [ "$cur" != "$before" ]; do
    [ -n "${first_parent[$cur]+x}" ] || { note "first-parent chain leaves the compared range at $cur"; emit unknown; }
    chain+=("$cur")
    [ "${#chain[@]}" -le "$MAX_CHAIN" ] || { note "more than $MAX_CHAIN first-parent commits"; emit unknown; }
    cur="${first_parent[$cur]}"
  done
fi

# --- classify each chain commit -----------------------------------------------
# A commit is accounted for when it is associated with a PR merged into this
# branch whose merge commit is itself in the chain, i.e. the PR landed with
# this push. Merge and squash merges land the merge commit itself; a rebase
# merge lands several commits, the last of which is the PR's merge_commit_sha.
in_chain=" ${chain[*]} "
head_pr=""
merged_prs=()
direct=()
for c in "${chain[@]}"; do
  prs=$(gh api "repos/$repo/commits/$c/pulls" \
    --jq "[.[] | select(.merged_at != null and .base.ref == \"$branch\") | \"\(.number) \(.merge_commit_sha)\"] | .[]" \
    2>/dev/null) || { note "PR lookup failed for $c"; emit unknown; }
  found=""
  while read -r num msha; do
    [ -n "$num" ] || continue
    if [[ "$in_chain" == *" $msha "* ]]; then
      found="$num"
      [ "$msha" = "$after" ] && head_pr="$num"
    fi
  done <<<"$prs"
  if [ -n "$found" ]; then
    [[ " ${merged_prs[*]} " == *" $found "* ]] || merged_prs+=("$found")
  else
    direct+=("${c:0:9}")
  fi
done

merged_csv=$(IFS=,; echo "${merged_prs[*]}")
if [ "${#direct[@]}" -eq 0 ] && [ -n "$head_pr" ]; then
  emit pr-merge "$head_pr" "$merged_csv" ""
fi
if [ "${#direct[@]}" -eq 0 ]; then
  note "every commit is accounted for, but no PR landed the head commit"
  emit unknown "" "$merged_csv" ""
fi
emit direct "" "$merged_csv" "$(IFS=,; echo "${direct[*]}")"
