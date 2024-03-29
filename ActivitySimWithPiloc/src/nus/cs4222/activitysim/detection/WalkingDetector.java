package nus.cs4222.activitysim.detection;

public class WalkingDetector {
    private static final String TAG = StepCounter.class.getName();
    private static final int MIN_CONTINUOUS_STEPS = 20;
    private static final int MAX_IDLE = 5000;
    private static final int WINDOW_LOW = 200;
    private static final int WINDOW_HIGH = 2000;

    private StepCounter mStepCounter;

    private int mLastValidStepCount = 0;
    private long mLastValidStepTime = 0L;
    private int mNumContinuousSteps = 0;

    public enum State {
        NOT_WALKING,
        MAYBE,
        WALKING
    }

    public WalkingDetector() {
        mStepCounter = new StepCounter();
    }

    public void doDetection(AccelSample sample) {
        mStepCounter.putAcclSample(sample);

        if (mStepCounter.getStepsCount() == mLastValidStepCount) {
            if (sample.timestamp > mLastValidStepTime + MAX_IDLE) {
                mNumContinuousSteps = 0;
            } else if (sample.timestamp > mLastValidStepTime + WINDOW_HIGH) {
                mNumContinuousSteps = 1;
            }
        } else if (mStepCounter.getLastStepTime() < mLastValidStepTime + WINDOW_LOW) {
            return;
        } else {
            if (mStepCounter.getLastStepTime() > mLastValidStepTime + WINDOW_HIGH) {
                mNumContinuousSteps = 1;
            } else if (mStepCounter.getLastStepTime() >= mLastValidStepTime + WINDOW_LOW) {
                mNumContinuousSteps++;
            }
            mLastValidStepCount = mStepCounter.getStepsCount();
            mLastValidStepTime = mStepCounter.getLastStepTime();
        }
    }

    public State getWalkingState() {
        if (mNumContinuousSteps == 0) {
            return State.NOT_WALKING;
        }
        if (mNumContinuousSteps > MIN_CONTINUOUS_STEPS) {
            return State.WALKING;
        }
        return State.MAYBE;
    }
}
