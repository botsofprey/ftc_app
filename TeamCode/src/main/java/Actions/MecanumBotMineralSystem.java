package Actions;

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.Servo;

import java.io.IOException;

import Actions.HardwareWrappers.ServoHandler;
import Actions.HardwareWrappers.SpoolMotor;
import MotorControllers.MotorController;

public class MecanumBotMineralSystem implements ActionHandler{
    MotorController rotator;
    SpoolMotor extender;
    ServoHandler collector;
    HardwareMap hardwareMap;

    public MecanumBotMineralSystem(HardwareMap hw){
        hardwareMap = hw;
        try {
            rotator = new MotorController("rotator", "MotorConfig/NeverRest40.json", hardwareMap);
            extender = new SpoolMotor(new MotorController("extender", "MotorConfig/NeverRest40.json", hardwareMap), 50, 50, 100, hardwareMap);
            rotator.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            extender.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            rotator.setDirection(DcMotorSimple.Direction.FORWARD);
            extender.setDirection(DcMotorSimple.Direction.FORWARD);
            rotator.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        collector = new ServoHandler("collector", hardwareMap);
        collector.setDirection(Servo.Direction.FORWARD);
        collector.setServoRanges(0, 180);
    }

    public void collect() {collector.setPosition(1);}
    public void spit() {collector.setPosition(0);}
    public void pauseCollection() {collector.setPosition(0.5);}

    public void liftArm() {rotator.setMotorPower(1);}
    public void lowerArm() {rotator.setMotorPower(-1);}
    public void pauseRotator() {rotator.holdPosition();}

    public void extendArm() {extender.extend();}
    public void retractArm() {extender.retract();}
    public void pauseExtender() {extender.pause();}

    @Override
    public boolean doAction(String action, long maxTimeAllowed) {
        return false;
    }

    @Override
    public boolean stopAction(String action) {
        return false;
    }

    @Override
    public boolean startDoingAction(String action) {
        return false;
    }

    @Override
    public void kill() {
        collector.setPosition(collector.getActualPosition());
        rotator.killMotorController();
        extender.kill();
    }
}
