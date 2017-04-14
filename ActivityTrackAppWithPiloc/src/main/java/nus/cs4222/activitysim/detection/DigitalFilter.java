package nus.cs4222.activitysim.detection;

public class DigitalFilter {
    private int mNumRegisters = 0;
    private float[] mSamples;
    private float mSum = 0.0f;

    public DigitalFilter(int numRegisters) {
        mNumRegisters = numRegisters;
        mSamples = new float[numRegisters];
    }

    public float putSample(float sample) {
        mSum = mSum - mSamples[mNumRegisters - 1] + sample;
        for (int i = mNumRegisters - 1; i > 0; i--) {
            mSamples[i] = mSamples[i - 1];
        }
        mSamples[0] = sample;
        return mSum;
    }

    public float getOutput() {
        return mSum;
    }
}
