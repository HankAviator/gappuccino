package io.github.hankaviator.playstoreskipdisabled;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.immutable.instruction.*;
import org.jf.dexlib2.immutable.reference.*;
import static org.junit.Assert.*;

public class ApprovalDiscoveryTest {
    private static final String HOST = "Lrenamed/ApprovalActivity;";
    private static final ImmutableFieldReference INDEX = new ImmutableFieldReference(HOST, "zz", "I");
    private static final ImmutableFieldReference PAGE = new ImmutableFieldReference(HOST, "aa", "I");
    private static final ImmutableFieldReference FLAG = new ImmutableFieldReference("Lrenamed/Item;", "q", "Z");
    private static List<Instruction> saved(String key, ImmutableFieldReference field) {
        return List.of(
                new ImmutableInstruction21c(Opcode.CONST_STRING, 2, new ImmutableStringReference(key)),
                new ImmutableInstruction22c(Opcode.IGET, 1, 4, field),
                new ImmutableInstruction12x(Opcode.MOVE, 3, 1),
                new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 5, 2, 3, 0, 0,
                        new ImmutableMethodReference("Landroid/os/Bundle;", "putInt", List.of("Ljava/lang/String;", "I"), "V")));
    }
    private static List<Instruction> branch(ImmutableFieldReference flag, int pageType) {
        return List.of(
                new ImmutableInstruction22c(Opcode.IGET_BOOLEAN, 0, 1, flag),
                new ImmutableInstruction21t(Opcode.IF_EQZ, 0, 5),
                new ImmutableInstruction11n(Opcode.CONST_4, 2, pageType),
                new ImmutableInstruction22c(Opcode.IPUT, 2, 3, PAGE),
                new ImmutableInstruction10x(Opcode.RETURN_VOID));
    }

    @Test public void savedKeysFollowRegistersRatherThanFieldOrderOrNames() {
        List<Instruction> code = new ArrayList<>(saved(ApprovalDiscovery.PAGE, PAGE));
        code.addAll(saved(ApprovalDiscovery.INDEX, INDEX));
        var result = ApprovalDiscovery.savedFields(code, HOST);
        assertEquals(INDEX, result.get(ApprovalDiscovery.INDEX));
        assertEquals(PAGE, result.get(ApprovalDiscovery.PAGE));
    }
    @Test public void clobberedRegistersDoNotBecomeSavedStateFields() {
        List<Instruction> code = new ArrayList<>(saved(ApprovalDiscovery.INDEX, INDEX));
        code.add(3, new ImmutableInstruction11n(Opcode.CONST_4, 3, 0));
        assertFalse(ApprovalDiscovery.savedFields(code, HOST).containsKey(ApprovalDiscovery.INDEX));
    }
    @Test public void renamedFlagIsIdentifiedByItsTruePageBranch() {
        assertEquals(FLAG, ApprovalDiscovery.disabledFlag(branch(FLAG, 1), PAGE));
        assertThrows(IllegalStateException.class, () -> ApprovalDiscovery.disabledFlag(branch(FLAG, 2), PAGE));
    }
    @Test public void multipleAutoUpdateBranchesAreRejected() {
        List<Instruction> code = new ArrayList<>(branch(FLAG, 1));
        code.addAll(branch(new ImmutableFieldReference("Lrenamed/Item;", "r", "Z"), 1));
        assertThrows(IllegalStateException.class, () -> ApprovalDiscovery.disabledFlag(code, PAGE));
    }
    @Test public void conflictingSavedKeysAreRejected() {
        List<Instruction> code = new ArrayList<>(saved(ApprovalDiscovery.INDEX, INDEX));
        code.addAll(saved(ApprovalDiscovery.INDEX, PAGE));
        assertThrows(IllegalStateException.class, () -> ApprovalDiscovery.savedFields(code, HOST));
    }

    private static final ImmutableFieldReference PENDING = new ImmutableFieldReference(HOST, "renamed", "Ljava/util/List;");
    private static List<Instruction> copiedList(boolean range) {
        return new ArrayList<>(List.of(
                new ImmutableInstruction21c(Opcode.CONST_STRING, 2, new ImmutableStringReference(ApprovalDiscovery.PENDING)),
                new ImmutableInstruction21c(Opcode.NEW_INSTANCE, 0, new ImmutableTypeReference("Ljava/util/ArrayList;")),
                new ImmutableInstruction22c(Opcode.IGET_OBJECT, 1, 4, PENDING),
                range
                        ? new ImmutableInstruction3rc(Opcode.INVOKE_DIRECT_RANGE, 0, 2,
                            new ImmutableMethodReference("Ljava/util/ArrayList;", "<init>", List.of("Ljava/util/Collection;"), "V"))
                        : new ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 2, 0, 1, 0, 0, 0,
                            new ImmutableMethodReference("Ljava/util/ArrayList;", "<init>", List.of("Ljava/util/Collection;"), "V")),
                new ImmutableInstruction12x(Opcode.MOVE_OBJECT, 3, 0),
                new ImmutableInstruction35c(Opcode.INVOKE_VIRTUAL, 3, 5, 2, 3, 0, 0,
                        new ImmutableMethodReference("Landroid/os/Bundle;", "putParcelableArrayList",
                            List.of("Ljava/lang/String;", "Ljava/util/ArrayList;"), "V"))));
    }
    @Test public void defensiveArrayListCopyRetainsTheOriginalListField() {
        assertEquals(PENDING, ApprovalDiscovery.savedFields(copiedList(false), HOST).get(ApprovalDiscovery.PENDING));
        assertEquals(PENDING, ApprovalDiscovery.savedFields(copiedList(true), HOST).get(ApprovalDiscovery.PENDING));
    }
    @Test public void overwrittenCopySourceDoesNotIdentifyAPendingField() {
        List<Instruction> code = copiedList(false);
        code.add(3, new ImmutableInstruction11n(Opcode.CONST_4, 1, 0));
        assertFalse(ApprovalDiscovery.savedFields(code, HOST).containsKey(ApprovalDiscovery.PENDING));
    }
    @Test public void unrelatedConstructorsAreNotTreatedAsListCopies() {
        List<Instruction> code = copiedList(false);
        code.set(3, new ImmutableInstruction35c(Opcode.INVOKE_DIRECT, 2, 0, 1, 0, 0, 0,
                new ImmutableMethodReference("Lunknown/Wrapper;", "<init>", List.of("Ljava/util/Collection;"), "V")));
        assertFalse(ApprovalDiscovery.savedFields(code, HOST).containsKey(ApprovalDiscovery.PENDING));
    }
}
