# Running the test

## Dataset Update Load Test
This Locust load test script performs update operations on a GBIF dataset using the GBIF Registry API. It updates the dataset title with a unique identifier to simulate real-world update scenarios.

## Prerequisites

- Python 3.x
- Locust installed (`pip install locust`): https://docs.locust.io/en/stable/installation.html

## Using the config file
locust -f [dataset_update.py](dataset_update.py) --host=https://api.gbif-dev.org

See example config file template: [config.yaml](config.yaml)

## Using environment variables (overrides config file)
export GBIF_BASE_URL=https://api.gbif.org
export GBIF_USERNAME=your_registry_username
export GBIF_PASSWORD=your_registry_password
export GBIF_DATASET_KEY=11111111-2222-3333-4444-555555555555

locust -f [dataset_update.py](dataset_update.py) --host=https://api.gbif-dev.org

## Important operational notes

 - This endpoint performs real updates. Use a test dataset or coordinate with GBIF before running load tests.
 - Start with low load (e.g., 1 user, wait_time=5000ms) to verify everything is working as expected.
 - For higher loads, consider:
   - Increasing wait_time
   - Using a fixed title suffix instead of UUIDs to reduce write amplification
 - Avoid updating structural fields (contacts, endpoints, etc.) unless explicitly testing them.
