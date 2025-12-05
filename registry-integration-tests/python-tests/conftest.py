"""
Pytest configuration and fixtures for ES9 endpoint tests.
"""
import os
import pytest
import requests


@pytest.fixture(scope="session")
def base_url():
    """
    Base URL for the Registry API.
    Defaults to http://localhost:8080 if REGISTRY_API_BASE_URL is not set.
    """
    return os.getenv("REGISTRY_API_BASE_URL", "http://localhost:8080")


@pytest.fixture(scope="session")
def api_base_url(base_url):
    """
    Full API base URL.
    For local testing, no /v1/ prefix is needed.
    For production/uat, use /v1/ prefix.
    """
    # Check if base_url already contains /v1
    if "/v1" in base_url:
        return base_url
    # For localhost, don't add /v1
    if "localhost" in base_url or "127.0.0.1" in base_url:
        return base_url
    # For other environments, add /v1
    return f"{base_url}/v1"


@pytest.fixture(scope="session")
def http_client():
    """
    HTTP client session for making requests.
    """
    session = requests.Session()
    session.headers.update({
        "Accept": "application/json",
        "Content-Type": "application/json"
    })
    return session


@pytest.fixture
def dataset_search_url(api_base_url):
    """URL for dataset search endpoint."""
    return f"{api_base_url}/dataset/search"


@pytest.fixture
def dataset_suggest_url(api_base_url):
    """URL for dataset suggest endpoint."""
    return f"{api_base_url}/dataset/suggest"


@pytest.fixture
def collections_search_url(api_base_url):
    """URL for collections/institutions cross-search endpoint."""
    return f"{api_base_url}/grscicoll/search"


@pytest.fixture
def institution_search_url(api_base_url):
    """URL for institution search endpoint."""
    return f"{api_base_url}/grscicoll/institution/search"


@pytest.fixture
def collection_search_url(api_base_url):
    """URL for collection search endpoint."""
    return f"{api_base_url}/grscicoll/collection/search"

