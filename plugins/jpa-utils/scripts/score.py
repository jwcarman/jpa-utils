#!/usr/bin/env python3
"""Scores one arm of a measurement (a baseline), or two and compares their successes with Fisher's exact test.

Usage: score.py <results dir> <arm A> [<arm B>] [--hidden N] [-v]

An arm is a glob of batch folders under the results dir (as run-evals.sh writes them), optionally
with the eval arm to read after a colon: 'old-b*', 'b*:without' (default arm: with). Each batch holds
<case>.json (claude plugin eval's report) and, for run n of an arm, <case>-<arm>-<n>.accept.txt
(lines "PASS accept:..." / "FAIL accept:...") and <case>-<arm>-<n>.check.txt (a "PASS build" or
"FAIL build" line).

A run succeeds when it ended without an error, every hidden test passed (and there were N of them,
with --hidden N), and its build passed. A run whose result files are missing fails; no run is ever
dropped. Prints per arm: n, successes, hidden-test and build passes, errors, mean cost, turns and
time; then the one-sided Fisher's exact p for each direction.
"""
import argparse
import glob
import json
import os
import re
from math import comb


def read(path):
    return open(path).read() if os.path.exists(path) else ""


def runs(results, spec, hidden):
    pattern, _, arm = spec.partition(":")
    arm = arm or "with"
    out = []
    for batch in sorted(glob.glob(os.path.join(results, pattern))):
        for report in sorted(glob.glob(os.path.join(batch, "*.json"))):
            data = json.load(open(report))
            for case in data.get("cases", []):
                for n, run in enumerate(case.get("arms", {}).get(arm, []), start=1):
                    tag = os.path.join(batch, f"{case['name']}-{arm}-{n}")
                    accept, check = read(f"{tag}.accept.txt"), read(f"{tag}.check.txt")
                    passed = len(re.findall(r"^PASS accept:", accept, re.M))
                    hidden_ok = passed > 0 and not re.search(r"^FAIL", accept, re.M) and (hidden is None or passed == hidden)
                    build_ok = bool(re.search(r"^PASS build", check, re.M))
                    out.append({
                        "batch": os.path.basename(batch), "n": n, "hidden": hidden_ok, "build": build_ok,
                        "error": run.get("error"), "success": hidden_ok and build_ok and run.get("error") is None,
                        "cost": run.get("costUsd") or 0.0, "turns": run.get("turns") or 0,
                        "time": run.get("durationSeconds") or 0,
                    })
    return out


def fisher_greater(a_succ, a_n, b_succ, b_n):
    """One-sided Fisher's exact p that arm a's success rate is higher than b's, given the margins."""
    total = a_succ + b_succ
    return sum(comb(a_n, k) * comb(b_n, total - k) for k in range(a_succ, min(a_n, total) + 1)) / comb(a_n + b_n, total)


def mean(values):
    return sum(values) / len(values) if values else float("nan")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("results")
    parser.add_argument("a")
    parser.add_argument("b", nargs="?", help="leave out to score one arm, as for a baseline")
    parser.add_argument("--hidden", type=int, help="how many hidden tests a case has (all must pass)")
    parser.add_argument("-v", action="store_true", help="one line per run")
    args = parser.parse_args()

    arms = {"A": (args.a, runs(args.results, args.a, args.hidden))}
    if args.b:
        arms["B"] = (args.b, runs(args.results, args.b, args.hidden))
    for label, (spec, rs) in arms.items():
        print(f"{label} {spec}: n={len(rs)} success={sum(r['success'] for r in rs)} "
              f"hidden={sum(r['hidden'] for r in rs)} build={sum(r['build'] for r in rs)} "
              f"errors={sum(r['error'] is not None for r in rs)} cost=${mean([r['cost'] for r in rs]):.3f} "
              f"turns={mean([r['turns'] for r in rs]):.1f} time={mean([r['time'] for r in rs]):.0f}s")
    a, b = arms["A"][1], arms.get("B", (None, []))[1]
    a_s, b_s = sum(r["success"] for r in a), sum(r["success"] for r in b)
    if a and b:
        print(f"p(B better than A) = {fisher_greater(b_s, len(b), a_s, len(a)):.4f}")
        print(f"p(A better than B) = {fisher_greater(a_s, len(a), b_s, len(b)):.4f}")
    if args.v:
        for label, (_, rs) in arms.items():
            for r in rs:
                print(label, r["batch"], r["n"], "success" if r["success"] else "FAIL",
                      f"hidden={r['hidden']} build={r['build']} error={r['error']} cost=${r['cost']:.3f}")


if __name__ == "__main__":
    main()
