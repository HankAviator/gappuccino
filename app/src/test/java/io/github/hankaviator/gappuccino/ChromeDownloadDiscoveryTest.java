package io.github.hankaviator.gappuccino;

import java.util.*;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.*;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChromeDownloadDiscoveryTest {
    private static List<String> signature(boolean flag) {
        List<String> p = new ArrayList<>(List.of("Landroid/app/Activity;", "Lrenamed/Manager;", "Ljava/lang/String;", "J", "Ljava/lang/String;", ChromeDownloadDiscovery.CALLBACK));
        if (flag) p.add("Z");
        return p;
    }
    private static ImmutableMethod method(String owner, String name, List<String> params, List<Instruction> instructions) {
        return new ImmutableMethod(owner, name, params.stream().map(p -> new ImmutableMethodParameter(p, Set.of(), null)).toList(), "V", 9,
                Set.of(), Set.of(), new ImmutableMethodImplementation(16, instructions, List.of(), List.of()));
    }
    private static ClassDef type(String owner, ImmutableMethod method) {
        return new ImmutableClassDef(owner, 1, "Ljava/lang/Object;", List.of(), null, Set.of(), List.of(), List.of(method));
    }
    private static Map<String, ClassDef> fixture(String suffix, boolean integer, String marker, boolean mixed) {
        String owner = "Lrenamed/Dialog" + suffix + ";", controller = "Lrenamed/Controller" + suffix + ";";
        List<Instruction> body = new ArrayList<>();
        body.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                new ImmutableMethodReference(integer ? "Ljava/lang/Integer;" : "Ljava/lang/Boolean;", "valueOf", List.of(integer ? "I" : "Z"), integer ? "Ljava/lang/Integer;" : "Ljava/lang/Boolean;")));
        if (mixed) body.add(new ImmutableInstruction35c(Opcode.INVOKE_STATIC, 1, 0, 0, 0, 0, 0,
                new ImmutableMethodReference("Ljava/lang/Boolean;", "valueOf", List.of("Z"), "Ljava/lang/Boolean;")));
        body.add(new ImmutableInstruction35c(Opcode.INVOKE_INTERFACE, 2, 0, 1, 0, 0, 0,
                new ImmutableMethodReference(ChromeDownloadDiscovery.CALLBACK, "onResult", List.of("Ljava/lang/Object;"), "V")));
        return Map.of(owner, type(owner, method(owner, "renamedShow", signature(integer), List.of(
                        new ImmutableInstruction21c(Opcode.CONST_STRING, 0, new ImmutableStringReference(marker)),
                        new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 0, new ImmutableTypeReference(controller))))),
                controller, type(controller, method(controller, "renamedClick", List.of(), body)));
    }
    @Test public void renamedOptimizedIntegerDialogUsesConfirmEventRatherThanBoolean() {
        var mapping = ChromeDownloadDiscovery.resolveClasses(fixture("A", true, ChromeDownloadDiscovery.MARKER, false));
        assertEquals("Lrenamed/DialogA;", mapping.owner());
        assertEquals(5, mapping.callback()); assertEquals(6, mapping.dangerous());
        assertEquals(ChromeDownloadDiscovery.Result.INTEGER, mapping.result());
        assertEquals(Integer.valueOf(1), ChromeDownloadDiscovery.accepted(mapping.result()));
    }
    @Test public void olderBooleanCallbackIsNotSentAnInteger() {
        var mapping = ChromeDownloadDiscovery.resolveClasses(fixture("A", false, ChromeDownloadDiscovery.MARKER, false));
        assertEquals(-1, mapping.dangerous());
        assertEquals(Boolean.TRUE, ChromeDownloadDiscovery.accepted(mapping.result()));
    }
    @Test public void unrelatedDownloadAndPolicyDialogsAreNotMatched() {
        assertThrows(IllegalStateException.class, () -> ChromeDownloadDiscovery.resolveClasses(fixture("A", true, "Download.PolicyWarningDialog.Events", false)));
    }
    @Test public void ambiguousCandidatesRetainOriginalUi() {
        Map<String, ClassDef> classes = new HashMap<>(fixture("A", true, ChromeDownloadDiscovery.MARKER, false));
        classes.putAll(fixture("B", true, ChromeDownloadDiscovery.MARKER, false));
        assertThrows(IllegalStateException.class, () -> ChromeDownloadDiscovery.resolveClasses(classes));
    }
    @Test public void ambiguousCallbackBoxingRetainsOriginalUi() {
        assertThrows(IllegalStateException.class, () -> ChromeDownloadDiscovery.resolveClasses(fixture("A", true, ChromeDownloadDiscovery.MARKER, true)));
    }
    @Test public void callbackAndDownloadParameterTypesMustMatch() {
        var p = signature(true);
        p.set(5, "Ljava/lang/Runnable;"); assertFalse(ChromeDownloadDiscovery.shape(p));
        p = signature(true); p.set(3, "I"); assertFalse(ChromeDownloadDiscovery.shape(p));
        p = signature(true); p.add("Z"); assertFalse(ChromeDownloadDiscovery.shape(p));
    }
}
