package nus.cs4222.activitysim;

import nus.cs4222.activitysim.DataStructure.Fingerprint;
import nus.cs4222.activitysim.detection.*;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

/**
 Class containing the activity detection algorithm.

 <p> You can code your activity detection algorithm in this class.
 (You may add more Java class files or add libraries in the 'libs'
 folder if you need).
 The different callbacks are invoked as per the sensor log files,
 in the increasing order of timestamps. In the best case, you will
 simply need to copy paste this class file (and any supporting class
 files and libraries) to the Android app without modification
 (in stage 2 of the project).

 <p> Remember that your detection algorithm executes as the sensor data arrives
 one by one. Once you have detected the user's current activity, output
 it using the {@link ActivitySimulator#outputDetectedActivity(UserActivities)}
 method. If the detected activity changes later on, then you need to output the
 newly detected activity using the same method, and so on.
 The detected activities are logged to the file "DetectedActivities.txt",
 in the same folder as your sensor logs.

 <p> To get the current simulator time, use the method
 {@link ActivitySimulator#currentTimeMillis()}. You can set timers using
 the {@link SimulatorTimer} class if you require. You can log to the
 console/DDMS using either {@code System.out.println()} or using the
 {@link android.util.Log} class. You can use the {@code SensorManager.getRotationMatrix()}
 method (and any other helpful methods) as you would normally do on Android.

 <p> Note: Since this is a simulator, DO NOT create threads, DO NOT sleep(),
 or do anything that can cause the simulator to stall/pause. You
 can however use timers if you require, see the documentation of the
 {@link SimulatorTimer} class.
 In the simulator, the timers are faked. When you copy the code into an
 actual Android app, the timers are real, but the code of this class
 does not need not be modified.
 */
public class ActivityDetection {
    private static final String TAG = ActivityDetection.class.getName();
    private static final long MIN_ACTIVITY_DURATION = 20000;

    private UserActivities mCurrentActivity = UserActivities.OTHER;
    private long mLastActivityChangeTime = 0;

    private SpeedSensor mSpeedSensor;
    private MotionSensor mMotionSensor;
    private WalkingDetector mWalkingDetector;
    private VehicleDetector mVehicleDetector;
    private EnvDetector mEnvDetector;

    /** Initialises the detection algorithm. */
    public void initDetection() throws Exception {
        mSpeedSensor = new SpeedSensor();
        mMotionSensor = new MotionSensor();
        mWalkingDetector = new WalkingDetector();
        mVehicleDetector = new VehicleDetector(mSpeedSensor, mMotionSensor);
        // If you are using the Piloc API, then you must load a radio map (in this case, Hande
        //  has provided the radio map data for the pathways marked in the map image in IVLE
        //  workbin, which represents IDLE_COM1 state). You can use your own radio map data, or
        //  code your own localization algorithm in PilocApi. Please see the "onWiFiSensorChanged()"
        //  method.
        pilocApi = new PilocApi();
        if (pilocApi.loadRadioMap(new File("radiomap.rm")) == null) {
            throw new IOException(
                    "Unable to open radio map file, did you specify the correct path in ActivityDetection.java?");
        }
        mEnvDetector = new EnvDetector(pilocApi);

        mLastActivityChangeTime = ActivitySimulator.currentTimeMillis();
    }

    /** De-initialises the detection algorithm. */
    public void deinitDetection()
            throws Exception {
        // Add de-initialisation code here, if any
    }

    private void detectActivity() {
        long currentTime = ActivitySimulator.currentTimeMillis();
        if (currentTime < mLastActivityChangeTime + MIN_ACTIVITY_DURATION) {
            return;
        }

        UserActivities newActivity = mCurrentActivity;

        WalkingDetector.State walkingState = mWalkingDetector.getWalkingState();
        VehicleDetector.State vehicleState = mVehicleDetector.getVehicleState();
        /* String msg = "Current Activity: " + mCurrentActivity +
                "\nWalking State: " + walkingState +
                "\nVehicle State: " + vehicleState;
        Log.d(TAG, msg);*/

        switch (mCurrentActivity) {
            case WALKING:
                if (vehicleState == VehicleDetector.State.IN_VEHICLE) {
                    newActivity = UserActivities.BUS;
                } else if (walkingState == WalkingDetector.State.NOT_WALKING &&
                        vehicleState == VehicleDetector.State.NOT_IN_VEHICLE) {
                    newActivity = detectIdleEnv();
                }
                break;
            case BUS:
                if (vehicleState == VehicleDetector.State.NOT_IN_VEHICLE &&
                        walkingState == WalkingDetector.State.WALKING) {
                    newActivity = UserActivities.WALKING;
                }
                break;
            case IDLE_INDOOR:
            case IDLE_COM1:
            case IDLE_OUTDOOR:
            case OTHER:
                if (walkingState == WalkingDetector.State.WALKING) {
                    newActivity = UserActivities.WALKING;
                } else if (mMotionSensor.getMotionState() == MotionSensor.State.MOVING &&
                        vehicleState == VehicleDetector.State.IN_VEHICLE) {
                    newActivity = UserActivities.BUS;
                } else {
                    newActivity = detectIdleEnv();
                }
        }

        if (newActivity != mCurrentActivity) {
            mCurrentActivity = newActivity;
            mLastActivityChangeTime = currentTime;
            ActivitySimulator.outputDetectedActivity(mCurrentActivity);
        }
    }

