'''
Python code to load a pre-trained specter2 model and use it to generate embeddings for all documents and queries in the corpus.
The document and query embeddings should use appropriate adapters on top of the base model.
Model embedding dims: 768
'''

from transformers import AutoAdapterModel, AutoTokenizer
import torch
import os
import numpy

class EmbeddingGenerator:

    def __init__(self, args, device="cpu"):
        """Load pre-trained model and tokenizer from models/specter2_base
        """
        self.model = AutoAdapterModel.from_pretrained("/u/mooney/ir-code/models/specter2_2023/base")
        self.tokenizer = AutoTokenizer.from_pretrained("/u/mooney/ir-code/models/specter2_2023/base")
        #Load adapters
        if args.use_adapter:
            self.model.load_adapter("/u/mooney/ir-code/models/specter2_2023/adhoc_query", set_active=False, load_as="query")
            self.model.load_adapter("/u/mooney/ir-code/models/specter2_2023/proximity", set_active=False, load_as="prox")
        self.model.to(device)
        self.device = device
        self.adapter = None
        self.args = args

    def get_embedding(self, text):
        """Tokenize input text and embed it using the model
        Return embedding
        """
        inputs = self.tokenizer(text, return_tensors="pt", max_length=512).to(self.device)
        #Use adapters
        if self.adapter:
            self.model.set_active_adapters(self.adapter)
        #Get embedding
        with torch.no_grad():
            outputs = self.model(**inputs)
        return outputs.last_hidden_state[:,0,:].cpu().numpy().reshape(-1)
    
    def embed_docs(self):
        """Generate embeddings for all documents in the corpus
        Load adapter from models/specter2_proximity if necessary
        """
        #Use correct adapter
        if self.args.use_adapter:
            self.adapter = "prox"
        for i in range(1, 1240):
            name = f"RN-0{i:04}"
            #Embed document
            with open(os.path.join(self.args.docs_folder, name), 'r') as docs:
                path = os.path.join(self.args.output_folder, "docs", name)
                os.makedirs(os.path.dirname(path), exist_ok=True)
                numpy.savetxt(path, [self.get_embedding(docs.read())], fmt='%f')
        self.adapter = None

    def embed_queries(self):
        """Generate embeddings for all queries in the corpus
        Load adapter from models/specter2_adhoc_query if necessary
        """
        #Use correct adapter
        if self.args.use_adapter:
            self.adapter = "query"
        with open(self.args.queries_file, 'r') as queries:
            query = queries.readlines()
            #Embed query
            for i in range(0, len(query), 3):
                path = os.path.join(self.args.output_folder, "queries", f"Q{i//3 + 1:03}")
                os.makedirs(os.path.dirname(path), exist_ok=True)
                numpy.savetxt(path, [self.get_embedding(query[i].strip())], fmt='%f')
        self.adapter = None