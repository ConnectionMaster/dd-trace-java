package datadog.trace.core.http;

import datadog.trace.core.serialization.StreamingBuffer;
import java.io.IOException;
import java.nio.ByteBuffer;
import lombok.extern.slf4j.Slf4j;
import okio.BufferedSink;
import okio.Okio;
import okio.Pipe;

@Slf4j
public class PipeBuffer implements StreamingBuffer {

  private final BufferedSink sink;

  public PipeBuffer(Pipe pipe) {
    this.sink = Okio.buffer(pipe.sink());
  }

  @Override
  public boolean isDirty() {
    return false;
  }

  @Override
  public void mark() {}

  @Override
  public boolean flush() {
    try {
      sink.flush();
      return true;
    } catch (IOException e) {
      log.debug("failed to flush", e);
    }
    return false;
  }

  @Override
  public void put(byte b) {
    try {
      sink.writeByte(b & 0xFF);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putShort(short s) {
    try {
      sink.writeShort(s & 0xFFFF);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putChar(char c) {
    try {
      sink.writeInt(c);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putInt(int i) {
    try {
      sink.writeInt(i);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putLong(long l) {
    try {
      sink.writeLong(l);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putFloat(float f) {
    try {
      sink.writeInt(Float.floatToIntBits(f));
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void putDouble(double d) {
    try {
      sink.writeLong(Double.doubleToLongBits(d));
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void put(byte[] bytes) {
    try {
      sink.write(bytes);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void put(byte[] bytes, int offset, int length) {
    try {
      sink.write(bytes, offset, length);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }

  @Override
  public void put(ByteBuffer buffer) {
    try {
      sink.write(buffer);
    } catch (IOException e) {
      log.debug("failed to write to okhttp body", e);
    }
  }
}
