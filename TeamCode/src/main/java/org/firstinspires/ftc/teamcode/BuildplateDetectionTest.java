/*
 * Copyright (c) 2019 OpenFTC Team
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.firstinspires.ftc.teamcode;

import android.renderscript.ScriptGroup;

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.CvType;
import org.opencv.core.Core;
import org.opencv.core.Size;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


@Autonomous(name = "BuildplateDetectionTest")
public class BuildplateDetectionTest extends LinearOpMode
{
    double hue;
    OpenCvCamera phoneCam;
    MainPipeline mainPipeline;
    double threshold = 150;
    double sensitivity;
    String side = "";
    double sideM;
    double sideS;
    double thing;
    double huetwo;

    private DcMotor motorFrontRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackRight;
    private DcMotor motorBackLeft;
    private CRServo leftIntake;
    private CRServo rightIntake;
    private Servo leftIntakeServo;
    private Servo rightIntakeServo;

    //Declare imu
    private BNO055IMU imu;



    @Override
    public void runOpMode() throws InterruptedException
    {
        motorFrontRight = hardwareMap.dcMotor.get("FR");
        motorFrontLeft = hardwareMap.dcMotor.get("FL");
        motorBackRight = hardwareMap.dcMotor.get("BR");
        motorBackLeft = hardwareMap.dcMotor.get("BL");
        leftIntake = hardwareMap.crservo.get("LI");
        rightIntake = hardwareMap.crservo.get("RI");
        leftIntakeServo = hardwareMap.servo.get("LIrelease");
        rightIntakeServo = hardwareMap.servo.get("RIrelease");



        //Initialize imu
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        //Reverse requred motors

        //Set zero power behaviors to brake
        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        //Create an IMURobot object that we will use to run the robot
        IMURobot robot = new IMURobot(motorFrontRight, motorFrontLeft, motorBackRight, motorBackLeft, imu, leftIntake, rightIntake, leftIntakeServo, rightIntakeServo, this);
        robot.setupRobot();//calibrate IMU, set any required parameters

        /*
         * Instantiate an OpenCvCamera object for the camera we'll be using.
         * In this sample, we're using the phone's internal camera. We pass it a
         * CameraDirection enum indicating whether to use the front or back facing
         * camera, as well as the view that we wish to use for camera monitor (on
         * the RC phone). If no camera monitor is desired, use the alternate
         * single-parameter constructor instead (commented out below)
         */
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier("cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        phoneCam = new OpenCvInternalCamera(OpenCvInternalCamera.CameraDirection.BACK, cameraMonitorViewId);

        // OR...  Do Not Activate the Camera Monitor View
        //phoneCam = new OpenCvInternalCamera(OpenCvInternalCamera.CameraDirection.BACK);

        /*
         * Open the connection to the camera device
         */
        phoneCam.openCameraDevice();

        /*
         * Specify the image processing pipeline we wish to invoke upon receipt
         * of a frame from the camera. Note that switching pipelines on-the-fly
         * (while a streaming session is in flight) *IS* supported.
         */
        mainPipeline = new MainPipeline();

        phoneCam.setPipeline(mainPipeline);

        /*
         * Tell the camera to start streaming images to us! Note that you must make sure
         * the resolution you specify is supported by the camera. If it is not, an exception
         * will be thrown.
         *
         * Also, we specify the rotation that the camera is used in. This is so that the image
         * from the camera sensor can be rotated such that it is always displayed with the image upright.
         * For a front facing camera, rotation is defined assuming the user is looking at the screen.
         * For a rear facing camera or a webcam, rotation is defined assuming the camera is facing
         * away from the user.
         */
        phoneCam.startStreaming(640, 480, OpenCvCameraRotation.SIDEWAYS_LEFT);

        /*
         * Wait for the user to press start on the Driver Station
         */
        waitForStart();
            /*
             * Send some stats to the telemetry
             */

            telemetry.addData("Center Point", mainPipeline.bcenterx + "," + mainPipeline.bcentery);
            //Input Upright Mid Point: 240,320
            //Input Sideways Mid Point: 320,240
            double inputCenterX = 320;
            double accuracy = 10;
            if (mainPipeline.bcenterx > inputCenterX + accuracy){
                side = "Right";
                sideM = 1;
            }else if (mainPipeline.bcenterx < inputCenterX - accuracy){
                side = "Left";
                sideM = -1;
            }
            else{
                side = "In the Center";
                sideM = 0;

            }
        if (mainPipeline.scenterx > inputCenterX + accuracy){
            side = "Right";
            sideS = 1;
        }else if (mainPipeline.scenterx < inputCenterX - accuracy){
            side = "Left";
            sideS = -1;
        }
        else{
            side = "In the Center";
            sideS = 0;

        }
            telemetry.addData("Side to Move:",side );

            telemetry.addData("Height of Input", mainPipeline.inputHeight);
            telemetry.addData("Width of Input", mainPipeline.inputWidth);
            telemetry.addData("Frame Count", phoneCam.getFrameCount());
            telemetry.addData("FPS", String.format("%.2f", phoneCam.getFps()));
            telemetry.addData("Total frame time ms", phoneCam.getTotalFrameTimeMs());
            telemetry.addData("Pipeline time ms", phoneCam.getPipelineTimeMs());
            telemetry.addData("Overhead time ms", phoneCam.getOverheadTimeMs());
            telemetry.addData("Theoretical max FPS", phoneCam.getCurrentPipelineMaxFps());

            telemetry.update();


            thing = 1;
            // 0 = foundation
            // 1 = skystone
            while (thing == 0 && sideM != 0){
            //restart angle tracking
            robot.resetAngle();

            //convert direction (degrees) into radians
            double newDirection = (90) * Math.PI/180 + Math.PI/4;
            //calculate powers needed using direction
            double leftPower = Math.cos(newDirection) * (0.3);
            double rightPower = Math.sin(newDirection) * (0.3);
            double correction = robot.getCorrection();
            robot.correctedTankStrafe(leftPower, rightPower, correction);

             //Input Upright Mid Point: 240,320
                //Input Sideways Mid Point: 320,240
                if (mainPipeline.bcenterx > inputCenterX + accuracy){
                    side = "Right";
                    sideM = 1;
                }else if (mainPipeline.bcenterx < inputCenterX - accuracy){
                    side = "Left";
                    sideM = -1;
                }
                else{
                    side = "In the Center";
                    sideM = 0;

                }
            telemetry.update();

        }

        while (thing == 1 && sideS != 0){
            //restart angle tracking
            robot.resetAngle();

            //convert direction (degrees) into radians
            double newDirection = (90) * Math.PI/180 + Math.PI/4;
            //calculate powers needed using direction
            double leftPower = Math.cos(newDirection) * (0.3);
            double rightPower = Math.sin(newDirection) * (0.3);
            double correction = robot.getCorrection();
            robot.correctedTankStrafe(leftPower, rightPower, correction);

            //Input Upright Mid Point: 240,320
            //Input Sideways Mid Point: 320,240
            if (mainPipeline.scenterx > inputCenterX + accuracy){
                side = "Right";
                sideS = 1;
            }else if (mainPipeline.scenterx < inputCenterX - accuracy){
                side = "Left";
                sideS = -1;
            }
            else{
                side = "In the Center";
                sideS = 0;

            }
            telemetry.update();

        }
            robot.completeStop();


    }

    /*
     * An example image processing pipeline to be run upon receipt of each frame from the camera.
     * Note that the processFrame() method is called serially from the frame worker thread -
     * that is, a new camera frame will not come in while you're still processing a previous one.
     * In other words, the processFrame() method will never be called multiple times simultaneously.
     *
     * However, the rendering of your processed image to the viewport is done in parallel to the
     * frame worker thread. That is, the amount of time it takes to render the image to the
     * viewport does NOT impact the amount of frames per second that your pipeline can process.
     *
     * IMPORTANT NOTE: this pipeline is NOT invoked on your OpMode thread. It is invoked on the
     * frame worker thread. This should not be a problem in the vast majority of cases. However,
     * if you're doing something weird where you do need it synchronized with your OpMode thread,
     * then you will need to account for that accordingly.
     */
    class MainPipeline extends OpenCvPipeline
    {
        List<MatOfPoint> bcontours = new ArrayList<>();
        List<MatOfPoint> scontours = new ArrayList<>();
        List<MatOfPoint> ycontours = new ArrayList<>();


        int bcenterx;
        int bcentery;
        int scenterx;
        int scentery;
        double inputHeight;
        double inputWidth;
        Mat hsvImage = new Mat();
        Mat buildplate = new Mat();
        Mat skystone = new Mat();
        Mat blurImg = new Mat();
        Mat cannyOutput = new Mat();
        Mat output = new Mat();
        Mat yellow = new Mat();
        Scalar myColor = new Scalar(0,255,255);
        Mat test = new Mat();
        Mat grey = new Mat();
        Mat greyImg = new Mat();

        @Override
        public Mat processFrame(Mat input) {
            input.copyTo(output);
            Mat mask = new Mat(input.rows(), input.cols(), CvType.CV_8U, Scalar.all(0));
            Mat cropped = new Mat(input.size(),input.type(),myColor);

            ycontours.clear();
            bcontours.clear();
            scontours.clear();

            Imgproc.Canny(input, cannyOutput, threshold, threshold * 2);

            //yellow = 60
            //Blue = 240
            //red = 0 or 360
            hue = 240;
            huetwo = 50;
            sensitivity = 10;

            Imgproc.GaussianBlur(input, blurImg, new Size(5, 5), 0);

            //converting blured image from BGR to HSV
            Imgproc.cvtColor(blurImg, hsvImage, Imgproc.COLOR_RGB2HSV);

            Core.inRange(hsvImage, new Scalar((hue / 2) - sensitivity, 100, 50), new Scalar((hue / 2) + sensitivity, 255, 255), buildplate);
            //Core.inRange(hsvImage, new Scalar((60 / 2) - sensitivity, 100, 50), new Scalar((60 / 2) + sensitivity, 255, 255), skystoney);

            Core.inRange(hsvImage, new Scalar((huetwo / 2) - sensitivity, 100, 50), new Scalar((huetwo / 2) + sensitivity, 255, 255), yellow);

            Imgproc.findContours(yellow, ycontours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            if (ycontours.size() > 0){

                MatOfPoint2f approxCurve = new MatOfPoint2f();

                for (int i = 0; i < ycontours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(ycontours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                    // Get bounding rect of contour
                    Rect rect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    double bmaxVal = 0;
                    int bmaxValIdx = 0;
                    for (int contourIdx = 0; contourIdx < ycontours.size(); contourIdx++) {
                        double contourArea = Imgproc.contourArea(ycontours.get(contourIdx));
                        if (bmaxVal < contourArea) {
                            bmaxVal = contourArea;
                            bmaxValIdx = contourIdx;
                        }
                    }

                    Rect ylargestRect = Imgproc.boundingRect(ycontours.get(bmaxValIdx));
                    Imgproc.line(input, new Point(0,ylargestRect.y), new Point(480, ylargestRect.y), new Scalar(100,100,100),3,8,0);
                    Imgproc.line(input, new Point(0,ylargestRect.y + ylargestRect.height), new Point(480, ylargestRect.y + ylargestRect.height), new Scalar(100,100,100),3,8,0);
                    Imgproc.rectangle(mask,new Point(0,ylargestRect.y), new Point(480, ylargestRect.y + ylargestRect.height), new Scalar(255,255,255),-1,8,0);
                    input.copyTo(cropped,mask);
                    cropped.copyTo(input);



                }
                Imgproc.cvtColor(input,grey, Imgproc.COLOR_RGB2GRAY);
                Imgproc.threshold(grey, greyImg,15,255,Imgproc.THRESH_BINARY_INV);
                Imgproc.findContours(greyImg, scontours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            }


            Imgproc.findContours(buildplate, bcontours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

            if (bcontours.size()>0 && scontours.size() == 0)
            {
                Imgproc.drawContours(output, bcontours, -1, new Scalar(0, 0, 255), 2);


                Imgproc.rectangle(output, new Point(320, 240), new Point(320,240), new Scalar(0,0,0),5,8,0); //Sideways
                Imgproc.rectangle(output, new Point(240, 320), new Point(240,320), new Scalar(255,255,255),5,8,0); //Upright
                Imgproc.line(output, new Point(310,640),new Point(310,0),new Scalar(0,0,0),3,8,0); //Upright
                Imgproc.line(output, new Point(330,640),new Point(330,0),new Scalar(0,0,0),3,8,0);

                inputHeight = input.size().height;
                inputWidth = input.size().width;


                MatOfPoint2f approxCurve = new MatOfPoint2f();

                //For each contour found
                for (int i = 0; i < bcontours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(bcontours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                    // Get bounding rect of contour
                    Rect brect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    Imgproc.rectangle(output, brect.tl(), brect.br(), new Scalar(255, 0, 0), 1, 8, 0);


                }

                double bmaxVal = 0;
                int bmaxValIdx = 0;
                for (int contourIdx = 0; contourIdx < bcontours.size(); contourIdx++) {
                    double contourArea = Imgproc.contourArea(bcontours.get(contourIdx));
                    if (bmaxVal < contourArea) {
                        bmaxVal = contourArea;
                        bmaxValIdx = contourIdx;
                    }
                }

                Imgproc.drawContours(output, bcontours, bmaxValIdx, new Scalar(0, 255, 0), 3);

                Rect blargestRect = Imgproc.boundingRect(bcontours.get(bmaxValIdx));
                Imgproc.rectangle(output, blargestRect.tl(), blargestRect.br(), new Scalar(0, 255, 0), 1, 8, 0);
                Imgproc.putText(output, "Buildplate Largest Rectangle", blargestRect.tl(), 2, 0.3, new Scalar(255, 255, 255));
                bcenterx = (blargestRect.x +blargestRect.x + blargestRect.width)/2;
                bcentery = (blargestRect.y + blargestRect.y + blargestRect.height)/2;
                Imgproc.rectangle(output, new Point(bcenterx, bcentery), new Point(bcenterx,bcentery), new Scalar(0,0,255),5,8,0); //Upright



            }
            if(scontours.size()>0 && bcontours.size() == 0)
            {
                Imgproc.drawContours(output, scontours, -1, new Scalar(0, 0, 255), 2);

                Imgproc.rectangle(output, new Point(320, 240), new Point(320,240), new Scalar(0,0,0),5,8,0); //Sideways
                Imgproc.rectangle(output, new Point(240, 320), new Point(240,320), new Scalar(255,255,255),5,8,0); //Upright
                Imgproc.line(output, new Point(310,640),new Point(310,0),new Scalar(0,0,0),3,8,0); //Upright
                Imgproc.line(output, new Point(330,640),new Point(330,0),new Scalar(0,0,0),3,8,0);

                inputHeight = input.size().height;
                inputWidth = input.size().width;


                MatOfPoint2f approxCurve = new MatOfPoint2f();

                //For each contour found
                for (int i = 0; i < scontours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(scontours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, approxCurve, approxDistance, true);

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(approxCurve.toArray());

                    // Get bounding rect of contour
                    Rect srect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    Imgproc.rectangle(output, srect.tl(), srect.br(), new Scalar(255, 0, 0), 1, 8, 0);


                }


                double smaxVal = 0;
                int smaxValIdx = 0;
                for (int contourIdx = 0; contourIdx < scontours.size(); contourIdx++) {
                    double contourArea = Imgproc.contourArea(scontours.get(contourIdx));
                    if (smaxVal < contourArea) {
                        smaxVal = contourArea;
                        smaxValIdx = contourIdx;
                    }
                }

                Imgproc.drawContours(output, scontours, smaxValIdx, new Scalar(0, 255, 0), 3);

                Rect slargestRect = Imgproc.boundingRect(scontours.get(smaxValIdx));
                Imgproc.rectangle(output, slargestRect.tl(), slargestRect.br(), new Scalar(100, 255, 100), 1, 8, 0);
                Imgproc.putText(output, "Skystone Largest Rectangle", slargestRect.tl(), 2, 0.3, new Scalar(255, 255, 255));
                scenterx = (slargestRect.x +slargestRect.x + slargestRect.width)/2;
                scentery = (slargestRect.y + slargestRect.y + slargestRect.height)/2;
                Imgproc.rectangle(output, new Point(scenterx, scentery), new Point(scenterx,scentery), new Scalar(0,0,255),5,8,0); //Upright



            }
            if (bcontours.size()>0 && scontours.size()>0){
                Imgproc.drawContours(output, scontours, -1, new Scalar(0, 0, 255), 2);

                Imgproc.rectangle(output, new Point(320, 240), new Point(320,240), new Scalar(0,0,0),5,8,0); //Sideways
                Imgproc.rectangle(output, new Point(240, 320), new Point(240,320), new Scalar(255,255,255),5,8,0); //Upright
                Imgproc.line(output, new Point(310,640),new Point(310,0),new Scalar(0,0,0),3,8,0); //Upright
                Imgproc.line(output, new Point(330,640),new Point(330,0),new Scalar(0,0,0),3,8,0);

                inputHeight = input.size().height;
                inputWidth = input.size().width;


                MatOfPoint2f sapproxCurve = new MatOfPoint2f();

                //For each contour found
                for (int i = 0; i < scontours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(scontours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, sapproxCurve, approxDistance, true);

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(sapproxCurve.toArray());

                    // Get bounding rect of contour
                    Rect srect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    Imgproc.rectangle(output, srect.tl(), srect.br(), new Scalar(255, 0, 0), 1, 8, 0);


                    double smaxVal = 0;
                    int smaxValIdx = 0;
                    for (int contourIdx = 0; contourIdx < scontours.size(); contourIdx++) {
                        double contourArea = Imgproc.contourArea(scontours.get(contourIdx));
                        if (smaxVal < contourArea) {
                            smaxVal = contourArea;
                            smaxValIdx = contourIdx;
                        }
                    }

                    Imgproc.drawContours(output, scontours, smaxValIdx, new Scalar(0, 255, 0), 3);

                    Rect slargestRect = Imgproc.boundingRect(scontours.get(smaxValIdx));
                    Imgproc.rectangle(output, slargestRect.tl(), slargestRect.br(), new Scalar(100, 255, 100), 1, 8, 0);
                    Imgproc.putText(output, "Skystone Largest Rectangle", slargestRect.tl(), 2, 0.3, new Scalar(255, 255, 255));
                    scenterx = (slargestRect.x +slargestRect.x + slargestRect.width)/2;
                    scentery = (slargestRect.y + slargestRect.y + slargestRect.height)/2;
                    Imgproc.rectangle(output, new Point(scenterx, scentery), new Point(scenterx,scentery), new Scalar(0,0,255),5,8,0); //Upright

                }

                Imgproc.drawContours(output, bcontours, -1, new Scalar(0, 0, 255), 2);

                Imgproc.rectangle(output, new Point(320, 240), new Point(320,240), new Scalar(0,0,0),5,8,0); //Sideways
                Imgproc.rectangle(output, new Point(240, 320), new Point(240,320), new Scalar(255,255,255),5,8,0); //Upright
                Imgproc.line(output, new Point(310,640),new Point(310,0),new Scalar(0,0,0),3,8,0); //Upright
                Imgproc.line(output, new Point(330,640),new Point(330,0),new Scalar(0,0,0),3,8,0);

                inputHeight = input.size().height;
                inputWidth = input.size().width;


                MatOfPoint2f bapproxCurve = new MatOfPoint2f();

                //For each contour found
                for (int i = 0; i < bcontours.size(); i++) {
                    //Convert contours(i) from MatOfPoint to MatOfPoint2f
                    MatOfPoint2f contour2f = new MatOfPoint2f(bcontours.get(i).toArray());
                    //Processing on mMOP2f1 which is in type MatOfPoint2f
                    double approxDistance = Imgproc.arcLength(contour2f, true) * 0.02;
                    Imgproc.approxPolyDP(contour2f, bapproxCurve, approxDistance, true);

                    //Convert back to MatOfPoint
                    MatOfPoint points = new MatOfPoint(bapproxCurve.toArray());

                    // Get bounding rect of contour
                    Rect brect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    Imgproc.rectangle(output, brect.tl(), brect.br(), new Scalar(255, 0, 0), 1, 8, 0);


                    double bmaxVal = 0;
                    int bmaxValIdx = 0;
                    for (int contourIdx = 0; contourIdx < bcontours.size(); contourIdx++) {
                        double contourArea = Imgproc.contourArea(bcontours.get(contourIdx));
                        if (bmaxVal < contourArea) {
                            bmaxVal = contourArea;
                            bmaxValIdx = contourIdx;
                        }
                    }

                    Imgproc.drawContours(output, bcontours, bmaxValIdx, new Scalar(0, 255, 0), 3);

                    Rect blargestRect = Imgproc.boundingRect(bcontours.get(bmaxValIdx));
                    Imgproc.rectangle(output, blargestRect.tl(), blargestRect.br(), new Scalar(0, 255, 0), 1, 8, 0);
                    Imgproc.putText(output, "Buildplate Largest Rectangle", blargestRect.tl(), 2, 0.3, new Scalar(255, 255, 255));
                    bcenterx = (blargestRect.x +blargestRect.x + blargestRect.width)/2;
                    bcentery = (blargestRect.y + blargestRect.y + blargestRect.height)/2;
                    Imgproc.rectangle(output, new Point(bcenterx, bcentery), new Point(bcenterx,bcentery), new Scalar(0,0,255),5,8,0); //Upright


                }


            }


            return output;
        }

    }
}