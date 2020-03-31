import java.util.LinkedList;
import java.util.Scanner;

import AgentAsserv.OpponentDetectionMode;
import AgentAsserv.PID;
import AgentAsserv.Robot;
import AgentAsserv.Trajectory;
import AgentAsserv.Trajectory.TrajectoryOrder;
import AgentAsserv.Trajectory.TrajectoryOrderGotoA;
import AgentAsserv.Trajectory.TrajectoryOrderGotoD;
import AgentAsserv.Trajectory.TrajectoryStatus;

class Boss {
	public static int ACUTAL_TIME_ms = 0;

	public static class PID {

		private double _kp;
		private double _sp;
		private double _out;
		private double _ki;
		private int _max_i;
		private int _integral;

		public PID(double kp, double ki, double kd, int max_i) {
			_kp = kp;
			_ki = ki;
			_max_i = max_i;
			_integral = 0;
		}

		public void setSetpoint(double sp) {
			_sp = sp;
		}

		public void compute(double feedback) {
			double delta = _sp - feedback;

			_integral += delta;
			if (_integral > _max_i) {
				_integral = _max_i;
			} else if (_integral < -_max_i) {
				_integral = _max_i;
			}

			_out = delta * _kp + _integral * _ki;
		}

		public double getOutput() {
			return _out;
		}
	}

	public static enum OpponentDetectionMode {
		FRONT, BACK
	}

	public static class Trajectory {
		public static enum TrajectoryStatus {
			SUCCESS, RUNNING, ORDER_DONE, TIMEOUT_BEFORE_END, ORDER_IN_PROGRESS

		}

		public static class TrajectoryOrder {
			private Trajectory _trajectory;
			private double _start_distance;
			private double _start_x;
			private double _start_y;
			private boolean _initialized;
			private double _start_angle;
			private int _start_time;

			public TrajectoryOrder(Trajectory trajectory) {
				_trajectory = trajectory;
				_initialized = false;
			}

			public void run() {
				_trajectory.addOrder(this);
			}

			public void initialize(double distance, double angle, double x, double y) {
				_start_distance = distance;
				_start_angle = angle;
				_start_x = x;
				_start_y = y;
				_start_time = ACUTAL_TIME_ms;
				_initialized = true;
			}

			public boolean isInitialized() {
				return _initialized;
			}

			public TrajectoryStatus compute() {
				return TrajectoryStatus.ORDER_DONE;
			}

			public int maxTime() {
				return -1;
			}

			public int getStartTime() {
				return _start_time;
			}

			public double getStartDistance() {
				return _start_distance;
			}

			public double getStartAngle() {
				return _start_angle;
			}

			public Trajectory getTrajectory() {
				return _trajectory;
			}

			protected double getEstimatedDistanceBeforeStop() {
				// TODO Auto-generated method stub
				return 0;
			}
		}

		public static final double TRAJECTORY_D_STOP_MM = 3.0;

		public static class TrajectoryOrderGotoD extends TrajectoryOrder {

			private double _distance;

			public TrajectoryOrderGotoD(Trajectory trajectory) {
				super(trajectory);
			}

			public TrajectoryOrder setDistance(double distance) {
				_distance = distance;
				return this;
			}

			@Override
			public TrajectoryStatus compute() {
				double diff = getStartDistance() + _distance - getTrajectory().getRobot().getDistance();
				double estimation = getEstimatedDistanceBeforeStop();

				if (_distance < 0) {
					if (getTrajectory().getRobot().isOpponentDetectionEnabled()) {
						getTrajectory().getRobot().enableOpponentDetection(OpponentDetectionMode.BACK);
					}
					if (diff > -TRAJECTORY_D_STOP_MM) {
						return TrajectoryStatus.ORDER_DONE;
					}

					estimation = -estimation;
				} else {
					if (getTrajectory().getRobot().isOpponentDetectionEnabled()) {
						getTrajectory().getRobot().enableOpponentDetection(OpponentDetectionMode.FRONT);
					}
					if (diff < TRAJECTORY_D_STOP_MM) {
						return TrajectoryStatus.ORDER_DONE;
					}
				}

				getTrajectory().getRobot().setAsservDistanceSetpoint(getStartDistance() + _distance);

				return TrajectoryStatus.ORDER_IN_PROGRESS;
			}

		}

		public static class TrajectoryOrderGotoA extends TrajectoryOrder {

			private double _angle;

			public TrajectoryOrderGotoA(Trajectory trajectory) {
				super(trajectory);
			}

			public TrajectoryOrder setAngle(double angle) {
				_angle = angle;
				return this;
			}

			@Override
			public TrajectoryStatus compute() {
				double target = find_best_angle(getStartAngle(), _angle);

				double l = Math.abs(target - getTrajectory().getRobot().getAngle());
				if (l < 2.5f) {
					return TrajectoryStatus.ORDER_DONE;
				}

				getTrajectory().getRobot().setAsservAngularSetpoint(target);
				getTrajectory().getRobot().setAsservDistanceSetpoint(getStartDistance());
				return TrajectoryStatus.ORDER_IN_PROGRESS;
			}

