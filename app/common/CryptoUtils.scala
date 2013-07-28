package common
import org.apache.commons.codec.digest.DigestUtils
import java.io.InputStream

/**
 * User: ub (13/7/13 - 12:48 PM)
 */
object CryptoUtils {
  def md5(input: String): String = DigestUtils.md5Hex(input)
  def md5(input: InputStream): String = DigestUtils.md5Hex(input)
}
