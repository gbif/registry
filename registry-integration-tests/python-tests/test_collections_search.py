"""
Tests for Collections Search endpoints (GrSciColl).
These tests verify ES9 compatibility after upgrade.
"""
import pytest


class TestCollectionsCrossSearch:
    """Test suite for GET /grscicoll/search endpoint (collections and institutions cross-search)."""

    def test_basic_search(self, http_client, collections_search_url):
        """Test basic cross-search with query parameter."""
        params = {"q": "museum"}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        # Results should be a list of collections/institutions
        if data:
            assert isinstance(data[0], dict)

    def test_empty_query(self, http_client, collections_search_url):
        """Test cross-search without query parameter."""
        response = http_client.get(collections_search_url)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_highlight(self, http_client, collections_search_url):
        """Test cross-search with highlight parameter."""
        params = {"q": "museum", "hl": "true"}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_entity_type_filter(self, http_client, collections_search_url):
        """Test filtering by entity type."""
        params = {"entityType": "INSTITUTION"}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_country_filter(self, http_client, collections_search_url):
        """Test filtering by country."""
        params = {"country": "DK"}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)

    def test_pagination(self, http_client, collections_search_url):
        """Test pagination with limit parameter."""
        params = {"limit": 10}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) <= 10

    def test_combined_params(self, http_client, collections_search_url):
        """Test search with multiple parameters combined."""
        params = {
            "q": "museum",
            "hl": "true",
            "entityType": "COLLECTION",
            "country": "DK",
            "limit": 5
        }
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) <= 5

    def test_response_structure(self, http_client, collections_search_url):
        """Test that response structure matches expected format."""
        params = {"limit": 1}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        
        if data:
            result = data[0]
            assert isinstance(result, dict)
            # Should have key or code or name
            assert "key" in result or "code" in result or "name" in result

    def test_es9_response_format(self, http_client, collections_search_url):
        """Test ES9 response format compatibility."""
        params = {"q": "test"}
        response = http_client.get(collections_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)


class TestInstitutionSearch:
    """Test suite for GET /grscicoll/institution/search endpoint."""

    def test_basic_search(self, http_client, institution_search_url):
        """Test basic institution search with query parameter."""
        params = {"q": "museum"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data
        # Response can be PagingResponse or FacetedSearchResponse
        if "results" in data:
            assert isinstance(data["results"], list)

    def test_empty_query(self, http_client, institution_search_url):
        """Test institution search without query parameter."""
        response = http_client.get(institution_search_url)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_highlight(self, http_client, institution_search_url):
        """Test institution search with highlight parameter."""
        params = {"hl": "true", "q": "museum"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_pagination(self, http_client, institution_search_url):
        """Test pagination with offset and limit."""
        params = {"offset": 0, "limit": 10}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        if "results" in data:
            assert len(data["results"]) <= 10

    def test_facets(self, http_client, institution_search_url):
        """Test faceted institution search."""
        params = {"facet": "country"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        # Facets may be in response
        assert "count" in data or "facets" in data or "results" in data

    def test_type_filter(self, http_client, institution_search_url):
        """Test filtering by institution type."""
        params = {"type": "HERBARIUM"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_country_filter(self, http_client, institution_search_url):
        """Test filtering by country."""
        params = {"country": "DK"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_combined_filters(self, http_client, institution_search_url):
        """Test institution search with multiple filters."""
        params = {
            "q": "museum",
            "type": "HERBARIUM",
            "country": "DK",
            "limit": 5
        }
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        if "results" in data:
            assert len(data["results"]) <= 5

    def test_response_structure(self, http_client, institution_search_url):
        """Test that response structure matches expected format."""
        params = {"limit": 1}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        
        # Should have count or results
        assert "count" in data or "results" in data
        
        if "results" in data and data["results"]:
            result = data["results"][0]
            assert isinstance(result, dict)
            assert "key" in result or "code" in result or "name" in result

    def test_es9_response_format(self, http_client, institution_search_url):
        """Test ES9 response format compatibility."""
        params = {"q": "test"}
        response = http_client.get(institution_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)


class TestCollectionSearch:
    """Test suite for GET /grscicoll/collection/search endpoint."""

    def test_basic_search(self, http_client, collection_search_url):
        """Test basic collection search with query parameter."""
        params = {"q": "herbarium"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data
        if "results" in data:
            assert isinstance(data["results"], list)

    def test_empty_query(self, http_client, collection_search_url):
        """Test collection search without query parameter."""
        response = http_client.get(collection_search_url)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_highlight(self, http_client, collection_search_url):
        """Test collection search with highlight parameter."""
        params = {"hl": "true", "q": "herbarium"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_pagination(self, http_client, collection_search_url):
        """Test pagination with offset and limit."""
        params = {"offset": 0, "limit": 10}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        if "results" in data:
            assert len(data["results"]) <= 10

    def test_facets(self, http_client, collection_search_url):
        """Test faceted collection search."""
        params = {"facet": "country"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        # Facets may be in response
        assert "count" in data or "facets" in data or "results" in data

    def test_content_type_filter(self, http_client, collection_search_url):
        """Test filtering by content type."""
        params = {"contentType": "PRESERVED_SPECIMEN"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_institution_key_filter(self, http_client, collection_search_url):
        """Test filtering by institution key."""
        # institutionKey must be a valid UUID format
        # Using an invalid key should return 400 (bad request)
        # Using a valid UUID format (even if non-existent) should return 200
        import uuid
        # Test with invalid format - should return 400
        params_invalid = {"institutionKey": "test-key"}
        response_invalid = http_client.get(collection_search_url, params=params_invalid)
        
        # Invalid UUID format should return 400
        if response_invalid.status_code == 400:
            # This is expected behavior for invalid UUID format
            return
        
        # If it doesn't return 400, try with valid UUID format
        valid_uuid = str(uuid.uuid4())
        params_valid = {"institutionKey": valid_uuid}
        response_valid = http_client.get(collection_search_url, params=params_valid)
        
        # Should return 200 even if no results found (valid UUID format)
        assert response_valid.status_code == 200
        data = response_valid.json()
        assert "count" in data or "results" in data

    def test_country_filter(self, http_client, collection_search_url):
        """Test filtering by country."""
        params = {"country": "DK"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert "count" in data or "results" in data

    def test_combined_filters(self, http_client, collection_search_url):
        """Test collection search with multiple filters."""
        params = {
            "q": "herbarium",
            "contentType": "PRESERVED_SPECIMEN",
            "country": "DK",
            "limit": 5
        }
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        if "results" in data:
            assert len(data["results"]) <= 5

    def test_response_structure(self, http_client, collection_search_url):
        """Test that response structure matches expected format."""
        params = {"limit": 1}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        
        # Should have count or results
        assert "count" in data or "results" in data
        
        if "results" in data and data["results"]:
            result = data["results"][0]
            assert isinstance(result, dict)
            assert "key" in result or "code" in result or "name" in result

    def test_es9_response_format(self, http_client, collection_search_url):
        """Test ES9 response format compatibility."""
        params = {"q": "test"}
        response = http_client.get(collection_search_url, params=params)
        
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, dict)

