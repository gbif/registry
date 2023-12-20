import json
from pathlib import Path
import html2text
from markdown import markdown
from string import Template
import os


from string import Formatter

class EmptyNoneType(object):

    def __nonzero__(self):
        return False

    def __str__(self):
        return ''

    def __getattr__(self, name):
        return EmptyNone

    def __getitem__(self, idx):
        return EmptyNone

EmptyNone = EmptyNoneType()

class EmptyNoneFormatter(Formatter):

    def get_value(self, field_name, args, kwds):
        v = Formatter.get_value(self, field_name, args, kwds)
        if v is None:
            return EmptyNone
        return v

EmptyNone = EmptyNoneType()


def remove_html(input_string):
    # Use html2text to convert HTML to plain text
    return html2text.html2text(input_string)

def remove_markdown(input_string):
    # Use markdown library to convert Markdown to plain text
    return markdown(input_string)

def remove_html_and_markdown(input_string):
    if input_string is None:
        return None

    # Remove Markdown syntax
    text_without_markdown = remove_markdown(input_string)

    # Remove HTML tags
    text_without_html = remove_html(text_without_markdown)

    return text_without_html


with open("prompt_template.txt") as f:
    template = f.read()

# Open the file for writing

# Call the function with the example JSON object and file handle
for json_file in os.listdir("crawl2"):
    print(f"processing file {json_file}")
    data = json.loads(Path(os.path.join("crawl2", json_file)).read_text())
    for index, item in enumerate(data["results"]):
        file_path = "data/" + item["key"] + ".txt"
        with open(file_path, 'w') as file:
            fmt = EmptyNoneFormatter()
            file.write(fmt.format(template, **item))
