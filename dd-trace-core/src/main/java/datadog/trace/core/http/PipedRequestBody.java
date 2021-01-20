package datadog.trace.core.http;

import static datadog.trace.core.http.OkHttpUtils.MSGPACK;

import datadog.trace.core.serialization.WritableFormatter;
import datadog.trace.core.serialization.msgpack.MsgPackWriter;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.BufferedSink;
import okio.Pipe;

public class PipedRequestBody extends RequestBody implements StreamingSession {

  private final Pipe pipe = new Pipe(8192);
  private final MsgPackWriter writer = new MsgPackWriter(new PipeBuffer(pipe));

  public WritableFormatter writer() {
    return writer;
  }

  @Override
  public MediaType contentType() {
    return MSGPACK;
  }

  @Override
  public void writeTo(BufferedSink sink) throws IOException {
    sink.writeAll(pipe.source());
  }

  @Override
  public void close() throws IOException {
    writer.flush();
    pipe.sink().close();
  }
}
