package com.bosch.iot.uok.common.context;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link TraceContext}.
 */
class TraceContextTest {

    @Test
    @DisplayName("Should create trace context with constructor")
    void shouldCreateWithConstructor() {
        TraceContext ctx = new TraceContext("trace123", "span456", "my-service");
        assertThat(ctx.getTraceId()).isEqualTo("trace123");
        assertThat(ctx.getSpanId()).isEqualTo("span456");
        assertThat(ctx.getParentSpanId()).isNull();
        assertThat(ctx.getServiceName()).isEqualTo("my-service");
    }

    @Test
    @DisplayName("Should create default trace context")
    void shouldCreateDefault() {
        TraceContext ctx = new TraceContext();
        assertThat(ctx.getTraceId()).isNull();
        assertThat(ctx.getSpanId()).isNull();
        assertThat(ctx.getParentSpanId()).isNull();
        assertThat(ctx.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should create child context from parent")
    void shouldCreateChildFromParent() {
        TraceContext parent = new TraceContext("trace-abc", "span-parent", "service-a");
        parent.setEnv("prod");
        parent.setBizDomain("iot");
        parent.setTeamName("backend");
        parent.setSampled(true);

        TraceContext child = TraceContext.createChild(parent, "span-child", "service-b");

        assertThat(child.getTraceId()).isEqualTo("trace-abc");
        assertThat(child.getSpanId()).isEqualTo("span-child");
        assertThat(child.getParentSpanId()).isEqualTo("span-parent");
        assertThat(child.getServiceName()).isEqualTo("service-b");
        assertThat(child.getEnv()).isEqualTo("prod");
        assertThat(child.getBizDomain()).isEqualTo("iot");
        assertThat(child.getTeamName()).isEqualTo("backend");
        assertThat(child.isSampled()).isTrue();
    }

    @Test
    @DisplayName("Should create child with sampled=false from parent")
    void shouldCreateChildNotSampled() {
        TraceContext parent = new TraceContext("trace-abc", "span-parent", "service-a");
        parent.setSampled(false);

        TraceContext child = TraceContext.createChild(parent, "span-child", "service-b");
        assertThat(child.isSampled()).isFalse();
    }

    @Test
    @DisplayName("Should throw when creating child from null parent")
    void shouldThrowWhenNullParent() {
        assertThatThrownBy(() -> TraceContext.createChild(null, "span-id", "service"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent trace context cannot be null");
    }

    @Test
    @DisplayName("Should identify root span correctly")
    void shouldIdentifyRootSpan() {
        TraceContext root = new TraceContext("trace-1", "span-1", "svc");
        assertThat(root.isRoot()).isTrue();

        root.setParentSpanId("parent-1");
        assertThat(root.isRoot()).isFalse();

        root.setParentSpanId("");
        assertThat(root.isRoot()).isTrue();

        root.setParentSpanId(null);
        assertThat(root.isRoot()).isTrue();
    }

    @Test
    @DisplayName("Should validate context correctly")
    void shouldValidateContext() {
        TraceContext empty = new TraceContext();
        assertThat(empty.isValid()).isFalse();

        TraceContext partial = new TraceContext();
        partial.setTraceId("trace-1");
        assertThat(partial.isValid()).isFalse();

        TraceContext valid = new TraceContext("trace-1", "span-1", "svc");
        assertThat(valid.isValid()).isTrue();

        TraceContext noTraceId = new TraceContext();
        noTraceId.setSpanId("span-1");
        assertThat(noTraceId.isValid()).isFalse();

        TraceContext emptyTraceId = new TraceContext();
        emptyTraceId.setTraceId("");
        emptyTraceId.setSpanId("span-1");
        assertThat(emptyTraceId.isValid()).isFalse();

        TraceContext emptySpanId = new TraceContext();
        emptySpanId.setTraceId("trace-1");
        emptySpanId.setSpanId("");
        assertThat(emptySpanId.isValid()).isFalse();
    }

    @Test
    @DisplayName("Should set and get all properties")
    void shouldSetAndGetProperties() {
        TraceContext ctx = new TraceContext();
        ctx.setTraceId("trace-1");
        ctx.setSpanId("span-1");
        ctx.setParentSpanId("parent-1");
        ctx.setServiceName("my-service");
        ctx.setEnv("prod");
        ctx.setBizDomain("iot-home");
        ctx.setTeamName("platform");
        ctx.setSampled(false);

        assertThat(ctx.getTraceId()).isEqualTo("trace-1");
        assertThat(ctx.getSpanId()).isEqualTo("span-1");
        assertThat(ctx.getParentSpanId()).isEqualTo("parent-1");
        assertThat(ctx.getServiceName()).isEqualTo("my-service");
        assertThat(ctx.getEnv()).isEqualTo("prod");
        assertThat(ctx.getBizDomain()).isEqualTo("iot-home");
        assertThat(ctx.getTeamName()).isEqualTo("platform");
        assertThat(ctx.isSampled()).isFalse();
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void shouldImplementEquals() {
        TraceContext ctx1 = new TraceContext("t1", "s1", "svc");
        TraceContext ctx2 = new TraceContext("t1", "s1", "svc");

        assertThat(ctx1).isEqualTo(ctx2);
        assertThat(ctx1.hashCode()).isEqualTo(ctx2.hashCode());

        // Same instance
        assertThat(ctx1).isEqualTo(ctx1);

        // Null
        assertThat(ctx1).isNotEqualTo(null);

        // Different type
        assertThat(ctx1).isNotEqualTo("string");
    }

    @Test
    @DisplayName("Should test equals with different fields")
    void shouldTestEqualsWithDifferentFields() {
        TraceContext base = new TraceContext("t1", "s1", "svc");

        // Different traceId
        TraceContext diffTraceId = new TraceContext("t2", "s1", "svc");
        assertThat(base).isNotEqualTo(diffTraceId);

        // Different spanId
        TraceContext diffSpanId = new TraceContext("t1", "s2", "svc");
        assertThat(base).isNotEqualTo(diffSpanId);

        // Different parentSpanId
        TraceContext diffParent = new TraceContext("t1", "s1", "svc");
        diffParent.setParentSpanId("p1");
        assertThat(base).isNotEqualTo(diffParent);

        // Different serviceName
        TraceContext diffSvc = new TraceContext("t1", "s1", "svc2");
        assertThat(base).isNotEqualTo(diffSvc);

        // Different env
        TraceContext diffEnv = new TraceContext("t1", "s1", "svc");
        diffEnv.setEnv("prod");
        assertThat(base).isNotEqualTo(diffEnv);

        // Different bizDomain
        TraceContext diffBiz = new TraceContext("t1", "s1", "svc");
        diffBiz.setBizDomain("iot");
        assertThat(base).isNotEqualTo(diffBiz);

        // Different teamName
        TraceContext diffTeam = new TraceContext("t1", "s1", "svc");
        diffTeam.setTeamName("team-a");
        assertThat(base).isNotEqualTo(diffTeam);

        // Different sampled
        TraceContext diffSampled = new TraceContext("t1", "s1", "svc");
        diffSampled.setSampled(false);
        assertThat(base).isNotEqualTo(diffSampled);
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void shouldImplementToString() {
        TraceContext ctx = new TraceContext("trace-1", "span-1", "svc");
        String str = ctx.toString();
        assertThat(str).contains("trace-1");
        assertThat(str).contains("span-1");
        assertThat(str).contains("svc");
    }
}
