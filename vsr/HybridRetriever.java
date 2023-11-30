package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class HybridRetriever {

    /**
     * Retrievers used for comination
     */
    public DeepRetriever retriever = null;
    public InvertedIndex index = null;


    /**
     * Constructor initializing retrievers
     * @param corpus used for DeepRetriever
     * @param dir used for InvertedIndex
     */
    public HybridRetriever(File corpus, File dir) {
        retriever = new DeepRetriever(corpus, true);
        index = new InvertedIndex(dir, DocumentIterator.TYPE_TEXT, false, false);
    }

    /**
     * Perform ranked retrieval on an input query based on lambda value, InvertedIndex, and DeepRetriever.
     */
    public Retrieval[] retrieve(String query, File deepQuery, double lambda) {
        // Sets up Retrieval arrays for DeepRetriever and InvertedIndex
        DeepDocumentReference queryDocRef = new DeepDocumentReference(deepQuery,
                retriever.dimension);
        Retrieval[] retrievals1 = retriever.retrieve(queryDocRef);
        Retrieval[] retrievals2 = index.retrieve(query);
        Retrieval[] combined = new Retrieval[retrievals1.length];
        // Searches for same document in InvertedIndex and DeepRetriever.
        for (int i = 0; i < retrievals1.length; i++) {
            double score = retrievals1[i].score * lambda;
            for (int j = 0; j < retrievals2.length; j++) {
                // Calculates new score based on lambda value
                if (retrievals1[i].docRef.file.getName().equals(retrievals2[j].docRef.file.getName())) {
                    score = (retrievals1[i].score * lambda) + ((1 - lambda) * retrievals2[j].score);
                    break;
                }
            }
            combined[i] = new Retrieval(retrievals1[i].docRef, score);
        }
        Arrays.sort(combined);
        return combined;
    }
}
