package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.cscore.UsbCamera;
import edu.wpi.first.cameraserver.CameraServer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.Relay;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Relay.*;
import edu.wpi.first.wpilibj.DigitalInput;

public class Robot extends TimedRobot {
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ty = table.getEntry("ty"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ta = table.getEntry("ta"); // area of the object
  NetworkTableEntry tv = table.getEntry("tv"); // 1 if have vision 0 if no vision
  NetworkTableEntry tlong = table.getEntry("tlong"); // length of longest side
  NetworkTableEntry tshort = table.getEntry("tshort"); // length of shortest side
  NetworkTableEntry tvert = table.getEntry("tvert"); // vertical distance
  NetworkTableEntry thor = table.getEntry("thor"); // horizontal distance
  NetworkTableEntry getpipe = table.getEntry("getpipe"); // this tells us what "pipeline" we are on, basically different
                                                         // settings for the camera
  NetworkTableEntry ts = table.getEntry("ts"); // skew or rotation of target

  Joystick joy = new Joystick(0);

  PWMVictorSPX frontLeft = new PWMVictorSPX(9);
  PWMVictorSPX frontRight = new PWMVictorSPX(7);
  PWMVictorSPX rearLeft = new PWMVictorSPX(8);
  PWMVictorSPX rearRight = new PWMVictorSPX(6);

  VictorSP solo = new VictorSP(2);
  VictorSP lift1 = new VictorSP(4);
  VictorSP lift2 = new VictorSP(5);
  VictorSP intake = new VictorSP(3);

  Relay belt = new Relay(1);
  Relay intakeFlip = new Relay(0);

  SpeedControllerGroup left = new SpeedControllerGroup(frontLeft, rearLeft);
  SpeedControllerGroup right = new SpeedControllerGroup(frontRight, rearRight);
  SpeedControllerGroup lift = new SpeedControllerGroup(lift1, lift2);
  SpeedControllerGroup yes = new SpeedControllerGroup(frontLeft, frontRight, rearLeft, rearRight);

  DifferentialDrive buffet = new DifferentialDrive(left, right);

  UsbCamera cam0;

  double pizza;
  double taco;

  double camx;
  double camy;
  double camarea;
  double targetWidth;
  double degreeWidth = 0;
  double targetRatio;
  double targetRatioInverse;
  double targetDistance;
  double vertAngle;
  double soloPew;

  boolean aligned;
  boolean intakeRun;
  boolean shootRun = false;
  boolean firstTimeThru = true;

  DigitalInput inDown = new DigitalInput(6);
  DigitalInput inUp = new DigitalInput(5);
  DigitalInput liftUp = new DigitalInput(4);
  DigitalInput liftDown = new DigitalInput(3);
  DigitalInput b1 = new DigitalInput(0);
  DigitalInput b2 = new DigitalInput(1);
  DigitalInput b3 = new DigitalInput(2);

  int beltDelay;

  double P = 1, I = 0, D = 1; // alignment
  double error, setpoint = 0, piAlign; // alignement
  double pDistance = 1, iDistance = 0, dDistance = 1; // distance shooter
  double errorDistance, setDistance = -8, piDistance; // distance shooter


  @Override
  public void robotInit() {
    // Does the most important part of our code
    SmartDashboard.putString("General Kenobi", "Hello there");
    cam0 = CameraServer.getInstance().startAutomaticCapture(0);
  }

  @Override
  public void robotPeriodic() {
SmartDashboard.putNumber("distance", distance());
SmartDashboard.putNumber("Solo Speed", soloPew);
SmartDashboard.putNumber("Vert Angle", vertAngle);
SmartDashboard.putNumber("PIAlignment", piAlign);
SmartDashboard.putNumber("PIShooter", piDistance);
  }

  @Override
  public void autonomousInit() {

  }

  @Override
  public void autonomousPeriodic() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    if (inDown.get() == false) {
      intakeFlip.set(Value.kReverse);
    } else if (tv == 1) {
      intakeFlip.set(Value.kOff);
      autoShoot();
    } else {
      intakeFlip.set(Value.kOff);
    }
  }
  @Override
  public void teleopPeriodic() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    pizza = joy.getRawAxis(1);
    taco = joy.getRawAxis(4);
    buffet.arcadeDrive(-pizza, taco);

    // Moves intake up and down
    if (inUp.get() == false && joy.getRawAxis(3) > .1) {
      intakeFlip.set(Value.kForward);
    } else if (inDown.get() == false && joy.getRawAxis(2) > .1) {
      intakeFlip.set(Value.kReverse);
    } else {
      intakeFlip.set(Value.kOff);
    }

