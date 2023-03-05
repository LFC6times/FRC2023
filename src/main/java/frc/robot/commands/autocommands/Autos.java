// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot.commands.autocommands;

import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.Commands;

import frc.robot.subsystems.ArmSubsystem;
import frc.robot.subsystems.ClawGripSubsystem;

import frc.robot.Constants.PositionConstants;

public final class Autos {
    /**
     * Example static factory for an autonomous command.
     */
    public static CommandBase exampleAuto(/* if you need a subsystem, pass it in as a parameter here, and make sure to pass them in when the method is called inside RobotContainer */) {
        // You can also pass in commands as parameters if you want to use existing commands as part of your autonomouse command (such as BalanceChargeStation.java *wink wink hint hint*)
        // Example command case
        return Commands.runOnce(() -> {
            // Autonomous scenario code
            
        }) /* susbsystems that are used inside the curly braces above must be passed in here */;

        // To implement a sequence of actions/commands use .andThen(), can be used to implement PathWeaver trajectories
        // To implement simultaneous actions/commands use .alongWith(), can also be used to implement PathWeaver trajectories
        // For example: return Commands.runOnce(() -> {}).andThen(RobotContainer.trajectory1Command);
        // Example of simultaneous implementation: return Commands.runOnce(() -> {}).alongWith(RobotContainer.trajectory1Command);
    }

    public static CommandBase defaultAuto() {
        // Example command case
        return Commands.runOnce(() -> {
            // Autonomous scenario code
        });

    }

    public static CommandBase armPlaceConeAuto(ArmSubsystem arm, ClawGripSubsystem claw) {
        return Commands.runOnce(() -> {
            double[] newArmPosition = PositionConstants.TOP_RIGHT_POS; // or maybe top left pos?
            arm.setIntendedCoordinates(newArmPosition[0], newArmPosition[1], newArmPosition[2], false);
            // we might have to wait before doing this, this could release the cone too early
            claw.setClawClosed(false); // open claw
        }).alongWith(Commands.runOnce(() -> {
            // drive here!
        }));
    }

    private Autos() {
        throw new UnsupportedOperationException("Autos is a utility class and cannot be instantiated!");
    }
}
