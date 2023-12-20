from llama_index import StorageContext, load_index_from_storage
import logging
import sys
import json

from llama_index.callbacks import CallbackManager, LlamaDebugHandler
from llama_index.llms import LlamaCPP
from llama_cpp import Llama
from llama_index.llms.llama_utils import messages_to_prompt, completion_to_prompt
from llama_index.indices.service_context import ServiceContext
from llama_index.indices.struct_store import JSONQueryEngine
from llama_index import GPTVectorStoreIndex, download_loader, TreeIndex, SimpleDirectoryReader, LLMPredictor, PromptHelper
import requests



logging.basicConfig(stream=sys.stdout, level=logging.INFO)  # Change INFO to DEBUG if you want more extensive logging
logging.getLogger().addHandler(logging.StreamHandler(stream=sys.stdout))

llama_debug = LlamaDebugHandler(print_trace_on_end=True)
callback_manager = CallbackManager([llama_debug])

llm = LlamaCPP(
    # optionally, you can set the path to a pre-downloaded model instead of model_url
    model_path='/Users/xrc439/dev/gbif/registry/registry-llm/llm/llama-2-13b-chat.Q5_K_M.gguf',
    temperature=0.1,
    max_new_tokens=256,
    # llama2 has a context window of 4096 tokens, but we set it lower to allow for some wiggle room
    context_window=3900,
    # kwargs to pass to __call__()
    generate_kwargs={},
    # kwargs to pass to __init__()
    # set to at least 1 to use GPU
    model_kwargs={"n_gpu_layers": 9},
    verbose=True,
)

llm_predictor = LLMPredictor(llm=llm)

prompt_helper = PromptHelper(num_output=256, chunk_size_limit=1000)

service_context = ServiceContext.from_defaults(embed_model="local", chunk_size=512, llm_predictor=llm_predictor, prompt_helper=prompt_helper)

storage_context = StorageContext.from_defaults(persist_dir="storage")
index = load_index_from_storage(storage_context, service_context=service_context)

query_engine = index.as_query_engine(service_context=service_context, streaming=True)
response = query_engine.query("What GBIF datasets are hosted by of the organization 'European Environment Agency'?")

print(response)
