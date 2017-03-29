package nus.cs4222.activitysim.detection;

public class VehicleDetector {
    public enum State {
        NOT_IN_VEHICLE,
        MAYBE_IN_VEHICLE,
        MAY_NOT_BE_IN_VEHICLE_1,
        MAY_NOT_BE_IN_VEHICLE_2,
        IN_VEHICLE
    }

    private static final double THRESH_SPEED = 7;

    private State mVehicleState = State.NOT_IN_VEHICLE;
    private SpeedSensor mSpeedSensor;

    public VehicleDetector(SpeedSensor speedSensor) {
        mSpeedSensor = speedSensor;
    }

    public void doDetection() {
        if (mSpeedSensor.getSpeed() >= THRESH_SPEED) {
            switch (mVehicleState) {
                case NOT_IN_VEHICLE:
                    mVehicleState = State.MAYBE_IN_VEHICLE;
                    break;
                case MAYBE_IN_VEHICLE:
                    mVehicleState = State.IN_VEHICLE;
                    break;
                case MAY_NOT_BE_IN_VEHICLE_1:
                case MAY_NOT_BE_IN_VEHICLE_2:
                    mVehicleState = State.MAYBE_IN_VEHICLE;
            }
        } else {
            switch (mVehicleState) {
                case MAYBE_IN_VEHICLE:
                    mVehicleState = State.MAY_NOT_BE_IN_VEHICLE_1;
                    break;
                case MAY_NOT_BE_IN_VEHICLE_1:
                    mVehicleState = State.NOT_IN_VEHICLE;
                    break;
                case MAY_NOT_BE_IN_VEHICLE_2:
                    mVehicleState = State.MAY_NOT_BE_IN_VEHICLE_1;
                    break;
                case IN_VEHICLE:
                    mVehicleState = State.MAY_NOT_BE_IN_VEHICLE_2;
                    break;
            }
        }
    }

    public State getVehicleState() {
        return mVehicleState;
    }
}
