package io.github.hankaviator.gappuccino.runtime;

import org.junit.Test;
import static org.junit.Assert.*;

public class ReflectionTest {
    static class Parent { private final String secret = "inherited"; private String inherited() { return secret; } }
    static class Builder extends Parent { boolean enabled = true; private Builder enableQuic(boolean value) { enabled = value; return this; } }
    @Test public void cronetBuilderBooleanPrimitiveAndFluentResult() {
        Builder builder = new Builder();
        assertSame(builder, Reflection.callMethod(builder, "enableQuic", false));
        assertFalse(builder.enabled);
    }
    @Test public void inheritedPrivateMembersAreAccessible() {
        Builder builder = new Builder();
        assertEquals("inherited", Reflection.getObjectField(builder, "secret"));
        assertEquals("inherited", Reflection.callMethod(builder, "inherited"));
    }
    @Test public void missingOptionalClassDoesNotInitializeAnything() {
        assertNull(Reflection.findClassIfExists("missing.optional.Cronet", getClass().getClassLoader()));
    }
}
