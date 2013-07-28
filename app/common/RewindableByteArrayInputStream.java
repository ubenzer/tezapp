package common;

import java.io.ByteArrayInputStream;

public class RewindableByteArrayInputStream extends ByteArrayInputStream {

  public RewindableByteArrayInputStream(byte[] buf) {
    super(buf);
  }

  public RewindableByteArrayInputStream(byte[] buf, int offset, int length) {
    super(buf, offset, length);
  }

  @Override
  public synchronized int read() {
    int tbReturned = super.read();
    if(tbReturned == -1) {
      resetToBeginning();
    }
    return tbReturned;
  }


  @Override
  public synchronized int read(byte b[], int off, int len) {
    int tbReturned = super.read(b, off, len);
    if(tbReturned == -1) {
      resetToBeginning();
    }
    return tbReturned;
  }
  
  
  
  private void resetToBeginning() {
    pos = 0;
  }
}