    private UserActivities detectIdleEnv() {
        EnvDetector.State envState = mEnvDetector.getEnvState();
        if (envState == EnvDetector.State.OUTDOOR) {
            return UserActivities.IDLE_OUTDOOR;
        }
        EnvDetector.Loc envLoc = mEnvDetector.getLoc();
        return envLoc == EnvDetector.Loc.COM1 ? UserActivities.IDLE_COM1 : UserActivities.IDLE_INDOOR;
    }

    /**
     Called when the accelerometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Accl x value (m/sec^2)
     @param   y            Accl y value (m/sec^2)
     @param   z            Accl z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onAcclSensorChanged( long timestamp ,
                                     float x ,
                                     float y ,
                                     float z ,
                                     int accuracy ) {
        mWalkingDetector.doDetection(new AccelSample(timestamp, x, y, z));
        detectActivity();
    }

    /**
     Called when the gravity sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Gravity x value (m/sec^2)
     @param   y            Gravity y value (m/sec^2)
     @param   z            Gravity z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGravitySensorChanged( long timestamp ,
                                        float x ,
                                        float y ,
                                        float z ,
                                        int accuracy ) {
    }

    /**
     Called when the linear accelerometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Linear Accl x value (m/sec^2)
     @param   y            Linear Accl y value (m/sec^2)
     @param   z            Linear Accl z value (m/sec^2)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLinearAcclSensorChanged( long timestamp ,
                                           float x ,
                                           float y ,
                                           float z ,
                                           int accuracy ) {
        mMotionSensor.putLinAccelSample(x, y, z);
        detectActivity();
    }

    /**
     Called when the magnetic sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Magnetic x value (microTesla)
     @param   y            Magnetic y value (microTesla)
     @param   z            Magnetic z value (microTesla)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onMagneticSensorChanged( long timestamp ,
                                         float x ,
                                         float y ,
                                         float z ,
                                         int accuracy ) {
    }

    /**
     Called when the gyroscope sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Gyroscope x value (rad/sec)
     @param   y            Gyroscope y value (rad/sec)
     @param   z            Gyroscope z value (rad/sec)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onGyroscopeSensorChanged( long timestamp ,
                                          float x ,
                                          float y ,
                                          float z ,
                                          int accuracy ) {
    }

    /**
     Called when the rotation vector sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   x            Rotation vector x value (unitless)
     @param   y            Rotation vector y value (unitless)
     @param   z            Rotation vector z value (unitless)
     @param   scalar       Rotation vector scalar value (unitless)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onRotationVectorSensorChanged( long timestamp ,
                                               float x ,
                                               float y ,
                                               float z ,
                                               float scalar ,
                                               int accuracy ) {
    }

    /**
     Called when the barometer sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   pressure     Barometer pressure value (millibar)
     @param   altitude     Barometer altitude value w.r.t. standard sea level reference (meters)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onBarometerSensorChanged( long timestamp ,
                                          float pressure ,
                                          float altitude ,
                                          int accuracy ) {
    }

    /**
     Called when the light sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   light        Light value (lux)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onLightSensorChanged(long timestamp ,
                                     float light ,
                                     int accuracy) {
        mEnvDetector.putLightSample(timestamp, light);
        detectActivity();
    }

    /**
     Called when the proximity sensor has changed.

     @param   timestamp    Timestamp of this sensor event
     @param   proximity    Proximity value (cm)
     @param   accuracy     Accuracy of the sensor data (you can ignore this)
     */
    public void onProximitySensorChanged( long timestamp ,
                                          float proximity ,
                                          int accuracy ) {
    }

    /**
     Called when the location sensor has changed.

     @param   timestamp    Timestamp of this location event
     @param   provider     "gps" or "network"
     @param   latitude     Latitude (deg)
     @param   longitude    Longitude (deg)
     @param   accuracy     Accuracy of the location data (you may use this) (meters)
     @param   altitude     Altitude (meters) (may be -1 if unavailable)
     @param   bearing      Bearing (deg) (may be -1 if unavailable)
     @param   speed        Speed (m/sec) (may be -1 if unavailable)
     */
    public void onLocationSensorChanged(long timestamp,
                                        String provider,
                                        double latitude,
                                        double longitude,
                                        float accuracy,
                                        double altitude,
                                        float bearing,
                                        float speed) {
        mSpeedSensor.putLocation(latitude, longitude, timestamp);
        mVehicleDetector.doDetection();
        detectActivity();
    }

    /**
     Called when the WiFi sensor has changed (i.e., a WiFi scan has been performed).

     @param   timestamp           Timestamp of this WiFi scan event
     @param   fingerprintVector   Vector of fingerprints from the WiFi scan
     */
    public void onWiFiSensorChanged( long timestamp ,
                                     Vector< Fingerprint > fingerprintVector ) {

        // You can use Piloc APIs here to figure out the indoor location in COM1, or do
        //  anything that will help you figure out the user activity.
        // You can use the method PilocApi.getLocation(fingerprintVector) to get the location
        //  in COM1 from the WiFi scan. You may use your own radio map, or even write your
        //  own localization algorithm in PilocApi.getLocation(). 

        // NOTE: Please use the "pilocApi" object defined below to use the Piloc API.
        mEnvDetector.putWifiSample(fingerprintVector);
        detectActivity();
    }

    /** Piloc API provided by Hande. */
    private PilocApi pilocApi;

    /** Helper method to convert UNIX millis time into a human-readable string. */
    private static String convertUnixTimeToReadableString(long millisec) {
        return sdf.format(new Date(millisec));
    }

    /** To format the UNIX millis time as a human-readable string. */
    private static final SimpleDateFormat sdf = new SimpleDateFormat( "yyyy-MM-dd-h-mm-ssa" );
}
