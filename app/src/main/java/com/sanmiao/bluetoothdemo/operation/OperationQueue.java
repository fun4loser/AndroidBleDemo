package com.sanmiao.bluetoothdemo.operation;

import android.os.Handler;

import java.util.ArrayDeque;

/**
 * @author 刚桥恕
 * - 操作队列
 * 每次从队列中拿出一个来执行，获得结果或者操作超时后，执行下一个
 * @date 2016/11/19 0019 10:34.
 */

public class OperationQueue {

    private Handler mHandler;
    private ArrayDeque<Operation> mOperationQueue = new ArrayDeque<>();
    private boolean mRunning = false;
    private Operation mOperationNow;
    private Runnable mTimeOutRunNext = new Runnable() {
        @Override
        public void run() {
            runNext();
        }
    };

    public OperationQueue(Handler handler) {
        mHandler = handler;
    }

    public void add(Operation operation) {
        mOperationQueue.add(operation);
        if (!mRunning) {
            start();
        }
    }

    public void satisfy(int type, String address) {
        if (mRunning && (type == mOperationNow.SATISFY_TYPE) && address.equals(mOperationNow.ID)) {
            mHandler.removeCallbacks(mTimeOutRunNext);
            runNext();
        }
    }

    private void runNext() {
        while (!mOperationQueue.isEmpty()) {
            mOperationNow = mOperationQueue.poll();
            if (mOperationNow.operate()) {
                mHandler.postDelayed(mTimeOutRunNext, mOperationNow.TIME_OUT);
                break;
            }
        }
        mRunning = !mOperationQueue.isEmpty();
    }

    private void start() {
        mRunning = true;
        runNext();
    }
}
