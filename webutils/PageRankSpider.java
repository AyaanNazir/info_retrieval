package ir.webutils;

import java.util.*;
import java.io.*;
import java.net.*;

import ir.utilities.*;
import ir.webutils.Graph;
import ir.webutils.LinkExtractor;
import ir.webutils.Spider;

public class PageRankSpider extends Spider{

    Graph graph = new Graph();

    /**
   * Checks command line arguments and performs the crawl.  <p> This
   * implementation calls <code>processArgs</code> and
   * <code>doCrawl</code>.
   *
   * @param args Command line arguments.
   */
  public void go(String[] args) {
    processArgs(args);
    doCrawl();
    graph = clear();
    System.out.println("Graph Structure");
    graph.print();
    double alpha = .15;
    int iterations = 50;
    HashMap<String, Double> ranks = new HashMap<String, Double>();
    HashMap<String, Double> temp = new HashMap<String, Double>();
    graph.resetIterator();
    Node iter = graph.nextNode();
    while (iter != null) {
        ranks.put(iter.page, 1.0 / count);
        temp.put(iter.page, 1.0 / count);
        iter = graph.nextNode();
    }

    for (int i = 0; i < iterations; i++) {
        System.out.println("Iteration " + i);
        graph.resetIterator();
        iter = graph.nextNode();
        while (iter != null) {
            temp.put(iter.page, rankPage(iter, ranks, alpha, count));
            iter = graph.nextNode();
        }
        double sum = 0;
        for (String x : temp.keySet()) {
            sum += temp.get(x);
            System.out.println("Unnormalized P = " + temp.get(x) + " | " + x);
        }
        for (String x : temp.keySet()) {
            temp.put(x, temp.get(x) / sum);
            ranks.put(x, temp.get(x) / sum);
            System.out.println("Normalized P = " + temp.get(x) + " | " + x);
        }
    }
    try {
        BufferedWriter write = new BufferedWriter(new FileWriter(new File(saveDir + "/page_ranks.txt")));
        for (String x : ranks.keySet()) {
            write.write(x + " " + ranks.get(x));
            write.newLine();
        }
        write.close();
    } catch (IOException e) {
        System.out.println(e);
    }
    System.out.println("\nPageRank:");
    for (Node x : graph.nodeArray()) {
        System.out.println("PR(" + x.name + "): " + ranks.get(x.page));
    }
  }

  /**
   * Performs the crawl.  Should be called after
   * <code>processArgs</code> has been called.  Assumes that
   * starting url has been set.  <p> This implementation iterates
   * through a list of links to visit.  For each link a check is
   * performed using {@link #visited visited} to make sure the link
   * has not already been visited.  If it has not, the link is added
   * to <code>visited</code>, and the page is retrieved.  If access
   * to the page has been disallowed by a robots.txt file or a
   * robots META tag, or if there is some other problem retrieving
   * the page, then the page is skipped.  If the page is downloaded
   * successfully {@link #indexPage indexPage} and {@link
   * #getNewLinks getNewLinks} are called if allowed.
   * <code>go</code> terminates when there are no more links to visit
   * or <code>count &gt;= maxCount</code>
   */
  public void doCrawl() {
    if (linksToVisit.size() == 0) {
      System.err.println("Exiting: No pages to visit.");
      System.exit(0);
    }
    visited = new HashSet<Link>();
    while (linksToVisit.size() > 0 && count < maxCount) {
      // Pause if in slow mode
      if (slow) {
        synchronized (this) {
          try {
            wait(1000);
          }
          catch (InterruptedException e) {
          }
        }
      }
      // Take the top link off the queue
      Link link = linksToVisit.remove(0);
      link.cleanURL(); // Standardize and clean the URL for the link
      System.out.println("Trying: " + link);
      // Skip if already visited this page
      if (!visited.add(link)) {
        System.out.println("Already visited");
        continue;
      }
      if (!linkToHTMLPage(link)) {
        System.out.println("Not HTML Page");
        continue;
      }
      HTMLPage currentPage = null;
      // Use the page retriever to get the page
      try {
        currentPage = retriever.getHTMLPage(link);
      }
      catch (PathDisallowedException e) {
        System.out.println(e);
        continue;
      }
      if (currentPage.empty()) {
        System.out.println("No Page Found");
        continue;
      }
      if (currentPage.indexAllowed()) {
        count++;
        System.out.println("Indexing" + "(" + count + "): " + link);
        indexPage(currentPage);
      }
      if (count < maxCount) {
        List<Link> newLinks = getNewLinks(currentPage);
        for (Link x : newLinks) {
            x.cleanURL();
        }
        // System.out.println("Adding the following links" + newLinks);
        // Add new links to end of queue
        linksToVisit.addAll(newLinks);
      }
    }
  }

  public Graph clear() {
    Graph temp = new Graph();
    graph.resetIterator();
    Node iter = graph.nextNode();
    while (iter != null) {
        if (iter.indexed) {
            Node empty = temp.getNode(iter.name);
            empty.page = iter.page;
            empty.indexed = true;
            for (Node x : iter.getEdgesOut()) {
                if (x.indexed && !empty.getEdgesOut().contains(temp.getNode(x.name))) {
                    empty.addEdge(temp.getNode(x.name));;
                }
            }
        } else {
            graph.iterator.remove();
        }
        iter = graph.nextNode();
    }
    return temp;
  }

  public double rankPage(Node iter, HashMap<String, Double> ranks, double alpha, int count) {
    double sum = 0;
    List<Node> nodes = iter.getEdgesIn();
    for (Node x : nodes) {
        sum += ranks.get(x.page) / x.getEdgesOut().size();
    }
    return ((1 - alpha) * sum) + (alpha / count);
  }

  /**
   * "Indexes" a <code>HTMLpage</code>.  This version just writes it
   * out to a file in the specified directory with a "P<count>.html" file name.
   *
   * @param page An <code>HTMLPage</code> that contains the page to
   *             index.
   */
  protected void indexPage(HTMLPage page) {
    String pageData = "P" + MoreString.padWithZeros(count, (int) Math.floor(MoreMath.log(maxCount, 10)) + 1);
    Node temp = graph.getNode(page.link.getURL().toString());
    temp.page = pageData + ".html";
    temp.indexed = true;
    LinkExtractor iterator = new LinkExtractor(page);
    for (Link x : iterator.extractLinks()) {
        if (!x.getURL().toString().equals(temp.name)) {
            temp.addEdge(graph.getNode(x.getURL().toString()));
        }
    }
    page.write(saveDir, pageData);
  }

  public static void main(String args[]) {
    new PageRankSpider().go(args);
  }

}
