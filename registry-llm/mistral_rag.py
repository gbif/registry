import os
from langchain.vectorstores import Chroma
from langchain.embeddings import OllamaEmbeddings
from langchain.text_splitter import RecursiveCharacterTextSplitter
from langchain.document_loaders import DirectoryLoader


def process_llm_response(llm_response):
    print(llm_response['result'])
    print('\n\nSources:')
    for source in llm_response["source_documents"]:
        print(source.metadata['source'])

# Ollama embeddings
embeddings_open = OllamaEmbeddings(model="mistral")

# Print number of txt files in directory
loader = DirectoryLoader('llm/datasmall/', glob="./*.txt")

docs = loader.load()
print(len(docs))

text_splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=0)
texts = text_splitter.split_documents(docs)

print(len(texts))


# Langchain documentation
persist_directory = 'vdb_langchain_doc_small'

# Your documents
# LangChain documentation from directory
persist_directory = 'vdb_langchain_doc_small'

vectordb = Chroma.from_documents(documents=texts,
                                 embedding=embeddings_open,
                                 persist_directory=persist_directory)

vectordb.persist()
