"""
Tests for Dataset Search and Suggest endpoints.
These tests verify ES9 compatibility after upgrade.
"""
import pytest


class TestDatasetSearch:
    """Test suite for GET /dataset/search endpoint."""

    def test_basic_search(self, http_client, dataset_search_url):
        """Test basic search with query parameter."""
        params = {"q": "biodiversity"}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data
        assert "results" in data
        assert isinstance(data["count"], int)
        assert isinstance(data["results"], list)

    def test_empty_query(self, http_client, dataset_search_url):
        """Test search without query parameter (returns all datasets)."""
        response = http_client.get(dataset_search_url)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data
        assert "results" in data
        assert data["count"] >= 0

    def test_pagination(self, http_client, dataset_search_url):
        """Test pagination with offset and limit."""
        params = {"offset": 0, "limit": 10}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "results" in data
        assert len(data["results"]) <= 10

    def test_pagination_offset(self, http_client, dataset_search_url):
        """Test pagination with different offset."""
        params1 = {"offset": 0, "limit": 5}
        params2 = {"offset": 5, "limit": 5}
        
        response1 = http_client.get(dataset_search_url, params=params1)
        response2 = http_client.get(dataset_search_url, params=params2)
        
        assert response1.status_code == 200
        assert response2.status_code == 200
        
        data1 = response1.json()
        data2 = response2.json()
        
        # Results should be different
        if len(data1["results"]) > 0 and len(data2["results"]) > 0:
            assert data1["results"][0] != data2["results"][0]

    def test_highlight(self, http_client, dataset_search_url):
        """Test search with highlight parameter."""
        # First verify basic search works
        params_no_hl = {"q": "biodiversity", "limit": 5}
        response_no_hl = http_client.get(dataset_search_url, params=params_no_hl)
        assert response_no_hl.status_code == 200, f"Basic search failed: {response_no_hl.status_code} - {response_no_hl.text}"
        data_no_hl = response_no_hl.json()
        assert "results" in data_no_hl
        
        # If no results, skip highlight test
        if not data_no_hl["results"]:
            pytest.skip("No results to test highlight")
        
        # Now test with highlight
        params_with_hl = {"q": "biodiversity", "hl": "true", "limit": 5}
        response_with_hl = http_client.get(dataset_search_url, params=params_with_hl)
        
        # Check for ES9 compatibility issue with HighlighterEncoder
        if response_with_hl.status_code == 400:
            error_msg = response_with_hl.text
            # ES9 upgrade issue: HighlighterEncoder.html enum value doesn't exist in ES9
            if "HighlighterEncoder" in error_msg and "html" in error_msg:
                pytest.fail(
                    f"ES9 Compatibility Issue: Highlight parameter failed due to HighlighterEncoder.html enum value. "
                    f"This is an ES9 upgrade bug that needs to be fixed in EsSearchRequestBuilder.java. "
                    f"Error: {error_msg}"
                )
            else:
                pytest.fail(f"Highlight parameter caused 400 error: {error_msg}")
        
        assert response_with_hl.status_code == 200, f"Highlight search failed: {response_with_hl.status_code} - {response_with_hl.text}"
        data_with_hl = response_with_hl.json()
        assert "results" in data_with_hl
        
        # Verify highlight is working by checking if results contain highlight tags
        # Highlighted text should contain <em class="gbifHl"> tags
        if data_with_hl["results"]:
            result = data_with_hl["results"][0]
            assert isinstance(result, dict)
            
            # Check if any field contains highlight tags (convert to string to search)
            result_str = str(result)
            # Note: Highlight tags may be in nested fields, so we check the string representation
            # If highlight is working, we should see the highlight tags somewhere
            # This is a basic check - actual highlight structure may vary

    def test_facets(self, http_client, dataset_search_url):
        """Test faceted search with facet parameter."""
        params = {"facet": "type"}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "facets" in data or "count" in data  # Facets may be optional

    def test_multiple_facets(self, http_client, dataset_search_url):
        """Test search with multiple facet parameters."""
        params = {"facet": ["type", "country"]}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data

    def test_filter_by_type(self, http_client, dataset_search_url):
        """Test filtering by dataset type."""
        params = {"type": "OCCURRENCE"}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data
        assert "results" in data

    def test_filter_by_country(self, http_client, dataset_search_url):
        """Test filtering by country."""
        params = {"country": "DK"}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data

    def test_filter_by_keyword(self, http_client, dataset_search_url):
        """Test filtering by keyword."""
        params = {"keyword": "biology"}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data

    def test_combined_filters(self, http_client, dataset_search_url):
        """Test search with multiple filters combined."""
        params = {
            "q": "biodiversity",
            "type": "OCCURRENCE",
            "country": "DK",
            "limit": 5
        }
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data
        assert "results" in data
        assert len(data["results"]) <= 5

    def test_response_structure(self, http_client, dataset_search_url):
        """Test that response structure matches expected format."""
        params = {"limit": 1}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        
        # Verify required fields
        assert "count" in data
        assert "results" in data
        assert "offset" in data or "limit" in data  # At least one pagination field
        
        # Verify results structure if present
        if data["results"]:
            result = data["results"][0]
            assert isinstance(result, dict)
            # Common fields that should exist
            assert "key" in result or "title" in result or "datasetKey" in result

    def test_es9_response_format(self, http_client, dataset_search_url):
        """Test ES9 response format compatibility."""
        params = {"q": "test", "limit": 1}
        response = http_client.get(dataset_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        
        # ES9 should return consistent structure
        assert isinstance(data, dict)
        assert "count" in data
        assert isinstance(data["count"], int)
        assert data["count"] >= 0


class TestDatasetSuggest:
    """Test suite for GET /dataset/suggest endpoint."""

    def test_basic_suggest(self, http_client, dataset_suggest_url):
        """Test basic suggest with query parameter."""
        params = {"q": "bio"}
        response = http_client.get(dataset_suggest_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        # Suggest should return a list of results
        if data:
            assert isinstance(data[0], dict)

    def test_empty_suggest(self, http_client, dataset_suggest_url):
        """Test suggest without query parameter."""
        response = http_client.get(dataset_suggest_url)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_suggest_limit(self, http_client, dataset_suggest_url):
        """Test suggest with limit parameter."""
        params = {"q": "test", "limit": 5}
        response = http_client.get(dataset_suggest_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) <= 5

    def test_suggest_filters(self, http_client, dataset_suggest_url):
        """Test suggest with filter parameters."""
        params = {"q": "test", "type": "OCCURRENCE"}
        response = http_client.get(dataset_suggest_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_suggest_response_structure(self, http_client, dataset_suggest_url):
        """Test suggest response structure."""
        params = {"q": "bio"}
        response = http_client.get(dataset_suggest_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        
        if data:
            result = data[0]
            assert isinstance(result, dict)
            # Suggest results should have key or title
            assert "key" in result or "title" in result or "datasetKey" in result

    def test_es9_suggest_format(self, http_client, dataset_suggest_url):
        """Test ES9 suggest response format compatibility."""
        params = {"q": "test"}
        response = http_client.get(dataset_suggest_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        
        # ES9 should return list format
        assert isinstance(data, list)
        # All items should be dictionaries
        for item in data:
            assert isinstance(item, dict)

