package io.github.hankaviator.gappuccino;

import java.io.File;
import java.util.*;
import org.jf.dexlib2.*;
import org.jf.dexlib2.iface.*;
import org.jf.dexlib2.iface.instruction.ReferenceInstruction;
import org.jf.dexlib2.iface.reference.*;

/** Find the file-type confirmation through its histogram and callback contract. */
final class ChromeDownloadDiscovery {
    static final String CALLBACK = "Lorg/chromium/base/Callback;";
    static final String MARKER = "Download.DangerousDialog.Events";
    enum Result { BOOLEAN, INTEGER }
    record Mapping(String owner, String method, List<String> parameters, int callback, int dangerous, Result result) {}

    static Mapping resolve(List<File> apks) throws Exception {
        Map<String, ClassDef> classes = new HashMap<>();
        for (File apk : apks) {
            var dex = DexFileFactory.loadDexContainer(apk, Opcodes.getDefault());
            for (String entry : dex.getDexEntryNames())
                for (var type : dex.getEntry(entry).getDexFile().getClasses()) classes.put(type.getType(), type);
        }
        return resolveClasses(classes);
    }
    static Mapping resolveClasses(Map<String, ? extends ClassDef> classes) {
        List<Mapping> found = new ArrayList<>();
        for (ClassDef type : classes.values()) for (Method method : type.getMethods()) {
            List<String> parameters = new ArrayList<>();
            for (CharSequence parameter : method.getParameterTypes()) parameters.add(parameter.toString());
            if (!method.getReturnType().equals("V") || !shape(parameters) || method.getImplementation() == null) continue;
            boolean marker = false;
            Set<String> constructed = new HashSet<>();
            for (var ins : method.getImplementation().getInstructions()) if (ins instanceof ReferenceInstruction r) {
                if (r.getReference() instanceof StringReference s && s.getString().equals(MARKER)) marker = true;
                if (ins.getOpcode() == Opcode.NEW_INSTANCE && r.getReference() instanceof TypeReference t) constructed.add(t.getType());
            }
            if (!marker) continue;
            Set<Result> results = new HashSet<>();
            for (String owner : constructed) {
                ClassDef controller = classes.get(owner);
                if (controller == null) continue;
                for (Method callback : controller.getMethods()) {
                    if (callback.getImplementation() == null) continue;
                    boolean callsResult = false;
                    Set<Result> boxing = new HashSet<>();
                    for (var ins : callback.getImplementation().getInstructions())
                        if (ins instanceof ReferenceInstruction r && r.getReference() instanceof MethodReference m) {
                            if (m.getDefiningClass().equals(CALLBACK) && m.getName().equals("onResult")
                                    && m.getParameterTypes().equals(List.of("Ljava/lang/Object;"))) callsResult = true;
                            if (m.getName().equals("valueOf")) {
                                if (m.getDefiningClass().equals("Ljava/lang/Integer;") && m.getParameterTypes().equals(List.of("I"))) boxing.add(Result.INTEGER);
                                if (m.getDefiningClass().equals("Ljava/lang/Boolean;") && m.getParameterTypes().equals(List.of("Z"))) boxing.add(Result.BOOLEAN);
                            }
                        }
                    if (callsResult) results.addAll(boxing);
                }
            }
            if (results.size() != 1) continue;
            int cb = parameters.indexOf(CALLBACK);
            found.add(new Mapping(type.getType(), method.getName(), parameters, cb,
                    parameters.get(parameters.size()-1).equals("Z") ? parameters.size()-1 : -1, results.iterator().next()));
        }
        if (found.size() != 1) throw new IllegalStateException("Ambiguous Chrome confirmation contract: " + found.size());
        return found.get(0);
    }
    static boolean shape(List<String> p) {
        if (p.size() < 6 || p.size() > 8 || !(p.get(0).equals("Landroid/content/Context;") || p.get(0).equals("Landroid/app/Activity;"))
                || !p.get(1).startsWith("L") || !p.get(2).equals("Ljava/lang/String;") || !p.get(3).equals("J")
                || !p.get(4).equals("Ljava/lang/String;")) return false;
        int next = 5;
        if (p.get(next).equals("I")) next++;
        if (next >= p.size() || !p.get(next++).equals(CALLBACK)) return false;
        return next == p.size() || next == p.size()-1 && p.get(next).equals("Z");
    }
    static Object accepted(Result result) {
        // Chromium's persisted DangerousDownloadDialogEvent.CONFIRM is 1.
        if (result == Result.INTEGER) return Integer.valueOf(1);
        return Boolean.TRUE;
    }
    private ChromeDownloadDiscovery() {}
}
