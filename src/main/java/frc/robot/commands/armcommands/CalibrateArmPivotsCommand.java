package frc.robot.commands.armcommands;

import frc.robot.subsystems.ArmSubsystem;
import edu.wpi.first.wpilibj2.command.CommandBase;

public class CalibrateArmPivotsCommand extends CommandBase {
    private final ArmSubsystem arm;

    private static enum CalibrationStates {
        CALIB_PIVOT_2,
        CALIB_PIVOT_1,
        FINISH
    }

    private CalibrationStates calibrationState;

    public CalibrateArmPivotsCommand(ArmSubsystem arm) {
        this.arm = arm;
        // Use addRequirements() here to declare subsystem dependencies.
        addRequirements(arm);
    }

    // Called when the command is initially scheduled.
    @Override
    public void initialize() {
        this.arm.setPIDControlState(false);
        this.calibrationState = CalibrationStates.CALIB_PIVOT_2;
        this.arm.setPivot1Speed(0);
        this.arm.setPivot2Speed(0);
    }

    // Called every time the scheduler runs while the command is scheduled.
    @Override
    public void execute() {
        this.arm.setPIDControlState(false);
        switch(this.calibrationState) {
            case CALIB_PIVOT_2:
                if(!this.arm.getPivot2LimitPressed()) {
                    this.arm.setPivot2Speed(-0.2);
                } else {
                    this.arm.setPivot2Speed(0);
                    this.calibrationState = CalibrationStates.CALIB_PIVOT_1;
                }
                break;
            case CALIB_PIVOT_1:
                if(!this.arm.getPivot1LimitPressed()) {
                    this.arm.setPivot1Speed(-0.2);
                } else {
                    this.arm.setPivot1Speed(0);
                    this.calibrationState = CalibrationStates.FINISH;
                }
                break;
            case FINISH:
                this.arm.setPivot1Speed(0);
                this.arm.setPivot2Speed(0);
                break;
        }
    }

    // Called once the command ends or is interrupted.
    @Override
    public void end(boolean interrupted) {
        this.arm.resetCoords();
        this.arm.setPIDControlState(true);
        this.arm.setPivot1Speed(0);
        this.arm.setPivot2Speed(0);
    }

    // Returns true when the command should end.
    @Override
    public boolean isFinished() {
        return this.calibrationState == CalibrationStates.FINISH || (this.arm.getPivot1LimitPressed() && this.arm.getPivot2LimitPressed());
    }

}