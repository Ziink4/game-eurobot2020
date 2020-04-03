import java.util.LinkedList;
import java.util.Scanner;

class AgentAsserv {
	public static int ACUTAL_TIME_ms = 0;
	private static boolean _sockDone;
	private static boolean _lightHouse;
	private static boolean _addLastRed;
	private static boolean _park;

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

	public static enum MecaState {
		IDLE, ACTIVATE_FRONT, TAKE, LIGHT, FLAG, WIND, ACTIVATE_RIGHT, ACTIVATE_LEFT, RELEASE;

		MecaState inverse(int playerI) {
			switch (this) {
			case ACTIVATE_RIGHT:
				return playerI == 1 ? this : ACTIVATE_LEFT;
			case ACTIVATE_LEFT:
				return playerI == 1 ? this : ACTIVATE_RIGHT;
			default:
				return this;
			}
		}
	}

	public static class Trajectory {
		public static enum TrajectoryStatus {
			SUCCESS, RUNNING, ORDER_DONE, TIMEOUT_BEFORE_END, ORDER_IN_PROGRESS, ORDER_NEXT

		}

		public static class TrajectoryOrder {
			private Trajectory _trajectory;
			private double _start_distance;
			private double _start_x;
			private double _start_y;
			private boolean _initialized;
			private double _start_angle;
			private int _start_time;
			private int _maxtime = -1;

			public TrajectoryOrder(Trajectory trajectory) {
				_trajectory = trajectory;
				_initialized = false;
			}

			public void run() {
				_trajectory.addOrder(this);
			}

			public void initialize(int time, double distance, double angle, double x, double y) {
				_start_distance = distance;
				_start_angle = angle;
				_start_x = x;
				_start_y = y;
				_start_time = time;
				_initialized = true;
			}

			public boolean isInitialized() {
				return _initialized;
			}

			public TrajectoryStatus compute() {
				return TrajectoryStatus.ORDER_DONE;
			}

			public int maxTime() {
				return _maxtime;
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

			public TrajectoryOrder setMaxTime(int max) {
				_maxtime = max;
				return this;
			}

			public double getStartX() {
				return _start_x;
			}

			public double getStartY() {
				return _start_y;
			}
		}

		public static final double TRAJECTORY_D_STOP_MM = 3.0;
		public static final double TRAJECTORY_XY_STOP_ANGLE_DEG = 5.0;
		public static final double TRAJECTORY_XY_STOP_MM = 3.0;

		public static class TrajectoryOrderGotoXY extends TrajectoryOrder {

			private double _x;
			private double _y;
			private double _d_stop = Double.NaN;

			public TrajectoryOrderGotoXY(Trajectory trajectory) {
				super(trajectory);
			}

			public TrajectoryOrderGotoXY setX(double x) {
				_x = x;
				return this;
			}

			public TrajectoryOrderGotoXY setY(double y) {
				_y = y;
				return this;
			}

