package common;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import play.Logger;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

public class ValidationUtils {
  private final static long MAX_PARSE_TIMEOUT = 10000L;
  public static boolean checkIfOntologyIsValid(final InputStream in, final String baseUri) {
    
    Promise<Model> callMeMaybe = Akka.future(new Callable<Model>() {
      @Override
      public Model call() throws Exception {
        Model model;
        try {
          model = ModelFactory.createOntologyModel();
          model.read(in, baseUri);
        } catch (Exception e) {
          Logger.error("Error parsing file as ontology: " + baseUri, e);
          return null;
        }
        return model;
      }
    });
    
    // The page of death for xerces: http://dbpedia.org/page/Pizza_Deliverance
    // See here: http://stackoverflow.com/questions/15926195/jena-reading-a-model-takes-forever/
    
    try {
      Model maybeModel = callMeMaybe.get(MAX_PARSE_TIMEOUT);
      if(maybeModel == null) {
        return false;
      }
    } catch (Exception e) {
      Logger.error("Parsing ontology took too much time! " + baseUri, e);
      return false;
    }
    return true;
  }
  
  public static List<Response> removeUnparsableFiles(List<Response> responses) {
    List<Response> tbReturned = new ArrayList<WS.Response>();
  
    List<Promise<? extends Response>> promises = new ArrayList<>();
    
    for(final Response r: responses) {
      
      Promise<Response> promise = Akka.future(new Callable<Response>() {
        @Override
        public Response call() throws Exception {
          if(ValidationUtils.checkIfOntologyIsValid(r.getBodyAsStream(), r.getUri().toString())) { 
            return r;
          }
          return null;
        }
      });
      promises.add(promise);
    }
    
    Promise<List<Response>> promiseAll = Promise.sequence(promises);
    List<Response> results;
    try {
      results = promiseAll.get(MAX_PARSE_TIMEOUT * responses.size());
    } catch (Exception e) {
      Logger.error("Exception while waiting for response from ontology parsers!", e);
      return tbReturned;
    }
    
    for(Response r: results) {
      if(r == null) { continue; }
      tbReturned.add(r);
    }
    
    return tbReturned;
  }
}
