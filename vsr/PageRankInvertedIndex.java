package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.vsr.DocumentReference;
import ir.vsr.InvertedIndex;
import ir.vsr.Retrieval;
import ir.classifiers.*;

public class PageRankInvertedIndex extends InvertedIndex{
    
    // bias based on popularity
    double weight;

    // page ranks
    HashMap<String, Double> rank;

    public PageRankInvertedIndex(File dirFile, short docType, boolean stem, boolean feedback, double w) {
        super(dirFile, docType, stem, feedback);
        // initialize new variables
        weight = w;
        rank = new HashMap<String, Double>();
        File page = new File("page_ranks.txt");
        try {
            Scanner kb = new Scanner(new FileReader(page));
            while (kb.hasNextLine()) {
                String[] pageData = kb.nextLine().split(" ");
                rank.put(pageData[0], Double.parseDouble(pageData[1]));
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
    }

    /**
     * Calculate the final score for a retrieval and return a Retrieval object representing
     * the retrieval with its final score.
     *
     * @param queryLength The length of the query vector, incorporated into the final score
     * @param docRef The document reference for the document concerned
     * @param score The partially computed score 
     * @return The retrieval object for the document described by docRef
     *     and score under the query with length queryLength
     */
    protected Retrieval getRetrieval(double queryLength, DocumentReference docRef, double score) {
        // Normalize score for the lengths of the two document vectors
        score = score / (queryLength * docRef.length);
        // multiply by weight
        score += rank.get(docRef.file.getName()) * weight;
        // Add a Retrieval for this document to the result array
        return new Retrieval(docRef, score);
    }

    /**
     * Index a directory of files and then interactively accept retrieval queries.
     * Command format: "InvertedIndex [OPTION]* [DIR]" where DIR is the name of
     * the directory whose files should be indexed, and OPTIONs can be
     * "-html" to specify HTML files whose HTML tags should be removed.
     * "-stem" to specify tokens should be stemmed with Porter stemmer.
     * "-feedback" to allow relevance feedback from the user.
     */
    public static void main(String[] args) {

        // Parse the arguments into a directory name and optional flag

        String dirName = args[args.length - 1];
        short docType = DocumentIterator.TYPE_TEXT;
        boolean stem = false, feedback = false;
        double setWeight = 0;
        for (int i = 0; i < args.length - 1; i++) {
            String flag = args[i];
            if (flag.equals("-weight")) {
                setWeight = Double.parseDouble(args[++i]);
            }
            else if (flag.equals("-html"))
                // Create HTMLFileDocuments to filter HTML tags
                docType = DocumentIterator.TYPE_HTML;
            else if (flag.equals("-stem"))
                // Stem tokens with Porter stemmer
                stem = true;
            else if (flag.equals("-feedback"))
                // Use relevance feedback
                feedback = true;
            else {
                throw new IllegalArgumentException("Unknown flag: "+ flag);
            }
        }

        // make sure old ranks don't influence new ones.
        File pages = new File(dirName, "page_ranks.txt");
        if (pages.renameTo(new File(dirName, "../page_ranks.txt"))) {
            pages.delete();
        }
        // Create an inverted index for the files in the given directory.
        PageRankInvertedIndex index = new PageRankInvertedIndex(new File(dirName), docType, stem, feedback, setWeight);
        // index.print();
        // Interactively process queries to this index.
        index.processQueries();

        // bring back page_ranks
        pages = new File("page_ranks.txt");
        if (pages.renameTo(new File(dirName, "page_ranks.txt"))) {
            pages.delete();
        }
    }

}