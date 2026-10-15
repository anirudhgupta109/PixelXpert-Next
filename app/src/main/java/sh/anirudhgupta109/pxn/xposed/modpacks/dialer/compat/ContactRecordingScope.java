package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat;

/** Limits a shared collection helper hook to one synchronous eligibility invocation. */
public final class ContactRecordingScope {
    private final ThreadLocal<Integer> depth = ThreadLocal.withInitial(() -> 0);

    public void enter() { depth.set(depth.get() + 1); }

    public void leave() {
        int remaining = depth.get() - 1;
        if (remaining <= 0) depth.remove();
        else depth.set(remaining);
    }

    public boolean includes(Object candidate) {
        return depth.get() > 0 && candidate instanceof String && !((String) candidate).trim().isEmpty();
    }
}
