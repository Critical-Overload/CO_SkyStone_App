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

import com.qualcomm.hardware.bosch.BNO055IMU;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvInternalCamera;
import org.openftc.easyopencv.OpenCvPipeline;

import java.util.ArrayList;
import java.util.List;


@TeleOp (name = "CVDetectionTestIMU")
public class CVDetectionTestIMU extends LinearOpMode
{
    double hue;
    OpenCvCamera phoneCam;
    MainPipeline mainPipeline;
    double threshold = 150;
    double sensitivity;
    String side = "";

    private DcMotor motorFrontRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackRight;
    private DcMotor motorBackLeft;

    //Declare imu
    private BNO055IMU imu;



    @Override
    public void runOpMode() throws InterruptedException
    {
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
        phoneCam.startStreaming(640, 480, OpenCvCameraRotation.UPRIGHT);

        /*
         * Wait for the user to press start on the Driver Station
         */

        motorFrontRight = hardwareMap.dcMotor.get("FR");
        motorFrontLeft = hardwareMap.dcMotor.get("FL");
        motorBackRight = hardwareMap.dcMotor.get("BR");
        motorBackLeft = hardwareMap.dcMotor.get("BL");
        //Initialize imu
        imu = hardwareMap.get(BNO055IMU.class, "imu");
        //Reverse requred motors
        //motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        //motorBackRight.setDirection(DcMotor.Direction.REVERSE);
        //Set zero power behaviors to brake
        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        //Create an IMURobot object that we will use to run the robot
        IMURobot robot = new IMURobot(motorFrontRight, motorFrontLeft, motorBackRight, motorBackLeft, imu, this);
        robot.setupRobot();//calibrate IMU, set any required parameters

        waitForStart();

        while (opModeIsActive())
        {
            /*
             * Send some stats to the telemetry
             */

            telemetry.addData("Center Point", mainPipeline.bcenterx + "," + mainPipeline.bcentery);
            //Input Upright Mid Point: 240,320
            //Input Sideways Mid Point: 320,240
            double inputCenterX = 240;
            double accuracy = 10;
            if (mainPipeline.bcenterx > inputCenterX + accuracy){
                side = "Right";
            }else if (mainPipeline.bcenterx < inputCenterX - accuracy){
                side = "Left";
            }
            else{
                side = "In the Center";
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
        }
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

        int bcenterx;
        int bcentery;
        int scenterx;
        int scentery;
        double inputHeight;
        double inputWidth;
        Mat hsvImage = new Mat();
        Mat buildplate = new Mat();
        Mat skystone = new Mat();
        Mat skystonew = new Mat();
        Mat skystoney = new Mat();
        Mat blurImg = new Mat();
        Mat cannyOutput = new Mat();

        @Override
        public Mat processFrame(Mat input) {
            bcontours.clear();
            scontours.clear();

            Imgproc.Canny(input, cannyOutput, threshold, threshold * 2);

            //yellow = 60
            //Blue = 240
            //red = 0 or 360
            hue = 240;
            sensitivity = 10;


            Imgproc.GaussianBlur(input, blurImg, new Size(5, 5), 0);

            //converting blured image from BGR to HSV
            Imgproc.cvtColor(blurImg, hsvImage, Imgproc.COLOR_RGB2HSV);

            Core.inRange(hsvImage, new Scalar((hue / 2) - sensitivity, 100, 50), new Scalar((hue / 2) + sensitivity, 255, 255), buildplate);
            //Core.inRange(hsvImage, new Scalar(0, 0, 0), new Scalar(180, 255, 30), buildplate);


            Imgproc.findContours(buildplate, bcontours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);


            if (bcontours.size()>0)
            {
                Imgproc.drawContours(input, bcontours, -1, new Scalar(0, 0, 255), 2);

                double bmaxVal = 0;
                int bmaxValIdx = 0;
                for (int contourIdx = 0; contourIdx < bcontours.size(); contourIdx++) {
                    double contourArea = Imgproc.contourArea(bcontours.get(contourIdx));
                    if (bmaxVal < contourArea) {
                        bmaxVal = contourArea;
                        bmaxValIdx = contourIdx;
                    }
                }

                Imgproc.drawContours(input, bcontours, bmaxValIdx, new Scalar(0, 255, 0), 3);

                Rect blargestRect = Imgproc.boundingRect(bcontours.get(bmaxValIdx));
                Imgproc.rectangle(input, blargestRect.tl(), blargestRect.br(), new Scalar(0, 255, 0), 1, 8, 0);
                Imgproc.putText(input, "Buildplate Largest Rectangle", blargestRect.tl(), 2, 0.3, new Scalar(255, 255, 255));
                bcenterx = (blargestRect.x +blargestRect.x + blargestRect.width)/2;
                bcentery = (blargestRect.y + blargestRect.y + blargestRect.height)/2;
                Imgproc.rectangle(input, new Point(bcenterx, bcentery), new Point(bcenterx,bcentery), new Scalar(0,0,255),5,8,0); //Upright



                Imgproc.rectangle(input, new Point(320, 240), new Point(320,240), new Scalar(0,0,0),5,8,0); //Sideways
                Imgproc.rectangle(input, new Point(240, 320), new Point(240,320), new Scalar(255,255,255),5,8,0); //Upright
                Imgproc.line(input, new Point(230,640),new Point(230,0),new Scalar(0,0,0),3,8,0); //Upright
                Imgproc.line(input, new Point(250,640),new Point(250,0),new Scalar(0,0,0),3,8,0);

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
                    Rect rect = Imgproc.boundingRect(points);


                    // draw enclosing rectangle (all same color, but you could use variable i to make them unique)
                    Imgproc.rectangle(input, rect.tl(), rect.br(), new Scalar(255, 0, 0), 1, 8, 0);


                }

            }


            return input;
        }

    }
}