package com.bytetorrent.apng;

import android.os.HandlerThread;
import android.os.Looper;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class FrameDecoderExecutor {
    private static int                      sPoolNumber         = 4;
    private        ArrayList<HandlerThread> mHandlerThreadGroup = new ArrayList<>();
    private        AtomicInteger            counter             = new AtomicInteger(0);

    private FrameDecoderExecutor() {
    }

    static class Inner {
        static final FrameDecoderExecutor sInstance = new FrameDecoderExecutor();
    }

    public void setPoolSize(int size) {
        sPoolNumber = size;
    }

    public static FrameDecoderExecutor getInstance() {
        return Inner.sInstance;
    }

    public Looper getLooper(int taskId) {
        int idx = taskId % sPoolNumber;
        if (idx >= mHandlerThreadGroup.size()) {
            HandlerThread handlerThread = new HandlerThread("FrameDecoderExecutor-" + idx);
            handlerThread.start();

            mHandlerThreadGroup.add(handlerThread);
            Looper looper = handlerThread.getLooper();
            if (looper != null) {
                return looper;
            } else {
                return Looper.getMainLooper();
            }
        } else {
            if (mHandlerThreadGroup.get(idx) != null) {
                Looper looper = mHandlerThreadGroup.get(idx).getLooper();
                if (looper != null) {
                    return looper;
                } else {
                    return Looper.getMainLooper();
                }
            } else {
                return Looper.getMainLooper();
            }
        }
    }

    public int generateTaskId() {
        return counter.getAndIncrement();
    }
}
