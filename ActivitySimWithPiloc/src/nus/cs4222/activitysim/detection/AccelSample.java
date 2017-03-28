package nus.cs4222.activitysim.detection;

public class AccelSample {
    public long timestamp;
    public float x;
    public float y;
    public float z;

    public AccelSample(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public AccelSample(long timestamp, float x, float y, float z) {
        this(x, y, z);
        this.timestamp = timestamp;
    }

    public float getComponent(int axis) {
        switch (axis) {
            case 0:
                return x;
            case 1:
                return y;
            case 2:
                return z;
            default:
                return 0;
        }
    }
}
