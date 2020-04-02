import java.util.LinkedList;
import java.util.Scanner;


public class AgentAsserv {
	public static int ACUTAL_TIME_ms = 0;
	public static boolean chore = true;

	public static class PID {

		private double _kp;
		private double _sp;
		private double _out;
		private double _ki;
		private int _max_i;
		private int _integral;
		private double _kd;
		private double _last_feeback;

		public PID(double kp, double ki, double kd, int max_i) {
			_kp = kp;
			_ki = ki;
			_kd = kd;
			_max_i = max_i;
			_integral = 0;
		}

		public void setSetpoint(double sp) {
			_sp = sp;
		}

		public void compute(double feedback) {
			double delta = _sp - feedback;
			double delta_d = _last_feeback - feedback;
			_last_feeback = feedback;

			_integral += delta;
			if (_integral > _max_i) {
				_integral = _max_i;
			} else if (_integral < -_max_i) {
				_integral = _max_i;
			}

			_out = delta * _kp + delta_d * _kd + _integral * _ki;
		}

		public double getOutput() {
			return _out;
		}
	}

	public static enum OpponentDetectionMode {
		FRONT, BACK
	}

	public static enum MecaState {
		IDLE, ACTIVATE_FRONT, TAKE, LIGHT, FLAG, WIND, ACTIVATE_RIGHT, ACTIVATE_LEFT, RELEASE
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
		public static final double TRAJECTORY_XY_STOP_ANGLE_DEG = 30.0;
		public static final double TRAJECTORY_XY_STOP_MM = 3.0;

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
				getTrajectory().getRobot().setMeca(_state);
				return TrajectoryStatus.ORDER_DONE;
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

			TrajectoryOrder order = _orders.peekFirst();

			if (order != null) {

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

		public String getOutputs(int current_time_ms) {
			compute(current_time_ms);

			return _left_motor + " " + _right_motor + " " + _meca.toString();
		}

		private void compute(int current_time_ms) {
			_trajectory.compute(current_time_ms);
			_pid_dist.compute(_distance);
			_pid_angu.compute(_angle);

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
		}

	}

	public static class Action {

		protected Robot _robot;

		public void checkDuringExec() {
			// TODO Auto-generated method stub

		}

		public void execute() {
			System.err.println("Exec ?");
		}

		public Action setRobot(Robot robot) {
			_robot = robot;
			return this;
		}

	}

	public static class WindsockAct extends Action {

		private int _id;

		public WindsockAct(int i) {
			_id = i;
		}

		public double getX() {
			switch (_id) {
			case 1:
				return 230;
			case 2:
				return 635;
			case -1:
				return 2270;
			case -2:
				return 2365;
			}

			return Double.NaN;
		}

		public void execute() {
			_robot.getTrajectory().gotoXY(getX(), getY()).run();
			_robot.getTrajectory().gotoA(-90).run();
			System.err.println("Exec WindsockAct " + _id);
		}

		private double getY() {
			return 500;
		}
	}

	public static class ActionScheduler {

		private LinkedList<Action> _actions = new LinkedList<Action>();
		private Action[] _current_actions = { null, null };
		private Robot[] _robots;

		public ActionScheduler(Robot[] robots) {
			_robots = robots;
		}

		public void registerAction(Action action) {
			_actions.add(action);
		}

		public void compute(int time_ms) {
			for (int i = 0; i < 2; i += 1) {
				if (_current_actions[i] != null) {
					_current_actions[i].checkDuringExec();
				}
			}

			for (int i = 0; i < 2; i += 1) {
				if (i == 2 && time_ms < 1000) {
					continue;
				}
				if (_current_actions[i] == null) {
					findAction(i);
				}
			}
		}

		private void findAction(int i) {
			for (Action a : _actions) {
				if ((_current_actions[0] != a) && (_current_actions[1] != a)) {
					_current_actions[i] = a;
					_current_actions[i].setRobot(_robots[i]).execute();
					break;
				}
			}
		}

	}

	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int turn = 0;
			int score = 2;
			boolean flag = false;
			Robot[] robots = { null, null };
			ActionScheduler as = new ActionScheduler(robots);

			boolean goto_compass = false;
			String compass = null;
			String playerColor = in.next();
			int playerColorI = 1;

			if (playerColor.equals("YELLOW")) {
				playerColorI = -1;
			}

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

