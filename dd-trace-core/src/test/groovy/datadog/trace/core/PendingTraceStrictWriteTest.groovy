package datadog.trace.core

import static datadog.trace.api.config.TracerConfig.TRACE_STRICT_WRITES_ENABLED

class PendingTraceStrictWriteTest extends PendingTraceTestBase {

  @Override
  CoreTracer.CoreTracerBuilder getBuilder() {
    def props = new Properties()
    props.setProperty(TRACE_STRICT_WRITES_ENABLED, "true")
    return CoreTracer.builder().withProperties(props)
  }

  def "trace is not reported until unfinished continuation is closed"() {
    when:
    def scope = tracer.activateSpan(rootSpan)
    scope.setAsyncPropagation(true)
    def continuation = scope.capture()
    scope.close()
    rootSpan.finish()

    then:
    trace.pendingReferenceCount.get() == 1
    trace.finishedSpans.asList() == [rootSpan]
    writer == []

    when: "root span buffer delay expires"
    writer.waitForTracesMax(1, 1)

    then:
    trace.pendingReferenceCount.get() == 1
    trace.finishedSpans.asList() == [rootSpan]
    writer == []
    writer.traceCount.get() == 0

    when: "continuation is closed"
    continuation.cancel()

    then:
    trace.pendingReferenceCount.get() == 0
    trace.finishedSpans.isEmpty()
    writer == [[rootSpan]]
    writer.traceCount.get() == 1
  }

  def "negative reference count throws an exception"() {
    when:
    def scope = tracer.activateSpan(rootSpan)
    scope.setAsyncPropagation(true)
    def continuation = scope.capture()
    scope.close()
    rootSpan.finish()

    then:
    trace.pendingReferenceCount.get() == 1
    trace.finishedSpans.asList() == [rootSpan]
    writer == []

    when: "continuation is finished the first time"
    continuation.cancel()

    then:
    trace.pendingReferenceCount.get() == 0
    trace.finishedSpans.isEmpty()
    writer == [[rootSpan]]
    writer.traceCount.get() == 1

    when: "continuation is finished the second time"
    // Yes this should be guarded by the used flag in the continuation,
    // so cancel it anyway to trigger the exception
    trace.cancelContinuation(continuation)

    then:
    thrown IllegalStateException
  }
}