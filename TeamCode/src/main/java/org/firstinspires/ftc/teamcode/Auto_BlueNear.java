package org.firstinspires.ftc.teamcode;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.arcrobotics.ftclib.hardware.SimpleServo;
import com.arcrobotics.ftclib.hardware.motors.Motor;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.teamcode.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.util.variables;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;
import java.util.ArrayList;
import java.util.List;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.openftc.easyopencv.OpenCvCamera;
import org.openftc.easyopencv.OpenCvCameraFactory;
import org.openftc.easyopencv.OpenCvCameraRotation;
import org.openftc.easyopencv.OpenCvPipeline;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;

@Autonomous
public class Auto_BlueNear extends LinearOpMode {
    SimpleServo clawAngleServo;
    SimpleServo clawRightServo;
    SimpleServo clawLeftServo;
    double cX = 0;
    double cY = 0;
    double width = 0;

    int position = 0;


    private OpenCvCamera controlHubCam;  // Use OpenCvCamera class from FTC SDK
    private static final int CAMERA_WIDTH = 640; // width  of wanted camera resolution
    private static final int CAMERA_HEIGHT = 480; // height of wanted camera resolution

    // Calculate the distance using the formula
    public static final double objectWidthInRealWorldUnits = 3.75;  // Replace with the actual width of the object in real-world units
    public static final double focalLength = 728;  // Replace with the focal length of the camera in pixels
   public ElapsedTime timer;
   public Motor m_motor;
   public Motor m_motor_2;

    public int pos;
    public int pos2;
    private void initOpenCV() {

        // Create an instance of the camera
        int cameraMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "cameraMonitorViewId", "id", hardwareMap.appContext.getPackageName());

        // Use OpenCvCameraFactory class from FTC SDK to create camera instance
        controlHubCam = OpenCvCameraFactory.getInstance().createWebcam(
                hardwareMap.get(WebcamName.class, "webcam1"), cameraMonitorViewId);

        controlHubCam.setPipeline(new YellowBlobDetectionPipeline());

