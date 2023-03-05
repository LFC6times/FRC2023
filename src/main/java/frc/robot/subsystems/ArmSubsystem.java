package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;
import com.revrobotics.RelativeEncoder;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.geometry.Pose3d;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.wpilibj.DigitalInput;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants.ArmConstants;
import frc.robot.Constants.PortConstants;
import frc.robot.Constants.PositionConstants;
import frc.robot.util.ForwardKinematicsUtil;
import frc.robot.util.InverseKinematicsUtil;
import frc.robot.util.MathUtil;
import frc.robot.util.NetworkTablesUtil;

public class ArmSubsystem extends SubsystemBase {
    private final CANSparkMax pivot1;
    private final CANSparkMax pivot2;
    private final CANSparkMax turret;

    private final RelativeEncoder pivot1Encoder;
    private final RelativeEncoder pivot2Encoder;
    private final RelativeEncoder turretEncoder;

    private final DigitalInput arm1Limit;
    private final DigitalInput arm2Limit;

    private final PIDController pidController1, pidController2, pidController3;

    private double targetX;
    private double targetY;
    private double targetZ;

    private double cur_x;
    private double cur_y;
    private double cur_z;

    private double targetAngle1;
    private double targetAngle2;
    private double targetAngleTurret;

    private static final double MAX_OUTPUT = 0.1;
    private static final double MIN_OUTPUT = -0.1;

    private boolean pidOn = false;
    private double[] startingCoords = ForwardKinematicsUtil.getCoordinatesFromAngles(ArmConstants.ARM_1_INITIAL_ANGLE, ArmConstants.ARM_2_INITIAL_ANGLE, 0);

    // arm control constructor
    public ArmSubsystem() {
        // Initialize arm motors
        this.pivot1 = new CANSparkMax(PortConstants.PIVOT1_PORT, MotorType.kBrushless);
        this.pivot2 = new CANSparkMax(PortConstants.PIVOT2_PORT, MotorType.kBrushless);
        this.turret = new CANSparkMax(PortConstants.TURRET_PORT, MotorType.kBrushless);

        this.pivot1.setInverted(true);
        this.pivot2.setInverted(true);
        this.turret.setInverted(false);

        // Set up arm encoders and position conversion factors
        this.pivot1Encoder = this.pivot1.getEncoder();
        this.pivot2Encoder = this.pivot2.getEncoder();
        this.turretEncoder = this.turret.getEncoder();

        // Arm Angle Conversion Factors
        this.pivot1Encoder.setPositionConversionFactor(2.88); // 125:1 gearbox 
        this.pivot2Encoder.setPositionConversionFactor(2.88); // 125:1 gearbox
        this.turretEncoder.setPositionConversionFactor(1); // 60:1 gearbox with drive wheel to lazy susan ratio
        // END

        this.pivot1Encoder.setPosition(ArmConstants.ARM_1_INITIAL_ANGLE);
        this.pivot2Encoder.setPosition(ArmConstants.ARM_2_INITIAL_ANGLE);
        this.turretEncoder.setPosition(0);

        // TODO: TUNE
        this.pidController1 = new PIDController(1.69e-2, 0, 0); // nice
        this.pidController1.setTolerance(ArmConstants.ANGLE_DELTA);
        this.pidController2 = new PIDController(9.6e-3, 0, 0);
        this.pidController2.setTolerance(ArmConstants.ANGLE_DELTA);
        this.pidController3 = new PIDController(9.6e-3, 0, 0);
        this.pidController3.setTolerance(ArmConstants.ANGLE_DELTA);
        // END

        // Initialize arm limit switches
        this.arm1Limit = new DigitalInput(PortConstants.PIVOT_1_LIMIT_PORT);
        this.arm2Limit = new DigitalInput(PortConstants.PIVOT_2_LIMIT_PORT);

        // Set starting arm angles
        this.targetAngle1 = ArmConstants.ARM_1_INITIAL_ANGLE;
        this.targetAngle2 = ArmConstants.ARM_2_INITIAL_ANGLE;
        this.targetAngleTurret = 0;

        // Get starting coords from the initial angle constants
        this.targetX = startingCoords[0];
        this.targetY = startingCoords[1];
        this.targetZ = startingCoords[2];
        this.cur_x = startingCoords[0];
        this.cur_y = startingCoords[1];
        this.cur_z = startingCoords[2];
    }

