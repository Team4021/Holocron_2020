/*----------------------------------------------------------------------------*/
/* Copyright (c) 2017-2018 FIRST. All Rights Reserved.                        */
/* Open Source Software - may be modified and shared by FRC teams. The code   */
/* must be accompanied by the FIRST BSD license file in the root directory of */
/* the project.                                                               */
/*----------------------------------------------------------------------------*/

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.drive.DifferentialDrive;
import edu.wpi.first.wpilibj.smartdashboard.SendableChooser;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.networktables.NetworkTable;
import edu.wpi.first.networktables.NetworkTableEntry;
import edu.wpi.first.networktables.NetworkTableInstance;
import edu.wpi.first.wpilibj.PWMVictorSPX;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.VictorSP;
import edu.wpi.first.wpilibj.SpeedControllerGroup;
import edu.wpi.first.wpilibj.Timer;

/**
 * The VM is configured to automatically run this class, and to call the
 * functions corresponding to each mode, as described in the TimedRobot
 * documentation. If you change the name of this class or the package after
 * creating this project, you must also update the build.gradle file in the
 * project.
 */
public class Robot extends TimedRobot {
  private static final String kDefaultAuto = "Default";
  private static final String kCustomAuto = "My Auto";
  private String m_autoSelected;
  private final SendableChooser<String> m_chooser = new SendableChooser<>();
  NetworkTable table = NetworkTableInstance.getDefault().getTable("limelight");
  NetworkTableEntry tx = table.getEntry("tx"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ty = table.getEntry("ty"); // angle on x-axis from the crosshairs on the object to origin
  NetworkTableEntry ta = table.getEntry("ta"); // area of the object
  NetworkTableEntry tlong = table.getEntry("tlong"); // length of longest side
  NetworkTableEntry tshort = table.getEntry("tshort"); // length of shortest side
  NetworkTableEntry tvert = table.getEntry("tvert"); // vertical distance
  NetworkTableEntry thor = table.getEntry("thor"); // horizontal distance
  NetworkTableEntry getpipe = table.getEntry("getpipe"); // this tells us what "pipeline" we are on, basically different settings for the camera
  NetworkTableEntry ts = table.getEntry("ts"); // skew or rotation of target

  Joystick joy = new Joystick(0);

  VictorSP frontLeft = new VictorSP(8);
  VictorSP frontRight = new VictorSP(2);
  VictorSP rearLeft = new VictorSP(4);
  VictorSP rearRight = new VictorSP(3);
  //VictorSP solo = new VictorSP(5);
  

  SpeedControllerGroup left = new SpeedControllerGroup(frontLeft, rearLeft);
  SpeedControllerGroup right = new SpeedControllerGroup(frontRight, rearRight);

  DifferentialDrive buffet = new DifferentialDrive(left, right);

  double pizza;
  double taco;

  double camx;
  double camy;
  double camarea;

  boolean aligned;
  boolean distanced;
  /**
   * This function is run when the robot is first started up and should be used
   * for any initialization code.
   */
  @Override
  public void robotInit() {
    m_chooser.setDefaultOption("Default Auto", kDefaultAuto);
    m_chooser.addOption("My Auto", kCustomAuto);
    SmartDashboard.putData("Auto choices", m_chooser);

    SmartDashboard.putString("General Kenobi", "Hello there");
  }

  /**
   * This function is called every robot packet, no matter the mode. Use this for
   * items like diagnostics that you want ran during disabled, autonomous,
   * teleoperated and test.
   *
   * <p>
   * This runs after the mode specific periodic functions, but before LiveWindow
   * and SmartDashboard integrated updating.
   */
  @Override
  public void robotPeriodic() {
  }

  /**
   * This autonomous (along with the chooser code above) shows how to select
   * between different autonomous modes using the dashboard. The sendable chooser
   * code works with the Java SmartDashboard. If you prefer the LabVIEW Dashboard,
   * remove all of the chooser code and uncomment the getString line to get the
   * auto name from the text box below the Gyro
   *
   * <p>
   * You can add additional auto modes by adding additional comparisons to the
   * switch structure below with additional strings. If using the SendableChooser
   * make sure to add them to the chooser code above as well.
   */
  @Override
  public void autonomousInit() {
    m_autoSelected = m_chooser.getSelected();
    // m_autoSelected = SmartDashboard.getString("Auto Selector", kDefaultAuto);
    System.out.println("Auto selected: " + m_autoSelected);
  }

  /**
   * This function is called periodically during autonomous.
   */
  @Override
  public void autonomousPeriodic() {
    switch (m_autoSelected) {
    case kCustomAuto:
      // Put custom auto code here
      break;
    case kDefaultAuto:
    default:
      // Put default auto code here
      break;
    }
  }

  /**
   * This function is called periodically during operator control.
   */
  @Override
  public void teleopPeriodic() {
    pizza = joy.getRawAxis(1);
    taco = joy.getRawAxis(4);
    buffet.arcadeDrive(-pizza, taco);

    camx = tx.getDouble(0.0);
    camy = ty.getDouble(0.0);
    camarea = ta.getDouble(0.0);
    SmartDashboard.putNumber("LimelightX", camx);
    SmartDashboard.putNumber("LimelightY", camy);
    SmartDashboard.putNumber("LimelightArea", camarea);
    NetworkTableInstance.getDefault();

    SmartDashboard.putBoolean("Aligned", aligned);
    SmartDashboard.putBoolean("DistancED", distanced);
    
    SmartDashboard.putBoolean("Motor Safety", frontLeft.isSafetyEnabled());

    if (joy.getRawButton(6) == true) {                  // Moves us into auto-shooting if button is pressed
      SmartDashboard.putBoolean("Button 6 pressed", joy.getRawButton(6));
      autoShoot();

    } else {
      SmartDashboard.putBoolean("Button 6 pressed", joy.getRawButton(6));
      aligned = false;
      distanced = false;
    }
    
  }

  /**
   * This function is called periodically during test mode.
   */
  @Override
  public void testPeriodic() {
  }

  public void autoShoot() {
    //camx=tx
    // Auto-Aligns to the reflective tape
    
    if ( camx > 5) {
      left.set(.25);
      right.set(.25);
      System.out.println("Should be turning right ("+camx+")");
      // On left, twist right
    } else if ( camx < -5) {
      left.set(-.25);
      right.set(-.25);
      System.out.println("Should be turning left ("+camx+")");
      // On right, twist left
    } else if ( camx > -5 && camx < 5) {
      aligned = true;
      System.out.println("Should be staying put because the x value is "+camx);
      // We be aligned
    } else {
      System.out.println("I am the print line... that doesn't do anything. Camx is "+camx);
    }

    // Moves to correct distance from reflective tape

    if (joy.getRawButton(6) && aligned == true && camy > 2.3) {
      left.set(-.25);
      right.set(.25);
    } else if (joy.getRawButton(6) && aligned == true && camy < -2.3) {
      left.set(.25);
      right.set(-.25);
    } else if (joy.getRawButton(6) && aligned == true && camy < 2.3 && camy > -2.3) {
      distanced = true;
    }

    // Shooting der ball m8

    if (joy.getRawButton(6) && distanced == true) {
      System.out.println("we got through the shooting boiz");
      //solo.set(1);
      Timer.delay(5);
      //solo.set(0);
      aligned = false;
      distanced = false;
    }
  
  }
}
