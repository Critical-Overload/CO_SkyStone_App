/**
 * IMURobot
 *
 * Library for using the IMU with our Robot
 *
 * 30 March 2019
 */

package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.hardware.bosch.JustLoggingAccelerationIntegrator;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

public class IMURobot {

    //Declare motors that will be used
    private DcMotor motorFrontRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackRight;
    private DcMotor motorBackLeft;

    //Declare the IMU
    private BNO055IMU imu;

    //Orientations are the sets of angles in the three planes
    private Orientation angles, lastAngles, startAngles;
    private double globalAngle;

    //Declare and initialize gain to be used for straight driving
    private double gain = 0.1;

    //Declare an opmode and a telemetry object
    private LinearOpMode opMode;
    private Telemetry telemetry;

    /**
     *
     * @param motorFrontRight The front right motor
     * @param motorFrontLeft The front left motor
     * @param motorBackRight The back right motor
     * @param motorBackLeft The back left motor
     * @param imu The IMU
     * @param opMode The Op Mode using the IMURobot object;
     *               for access to the methods opModeIsActive, the exception InterruptedException, and telemetry
     */
    public IMURobot(DcMotor motorFrontRight, DcMotor motorFrontLeft, DcMotor motorBackRight, DcMotor motorBackLeft,
                     BNO055IMU imu, LinearOpMode opMode){
        this.motorFrontRight = motorFrontRight;
        this.motorFrontLeft = motorFrontLeft;
        this.motorBackRight = motorBackRight;
        this.motorBackLeft = motorBackLeft;
        this.imu = imu;
        this.opMode = opMode;
        this.telemetry = opMode.telemetry;
    }

    /**
     * Get the IMU parameters
     * @return IMU parameters
     */
    private BNO055IMU.Parameters getIMUParameters(){
        BNO055IMU.Parameters parameters = new BNO055IMU.Parameters();
        parameters.angleUnit           = BNO055IMU.AngleUnit.DEGREES;
        parameters.accelUnit           = BNO055IMU.AccelUnit.METERS_PERSEC_PERSEC;
        parameters.calibrationDataFile = "BNO055IMUCalibration.json"; // see the calibration sample opmode
        parameters.loggingEnabled      = true;
        parameters.loggingTag          = "IMU";
        parameters.accelerationIntegrationAlgorithm = new JustLoggingAccelerationIntegrator();
        parameters.mode                = BNO055IMU.SensorMode.IMU;

        return parameters;
    }

    /**
     * Set up and initialize the IMU parameters
     */
    private void setIMUParameters(){
        BNO055IMU.Parameters parameters = getIMUParameters();
        imu.initialize(parameters);
    }

    /**
     * Set directions and zero power behaviors for applicable motors, calibrate the IMU
     * @throws InterruptedException if robot is stopped while IMU is still calibrating
     */
    public void setupRobot() throws InterruptedException{
        motorBackLeft.setDirection(DcMotor.Direction.REVERSE);
        motorFrontLeft.setDirection(DcMotor.Direction.REVERSE);

        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        setIMUParameters();

        while (!imu.isGyroCalibrated()) {
            telemetry.addData("IMU", "calibrating...");
            telemetry.update();
            Thread.sleep(50);
        }

        telemetry.addData("IMU", "ready");
        telemetry.update();
    }

    /**
     * Reset the angle measurement
     */
    private void resetAngle() {
        lastAngles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        startAngles = lastAngles;
        globalAngle = 0;
    }

    /**
     * Get the change in angle since the last angle reset
     * @return angle in degrees, positive = counterclockwise, negative = clockwise
     */
    private double getAngle(){
        //Get a new angle measurement
        angles = imu.getAngularOrientation(AxesReference.INTRINSIC, AxesOrder.ZYX, AngleUnit.DEGREES);
        //Get the difference between current angle measurement and last angle measurement
        double deltaAngle = angles.firstAngle - lastAngles.firstAngle;

        //Process the angle to keep it within (-180,180)
        //(Once angle passes +180, it will rollback to -179, and vice versa)
        if (deltaAngle < -180)
            deltaAngle += 360;
        else if (deltaAngle > 180)
            deltaAngle -= 360;

        //Add the change in angle since last measurement (deltaAngle)
        //to the change in angle since last reset (globalAngle)
        globalAngle += deltaAngle;
        //Set last angle measurement to current angle measurement
        lastAngles = angles;

        return globalAngle;
    }

