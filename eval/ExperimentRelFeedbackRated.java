package ir.eval;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.eval.ExperimentRated;
import ir.utilities.*;
import ir.vsr.*;

public class ExperimentRelFeedbackRated extends ExperimentRated{
    
    //number of documents
    public int numberOfDocs;

    public boolean control;

    public boolean binary;

    public boolean stem;

    /**
   * Constructor that just calls the Experiment constructor
   */
  public ExperimentRelFeedbackRated(File corpusDir, File queryFile, File outFile, short docType, 
          boolean stem, int docs, boolean isControl, boolean isBinary) throws IOException {
      super(corpusDir, queryFile, outFile, docType, stem);
      this.index = new InvertedIndexRated(corpusDir, docType, stem, false);
      numberOfDocs = docs;
      this.stem = stem;
      control = isControl;
      binary = isBinary;
  }

/**
   * Process the next query read from the query file reader and evaluate
   * results compared to known relevant docs also read from the query file.
   * This version computes NDCG results for each query, storing summed 
   * results in NDCGvalues
   *
   * @return true if query successfully read, else false if no more queries
   * in query file
   */
  boolean processQuery(BufferedReader in) throws IOException {
    String query = in.readLine();   // get the query
    if (query == null) return false;  // return false if end of file
    System.out.println("\nQuery " + (rpResults.size() + 1) + ": " + query);

    // Process the query and get the ranked retrievals
    ArrayList<Retrieval> retrievals = new ArrayList<>(Arrays.asList(index.retrieve(query)));
    System.out.println("Returned " + retrievals.size() + " documents.");

    HashMapVector queryVector = (new TextStringDocument(query, stem)).hashMapVector();
    FeedbackRated fdback = new FeedbackRated(queryVector, retrievals.toArray(new Retrieval[retrievals.size()]), index);

    // Get the correct retrievals
    ArrayList<String> correctRetrievals = new ArrayList<String>();
    getCorrectRatedRetrievals(in, correctRetrievals);

    for (int i = 0; i < numberOfDocs; i++) {
      if (correctRetrievals.contains(retrievals.get(i).docRef.file.getName())) {
        if (binary) {
          fdback.relavant.put(retrievals.get(i).docRef, 1.0);
        } else {
          fdback.relavant.put(retrievals.get(i).docRef, ratingsMap.get(retrievals.get(i).docRef.file.getName()));
        }
      } else {
          fdback.irrelavant.put(retrievals.get(i).docRef, -1.0);
      }
    }

    for (DocumentReference doc : fdback.relavant.keySet()) {
      String name = doc.file.getName();
      if (correctRetrievals.contains(name)) {
        correctRetrievals.remove(name);
      }
    }

    if (!control) {
      queryVector = fdback.newQuery();
      retrievals = new ArrayList<>(Arrays.asList(index.retrieve(queryVector)));
    }

    for (DocumentReference doc : fdback.relavant.keySet()) {
      String name = doc.file.getName();
      for (Retrieval ret : retrievals) {
        if (name.equals(ret.docRef.file.getName())) {
          retrievals.remove(ret);
          break;
        }
      }
    }

    for (DocumentReference doc : fdback.irrelavant.keySet()) {
      String name = doc.file.getName();
      for (Retrieval ret : retrievals) {
        if (name.equals(ret.docRef.file.getName())) {
          retrievals.remove(ret);
          break;
        }
      }
    }

    // Generate Recall/Precision points and save in rpResults
    rpResults.add(evalRetrievals(retrievals.toArray(new Retrieval[retrievals.size()]), correctRetrievals));

    // Update the NDCG values for this query
    UpdateNDCG(retrievals.toArray(new Retrieval[retrievals.size()]), correctRetrievals);    

    // Read the blank line delimiter between queries in the query file
    String line = in.readLine();
    if (!(line == null || line.trim().equals(""))) {
      System.out.println("\nCould not find blank line after query, bad queryFile format");
      System.exit(1);
    }
    return true;
  }

  /**
   * Evaluate retrieval performance on a given query test corpus and
   * generate a recall/precision graph and table of NDCG results.
   * Command format: "Experiment [OPTION]* [DIR] [QUERIES] [OUTFILE]" where:
   * DIR is the name of the directory whose files should be indexed.
   * QUERIES is a file of queries paired with relevant docs 
   * and continuous gold-standard relevance ratings (see queryFile).
   * OUTFILE is the name of the file to put the output. The plot
   * data for the recall precision curve is stored in this file and a
   * gnuplot file for the graph is the same name with a ".gplot" extension
   * and a NDCG result file is the same name with a ".ndcg" extension
   * OPTIONs can be
   * "-html" to specify HTML files whose HTML tags should be removed, and
   * "-stem" to specify tokens should be stemmed with Porter stemmer.
   */
  public static void main(String[] args) throws IOException {
    // Parse the arguments into a directory name and optional flag
    String corpusDir = args[args.length - 4];
    String queryFile = args[args.length - 3];
    String outFile = args[args.length - 2];
    int numSimulatedFeedback = Integer.parseInt(args[args.length - 1]);
    short docType = DocumentIterator.TYPE_TEXT;
    boolean stem = false;
    boolean binary = false;
    boolean control = false;
    for (int i = 0; i < args.length - 4; i++) {
      String flag = args[i];
      if (flag.equals("-html")) {
        docType = DocumentIterator.TYPE_HTML;
      } else if (flag.equals("-stem")) {
        stem = true;
      } else if (flag.equals("-binary"))
        binary = true;
      else if (flag.equals("-control"))
        control = true;
      else {
        throw new IllegalArgumentException("Unknown flag: " + flag);
      }
    }
    ExperimentRelFeedbackRated exper = new ExperimentRelFeedbackRated(new File(corpusDir), new File(queryFile),
        new File(outFile), docType, stem, numSimulatedFeedback, control, binary);
    // Generate a recall precision curve and NDCG results for this dataset
    // makeRpCurve must be first since it calculates the statistics for both
    exper.makeRpCurve();
    exper.makeNDCGtable();
  }

}