				if (!chore) {
					if (turn == 0) {
						as.registerAction(new WindsockAct(1 * playerColorI));
						as.registerAction(new WindsockAct(2 * playerColorI));

						robots[0].getTrajectory().gotoD(500).run();
					}
				} else {
					if (turn == 0) {
						/*
						 * if (playerColor.equals("BLUE")) { robots[0].getTrajectory().gotoD(1500 -
						 * robots[0].getX()).run(); } else {
						 * robots[0].getTrajectory().gotoD(robots[0].getX() - 1500).run(); }
						 */
						robots[0].getTrajectory().gotoD(350).run();
						robots[0].getTrajectory().gotoA(-90).run();
						robots[0].getTrajectory().meca(MecaState.ACTIVATE_FRONT).run();
						robots[0].getTrajectory().gotoD(1200).setMaxTime(5000).run();
						robots[0].getTrajectory().meca(MecaState.WIND).run();
						robots[0].getTrajectory().gotoD(-1000).run();
						if (playerColor.equals("BLUE")) {
							robots[0].getTrajectory().gotoA(45).run();
						} else {
							robots[0].getTrajectory().gotoA(135).run();
						}
					} else if (turn == 3) {
						robots[1].getTrajectory().gotoD(30).run();
						robots[1].getTrajectory().gotoA(90).run();
						if (playerColor.equals("BLUE")) {
							robots[1].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
						} else {
							robots[1].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
						}
						robots[1].getTrajectory().gotoD(-935).run();
						robots[1].getTrajectory().meca(MecaState.TAKE).run();
						for (int i = 0; i < 4; i += 1) {
							robots[1].getTrajectory().gotoD(75).run();
							if (playerColor.equals("BLUE")) {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
							} else {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
							}
							robots[1].getTrajectory().meca(MecaState.TAKE).run();
							if (playerColor.equals("BLUE")) {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
							} else {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
							}
							robots[1].getTrajectory().meca(MecaState.TAKE).run();
						}
						robots[1].getTrajectory().gotoD(580).run();
						if (playerColor.equals("BLUE")) {
							robots[1].getTrajectory().gotoA(0).run();
						} else {
							robots[1].getTrajectory().gotoA(180).run();
						}
						robots[1].getTrajectory().gotoD(70).run();
						robots[1].getTrajectory().gotoA(90).run();
						for (int i = 0; i < 4; i += 1) {
							robots[1].getTrajectory().gotoD(10).run();
							if (playerColor.equals("BLUE")) {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
							} else {
								robots[1].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
							}
							robots[1].getTrajectory().meca(MecaState.RELEASE).run();
						}
						robots[1].getTrajectory().gotoD(500).run();
						robots[1].getTrajectory().meca(MecaState.ACTIVATE_FRONT).run();
						robots[1].getTrajectory().meca(MecaState.TAKE).run();
						if (playerColor.equals("BLUE")) {
							robots[1].getTrajectory().gotoA(90-20).run();
						} else {
							robots[1].getTrajectory().gotoA(90+20).run();
						}
						robots[1].getTrajectory().meca(MecaState.ACTIVATE_FRONT).run();
						robots[1].getTrajectory().meca(MecaState.TAKE).run();
						robots[1].getTrajectory().gotoA(90).run();
						robots[1].getTrajectory().meca(MecaState.ACTIVATE_FRONT).run();
						robots[1].getTrajectory().gotoD(400).setMaxTime(2500).run();
						robots[1].getTrajectory().meca(MecaState.LIGHT).run();
						robots[1].getTrajectory().gotoD(-700).run();
					} else if (compass != null && !goto_compass && robots[1].getTrajectory().isDone()) {

						goto_compass = true;
						if (compass.equals("N")) {
							if (playerColor.equals("BLUE")) {
								robots[0].getTrajectory().gotoA(180).run();
								robots[0].getTrajectory().gotoD(350).run();
								robots[0].getTrajectory().gotoA(90).run();
								robots[0].getTrajectory().gotoD(400).run();
								robots[1].getTrajectory().gotoD(500).run();
							} else {
								robots[0].getTrajectory().gotoA(0).run();
								robots[0].getTrajectory().gotoD(350).run();
								robots[0].getTrajectory().gotoA(90).run();
								robots[0].getTrajectory().gotoD(400).run();
								robots[1].getTrajectory().gotoD(500).run();
							}
						} else {
							if (playerColor.equals("BLUE")) {
								robots[0].getTrajectory().gotoA(180).run();
								robots[0].getTrajectory().gotoD(350).run();
								robots[0].getTrajectory().gotoA(-90).run();
								robots[0].getTrajectory().gotoD(200).run();
								robots[1].getTrajectory().gotoD(-500).run();
							} else {
								robots[0].getTrajectory().gotoA(0).run();
								robots[0].getTrajectory().gotoD(350).run();
								robots[0].getTrajectory().gotoA(-90).run();
								robots[0].getTrajectory().gotoD(200).run();
								robots[1].getTrajectory().gotoD(-500).run();
							}
						}

						score += 10;
					} else if (ACUTAL_TIME_ms > 96000 && !flag) {
						boolean take_right = false;
						if (compass.equals("N")) {
							if (playerColor.equals("BLUE")) {
								take_right = true;
							}
						} else {
							if (!playerColor.equals("BLUE")) {
								take_right = true;
							}
						}

						if (take_right) {
							robots[0].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
							robots[0].getTrajectory().meca(MecaState.TAKE).run();
							robots[0].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
							robots[0].getTrajectory().meca(MecaState.RELEASE).run();
						} else {
							robots[0].getTrajectory().meca(MecaState.ACTIVATE_LEFT).run();
							robots[0].getTrajectory().meca(MecaState.TAKE).run();
							robots[0].getTrajectory().meca(MecaState.ACTIVATE_RIGHT).run();
							robots[0].getTrajectory().meca(MecaState.RELEASE).run();
						}

						// robots[0].getTrajectory().meca(MecaState.FLAG).run();
						robots[1].setMeca(MecaState.FLAG);
						flag = true;
						score += 10; // flag
						score += 13; // light
						score += 5; // winsocks
						score += 2; // cup
						score += 4; // cup
					}
				}

				if (!chore) {
					as.compute(ACUTAL_TIME_ms);
				}
				for (int i = 0; i < 2; i++) {
					System.out.println(robots[i].getOutputs(ACUTAL_TIME_ms));
					System.err.println(robots[i].debugPosition());
				}
				System.out.println(score);
				turn += 1;
				ACUTAL_TIME_ms += 350;
			}
		}
	}
}
