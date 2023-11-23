import requests


def get_dataset_query(limit=10, offset=0):
    return """
    query: {
              datasetSearch(limit: %d, offset: %d) {
                results {
                    key
                    decades
                    description
                    doi
                    hostingOrganizationKey
                    hostingOrganizationTitle
                    keywords
                    license
                    logoUrl
                    publishingCountry
                    publishingOrganizationKey
                    publishingOrganizationTitle
                    recordCount
                    title
                    type
                    subtype
                    occurrenceCount
                    literatureCount
                    publishingOrganization {
                      title
                      description
                      abbreviation
                      endorsingNode {
                        title
                      }
                    }
                    hostingOrganization {
                      title
                      description
                      abbreviation
                      endorsingNode {
                        title
                      }
                   }
                 }
              }
            }
    """ % (limit, offset)

def get_datasets(url, limit=10, offset=0):
    headers = {
        'Content-Type': 'application/json',
    }

    # GraphQL query with variables for limit and offset
    dataset_query = get_dataset_query(limit, offset)

    response = requests.post(url, headers=headers, json=dataset_query, verify=False)

    if response.status_code == 200:
        return response.json()
    else:
        print(f"Error {response.status_code}: {response.text}")
        return None

def crawl_graphql_api(url, limit=10):
    offset = 0
    all_responses = []

    while True:
        response_data = get_datasets(url, limit, offset)

        if not response_data:
            break

        # Assuming the actual data is nested under 'data' key in the response
        data = response_data.get('data', {}).get('datasetSearch', [])

        if not data:
            break

        all_responses.extend(data)

        # If the number of items in the response is less than the limit, we have reached the end
        if len(data) < limit:
            break

        offset += limit

    return all_responses

# Example usage
graphql_url = 'https://graphql.gbif.org/graphql'

result = crawl_graphql_api(graphql_url)

# Do something with the result
print(result)
