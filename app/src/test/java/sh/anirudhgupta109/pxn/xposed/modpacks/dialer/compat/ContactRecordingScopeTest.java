package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat;

import org.junit.Test;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import static org.junit.Assert.*;

public class ContactRecordingScopeTest {
    @Test public void sharedCollectionLookupsOutsideEligibilityStayUntouched() {
        ContactRecordingScope scope = new ContactRecordingScope();
        assertFalse(scope.includes("synthetic-contact"));
        scope.enter();
        assertTrue(scope.includes("synthetic-contact"));
        for (Object value : new Object[]{null, "", " \t\n", 42}) assertFalse(scope.includes(value));
        scope.leave();
        assertFalse(scope.includes("synthetic-contact"));
    }

    @Test public void nestedEligibilityKeepsOuterScopeAndClearsAfterException() {
        ContactRecordingScope scope = new ContactRecordingScope();
        scope.enter();
        try {
            scope.enter();
            try { throw new IllegalStateException("synthetic failure"); }
            catch (IllegalStateException expected) { assertTrue(scope.includes("synthetic-contact")); }
            finally { scope.leave(); }
            assertTrue(scope.includes("synthetic-contact"));
        } finally { scope.leave(); }
        assertFalse(scope.includes("synthetic-contact"));
    }

    @Test public void suspendedInvocationDoesNotLeakToUnrelatedWorkOrOtherThread() throws Exception {
        ContactRecordingScope scope = new ContactRecordingScope();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            scope.enter();
            assertFalse(executor.submit(() -> scope.includes("synthetic-contact")).get());
            scope.leave(); // A suspended coroutine has returned from its hooked invocation.
            assertFalse(scope.includes("synthetic-contact"));
            assertTrue(executor.submit(() -> {
                scope.enter(); // Resumption on another thread is a new invocation.
                try { return scope.includes("synthetic-contact"); }
                finally { scope.leave(); }
            }).get());
            assertFalse(executor.submit(() -> scope.includes("synthetic-contact")).get());
            assertFalse(scope.includes("synthetic-contact"));
        } finally { executor.shutdownNow(); }
    }
}