    public void reset() {
        this.pidOn = false;
        this.cur_x = startingCoords[0];
        this.targetY = startingCoords[1];
        this.targetZ = startingCoords[2];
    }

    /**
     * Changes the intended coordinates by dx, dy, and dz
     */
    public void moveVector(double dx, double dy, double dz) {
        updateCurrentCoordinates();
        setIntendedCoordinates(targetX + dx, targetY + dy, targetZ + dz, false);
    }

    /**
     * Get the current angles from motor encoders in DEGREES
     * 
     * @return [pivot1Angle, pivot2Angle, turretAngle]
     */
    public double[] getCurrentAnglesDeg() {
        double angle1 = pivot1Encoder.getPosition();
        double angle2 = pivot2Encoder.getPosition();
        double angle3 = turretEncoder.getPosition();

        return new double[]{angle1, angle2, angle3};
    }

    /**
     * Get the current angles from motor encoders in radians
     * 
     * @return [pivot1Angle, pivot2Angle, turretAngle]
     */
    public double[] getCurrentAnglesRad() {
        double angle1 = Math.toRadians(pivot1Encoder.getPosition());
        double angle2 = Math.toRadians(pivot2Encoder.getPosition());
        double angle3 = Math.toRadians(turretEncoder.getPosition());

        return new double[]{angle1, angle2, angle3};
    }

    public void setPivot1Speed(double speed) {
        this.pivot1.set(speed);
    }

    public void setPivot2Speed(double speed) {
        this.pivot2.set(speed);
    }

    public void setTurretSpeed(double speed) {
        this.turret.set(speed);
    }

    public void stopAllMotors() {
        this.pivot1.set(0);
        this.pivot2.set(0);
        this.turret.set(0);
    }

    /**
     * Uses motor encoder angles to update the current coordinates
     */
    public void updateCurrentCoordinates() {
        double[] tempAngles = getCurrentAnglesDeg();
        double[] coords = ForwardKinematicsUtil.getCoordinatesFromAngles(tempAngles[0], tempAngles[1], tempAngles[2]);
        this.cur_x = coords[0];
        this.cur_y = coords[1];
        this.cur_z = coords[2];
    }

    /*
     * returns current coordinates
     * 
     * @return [x, y, z], where y is the height above ground.
     */
    public double[] getCurrentCoordinates() {
        updateCurrentCoordinates();
        return new double[]{this.cur_x, this.cur_y, this.cur_z};
    }

    /**
     * return coordinates in which the arm "should" move towards
     * 
     * @return [x, y, z], where y is the height above ground
     */
    public double[] getIntendedCoordinates() {
        return new double[]{this.targetX, this.targetY, this.targetZ};
    }

    public boolean getPivot1LimitPressed() {
        return !this.arm1Limit.get();
    }

    public boolean getPivot2LimitPressed() {
        return !this.arm2Limit.get();
    }

    public void goTowardIntendedCoordinates() {
        double[] angles = getCurrentAnglesDeg();

        if (Double.isNaN(angles[0]) || Double.isNaN(angles[1]) || Double.isNaN(angles[2]) || Double.isNaN(targetAngle1) || Double.isNaN(targetAngle2) || Double.isNaN(targetAngleTurret)) {
            System.out.println("An angle is NaN, so skip");
            return;
        }

        double p1Speed = pidController1.calculate(angles[0], targetAngle1);
        double p2Speed = pidController2.calculate(angles[1], targetAngle2);
        double turretSpeed = pidController3.calculate(angles[2], targetAngleTurret);
        System.out.println(angles[0] + " " + angles[1] + " " + " " + angles[2]);
        // System.out.println(targetAngle1 + " " + targetAngle2 + " " );

        if (Double.isNaN(p1Speed) || Double.isNaN(p2Speed) || Double.isNaN(turretSpeed)) {
            System.out.println("PID is NaN, so skip");
            return;
        }

        p1Speed = Math.min(MAX_OUTPUT, Math.max(p1Speed, MIN_OUTPUT));
        p2Speed = Math.min(MAX_OUTPUT, Math.max(p2Speed, MIN_OUTPUT));
        turretSpeed = Math.min(MAX_OUTPUT, Math.max(turretSpeed, MIN_OUTPUT));
        System.out.println("SPEEDS: " + p1Speed + " " + p2Speed + " " + turretSpeed);
        setPivot1Speed(p1Speed);
        setPivot2Speed(p2Speed);
        setTurretSpeed(turretSpeed);
    }

