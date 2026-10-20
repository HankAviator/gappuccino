package io.github.hankaviator.gappuccino;

import org.junit.Test;
import static org.junit.Assert.*;

public class KeepDeleteDiscoveryTest {
    @Test public void onlyOrdinaryTrashRequestsAreAccepted() {
        assertTrue(KeepDeleteDiscovery.ordinaryTrash("request_trash_note"));
        assertTrue(KeepDeleteDiscovery.ordinaryTrash("confirm_trash_notes"));
        for (String request : new String[]{null, "delete_notes", "delete_shared_notes", "request_delete_shared_note", "delete_label", "delete_task", ""})
            assertFalse(KeepDeleteDiscovery.ordinaryTrash(request));
    }
}
