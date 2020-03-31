# GBIF Registry Occurrence Client

This module contains only WS client `OccurrenceMetricsClient` which counts number of occurrences for the dataset.
It uses [OpenFeign client](https://github.com/OpenFeign/feign).

Consider moving `OccurrenceMetricsClient` out of the project.

# Configuration

 * `occurrence.ws.url` occurrence API URL (e.g. http://api.gbif-dev.org/v1/occurrence/)

[Parent](../README.md)
