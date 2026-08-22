package com.xperiatk.lightart;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.widget.Toast;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

final class ArtGLSurfaceView extends GLSurfaceView {
    private final ArtRenderer artRenderer;

    ArtGLSurfaceView(Context context, String script) {
        super(context);
        setEGLContextClientVersion(2);
        setPreserveEGLContextOnPause(true);
        artRenderer = new ArtRenderer(context, script);
        setRenderer(artRenderer);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    void setPaused(boolean paused) {
        artRenderer.paused = paused;
        setRenderMode(paused ? GLSurfaceView.RENDERMODE_WHEN_DIRTY : GLSurfaceView.RENDERMODE_CONTINUOUSLY);
        if (paused) requestRender();
    }
    boolean isPaused() { return artRenderer.paused; }
    void resetAnimation() { queueEvent(artRenderer::resetTime); }
    void setSpeedScale(float scale) { queueEvent(() -> artRenderer.setSpeedScale(scale)); }
    void release() { queueEvent(artRenderer::release); }

    private static final class ArtRenderer implements Renderer {
        private final Context context;
        private long engine;
        private int width;
        private int height;
        private boolean errorShown;
        volatile boolean paused;

        ArtRenderer(Context context, String script) {
            this.context = context.getApplicationContext();
            engine = NativeArt.createEngine(script);
        }

        @Override public void onSurfaceCreated(GL10 gl, EGLConfig config) {
            if (engine != 0) NativeArt.initGl(engine);
        }

        @Override public void onSurfaceChanged(GL10 gl, int width, int height) {
            this.width = width;
            this.height = height;
        }

        @Override public void onDrawFrame(GL10 gl) {
            if (engine == 0) return;
            String error = NativeArt.getError(engine);
            if (error != null && !error.isEmpty()) {
                if (!errorShown) {
                    errorShown = true;
                    new Handler(context.getMainLooper()).post(() -> Toast.makeText(context, error, Toast.LENGTH_LONG).show());
                }
                return;
            }
            if (!paused) NativeArt.render(engine, width, height);
        }

        void resetTime() { if (engine != 0) NativeArt.resetTime(engine); }
        void setSpeedScale(float scale) { if (engine != 0) NativeArt.setSpeedScale(engine, scale); }

        void release() {
            if (engine != 0) {
                NativeArt.destroyEngine(engine);
                engine = 0;
            }
        }
    }
}
