package sh.siava.pixelxpert.xposed.utils;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.OutputConfiguration;
import android.hardware.camera2.params.SessionConfiguration;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.Surface;

import androidx.annotation.NonNull;

import java.util.Collections;
import java.util.concurrent.Executor;

import sh.siava.pixelxpert.xposed.utils.toolkit.Logger;

public class HighBrightnessTorch {
    private static HighBrightnessTorch instance;
    private final CameraManager cameraManager;
    private final HandlerThread cameraThread = new HandlerThread("HighBrightnessTorchThread");
    private final Handler cameraHandler;
    private final Executor cameraExecutor;
    private final SurfaceTexture surfaceTexture = new SurfaceTexture(0);
    private final Surface surface = new Surface(surfaceTexture);

    private String cameraId;
    private int maxBrightness = -1;
    private int curBrightness = 0;
    private int desiredBrightness = 0;
    private CameraDevice camera;
    private CameraCaptureSession session;
    private boolean isActivating = false;
    private boolean isOn = false;

    private static final String NS_2020 = "com.google.pixel.experimental2020";
    private static final CaptureRequest.Key<Integer> REQUEST_FLASHLIGHT_BRIGHTNESS =
            new CaptureRequest.Key<>(NS_2020 + ".flashlightBrightness", Integer.TYPE);
    private static final CaptureRequest.Key<Boolean> REQUEST_FLASHLIGHT_BRIGHTNESS_ENABLED =
            new CaptureRequest.Key<>(NS_2020 + ".flashlightBrightnessEnabled", Boolean.TYPE);
    private static final CameraCharacteristics.Key<Integer> CHARACTERISTICS_FLASHLIGHT_BRIGHTNESS_LEVEL_MAX =
            new CameraCharacteristics.Key<>(NS_2020 + ".flashlightBrightnessLevelMax", Integer.TYPE);

    public static void init(CameraManager manager) {
        if (instance == null) {
            Logger.log("Initializing HighBrightnessTorch");
            instance = new HighBrightnessTorch(manager);
        }
    }

    public static HighBrightnessTorch getInstance() {
        return instance;
    }

    private HighBrightnessTorch(CameraManager manager) {
        cameraManager = manager;
        cameraThread.start();
        cameraHandler = new Handler(cameraThread.getLooper());
        cameraExecutor = command -> cameraHandler.post(command);

        try {
            for (String id : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                Integer maxB = characteristics.get(CHARACTERISTICS_FLASHLIGHT_BRIGHTNESS_LEVEL_MAX);
                if (maxB != null && maxB > 0) {
                    cameraId = id;
                    maxBrightness = maxB;
                    Logger.log("Found supported camera ID: " + cameraId + " with max brightness: " + maxBrightness);
                    break;
                }
            }
        } catch (Exception e) {
            Logger.log("Error initializing HighBrightnessTorch", e);
        }
    }

    public boolean isSupported() {
        return cameraId != null;
    }

    public boolean isOn() {
        return isOn;
    }

    public int getMaxBrightness() {
        return maxBrightness;
    }

    public void setTorch(boolean enabled, int brightness) {
        Logger.log("setTorch called: enabled=" + enabled + ", brightness=" + brightness + " (isSupported=" + isSupported() + ")");
        if (!isSupported()) return;
        
        if (!enabled || brightness == 0) {
            desiredBrightness = 0;
            closeCamera();
            return;
        }

        desiredBrightness = Math.min(brightness, maxBrightness);

        if (!isOn && !isActivating) {
            Logger.log("Opening camera...");
            openCamera();
        } else if (isOn) {
            Logger.log("Camera already on, updating brightness to " + desiredBrightness);
            performCapture();
        } else {
            Logger.log("Camera is currently activating, waiting for it to open...");
        }
    }

    private void openCamera() {
        isActivating = true;
        try {
            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice cameraDevice) {
                    Logger.log("Camera opened successfully");
                    if (desiredBrightness == 0) {
                        Logger.log("Desired brightness is 0 by the time camera opened. Closing.");
                        cameraDevice.close();
                        isActivating = false;
                        return;
                    }
                    camera = cameraDevice;
                    createSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice cameraDevice) {
                    Logger.log("Camera disconnected");
                    closeCamera();
                }

                @Override
                public void onError(@NonNull CameraDevice cameraDevice, int error) {
                    Logger.log("Camera error: " + error);
                    closeCamera();
                }
            }, cameraHandler);
        } catch (SecurityException | CameraAccessException e) {
            Logger.log("Failed to open camera", e);
            closeCamera();
        }
    }

    private void createSession() {
        try {
            Logger.log("Creating capture session");
            SessionConfiguration sessionConfiguration = new SessionConfiguration(
                    SessionConfiguration.SESSION_REGULAR,
                    Collections.singletonList(new OutputConfiguration(surface)),
                    cameraExecutor,
                    new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession captureSession) {
                            Logger.log("Capture session configured");
                            if (desiredBrightness == 0) {
                                Logger.log("Desired brightness is 0 by the time session configured. Closing.");
                                captureSession.close();
                                closeCamera();
                                return;
                            }
                            session = captureSession;
                            isActivating = false;
                            isOn = true;
                            performCapture();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession captureSession) {
                            Logger.log("Capture session configure failed");
                            closeCamera();
                        }
                    });
            camera.createCaptureSession(sessionConfiguration);
        } catch (Exception e) {
            Logger.log("Failed to create session", e);
            closeCamera();
        }
    }

    private void performCapture() {
        if (!isOn || session == null) return;
        
        try {
            if (curBrightness != desiredBrightness) {
                Logger.log("Performing capture with brightness: " + desiredBrightness);
                curBrightness = desiredBrightness;
                CaptureRequest.Builder builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                builder.addTarget(surface);
                builder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                builder.set(REQUEST_FLASHLIGHT_BRIGHTNESS_ENABLED, true);
                builder.set(REQUEST_FLASHLIGHT_BRIGHTNESS, curBrightness);
                
                session.capture(builder.build(), null, cameraHandler);
            }
        } catch (Exception e) {
            Logger.log("Failed to perform capture", e);
            closeCamera();
        }
    }

    public void closeCamera() {
        Logger.log("closeCamera called");
        session = null;
        if (camera != null) {
            camera.close();
            camera = null;
        }
        isOn = false;
        isActivating = false;
        curBrightness = 0;
    }
}
