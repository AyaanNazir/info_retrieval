package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

/**
 * An inverted index for vector-space information retrieval. Contains methods
 * for creating an inverted index from a set of documents and retrieving ranked
 * matches to queries using standard TF/IDF weighting and cosine similarity.
 *
 * @author Ray Mooney
 */
public class InvertedProxIndex extends InvertedIndex {

  /**
   * Create an inverted index of the documents in a directory.
   *
   * @param dirFile  The directory of files to index.
   * @param docType  The type of documents to index (See docType in DocumentIterator)
   * @param stem     Whether tokens should be stemmed with Porter stemmer.
   * @param feedback Whether relevance feedback should be used.
   */
  public InvertedProxIndex(File dirFile, short docType, boolean stem, boolean feedback) {
    super(dirFile, docType, stem, feedback);
  }

  /**
   * Create an inverted index of the documents in a List of Example objects of documents
   * for text categorization.
   *
   * @param examples A List containing the Example objects for text categorization to index
   */
  public InvertedProxIndex(List<Example> examples) {
    super(examples);
  }

  /**
   * Index the documents in dirFile.
   */
  protected void indexDocuments() {
    if (!tokenHash.isEmpty() || !docRefs.isEmpty()) {
      // Currently can only index one set of documents when an index is created
      throw new IllegalStateException("Cannot indexDocuments more than once in the same InvertedIndex");
    }
    // Get an iterator for the documents
    DocumentIterator docIter = new DocumentIterator(dirFile, docType, stem);
    System.out.println("Indexing documents in " + dirFile);
    // Loop, processing each of the documents

    while (docIter.hasMoreDocuments()) {
      FileDocument doc = docIter.nextDocument();
      // Create a document vector for this document
      // System.out.print(doc.file.getName() + ",");
      HashMapVector vector = doc.hashMapVector();
      allPositons.put(doc.file.getName(), vector);
      indexDocument(doc, vector);
    }
    // Now that all documents have been processed, we can calculate the IDF weights for
    // all tokens and the resulting lengths of all weighted document vectors.
    computeIDFandDocumentLengths();
    System.out.println("\nIndexed " + docRefs.size() + " documents with " + size() + " unique terms.");
  }

  /**
   * Perform ranked retrieval on this input query Document vector.
   */
  public Retrieval[] retrieve(HashMapVector vector) {
    // Create a hashtable to store the retrieved documents. Keys
    // are docRefs and values are DoubleValues which indicate the
    // partial score accumulated for this document so far.
    // As each token in the query is processed, each document
    // it indexes is added to this hashtable and its retrieval
    // score (similarity to the query) is appropriately updated.
    Map<DocumentReference, DoubleValue> retrievalHash = new HashMap<DocumentReference, DoubleValue>();
    // Initialize a variable to store the length of the query vector
    double queryLength = 0.0;
    // Iterate through each token in the query input Document
    for (Map.Entry<String, Weight> entry : vector.entrySet()) {
      String token = entry.getKey();
      double count = entry.getValue().getValue();
      // Determine the score added to the similarity of each document
      // indexed under this token and update the length of the
      // query vector with the square of the weight for this token.
      queryLength = queryLength + incorporateToken(token, count, retrievalHash);
    }
    // Finalize the length of the query vector by taking the square-root of the
    // final sum of squares of its token weights.
    queryLength = Math.sqrt(queryLength);
    // Make an array to store the final ranked Retrievals.
    Retrieval[] retrievals = new Retrieval[retrievalHash.size()];
    // Iterate through each of the retrieved documents stored in
    // the final retrievalHash.
    int retrievalCount = 0;
    for (Map.Entry<DocumentReference, DoubleValue> entry : retrievalHash.entrySet()) {
      DocumentReference docRef = entry.getKey();
      double score = entry.getValue().value;
      retrievals[retrievalCount++] = (Retrieval) getRetrieval(queryLength, docRef, score);
    }
    for (Retrieval ret : retrievals) {
      double proximity = 0;
      int count = 0;
      FileDocument doc = docType == 0 ? new TextFileDocument(ret.docRef.file, true) : new HTMLFileDocument(ret.docRef.file, true);
      for (Map.Entry<String, Weight> entry1 : vector.entrySet()) {
        for (Map.Entry<String, Weight> entry2 : vector.entrySet()) {
          String word1 = entry1.getKey();
          // System.out.println(allPositons);
          // System.out.println(allPositons.get(doc.file.getName()));
          // System.out.println(allPositons.get(doc.file.getName()).positions);
          // System.out.println(allPositons.get(doc.file.getName()).positions.get(word1));
          List<Integer> positions1 = allPositons.get(doc.file.getName()).positions.get(word1);
          String word2 = entry2.getKey();
          List<Integer> positions2 = allPositons.get(doc.file.getName()).positions.get(word2);
          if (!word1.equals(word2)) {
            if (positions1 != null && positions2 != null) {
              for (int i = 0; i < positions1.size(); i++) {
                for (int j = 0; j < positions2.size(); j++) {
                  count++;
                  proximity += Math.abs(positions1.get(i) - positions2.get(j));
                }
              }
            } else {
              proximity += 1000;
              count++;
            }
          }
        }
      }
      ret.prox = count == 0 ? 0 : proximity / count / 1000;
    }
    // Sort the retrievals to produce a final ranked list using the
    // Comparator for retrievals that produces a best to worst ordering.
    Arrays.sort(retrievals);
    return retrievals;
  }

  /**
   * Print out at most MAX_RETRIEVALS ranked retrievals starting at given starting rank number.
   * Include the rank number and the score.
   */
  public void printRetrievals(Retrieval[] retrievals, int start) {
    System.out.println("");
    if (start >= retrievals.length)
      System.out.println("No more retrievals.");
    for (int i = start; i < Math.min(retrievals.length, start + MAX_RETRIEVALS); i++) {
      System.out.println(MoreString.padTo((i + 1) + ". ", 4) +
          MoreString.padTo(retrievals[i].docRef.file.getName(), 20) +
          " Score: " +
          MoreMath.roundTo(retrievals[i].score / retrievals[i].prox, 5) + 
          " (Vector: " + + MoreMath.roundTo(retrievals[i].score, 5) + 
          "; Proximity: " + MoreMath.roundTo(retrievals[i].prox, 5) + ");");
    }
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
    for (int i = 0; i < args.length - 1; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
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


    // Create an inverted index for the files in the given directory.
    InvertedProxIndex index = new InvertedProxIndex(new File(dirName), docType, stem, feedback);
    // index.print();
    // Interactively process queries to this index.
    index.processQueries();
  }

}
