'''
Call generate_embeddings.py
Run as:
    python test_embedder.py
    python test_embedder.py --use-adapter
'''

import torch
from generate_embeddings import EmbeddingGenerator
import argparse

parser = argparse.ArgumentParser()
parser.add_argument("--docs-folder", default="/u/mooney/ir-code/corpora/cf", help="Path to documents folder")
parser.add_argument("--queries-file", default="/u/mooney/ir-code/queries/cf/queries", help="Path to queries file")
parser.add_argument("--models-folder", default="/u/mooney/ir-code/models/specter2_2023", help="Path to model folder")
parser.add_argument("--use-adapter", action="store_true", help="Use adapter for specter2")
args = parser.parse_args()

if args.use_adapter:
    args.output_folder = "embeddings/specter2_adapter"
else:
    args.output_folder = "embeddings/specter2_base"

device = torch.device("cpu")


if __name__ == "__main__":
    embedder = EmbeddingGenerator(args, device)
    embedder.embed_docs()
    embedder.embed_queries()