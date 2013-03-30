package common;

import org.apache.commons.validator.routines.UrlValidator;

public class Utils {
  public static UrlValidator httpSValidator = new UrlValidator(new String[] {"http","https"});
  
  public static boolean isBlank(String s) {
    return (s == null) || (s.length() == 0) || (s.trim().length() == 0);
  }
}
