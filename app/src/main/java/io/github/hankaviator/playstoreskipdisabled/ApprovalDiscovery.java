package io.github.hankaviator.playstoreskipdisabled;

import java.io.File;
import java.io.IOException;
import java.util.*;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.instruction.*;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;

/** Resolve fields from Bundle keys and the approval branch, rather than their names. */
final class ApprovalDiscovery {
    static final String PENDING = "MultiInstallActivity.installs-for-approval";
    static final String INDEX = "MultiInstallActivity.current-install-index";
    static final String PAGE = "MultiInstallActivity.current-page-type";
    record Mapping(String renderer, FieldReference pending, FieldReference index,
                   FieldReference page, FieldReference disabled) {}
    private ApprovalDiscovery() {}

    static Mapping resolve(File apk, String activityName) throws IOException {
        String owner = "L" + activityName.replace('.', '/') + ";";
        var container = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault());
        for (String entry : container.getDexEntryNames()) {
            for (ClassDef type : container.getEntry(entry).getDexFile().getClasses()) {
                if (!type.getType().equals(owner)) continue;
                List<Method> renderers = new ArrayList<>(), savers = new ArrayList<>();
                for (Method method : type.getMethods()) {
                    if (method.getImplementation() == null) continue;
                    if (method.getName().equals("onSaveInstanceState")
                            && method.getParameterTypes().equals(List.of("Landroid/os/Bundle;"))) savers.add(method);
                    if (method.getReturnType().equals("V")
                            && method.getParameterTypes().equals(List.of("Z"))
                            && hasString(method, "InstallApprovalFragment.approvalType")
                            && hasString(method, "InstallApprovalFragment.packageName")) renderers.add(method);
                }
                if (renderers.size() != 1 || savers.size() != 1)
                    throw new IllegalStateException("Ambiguous approval renderer or saved-state method");
                Map<String, FieldReference> fields = savedFields(savers.get(0).getImplementation().getInstructions(), owner);
                FieldReference pending = require(fields, PENDING), index = require(fields, INDEX), page = require(fields, PAGE);
                if (!index.getType().equals("I") || !page.getType().equals("I")
                        || same(index, page) || !(pending.getType().equals("Ljava/util/ArrayList;")
                        || pending.getType().equals("Ljava/util/List;")))
                    throw new IllegalStateException("Unexpected approval state types");
                Method renderer = renderers.get(0);
                FieldReference disabled = disabledFlag(renderer.getImplementation().getInstructions(), page);
                return new Mapping(renderer.getName(), pending, index, page, disabled);
            }
        }
        throw new IllegalStateException("Approval activity DEX not found");
    }

    private static boolean hasString(Method method, String key) {
        for (Instruction instruction : method.getImplementation().getInstructions())
            if (instruction instanceof ReferenceInstruction ref
                    && ref.getReference() instanceof StringReference text && text.getString().equals(key)) return true;
        return false;
    }

    static Map<String, FieldReference> savedFields(Iterable<? extends Instruction> instructions, String owner) {
        Map<Integer, Object> registers = new HashMap<>();
        Map<String, FieldReference> fields = new HashMap<>();
        for (Instruction instruction : instructions) {
            if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof MethodReference call
                    && call.getDefiningClass().equals("Landroid/os/Bundle;")
                    && (call.getName().equals("putInt") || call.getName().equals("putParcelableArrayList"))) {
                int[] args = arguments(instruction);
                if (args.length == 3 && registers.get(args[1]) instanceof String key
                        && registers.get(args[2]) instanceof FieldReference field
                        && field.getDefiningClass().equals(owner)) {
                    FieldReference previous = fields.putIfAbsent(key, field);
                    if (previous != null && !same(previous, field))
                        throw new IllegalStateException("Conflicting saved-state field for " + key);
                }
            }
            update(registers, instruction);
            if (instruction instanceof OffsetInstruction || !instruction.getOpcode().canContinue()) registers.clear();
        }
        return fields;
    }

    static FieldReference disabledFlag(Iterable<? extends Instruction> instructions, FieldReference page) {
        Map<Integer, Instruction> code = new LinkedHashMap<>();
        int address = 0;
        for (Instruction instruction : instructions) { code.put(address, instruction); address += instruction.getCodeUnits(); }
        Map<Integer, Object> registers = new HashMap<>();
        List<FieldReference> matches = new ArrayList<>();
        for (var item : code.entrySet()) {
            Instruction instruction = item.getValue();
            if ((instruction.getOpcode() == Opcode.IF_EQZ || instruction.getOpcode() == Opcode.IF_NEZ)
                    && instruction instanceof OneRegisterInstruction one
                    && registers.get(one.getRegisterA()) instanceof FieldReference flag
                    && flag.getType().equals("Z") && !flag.getDefiningClass().equals(page.getDefiningClass())) {
                int truePath = instruction.getOpcode() == Opcode.IF_EQZ
                        ? item.getKey() + instruction.getCodeUnits()
                        : item.getKey() + ((OffsetInstruction) instruction).getCodeOffset();
                if (setsAutoUpdatePage(code, truePath, page, registers)) matches.add(flag);
            }
            update(registers, instruction);
            if (instruction instanceof OffsetInstruction || !instruction.getOpcode().canContinue()) registers.clear();
        }
        if (matches.size() != 1) throw new IllegalStateException("Ambiguous auto-update-disabled branch");
        return matches.get(0);
    }

    private static boolean setsAutoUpdatePage(Map<Integer, Instruction> code, int address,
                                             FieldReference page, Map<Integer, Object> source) {
        Map<Integer, Object> registers = new HashMap<>(source);
        for (int steps = 0; steps < 32; steps++) {
            Instruction instruction = code.get(address);
            if (instruction == null) return false;
            if (instruction.getOpcode() == Opcode.IPUT && instruction instanceof ReferenceInstruction ref
                    && ref.getReference() instanceof FieldReference field && same(field, page)) {
                return Integer.valueOf(1).equals(registers.get(((TwoRegisterInstruction) instruction).getRegisterA()));
            }
            if (instruction instanceof OffsetInstruction || !instruction.getOpcode().canContinue()) return false;
            update(registers, instruction);
            address += instruction.getCodeUnits();
        }
        return false;
    }

    private static void update(Map<Integer, Object> registers, Instruction instruction) {
        if (!(instruction instanceof OneRegisterInstruction one) || !instruction.getOpcode().setsRegister()) return;
        Object value = null;
        if (instruction instanceof ReferenceInstruction ref && ref.getReference() instanceof StringReference text)
            value = text.getString();
        else if (instruction.getOpcode().name.startsWith("iget") && instruction instanceof ReferenceInstruction ref
                && ref.getReference() instanceof FieldReference field) value = field;
        else if (instruction.getOpcode().name.startsWith("move") && instruction instanceof TwoRegisterInstruction two)
            value = registers.get(two.getRegisterB());
        else if (instruction.getOpcode().name.startsWith("const") && instruction instanceof NarrowLiteralInstruction literal)
            value = literal.getNarrowLiteral();
        if (value == null) registers.remove(one.getRegisterA()); else registers.put(one.getRegisterA(), value);
    }

    private static int[] arguments(Instruction instruction) {
        if (instruction instanceof FiveRegisterInstruction five) {
            int[] values = {five.getRegisterC(), five.getRegisterD(), five.getRegisterE(), five.getRegisterF(), five.getRegisterG()};
            return Arrays.copyOf(values, five.getRegisterCount());
        }
        if (instruction instanceof RegisterRangeInstruction range) {
            int[] values = new int[range.getRegisterCount()];
            for (int i = 0; i < values.length; i++) values[i] = range.getStartRegister() + i;
            return values;
        }
        return new int[0];
    }
    private static FieldReference require(Map<String, FieldReference> fields, String key) {
        FieldReference field = fields.get(key);
        if (field == null) throw new IllegalStateException("No saved-state field for " + key);
        return field;
    }
    private static boolean same(FieldReference a, FieldReference b) {
        return a.getDefiningClass().equals(b.getDefiningClass()) && a.getName().equals(b.getName())
                && a.getType().equals(b.getType());
    }
}
