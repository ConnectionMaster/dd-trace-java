package datadog.trace.common.metrics;

import datadog.trace.core.http.StreamingSession;

public interface Sink {

  StreamingSession startSession();

  void register(EventListener listener);
}
