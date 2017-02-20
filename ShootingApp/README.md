## Group 5 Assignment 2

#### Group Members:

* Fan Jiahuan - A0104278H
* Nguyen Duc Thien - A0093587M
* Yu Shijia - A0115992X

#### Implementation Summary

The additional codes used for orientation detection implement the following steps:

1. Register additional geomagnetic sensor and rotation vector sensor (if available)
2. Copy sensor values in `onSensorChanged` to corresponding instance variables
3. If the phone is not facing up, we do not perform orientation detection
4. Otherwise, we obtain the rotation matrix using the following steps:
  1. If the rotation vector is avaible, use `getRotationMatrixFromVector`
  2. Otherwise if the gravity sensor is available, use `getRotationMatrix` with the gravity and magnetometer readings
  3. Otherwise, use `getRotationMatrix` with the accelerometer and magnetometer readings  
5. We get the Euler angles from the rotation matrix using `getOrientation`.
6. Convert the Azimuth component of the Euler angles to degree from 0 to 360.
   Rotation is positive clockwise with respect to the magnetic North.
7. Divide the detected direction by 45 to obtain the shooting region.
