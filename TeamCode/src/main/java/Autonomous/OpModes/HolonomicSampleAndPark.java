/* Copyright (c) 2018 FIRST. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted (subject to the limitations in the disclaimer below) provided that
 * the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of FIRST nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 *
 * NO EXPRESS OR IMPLIED LICENSES TO ANY PARTY'S PATENT RIGHTS ARE GRANTED BY THIS
 * LICENSE. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package Autonomous.OpModes;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import org.firstinspires.ftc.robotcore.external.ClassFactory;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer;
import org.firstinspires.ftc.robotcore.external.navigation.VuforiaLocalizer.CameraDirection;
import org.firstinspires.ftc.robotcore.external.tfod.Recognition;
import org.firstinspires.ftc.robotcore.external.tfod.TFObjectDetector;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import Autonomous.*;
import DriveEngine.JennyNavigation;

/**
 * This 2018-2019 OpMode illustrates the basics of using the TensorFlow Object Detection API to
 * determine the position of the gold and silver minerals.
 * <p>
 * Use Android Studio to Copy this Class, and Paste it into your team's code folder with a new name.
 * Remove or comment out the @Disabled line to add this opmode to the Driver Station OpMode list.
 * <p>
 * IMPORTANT: In order to use this OpMode, you need to obtain your own Vuforia license key as
 * is explained below.
 */
@Autonomous(name = "Silver Side Autonomous Holonomic Drive", group = "Concept")
//@Disabled
public class HolonomicSampleAndPark extends LinearOpMode {
    private static final String TFOD_MODEL_ASSET = "RoverRuckus.tflite";
    private static final String LABEL_GOLD_MINERAL = "Gold Mineral";
    private static final String LABEL_SILVER_MINERAL = "Silver Mineral";
    enum Position {LEFT, CENTER, RIGHT};
    Position goldMineralPosition = Position.CENTER;
    final int LEFT = 0, CENTER = 1, RIGHT = 2;
    int[] positionVotes = {0, 0, 0};
    boolean hasNotMoved = true;
    JennyNavigation navigation;

    /*
     * IMPORTANT: You need to obtain your own license key to use Vuforia. The string below with which
     * 'parameters.vuforiaLicenseKey' is initialized is for illustration only, and will not function.
     * A Vuforia 'Development' license key, can be obtained free of charge from the Vuforia developer
     * web site at https://developer.vuforia.com/license-manager.
     *
     * Vuforia license keys are always 380 characters long, and look as if they contain mostly
     * random data. As an example, here is a example of a fragment of a valid key:
     *      ... yIgIzTqZ4mWjk9wd3cZO9T1axEqzuhxoGlfOOI2dRzKS4T0hQ8kT ...
     * Once you've obtained a license key, copy the string from the Vuforia web site
     * and paste it in to your code on the next line, between the double quotes.
     */
    private static final String VUFORIA_KEY = "Afh+Mi//////AAAAGT/WCUCZNUhEt3/AvBZOSpKBjwlgufihL3d3H5uiMfbq/1tDOM6w+dgMIdKUvVFEjNNy9zSaruPDbwX0HwjI6BEvxuWbw+UcZFcfF7i4g7peD4zSCEyZBCi59q5H/a2aTsnJVaG0WO0pPawHDuuScrMsA/QPKQGV/pZOT6rK8cW2C3bEkZpZ1qqkSM5zNeKs2OQtr8Bvl2nQiVK6mQ3ZT4fxWGb7P/iTZ4k1nEhkxI56sr5HlxmSd0WOx9i8hYDTJCASU6wwtOeUHZYigZmdRYuARS+reLJRXUylirmoU8kVvMK1p2Kf8dajEWsTuPwBec/BSaygmpqD0WkAc2B1Vmaa/1zTRfYNR3spIfjHQCYu";

    /**
     * {@link #vuforia} is the variable we will use to store our instance of the Vuforia
     * localization engine.
     */
    private VuforiaLocalizer vuforia;

    /**
     * {@link #tfod} is the variable we will use to store our instance of the Tensor Flow Object
     * Detection engine.
     */
    private TFObjectDetector tfod;

