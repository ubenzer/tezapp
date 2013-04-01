package service;

import java.util.HashSet;
import java.util.Set;

import org.codehaus.jackson.JsonNode;

import play.Logger;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

import common.DownloadUtils;

public class WatsonREST {
  private static final String KEYWORD_SEARCH = "http://watson.kmi.open.ac.uk/API/semanticcontent/keywords/";
  
  public static Set<String> searchByKeywordSync(String keyword) {
    WSRequestHolder ws = WS.url(KEYWORD_SEARCH);
    ws.setQueryParameter("q", keyword);
    ws.setHeader("Accept", "application/json");
    
    Response response = DownloadUtils.nonExceptionalDownload(ws);
    return searchByKeywordReponseParser(response);
  }
  private static Set<String> searchByKeywordReponseParser(Response r) {
    Set<String> tbReturned = new HashSet<>();
    try {
      JsonNode json = r.asJson();
      JsonNode semanticContentArray = json.get("SemanticContent-array").get("SemanticContent");
      if(semanticContentArray != null) {
        for(JsonNode jn: semanticContentArray) {
          tbReturned.add(jn.asText());
        }
      }
    } catch (Exception e) {
      Logger.error("Error while parsing Watson JSON", e);
    }
    return tbReturned;
  }
}
