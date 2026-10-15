package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import io.github.libxposed.api.XposedInterface;
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger;

/** Version-pinned override of selected-contact membership during recording eligibility. */
public final class AllContactsRecordingCompat {
    private static final long SUPPORTED_VERSION = 20129568L; // 241.0.991235506-pixel
    private AllContactsRecordingCompat() {}

    public static void install(Context context, ClassLoader loader, XposedInterface xposed) {
        List<XposedInterface.HookHandle> hooks = new ArrayList<>();
        try {
            long version = context.getPackageManager()
                    .getPackageInfo("com.google.android.dialer", 0).getLongVersionCode();
            if (version != SUPPORTED_VERSION) {
                unavailable(context, "unsupported Phone version " + version);
                return;
            }
            Class<?> continuation = Class.forName("aqrq", false, loader);
            Method eligibility = Class.forName("phj", false, loader).getDeclaredMethod("b", continuation);
            Method contains = Class.forName("aquk", false, loader)
                    .getDeclaredMethod("cD", Iterable.class, Object.class);
            if (eligibility.getReturnType() != Object.class || contains.getReturnType() != boolean.class
                    || Modifier.isStatic(eligibility.getModifiers()) || !Modifier.isStatic(contains.getModifiers())) {
                throw new IllegalStateException("Unexpected eligibility method shape");
            }
            if (!xposed.deoptimize(eligibility)) throw new IllegalStateException("Method deoptimization failed");
            ContactRecordingScope scope = new ContactRecordingScope();
            hooks.add(xposed.hook(contains).intercept(chain ->
                    scope.includes(chain.getArg(1)) ? true : chain.proceed()));
            verifyMembershipHook(contains, scope);
            hooks.add(xposed.hook(eligibility).intercept(chain -> {
                scope.enter();
                try { return chain.proceed(); }
                finally { scope.leave(); } // Also clears on failure or coroutine suspension.
            }));
            Logger.log("PixelXpert: all-contacts recording adapter active for Phone " + version);
        } catch (Throwable error) {
            for (XposedInterface.HookHandle hook : hooks) hook.unhook();
            unavailable(context, "adapter initialization failed: " + error.getClass().getSimpleName());
        }
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
        Logger.log("PixelXpert: all-contacts recording adapter inactive: " + reason);
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context,
                "PixelXpert all-contacts recording needs a compatibility update for this Phone version. "
                + "Automatic recording uses Google's selected contacts.", Toast.LENGTH_LONG).show());
    }
}
