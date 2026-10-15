package sh.anirudhgupta109.pxn.xposed.modpacks.dialer.compat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import io.github.libxposed.api.XposedInterface;
import sh.anirudhgupta109.pxn.xposed.utils.toolkit.Logger;

/** Explicit, fail-closed adapter for Google Phone's obfuscated Fermat recording implementation. */
public final class FermatRecordingCompat {
    public static final long SUPPORTED_VERSION = 20129568L; // 241.0.991235506-pixel
    private static final String PHONE = "com.google.android.dialer";
    private FermatRecordingCompat() {}

    public static void install(Context context, ClassLoader loader,
            BooleanSupplier removeAnnouncement, XposedInterface xposed) {
        List<XposedInterface.HookHandle> hooks = new ArrayList<>();
        try {
            long version = context.getPackageManager().getPackageInfo(PHONE, 0).getLongVersionCode();
            if (version != SUPPORTED_VERSION) {
                if (removeAnnouncement.getAsBoolean()) {
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
            for (Method method : promptMethods) {
                if (!xposed.deoptimize(method)) throw new IllegalStateException("Method deoptimization failed");
            }

            for (Method method : promptMethods) {
                hooks.add(xposed.hook(method).intercept(chain -> {
                    if (removeAnnouncement.getAsBoolean()
                            && recordingFeature.isInstance(feature.get(chain.getThisObject()))) {
                        // Startup resets the field, so select the native skip branch at
                        // each prompt entry instead of only changing the constructor.
                        playPrompts.setBoolean(chain.getThisObject(), false);
                    }
                    return chain.proceed();
                }));
            }

            Logger.log("PixelXpert: Fermat recording adapter active for Phone " + version);
        } catch (Throwable error) {
            for (XposedInterface.HookHandle hook : hooks) hook.unhook();
            if (removeAnnouncement.getAsBoolean()) {
                unavailable(context, "adapter initialization failed: " + error.getClass().getSimpleName());
            }
        }
    }

    private static void requireReturn(Method method, Class<?> expected) {
        if (method.getReturnType() != expected) throw new IllegalStateException("Unexpected method shape");
    }

    private static void unavailable(Context context, String reason) {
        Logger.log("PixelXpert: Fermat recording adapter inactive: " + reason);
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(context,
                "PixelXpert announcement removal needs a compatibility update for this Phone version.", Toast.LENGTH_LONG).show());
    }
}
