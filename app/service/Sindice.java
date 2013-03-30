package service;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

public class Sindice {
  private static final String KEYWORD_SEARCH = "http://watson.kmi.open.ac.uk/API/semanticcontent/keywords/";
  
  public static Promise<WS.Response> searchByKeyword(String keyword) {
    
    WSRequestHolder ws = WS.url(KEYWORD_SEARCH);
    ws.setQueryParameter("q", keyword);
    ws.setHeader("Accept", "application/json");
    ws.setTimeout(99999999);
    Promise<WS.Response> promise = ws.get();
    return promise;
  }
  
  public static Set<String> searchByKeywordSync(String keyword) {
    Promise<WS.Response> promise = searchByKeyword(keyword);
    Response response = promise.get(99999999L);
    return searchByKeywordReponseParser(response);
  }
  private static Set<String> searchByKeywordReponseParser(Response r) {
    Set<String> tbReturned = new HashSet<>();
    JsonNode json = r.asJson();
    //json.toString();
    JsonNode semanticContentArray = json.get("SemanticContent-array").get("SemanticContent");
    if(semanticContentArray != null) {
      for(JsonNode jn: semanticContentArray) {
        tbReturned.add(jn.asText());
      }
    }
    return tbReturned;
  }
  
}
