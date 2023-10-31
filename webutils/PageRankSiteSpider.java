package ir.webutils;

import java.util.*;
import java.io.*;
import java.net.*;

import ir.utilities.*;
import ir.webutils.Graph;
import ir.webutils.LinkExtractor;
import ir.webutils.Spider;

public class PageRankSiteSpider extends PageRankSpider{

    /**
   * Returns a list of links to follow from a given page.
   * Subclasses can use this method to direct the spider's path over
   * the web by returning a subset of the links on the page.
   *
   * @param page The current page.
   * @return Links to be visited from this page
   */
  protected List<Link> getNewLinks(HTMLPage page) {
    List<Link> pageLinks = new LinkExtractor(page).extractLinks();
    URL pageURL = page.getLink().getURL();
    for (int i = 0; i < pageLinks.size(); i++) {
        pageLinks.get(i).cleanURL(pageURL);
        if (!pageURL.getHost().equals(pageLinks.get(i).getURL().getHost())) {
            pageLinks.remove(i);
            i--;
        }
    }
    return pageLinks;
  }

  public static void main(String args[]) {
    new PageRankSiteSpider().go(args);
  }

}
