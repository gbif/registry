import json
from pathlib import Path

import os

# Open the file for writing

# Call the function with the example JSON object and file handle
for json_file in os.listdir("crawl2"):
    data = json.loads(Path(os.path.join("crawl2", json_file)).read_text())
    file_path = "datasets.jsonl"
    with open(file_path, 'w') as file:
        for index, dataset in enumerate(data["results"]):
            file.write(json.dumps(dataset, separators=(',', ':')) + "\n")
