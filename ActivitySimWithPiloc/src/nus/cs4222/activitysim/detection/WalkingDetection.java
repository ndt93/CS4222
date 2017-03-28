package nus.cs4222.activitysim.detection;

public class WalkingDetection {
    private static final String TAG = WalkingDetection.class.getName();

    enum State {
        WARMING_UP, READY
    }

    private State mState;

    private DigitalFilter[] mDigitalFilters = new DigitalFilter[3];

    private int mThresholdsUpdatePeriod;
    private int mSamplingCounter = 0;

    private float[] mMax = new float[3];
    private float[] mMin = new float[3];
    private float[] mThresholds = new float[3];

    public WalkingDetection(int thresholdsUpdatePeriod) {
        mThresholdsUpdatePeriod = thresholdsUpdatePeriod;

        for (int i = 0; i < 3; i++) {
            mDigitalFilters[i] = new DigitalFilter(4);
            mMax[i] = Float.MIN_VALUE;
            mMin[i] = Float.MAX_VALUE;
        }

        mState = State.WARMING_UP;
    }

    public void putAcclSample(AccelSample sample) {
        float filteredX = mDigitalFilters[0].putSample(sample.x);
        float filteredY = mDigitalFilters[1].putSample(sample.y);
        float filteredZ = mDigitalFilters[2].putSample(sample.z);
        AccelSample sampleResult = new AccelSample(sample.timestamp, filteredX, filteredY, filteredZ);

        updateMinMax(sampleResult);

        mSamplingCounter++;

        if (mSamplingCounter == 50) {
            updateThresholds();
            mSamplingCounter = 0;
            mState = State.READY;
        }

        if (mState == State.READY) {

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
        private float mSampleNew;
        private float mSampleOld;
        private float mPrecision;

        public ShiftRegisters(float precision) {
            mPrecision = precision;
        }

        /**
         * Pushes a new sample into the registers. Succeeds if the difference
         * between the new sample and the last pushed sample is more than {@code precision}.
         * @param sample
         * @return true of the push succeed
         */
        public boolean push(float sample) {
            mSampleOld = mSampleNew;
            if (Math.abs(sample - mSampleNew) <= mPrecision) {
                return false;
            }
            mSampleNew = sample;
            return true;
        }

        public float getChange() {
            return mSampleNew - mSampleOld;
        }
    }

}
