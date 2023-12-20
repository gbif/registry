# Import the necessary libraries
import torch
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
import transformers
from datasets import load_dataset
from datetime import datetime

def generate_and_tokenize_prompt(prompt):
    return tokenizer(formatting_func(prompt))

def tokenize(prompt):
    result = tokenizer(
        prompt,
        truncation=True,
        max_length=512,
        padding="max_length",
    )
    result["labels"] = result["input_ids"].copy()
    return result


def formatting_func(dataset):
    text = f"### The following is a description of GBIF Dataset: {dataset}"
    return text


# Define custom quantization configuration for BitsAndBytes (BNB) quantization
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,                    # Load the model with 4-bit quantization
    bnb_4bit_use_double_quant=True,       # Use double quantization for 4-bit weights
    bnb_4bit_quant_type="nf4",           # Use nf4 quantization method
    bnb_4bit_compute_dtype=torch.bfloat16 # Compute with 4-bit quantized weights in bfloat16 data type
)

# Specify the pre-trained model identifier
model_id = "mistralai/Mistral-7B-v0.1"

torch.cuda.is_available = lambda : False
# Load the pre-trained model with the specified quantization configuration
model = AutoModelForCausalLM.from_pretrained(model_id,  device_map='mps')

# Load the tokenizer for the same pre-trained model and add an end-of-sequence token
tokenizer = AutoTokenizer.from_pretrained(model_id, model_max_length=512,
                                          padding_side="left",
                                          add_eos_token=True)

tokenizer.pad_token = tokenizer.eos_token

datasets = load_dataset('json', data_files='datasets.jsonl')['train'].train_test_split(0.1)
tokenized_train_dataset = datasets['train'].map(generate_and_tokenize_prompt)
tokenized_val_dataset = datasets['test'].map(generate_and_tokenize_prompt)


project = "gbif-datasets-finetune"
base_model_name = "mistral"
run_name = base_model_name + "-" + project
output_dir = "./" + run_name

model.is_parallelizable = False
model.model_parallel = False

trainer = transformers.Trainer(
    model=model,
    train_dataset=tokenized_train_dataset,
    eval_dataset=tokenized_val_dataset,
    args=transformers.TrainingArguments(
        output_dir=output_dir,
        warmup_steps=1,
        per_device_train_batch_size=2,
        gradient_accumulation_steps=1,
        gradient_checkpointing=True,
        max_steps=500,
        learning_rate=2.5e-5, # Want a small lr for finetuning
        bf16=False,
        optim="paged_adamw_8bit",
        logging_steps=25,              # When to start reporting loss
        logging_dir="./logs",        # Directory for storing logs
        save_strategy="steps",       # Save the model checkpoint every logging step
        save_steps=25,                # Save checkpoints every 50 steps
        evaluation_strategy="steps", # Evaluate the model every logging step
        eval_steps=25,               # Evaluate and save checkpoints every 50 steps
        do_eval=True,                # Perform evaluation at the end of training
        run_name=f"{run_name}-{datetime.now().strftime('%Y-%m-%d-%H-%M')}"          # Name of the W&B run (optional)
    ),
    data_collator=transformers.DataCollatorForLanguageModeling(tokenizer, mlm=False),
)

model.config.use_cache = False  # silence the warnings. Please re-enable for inference!
trainer.train()

