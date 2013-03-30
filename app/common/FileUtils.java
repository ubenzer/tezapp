package common;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.commons.io.IOUtils;


public class FileUtils extends org.apache.commons.io.FileUtils {

   /**
     * Copy an input stream's contents into a file. Throws
     * runtime exception on failure.
     */
    public static void write(InputStream is, File f) {
      OutputStream os = null;
      try {
        os = new FileOutputStream(f);
        int read = 0;
        byte[] buffer = new byte[8096];
        while ((read = is.read(buffer)) > 0) {
          os.write(buffer, 0, read);
        }
      } catch(IOException e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
      }
    }
    /**
     * Copy an input stream's contents into a file and returns File MD5.
     * Throws runtime exception on failure.
     */
    public static String writeAndMD5(InputStream is, File f) throws NoSuchAlgorithmException {
      String result = null;
      MessageDigest md = MessageDigest.getInstance("MD5");
      OutputStream os = null;
      DigestInputStream dis = null;
      try {
        dis = new DigestInputStream(is, md);
        os = new FileOutputStream(f);
        int read = 0;
        byte[] buffer = new byte[8096];
        while ((read = dis.read(buffer)) > 0) {
          os.write(buffer, 0, read);
        }
      } catch(IOException e) {
        throw new RuntimeException(e);
      } finally {
        IOUtils.closeQuietly(dis);
        IOUtils.closeQuietly(is);
        IOUtils.closeQuietly(os);
      }
      BigInteger hash = new BigInteger(1, md.digest());
      result = hash.toString(16);
      while(result.length() < 32) {
        result = "0" + result;
      }
      return result;
    }
}
