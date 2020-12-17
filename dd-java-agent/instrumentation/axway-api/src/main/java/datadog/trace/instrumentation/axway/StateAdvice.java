package datadog.trace.instrumentation.axway;

import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.activateSpan;
import static datadog.trace.bootstrap.instrumentation.api.AgentTracer.startSpan;
import static datadog.trace.instrumentation.axway.AxwayHTTPPluginDecorator.DECORATE;

import datadog.trace.bootstrap.instrumentation.api.AgentScope;
import datadog.trace.bootstrap.instrumentation.api.AgentSpan;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;

public class StateAdvice {

  @Advice.OnMethodEnter(suppress = Throwable.class)
  public static AgentScope onEnter(
      @Advice.FieldValue String host,
      @Advice.FieldValue String port,
      @Advice.FieldValue java.net.URI uri,
      @Advice.FieldValue Object headers,
      @Advice.This final Object thisObj) {
    try {
      Method m = headers.getClass().getDeclaredMethod("setHeader");
      m.setAccessible(true);
      Object res1 = m.invoke(headers, AxwayHTTPPluginDecorator.CORRELATION_HOST, host);
      Object res2 = m.invoke(headers, AxwayHTTPPluginDecorator.CORRELATION_PORT, port);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      e.printStackTrace();
    }

    final AgentSpan span = startSpan(AxwayHTTPPluginDecorator.AXWAY_TRANSACTION);
    final AgentScope scope = activateSpan(span);
    DECORATE.afterStart(span);
    DECORATE.onRequest(span, thisObj);

    return scope;
  }

  //  @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
  //  public static void onExit(@Advice.FieldValue String host,
  //                            @Advice.FieldValue String port,
  //                            @Advice.FieldValue java.net.URI uri,
  //                            @Advice.FieldValue Object headers,
  //                            @Advice.Enter final AgentScope scope,
  //                            @Advice.Thrown final Throwable throwable) {
  //    if (scope == null) {
  //      return;
  //    }
  //  }
}
