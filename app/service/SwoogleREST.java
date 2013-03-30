package service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import play.Logger;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import common.DownloadUtils;
import common.Utils;

public class SwoogleREST {

  private static final String ACCESS_KEY = "52fc0c56ec4942e2a5268356d0b8af23";
  private static final String SEARCH_ONTOLOGY_API_URL = "http://sparql.cs.umbc.edu/swoogle31/q";
  private static final int SWOOGLE_MAX_RESULT = 1000;
  private static final int SWOOGLE_RESULT_PER_PAGE = 10;

  public static WSRequestHolder generateOntologySearchRequest(String searchQuery, int startResult) {
    if(startResult >= SWOOGLE_MAX_RESULT) {
      throw new IllegalArgumentException();
    }

    WSRequestHolder ws = WS.url(SEARCH_ONTOLOGY_API_URL);
    ws.setQueryParameter("queryType", "search_swd_ontology")
      .setQueryParameter("key", ACCESS_KEY)
      .setQueryParameter("searchString", searchQuery)
      .setQueryParameter("searchStart", String.valueOf(startResult));
    
    return ws;
  }
  
  public static Set<String> searchOntologySync(String searchQuery) {
    Set<String> tbReturned = new HashSet<>();

    int start = 1;
    
    /* Send first page search to get result count */
    WSRequestHolder request = generateOntologySearchRequest(searchQuery, start);
    Response response;
    try {
      response = request.get().get(DownloadUtils.DEFAULT_PROMISE_TIMEOUT_MS);
    } catch (Exception e) {
      Logger.error("Can't get results.", e);
      return tbReturned;
    }

    if(response == null) {
      Logger.error("Response is null!");
      return tbReturned;
    }
    
    Document doc;
    try {
      doc = response.asXml();
    } catch (Exception e) {
      Logger.error("Cant parse results as XML", e);
      return tbReturned;
    }
    int searchCount = getNormalizedSearchResultCount(doc);
    
    List<WSRequestHolder> requestList = new ArrayList<>();
    for(int i=1; i< StrictMath.floor(searchCount/SWOOGLE_RESULT_PER_PAGE) + (searchCount%SWOOGLE_RESULT_PER_PAGE > 0 ? 1 : 0); i++) {
      start = (i * SWOOGLE_RESULT_PER_PAGE) + 1;
      requestList.add(generateOntologySearchRequest(searchQuery, start));
    }

    // https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1YE-y7d0L8Q interesting...
    List<Response> responses = DownloadUtils.concurrentDownload(requestList);

    for(Response r: responses) {
      try {
        doc = r.asXml();
        NodeList nodes = doc.getElementsByTagName("wob:SemanticWebDocument");
        
        for(int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if(node.getNodeType() != Node.ELEMENT_NODE) {
            Logger.error("Invalid node type: " + node.getNodeType());
            continue;
          }
          Element element = (Element)node;
 
          // get ontology name
          String uri = element.getAttribute("rdf:about");
          if(Utils.isBlank(uri)) {
            Logger.error("Uri is blank.");
            continue;
          }
          
          tbReturned.add(uri);
        }
      } catch (Exception e) {
        Logger.error("Error while parsing: " + r.getUri().toString());
      }
    }
    return tbReturned;
  }

  private static int getNormalizedSearchResultCount(Document doc) {
    Node searchCountNode = doc.getElementsByTagName("swoogle:hasSearchTotalResults").item(0);
    int searchCount = Integer.parseInt(searchCountNode.getTextContent());
    if(searchCount > SWOOGLE_MAX_RESULT) { searchCount = SWOOGLE_MAX_RESULT; }
    return searchCount;
  }
}
