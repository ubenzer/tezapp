package controllers;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import play.Logger;
import play.Play;
import play.libs.WS;
import play.libs.WS.Response;
import play.mvc.Controller;
import play.mvc.Result;
import service.WatsonREST;

import common.FileUtils;
import common.Utils;

public class Watson extends Controller {

  public static Result index() {
    
    final String keyword = "pizza umut";
    
    final Set<String> entitySet = new HashSet<>();
    final Set<String> keywordSet = new HashSet<>();
    
    StringTokenizer keywordST = new StringTokenizer(keyword, "\t\r\n\f ,;");
    while(keywordST.hasMoreElements()) {
      String tmpS = keywordST.nextToken();
      if(Utils.isBlank(tmpS)) {
        continue;
      }
      if(Utils.httpSValidator.isValid(tmpS)) {
        entitySet.add(tmpS);
      } else {
        keywordSet.add(tmpS);
      }
    }
    
    Set<String> tbDownloaded = new HashSet<String>();
    for(String kw: keywordSet) {
      tbDownloaded.addAll(WatsonREST.searchByKeywordSync(kw));
    }
    
    for(String uri: tbDownloaded) {
      
      try {
        Response r = WS.url(uri).get().get();
        
        if(r.getStatus() != 200) {
          Logger.error("Ontology on " + uri + " cannot be downloaded: " + r.getStatus());
        }
        
        File target = new File(Play.application().path() + File.separator + "ontologies" + File.separator + "watson" + File.separator + uri);
        target.getParentFile().mkdirs();
        target = new File(Play.application().path() + File.separator + "ontologies" + File.separator + "watson" + File.separator + uri);

        FileUtils.write(r.getBodyAsStream(), target);
      } catch (Exception e) {
        Logger.error("Ontology download problem on uri: " + uri, e);
        //e.printStackTrace();
      }
    }
    
    return TODO;
  }
}
