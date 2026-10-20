package io.github.hankaviator.gappuccino;

import java.io.File;
import java.util.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.*;

/** Locate Keep's confirmation fragment through its argument contract. */
final class KeepDeleteDiscovery {
    record Mapping(String owner, String createDialog, String arguments) {}
    static Mapping resolve(File apk) throws Exception {
        var dex = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault());
        List<Mapping> found = new ArrayList<>();
        for (String entry : dex.getDexEntryNames()) for (var type : dex.getEntry(entry).getDexFile().getClasses()) {
            Set<String> strings = new HashSet<>();
            for (var method : type.getMethods()) {
                if (!method.getName().equals("<clinit>") || method.getImplementation() == null) continue;
                for (var ins : method.getImplementation().getInstructions())
                    if (ins instanceof ReferenceInstruction r && r.getReference() instanceof StringReference s) strings.add(s.getString());
            }
            if (!strings.containsAll(Set.of("_requestCode", "_positiveTextId", "_displayOptions"))) continue;
            for (var method : type.getMethods()) {
                if (!method.getReturnType().equals("Landroid/app/Dialog;")
                        || !method.getParameterTypes().equals(List.of("Landroid/os/Bundle;"))
                        || method.getImplementation() == null) continue;
                Set<String> fields = new HashSet<>();
                for (var ins : method.getImplementation().getInstructions())
                    if (ins instanceof ReferenceInstruction r && r.getReference() instanceof FieldReference f
                            && f.getType().equals("Landroid/os/Bundle;") && ins.getOpcode().name.startsWith("iget")) fields.add(f.getName());
                if (fields.size() == 1) found.add(new Mapping(type.getType(), method.getName(), fields.iterator().next()));
            }
        }
        if (found.size() != 1) throw new IllegalStateException("Ambiguous Keep confirmation contract: " + found.size());
        return found.get(0);
    }
    static boolean ordinaryTrash(String request) {
        return "request_trash_note".equals(request) || "confirm_trash_notes".equals(request);
    }
    private KeepDeleteDiscovery() {}
}