    /**
     * sets the coordinate in which the arm "should" move towards
     * 
     * @param x the target x coordinate
     * @param y the target y coordinate (height)
     * @param z the target z coordinate
     * @param flipped whether the arm should act "flipped", i.e. the claw would approach from the gamepiece from the top. True for "top approach", False for "side approach"
     */
    public void setIntendedCoordinates(double x, double y, double z, boolean flipped) {
        if (this.targetX == x && this.targetY == y && this.targetZ == z) { // if intended coordinates are same, then don't change target
            return;
        }

        /* 
        double dist = MathUtil.distance(0, x, 0, y, 0, z);
        if(dist > ArmConstants.LIMB1_LENGTH + ArmConstants.LIMB2_LENGTH - ArmConstants.MAX_REACH_REDUCTION || dist < ArmConstants.MIN_HOR_DISTANCE) {
            return;
        }*/

        // Updates target Angles
        double[] targetAngles = InverseKinematicsUtil.getAnglesFromCoordinates(x, y, z, flipped);

        if (Double.isNaN(targetAngles[0]) || Double.isNaN(targetAngles[1]) || Double.isNaN(targetAngles[2])) {
            System.out.println("Hi this is the Arm Death Prevention Hotline @copyright setIntendedCoordinates");
            return;
        }

        // Updates target angles
        targetAngle1 = targetAngles[0];
        targetAngle2 = targetAngles[1];
        targetAngleTurret = targetAngles[2];

        // Updates target coordinates
        double[] adjustedCoordinates = ForwardKinematicsUtil.getCoordinatesFromAngles(targetAngle1, targetAngle2, targetAngleTurret);
        this.targetX = adjustedCoordinates[0];
        this.targetY = adjustedCoordinates[1];
        this.targetZ = adjustedCoordinates[2];
    }

    public CommandBase calibrateArm() {
        pidOn = false;
        return this.runOnce(() -> {
            while (!getPivot2LimitPressed()) {
                setPivot2Speed(-0.1);
            }
            setPivot2Speed(0);
            while (!getPivot1LimitPressed()) {
                setPivot1Speed(-0.1);
            }
            setPivot1Speed(0);
            this.pivot1Encoder.setPosition(ArmConstants.ARM_1_INITIAL_ANGLE);
            this.pivot2Encoder.setPosition(ArmConstants.ARM_2_INITIAL_ANGLE);
            pidOn = true;
        });
    }

    /**
     * Sets the usage of PID control for the arm.
     * @param value True for enabled PID, False for disabled.
     */
    public void setPIDControlState(boolean value) {
        pidOn = value;
    }

    /**
     * Get the usage of PID control for the arm.
     * @return True if enabled, false otherwise
     */
    public boolean getPIDControlOn() {
        return pidOn;
    }

    /**
     * Returns the current claw pose. Note that the claw pose is stored as {x, z, y}, where y is the height of the claw. This allows you to use {@link Pose3d#toPose2d()} to get a correct 2D pose.
     * @return A Pose3d object representing the current claw pose.
     */
    public Pose3d getClawPose() {
        this.updateCurrentCoordinates(); // Make sure the coordinates are the latest ones.
        return new Pose3d(this.cur_x, this.cur_z, this.cur_y, new Rotation3d()); // z and y are swapped to handle our global coordinate system (the final coord parameter is the height).
    }

