package nus.cs4222.activitysim.detection;

import nus.cs4222.activitysim.DataStructure;
import nus.cs4222.activitysim.PilocApi;

import java.util.Calendar;
import java.util.Vector;

import static nus.cs4222.activitysim.detection.EnvDetector.Loc.COM1;
import static nus.cs4222.activitysim.detection.EnvDetector.Loc.OTHER;

public class EnvDetector {
    public enum State {
        INDOOR, OUTDOOR
    }

    public enum Loc {
        COM1, OTHER
    }

    private static float EVENING_LIGHT_THRESH = 550;
    private static float MORNING_LIGHT_THRESH = 1500;

    private State mState;
    private Loc mLoc;

    private Calendar mCalendar;
    private PilocApi mPilocApi;

    public EnvDetector(PilocApi pilocApi) {
        mCalendar = Calendar.getInstance();
        mPilocApi = pilocApi;
    }

    public void putLightSample(long timestamp, float light) {
        float thresh = MORNING_LIGHT_THRESH;
        mCalendar.setTimeInMillis(timestamp);
        if (mCalendar.get(Calendar.HOUR_OF_DAY) >= 18) {
            thresh = EVENING_LIGHT_THRESH;
        }

        mState = light > thresh ? State.OUTDOOR : State.INDOOR;
    }

    public void putWifiSample(Vector<DataStructure.Fingerprint> fpVec) {
        mLoc = mPilocApi.getLocation(fpVec) != null ? COM1 : OTHER;
    }

    public State getEnvState() {
        return mState;
    }

    public Loc getLoc() {
        return mLoc;
    }
}
