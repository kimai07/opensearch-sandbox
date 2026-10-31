#!/usr/bin/env python3
"""リグレッションテストの結果を集計し、Job Summary に表を出力する。"""
import argparse
import json
import os
import pathlib
import sys

import search_report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--report", required=True, type=pathlib.Path)
    parser.add_argument("--cases", required=True, type=pathlib.Path)
    args = parser.parse_args()

    if not args.report.exists():
        print(f"Report not found: {args.report}", file=sys.stderr)
        return 1

    cases = json.loads(args.cases.read_text(encoding="utf-8"))
    results = search_report.read_regression_results(args.report, cases)
    print(search_report.summary_line(results))
    print(search_report.mismatch_line(results))
    search_report.publish(search_report.render_regression_markdown(results), results, os.environ)
    return 0


if __name__ == "__main__":
    sys.exit(main())
