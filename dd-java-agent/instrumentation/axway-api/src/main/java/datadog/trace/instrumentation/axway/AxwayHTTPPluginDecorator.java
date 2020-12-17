package datadog.trace.instrumentation.axway;

import datadog.trace.bootstrap.instrumentation.decorator.HttpClientDecorator;
import java.lang.reflect.Field;
import java.net.URI;

public class AxwayHTTPPluginDecorator extends HttpClientDecorator<Object, Object> {
  public static final String CORRELATION_HOST = "CORRELATION_HOST";
  public static final String CORRELATION_PORT = "CORRELATION_PORT";
  public static final String AXWAY_TRANSACTION = "axway.transaction";

  public static final AxwayHTTPPluginDecorator DECORATE = new AxwayHTTPPluginDecorator();

  @Override
  protected String[] instrumentationNames() {
    return new String[] {"axway-api"};
  }

  @Override
  protected String component() {
    return "axway.HTTPPlugin";
  }

  @Override
  protected String method(final Object httpRequest) {
    return "GET"; // "httpRequest.getMethod() ??";
  }

  @Override
  protected URI url(final Object axwayTransactionState) {
    try {
      Field f = axwayTransactionState.getClass().getDeclaredField("uri");
      f.setAccessible(true);
      URI uri = (URI) f.get(axwayTransactionState);
      return uri;
    } catch (NoSuchFieldException | IllegalAccessException | ClassCastException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  protected int status(final Object clientResponse) {
    return 0;
  }
}
