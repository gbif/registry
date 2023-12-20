import os
from langchain.vectorstores import Chroma
from langchain.llms import Ollama
from langchain_experimental.llms.ollama_functions import OllamaFunctions
from langchain.callbacks.manager import CallbackManager
from langchain.callbacks.streaming_stdout import StreamingStdOutCallbackHandler
from langchain.chains import RetrievalQA
from langchain.embeddings import OllamaEmbeddings
from langchain.prompts import PromptTemplate
from langchain.chat_models import ChatOllama
from transformers import (
    AutoTokenizer,
    AutoModelForCausalLM,
    BitsAndBytesConfig,
    pipeline
)
from langchain.chat_models import ChatOllama

def process_llm_response(llm_response):
    print(llm_response['result'])
    print('\n\nSources:')
    for source in llm_response["source_documents"]:
        print(source.metadata['source'])

# Langchain documentation
persist_directory = 'vdb_langchain_doc_small'

# Your documents
# LangChain documentation from directory
persist_directory = 'vdb_langchain_doc_small'

embeddings_open = OllamaEmbeddings(model="mistral")

vectordb = Chroma(embedding_function=embeddings_open,
                  persist_directory=persist_directory)


retriever = vectordb.as_retriever()

llm_open = Ollama(model="mistral",
                  callback_manager=CallbackManager([StreamingStdOutCallbackHandler()]))

ollama_llm = "mistral"
model = ChatOllama(model=ollama_llm)

prompt_template = """
### [INST]
Instruction: Answer the question based on your
fantasy football knowledge. Here is context to help:

{context}

### QUESTION:
{question}

[/INST]
 """
# Create prompt from prompt template
prompt = PromptTemplate(
    input_variables=["context", "question"],
    template=prompt_template,
)

# Create llm chain
llm_chain = LLMChain(llm=mistral_llm, prompt=prompt)

qa_chain = RetrievalQA.from_chain_type(llm=llm_open,
                                       chain_type="stuff",
                                       retriever=retriever,
                                       return_source_documents=True,
                                       verbose=True)

rag_chain = (
        {"context": retriever, "question": RunnablePassthrough()}
        | llm_chain
)


# RAG chain
chain = (
        RunnableParallel({"context": retriever, "question": RunnablePassthrough()})
        | prompt
        | model
        | StrOutputParser()
)

# Add typing for input
class Question(BaseModel):
    __root__: str

chain = chain.with_types(input_type=Question)

# Question
query = "What is the key of dataset Liste des publications par taxon TOGO?"
llm_response = rag_chain.query(query)
process_llm_response(llm_response)