    /**
     * Create telemetry with the angle at last reset, current angle, and the change in angle since last reset
     */
    private void composeAngleTelemetry(){
        telemetry.addData("Start Angle", startAngles.firstAngle);
        telemetry.addData("Current Angle", angles.firstAngle);
        telemetry.addData("Global Angle", globalAngle);
    }

    /**
     * Complete stop, sets all motor powers to 0
     */
    public void completeStop(){
        motorFrontLeft.setPower(0);
        motorFrontRight.setPower(0);
        motorBackLeft.setPower(0);
        motorBackRight.setPower(0);
    }

    /**
     * Robot turns clockwise at a set power in tank drive
     * @param power desired power for motors
     */
    public void turnClockwise(double power){
        motorFrontLeft.setPower(-power);
        motorBackLeft.setPower(-power);
        motorFrontRight.setPower(power);
        motorBackRight.setPower(power);
    }

    /**
     * Robot turns counterclockwise at a set power in tank drive
     * @param power desired power for motors
     */
    public void turnCounterClockwise(double power){
        motorFrontLeft.setPower(power);
        motorBackLeft.setPower(power);
        motorFrontRight.setPower(-power);
        motorBackRight.setPower(-power);
    }

    /**
     * Make a precise turn using the gyro
     * @param degrees Negative = clockwise, positive = counterclockwise, min = -180, max = 180
     * @param power Power to turn at
     * @throws InterruptedException if robot is stopped
     */
    public void gyroTurn(int degrees, double power) throws InterruptedException{
        //restart angle tracking
        resetAngle();

        if(degrees < 0){
            turnClockwise(power);
        }else if(degrees > 0){
            turnCounterClockwise(power);
        }else{
            return;
        }

        //Rotate until current angle is equal to the target angle
        if (degrees < 0){
            while (opMode.opModeIsActive() && getAngle() > degrees){
                composeAngleTelemetry();
                //display the target angle
                telemetry.addData("Target angle", degrees);
                telemetry.update();
            }
        }else{
            while (opMode.opModeIsActive() && getAngle() < degrees) {
                composeAngleTelemetry();
                //display the target angle
                telemetry.addData("Target angle", degrees);
                telemetry.update();
            }
        }

        completeStop();
        //Wait .5 seconds to ensure robot is stopped before continuing
        Thread.sleep(500);
        resetAngle();
    }

    /**
     * Check if robot is moving in a straight line using gyro sensor.
     * If not, get a power correction using a preset gain.
     * @return correction, positive = correct clockwise, negative = correct counterclockwise
     */
    private double getCorrection(){
        //Get the current angle of the robot
        double angle = getAngle();
        double correction;

        //Use the angle to calculate the correction
        if (angle == 0){
            //If angle = 0, robot is moving straight; no correction needed
            correction = 0;
        }else{
            //If angle != 0, robot is not moving straight
            //Correction is negative angle (to move the robot in the opposite direction)
            //multiplied by gain; the gain is the sensitivity to angle
            //We have determined that .1 is a good gain; higher gains result in overcorrection
            //Lower gains are ineffective
            correction = -angle*gain;
        }
        //Display correction
        telemetry.addData("Correction", correction);
        telemetry.update();

        return correction;
    }

    /**
     * Tank Drive, with separate powers for left and right side
     * @param leftPower power for left motors
     * @param rightPower power for right motors
     */
    public void tankDrive(double leftPower, double rightPower){
        motorFrontLeft.setPower(leftPower);
        motorFrontRight.setPower(rightPower);
        motorBackLeft.setPower(leftPower);
        motorBackRight.setPower(rightPower);
    }

    /**
     * Drive straight using the gyro
     * @param power power
     * @param seconds time to run
     * @throws InterruptedException if the robot is stopped
     */
    public void gyroDrive(double power, int seconds) throws InterruptedException{
        //restart angle tracking
        resetAngle();

        //create an ElapsedTime object to track the time the robot moves
        ElapsedTime timer = new ElapsedTime();
        //restart time tracking
        timer.reset();

        //drive straight with gyro until timer reaches number of given seconds
        while(timer.seconds() < seconds && opMode.opModeIsActive()){
            //Get a correction
            double correction = getCorrection();
            //Use the correction to adjust robot power so robot drives straight
            tankDrive(power + correction, power - correction);
        }
        completeStop();
        //Wait .5 seconds to ensure robot is stopped before continuing
        Thread.sleep(500);
        resetAngle();
    }

    /**
     * Change the gain
     * @param newGain gain (sensitivity): 0 to 1 (anything over
     */
    public void setNewGain(double newGain){
        gain = newGain;
    }
}
