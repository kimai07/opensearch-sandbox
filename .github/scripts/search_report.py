"""検索テストの結果を集計し、GitHub Actions の Job Summary 用の Markdown を作成する。"""
import re
import xml.etree.ElementTree as ElementTree

MATCH = "✅"
MISMATCH = "❌"
STATUS_DESCRIPTION_LIMIT = 140
CASE_NAME_INDEX = re.compile(r"\[(\d+)]$")


def summary_line(results):
    matched = sum(1 for result in results if result["matched"])
    return f"{len(results)}件中 {matched}件一致、{len(results) - matched}件不一致"


def status_description(results):
    """PR のステータスの説明文を作成する。長い場合は不一致のケースを切り詰める。"""
    summary = summary_line(results)
    mismatches = mismatched_names(results)
    if not mismatches:
        return summary
    description = f"{summary}: {', '.join(mismatches)}"
    if len(description) <= STATUS_DESCRIPTION_LIMIT:
        return description
    return description[: STATUS_DESCRIPTION_LIMIT - 1] + "…"


def mismatched_names(results):
    return [result["name"] for result in results if not result["matched"]]


def format_query(query):
    visible = query.replace("|", "\\|") if query.strip() else query.replace(" ", "␠")
    return f"`{visible}`"


def format_value(value):
    return "-" if value is None else str(value)


def match_mark(result):
    return MATCH if result["matched"] else MISMATCH


def mismatch_line(results):
    return f"不一致のケース: {', '.join(mismatched_names(results)) or 'なし'}"


def render_compare_markdown(results):
    lines = [
        "## main と PR のレスポンス比較",
        "",
        f"**{summary_line(results)}**",
        "",
        mismatch_line(results),
        "",
        "| ケース | 検索文字列 | size | ステータス（main） | ステータス（PR） | ヒット件数（main） | ヒット件数（PR） | レスポンス一致 |",
        "|---|---|---|---|---|---|---|---|",
    ]
    for result in results:
        lines.append(
            f"| {result['name']} | {format_query(result['q'])} | {format_value(result['size'])} "
            f"| {format_value(result['base_status'])} | {format_value(result['head_status'])} "
            f"| {format_value(result['base_hits'])} | {format_value(result['head_hits'])} "
            f"| {match_mark(result)} |"
        )

    for result in results:
        if not result["matched"]:
            lines += [
                "",
                f"<details><summary>{result['name']} の差分</summary>",
                "",
                "````diff",
                result["diff"],
                "````",
                "",
                "</details>",
            ]
    return "\n".join(lines) + "\n"


def render_regression_markdown(results):
    lines = [
        "## 期待値との比較",
        "",
        f"**{summary_line(results)}**",
        "",
        mismatch_line(results),
        "",
        "| ケース | 検索文字列 | size | 期待値との一致 |",
        "|---|---|---|---|",
    ]
    for result in results:
        lines.append(
            f"| {result['name']} | {format_query(result['q'])} | {format_value(result['size'])} "
            f"| {match_mark(result)} |"
        )
    return "\n".join(lines) + "\n"


def read_regression_results(report_path, cases):
    """Failsafe のレポートを読み、cases.json のケースごとの結果を返す。

    パラメータ化テストの [n] は cases.json の n 番目のケースに対応する。
    レポートにないケースは、実行されなかったものとして不一致にする。
    """
    passed = {}
    for testcase in ElementTree.parse(report_path).getroot().iter("testcase"):
        index = CASE_NAME_INDEX.search(testcase.get("name", ""))
        if index:
            failed = testcase.find("failure") is not None or testcase.find("error") is not None
            passed[int(index.group(1))] = not failed

    return [
        {"name": case["name"], "q": case["q"], "size": case.get("size"), "matched": passed.get(number, False)}
        for number, case in enumerate(cases, start=1)
    ]


def publish(markdown, results, env):
    """Job Summary と、後続のジョブが読む出力に書き込む。GitHub Actions の外では何もしない。"""
    summary_path = env.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        with open(summary_path, "a", encoding="utf-8") as file:
            file.write(markdown)

    output_path = env.get("GITHUB_OUTPUT")
    if output_path:
        with open(output_path, "a", encoding="utf-8") as file:
            file.write(f"summary={status_description(results)}\n")
            file.write(f"counts={summary_line(results)}\n")
            file.write(f"mismatches={','.join(mismatched_names(results))}\n")
