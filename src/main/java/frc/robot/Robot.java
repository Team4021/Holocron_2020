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

  double P = 1; // alignment
  double error, setpoint = 0, piAlign; // alignement
  double pShooter = 1, iShooter = 0, dShooter = 1; // distance shooter
  double errorShooter, setShooter = -8, piShooter; // distance shooter


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
SmartDashboard.putNumber("PIShooter", piShooter);
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
	/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @Override
  public void teleopPeriodic() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
    pizza = joy.getRawAxis(1);
    taco = joy.getRawAxis(4);
    buffet.arcadeDrive(-pizza, taco);
		
    lift();

    intakeFlip();
		
    intakeRun();

    manShooter();
    
    belt();
	  
    camx = tx.getDouble(0.0);
    camy = ty.getDouble(0.0);
    camarea = ta.getDouble(0.0);
    vertAngle = tvert.getDouble(0);
    targetWidth = thor.getDouble(0);

    SmartDashboard.putNumber("Vision", tv);
    SmartDashboard.putNumber("LimelightX", camx);
    SmartDashboard.putNumber("LimelightY", camy);
    SmartDashboard.putNumber("LimelightArea", camarea);
    NetworkTableInstance.getDefault();
    SmartDashboard.putBoolean("Aligned", aligned);
    SmartDashboard.putNumber("Angle width", degreeWidth);
  } // teleopperiodic
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  @Override
  public void testPeriodic() {
  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void autoShoot() {
    soloPew = ((vertAngle / 975) * vertAngle);
    // Auto-Aligns to the reflective tape
    aligned = false;
    PIDa();
    PIDs();

    if (camx > .75) {
      right.set(0);
      left.set(Math.abs(piAlign));
      aligned = false;
    } else if (camx < -.75) {
      right.set(-piAlign);
      left.set(0);
    } else if (camx > -.75 && camx < .75) {
      aligned = true;
      // We be aligned
    } else {
      System.out.println("This ain't it chief");
    }

    if (aligned == true) { // Still not sure about this whole system, only 12 degress of play in motor
      solo.set(-PIDs());
    } else {
      solo.set(0);
    } // MIN DISTANCE IS 6.8//////

    if (aligned == true && beltDelay >= 75) {
      belt.set(Value.kReverse);
    } else if (aligned == true && beltDelay < 75) {
      belt.set(Value.kOff);
      ++beltDelay;
    } else {
      belt.set(Value.kOff);
    } 
  }
  /*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void lift() {
							  // Moves lift up and down
   if (liftUp.get() == false && joy.getRawButton(6)) {
     lift.set(1);
   } else if (liftDown.get() == false && joy.getRawButton(5)) {
     lift.set(-1);
   } else {
     lift.set(0);
   }
 }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
	public void intakeFlip() {
	// Moves intake up and down
   if (inUp.get() == false && joy.getRawAxis(3) > .1) {
     intakeFlip.set(Value.kForward);
   } else if (inDown.get() == false && joy.getRawAxis(2) > .1) {
     intakeFlip.set(Value.kReverse);
   } else {
     intakeFlip.set(Value.kOff);
  }
}
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
	public void intakeRun() {
	 // Creates a toggle for intake motors
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
}
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
	public void manShooter() {
	if (joy.getRawButtonPressed(3)) {
      shootRun = !shootRun;
    }
    if (shootRun == true) {
      solo.set(-.85);
    } else {
      solo.set(0);
    }
	}
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public void belt() {
    final double tv = NetworkTableInstance.getDefault().getTable("limelight").getEntry("tv").getDouble(0);
  // Runs the intake motors
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

  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public double distance() {
    /* d = (h2-h1) / tan(a1+a2)
   h2 = height of camera
   h1 = height of target from ground
   a1 = degree of camera from horizontal to ground
   a2 = degree of camera to target ////// use tvert variable */

    return ((98.25-22) / Math.tan(30 + vertAngle));
  }
/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public double PIDa() {
    P =.03;
    error = setpoint - camx;
    if (Math.abs(P*error) < .2) {
      piAlign = .2;
    } else {
      piAlign = P*error;
    }
    return piAlign;
  }
	/*-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=*/
  public double PIDs() { 
    pShooter = .037; // We're coming
    errorShooter = setShooter - Math.abs(camy);
    if (pShooter*errorShooter > -.75) {
      piShooter = .75;
    } else {
      piShooter = pShooter*errorShooter;
    }
    return Math.abs(piShooter);
  }
}
