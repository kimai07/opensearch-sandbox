#!/usr/bin/env python3
"""2 つの検索 API の jar を起動し、同じリクエストのレスポンスを比較する。

PR が比較の処理を変更できないよう、このスクリプトは PR のコードとは別のチェックアウトから実行する。
結果を Job Summary に出力し、不一致があれば終了コード 1 で終了する。
"""
import argparse
import difflib
import json
import os
import pathlib
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

import search_report

INDEX = "compare-articles"
BASE_PORT = 18081
HEAD_PORT = 18082
STARTUP_TIMEOUT_SECONDS = 90


def send(method, url, body=None, content_type="application/json"):
    headers = {"Content-Type": content_type} if body is not None else {}
    request = urllib.request.Request(url, data=body, method=method, headers=headers)
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            return response.status, response.read().decode("utf-8")
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode("utf-8")


def reset_index(opensearch_url, resources):
    index_url = f"{opensearch_url}/{INDEX}"
    send("DELETE", index_url)

    status, text = send("PUT", index_url, (resources / "mapping.json").read_bytes())
    if status != 200:
        sys.exit(f"Failed to create index: {status} {text}")

    status, text = send(
        "POST",
        f"{index_url}/_bulk?refresh=true",
        (resources / "articles.ndjson").read_bytes(),
        "application/x-ndjson",
    )
    if status != 200 or json.loads(text)["errors"]:
        sys.exit(f"Failed to index documents: {status} {text}")


def start_app(jar, port, log_path):
    env = {**os.environ, "OPENSEARCH_INDEX": INDEX}
    log = log_path.open("w", encoding="utf-8")
    return subprocess.Popen(
        ["java", "-jar", str(jar), f"--server.port={port}"],
        env=env,
        stdout=log,
        stderr=subprocess.STDOUT,
    )


def wait_until_ready(process, port, log_path):
    deadline = time.monotonic() + STARTUP_TIMEOUT_SECONDS
    url = f"http://localhost:{port}{search_report.DEFAULT_ENDPOINT}?q=ready"
    while time.monotonic() < deadline:
        if process.poll() is not None:
            sys.exit(f"Application exited early. See {log_path}")
        try:
            status, _ = send("GET", url)
            if status == 200:
                return
        except urllib.error.URLError:
            pass
        time.sleep(1)
    sys.exit(f"Application did not become ready. See {log_path}")


def fetch(port, endpoint, case):
    params = {"q": case["q"]}
    if "size" in case:
        params["size"] = case["size"]
    query = urllib.parse.urlencode(params)
    status, text = send("GET", f"http://localhost:{port}{endpoint}?{query}")
    body = json.loads(text)
    snapshot = {"status": status, "body": body}
    lines = json.dumps(snapshot, indent=2, sort_keys=True, ensure_ascii=False).splitlines()
    return {"status": status, "hits": body.get("total"), "lines": lines}


def compare_case(case, output_dir):
    endpoint = case.get("endpoint", search_report.DEFAULT_ENDPOINT)
    base = fetch(BASE_PORT, endpoint, case)
    head = fetch(HEAD_PORT, endpoint, case)
    diff = "\n".join(
        difflib.unified_diff(
            base["lines"],
            head["lines"],
            fromfile=f"main/{case['name']}",
            tofile=f"pr/{case['name']}",
            lineterm="",
        )
    )
    if diff:
        (output_dir / f"{case['name']}.diff").write_text(diff + "\n", encoding="utf-8")
        print(diff)
    return {
        "name": case["name"],
        "endpoint": endpoint,
        "q": case["q"],
        "size": case.get("size"),
        "base_status": base["status"],
        "head_status": head["status"],
        "base_hits": base["hits"],
        "head_hits": head["hits"],
        "matched": not diff,
        "diff": diff,
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-jar", required=True, type=pathlib.Path)
    parser.add_argument("--head-jar", required=True, type=pathlib.Path)
    parser.add_argument("--resources", required=True, type=pathlib.Path)
    parser.add_argument("--output-dir", required=True, type=pathlib.Path)
    parser.add_argument("--opensearch-url", default="http://localhost:9200")
    parser.add_argument("--environment", default="docker")
    args = parser.parse_args()

    args.output_dir.mkdir(parents=True, exist_ok=True)
    cases = json.loads((args.resources / "cases.json").read_text(encoding="utf-8"))
    reset_index(args.opensearch_url, args.resources)

    base_log = args.output_dir / "base.log"
    head_log = args.output_dir / "head.log"
    base = start_app(args.base_jar, BASE_PORT, base_log)
    head = start_app(args.head_jar, HEAD_PORT, head_log)
    try:
        wait_until_ready(base, BASE_PORT, base_log)
        wait_until_ready(head, HEAD_PORT, head_log)
        results = [compare_case(case, args.output_dir) for case in cases]
    finally:
        base.terminate()
        head.terminate()
        base.wait()
        head.wait()
        send("DELETE", f"{args.opensearch_url}/{INDEX}")

    print(search_report.summary_line(results))
    print(search_report.mismatch_line(results))
    host = urllib.parse.urlparse(args.opensearch_url).netloc
    markdown = search_report.render_compare_markdown(results, args.environment, host)
    search_report.publish(markdown, results, os.environ)
    return 1 if search_report.mismatched_names(results) else 0


if __name__ == "__main__":
    sys.exit(main())
