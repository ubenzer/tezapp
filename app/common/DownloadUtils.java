package common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import play.Logger;
import play.Play;
import play.libs.Akka;
import play.libs.F.Promise;
import play.libs.WS;
import play.libs.WS.Response;
import play.libs.WS.WSRequestHolder;

public class DownloadUtils {
  public static final int DEFAULT_CONCURRENT_DOWNLOAD_COUNT = 50;
  public static final long DEFAULT_PROMISE_TIMEOUT_MS = Play.application().configuration().getInt("ws.timeout");
  
  public static Promise<Response> promiseNonExceptionalDownload(final WSRequestHolder ws) {
    
    Promise<Response> nonExceptionalPromise = Akka.future(new Callable<Response>() {
      @Override
      public Response call() throws Exception {
        Promise<WS.Response> innerPromise = ws.get();
        Response r = null;
        try {
          r = innerPromise.get(DEFAULT_PROMISE_TIMEOUT_MS);
        } catch (Exception e) {
          Logger.error("Inner promise exception while fetching url.", e);
        }
        return r;
      }
    });
    
    return nonExceptionalPromise;
  }
  public static Response nonExceptionalDownload(final WSRequestHolder wsrequest) {
    Promise<WS.Response> innerPromise = wsrequest.get();
    Response r = null;
    try {
      r = innerPromise.get(DEFAULT_PROMISE_TIMEOUT_MS);
    } catch (Exception e) {
      Logger.error("Inner promise exception while fetching an url", e);
    }
    return r;
  }
  
  
  public static Promise<List<Response>> promiseConcurrentDownload(Collection<WSRequestHolder> downloadList) {
    List<Promise<? extends WS.Response>> promiseList = new ArrayList<>();

    for(final WSRequestHolder wsrequest: downloadList) {
      
      Promise<Response> nonExceptionalPromise = Akka.future(new Callable<Response>() {
        @Override
        public Response call() throws Exception {
          Promise<WS.Response> innerPromise = wsrequest.get();
          Response r = null;
          try {
            r = innerPromise.get(DEFAULT_PROMISE_TIMEOUT_MS);
          } catch (Exception e) {
            Logger.error("Inner promise exception while fetching an url", e);
          }
          return r;
        }
      });

      promiseList.add(nonExceptionalPromise);
    }

    return Promise.sequence(promiseList); 
  }
  public static List<Response> concurrentDownload(Collection<WSRequestHolder> downloadList) {
    List<Response> responses = new ArrayList<WS.Response>(downloadList.size());
    Set<WSRequestHolder> bucket = new java.util.HashSet<>();
    for(WSRequestHolder request: downloadList) {
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
      bucket.add(request);
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
