package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.Servo;

@TeleOp(name = "AAMainTeleOp")
public class MainTeleOp extends LinearOpMode {
    //testing
    private DcMotor motorFrontRight;
    private DcMotor motorFrontLeft;
    private DcMotor motorBackLeft;
    private DcMotor motorBackRight;
    private DcMotor arm;
    private CRServo leftIntake;
    private CRServo rightIntake;

    private Servo leftIntakeServo;
    private Servo rightIntakeServo;
    private Servo flimsy;
    private CRServo clawMover;
    private Servo claw;

    private DigitalChannel touch;

    @Override
    public void runOpMode() throws InterruptedException {

        motorFrontRight = hardwareMap.dcMotor.get("FR");
        motorFrontLeft = hardwareMap.dcMotor.get("FL");
        motorBackLeft = hardwareMap.dcMotor.get("BL");
        motorBackRight = hardwareMap.dcMotor.get("BR");
        arm = hardwareMap.dcMotor.get("arm");

        leftIntake = hardwareMap.crservo.get("LI");
        rightIntake = hardwareMap.crservo.get("RI");

        leftIntakeServo = hardwareMap.servo.get("LIrelease");
        rightIntakeServo = hardwareMap.servo.get("RIrelease");
        flimsy = hardwareMap.servo.get("flimsy");
        clawMover = hardwareMap.crservo.get("clawMover");
        claw = hardwareMap.servo.get("claw");

        motorFrontRight.setDirection(DcMotor.Direction.REVERSE);
        motorBackRight.setDirection(DcMotor.Direction.REVERSE);
        leftIntake.setDirection(CRServo.Direction.REVERSE);

        motorFrontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorFrontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        motorBackLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        arm.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        arm.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        arm.setMode(DcMotor.RunMode.RUN_USING_ENCODER);


        double powerMod = 1.0;
        double intakeMod = 1.0;

        waitForStart();

        while(opModeIsActive()){

            /*
            Checks if right bumper is pressed. If so, power is reduced
             */
            if(gamepad1.right_bumper){
                powerMod = 0.5;
            }else{
                powerMod = 1.0;
            }

            if(gamepad2.left_bumper){
                intakeMod = 0.5;
            }else{
                intakeMod = 1.0;
            }

            if(gamepad2.a){
                leftIntakeServo.setPosition(0);
                rightIntakeServo.setPosition(1);
            }

            if(gamepad2.b){
                leftIntakeServo.setPosition(1);
                rightIntakeServo.setPosition(0);
            }

            if(gamepad2.dpad_up){
                leftIntakeServo.setPosition(leftIntakeServo.getPosition() + 0.01);
                rightIntakeServo.setPosition(rightIntakeServo.getPosition() - 0.01);

            }

            if(gamepad2.dpad_down){
                leftIntakeServo.setPosition(leftIntakeServo.getPosition() - 0.01);
                rightIntakeServo.setPosition(rightIntakeServo.getPosition() + 0.01);
            }

            if(gamepad2.x){
                claw.setPosition(0);
            }


            if(gamepad2.y){
                claw.setPosition(1);
            }

            arm.setPower(gamepad2.right_stick_y*0.5);

            clawMover.setPower(gamepad2.right_stick_y*0.25+gamepad2.left_stick_y*0.25);

            if(gamepad1.a){
                flimsy.setPosition(flimsy.getPosition() + 0.01);
            }
            if(gamepad1.b){
                flimsy.setPosition(flimsy.getPosition() - 0.01);
            }


            telemetry.addData("Flimsy servo position", flimsy.getPosition());

            leftIntake.setPower((gamepad2.right_trigger * intakeMod)-(gamepad2.left_trigger * intakeMod));
            rightIntake.setPower((gamepad2.right_trigger * intakeMod)-(gamepad2.left_trigger * intakeMod));

            double angle = Math.atan2(gamepad1.right_stick_y, gamepad1.right_stick_x) - (Math.PI/4);
            double r = Math.hypot(gamepad1.right_stick_x, gamepad1.right_stick_y);
            double rotation = gamepad1.left_stick_x;

            double powerOne = r*Math.sin(angle);
            double powerTwo = r*Math.cos(angle);

            motorFrontLeft.setPower((powerOne - (rotation))*powerMod);
            motorFrontRight.setPower((powerTwo + (rotation))*powerMod);
            motorBackLeft.setPower((powerTwo - (rotation))*powerMod);
            motorBackRight.setPower((powerOne + (rotation))*powerMod);

            telemetry.addData("FL Power", motorFrontLeft.getPower());
            telemetry.addData("BL Power", motorBackLeft.getPower());
            telemetry.addData("FR Power", motorFrontRight.getPower());
            telemetry.addData("BR Power", motorBackRight.getPower());

            telemetry.update();
            idle();

        }
    }
}
