package datadog.trace.common.metrics

import datadog.trace.core.http.StreamingSession
import datadog.trace.test.util.DDSpecification
import okhttp3.Call
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import spock.lang.Requires

import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch

import static datadog.trace.api.Platform.isJavaVersionAtLeast
import static datadog.trace.common.metrics.EventListener.EventType.BAD_PAYLOAD
import static datadog.trace.common.metrics.EventListener.EventType.DOWNGRADED
import static datadog.trace.common.metrics.EventListener.EventType.ERROR
import static datadog.trace.common.metrics.EventListener.EventType.OK
import static java.util.concurrent.TimeUnit.SECONDS

@Requires({ isJavaVersionAtLeast(8) })
class OkHttpSinkTest extends DDSpecification {

  def "http status code #responseCode yields #eventType"() {
    setup:
    String agentUrl = "http://localhost:8126"
    String path = "v0.5/stats"
    CountDownLatch latch = new CountDownLatch(1)
    EventListener listener = new BlockingListener(latch)
    OkHttpClient client = Mock(OkHttpClient)
    client.newCall(_) >> { Request request -> respond(request, responseCode) }
    OkHttpSink sink = new OkHttpSink(client, agentUrl, path)
    sink.register(listener)

    when:
    StreamingSession session = sink.startSession()
    session.close()

    then:
    latch.await(5, SECONDS)
    listener.events.size() == 1
    listener.events[0] == eventType

    where:
    eventType   | responseCode
    DOWNGRADED  | 404
    ERROR       | 500
    ERROR       | 0 // throw
    BAD_PAYLOAD | 400
    OK          | 200
    OK          | 201
  }

  def respond(Request request, int code) {
    if (0 == code) {
      return error(request)
    }
    return Mock(Call) {
      it.execute() >> new Response.Builder()
        .code(code)
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .message("message")
        .body(ResponseBody.create(MediaType.get("text/plain"), "message"))
        .build()
    }
  }

  def error(Request request) {
    return Mock(Call) {
      it.execute() >> { throw new IOException("thrown by test") }
    }
  }

  class BlockingListener implements EventListener {

    private final CountDownLatch latch
    private List<EventType> events = new CopyOnWriteArrayList<>()

    BlockingListener(CountDownLatch latch) {
      this.latch = latch
    }

    @Override
    void onEvent(EventType eventType, String message) {
      events.add(eventType)
      latch.countDown()
    }
  }

}
