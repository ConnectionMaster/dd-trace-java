package datadog.trace.core.http;

import datadog.trace.core.serialization.WritableFormatter;

public interface StreamingSession extends AutoCloseable {

  WritableFormatter writer();
}
