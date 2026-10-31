"""pr_comment のテスト。python3 -m unittest discover -s .github/scripts で実行する。"""
import unittest
from unittest import mock

import pr_comment

RUN_URL = "https://github.com/owner/repo/actions/runs/1"


class RenderCommentTest(unittest.TestCase):
    def render(self, counts="9件中 9件一致、0件不一致", mismatches=""):
        return pr_comment.render_comment(counts, mismatches, RUN_URL, "f18cb1ae0e78")

    def test_starts_with_marker(self):
        self.assertTrue(self.render().startswith(pr_comment.MARKER))

    def test_all_match(self):
        body = self.render()

        self.assertIn("✅ **search-compare: 9件中 9件一致、0件不一致**", body)
        self.assertNotIn("不一致のケース", body)
        self.assertIn(f"詳細: [実行結果]({RUN_URL})", body)
        self.assertIn("比較したコミット: `f18cb1a`", body)

    def test_mismatch_lists_cases(self):
        body = self.render("9件中 7件一致、2件不一致", "size-limit,single-term")

        self.assertIn("❌ **search-compare: 9件中 7件一致、2件不一致**", body)
        self.assertIn("不一致のケース:\n- size-limit\n- single-term", body)

    def test_not_compared(self):
        body = self.render(counts="")

        self.assertIn("⚠️ **search-compare: 比較を実行できませんでした**", body)
        self.assertNotIn("比較したコミット", body)
        self.assertIn(f"詳細: [実行結果]({RUN_URL})", body)


class UpsertCommentTest(unittest.TestCase):
    def test_updates_existing_comment(self):
        calls = []

        def run(args):
            calls.append(args)
            return "123\n" if "--paginate" in args else ""

        pr_comment.upsert_comment("owner/repo", "8", "body", run)

        self.assertEqual(
            calls[-1],
            ["gh", "api", "--method", "PATCH", "repos/owner/repo/issues/comments/123", "-f", "body=body"],
        )

    def test_creates_comment_when_missing(self):
        calls = []

        def run(args):
            calls.append(args)
            return ""

        pr_comment.upsert_comment("owner/repo", "8", "body", run)

        self.assertEqual(
            calls[-1],
            ["gh", "api", "--method", "POST", "repos/owner/repo/issues/8/comments", "-f", "body=body"],
        )

    def test_updates_only_first_comment_when_duplicated(self):
        run = mock.Mock(side_effect=["123\n456\n", ""])

        pr_comment.upsert_comment("owner/repo", "8", "body", run)

        self.assertEqual(run.call_count, 2)
        self.assertIn("repos/owner/repo/issues/comments/123", run.call_args.args[0])


if __name__ == "__main__":
    unittest.main()