    @Override
    public void runOpMode() {
        // The TFObjectDetector uses the camera frames from the VuforiaLocalizer, so we create that
        // first.
        initVuforia();
        try {
            navigation = new JennyNavigation(hardwareMap, new Location(0, 0), 0,"RobotConfig/JennyV2.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (ClassFactory.getInstance().canCreateTFObjectDetector()) {
            initTfod();
        } else {
            telemetry.addData("Sorry!", "This device is not compatible with TFOD");
        }

        /** Wait for the game to begin */
        telemetry.addData(">", "Press Play to start tracking");
        telemetry.update();
        waitForStart();
        long startTime = System.currentTimeMillis();
        if (opModeIsActive()) {
            /** Activate Tensor Flow Object Detection. */
            if (tfod != null) {
                tfod.activate();
            }
            while (opModeIsActive()) {
                if (tfod != null) {
                    // getUpdatedRecognitions() will return null if no new information is available since
                    // the last time that call was made.
                    List<Recognition> updatedRecognitions = tfod.getUpdatedRecognitions();
                    if (updatedRecognitions != null) {
                        Recognition[] minerals = filter(updatedRecognitions);
                        telemetry.addData("# Object Detected", minerals.length);
                        if (minerals.length >= 3) {
                            int goldMineralX = -1;
                            int silverMineral1X = -1;
                            int silverMineral2X = -1;
                            for (Recognition recognition : minerals) {
                                if (recognition.getLabel().equals(LABEL_GOLD_MINERAL)) {
                                    goldMineralX = (int) recognition.getLeft();
                                } else if (silverMineral1X == -1) {
                                    silverMineral1X = (int) recognition.getLeft();
                                } else {
                                    silverMineral2X = (int) recognition.getLeft();
                                }
                            }
                            if (goldMineralX != -1 && silverMineral1X != -1 && silverMineral2X != -1) {
                                if (goldMineralX < silverMineral1X && goldMineralX < silverMineral2X) {
                                    goldMineralPosition = Position.LEFT;
                                    positionVotes[LEFT]++;
                                    telemetry.addData("Gold Mineral Position", "Left");
                                } else if (goldMineralX > silverMineral1X && goldMineralX > silverMineral2X) {
                                    goldMineralPosition = Position.RIGHT;
                                    positionVotes[RIGHT]++;
                                    telemetry.addData("Gold Mineral Position", "Right");
                                } else {
                                    goldMineralPosition = Position.CENTER;
                                    positionVotes[CENTER]++;
                                    telemetry.addData("Gold Mineral Position", "Center");
                                }
                            }
                        }
                    }
                }

                if(positionVotes[LEFT] + positionVotes[CENTER] + positionVotes[RIGHT] > 15 && hasNotMoved) {
                    if (positionVotes[LEFT] > positionVotes[RIGHT] && positionVotes[LEFT] > positionVotes[CENTER]) {
                        telemetry.addData("driving...", "left");
                        telemetry.update();
                        navigation.driveDistance(50, -20, 15, this);
                    } else if (positionVotes[RIGHT] > positionVotes[LEFT] && positionVotes[RIGHT] > positionVotes[CENTER]) {
                        telemetry.addData("driving...", "right");
                        telemetry.update();
                        navigation.driveDistance(50, 20, 15, this);
                    } else {
                        telemetry.addData("driving...", "center");
                        telemetry.update();
                        navigation.driveDistance(50, 0, 15, this);
                    }
                    hasNotMoved = false;
                } else if(System.currentTimeMillis() - startTime >= 25000 && hasNotMoved) navigation.driveDistance(50, 0, 15, this);

                telemetry.update();
            }
            navigation.stopNavigation();
        }

        if (tfod != null) {
            tfod.shutdown();
        }
    }

    /**
     * Initialize the Vuforia localization engine.
     */
    private void initVuforia() {
        /*
         * Configure Vuforia by creating a Parameter object, and passing it to the Vuforia engine.
         */
        VuforiaLocalizer.Parameters parameters = new VuforiaLocalizer.Parameters();

        parameters.vuforiaLicenseKey = VUFORIA_KEY;
        parameters.cameraDirection = CameraDirection.BACK;

        //  Instantiate the Vuforia engine
        vuforia = ClassFactory.getInstance().createVuforia(parameters);

        // Loading trackables is not necessary for the Tensor Flow Object Detection engine.
    }

    /**
     * Initialize the Tensor Flow Object Detection engine.
     */
    private void initTfod() {
        int tfodMonitorViewId = hardwareMap.appContext.getResources().getIdentifier(
                "tfodMonitorViewId", "id", hardwareMap.appContext.getPackageName());
        TFObjectDetector.Parameters tfodParameters = new TFObjectDetector.Parameters(tfodMonitorViewId);
        tfod = ClassFactory.getInstance().createTFObjectDetector(tfodParameters, vuforia);
        tfod.loadModelFromAsset(TFOD_MODEL_ASSET, LABEL_GOLD_MINERAL, LABEL_SILVER_MINERAL);
    }

    private Recognition[] filter(List<Recognition> minerals) {
        Recognition[] mineralsArray = minerals.toArray(new Recognition[0]);
        Arrays.sort(mineralsArray, new Comparator<Recognition>() {
            @Override
            public int compare(Recognition r1, Recognition r2) {
                return (int) (r1.getBottom() - r2.getBottom());
            }
        });
        return mineralsArray;
    }
}