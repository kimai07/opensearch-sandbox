"""search_report のテスト。python3 -m unittest discover -s .github/scripts で実行する。"""
import pathlib
import tempfile
import unittest

import search_report


def result(name="single-term", matched=True, **overrides):
    values = {
        "name": name,
        "endpoint": "/api/v1/articles/search",
        "q": "BM25",
        "size": None,
        "base_status": 200,
        "head_status": 200,
        "base_hits": 1,
        "head_hits": 1,
        "matched": matched,
        "diff": "",
    }
    return {**values, **overrides}


def compare_markdown(results):
    return search_report.render_compare_markdown(results, "docker", "localhost:9200")


def regression_markdown(results):
    return search_report.render_regression_markdown(results, "docker", "localhost:9200")


class SummaryLineTest(unittest.TestCase):
    def test_all_match(self):
        results = [result(), result("other")]

        self.assertEqual(search_report.summary_line(results), "2件中 2件一致、0件不一致")

    def test_with_mismatch(self):
        results = [result(), result("other", matched=False)]

        self.assertEqual(search_report.summary_line(results), "2件中 1件一致、1件不一致")


class StatusDescriptionTest(unittest.TestCase):
    def test_all_match_shows_summary_only(self):
        self.assertEqual(search_report.status_description([result()]), "1件中 1件一致、0件不一致")

    def test_mismatch_lists_cases(self):
        results = [result(), result("size-limit", matched=False)]

        self.assertEqual(search_report.status_description(results), "2件中 1件一致、1件不一致: size-limit")

    def test_long_description_is_truncated(self):
        results = [result(f"long-case-name-{number:03}", matched=False) for number in range(20)]

        description = search_report.status_description(results)

        self.assertEqual(len(description), search_report.STATUS_DESCRIPTION_LIMIT)
        self.assertTrue(description.endswith("…"))


class EndpointGroupingTest(unittest.TestCase):
    RESULTS = [
        result("a", endpoint="/api/v1/articles/search"),
        result("b", matched=False, endpoint="/api/v1/items/search", diff="-a"),
        result("c", endpoint="/api/v1/articles/search"),
    ]

    def test_environment_appears_once(self):
        markdown = compare_markdown(self.RESULTS)

        self.assertEqual(markdown.count("環境: docker / ホスト: localhost:9200"), 1)

    def test_overall_summary_is_shown(self):
        self.assertIn("**全体: 3件中 2件一致、1件不一致**", compare_markdown(self.RESULTS))

    def test_each_endpoint_has_heading_and_summary(self):
        markdown = compare_markdown(self.RESULTS)

        self.assertIn("### `GET /api/v1/articles/search`\n\n2件中 2件一致、0件不一致", markdown)
        self.assertIn("### `GET /api/v1/items/search`\n\n1件中 0件一致、1件不一致", markdown)

    def test_cases_stay_under_their_endpoint(self):
        markdown = compare_markdown(self.RESULTS)
        items_section = markdown.split("### `GET /api/v1/items/search`")[1]
        articles_section = markdown.split("### `GET /api/v1/items/search`")[0]

        self.assertIn("| a |", articles_section)
        self.assertIn("| c |", articles_section)
        self.assertIn("| b |", items_section)
        self.assertNotIn("| a |", items_section)

    def test_endpoints_keep_first_appearance_order(self):
        markdown = compare_markdown(self.RESULTS)

        self.assertLess(markdown.index("/api/v1/articles/search"), markdown.index("/api/v1/items/search"))

    def test_regression_groups_by_endpoint(self):
        markdown = regression_markdown(self.RESULTS)

        self.assertEqual(markdown.count("環境: docker / ホスト: localhost:9200"), 1)
        self.assertIn("### `GET /api/v1/items/search`", markdown)