			public static double find_best_angle(double current_angle, double angle) {
				// get modulo multiplier
				double abase = Math.floor(current_angle / 360.0) * 360.0;

				// set set point in valid range (-180 > 180)
				while (angle > 180.0) {
					angle -= 360.0;
				}
				while (angle < -180.0) {
					angle += 360.0;
				}

				// find best target
				double t1 = abase + angle;
				double t2 = abase + angle + 360.0;
				double t3 = abase + angle - 360.0;
				double t4 = abase + angle - 2 * 360.0;

				double dt1 = Math.abs(t1 - current_angle);
				double dt2 = Math.abs(t2 - current_angle);
				double dt3 = Math.abs(t3 - current_angle);
				double dt4 = Math.abs(t4 - current_angle);

				double target = t1;
				double dtarget = dt1;
				if (dt2 < dtarget) {
					target = t2;
					dtarget = dt2;
				}
				if (dt3 < dtarget) {
					target = t3;
					dtarget = dt3;
				}
				if (dt4 < dtarget) {
					target = t4;
					dtarget = dt4;
				}

				return target;
			}
		}

		private LinkedList<TrajectoryOrder> _orders = new LinkedList<TrajectoryOrder>();
		private TrajectoryStatus _result;
		private Robot _robot;
		private boolean _estimations_need_recompute;

		public Trajectory(Robot robot) {
			_result = TrajectoryStatus.SUCCESS;
			_robot = robot;
			_estimations_need_recompute = false;
		}

		public void addOrder(TrajectoryOrder order) {
			_orders.addLast(order);
			_estimations_need_recompute = true;
		}

		public void compute() {
			if (_estimations_need_recompute) {
				compute_estimations();
				_estimations_need_recompute = false;
			}

			TrajectoryOrder order = _orders.peekFirst();

			if (order != null) {

				// check if order is new
				if (!order.isInitialized()) {
					// set real start values
					order.initialize(_robot.getDistance(), _robot.getAngle(), _robot.getX(), _robot.getY());
				}
				TrajectoryStatus status = order.compute();

				if (order.maxTime() >= 0) {
					if (ACUTAL_TIME_ms - order.getStartTime() > order.maxTime()) {
						status = TrajectoryStatus.ORDER_DONE;
						_result = TrajectoryStatus.TIMEOUT_BEFORE_END;

						_robot.setAsservDistanceSetpoint(_robot.getDistance());
						_robot.setAsservAngularSetpoint(_robot.getAngle());
					}
				}

				// remove order of the list if needed
				if (status == TrajectoryStatus.ORDER_DONE) {
					_orders.pollFirst();
				}
			} else if (_result == TrajectoryStatus.RUNNING) {
				_result = TrajectoryStatus.SUCCESS;
			}

		}

		private void compute_estimations() {
			// TODO Auto-generated method stub

		}

		public Robot getRobot() {
			return _robot;
		}

		public TrajectoryOrder gotoD(double distance) {
			return new TrajectoryOrderGotoD(this).setDistance(distance);
		}

		public TrajectoryOrder gotoA(double angle) {
			return new TrajectoryOrderGotoA(this).setAngle(angle);
		}
	}

	public static class Robot {
		private static final double TICK_TO_MM = 0.05;
		private static final double TICK_TO_DEG = (360.0 / (2 * Math.PI * 250.0 / (2 * TICK_TO_MM)));

		private double _x;
		private double _angle;
		private double _y;
		private int _leftEncoder;
		private int _rightEncoder;
		private double _distance;
		private int _left_motor;
		private int _right_motor;
		private PID _pid_dist = new PID(0.1, 0.1, 0, 50);
		private PID _pid_angu = new PID(3, 0, 0, 0);
		private Trajectory _trajectory;

		public Robot() {
			_trajectory = new Trajectory(this);
		}

		public void enableOpponentDetection(OpponentDetectionMode back) {
			// TODO Auto-generated method stub

		}

		public boolean isOpponentDetectionEnabled() {
			return false;
		}

		public double getDistance() {
			return _distance;
		}

		public double getAngle() {
			return _angle;
		}

		public double getY() {
			return _y;
		}

		public void setPosition(int x, int y, int angle) {
			_x = x;
			_y = y;
			_angle = angle;
			setAsservAngularSetpoint(_angle);
		}

		public String debugPosition() {
			return Math.round(_distance) + " " + Math.round(_x) + " " + Math.round(_y) + " " + _angle;
		}

		public void setEncoders(int leftEncoder, int rightEncoder) {
			int delta_left = leftEncoder - _leftEncoder;
			int delta_right = rightEncoder - _rightEncoder;
			_leftEncoder = leftEncoder;
			_rightEncoder = rightEncoder;
			int _deltaDistance = delta_left + delta_right;

			_distance += _deltaDistance * TICK_TO_MM;
			_angle += (-delta_left + delta_right) * TICK_TO_DEG;

			_x += _deltaDistance * TICK_TO_MM * Math.cos(Math.toRadians(_angle));
			_y += _deltaDistance * TICK_TO_MM * Math.sin(Math.toRadians(_angle));
		}

