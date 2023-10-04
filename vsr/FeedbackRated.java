package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.vsr.Feedback;
import ir.vsr.HashMapVector;
import ir.vsr.InvertedIndex;
import ir.vsr.Retrieval;

public class FeedbackRated extends Feedback {

    public HashMap<DocumentReference, Double> relavant;
    public HashMap<DocumentReference, Double> irrelavant;

    /**
    * Create a feedback object for this query with initial retrievals to be rated
    */
    public FeedbackRated(HashMapVector queryVector, Retrieval[] retrievals, InvertedIndex invertedIndex) {
        super(queryVector, retrievals, invertedIndex);
        relavant = new HashMap<>();
        irrelavant = new HashMap<>();
    }

    /**
    * Add a document to the list of those deemed relevant
    */
    public void addGood(DocumentReference docRef, double rank) {
        relavant.put(docRef, rank);
    }

    /**
    * Add a document to the list of those deemed irrelevant
    */
    public void addBad(DocumentReference docRef, double rank) {
        irrelavant.put(docRef, rank);
    }

    /**
    * Has the user rated any documents yet?
    */
    public boolean isEmpty() {
        if (relavant.isEmpty() && irrelavant.isEmpty())
            return true;
        else
            return false;
    }

    /**
    * Prompt the user for feedback on this numbered retrieval
    */
    public void getFeedback(int showNumber) {
        // Get the docRef for this document (remember showNumber starts at 1 and is 1 greater than array index)
        DocumentReference docRef = retrievals[showNumber - 1].docRef;
        String response = UserInput.prompt("Is document #" + showNumber + ":" + docRef.file.getName() +
            " relevant (enter a number between -1 and 1 where -1: very irrelevant, 0: unsure, +1: very relevant)?: ");
        double relevance = Double.parseDouble(response);
        if (relevance >= 0)
            addGood(docRef, relevance);
        else
            addBad(docRef, relevance);
    }

    /**
    * Has the user already provided feedback on this numbered retrieval?
    */
    public boolean haveFeedback(int showNumber) {
        // Get the docRef for this document (remember showNumber starts at 1 and is 1 greater than array index)
        DocumentReference docRef = retrievals[showNumber - 1].docRef;
        if (goodDocRefs.contains(docRef) || badDocRefs.contains(docRef))
            return true;
        else
            return false;
    }

    /**
    * Use the Ide_regular algorithm to compute a new revised query.
    *
    * @return The revised query vector.
    */
    public HashMapVector newQuery() {
        // Start the query as a copy of the original
        HashMapVector newQuery = queryVector.copy();
        // Normalize query by maximum token frequency and multiply by alpha
        newQuery.multiply(ALPHA / newQuery.maxWeight());
        // Add in the vector for each of the positively rated documents
        for (DocumentReference docRef : relavant.keySet()) {
            // Get the document vector for this positive document
            Document doc = docRef.getDocument(invertedIndex.docType, invertedIndex.stem);
            HashMapVector vector = doc.hashMapVector();
            // Multiply positive docs by beta and normalize by max token frequency
            vector.multiply(relavant.get(docRef) * (BETA / vector.maxWeight()));
            // Add it to the new query vector
            newQuery.add(vector);
        }
        // Subtract the vector for each of the negatively rated documents
        for (DocumentReference docRef : irrelavant.keySet()) {
            // Get the document vector for this negative document
            Document doc = docRef.getDocument(invertedIndex.docType, invertedIndex.stem);
            HashMapVector vector = doc.hashMapVector();
            // Multiply negative docs by beta and normalize by max token frequency
            vector.multiply(Math.abs(irrelavant.get(docRef) * (GAMMA / vector.maxWeight())));
            // Subtract it from the new query vector
            newQuery.subtract(vector);
        }
        return newQuery;
    }

}
