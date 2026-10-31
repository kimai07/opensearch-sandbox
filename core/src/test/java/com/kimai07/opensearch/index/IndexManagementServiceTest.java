package com.kimai07.opensearch.index;

import com.kimai07.opensearch.client.IndexClient;
import com.kimai07.opensearch.config.OpenSearchConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.indices.GetIndicesSettingsResponse;
import org.opensearch.client.opensearch.indices.GetMappingResponse;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IndexManagementServiceTest {

    private IndexClient mockClient;
    private IndexManagementService service;

    @BeforeEach
    void setUp() {
        mockClient = mock(IndexClient.class);
        OpenSearchConfig config = new OpenSearchConfig.Builder().numberOfShards(1).numberOfReplicas(0).build();

        service = new IndexManagementService(mockClient, config);
    }

    @Nested
    class CreateIndex {
        @Test
        @DisplayName("マッピングなしの場合、マッピングなしで作成され結果がtrueであること")
        void testCreateIndexWithoutMappings() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(true);

            boolean result = service.createIndex("test-index");

            assertThat(result).isTrue();
            verify(mockClient).createIndex("test-index", 1, 0, null, false);
        }

        @Test
        @DisplayName("マッピングありの場合、指定マッピングで作成され結果がtrueであること")
        void testCreateIndexWithMappings() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(true);

            Map<String, Property> properties = Map.of("title", Property.of(p -> p.text(t -> t)), "content",
                    Property.of(p -> p.text(t -> t)));

            boolean result = service.createIndex("test-index", properties);

            assertThat(result).isTrue();
            verify(mockClient).createIndex("test-index", 1, 0, properties, false);
        }

        @Test
        @DisplayName("k-NNが有効な場合、k-NN有効で作成され結果がtrueであること")
        void testCreateIndexWithKnnEnabled() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(true);

            Map<String, Property> properties = Map.of("vector", Property.of(p -> p.knnVector(kv -> kv.dimension(128))));

            boolean result = service.createIndex("test-index", properties, true);

            assertThat(result).isTrue();
            verify(mockClient).createIndex("test-index", 1, 0, properties, true);
        }

        @Test
        @DisplayName("マッピングが空な場合、作成結果がtrueであること")
        void testCreateIndexWithEmptyMappings() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(true);

            boolean result = service.createIndex("test-index", Map.of());

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("未承認の場合、作成結果がfalseであること")
        void testCreateIndexNotAcknowledged() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean())).thenReturn(false);

            boolean result = service.createIndex("test-index");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("通信エラーが発生した場合、例外がIOExceptionであること")
        void testCreateIndexThrowsIOException() throws IOException {
            when(mockClient.createIndex(anyString(), anyInt(), anyInt(), any(), anyBoolean()))
                    .thenThrow(new IOException("Index creation failed"));

            assertThatThrownBy(() -> service.createIndex("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Index creation failed");
        }
    }

    @Nested
    class DeleteIndex {
        @Test
        @DisplayName("承認済みの場合、削除結果がtrueであること")
        void testDeleteIndexSuccess() throws IOException {
            when(mockClient.deleteIndex(anyString())).thenReturn(true);

            boolean result = service.deleteIndex("test-index");

            assertThat(result).isTrue();
            verify(mockClient).deleteIndex("test-index");
        }

        @Test
        @DisplayName("未承認の場合、削除結果がfalseであること")
        void testDeleteIndexNotAcknowledged() throws IOException {
            when(mockClient.deleteIndex(anyString())).thenReturn(false);

            boolean result = service.deleteIndex("test-index");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("通信エラーが発生した場合、例外がIOExceptionであること")
        void testDeleteIndexThrowsIOException() throws IOException {
            when(mockClient.deleteIndex(anyString())).thenThrow(new IOException("Index deletion failed"));

            assertThatThrownBy(() -> service.deleteIndex("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Index deletion failed");
        }
    }

    @Nested
    class IndexExists {
        @Test
        @DisplayName("インデックスが存在する場合、確認結果がtrueであること")
        void testIndexExistsReturnsTrue() throws IOException {
            when(mockClient.indexExists(anyString())).thenReturn(true);

            boolean result = service.indexExists("existing-index");

            assertThat(result).isTrue();
            verify(mockClient).indexExists("existing-index");
        }

        @Test
        @DisplayName("インデックスが存在しない場合、確認結果がfalseであること")
        void testIndexExistsReturnsFalse() throws IOException {
            when(mockClient.indexExists(anyString())).thenReturn(false);

            boolean result = service.indexExists("non-existing-index");

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("通信エラーが発生した場合、例外がIOExceptionであること")
        void testIndexExistsThrowsIOException() throws IOException {
            when(mockClient.indexExists(anyString())).thenThrow(new IOException("Check failed"));

            assertThatThrownBy(() -> service.indexExists("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Check failed");
        }
    }

    @Nested
    class PutMapping {
        @Test
        @DisplayName("承認済みの場合、マッピング更新結果がtrueであること")
        void testPutMappingSuccess() throws IOException {
            when(mockClient.putMapping(anyString(), any())).thenReturn(true);

            Map<String, Property> properties = Map.of("newField", Property.of(p -> p.text(t -> t)));

            boolean result = service.putMapping("test-index", properties);

            assertThat(result).isTrue();
            verify(mockClient).putMapping("test-index", properties);
        }

        @Test
        @DisplayName("未承認の場合、マッピング更新結果がfalseであること")
        void testPutMappingNotAcknowledged() throws IOException {
            when(mockClient.putMapping(anyString(), any())).thenReturn(false);

            Map<String, Property> properties = Map.of("newField", Property.of(p -> p.text(t -> t)));

            boolean result = service.putMapping("test-index", properties);

            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("通信エラーが発生した場合、例外がIOExceptionであること")
        void testPutMappingThrowsIOException() throws IOException {
            when(mockClient.putMapping(anyString(), any())).thenThrow(new IOException("Mapping update failed"));

            Map<String, Property> properties = Map.of("newField", Property.of(p -> p.text(t -> t)));

            assertThatThrownBy(() -> service.putMapping("test-index", properties)).isInstanceOf(IOException.class)
                    .hasMessage("Mapping update failed");
        }
    }

    @Nested
    class IndexTemplate {
        @Test
        @DisplayName("承認済みの場合、テンプレート作成結果がtrueであること")
        void testPutIndexTemplateSuccess() throws IOException {
            when(mockClient.putIndexTemplate(anyString(), anyString(), anyInt(), anyInt(), any())).thenReturn(true);

            Map<String, Property> properties = Map.of("message", Property.of(p -> p.text(t -> t)));

            boolean result = service.putIndexTemplate("logs-template", "logs-*", properties);

            assertThat(result).isTrue();
            verify(mockClient).putIndexTemplate("logs-template", "logs-*", 1, 0, properties);
        }

        @Test
        @DisplayName("承認済みの場合、テンプレート削除結果がtrueであること")
        void testDeleteIndexTemplateSuccess() throws IOException {
            when(mockClient.deleteIndexTemplate(anyString())).thenReturn(true);

            boolean result = service.deleteIndexTemplate("logs-template");

            assertThat(result).isTrue();
            verify(mockClient).deleteIndexTemplate("logs-template");
        }

        @Test
        @DisplayName("テンプレート作成で通信エラーが発生した場合、例外がIOExceptionであること")
        void testPutIndexTemplateThrowsIOException() throws IOException {
            when(mockClient.putIndexTemplate(anyString(), anyString(), anyInt(), anyInt(), any()))
                    .thenThrow(new IOException("Template creation failed"));

            Map<String, Property> properties = Map.of("message", Property.of(p -> p.text(t -> t)));

            assertThatThrownBy(() -> service.putIndexTemplate("logs-template", "logs-*", properties))
                    .isInstanceOf(IOException.class).hasMessage("Template creation failed");
        }

        @Test
        @DisplayName("テンプレート削除で通信エラーが発生した場合、例外がIOExceptionであること")
        void testDeleteIndexTemplateThrowsIOException() throws IOException {
            when(mockClient.deleteIndexTemplate(anyString())).thenThrow(new IOException("Template deletion failed"));

            assertThatThrownBy(() -> service.deleteIndexTemplate("logs-template")).isInstanceOf(IOException.class)
                    .hasMessage("Template deletion failed");
        }
    }

    @Nested
    class RefreshIndex {
        @Test
        @DisplayName("正常な場合、リフレッシュ結果がtrueであること")
        void testRefreshIndexSuccess() throws IOException {
            boolean result = service.refreshIndex("test-index");

            assertThat(result).isTrue();
            verify(mockClient).refreshIndex("test-index");
        }

        @Test
        @DisplayName("通信エラーが発生した場合、例外がIOExceptionであること")
        void testRefreshIndexThrowsIOException() throws IOException {
            doThrow(new IOException("Refresh failed")).when(mockClient).refreshIndex(anyString());

            assertThatThrownBy(() -> service.refreshIndex("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Refresh failed");
        }
    }

    @Nested
    class GetIndexInfo {
        @Test
        @DisplayName("正常な場合、取得結果がクライアントの設定レスポンスであること")
        void testGetIndexSettingsSuccess() throws IOException {
            GetIndicesSettingsResponse mockResponse = mock(GetIndicesSettingsResponse.class);
            when(mockClient.getIndexSettings(anyString())).thenReturn(mockResponse);

            GetIndicesSettingsResponse result = service.getIndexSettings("test-index");

            assertThat(result).isEqualTo(mockResponse);
            verify(mockClient).getIndexSettings("test-index");
        }

        @Test
        @DisplayName("設定取得で通信エラーが発生した場合、例外がIOExceptionであること")
        void testGetIndexSettingsThrowsIOException() throws IOException {
            when(mockClient.getIndexSettings(anyString())).thenThrow(new IOException("Get settings failed"));

            assertThatThrownBy(() -> service.getIndexSettings("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Get settings failed");
        }

        @Test
        @DisplayName("正常な場合、取得結果がクライアントのマッピングレスポンスであること")
        void testGetIndexMappingSuccess() throws IOException {
            GetMappingResponse mockResponse = mock(GetMappingResponse.class);
            when(mockClient.getIndexMapping(anyString())).thenReturn(mockResponse);

            GetMappingResponse result = service.getIndexMapping("test-index");

            assertThat(result).isEqualTo(mockResponse);
            verify(mockClient).getIndexMapping("test-index");
        }

        @Test
        @DisplayName("マッピング取得で通信エラーが発生した場合、例外がIOExceptionであること")
        void testGetIndexMappingThrowsIOException() throws IOException {
            when(mockClient.getIndexMapping(anyString())).thenThrow(new IOException("Get mapping failed"));

            assertThatThrownBy(() -> service.getIndexMapping("test-index")).isInstanceOf(IOException.class)
                    .hasMessage("Get mapping failed");
        }
    }

    @Nested
    class VectorDocumentRecord {
        @Test
        @DisplayName("全項目を指定した場合、各項目が指定値であること")
        void testVectorDocumentCreation() {
            float[] vector = { 0.1f, 0.2f, 0.3f };
            Map<String, Object> metadata = Map.of("title", "Test Document");

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("id1", vector,
                    metadata);

            assertThat(doc.id()).isEqualTo("id1");
            assertThat(doc.vector()).isEqualTo(vector);
            assertThat(doc.metadata()).containsEntry("title", "Test Document");
        }

        @Test
        @DisplayName("メタデータがnullな場合、metadataがnullであること")
        void testVectorDocumentWithNullMetadata() {
            float[] vector = { 0.5f, 0.6f };

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("id2", vector, null);

            assertThat(doc.id()).isEqualTo("id2");
            assertThat(doc.vector()).containsExactly(0.5f, 0.6f);
            assertThat(doc.metadata()).isNull();
        }

        @Test
        @DisplayName("ベクトルが空な場合、vectorが空であること")
        void testVectorDocumentWithEmptyVector() {
            float[] emptyVector = {};
            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("id1", emptyVector,
                    null);

            assertThat(doc.vector()).isEmpty();
        }

        @Test
        @DisplayName("メタデータが空な場合、metadataが空であること")
        void testVectorDocumentWithEmptyMetadata() {
            float[] vector = { 0.1f };
            Map<String, Object> emptyMetadata = Map.of();

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("id1", vector,
                    emptyMetadata);

            assertThat(doc.metadata()).isEmpty();
        }

        @Test
        @DisplayName("IDがnullな場合、idがnullであること")
        void testVectorDocumentWithNullId() {
            float[] vector = { 0.1f, 0.2f };

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument(null, vector, null);

            assertThat(doc.id()).isNull();
        }

        @Test
        @DisplayName("1024次元のベクトルの場合、全要素が保持された状態であること")
        void testVectorDocumentWithLargeVector() {
            float[] largeVector = new float[1024];
            for (int i = 0; i < largeVector.length; i++) {
                largeVector[i] = i * 0.001f;
            }

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("large", largeVector,
                    null);

            assertThat(doc.vector()).hasSize(1024);
            assertThat(doc.vector()[0]).isEqualTo(0.0f);
            assertThat(doc.vector()[1023]).isCloseTo(1.023f, org.assertj.core.api.Assertions.within(0.001f));
        }

        @Test
        @DisplayName("特殊な浮動小数点値の場合、値がそのまま保持された状態であること")
        void testVectorDocumentWithSpecialFloatValues() {
            float[] specialVector = { Float.MAX_VALUE, Float.MIN_VALUE, 0.0f, -0.0f };

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("special",
                    specialVector, null);

            assertThat(doc.vector()).containsExactly(Float.MAX_VALUE, Float.MIN_VALUE, 0.0f, -0.0f);
        }

        @Test
        @DisplayName("ベクトルが別インスタンスな場合、ドキュメントが非等価であること")
        void testVectorDocumentEquality() {
            float[] vector1 = { 0.1f, 0.2f };
            float[] vector2 = { 0.1f, 0.2f };
            Map<String, Object> metadata = Map.of("key", "value");

            IndexManagementService.VectorDocument doc1 = new IndexManagementService.VectorDocument("id1", vector1,
                    metadata);
            IndexManagementService.VectorDocument doc2 = new IndexManagementService.VectorDocument("id1", vector2,
                    metadata);

            // Recordのequalsは配列の参照を比較するため、異なるインスタンスは等しくない
            assertThat(doc1).isNotEqualTo(doc2);
            assertThat(doc1.id()).isEqualTo(doc2.id());
        }

        @Test
        @DisplayName("ベクトルが同一インスタンスな場合、ドキュメントが等価であること")
        void testVectorDocumentSameInstance() {
            float[] vector = { 0.1f, 0.2f };
            Map<String, Object> metadata = Map.of("key", "value");

            IndexManagementService.VectorDocument doc1 = new IndexManagementService.VectorDocument("id1", vector,
                    metadata);
            IndexManagementService.VectorDocument doc2 = new IndexManagementService.VectorDocument("id1", vector,
                    metadata);

            // 同じ配列インスタンスを使用した場合は等しい
            assertThat(doc1).isEqualTo(doc2);
        }

        @Test
        @DisplayName("複数の型を含むメタデータの場合、全エントリが保持された状態であること")
        void testVectorDocumentWithComplexMetadata() {
            float[] vector = { 0.1f };
            Map<String, Object> metadata = Map.of("title", "Test", "count", 42, "score", 0.95, "active", true);

            IndexManagementService.VectorDocument doc = new IndexManagementService.VectorDocument("id1", vector,
                    metadata);

            assertThat(doc.metadata()).containsEntry("title", "Test").containsEntry("count", 42)
                    .containsEntry("score", 0.95).containsEntry("active", true);
        }
    }
}
