import requests
from llama_index import GPTVectorStoreIndex, download_loader
data = requests.get("https://api.gbif.org/v1/dataset?type=Occurrence").json()

JsonDataReader = download_loader("JsonDataReader")
loader = JsonDataReader()
documents = loader.load_data(data)
index = GPTVectorStoreIndex.from_documents(documents)
index.query("What description has the dataset Bird collection (TSZ-bird) The Arctic University Museum of Norway")