			@Override
			public TrajectoryStatus compute() {
				if(Double.isNaN(_x)) {
					_x = getStartX();
				}
				if(Double.isNaN(_y)) {
					_y = getStartY();
				}
				double x = getTrajectory().getRobot().getX();
				double y = getTrajectory().getRobot().getY();

				double tx = _x - getStartX();
				double ty = _y - getStartY();
				double td = tx * tx + ty * ty;

				double cx = x - getStartX();
				double cy = y - getStartY();
				double cd = cx * cx + cy * cy;

				if (getTrajectory().getRobot().isOpponentDetectionEnabled()) {
					getTrajectory().getRobot().enableOpponentDetection(OpponentDetectionMode.FRONT);
				}

				if (Math.sqrt(cd) > Math.sqrt(td) - TRAJECTORY_XY_STOP_MM) {
					return TrajectoryStatus.ORDER_DONE;
				} else {
					double dx = _x - x;
					double dy = _y - y;
					double dd = dx * dx + dy * dy;

					double cur_angle = getTrajectory().getRobot().getAngle();
					double abs_target = Math.atan2(dy, dx) * 180.0f / Math.PI;

					double target = TrajectoryOrderGotoA.find_best_angle(cur_angle, abs_target);
					if (Math.abs(cur_angle - target) < TRAJECTORY_XY_STOP_ANGLE_DEG) {
						_d_stop = Double.NaN;
						getTrajectory().getRobot().setAsservDistanceSetpoint(getTrajectory().getRobot().getDistance()
								+ Math.sqrt(dd) + getEstimatedDistanceBeforeStop());
					} else {
						if (Double.isNaN(_d_stop)) {
							_d_stop = getTrajectory().getRobot().getDistance();
						}
						getTrajectory().getRobot().setAsservDistanceSetpoint(_d_stop);
					}

					getTrajectory().getRobot().setAsservAngularSetpoint(target);
				}

				return TrajectoryStatus.ORDER_IN_PROGRESS;
			}
		}

		public static class TrajectoryOrderScore extends TrajectoryOrder {

			private int _value;

			public TrajectoryOrderScore(Trajectory trajectory) {
				super(trajectory);
			}

			public TrajectoryOrderScore setValue(int v) {
				_value = v;
				return this;
			}

			@Override
			public TrajectoryStatus compute() {
				getTrajectory().getRobot().addScore(_value);
				return TrajectoryStatus.ORDER_NEXT;
			}
		}

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

		public static class TrajectoryOrderMeca extends TrajectoryOrder {

			private MecaState _state;

			public TrajectoryOrderMeca(Trajectory trajectory) {
				super(trajectory);
			}

			public TrajectoryOrder setState(MecaState state) {
				_state = state;
				return this;
			}

