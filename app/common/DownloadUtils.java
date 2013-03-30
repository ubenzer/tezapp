package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;

public class DownloadUtils {
  public static final int DEFAULT_CONCURRENT_DOWNLOAD_COUNT = 30;
  public static final long DEFAULT_PROMISE_TIMEOUT_MS = Play.application().configuration().getInt("ws.timeout");
  
  public static Promise<List<Response>> promiseConcurrentDownload(Collection<String> downloadList) {
    List<Promise<? extends WS.Response>> promiseList = new ArrayList<>();

    for(final String url: downloadList) {
      
      Logger.info("Fetch started for " + url + " " + new Date().toString());
      
      Promise<Response> nonExceptionalPromise = Akka.future(new Callable<Response>() {
        @Override
        public Response call() throws Exception {
          Promise<WS.Response> innerPromise = WS.url(url).get();
          Response r = null;
          try {
            r = innerPromise.get(DEFAULT_PROMISE_TIMEOUT_MS);
          } catch (Exception e) {
            Logger.error("Inner promise exception while fetching url e: " + (e.getCause() != null ? e.getCause().getClass().getCanonicalName() : e.getClass().getCanonicalName()) + " u: " + url);
          }
          return r;
        }
      });

      promiseList.add(nonExceptionalPromise);
    }

    return Promise.sequence(promiseList); 
  }
  public static List<Response> concurrentDownload(Collection<String> uriList) {
    List<Response> responses = new ArrayList<WS.Response>(uriList.size());
    Set<String> bucket = new java.util.HashSet<>();
    for(String uri: uriList) {
      if(bucket.size() >= DownloadUtils.DEFAULT_CONCURRENT_DOWNLOAD_COUNT) {
        Promise<List<Response>> promises = DownloadUtils.promiseConcurrentDownload(bucket);
        bucket.clear();
        try {
          List<Response> iResponses = promises.get(DownloadUtils.DEFAULT_PROMISE_TIMEOUT_MS * 2);
          for(Response r: iResponses) {
            if(r != null) {
              responses.add(r);
            }
          }
        } catch (Exception e) {
          Logger.error("Shouldn't happen! 1", e);
        }
      }
      bucket.add(uri);
    }
    if(bucket.size() > 0) {
      Promise<List<Response>> promises = DownloadUtils.promiseConcurrentDownload(bucket);
      bucket.clear();
      try {
        List<Response> iResponses = promises.get(DownloadUtils.DEFAULT_PROMISE_TIMEOUT_MS * 2);
        for(Response r: iResponses) {
          if(r != null) {
            responses.add(r);
          }
        }
      } catch (Exception e) {
        Logger.error("Shouldn't happen! 2", e);
      }
    }
    return responses;
  }
  public static List<Response> removeBadResponses(Collection<Response> responses) {
    List<Response> tbReturned = new ArrayList<>();
    for(Response r: responses) {
      if(r != null && r.getStatus() == 200) {
        tbReturned.add(r);
      }
    }
    return tbReturned;
  }
}
