package ir.vsr;

import java.io.*;
import java.util.*;
import java.lang.*;

import ir.utilities.*;
import ir.classifiers.*;

public class InvertedIndexRated extends InvertedIndex {

  public HashMapVector queryHashMapVector;
    
    /**
   * Create an inverted index of the documents in a directory.
   *
   * @param dirFile  The directory of files to index.
   * @param docType  The type of documents to index (See docType in DocumentIterator)
   * @param stem     Whether tokens should be stemmed with Porter stemmer.
   * @param feedback Whether relevance feedback should be used.
   */
  public InvertedIndexRated(File dirFile, short docType, boolean stem, boolean feedback) {
    super(dirFile, docType, stem, feedback);
  }

  /**
   * Create an inverted index of the documents in a List of Example objects of documents
   * for text categorization.
   *
   * @param examples A List containing the Example objects for text categorization to index
   */
  public InvertedIndexRated(List<Example> examples) {
    super(examples);
  }

  /**
   * Print out a ranked set of retrievals. Show the file name and score for
   * the top retrieved documents in order. Then allow user to see more or display
   * individual documents.
   */
  public void presentRetrievals(HashMapVector queryVector, Retrieval[] retrievals) {
    if (showRetrievals(retrievals)) {
      // Data structure for saving info about any user feedback for relevance feedback
      FeedbackRated fdback = null;
      if (feedback)
        fdback = new FeedbackRated(queryVector, retrievals, this);
      // The number of the last document presented
      int currentPosition = MAX_RETRIEVALS;
      // The number of a document to be displayed.  This is one one greater than the array index
      // in retrievals, since counting for the user starts at 1 instead of 0.
      int showNumber = 0;
      // Loop accepting user commands for processing retrievals
      do {
        String command = UserInput.prompt("\n Enter command:  ");
        // If command is empty then exit the interactive loop
        if (command.equals(""))
          break;
        if (command.equals("m")) {
          // The "more" command, print a list of the next MAX_RETRIEVALS batch of retrievals
          printRetrievals(retrievals, currentPosition);
          currentPosition = currentPosition + MAX_RETRIEVALS;
          continue;
        }
        if (command.equals("r") && feedback) {
          // The "redo" command re-excutes a revised query using Ide_regular
          if (fdback.isEmpty()) {
            System.out.println("Need to first view some documents and provide feedback.");
            continue;
          }
          System.out.println("Positive docs: " + fdback.relavant.keySet() +
              "\nNegative docs: " + fdback.irrelavant.keySet());
          System.out.println("Executing New Expanded and Reweighted Query: ");
          queryVector = fdback.newQuery();
          retrievals = retrieve(queryVector);
          // Update the list of retrievals stored in the feedback
          fdback.retrievals = retrievals;
          if (showRetrievals(retrievals))
            continue;
          else
            break;
        }
        // See if command is a number
        try {
          showNumber = Integer.parseInt(command);
        }
        catch (NumberFormatException e) {
          // If not a number, it is an unknown command
          System.out.println("Unknown command.");
          System.out.println("Enter `m' to see more, a number to show the nth document, nothing to exit.");
          if (feedback && !fdback.isEmpty())
            System.out.println("Enter `r' to use any feedback given to `redo' with a revised query.");
          continue;
        }
        // Display the selected document number in Netscape
        if (showNumber > 0 && showNumber <= retrievals.length) {
          System.out.println("Showing document " + showNumber + " in the " + Browser.BROWSER_NAME + " window.");
          Browser.display(retrievals[showNumber - 1].docRef.file);
          // If accepting feedback and have not rated this item, then get relevance feedback
          if (feedback && !fdback.haveFeedback(showNumber))
            fdback.getFeedback(showNumber);
        } else {
          System.out.println("No such document number: " + showNumber);
        }
      }
      while (true);
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
    boolean stem = false, feedback = true;
    for (int i = 0; i < args.length - 1; i++) {
      String flag = args[i];
      if (flag.equals("-html"))
        // Create HTMLFileDocuments to filter HTML tags
        docType = DocumentIterator.TYPE_HTML;
      else if (flag.equals("-stem"))
        // Stem tokens with Porter stemmer
        stem = true;
      else {
        throw new IllegalArgumentException("Unknown flag: "+ flag);
      }
    }


    // Create an inverted index for the files in the given directory.
    InvertedIndexRated index = new InvertedIndexRated(new File(dirName), docType, stem, feedback);
    // index.print();
    // Interactively process queries to this index.
    index.processQueries();
  }

}
