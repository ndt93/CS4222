package nus.cs4222.activitysim.detection;

public class StepCounter {
    private static final String TAG = StepCounter.class.getName();
    private static final int DEFAULT_THRESH_UPDATE_PERIOD = 50;

    enum State {
        WARMING_UP, READY
    }

    private State mState;

    private DigitalFilter[] mDigitalFilters = new DigitalFilter[3];
    private ShiftRegisters[] mShiftRegisters = new ShiftRegisters[3];

    private int mThresholdsUpdatePeriod;
    private int mSamplingCounter = 0;

    private float[] mMax = new float[3];
    private float[] mMin = new float[3];
    private float[] mThresholds = new float[3];

    private static final float MIN_CHANGE = 1f;

    private int mNumSteps = 0;
    private long mLastStepTime = 0L;

    public StepCounter() {
        this(DEFAULT_THRESH_UPDATE_PERIOD);
    }

    public StepCounter(int thresholdsUpdatePeriod) {
        mThresholdsUpdatePeriod = thresholdsUpdatePeriod;

        for (int i = 0; i < 3; i++) {
            mDigitalFilters[i] = new DigitalFilter(4);
            mShiftRegisters[i] = new ShiftRegisters(MIN_CHANGE);
        }

        resetMinMax();

        mState = State.WARMING_UP;
    }

    public void putAcclSample(AccelSample sample) {
        float filteredX = mDigitalFilters[0].putSample(sample.x);
        float filteredY = mDigitalFilters[1].putSample(sample.y);
        float filteredZ = mDigitalFilters[2].putSample(sample.z);
        AccelSample sampleResult = new AccelSample(sample.timestamp, filteredX, filteredY, filteredZ);

        updateMinMax(sampleResult);

        mSamplingCounter++;

        if (mSamplingCounter == mThresholdsUpdatePeriod) {
            updateThresholds();
            resetMinMax();
            mSamplingCounter = 0;
            mState = State.READY;
        }

        updateShiftRegisters(sampleResult);

        if (mState == State.READY) {
            if (doStepDetection()) {
                mNumSteps++;
                mLastStepTime = sample.timestamp;
            }
        }
    }

    public int getStepsCount() {
        return mNumSteps;
    }

    public long getLastStepTime() {
        return mLastStepTime;
    }

    private boolean doStepDetection() {
        int mostActiveAxis = -1;
        float maxChange = 0.0f;
        for (int i = 0; i < 3; i++) {
            if (mShiftRegisters[i].getChange() > maxChange) {
                maxChange = mShiftRegisters[i].getChange();
                mostActiveAxis = i;
            }
        }

        if (maxChange < MIN_CHANGE) {
            return false;
        }

        if (mShiftRegisters[mostActiveAxis].mSampleOld > mThresholds[mostActiveAxis] &&
            mShiftRegisters[mostActiveAxis].mSampleNew < mThresholds[mostActiveAxis]) {
            return true;
        }

        return false;
    }

    private void resetMinMax() {
        for (int i = 0; i < 3; i++) {
            mMax[i] = -Float.MAX_VALUE;
            mMin[i] = Float.MAX_VALUE;
        }
    }

    private void updateShiftRegisters(AccelSample sample) {
        for (int i = 0; i < 3; i++) {
            mShiftRegisters[i].push(sample.getComponent(i));
        }
    }

    private void updateThresholds() {
        for (int i = 0; i < 3; i++) {
            mThresholds[i] = (mMax[i] + mMin[i]) / 2;
        }
    }

    private void updateMinMax(AccelSample sample) {
        for (int i = 0; i < 3; i++) {
            if (sample.getComponent(i) > mMax[i]) {
                mMax[i] = sample.getComponent(i);
            }
            if (sample.getComponent(i) < mMin[i]) {
                mMin[i] = sample.getComponent(i);
            }
        }
    }

    private class ShiftRegisters {
        public float mSampleNew;
        public float mSampleOld;
        private float mPrecision;

        private boolean mIsLastPushSuccessful = false;

        public ShiftRegisters(float precision) {
            mPrecision = precision;
        }

        /**
         * Pushes a new sample into the registers. Succeeds if the difference
         * between the new sample and the last pushed sample is more than {@code precision}.
         */
        public void push(float sample) {
            mSampleOld = mSampleNew;
            if (Math.abs(sample - mSampleNew) <= mPrecision) {
                mIsLastPushSuccessful = false;
            }
            mSampleNew = sample;
            mIsLastPushSuccessful = true;
        }

        public float getChange() {
            if (!mIsLastPushSuccessful) {
                return 0;
            }
            return Math.abs(mSampleNew - mSampleOld);
        }
    }

}
