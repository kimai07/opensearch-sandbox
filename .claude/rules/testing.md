---
paths:
  - "**/src/test/**/*.java"
  - "**/src/integration-test/**/*.java"
---

# テスト作成ルール

## DisplayName

すべての `@Test` と `@ParameterizedTest` メソッドに `@DisplayName` を付けます。

- 末尾は「であること」で説明します。
- テスト条件がある場合は、「〜な場合、」のように条件を含めます。「場合」が入っていれば、「〜の場合、」「〜した場合、」など自然な表現でかまいません。
- `@DisplayName` は `@Test` の直後に書きます。
- `@Nested` クラスはグループ分けが目的のため、`@DisplayName` は付けません。

```java
@Nested
class CreateIndex {
    @Test
    @DisplayName("未承認の場合、作成結果がfalseであること")
    void testCreateIndexNotAcknowledged() throws IOException {
        // ...
    }
}
```

| 種類 | 例 |
|---|---|
| 条件なし | `取得したクライアントがnull以外であること` |
| 状態が条件 | `メタデータがnullな場合、metadataがnullであること` |
| 操作が条件 | `複数回取得した場合、同一インスタンスであること` |
| 異常系 | `通信エラーが発生した場合、例外がIOExceptionであること` |

## テストの構成

- テストクラスは、対象クラスと同じパッケージに `<対象クラス名>Test` という名前で作成します。
- メソッド名は `test` で始め、検証内容が分かる名前にします。
- 関連するテストは、機能ごとに `@Nested` クラスでまとめます。
- 単体テストは `src/test`、OpenSearch の起動が必要な結合テストは `src/integration-test` に置きます。
- 結合テストは `<対象>IT` という名前にします。`mvn test` では実行されず、`mvn verify -Pregression` で実行されます。

## 使用ライブラリ

- テストは JUnit 6 で書きます。
- 検証には AssertJ を使います。
- `IndexClient` や `SearchClient` などの依存は Mockito でモックします。
- `api` の Controller は `@WebMvcTest`、service 以下は通常の単体テストで書きます。