        controlHubCam.openCameraDevice();
        controlHubCam.startStreaming(CAMERA_WIDTH, CAMERA_HEIGHT, OpenCvCameraRotation.UPRIGHT);
    }

    class YellowBlobDetectionPipeline extends OpenCvPipeline {
        @Override
        public Mat processFrame(Mat input) {
            // Preprocess the frame to detect yellow regions
            Mat yellowMask = preprocessFrame(input);

            // Find contours of the detected yellow regions
            List<MatOfPoint> contours = new ArrayList<>();
            Mat hierarchy = new Mat();
            Imgproc.findContours(yellowMask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

            // Find the largest yellow contour (blob)
            MatOfPoint largestContour = findLargestContour(contours);

            if (largestContour != null) {
                // Draw a red outline around the largest detected object
                Imgproc.drawContours(input, contours, contours.indexOf(largestContour), new Scalar(255, 0, 0), 2);
                // Calculate the width of the bounding box
                width = calculateWidth(largestContour);

                // Display the width next to the label
                String widthLabel = "Width: " + (int) width + " pixels";
                Imgproc.putText(input, widthLabel, new Point(cX + 10, cY + 20), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                //Display the Distance
                String distanceLabel = "Distance: " + String.format("%.2f", getDistance(width)) + " inches";
                Imgproc.putText(input, distanceLabel, new Point(cX + 10, cY + 60), Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                // Calculate the centroid of the largest contour
                Moments moments = Imgproc.moments(largestContour);
                cX = moments.get_m10() / moments.get_m00();
                cY = moments.get_m01() / moments.get_m00();

                // Draw a dot at the centroid
                String label = "(" + (int) cX + ", " + (int) cY + ")";
                Imgproc.putText(input, label, new Point(cX + 10, cY), Imgproc.FONT_HERSHEY_COMPLEX, 0.5, new Scalar(0, 255, 0), 2);
                Imgproc.circle(input, new Point(cX, cY), 5, new Scalar(0, 255, 0), -1);

            }

            return input;
        }

        private Mat preprocessFrame(Mat frame) {
            Mat hsvFrame = new Mat();
            Imgproc.cvtColor(frame, hsvFrame, Imgproc.COLOR_BGR2HSV);
            //BLUE
            Scalar lowerBlue = new Scalar(25,50,50);
            Scalar upperBlue = new Scalar(32,255,255);


            Mat blueMask = new Mat();
            Core.inRange(hsvFrame, lowerBlue, upperBlue, blueMask);

            Mat kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(5, 5));
            Imgproc.morphologyEx(blueMask, blueMask, Imgproc.MORPH_OPEN, kernel);
            Imgproc.morphologyEx(blueMask, blueMask, Imgproc.MORPH_CLOSE, kernel);

            return blueMask;
        }

        private MatOfPoint findLargestContour(List<MatOfPoint> contours) {
            double maxArea = 0;
            MatOfPoint largestContour = null;

            for (MatOfPoint contour : contours) {
                double area = Imgproc.contourArea(contour);
                if (area > maxArea) {
                    maxArea = area;
                    largestContour = contour;
                }
            }

            return largestContour;
        }

        private double calculateWidth(MatOfPoint contour) {
            Rect boundingRect = Imgproc.boundingRect(contour);
            return boundingRect.width;
        }

    }

    private static double getDistance(double width) {
        double distance = (objectWidthInRealWorldUnits * focalLength) / width;
        return distance;
    }
    @Override
    public void runOpMode() throws InterruptedException {
        SampleMecanumDrive drive = new SampleMecanumDrive(hardwareMap);
        clawRightServo = new SimpleServo(
                hardwareMap, "clawRightServo", 10, 120,
                AngleUnit.DEGREES
        );

        clawLeftServo = new SimpleServo(
                hardwareMap, "clawLeftServo", 10, 120,
                AngleUnit.DEGREES
        );
        clawAngleServo = new SimpleServo(
                hardwareMap, "clawAngleServo", 0, 180,
                AngleUnit.DEGREES
        );
        m_motor = new Motor(hardwareMap, "armLiftMotor", 8192, 60);
        m_motor.setRunMode(Motor.RunMode.PositionControl);
        m_motor.setPositionCoefficient(0.01);
        pos = m_motor.getCurrentPosition();
        m_motor.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        m_motor.setPositionTolerance(100);
        m_motor_2 = new Motor(hardwareMap, "armSlideMotor", 1425.2, 117);
        m_motor_2.setRunMode(Motor.RunMode.PositionControl);
        m_motor_2.setPositionCoefficient(0.05);
        pos2 = m_motor_2.getCurrentPosition();
        m_motor_2.setInverted(true);
        m_motor_2.setZeroPowerBehavior(Motor.ZeroPowerBehavior.BRAKE);
        m_motor_2.setPositionTolerance(50);

        Pose2d startPose = new Pose2d(0, 0);
        timer = new ElapsedTime();

        drive.setPoseEstimate(startPose);

        Trajectory Center_1 = drive.trajectoryBuilder(startPose)
                .splineTo(new Vector2d(variables.Center1x, variables.Center1y), 0)
                .build();
        Trajectory Center_2 = drive.trajectoryBuilder(Center_1.end(),true)
                .splineTo(new Vector2d(variables.Near2xCenter, variables.Near2y), Math.toRadians(90))
                .build();
        Trajectory Center_3 = drive.trajectoryBuilder(Center_2.end())
                .back (variables.NearBoard)
                .build();
        Trajectory Center_4 = drive.trajectoryBuilder(Center_3.end())
                .strafeRight(variables.Park1Center)
                .build();
        Trajectory Center_5 = drive.trajectoryBuilder(Center_4.end())
                .back(variables.Park2)
                .build();
        Trajectory Obs_11 = drive.trajectoryBuilder(startPose)
                .splineTo (new Vector2d(variables.Obs11x, variables.Obs11y), Math.toRadians(-90))
                .forward(variables.Obs11Forward)
                .build();
        Trajectory Obs_12 = drive.trajectoryBuilder(Obs_11.end(), true)
                .splineTo(new Vector2d(variables.Near2xCenter + 6 , variables.Near2y), Math.toRadians(90))
                .build();
        Trajectory Obs_13 = drive.trajectoryBuilder(Obs_12.end())
                .back (variables.NearBoard)
                .build();
        Trajectory Obs_14 = drive.trajectoryBuilder(Obs_13.end())
                .strafeRight(variables.Park1Center + 6)
                .build();
        Trajectory Obs_15 = drive.trajectoryBuilder(Obs_14.end())
                .back(variables.Park2)
                .build();
        Trajectory Free_11 = drive.trajectoryBuilder(startPose)
                .splineTo(new Vector2d(variables.Free11x, variables.Free11y), 0)
                .build();
        Trajectory Free_12 = drive.trajectoryBuilder(Free_11.end(), true)
                .splineTo(new Vector2d(variables.Near2xCenter - 6, variables.Near2y), Math.toRadians(90))
                .build();
        Trajectory Free_13 = drive.trajectoryBuilder(Free_12.end())
                .back (variables.NearBoard)
                .build();
        Trajectory Free_14 = drive.trajectoryBuilder(Free_13.end())
                .strafeRight(variables.Park1Center - 6)
                .build();
        Trajectory Free_15 = drive.trajectoryBuilder(Free_14.end())
                .back(variables.Park2)
                .build();
        Trajectory NF_11 = drive.trajectoryBuilder(startPose)
                .splineTo(new Vector2d(variables.Near2xCenter, variables.Near2y), Math.toRadians(270))
                .build();
        Trajectory NF_12 = drive.trajectoryBuilder(NF_11.end())
                .back (variables.NearBoard)
                .build();
        Trajectory NF_13 = drive.trajectoryBuilder(NF_12.end())
                .strafeRight(variables.Park1Center)
                .build();
        Trajectory NF_14 = drive.trajectoryBuilder(NF_13.end())
                .back(variables.Park2)
                .build();

        initOpenCV();
        FtcDashboard dashboard = FtcDashboard.getInstance();
        telemetry = new MultipleTelemetry(telemetry, dashboard.getTelemetry());
        FtcDashboard.getInstance().startCameraStream(controlHubCam, 30);

        waitForStart();

        if (isStopRequested())
            return;

        clawRightServo.turnToAngle(variables.gripDegrees1);
        clawLeftServo.turnToAngle(variables.gripDegrees);
        clawAngleServo.turnToAngle(variables.AutoCLawDown);
        timer.reset();
        while (timer.seconds() < 1);


        if ((int) cX > 200 && (int) cX < 400 && opModeIsActive()){
            position = variables.CENTRE;
            drive.followTrajectory(Center_1);
        }

        else if ((int) cX > 0 && (int) cX < 200 && opModeIsActive()){
            position = variables.LEFT;
            drive.followTrajectory(Free_11);
        }
        else if ((int) cX > 400 && (int) cX < 700 && opModeIsActive()){
            position = variables.RIGHT;
            drive.followTrajectory(Obs_11);

        }
        else if (opModeIsActive()) {
            position = variables.NOTDETECTED;
            ClawPark();
            drive.followTrajectory(NF_11);
        }
        controlHubCam.stopStreaming();


        if (position != variables.NOTDETECTED && opModeIsActive()) {
            DropPixel(true, false);
            ClawPark();
        }

        if (position == variables.NOTDETECTED && opModeIsActive()) {
            ArmUP();
            drive.followTrajectory(NF_12);
            DropPixel(true, true);
            ArmFromBoard();
            ArmPark();
            drive.followTrajectory(NF_13);
            drive.followTrajectory(NF_14);
        }

        if (position == variables.CENTRE && opModeIsActive()){
            drive.followTrajectory(Center_2);
            ArmUP();
            drive.followTrajectory(Center_3);
            DropPixel(false, true);
            ArmFromBoard();
            ArmPark();
            drive.followTrajectory(Center_4);
            drive.followTrajectory(Center_5);

        }
        if (position == variables.LEFT && opModeIsActive()){
            drive.followTrajectory(Free_12);
            ArmUP();
            drive.followTrajectory(Free_13);
            DropPixel(false, true);
            ArmFromBoard();
            ArmPark();
            drive.followTrajectory(Free_14);
            drive.followTrajectory(Free_15);

        }
        if (position == variables.RIGHT && opModeIsActive()){
            drive.followTrajectory(Obs_12);
            ArmUP();
            drive.followTrajectory(Obs_13);
            DropPixel(false, true);
            ArmFromBoard();
            ArmPark();
            drive.followTrajectory(Obs_14);
            drive.followTrajectory(Obs_15);

        }


        // Release resources

    }



void ClawDown()
{
    clawAngleServo.turnToAngle(variables.AutoCLawDown);
    /*timer.reset();
    while (timer.seconds() < 1);
*/
}
void DropPixel(boolean Purple, boolean Yellow)
{
    timer.reset();
    while (timer.seconds() < 0.5 ) {
    }

    if (Purple) clawLeftServo.turnToAngle(variables.gripDegrees1);
    if (Yellow) clawRightServo.turnToAngle(variables.gripDegrees);
    timer.reset();
    while (timer.seconds() < 0.2) ;

}
void ClawPark()
{

    clawLeftServo.turnToAngle(variables.gripDegrees);
    clawRightServo.turnToAngle(variables.gripDegrees1);
    clawAngleServo.turnToAngle(variables.AutoCLawPark);

}
void ArmUP()
{
    clawAngleServo.turnToAngle(variables.ClawAngleDeposit);
    m_motor.setTargetPosition(pos - variables.nearBoard);
    timer.reset();
    while (!m_motor.atTargetPosition() && timer.seconds() < variables.timer_motor ){
        m_motor.set(variables.speed_arm);
    }
    m_motor.stopMotor();
}
    void ArmFromBoard()
    {
        timer.reset();
        while (timer.seconds() < 0.2 ) {
        }
        clawAngleServo.turnToAngle(variables.ClawAngleDeposit-10);
        timer.reset();
        while (timer.seconds() < 0.2 ) {
        }

        m_motor.setTargetPosition(pos - variables.nearBoard+200);
        timer.reset();
        while (!m_motor.atTargetPosition() && timer.seconds() < variables.timer_motor ){
            m_motor.set(variables.speed_arm);
        }
        m_motor.stopMotor();
    }
void ArmPark(){
    ClawPark();
    m_motor.setTargetPosition(pos);
    timer.reset();
    while (!m_motor.atTargetPosition() && timer.seconds() < variables.timer_motor ){
        m_motor.set(variables.speed_arm);
    }
    m_motor.stopMotor();

}
   /* void ArmUP_Front()
    {
        clawAngleServo.turnToAngle(15);
        m_motor.setTargetPosition(pos - 600);
        timer.reset();
        while (!m_motor.atTargetPosition() && timer.seconds() < variables.timer_motor ){
            m_motor.set(variables.speed_arm*2);
        }
        m_motor_2.setTargetPosition(pos2+100);

        timer.reset();
        while (!m_motor_2.atTargetPosition() && timer.seconds() < variables.timer_motor_2) {
            m_motor_2.set(variables.speed_extender);
        }
        timer.reset();
        while (timer.seconds() < 10 ) {
        }
        m_motor.stopMotor();

        m_motor_2.stopMotor();

    }
*/
}