		public String getOutputs() {
			compute();

			return _left_motor + " " + _right_motor + " IDLE";
		}

		private void compute() {
			_trajectory.compute();
			_pid_dist.compute(_distance);
			_pid_angu.compute(_angle);

			double left_motor = _pid_dist.getOutput() - _pid_angu.getOutput();
			double right_motor = _pid_dist.getOutput() + _pid_angu.getOutput();

			double scale = 1.0;
			if (Math.abs(left_motor) / 100.0 > scale) {
				scale = Math.abs(left_motor) / 100.0;
			}
			if (Math.abs(right_motor) / 100.0 > scale) {
				scale = Math.abs(right_motor) / 100.0;
			}

			_left_motor = (int) (left_motor / scale);
			_right_motor = (int) (right_motor / scale);
		}

		public void setAsservDistanceSetpoint(double dist) {
			_pid_dist.setSetpoint(dist);
		}

		public void setAsservAngularSetpoint(double angle) {
			_pid_angu.setSetpoint(angle);
		}

		public double getX() {
			return _x;
		}

		public Trajectory getTrajectory() {
			return _trajectory;
		}

	}

	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int turn = 0;
			int score = 0;
			Robot[] robots = { null, null };

			boolean goto_compass = false;
			String compass = null;
			String playerColor = in.next();
			for (int i = 0; i < 2; i++) {
				int x = in.nextInt();
				int y = in.nextInt();
				int angle = in.nextInt();

				robots[i] = new Robot();
				robots[i].setPosition(x, y, angle);
			}

			// game loop
			while (true) {
				for (int i = 0; i < 2; i++) {
					int leftEncoder = in.nextInt();
					int rightEncoder = in.nextInt();
					String lastTakenColor = in.next();
					String detectedCompass = in.next();
					robots[i].setEncoders(leftEncoder, rightEncoder);
					if (!detectedCompass.equals("?") && ACUTAL_TIME_ms > 26000) {
						compass = detectedCompass;
					}
					System.err.println(detectedCompass);
				}

				for (int i = 0; i < 2; i++) {
					int frontLowSensor = in.nextInt();
					int rightLowSensor = in.nextInt();
					int backLowSensor = in.nextInt();
					int leftLowSensor = in.nextInt();
					int frontLeftHighSensor = in.nextInt();
					int frontRightHighSensor = in.nextInt();
					int backRightHighSensor = in.nextInt();
					int backLeftHighSensor = in.nextInt();
				}
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 360; j++) {
						int lidarData = in.nextInt();
					}
				}

				if (turn == 0) {
					if (playerColor.equals("BLUE")) {
						robots[0].getTrajectory().gotoD(1500 - robots[0].getX()).run();
					} else {
						robots[0].getTrajectory().gotoD(robots[0].getX() - 1500).run();
					}
					robots[0].getTrajectory().gotoA(90).run();
				} else if (turn == 5) {
					robots[1].getTrajectory().gotoD(100).run();
					robots[1].getTrajectory().gotoA(90).run();
				} else if (compass != null && !goto_compass) {
					goto_compass = true;
					if (compass.equals("N")) {
						if (playerColor.equals("BLUE")) {
							robots[0].getTrajectory().gotoA(180).run();
							robots[0].getTrajectory().gotoD(1300).run();
							robots[0].getTrajectory().gotoA(90).run();
							robots[0].getTrajectory().gotoD(500).run();
							robots[1].getTrajectory().gotoD(500).run();
						} else {
							robots[0].getTrajectory().gotoA(0).run();
							robots[0].getTrajectory().gotoD(1300).run();
							robots[0].getTrajectory().gotoA(90).run();
							robots[0].getTrajectory().gotoD(500).run();
							robots[1].getTrajectory().gotoD(500).run();
						}
					} else {
						if (playerColor.equals("BLUE")) {
							robots[0].getTrajectory().gotoA(180).run();
							robots[0].getTrajectory().gotoD(1300).run();
							robots[0].getTrajectory().gotoA(-90).run();
							robots[0].getTrajectory().gotoD(500).run();
							robots[1].getTrajectory().gotoD(-500).run();
						} else {
							robots[0].getTrajectory().gotoA(0).run();
							robots[0].getTrajectory().gotoD(1300).run();
							robots[0].getTrajectory().gotoA(-90).run();
							robots[0].getTrajectory().gotoD(500).run();
							robots[1].getTrajectory().gotoD(-500).run();
						}
					}

					score += 10;
				} else {

				}

				for (int i = 0; i < 2; i++) {
					System.out.println(robots[i].getOutputs());
				}
				System.out.println(score);
				turn += 1;
				ACUTAL_TIME_ms += 350;
			}
		}
	}
}
