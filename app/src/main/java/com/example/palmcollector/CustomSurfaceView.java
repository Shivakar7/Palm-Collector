package com.example.palmcollector;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.View;

import com.google.mediapipe.solutioncore.ImageSolutionResult;
import com.google.mediapipe.solutioncore.ResultGlRenderer;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceViewRenderer;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class CustomSurfaceView<T extends ImageSolutionResult> extends GLSurfaceView {
    private static final String TAG = "SolutionGlSurfaceView";
    SolutionGlSurfaceViewRenderer<T> renderer = new SolutionGlSurfaceViewRenderer();

    public void setSolutionResultRenderer(ResultGlRenderer<T> resultRenderer) {
        this.renderer.setSolutionResultRenderer(resultRenderer);
    }

    public void setRenderData(T solutionResult) {
        this.renderer.setRenderData(solutionResult, false);
    }

    public void setRenderData(T solutionResult, boolean produceTextureFrames) {
        this.renderer.setRenderData(solutionResult, produceTextureFrames);
    }

    public void setRenderInputImage(boolean renderInputImage) {
        this.renderer.setRenderInputImage(renderInputImage);
    }

    public CustomSurfaceView(Context context, EGLContext glContext, int glMajorVersion) {
        super(context);
        this.setEGLContextClientVersion(glMajorVersion);
        this.getHolder().addCallback(new DummyHolderCallback());
        this.setEGLContextFactory(new EGLContextFactory() {
            public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig) {
                int[] contextAttrs = new int[]{12440, glMajorVersion, 12344};
                return egl.eglCreateContext(display, eglConfig, glContext, contextAttrs);
            }

            public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context) {
                if (!egl.eglDestroyContext(display, context)) {
                    throw new RuntimeException("eglDestroyContext failed");
                }
            }
        });
        this.renderer.setTextureTarget(3553);
        super.setRenderer(this.renderer);
        this.setRenderMode(0);
        this.setVisibility(View.VISIBLE);
    }

    private class DummyHolderCallback implements Callback {
        private DummyHolderCallback() {
        }

        public void surfaceCreated(SurfaceHolder holder) {
            Log.d("SolutionGlSurfaceView", "main surfaceCreated");
        }

        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            Log.d("SolutionGlSurfaceView", String.format("main surfaceChanged. width: %d height: %d glViewWidth: %d glViewHeight: %d", width, height, CustomSurfaceView.this.getWidth(), CustomSurfaceView.this.getHeight()));
        }

        public void surfaceDestroyed(SurfaceHolder holder) {
            Log.d("SolutionGlSurfaceView", "main surfaceDestroyed");
        }
    }
}