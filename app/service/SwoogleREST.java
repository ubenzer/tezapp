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
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import common.Utils;

public class SwoogleREST {

  private static final String ACCESS_KEY = "52fc0c56ec4942e2a5268356d0b8af23";
  private static final String SEARCH_ONTOLOGY_API_URL = "http://sparql.cs.umbc.edu/swoogle31/q";
  private static final int SWOOGLE_MAX_RESULT = 1000;
  private static final int SWOOGLE_RESULT_PER_PAGE = 10;

  public static Promise<WS.Response> searchOntology(String searchQuery, int startResult) {
    if(startResult >= SWOOGLE_MAX_RESULT) {
      throw new IllegalArgumentException();
    }

    WSRequestHolder ws = WS.url(SEARCH_ONTOLOGY_API_URL);
    ws.setTimeout(100002);
    ws.setQueryParameter("queryType", "search_swd_ontology")
      .setQueryParameter("key", ACCESS_KEY)
      .setQueryParameter("searchString", searchQuery)
      .setQueryParameter("searchStart", String.valueOf(startResult));
    
    final Promise<WS.Response> promise = ws.get();
    
    return promise;
  }
  
  public static Set<String> searchOntologySync(String searchQuery) {
    Set<String> tbReturned = new HashSet<>();
    List<Promise<? extends WS.Response>> promiseList = new ArrayList<>();
    int start = 1;
    
    Promise<WS.Response> promise = searchOntology(searchQuery, start);
    Response response = promise.get(10003L);
    promiseList.add(promise);
    
    Document doc = response.asXml();

    Node searchCountNode = doc.getElementsByTagName("swoogle:hasSearchTotalResults").item(0);
    int searchCount = Integer.parseInt(searchCountNode.getTextContent());
    if(searchCount > SWOOGLE_MAX_RESULT) { searchCount = SWOOGLE_MAX_RESULT; }
    
    for(int i=1; i< StrictMath.floor(searchCount/SWOOGLE_RESULT_PER_PAGE) + (searchCount%SWOOGLE_RESULT_PER_PAGE > 0 ? 1 : 0); i++) {
      start = (i * SWOOGLE_RESULT_PER_PAGE) + 1;
      promiseList.add(searchOntology(searchQuery, start));
    }

    // https://groups.google.com/forum/?fromgroups=#!topic/play-framework/1YE-y7d0L8Q interesting...
    Promise<List<Response>> sequence = Promise.sequence(promiseList);
    
    List<Response> responses = sequence.get(99999999999L);
    List<String> md5sumlsit = new ArrayList<>();
    for(Response r: responses) {
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
        
        // get ontology sum
        String md5sum = element.getElementsByTagName("swoogle:hasMd5sum").item(0).getTextContent();
        if(md5sumlsit.contains(md5sum)) {
          Logger.warn("This ontologies another copy is alredy in the list: " + uri);
          continue;
        }
        
        NodeList fsizeNL = element.getElementsByTagName("swoogle:hasLength");
        if(fsizeNL == null) {
          Logger.error("Size is empty?");
          continue;
        }
        Node fsizeN = fsizeNL.item(0);
        if(fsizeN == null) {
          Logger.error("Size is empty? 2");
          continue;
        }
        String sizeS = fsizeN.getTextContent();
        if(Utils.isBlank(sizeS)) {
          Logger.error("Size is empty? 3");
          continue;
        }
        long size;
        try {
          size = Long.parseLong(sizeS);
        } catch (Exception e) {
          Logger.error("Invalid long format: " + sizeS, e);
          continue;
        }
   
        if(size < 1024 * 2) {
          Logger.error("This ontology (?) is too small: " + uri);
          continue;
        }
        
        tbReturned.add(uri);
      }
    }
    return tbReturned;
  }
}