    // Hopefully creates a toggle for intake motors
    if (joy.getRawButtonPressed(1)) {
      intakeRun = !intakeRun;
    }

    // Runs the intake motors
    if (intakeRun == true) {
      intake.set(-1);
    } else if (joy.getRawButton(2)) {
      intake.set(1);
    } else {
      intake.set(0);
    }

    // Moves lift up and down
    if (liftUp.get() == false && joy.getRawButton(6)) {
      lift.set(1);
    } else if (liftDown.get() == false && joy.getRawButton(5)) {
      lift.set(-1);
    } else {
      lift.set(0);
    }
    if (joy.getRawButtonPressed(3)) {
      shootRun = !shootRun;
    }
    if (shootRun == true) {
      solo.set(-.85);
    } else {
      solo.set(0);
    }
    // Runs the intake motors
    // if (slorpMode== true) {
    if (joy.getRawButton(7)) {
      belt.set(Value.kForward);
    } else if (joy.getRawButton(8)) {
      belt.set(Value.kReverse);
    } else if (joy.getRawButton(4) == true && tv == 1) {// Moves us into auto-shooting if button is pressed
      autoShoot();
    } else {
      aligned = false;
      belt.set(Value.kOff);
      beltDelay = 0;
    } /*
       * else { belt.set(Value.kOff); }
       */
    /*
     * } else { if (b3.get() == true && joy.getRawButton(4) == false) {
     * belt.set(Value.kOff); } else if (b2.get() == true && b1.get() == false &&
     * joy.getRawButton(4) == false) { belt.set(Value.kOff); } else if
     * (joy.getRawButton(4) == false) { belt.set(Value.kReverse); } }
     */

    camx = tx.getDouble(0.0);
    camy = ty.getDouble(0.0);
    camarea = ta.getDouble(0.0);
    vertAngle = tvert.getDouble(0);
    targetWidth = thor.getDouble(0);
    degreeWidth = targetWidth * 0.16875;// degrees per pixel
    targetRatio = 10 / degreeWidth; // ratio of width of desired target to width of target (13 ft away and
    // perpendicular)
    // if the robot is closer, then it should move more (and if farther away, it
    // should move less)
    // up close, degreeWidth will be greater than 1.0602..., and so the robot will
    // try to be more accurate
    // farther away, it will have more leeway
    // System.out.println("The target is " + targetWidth + " pixels wide, " +
    // degreeWidth + " degrees.");

    SmartDashboard.putNumber("Vision", tv);
    SmartDashboard.putNumber("LimelightX", camx);
    SmartDashboard.putNumber("LimelightY", camy);
    SmartDashboard.putNumber("LimelightArea", camarea);
    NetworkTableInstance.getDefault();
    SmartDashboard.putBoolean("Aligned", aligned);
    SmartDashboard.putNumber("Angle width", degreeWidth);
  } // teleopperiodic

  @Override
  public void testPeriodic() {
  }

  public void autoShoot() {
    soloPew = ((vertAngle / 975) * vertAngle);
    // Auto-Aligns to the reflective tape
    aligned = false;
    PIDa();

    if (camx > 1 || camx < -1) {
      buffet.arcadeDrive(0, -piAlign);
      aligned = false;
    } else if (camx > -1 && camx < 1) {
      aligned = true;
      // We be aligned
    } else {
      System.out.println("I am the print line... that doesn't do anything. Camx is " + camx);
    }

    /*if (aligned == true && soloPew >= .75) {
      solo.set(soloPew);
    } else if (aligned == true && soloPew < .75) {
      solo.set(.75);
    } else {
      solo.set(0);
    }

    if (aligned == true && beltDelay >= 60) {
      belt.set(Value.kReverse);
    } else if (aligned == true && beltDelay < 60) {
      belt.set(Value.kOff);
      ++beltDelay;
    } else {
      belt.set(Value.kOff);
    } */
  }

  public double distance() {
    /* d = (h2-h1) / tan(a1+a2)
   h2 = height of camera
   h1 = height of target from ground
   a1 = degree of camera from horizontal to ground
   a2 = degree of camera to target ////// use tvert variable */

    return ((22-98.25) / Math.tan(30 + vertAngle));
  }

  public void PIDa() {
    P =.05;
    error = setpoint - camx;
    I = (error*.02);
    piAlign = P*error + I;
  }
  public void PIDs() { 
    pDistance = .001;
    errorDistance = setDistance - camy;
    iDistance = (errorDistance*.02);
    piDistance = pDistance*errorDistance + iDistance;
  }
}
