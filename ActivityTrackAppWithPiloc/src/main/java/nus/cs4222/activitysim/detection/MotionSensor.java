package nus.cs4222.activitysim.detection;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

public class MotionSensor {
    private static final int WINDOW_SIZE = 200;
    private static final float THRESH = 0.5f;

    public enum State {
        IDLE, MOVING
    }

    private State mState;
    private Queue<Integer> mSampleMarkers;
    private int mNumActiveSamples = 0;

    public MotionSensor() {
        mSampleMarkers = new ArrayBlockingQueue<>(WINDOW_SIZE);
        for (int i = 0; i < WINDOW_SIZE; i++) {
            mSampleMarkers.add(0);
        }
    }

    public void putLinAccelSample(float x, float y, float z) {
        x = Math.abs(x);
        y = Math.abs(y);
        z = Math.abs(z);
        float maxChange = Math.max(Math.max(x, y), z);

        mNumActiveSamples -= mSampleMarkers.poll();

        if (maxChange >= THRESH) {
            mSampleMarkers.add(1);
            mNumActiveSamples++;
        } else {
            mSampleMarkers.add(0);
        }

        float proportionActive = (float)mNumActiveSamples / WINDOW_SIZE;
        if (proportionActive > 0.7) {
            mState = State.MOVING;
        } else if (proportionActive < 0.3){
            mState = State.IDLE;
        }
    }

    public State getMotionState() {
        return mState;
    }
}