    @Override
    public void periodic() {
        System.out.println("ARM MOTOR ENCODERS: PIV1: " + this.pivot1Encoder.getPosition() + ", PIV2: " + this.pivot2Encoder.getPosition() + ", TURRET: " + this.turretEncoder.getPosition());
        System.out.println("TARGET COORDS: " + targetX + ", " + targetY + ", " + targetZ);
        System.out.println("TARGET ANGLES: " + targetAngle1 + ", " + targetAngle2 + ", " + targetAngleTurret);
        // System.out.println("CURRENT TARGET COORDS ARM: " + targetX + ", " + targetY + ", " + targetZ);
        // System.out.println("ARM LIMIT SWITCHES: LIM1: " + this.getPivot1LimitPressed() + ", LIM2: " + this.getPivot2LimitPressed());
        double[] startingCoords = ForwardKinematicsUtil.getCoordinatesFromAngles(ArmConstants.ARM_1_INITIAL_ANGLE, ArmConstants.ARM_2_INITIAL_ANGLE, this.getCurrentAnglesDeg()[2]);
        
        // handles limit switches
        if (getPivot1LimitPressed() && Math.abs(this.pivot1Encoder.getPosition() - ArmConstants.ARM_1_INITIAL_ANGLE) > 0.1) {
            this.pivot1Encoder.setPosition(ArmConstants.ARM_1_INITIAL_ANGLE);
            this.targetX = startingCoords[0];
            this.targetY = startingCoords[1];
            this.targetZ = startingCoords[2];
            this.cur_x = startingCoords[0];
            this.cur_y = startingCoords[1];
            this.cur_z = startingCoords[2];
        }

        if (getPivot2LimitPressed() && Math.abs(this.pivot2Encoder.getPosition() - ArmConstants.ARM_2_INITIAL_ANGLE) > 0.1) {
            this.pivot2Encoder.setPosition(ArmConstants.ARM_2_INITIAL_ANGLE);
            this.targetX = startingCoords[0];
            this.targetY = startingCoords[1];
            this.targetZ = startingCoords[2];
            this.cur_x = startingCoords[0];
            this.cur_y = startingCoords[1];
            this.cur_z = startingCoords[2];
        }

        /*
         * Moves arm to specific positions for placing game pieces. The robot is assumed to be positioned directly in front of an april tag.
         * Each key corresponds to a specific position on a 3x3 grid, as shown below:
         *          [7  8  9]
         *          [4  5  6]
         *          [1  2  3]
         *              ^
         *            robot
         * 
         * There are 3 of these 3x3 grids.
         * 
         * A check for a joystick button pressed or something similar can be added if we want to add another check other than the key on the keyboard being pressed
         */

        //TODO: fix - ebay kid, 3-1-2023. the constants are wrong
        /*
        double[] coordinates = new double[3];
        int currKey = NetworkTablesUtil.getKeyInteger();
        switch (currKey) {
            case 1:
                coordinates = PositionConstants.BOTTOM_LEFT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 2:
                coordinates = PositionConstants.BOTTOM_MIDDLE_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 3:
                coordinates = PositionConstants.BOTTOM_RIGHT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 4:
                coordinates = PositionConstants.CENTER_LEFT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 5:
                coordinates = PositionConstants.CENTER_MIDDLE_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 6:
                coordinates = PositionConstants.CENTER_RIGHT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 7:
                coordinates = PositionConstants.TOP_LEFT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 8:
                coordinates = PositionConstants.TOP_CENTER_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            case 9:
                coordinates = PositionConstants.TOP_RIGHT_POS;
                setIntendedCoordinates(coordinates[0], coordinates[1], coordinates[2], false);
                break;
            default:
                System.out.println("A key within 1-9 was not pressed");
                break;
                
        }
        */
        

        //handles PID
        if (pidOn) {
            goTowardIntendedCoordinates();
        }
    }

    @Override
    public void simulationPeriodic() {
        // This method will be called once per scheduler run during simulation
    }
}