class CompareMarkdownTest(unittest.TestCase):
    def test_row_uses_check_mark_for_match(self):
        markdown = compare_markdown([result()])

        self.assertIn("| single-term | `BM25` | - | 200 | 200 | 1 | 1 | ✅ |", markdown)

    def test_row_uses_cross_mark_for_mismatch(self):
        markdown = compare_markdown(
            [result("size-limit", matched=False, q="search", size=2, base_hits=6, head_hits=5, diff="-a\n+b")]
        )

        self.assertIn("| size-limit | `search` | 2 | 200 | 200 | 6 | 5 | ❌ |", markdown)

    def test_header_has_response_match_column(self):
        markdown = compare_markdown([result()])

        self.assertIn("| ヒット件数（main） | ヒット件数（PR） | レスポンス一致 |", markdown)

    def test_blank_query_is_visible(self):
        markdown = compare_markdown([result(q=" ", base_hits=None, head_hits=None)])

        self.assertIn("| `␠` | - | 200 | 200 | - | - |", markdown)

    def test_space_between_words_is_kept(self):
        markdown = compare_markdown([result(q="vector search")])

        self.assertIn("`vector search`", markdown)

    def test_pipe_does_not_break_table(self):
        markdown = compare_markdown([result(q="a|b")])

        self.assertIn("`a\\|b`", markdown)

    def test_mismatch_lists_failed_case_and_diff(self):
        markdown = compare_markdown([result("size-limit", matched=False, diff="-a\n+b")])

        self.assertIn("不一致のケース: size-limit", markdown)
        self.assertIn("<details>", markdown)
        self.assertIn("-a\n+b", markdown)

    def test_all_match_has_no_diff_section(self):
        markdown = compare_markdown([result()])

        self.assertIn("不一致のケース: なし", markdown)
        self.assertNotIn("<details>", markdown)


class RegressionMarkdownTest(unittest.TestCase):
    def test_row_shows_expected_match(self):
        markdown = regression_markdown([result(), result("other", matched=False)])

        self.assertIn("| single-term | `BM25` | - | ✅ |", markdown)
        self.assertIn("| other | `BM25` | - | ❌ |", markdown)
        self.assertIn("2件中 1件一致、1件不一致", markdown)


class FailsafeReportTest(unittest.TestCase):
    REPORT = """<testsuite name="ArticleSearchRegressionIT">
  <testcase name="testResponseMatchesGolden(RegressionCase)[1]"/>
  <testcase name="testResponseMatchesGolden(RegressionCase)[2]">
    <failure message="body.total"/>
  </testcase>
  <testcase name="testResponseMatchesGolden(RegressionCase)[3]"><error message="boom"/></testcase>
  <testcase name="testGoldenFilesHaveMatchingCases"/>
</testsuite>
"""

    def test_maps_parameterized_results_to_cases(self):
        cases = [{"name": "a", "q": "x"}, {"name": "b", "q": "y", "size": 2}, {"name": "c", "q": "z"}]
        with tempfile.TemporaryDirectory() as directory:
            report = pathlib.Path(directory) / "report.xml"
            report.write_text(self.REPORT, encoding="utf-8")

            results = search_report.read_regression_results(report, cases)

        self.assertEqual([r["name"] for r in results], ["a", "b", "c"])
        self.assertEqual(results[0]["endpoint"], search_report.DEFAULT_ENDPOINT)
        self.assertEqual([r["matched"] for r in results], [True, False, False])
        self.assertEqual(results[1]["size"], 2)

    def test_missing_case_counts_as_mismatch(self):
        cases = [{"name": "a", "q": "x"}, {"name": "b", "q": "y"}, {"name": "c", "q": "z"}, {"name": "d", "q": "w"}]
        with tempfile.TemporaryDirectory() as directory:
            report = pathlib.Path(directory) / "report.xml"
            report.write_text(self.REPORT, encoding="utf-8")

            results = search_report.read_regression_results(report, cases)

        self.assertFalse(results[3]["matched"])


class GithubOutputTest(unittest.TestCase):
    def test_writes_step_summary_and_output(self):
        with tempfile.TemporaryDirectory() as directory:
            summary_path = pathlib.Path(directory) / "summary.md"
            output_path = pathlib.Path(directory) / "output"
            env = {"GITHUB_STEP_SUMMARY": str(summary_path), "GITHUB_OUTPUT": str(output_path)}

            search_report.publish("# 表", [result(), result("other", matched=False)], env)

            self.assertIn("# 表", summary_path.read_text(encoding="utf-8"))
            self.assertEqual(
                output_path.read_text(encoding="utf-8"),
                "summary=2件中 1件一致、1件不一致: other\ncounts=2件中 1件一致、1件不一致\nmismatches=other\n",
            )

    def test_skips_files_outside_github_actions(self):
        search_report.publish("# 表", [result()], {})


if __name__ == "__main__":
    unittest.main()
