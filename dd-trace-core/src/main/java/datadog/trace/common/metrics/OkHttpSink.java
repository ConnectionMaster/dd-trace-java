package datadog.trace.common.metrics;

import static datadog.trace.common.metrics.EventListener.EventType.BAD_PAYLOAD;
import static datadog.trace.common.metrics.EventListener.EventType.DOWNGRADED;
import static datadog.trace.common.metrics.EventListener.EventType.ERROR;
import static datadog.trace.common.metrics.EventListener.EventType.OK;
import static datadog.trace.core.http.OkHttpUtils.buildHttpClient;
import static java.util.concurrent.TimeUnit.SECONDS;

import datadog.trace.core.http.OkHttpUtils;
import datadog.trace.core.http.PipedRequestBody;
import datadog.trace.core.http.StreamingSession;
import datadog.trace.util.AgentTaskScheduler;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jctools.queues.SpscArrayQueue;

@Slf4j
public final class OkHttpSink implements Sink, EventListener, AutoCloseable {

  private final OkHttpClient client;
  private final HttpUrl metricsUrl;
  private final List<EventListener> listeners;
  private final SpscArrayQueue<Request> enqueuedRequests = new SpscArrayQueue<>(10);

  private final AgentTaskScheduler.Scheduled<?> cancellation;

  public OkHttpSink(String agentUrl, long timeoutMillis) {
    this(buildHttpClient(HttpUrl.get(agentUrl), timeoutMillis), agentUrl, "v0.5/stats");
  }

  public OkHttpSink(OkHttpClient client, String agentUrl, String path) {
    this.client = client;
    this.metricsUrl = HttpUrl.get(agentUrl).resolve(path);
    this.listeners = new CopyOnWriteArrayList<>();
    this.cancellation =
        AgentTaskScheduler.INSTANCE.scheduleAtFixedRate(
            new Sender(enqueuedRequests), this, 1, 1, SECONDS);
  }

  private void send(Request request) {
    try (final okhttp3.Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        handleFailure(response);
      } else {
        onEvent(OK, "");
      }
    } catch (IOException e) {
      onEvent(ERROR, e.getMessage());
    }
  }

  @Override
  public void onEvent(EventListener.EventType eventType, String message) {
    for (EventListener listener : listeners) {
      listener.onEvent(eventType, message);
    }
  }

  @Override
  public StreamingSession startSession() {
    PipedRequestBody body = OkHttpUtils.pipedMsgPackRequestBody();
    Request request = OkHttpUtils.prepareRequest(metricsUrl).put(body).build();
    enqueuedRequests.offer(request);
    return body;
  }

  @Override
  public void register(EventListener listener) {
    this.listeners.add(listener);
  }

  private void handleFailure(okhttp3.Response response) throws IOException {
    final int code = response.code();
    if (code == 404) {
      onEvent(DOWNGRADED, "could not find endpoint");
    } else if (code >= 400 && code < 500) {
      onEvent(BAD_PAYLOAD, response.body().string());
    } else if (code >= 500) {
      onEvent(ERROR, response.body().string());
    }
  }

  @Override
  public void close() {
    if (null != cancellation) {
      cancellation.cancel();
    }
  }

  private static final class Sender implements AgentTaskScheduler.Task<OkHttpSink> {

    private final SpscArrayQueue<Request> inbox;

    private Sender(SpscArrayQueue<Request> inbox) {
      this.inbox = inbox;
    }

    @Override
    public void run(OkHttpSink target) {
      Request request;
      while ((request = inbox.poll()) != null) {
        target.send(request);
      }
    }
  }
}
