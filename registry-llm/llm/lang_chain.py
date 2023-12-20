from langchain.document_loaders import JSONLoader
import json
from pathlib import Path
from pprint import pprint

file_path ='q.json'
data = json.loads(Path(file_path).read_text())
loader = JSONLoader(
    file_path='q.json',
    jq_schema='.datasets[]',
    text_content=False)

data = loader.load()

pprint(data)
