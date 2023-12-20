import requests
import json


def get_dataset_query(limit=10, offset=0):
    return """{\"query\": \"{datasetSearch(limit: %d, offset: %d) { results { key decades description doi hostingOrganizationKey hostingOrganizationTitle keywords license logoUrl publishingCountry publishingOrganizationKey publishingOrganizationTitle recordCount title type subtype occurrenceCount literatureCount publishingOrganization { title description abbreviation endorsingNode { title }} hostingOrganization { title description abbreviation endorsingNode { title }}}}}\"}""" % (limit, offset)


def get_datasets(url, limit=10, offset=0):
    headers = {
        'Content-Type': 'application/json'
    }

    # GraphQL query with variables for limit and offset
    dataset_query = get_dataset_query(limit, offset)

    response = requests.post(url, headers=headers, data=dataset_query)

    if response.status_code == 200:
        return response.json()
    else:
        print(f"Error {response.status_code}: {response.text}")
        return None


def crawl_graphql_api(url, outdir, limit=10):
    offset = 0

    error_file_path = outdir + "errors.txt"
    while True:
        print(f"Crawling page {offset}, limit {limit}\n")
        response_data = get_datasets(url, limit, offset)

        if not response_data:

            with open(error_file_path, 'w') as fp:
                print(f"Error crawling page {offset}, limit {limit}\n")
                fp.write(f"limit {limit} offset {offset}\n")
            continue

        # Assuming the actual data is nested under 'data' key in the response
        data = response_data.get('data', {}).get('datasetSearch', [])

        if not data:
            break
        file_path = outdir + str(offset) + ".json"
        with open(file_path, 'w') as fp:
            fp.write(json.dumps(data))
        # If the number of items in the response is less than the limit, we have reached the end
        if len(data["results"]) < limit:
            break

        offset += limit


# Example usage
graphql_url = 'https://graphql.gbif.org/graphql'

crawl_graphql_api(graphql_url,"crawl2/", 10)