			@Override
			public TrajectoryStatus compute() {
				if (!getTrajectory().getRobot().mecaSet()) {
					getTrajectory().getRobot().setMeca(_state);
					return TrajectoryStatus.ORDER_NEXT;
				} else {
					return TrajectoryStatus.ORDER_IN_PROGRESS;
				}
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

		public void compute(int current_time_ms) {
			if (_estimations_need_recompute) {
				compute_estimations();
				_estimations_need_recompute = false;
			}

			while (true) {
				TrajectoryOrder order = _orders.peekFirst();
				if (order == null) {
					if (_result == TrajectoryStatus.RUNNING) {
						_result = TrajectoryStatus.SUCCESS;
					}
					break;
				}

				// check if order is new
				if (!order.isInitialized()) {
					// set real start values
					order.initialize(current_time_ms, _robot.getDistance(), _robot.getAngle(), _robot.getX(),
							_robot.getY());
				}
				TrajectoryStatus status = order.compute();

				if (order.maxTime() >= 0) {
					if (current_time_ms - order.getStartTime() > order.maxTime()) {
						status = TrajectoryStatus.ORDER_DONE;
						_result = TrajectoryStatus.TIMEOUT_BEFORE_END;

						_robot.setAsservDistanceSetpoint(_robot.getDistance());
						_robot.setAsservAngularSetpoint(_robot.getAngle());
					}
				}

				// remove order of the list if needed
				if (status == TrajectoryStatus.ORDER_DONE) {
					_orders.pollFirst();
					break;
				} else if (status == TrajectoryStatus.ORDER_NEXT) {
					_orders.pollFirst();
				} else {
					break;
				}
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

		public void removeAllOrders() {
			_orders.clear();
			_estimations_need_recompute = true;
		}

		public TrajectoryOrder meca(MecaState state) {
			return new TrajectoryOrderMeca(this).setState(state);
		}

		public boolean isDone() {
			return _orders.size() == 0;
		}

		public TrajectoryOrderGotoXY gotoXY(double x, double y) {
			return new TrajectoryOrderGotoXY(this).setX(x).setY(y);
		}

		public TrajectoryOrderScore addScore(int value) {
			return new TrajectoryOrderScore(this).setValue(value);
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
		private PID _pid_dist = new PID(1, 1, 0.0, 50);
		private PID _pid_angu = new PID(2, 0, 0, 0);
		private Trajectory _trajectory;
		private MecaState _meca = MecaState.IDLE;
		private boolean _mecaSet = false;
		private int[] _score;
		private boolean _flag = false;
		public boolean _park;

		public Robot(int[] score) {
			_trajectory = new Trajectory(this);
			_score = score;
		}

		public void addScore(int value) {
			_score[0] += value;
		}

		public boolean mecaSet() {
			return _mecaSet;
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

		public String getOutputs(int current_time_ms) {
			compute(current_time_ms);

			return _left_motor + " " + _right_motor + " " + _meca.toString();
		}

		private void compute(int current_time_ms) {
			_mecaSet = false;
			_trajectory.compute(current_time_ms);
			_pid_dist.compute(_distance);
			_pid_angu.compute(_angle);

			if (!_mecaSet && !_flag && current_time_ms > 95000) {
				_flag = true;
				setMeca(MecaState.FLAG);
				addScore(5);
			}

			double left_motor = _pid_dist.getOutput() - _pid_angu.getOutput();
			double right_motor = _pid_dist.getOutput() + _pid_angu.getOutput();

			double scale = 1.0;
			if (Math.abs(left_motor) / 1000.0 > scale) {
				scale = Math.abs(left_motor) / 1000.0;
			}
			if (Math.abs(right_motor) / 1000.0 > scale) {
				scale = Math.abs(right_motor) / 1000.0;
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

		public void setMeca(MecaState meca) {
			_meca = meca;
			_mecaSet = true;
		}

	}

	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int turn = 0;
			int[] score = { 2 };

			Robot[] robots = { null, null };

			boolean goto_compass = false;
			String compass = null;
			String playerColor = in.next();
			int playerI = 1;
			if (!playerColor.equals("BLUE")) {
				playerI = -1;
			}
			for (int i = 0; i < 2; i++) {
				int x = in.nextInt();
				int y = in.nextInt();
				int angle = in.nextInt();

				robots[i] = new Robot(score);
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
					robots[0].getTrajectory().gotoD(500).run();
					try2ndPort(robots[0], playerI);
				} else if (turn == 1) {
					robots[1].getTrajectory().gotoD(200).run();
				} else {
					if (ACUTAL_TIME_ms >= 85000) {
						if(!_park) {
							tryPark(robots[0], robots[1], playerI, compass.equals("N") ? 1 : - 1);
						}
					} else {
						if (robots[0].getTrajectory().isDone()) {
							if (!_sockDone) {
								tryWinsock(robots[0], playerI);
							} else if (!_addLastRed) {
								tryAddLastRed(robots[0], playerI);
							}
						}

						if (robots[1].getTrajectory().isDone()) {
							if (!_lightHouse) {
								tryLightHouse(robots[1], playerI);
							}

						}
					}

				}

				for (int i = 0; i < 2; i++) {
					System.out.println(robots[i].getOutputs(ACUTAL_TIME_ms));
					System.err.println(robots[i].debugPosition());
					System.err.println(compass);
				}
				System.out.println(score[0]);
				turn += 1;
				ACUTAL_TIME_ms += 350;
			}
		}
	}
	
	private static void tryPark(Robot r1, Robot r2, int playerI, int direction) {
		r2.getTrajectory().gotoXY(1500 - playerI * (1500 - 500), 2000 - 800).run();
		r2.getTrajectory().gotoA(90).run();
		r2.getTrajectory().gotoXY(Double.NaN, 2000 - 800 + direction * 550).run();
		r2.getTrajectory().gotoA(90 - playerI * 90).run();
		r2.getTrajectory().gotoD(-350).run();
		r2.getTrajectory().addScore(5).run();
		
		r1.getTrajectory().gotoXY(1500 - playerI * (1500 - 950), 2000 - 800).run();
		r1.getTrajectory().gotoA(90).run();
		r1.getTrajectory().gotoXY(Double.NaN, 2000 - 800 + direction * 450).run();
		r1.getTrajectory().gotoA(90 - playerI * 90).run();
		r1.getTrajectory().gotoD(-600).run();
		r1.getTrajectory().addScore(5).run();
		
		_park = true;
	}

	private static void tryLightHouse(Robot robot, int playerI) {
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().gotoXY(1500 - playerI * (1500 - 300), 1500).run();
		robot.getTrajectory().gotoA(90).run();
		robot.getTrajectory().meca(MecaState.TAKE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(380).run();
		robot.getTrajectory().meca(MecaState.LIGHT).run();
		robot.getTrajectory().addScore(13).run();
		_lightHouse = true;
	}

	private static void tryAddLastRed(Robot robot, int playerI) {
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoXY(1500 - playerI * (1500 - 150), 2000 - 1750).run();
		robot.getTrajectory().gotoA(90).run();
		robot.getTrajectory().meca(MecaState.TAKE).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoXY(1500 + playerI * 250, 450).run();
		robot.getTrajectory().gotoA(90 - playerI * 90).run();
		robot.getTrajectory().gotoD(100).run();
		robot.getTrajectory().meca(MecaState.RELEASE).run();
		robot.getTrajectory().addScore(4).run();
		robot.getTrajectory().gotoD(-300).run();
		robot.getTrajectory().gotoA(90).run();
		_addLastRed = true;
	}

	private static void tryWinsock(Robot robot, int playerI) {
		robot.getTrajectory().gotoXY(1500 - playerI * (1500 - 635), 470).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoA(-90).run();
		robot.getTrajectory().gotoD(350).run();
		robot.getTrajectory().gotoA(90 + playerI * 90).run();
		robot.getTrajectory().meca(MecaState.WIND).run();
		robot.getTrajectory().addScore(5);
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(405).run();
		robot.getTrajectory().meca(MecaState.WIND).run();
		robot.getTrajectory().addScore(15).run();
		_sockDone = true;
	}

	private static void try2ndPort(Robot robot, int playerI) {
		robot.setMeca(MecaState.ACTIVATE_FRONT);
		robot.getTrajectory().gotoXY(1500 - playerI * 435, 525).run();
		robot.getTrajectory().meca(MecaState.TAKE).run();
		robot.getTrajectory().gotoD(15).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoA(90 - playerI * 90).run();
		robot.getTrajectory().meca(MecaState.TAKE).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoXY(1500 - playerI * 165, 520).run();
		robot.getTrajectory().gotoA(90 - playerI * 90).run();
		robot.getTrajectory().meca(MecaState.TAKE).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoA(90 - playerI * 90).run();
		robot.getTrajectory().gotoXY(1500 + playerI * 165, 520).run();
		robot.getTrajectory().meca(MecaState.TAKE).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(40).run();
		robot.getTrajectory().gotoA(-90).run();
		robot.getTrajectory().gotoD(300).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(155).run();
		robot.getTrajectory().meca(MecaState.TAKE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().gotoA(-90 + playerI * 20).run();
		robot.getTrajectory().meca(MecaState.TAKE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoA(-90).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(-145).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_RIGHT.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.TAKE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(-30).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_FRONT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(-30).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.TAKE.inverse(playerI)).run();
		robot.getTrajectory().meca(MecaState.ACTIVATE_LEFT.inverse(playerI)).run();
		robot.getTrajectory().gotoD(-50).run();
		robot.getTrajectory().gotoA(-90 - playerI * 20).run();
		robot.getTrajectory().meca(MecaState.RELEASE.inverse(playerI)).run();
		robot.getTrajectory().addScore(20).run();
		robot.getTrajectory().gotoD(-100).run();
	}
}
