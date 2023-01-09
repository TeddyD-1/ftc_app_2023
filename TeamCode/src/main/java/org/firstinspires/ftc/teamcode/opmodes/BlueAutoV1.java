package org.firstinspires.ftc.teamcode.opmodes;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import com.acmerobotics.roadrunner.trajectory.Trajectory;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.roadrunner.drive.PoseStorage;
import org.firstinspires.ftc.teamcode.roadrunner.drive.SampleMecanumDrive;
import org.firstinspires.ftc.teamcode.subsystems.Belt;
import org.firstinspires.ftc.teamcode.subsystems.Claw;
import org.firstinspires.ftc.teamcode.subsystems.Lift;
import org.firstinspires.ftc.teamcode.subsystems.TurnTable;

@Autonomous(name = "RedAuto")
@Config
public class BlueAutoV1 extends LinearOpMode {

    State currentState = State.IDLE;
    SampleMecanumDrive drive;
    Pose2d startingPos = new Pose2d(12, -62, Math.toRadians(0));
    ElapsedTime runtime = new ElapsedTime();
    int cyclesCompleted = 0;

    Lift lift = new Lift();
    Claw claw = new Claw();
    Belt belt = new Belt();
    TurnTable turntable = new TurnTable();
    
    Trajectory firstHighPole, firstConeStack, coneStack, placeHighPole, park;

    // For drivetrain states/trajectories, GO_{FIRST PLACE}_{LAST PLACE}
    enum State {
        GO_SUBSTATION_HIGHJUNC,
        DROP_OFF_HIGH_CONE,
        GO_HIGHJUNC_CONESTACKS,
        PICK_UP_FROM_CONESTACKS,
        GO_CONESTACKS_HIGHJUNC,
        IDLE
    }

    void next(State s) {
        time = runtime.seconds();
        currentState = s;
    }
    
    public void buildTrajectories() {

        /*
        IMPORTANT POINTS:
        (12, 24): Corner of cone
        (52, 12): Pickup position
        (20, 12): Corner of cone
        (10, 35): Test ending position
         */

        /*
        ORDER OF TRAJECTORIES:
        firstHighPole
        firstConeStack

        loop:
        placeHighPole
        coneStack
        */

        firstHighPole = drive.trajectoryBuilder(startingPos)
                .lineTo(new Vector2d(12, 24))
                .build();

        firstConeStack = drive.trajectoryBuilder(firstHighPole.end())
                .splineTo(new Vector2d(52,12), Math.toRadians(0))
                .build();

        placeHighPole = drive.trajectoryBuilder(firstConeStack.end())
                .lineTo(new Vector2d(20, 12))
                .build();

        coneStack = drive.trajectoryBuilder(placeHighPole.end())
                .splineTo(new Vector2d(52,12), Math.toRadians(0))
                .build();

        park = drive.trajectoryBuilder(placeHighPole.end())
                .splineToConstantHeading(new Vector2d(10, 35), Math.toRadians(90))
                .build();
    }


    @Override
    public void runOpMode() throws InterruptedException {

        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        // hardware map only accessible in runOpMode()
        drive = new SampleMecanumDrive(hardwareMap);
        buildTrajectories();

        runtime.reset();
        waitForStart();

        claw.init(hardwareMap);
        belt.init(hardwareMap);
        turntable.init(hardwareMap);
        lift.init(hardwareMap);

        while (opModeIsActive()) {
            telemetry.addLine("running");
            double elapsed = runtime.seconds() - time;

            switch(currentState) {
                case GO_SUBSTATION_HIGHJUNC:
                    if (!drive.isBusy()) {
                        drive.followTrajectoryAsync(firstHighPole);
                        next(State.IDLE);
                    }
                    break;
                case DROP_OFF_HIGH_CONE:
                    if (!drive.isBusy()) {
                        drive.followTrajectoryAsync(placeHighPole);
                        cyclesCompleted++;
                        next(State.GO_HIGHJUNC_CONESTACKS);
                    }
                    break;
                case GO_HIGHJUNC_CONESTACKS:
                    if (!drive.isBusy()) {
                        drive.followTrajectoryAsync(coneStack);
                        next(State.PICK_UP_FROM_CONESTACKS);
                    }
                    break;
                case PICK_UP_FROM_CONESTACKS:
                    if (!drive.isBusy()) {
                        drive.followTrajectoryAsync(placeHighPole);
                        next(State.GO_CONESTACKS_HIGHJUNC);
                    }
                    break;
            }

            drive.update();

            Pose2d poseEstimate = drive.getPoseEstimate();
            PoseStorage.currentPose = poseEstimate;

            telemetry.addData("x", poseEstimate.getX());
            telemetry.addData("y", poseEstimate.getY());
            telemetry.addData("heading", poseEstimate.getHeading());
            telemetry.addData("current state", currentState);
            telemetry.update();
        }
    }
}
