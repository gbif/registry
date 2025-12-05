# ES9 Python Test Suite

This directory contains Python integration tests for verifying Elasticsearch 9.0.4 upgrade compatibility with all search endpoints in the GBIF Registry.

## Overview

These tests verify that all Elasticsearch-powered search endpoints continue to work correctly after the ES9 upgrade. The tests make real HTTP requests to the API and validate response structures and formats.

## Tested Endpoints

1. `GET /dataset/search` - Dataset search (faceted search)
2. `GET /dataset/suggest` - Dataset suggest/autocomplete
3. `GET /grscicoll/search` - Collections and institutions cross-search
4. `GET /grscicoll/institution/search` - Institution search
5. `GET /grscicoll/collection/search` - Collection search

**Note:** For local testing, endpoints are accessed without `/v1/` prefix. For production/UAT environments, the `/v1/` prefix is automatically added.

## Prerequisites

- Python 3.7 or higher
- pip (Python package manager)

## Installation

1. Install the required dependencies:

```bash
pip install -r requirements.txt
```

Or using a virtual environment (recommended):

```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Configuration

The test suite uses the `REGISTRY_API_BASE_URL` environment variable to determine the base URL of the API. If not set, it defaults to `http://localhost:8080`.

### Setting the Base URL

**Linux/macOS:**
```bash
export REGISTRY_API_BASE_URL=http://localhost:8080
pytest
```

**Windows:**
```cmd
set REGISTRY_API_BASE_URL=http://localhost:8080
pytest
```

**For different environments:**
```bash
export REGISTRY_API_BASE_URL=https://api.gbif-uat.org
pytest
```

## Running Tests

### Run all tests

```bash
pytest
```

### Run specific test file

```bash
pytest test_dataset_search.py
pytest test_collections_search.py
```

### Run specific test class

```bash
pytest test_dataset_search.py::TestDatasetSearch
pytest test_collections_search.py::TestCollectionsCrossSearch
```

### Run specific test method

```bash
pytest test_dataset_search.py::TestDatasetSearch::test_basic_search
```

### Run with verbose output

```bash
pytest -v
```

### Run with detailed output

```bash
pytest -vv
```

### Run and show print statements

```bash
pytest -s
```

## Test Structure

- `conftest.py` - Pytest fixtures for base URL, HTTP client, and endpoint URLs
- `test_dataset_search.py` - Tests for dataset search and suggest endpoints
- `test_collections_search.py` - Tests for collections search endpoints

## Test Coverage

Each endpoint is tested for:

- Basic search functionality
- Empty query handling
- Pagination (offset, limit)
- Facets (for faceted search endpoints)
- Highlight parameter
- Filter parameters (endpoint-specific)
- Response structure validation
- ES9 response format compatibility

## Troubleshooting

### Connection Errors

If you get connection errors, make sure:

1. The Registry API server is running
2. The `REGISTRY_API_BASE_URL` is correctly set
3. The server is accessible from your machine
4. No firewall is blocking the connection

### Test Failures

If tests fail:

1. Check that the API server is running and accessible
2. For local testing, endpoints should NOT include `/v1/` prefix
3. For production/UAT, the `/v1/` prefix is automatically added
4. Ensure Elasticsearch 9.0.4 is properly configured
5. Check that test data exists in the database/Elasticsearch index
6. If you get 403 errors, check if authentication is required (some endpoints may require API keys)

### Import Errors

If you get import errors:

1. Make sure you've installed dependencies: `pip install -r requirements.txt`
2. Verify you're using the correct Python version (3.7+)
3. Check that you're in the correct directory

## Notes

- These are integration tests that require a running API server
- Tests make real HTTP requests and may take some time to complete
- Some tests may fail if the test environment doesn't have sufficient data
- The tests are designed to be flexible and handle various response formats

