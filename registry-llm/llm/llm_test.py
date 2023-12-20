from langchain.embeddings.huggingface import HuggingFaceEmbeddings
from langchain.llms import GPT4All
from llama_index.node_parser import SimpleNodeParser
from llama_index.langchain_helpers.text_splitter import TokenTextSplitter
from llama_index.embeddings.langchain  import LangchainEmbedding
from llama_index import (
    GPTVectorStoreIndex,
    LLMPredictor,
    ServiceContext,
    StorageContext,
    download_loader,
    PromptHelper
)
local_llm_path = '/Users/xrc439/dev/gbif/registry/registry-llm/llm/llama-2-13b-chat.Q5_K_M.gguf'
llm = GPT4All(model=local_llm_path, backend='gptj', streaming=True, max_tokens=512)
llm_predictor = LLMPredictor(llm=llm)

embed_model = LangchainEmbedding(HuggingFaceEmbeddings(model_name="sentence-transformers/all-mpnet-base-v2"))


prompt_helper = PromptHelper(chunk_size_limit=512, num_output=256, chunk_overlap_ratio=0.1)
service_context = ServiceContext.from_defaults(
    llm_predictor=llm_predictor,
    embed_model=embed_model,
    prompt_helper=prompt_helper,
    node_parser=SimpleNodeParser(text_splitter=TokenTextSplitter(chunk_size=300, chunk_overlap=20))
)

index = GPTVectorStoreIndex.from_documents(documents, service_context=service_context)
