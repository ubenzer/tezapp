package service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import common.DownloadUtils;
import common.Utils;

public class SindiceREST {

  private static final String SEARCH_ONTOLOGY_API_URL = "http://api.sindice.com/v3/search";
  private static final int SINDICE_MAX_RESULT_PAGE = 100;

  public static WSRequestHolder generateOntologySearchRequest(String searchQuery, int page) {
    if(page > SINDICE_MAX_RESULT_PAGE) {
      throw new IllegalArgumentException();
    }

    WSRequestHolder ws = WS.url(SEARCH_ONTOLOGY_API_URL);
    ws.setQueryParameter("fq", "format:RDF")
      .setQueryParameter("format", "json")
      .setQueryParameter("q", searchQuery)
      .setQueryParameter("page", String.valueOf(page));
    
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
    
    JsonNode jsonRoot = response.asJson();
    if(jsonRoot == null) {
      Logger.error("Cannot convert to JSON!");
      return tbReturned;
    }
    
    int pageCount = getNormalizedPageCount(jsonRoot);
    
    List<WSRequestHolder> requestList = new ArrayList<>();
    for(int i=2; i <=  pageCount; i++) {
      requestList.add(generateOntologySearchRequest(searchQuery, i));
    }

    List<Response> responses = DownloadUtils.concurrentDownload(requestList);
    responses.add(response);
    for(Response r: responses) {
      try {
        jsonRoot = r.asJson();
        JsonNode elementsJson = jsonRoot.get("entries");
        
        for(JsonNode elementJson: elementsJson) {
          JsonNode linkJson = elementJson.get("link");
          if(linkJson == null) { Logger.error("Link node doesn't exists."); continue; }
          String link = linkJson.asText();
          if(Utils.isBlank(link)) {
            Logger.error("Link node is blank.");
            continue; 
          }

          tbReturned.add(link);
        }
      } catch (Exception e) {
        Logger.error("Error while parsing: " + r.getUri().toString());
      }
    }
    return tbReturned;
  }

  private static int getNormalizedPageCount(JsonNode wholeJson) {
    JsonNode countJson = wholeJson.get("totalResults");
    if(countJson == null) { return 0; }
    
    JsonNode itemsPerPageJson = wholeJson.get("itemsPerPage");
    if(itemsPerPageJson == null) { return 0; }
    
    int count = countJson.asInt(0);
    int itemsPerPage = itemsPerPageJson.asInt(0);
    
    int realPageCount = (int) Math.ceil(count / itemsPerPage);
    
    if(realPageCount > SINDICE_MAX_RESULT_PAGE) { return SINDICE_MAX_RESULT_PAGE; }
    return realPageCount;
  }
  
}
