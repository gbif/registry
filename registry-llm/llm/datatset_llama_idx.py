import logging
import sys
import json

from llama_index.callbacks import CallbackManager, LlamaDebugHandler
from llama_index.llms import LlamaCPP
from llama_cpp import Llama
from llama_index.llms.llama_utils import messages_to_prompt, completion_to_prompt
from llama_index.indices.service_context import ServiceContext
from llama_index.indices.struct_store import JSONQueryEngine

logging.basicConfig(stream=sys.stdout, level=logging.INFO)  # Change INFO to DEBUG if you want more extensive logging
logging.getLogger().addHandler(logging.StreamHandler(stream=sys.stdout))

llama_debug = LlamaDebugHandler(print_trace_on_end=True)
callback_manager = CallbackManager([llama_debug])

llm = Llama(
    # optionally, you can set the path to a pre-downloaded model instead of model_url
    model_path='/Users/xrc439/dev/gbif/registry/registry-llm/llm/llama-2-13b-chat.Q5_K_M.gguf',

    # kwargs to pass to __call__()
    generate_kwargs={},

    # kwargs to pass to __init__()
    # set to at least 1 to use GPU
    model_kwargs={"n_gpu_layers": 4}, # I need to play with this and see if it actually helps

    # transform inputs into Llama2 format
    messages_to_prompt=messages_to_prompt,
    completion_to_prompt=completion_to_prompt,
    verbose=True,
)

# Test on some sample data
json_value = {
    "blogPosts": [
        {
            "id": 1,
            "title": "First blog post",
            "content": "This is my first blog post"
        },
        {
            "id": 2,
            "title": "Second blog post",
            "content": "This is my second blog post"
        }
    ],
    "comments": [
        {
            "id": 1,
            "content": "Nice post!",
            "username": "jerry",
            "blogPostId": 1
        },
        {
            "id": 2,
            "content": "Interesting thoughts",
            "username": "simon",
            "blogPostId": 2
        },
        {
            "id": 3,
            "content": "Loved reading this!",
            "username": "simon",
            "blogPostId": 2
        }
    ]
}

# JSON Schema object that the above JSON value conforms to
json_schema = {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "description": "Schema for a very simple blog post app",
    "type": "object",
    "properties": {
        "blogPosts": {
            "description": "List of blog posts",
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "id": {
                        "description": "Unique identifier for the blog post",
                        "type": "integer"
                    },
                    "title": {
                        "description": "Title of the blog post",
                        "type": "string"
                    },
                    "content": {
                        "description": "Content of the blog post",
                        "type": "string"
                    }
                },
                "required": ["id", "title", "content"]
            }
        },
        "comments": {
            "description": "List of comments on blog posts",
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "id": {
                        "description": "Unique identifier for the comment",
                        "type": "integer"
                    },
                    "content": {
                        "description": "Content of the comment",
                        "type": "string"
                    },
                    "username": {
                        "description": "Username of the commenter (lowercased)",
                        "type": "string"
                    },
                    "blogPostId": {
                        "description": "Identifier for the blog post to which the comment belongs",
                        "type": "integer"
                    }
                },
                "required": ["id", "content", "username", "blogPostId"]
            }
        }
    },
    "required": ["blogPosts", "comments"]
}


service_context = ServiceContext.from_defaults(llm=llm, embed_model="local")

raw_query_engine = JSONQueryEngine(
    json_value=json_value,
    json_schema=json_schema,
    service_context=service_context,
    synthesize_response=False,
)
response = nl_query_engine.query("What comments has Jerry been writing?",)
print(response)

response = raw_query_engine.query("What comments has Jerry been writing?",)
print(response)
