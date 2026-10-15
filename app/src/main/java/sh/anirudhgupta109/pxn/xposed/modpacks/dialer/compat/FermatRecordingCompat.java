package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;

/** Explicit, fail-closed adapter for Google Phone's obfuscated Fermat recording implementation. */
public final class FermatRecordingCompat {
    public static final long SUPPORTED_VERSION = 20129568L; // 241.0.991235506-pixel
    private static final String PHONE = "com.google.android.dialer";
    private FermatRecordingCompat() {}

    public static void install(Context context, ClassLoader loader,
            BooleanSupplier removeAnnouncement, BooleanSupplier allContacts) {
        List<XC_MethodHook.Unhook> hooks = new ArrayList<>();
        try {
            long version = context.getPackageManager().getPackageInfo(PHONE, 0).getLongVersionCode();
            if (version != SUPPORTED_VERSION) {
                if (removeAnnouncement.getAsBoolean() || allContacts.getAsBoolean()) {
                    unavailable(context, "unsupported Phone version " + version);
                }
                return;
            }
            // Resolve and validate everything before installing any hooks. These names are
            // specific to the version above; never guess mappings on a different release.
            Class<?> continuation = Class.forName("aqrq", false, loader);
            Class<?> session = Class.forName("owh", false, loader);
            Class<?> featureInterface = Class.forName("pfj", false, loader);
            Class<?> recordingFeature = Class.forName("pfx", false, loader);
            Field feature = session.getDeclaredField("q");
            Field playPrompts = session.getDeclaredField("k");
            feature.setAccessible(true);
            playPrompts.setAccessible(true);
            if (feature.getType() != featureInterface || playPrompts.getType() != boolean.class
                    || !featureInterface.isAssignableFrom(recordingFeature)) {
                throw new IllegalStateException("Unexpected recording session shape");
            }
            Method[] promptMethods = {
                session.getDeclaredMethod("d", continuation), // Before-disclaimer countdown
                session.getDeclaredMethod("f", continuation), // Starting disclaimer
                session.getDeclaredMethod("e", continuation)  // Ending disclaimer
            };
            for (Method method : promptMethods) requireReturn(method, Object.class);
            Method eligibility = Class.forName("phj", false, loader).getDeclaredMethod("b", continuation);
            Method contains = Class.forName("aquk", false, loader)
                    .getDeclaredMethod("cD", Iterable.class, Object.class);
            requireReturn(eligibility, Object.class);
            requireReturn(contains, boolean.class);
            Method deoptimize = XposedBridge.class.getMethod("deoptimizeMethod", Member.class);
            for (Method method : promptMethods) deoptimizeChecked(deoptimize, method);
            deoptimizeChecked(deoptimize, eligibility); // Keep the collection lookup from being inlined.

            for (Method method : promptMethods) {
                hooks.add(XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        if (removeAnnouncement.getAsBoolean()
                                && recordingFeature.isInstance(feature.get(param.thisObject))) {
                            // Session.start resets this field, so changing it only in the
                            // constructor does not work. At each prompt entry, select Google's
                            // native skip branch: true / PLAYBACK_SKIPPED / Unit respectively.
                            // No fake audio, global TTS interception or recording-state shortcut.
                            playPrompts.setBoolean(param.thisObject, false);
                        }
                    }
                }));
            }

            ContactRecordingScope scope = new ContactRecordingScope();
            hooks.add(XposedBridge.hookMethod(contains, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (scope.includes(param.args[1])) param.setResult(true);
                }
            }));
            verifyMembershipHook(contains, scope);
            hooks.add(XposedBridge.hookMethod(eligibility, new XC_MethodHook() {
                @Override protected void beforeHookedMethod(MethodHookParam param) {
                    if (allContacts.getAsBoolean()) {
                        scope.enter();
                        param.setObjectExtra("pxnContactScopeEntered", true);
                    }
                }
                @Override protected void afterHookedMethod(MethodHookParam param) {
                    // afterHookedMethod also runs on exceptions and coroutine suspension.
                    // Every resumed invocation establishes a fresh scope on its own thread.
                    if (Boolean.TRUE.equals(param.getObjectExtra("pxnContactScopeEntered"))) scope.leave();
                }
            }));
            XposedBridge.log("PixelXpert: Fermat recording adapter active for Phone " + version);
        } catch (Throwable error) {
            for (XC_MethodHook.Unhook hook : hooks) hook.unhook();
            if (removeAnnouncement.getAsBoolean() || allContacts.getAsBoolean()) {
                unavailable(context, "adapter initialization failed: " + error.getClass().getSimpleName());
            }
        }
    }

    private static void deoptimizeChecked(Method deoptimize, Method target) throws Exception {
        if (Boolean.FALSE.equals(deoptimize.invoke(null, target))) {
            throw new IllegalStateException("Method deoptimization failed");
        }
    }

    private static void requireReturn(Method method, Class<?> expected) {
        if (method.getReturnType() != expected) throw new IllegalStateException("Unexpected method shape");
    }

    private static void verifyMembershipHook(Method contains, ContactRecordingScope scope) throws Exception {
        // Exercise the real installed hook with synthetic values, never with contacts.
        Object[] invalid = {"", "  ", null, Integer.valueOf(1)};
        if ((Boolean) contains.invoke(null, Collections.emptySet(), "synthetic-contact")) {
            throw new IllegalStateException("Membership override leaked outside recording eligibility");
        }
        scope.enter();
        try {
            if (!(Boolean) contains.invoke(null, Collections.emptySet(), "synthetic-contact")) {
                throw new IllegalStateException("Membership hook is ineffective");
            }
            for (Object candidate : invalid) {
                if ((Boolean) contains.invoke(null, Collections.emptySet(), candidate)) {
                    throw new IllegalStateException("Invalid contact candidate included");
                }
            }
        } finally { scope.leave(); }
        if ((Boolean) contains.invoke(null, Collections.emptySet(), "synthetic-contact")) {
            throw new IllegalStateException("Membership scope was not cleared");
        }
    }

    private static void unavailable(Context context, String reason) {
        XposedBridge.log("PixelXpert: Fermat recording adapter inactive: " + reason);
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context,
                "PixelXpert recording tweaks need a compatibility update for this Phone version. "
                + "Automatic recording uses Google's selected contacts.", Toast.LENGTH_LONG).show());
    }
}
