package com.codingame.game;

import java.util.LinkedList;
import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.dynamics.contact.ContactPoint;
import org.dyn4j.dynamics.joint.PrismaticJoint;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;
import com.codingame.gameengine.module.entities.Circle;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.entities.Text.FontWeight;

public class Player extends AbstractMultiplayerPlayer implements ZObject {
	private enum MechanicalState {
		IDLE, ACTIVATE_FRONT, ACTIVATE_RIGHT, ACTIVATE_LEFT, TAKE,
	}

	private static final int OFFSET_W = 1610;
	private static final int MAX_CUP_PER_ROBOT = 4;
	private static final int SIDE_GRABBER_L_mm = 85;
	private static final int SIDE_GRABBER_W_mm = 25;
	private static final double MAX_MOTOR = 1000.0;
	private static final double V_MAX = 2;

	private Body[] _body = { null, null };
	private Group[] _shape = { null, null };

	private double[] _width_mm = { 250, 250 };
	private double[] _height_mm = { 150, 150 };
	private Vector2[] _last_robot_position = { null, null };
	private Vector2[] _last_robot_direction = { null, null };
	private double[] _total_left_value = { 0, 0 };
	private double[] _total_right_value = { 0, 0 };
	private Text _scoreArea;
	private boolean _isOutOfStartingArea = false;
	private boolean _fail = false;
	private Text _regularScoreArea;
	private Text _estimatedScoreArea;
	private int _estimatedScore;
	private MechanicalState[] _mechanical_state = { MechanicalState.IDLE, MechanicalState.IDLE };
	private Circle[] _graberFront = { null, null };
	private com.codingame.gameengine.module.entities.Rectangle[] _graberLeft = { null, null };
	private com.codingame.gameengine.module.entities.Rectangle[] _graberRight = { null, null };
	private LinkedList<Eurobot2020Cup>[] _cupTaken = null;
	private int[] _lastPenalty = { -50000, -50000 };
	private Text _penaltiesArea;
	private Text _bonusArea;
	private LidarSensor[] _lidars = { null, null };
	private int _penalties;
	private LightHouse _lighthouse = null;
	private WindSock[] _windsocks = { null, null };

	private LinkedList<LinkedList<IRSensor>> _sensors = new LinkedList<LinkedList<IRSensor>>();

	private Sprite[] _flag = { null, null };
	private Body[] _refbody= { null, null };
	private double[] _angularVelocity = {0, 0};
	private double[] _velocity = {0, 0};
	

	public void updateSetpoints() {
		int i;
		for (i = 0; i < 2; i += 1) {
		_refbody[i].translate(_body[i].getWorldCenter().subtract(_refbody[i].getWorldCenter()));
		_refbody[i].setAngularVelocity(_angularVelocity[i]);
		Vector2 velocity2D = _body[i].getTransform().getRotation().rotate90().toVector(_velocity[i]);
		_body[i].setLinearVelocity(velocity2D);
		}
	}

	int getAction(Referee referee) throws NumberFormatException, TimeoutException, ArrayIndexOutOfBoundsException {
		// Extract robot 1 and 2 set points
		int i;
		for (i = 0; i < 2; i += 1) {
			String[] line1 = this.getOutputs().get(i).split(" ");
			double left_motor = Integer.parseInt(line1[0]);
			double right_motor = Integer.parseInt(line1[1]);
			String order = line1[2];

			// clamp motors set point
			if (left_motor > MAX_MOTOR) {
				left_motor = MAX_MOTOR;
			}
			if (left_motor < -MAX_MOTOR) {
				left_motor = -MAX_MOTOR;
			}
			if (right_motor > MAX_MOTOR) {
				right_motor = MAX_MOTOR;
			}
			if (right_motor < -MAX_MOTOR) {
				right_motor = -MAX_MOTOR;
			}

			// assign motor setpoints
			final double max_speed = (V_MAX / (_width_mm[i] / 2000.0));
			_angularVelocity[i] = (right_motor - left_motor) / (2 * MAX_MOTOR) * max_speed;
			_velocity[i] = (right_motor + left_motor) / (2 * MAX_MOTOR) * V_MAX;

			

			// check collision
			List<ContactPoint> cts = _body[i].getContacts(false);
			for (ContactPoint cp : cts) {
				Body b = cp.getBody1();
				if (b == _body[i]) {
					b = cp.getBody2();
				}
				if (b.getUserData() instanceof Player) {
					Player p = (Player) b.getUserData();
					if (p != this) {
						// check if we are moving foward
						Vector2 op_pos = _body[i].getTransform()
								.getInverseTransformed(b.getTransform().getTranslation());
						if (op_pos.y >= 0) {
							if (_velocity[i] <= 0) {
								continue;
							}
						} else {
							if (_velocity[i] >= 0) {
								continue;
							}
						}

						// add penalties
						int now = referee.getElapsedTime();
						int delta = now - _lastPenalty[i];
						_lastPenalty[i] = now;
						if (delta > 2000) {
							_penalties += 20;
						}
					}
				}
			}

			// Check forbidden areas
			AABB p1;
			AABB p2;

			// Génération des zones de marquage de points
			if (getIndex() == 1) {
				p1 = new AABB(0.0, 2.0 - 1.1, 0.4, 2.0 - 0.5);
				p2 = new AABB(1.65, 0, 1.95, 2.0 - 1.7);
			} else {
				p1 = new AABB(3.0 - 0.4, 2.0 - 1.1, 3.0, 2.0 - 0.5);
				p2 = new AABB(1.05, 0, 1.35, 2.0 - 1.7);
			}

			List<DetectResult> res = new LinkedList<DetectResult>();
			if (referee.getWorld().detect(p1, _body[i], true, res)) {
				deactivateAndReset(referee, "You can not be in this area");
			}
			if (referee.getWorld().detect(p2, _body[i], true, res)) {
				deactivateAndReset(referee, "You can not be in this area");
			}

			// parse mechanical order
			switch (order) {
			case "IDLE":
				// Do nothing
				break;

			case "ACTIVATE_FRONT":
				// prepare taking from front
				_mechanical_state[i] = MechanicalState.ACTIVATE_FRONT;
				break;

			case "ACTIVATE_LEFT":
				// prepare taking from front
				_mechanical_state[i] = MechanicalState.ACTIVATE_LEFT;
				break;

			case "ACTIVATE_RIGHT":
				// prepare taking from front
				_mechanical_state[i] = MechanicalState.ACTIVATE_RIGHT;
				break;

			case "TAKE": {
				// take something
				Convex shape = getActionShape(i);

				if (shape != null) {
					LinkedList<DetectResult> results = new LinkedList<DetectResult>();
					referee.getWorld().detect(shape, results);
					for (DetectResult r : results) {
						if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
							Eurobot2020Cup cup = (Eurobot2020Cup) r.getBody().getUserData();
							take(referee, i, cup);
							break;
						}
					}
				}

				_mechanical_state[i] = MechanicalState.IDLE;
			}
				break;

			case "RELEASE":
				// take something
				switch (_mechanical_state[i]) {
				case ACTIVATE_FRONT:
					if (_cupTaken != null) {
						if (_cupTaken[i].size() > 0) {
							Eurobot2020Cup c = _cupTaken[i].pollLast();
							c.addToTable(referee, _body[i].getTransform()
									.getTransformed(new Vector2(0, 0.08 + _height_mm[i] / 2000.0)));
						}
					}
					break;

				case ACTIVATE_LEFT:
					if (_cupTaken != null) {
						if (_cupTaken[i].size() > 0) {
							Eurobot2020Cup c = _cupTaken[i].pollLast();
							c.addToTable(referee, _body[i].getTransform()
									.getTransformed(new Vector2(-0.08 - _width_mm[i] / 2000.0, 0)));
						}
					}
					break;

				case ACTIVATE_RIGHT:
					if (_cupTaken != null) {
						if (_cupTaken[i].size() > 0) {
							Eurobot2020Cup c = _cupTaken[i].pollLast();
							c.addToTable(referee, _body[i].getTransform()
									.getTransformed(new Vector2(0.08 + _width_mm[i] / 2000.0, 0)));
						}
					}
					break;

				default:
					break;
				}

				_mechanical_state[i] = MechanicalState.IDLE;
				break;

			case "LIGHT": {
				// take something
				Convex shape = getActionShape(i);

				if (shape != null) {
					LinkedList<DetectResult> results = new LinkedList<DetectResult>();
					referee.getWorld().detect(shape, results);
					for (DetectResult r : results) {
						if (r.getBody().getUserData() instanceof LightHouse) {
							((LightHouse) r.getBody().getUserData()).activate();
							break;
						}
					}
				}

				_mechanical_state[i] = MechanicalState.IDLE;

			}
				break;

			case "WIND": {
				// take something
				Convex shape = getActionShape(i);

				if (shape != null) {
					LinkedList<DetectResult> results = new LinkedList<DetectResult>();
					referee.getWorld().detect(shape, results);
					for (DetectResult r : results) {
						if (r.getBody().getUserData() instanceof WindSock) {
							((WindSock) r.getBody().getUserData()).activate();
							break;
						}
					}
				}

				_mechanical_state[i] = MechanicalState.IDLE;

			}
				break;

			case "FLAG":
				_flag[i].setVisible(true);
				if (referee.getElapsedTime() < 95000) {
					this.deactivateAndReset(referee, "You can not show the flag before the 95 s");
				}
				_mechanical_state[i] = MechanicalState.IDLE;
				break;

			default:
				this.deactivate("Invalid order '" + order + "'");
				break;
			}
		}

		// Extract score data
		_estimatedScore = Integer.parseInt(this.getOutputs().get(2));

		return 0;
	}

	private Convex getActionShape(int robot) {
		Convex shape = null;
		switch (_mechanical_state[robot]) {
		case ACTIVATE_FRONT:
			shape = new org.dyn4j.geometry.Circle(0.04);
			shape.translate(_body[robot].getWorldPoint(new Vector2(0, _height_mm[robot] / 2000.0)));
			break;

		case ACTIVATE_LEFT:
			shape = new org.dyn4j.geometry.Rectangle(_width_mm[robot] / 2000.0 + (SIDE_GRABBER_L_mm / 1000.0), SIDE_GRABBER_W_mm / 1000.0);
			shape.rotate(_body[robot].getTransform().getRotationAngle());
			shape.translate(
					_body[robot].getTransform().getTransformed(new Vector2(-_width_mm[robot] / 4000.0 - (SIDE_GRABBER_L_mm / 1000.0), 0)));
			break;

		case ACTIVATE_RIGHT:
			shape = new org.dyn4j.geometry.Rectangle(_width_mm[robot] / 2000.0 + (SIDE_GRABBER_L_mm / 1000.0), SIDE_GRABBER_W_mm / 1000.0);
			shape.translate(
					_body[robot].getTransform().getTransformed(new Vector2(_width_mm[robot] / 4000.0 + (SIDE_GRABBER_L_mm / 1000.0), 0)));
			break;

		default:
			break;
		}

		return shape;
	}

	@SuppressWarnings("unchecked")
	private void take(Referee referee, int robot, Eurobot2020Cup cup) {
		if (_cupTaken == null) {
			_cupTaken = (LinkedList<Eurobot2020Cup>[]) new LinkedList<?>[2];
			_cupTaken[0] = new LinkedList<Eurobot2020Cup>();
			_cupTaken[1] = new LinkedList<Eurobot2020Cup>();
		}
		if (_cupTaken[robot].size() < MAX_CUP_PER_ROBOT) {
			double x = -0.3 - 0.1 * _cupTaken[robot].size();
			if (getIndex() != 0) {
				x = 3.0 - x;
			}
			cup.removeFromTable(referee, x, 0.75 - 0.25 * robot);
			_cupTaken[robot].add(cup);
		}
	}

	@Override
	public int getExpectedOutputLines() {
		return 3;
	}

	public Body[] getBodies() {
		return _body;
	}

	public void deactivateAndReset(Referee referee, String reason) {
		deactivate(reason);
		reset(referee);
		_fail = true;
	}

	public void reset(Referee referee) {
		for (int i = 0; i < 2; i += 1) {
			referee.getWorld().removeBody(_body[i]);
			_body[i].translateToOrigin();
			_body[i].rotate(-_body[i].getTransform().getRotationAngle());
			if (getIndex() == 0) {
				_body[i].rotate(-Math.PI / 2);
				if (i == 0) {
					_body[i].translate(0.4 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				} else {
					_body[i].translate(0.0 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				}

			} else {
				_body[i].rotate(Math.PI / 2);
				if (i == 0) {
					_body[i].translate(3.0 - 0.4 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				} else {
					_body[i].translate(3.0 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				}
			}
			_body[i].setLinearVelocity(new Vector2(0, 0));
			_body[i].setAngularVelocity(0);
			referee.getWorld().addBody(_body[i]);
			_body[i].setLinearVelocity(new Vector2(0, 0));
			_body[i].setAngularVelocity(0);
			_body[i].setLinearDamping(9999);
			_body[i].setAngularDamping(9999);
		}
	}

	public void createBodies(Referee referee) {
		int color;

		// Detection de la couleur
		if (getIndex() == 0) {
			color = 0x007cb0;
		} else {
			color = 0xf7b500;
		}

		_lighthouse = new LightHouse(referee, getIndex());
		for (int i = 0; i < 2; i += 1) {
			_windsocks[i] = new WindSock(referee, getIndex(), i);
		}

		for (int i = 0; i < 2; i += 1) {
			// Corps pour le moteur physique
			_body[i] = new Body();
			_refbody[i] = new Body();
			_refbody[i].setUserData(this);
			_refbody[i].setMass(MassType.FIXED_ANGULAR_VELOCITY);

			Rectangle shape = new Rectangle(_width_mm[i] / 1000.0, _height_mm[i] / 1000.0);

			BodyFixture fixtureBody = new BodyFixture(shape);
			fixtureBody.setDensity(200);
			fixtureBody.setRestitution(0);
			fixtureBody.setFriction(10000);
			_body[i].addFixture(fixtureBody);
			_body[i].translateToOrigin();
			_body[i].setMass(MassType.NORMAL);
			_body[i].setAutoSleepingEnabled(false);
			_body[i].setBullet(true);
			_body[i].setUserData(this);
			_body[i].setLinearDamping(0.1);
			_body[i].setAngularDamping(0.001);

			fixtureBody = new BodyFixture(new org.dyn4j.geometry.Circle(0.05));
			fixtureBody.setSensor(true);
			_body[i].addFixture(fixtureBody);

			LinkedList<IRSensor> irSensorList = new LinkedList<IRSensor>();
			_sensors.add(irSensorList);

			double sensor_max_distance = 0.8;
			double lidar_max_distance = 5;

			irSensorList.add(new IRSensor(referee, SensorType.LOW, this, i, 0, sensor_max_distance,
					new Vector2(0, _height_mm[i] / 2000.0)));
			irSensorList.add(new IRSensor(referee, SensorType.LOW, this, i, 90, sensor_max_distance,
					new Vector2(_width_mm[i] / 2000.0, 0)));
			irSensorList.add(new IRSensor(referee, SensorType.LOW, this, i, 180, sensor_max_distance,
					new Vector2(0, -_height_mm[i] / 2000.0)));
			irSensorList.add(new IRSensor(referee, SensorType.LOW, this, i, -90, sensor_max_distance,
					new Vector2(-_width_mm[i] / 2000.0, 0)));

			irSensorList.add(new IRSensor(referee, SensorType.HIGH, this, i, 0, sensor_max_distance,
					new Vector2(-_width_mm[i] / 2000.0, _height_mm[i] / 2000.0)));
			irSensorList.add(new IRSensor(referee, SensorType.HIGH, this, i, 0, sensor_max_distance,
					new Vector2(_width_mm[i] / 2000.0, _height_mm[i] / 2000.0)));
			irSensorList.add(new IRSensor(referee, SensorType.HIGH, this, i, 180, sensor_max_distance,
					new Vector2(_width_mm[i] / 2000.0, -_height_mm[i] / 2000.0)));
			irSensorList.add(new IRSensor(referee, SensorType.HIGH, this, i, 180, sensor_max_distance,
					new Vector2(-_width_mm[i] / 2000.0, -_height_mm[i] / 2000.0)));

			_lidars[i] = new LidarSensor(referee, this, i, lidar_max_distance);

			Vector2 position = new Vector2();
			double rotation = 0;
			if (getIndex() == 0) {
				rotation = - Math.PI / 2;
				if (i == 0) {
					position.add(0.4 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				} else {
					position.add(0.0 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				}

			} else {
				rotation = Math.PI / 2;
				if (i == 0) {
					position.add(3.0 - 0.4 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				} else {
					position.add(3.0 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				}

			}

			_body[i].rotate(rotation);
			_body[i].translate(position);
			_refbody[i].rotate(rotation);	
			_refbody[i].translate(position);
		
			
			referee.getWorld().addBody(_body[i]);
			referee.getWorld().addBody(_refbody[i]);
			
			PrismaticJoint pj = new PrismaticJoint(_refbody[i], _body[i], position, new Vector2(0, 1).rotate(rotation));
			referee.getWorld().addJoint(pj);
			
			// Dessin sur l'interface
			com.codingame.gameengine.module.entities.Rectangle rectangle = referee.getGraphicEntityModule()
					.createRectangle();
			rectangle.setWidth((int) _width_mm[i]).setHeight((int) _height_mm[i]);
			rectangle.setLineColor(0x000000);
			rectangle.setLineWidth(2);
			rectangle.setFillColor(color);
			rectangle.setX((int) (-_width_mm[i] / 2));
			rectangle.setY((int) (-_height_mm[i] / 2));
			Text text = referee.getGraphicEntityModule().createText(String.format("%d", i + 1));
			text.setFontSize(64).setFontWeight(FontWeight.BOLD).setStrokeColor(0xFFFFFF).setFillColor(0xFFFFFF)
					.setX(-16).setY(-32);

			_graberFront[i] = referee.getGraphicEntityModule().createCircle();
			_graberFront[i].setFillColor(0xFFFFFF);
			_graberFront[i].setRadius(40);
			_graberFront[i].setY((int) (-_height_mm[i] / 2));
			_graberFront[i].setFillAlpha(0.5);
			_graberFront[i].setLineAlpha(0);
			_graberFront[i].setVisible(false);
			
			_graberLeft[i] = referee.getGraphicEntityModule().createRectangle();
			_graberLeft[i].setFillColor(0xFFFFFF);
			_graberLeft[i].setHeight(SIDE_GRABBER_W_mm);
			_graberLeft[i].setY(-SIDE_GRABBER_W_mm/2);
			_graberLeft[i].setWidth(SIDE_GRABBER_L_mm);
			_graberLeft[i].setX((int) (-_width_mm[i] / 2 - SIDE_GRABBER_L_mm));
			_graberLeft[i].setFillAlpha(0.5);
			_graberLeft[i].setLineAlpha(0);
			_graberLeft[i].setVisible(false);
			
			_graberRight[i] = referee.getGraphicEntityModule().createRectangle();
			_graberRight[i].setFillColor(0xFFFFFF);
			_graberRight[i].setHeight(SIDE_GRABBER_W_mm);
			_graberRight[i].setWidth(SIDE_GRABBER_L_mm);
			_graberRight[i].setX((int) (_width_mm[i] / 2));
			_graberRight[i].setY(-SIDE_GRABBER_W_mm/2);
			_graberRight[i].setFillAlpha(0.5);
			_graberRight[i].setLineAlpha(0);
			_graberRight[i].setVisible(false);

			_flag[i] = referee.getGraphicEntityModule().createSprite().setImage("Flag_CO.png");
			_flag[i].setScale(0.5).setX(-450 / 4).setY(-225 / 4);
			_flag[i].setVisible(false);

			_shape[i] = referee.getGraphicEntityModule().createGroup();
			_shape[i].add(_graberFront[i]);
			_shape[i].add(_graberLeft[i]);
			_shape[i].add(_graberRight[i]);
			_shape[i].add(rectangle);
			_shape[i].add(text);
			_shape[i].add(_flag[i]);
		}

		// Créations des textes
		_regularScoreArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF)
				.setStrokeColor(0xFFFFFF).setFontSize(32).setX(10 + getIndex() * OFFSET_W).setY(300);
		_estimatedScoreArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF)
				.setStrokeColor(0xFFFFFF).setFontSize(32).setX(10 + getIndex() * OFFSET_W).setY(350);
		_penaltiesArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF).setStrokeColor(0xFFFFFF)
				.setFontSize(32).setX(10 + getIndex() * OFFSET_W).setY(400);
		_bonusArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF).setStrokeColor(0xFFFFFF)
				.setFontSize(32).setX(10 + getIndex() * OFFSET_W).setY(450);
		_scoreArea = referee.getGraphicEntityModule().createText("000").setFillColor(color).setStrokeColor(0xFFFFFF)
				.setFontSize(128).setFontWeight(FontWeight.BOLDER).setX(35).setY(35);

		Text nickname = referee.getGraphicEntityModule().createText(getNicknameToken()).setX(0).setFontSize(42)
				.setFontWeight(FontWeight.BOLDER).setFillColor(color).setStrokeColor(color);
		Sprite avatar = referee.getGraphicEntityModule().createSprite().setImage(getAvatarToken());
		avatar.setAnchorY(1);
		avatar.setBaseHeight(150);
		avatar.setBaseWidth(150);
		avatar.setY(referee.getGraphicEntityModule().getWorld().getHeight());
		nickname.setY(avatar.getY() - avatar.getBaseHeight() - 20);
		nickname.setAnchorY(1);

		if (getIndex() == 1) {
			avatar.setAnchorX(1);
			avatar.setX(referee.getGraphicEntityModule().getWorld().getWidth());

			nickname.setAnchorX(1);
			nickname.setX(referee.getGraphicEntityModule().getWorld().getWidth());

			_scoreArea.setX(nickname.getX() - _scoreArea.getX());
			_scoreArea.setAnchorX(1);
		}
	}

	public void render(Referee referee) {

		if (!_isOutOfStartingArea) {
			// Detection si le robot est sorti !
			AABB startarea = new AABB(0 + getIndex() * (3 - 0.4), 2.0 - 1.07, 0.4 + getIndex() * (3 - 0.4),
					2.0 - 0.530);

			for (int i = 0; i < 2; i += 1) {
				if (!referee.getWorld().detect(startarea, _body[i], false, new LinkedList<DetectResult>())) {
					_isOutOfStartingArea = true;
				}
			}
		}

		// Calcul du score
		computeScore(referee);

		for (int i = 0; i < 2; i += 1) {

			// Compute sensor detection
			for (IRSensor sensor : _sensors.get(i)) {
				sensor.compute(referee, _body[i]);
			}
			_lidars[i].compute(referee, _body[i]);

			// Récupération de la position en mètres et la rotation en radians
			Vector2 position = _body[i].getTransform().getTranslation();
			double rotation = _body[i].getTransform().getRotationAngle();

			// Converion en mm
			position.x *= 1000;
			position.y *= 1000;

			// Modification de la rotation car le repère de l'écran est indirect
			rotation = 0 - rotation;
			referee.displayShape(_shape[i], position, rotation, 1);

			Convex shape;
			// Affichage de la meca
			_graberRight[i].setLineWidth(0);
			_graberFront[i].setLineWidth(0);
			_graberLeft[i].setLineWidth(0);
			switch (_mechanical_state[i]) {
			case ACTIVATE_FRONT:
				_graberFront[i].setVisible(true);
				break;

			case ACTIVATE_LEFT:
				_graberLeft[i].setVisible(true);
				break;

			case ACTIVATE_RIGHT:
				_graberRight[i].setVisible(true);
				break;

			default:
			case IDLE:
				_graberFront[i].setVisible(false);
				_graberLeft[i].setVisible(false);
				_graberRight[i].setVisible(false);
				break;
			}
			
			shape = getActionShape(i);
			if (shape != null) {
				// Recherche d'élements prenables
				LinkedList<DetectResult> results = new LinkedList<DetectResult>();
				referee.getWorld().detect(shape, results);
				for (DetectResult r : results) {
					Object o = r.getBody().getUserData();
					if ((o instanceof Eurobot2020Cup) || (o instanceof LightHouse) || (o instanceof WindSock)) {
						switch (_mechanical_state[i]) {
						case ACTIVATE_FRONT:
							_graberFront[i].setLineColor(0);
							_graberFront[i].setLineWidth(16);
							_graberFront[i].setLineAlpha(1);
							break;
						case ACTIVATE_LEFT:
							_graberLeft[i].setLineColor(0);
							_graberLeft[i].setLineWidth(16);
							_graberLeft[i].setLineAlpha(1);
							break;
						case ACTIVATE_RIGHT:
							_graberRight[i].setLineColor(0);
							_graberRight[i].setLineWidth(16);
							_graberRight[i].setLineAlpha(1);
							break;
						default:
							break;
						}
					}
				}
			}
		}
	}

	private void computeScore(Referee referee) {
		int score = 0;
		int bonus1 = 0;
		int bonus2 = 0;
		int classical_score = 2; // for the light house

		if (_isOutOfStartingArea && !_fail) {
			bonus1 = 5;
			score = bonus1;

			AABB p1;
			AABB p2;
			AABB p1g;
			AABB p1r;
			AABB p2g;
			AABB p2r;

			// Génération des zones de marquage de points
			if (getIndex() == 0) {
				p1 = new AABB(0.0, 2.0 - 1.1, 0.4, 2.0 - 0.5);
				p2 = new AABB(1.65, 0, 1.95, 2.0 - 1.7);

				p1g = new AABB(0.0, 2.0 - 0.53, 0.4, 2 - 0.50);
				p1r = new AABB(0.0, 2.0 - 1.1, 0.4, 2 - 1.07);

				p2g = new AABB(1.65, 0, 1.75, 2 - 1.7);
				p2r = new AABB(1.85, 0, 1.95, 2 - 1.7);
			} else {
				p1 = new AABB(3.0 - 0.4, 2.0 - 1.1, 3.0, 2.0 - 0.5);
				p2 = new AABB(1.05, 0, 1.35, 2.0 - 1.7);

				p1r = new AABB(3 - 0.4, 2.0 - 0.53, 3.0, 2 - 0.50);
				p1g = new AABB(3 - 0.4, 2.0 - 1.1, 3.0, 2 - 1.07);

				p2g = new AABB(1.05, 0, 1.15, 2 - 1.7);
				p2r = new AABB(1.25, 0, 1.35, 2 - 1.7);
			}

			LinkedList<DetectResult> results = new LinkedList<DetectResult>();
			referee.getWorld().detect(p1, results);
			referee.getWorld().detect(p2, results);
			for (DetectResult r : results) {
				if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
					classical_score += 1;
				}
			}

			// Vérification cannaux port 1
			results.clear();
			int green = 0;
			referee.getWorld().detect(p1g, results);
			for (DetectResult r : results) {
				if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
					Eurobot2020Cup c = (Eurobot2020Cup) r.getBody().getUserData();
					if (c.getType() == Eurobot2020CupType.GREEN) {
						green += 1;
					}
				}
			}

			results.clear();
			int red = 0;
			referee.getWorld().detect(p1r, results);
			for (DetectResult r : results) {
				if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
					Eurobot2020Cup c = (Eurobot2020Cup) r.getBody().getUserData();
					if (c.getType() == Eurobot2020CupType.RED) {
						red += 1;
					}
				}
			}

			classical_score += red + green + 2 * Integer.min(red, green);

			// Vérification cannaux port 2
			results.clear();
			green = 0;
			referee.getWorld().detect(p2g, results);
			for (DetectResult r : results) {
				if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
					Eurobot2020Cup c = (Eurobot2020Cup) r.getBody().getUserData();
					if (c.getType() == Eurobot2020CupType.GREEN) {
						green += 1;
					}
				}
			}

			results.clear();
			red = 0;
			referee.getWorld().detect(p2r, results);
			for (DetectResult r : results) {
				if (r.getBody().getUserData() instanceof Eurobot2020Cup) {
					Eurobot2020Cup c = (Eurobot2020Cup) r.getBody().getUserData();
					if (c.getType() == Eurobot2020CupType.RED) {
						red += 1;
					}
				}
			}

			classical_score += red + green + 2 * Integer.min(red, green);

			// compute stop area points
			AABB boxN;
			AABB boxS;
			org.dyn4j.geometry.Circle circN;
			org.dyn4j.geometry.Circle circS;

			if (getIndex() == 0) {
				boxN = new AABB(0, 2.0 - 0.5, 0.4, 2.0 - 0.1);
				circN = new org.dyn4j.geometry.Circle(0.4);
				circN.translate(0, 2.0 - 0.5);
				boxS = new AABB(0, 2.0 - 1.5, 0.4, 2.0 - 1.1);
				circS = new org.dyn4j.geometry.Circle(0.4);
				circS.translate(0, 2.0 - 1.1);
			} else {
				boxN = new AABB(3 - 0.4, 2.0 - 0.5, 3, 2.0 - 0.1);
				circN = new org.dyn4j.geometry.Circle(0.4);
				circN.translate(3.0, 2.0 - 0.5);
				boxS = new AABB(3 - 0.4, 2.0 - 1.5, 3, 2.0 - 1.1);
				circS = new org.dyn4j.geometry.Circle(0.4);
				circS.translate(3.0, 2.0 - 1.1);
			}

			List<DetectResult> dboxN = new LinkedList<DetectResult>();
			List<DetectResult> dcircN = new LinkedList<DetectResult>();
			List<DetectResult> dboxS = new LinkedList<DetectResult>();
			List<DetectResult> dcircS = new LinkedList<DetectResult>();
			referee.getWorld().detect(boxN, dboxN);
			referee.getWorld().detect(circN, dcircN);
			referee.getWorld().detect(boxS, dboxS);
			referee.getWorld().detect(circS, dcircS);

			// detect robot areas
			int r0N = 0;
			int r0S = 0;
			int r1N = 0;
			int r1S = 0;
			for (DetectResult r : dboxN) {
				if (r.getBody() == _body[0]) {
					r0N = 1;
				}
				if (r.getBody() == _body[1]) {
					r1N = 1;
				}
			}
			for (DetectResult r : dcircN) {
				if ((r.getBody() == _body[0]) && (r0N > 0)) {
					r0N = 2;
				}
				if ((r.getBody() == _body[1]) && (r1N > 0)) {
					r1N = 2;
				}
			}
			for (DetectResult r : dboxS) {
				if (r.getBody() == _body[0]) {
					r0S = 1;
				}
				if (r.getBody() == _body[1]) {
					r1S = 1;
				}
			}
			for (DetectResult r : dcircS) {
				if ((r.getBody() == _body[0]) && (r0S > 0)) {
					r0S = 2;
				}
				if ((r.getBody() == _body[1]) && (r1S > 0)) {
					r1S = 2;
				}
			}
			// System.err.println(getIndex() + " "+ referee.getElapsedTime() + " " + r0N + "
			// " + r1N + " " + r0S + " " + r1S);
			int r0 = 0;
			int r1 = 0;
			if (r0N == 2) {
				r0 = 1;
			} else if (r0S == 2) {
				r0 = -1;
			}
			if (r1N == 2) {
				r1 = 1;
			} else if (r1S == 2) {
				r1 = -1;
			}

			if (referee.compassIsNorth()) {
				if ((r0 == 1) && (r1 == 1)) {
					classical_score += 10;
				} else if ((r0 == 1) && (r1 == 0)) {
					classical_score += 5;
				} else if ((r0 == 0) && (r1 == 1)) {
					classical_score += 5;
				} else if ((r0 == -1) && (r1 == -1)) {
					classical_score += 5;
				}
			} else {
				if ((r0 == -1) && (r1 == -1)) {
					classical_score += 10;
				} else if ((r0 == -1) && (r1 == 0)) {
					classical_score += 5;
				} else if ((r0 == 0) && (r1 == -1)) {
					classical_score += 5;
				} else if ((r0 == 1) && (r1 == 1)) {
					classical_score += 5;
				}
			}

			// add lighthouse
			if (_lighthouse.isActivated()) {
				classical_score += 13;
			}

			// show flag
			if (_flag[0].isVisible() || _flag[1].isVisible()) {
				classical_score += 10;
			}

			// winsocks
			if (_windsocks[0].isActivated() && _windsocks[1].isActivated()) {
				classical_score += 15;
			} else if (_windsocks[0].isActivated() || _windsocks[1].isActivated()) {
				classical_score += 5;
			}

			bonus2 = (int) (Math.ceil(0.3 * classical_score) - Math.abs(_estimatedScore - classical_score));
			if (bonus2 < 0) {
				bonus2 = 0;
			}
			score += bonus2;

			score += classical_score;
			score -= _penalties;

			if (score < 0) {
				score = 0;
			}
		}

		_regularScoreArea.setText(String.format("Regular points: %d", classical_score));
		_estimatedScoreArea.setText(String.format("Est. points: %d", _estimatedScore));
		_penaltiesArea.setText(String.format("Penalties: %d", -_penalties));
		_bonusArea.setText(String.format("Bonus: %d + %d", bonus1, bonus2));
		setScore(score);
		_scoreArea.setText(String.format("%03d", score));
	}

	public void compute() {
		for (int i = 0; i < 2; i += 1) {

			if (Double.isNaN(_body[i].getTransform().getTranslationX())) {
				// BUG DYN4J ??? Nan in transform...
				_body[i].setTransform(_body[i].getInitialTransform());
			} else if (Double.isNaN(_body[i].getTransform().getValues()[0])) {
				// BUG DYN4J ??? Nan in transform...
				_body[i].setTransform(_body[i].getInitialTransform());
			}

			Vector2 position = _body[i].getWorldPoint(new Vector2(0, 0));
			Vector2 direction = _body[i].getWorldVector(new Vector2(1, 0));
			double delta_d = 0;
			double delta_a = 0;

			if (_last_robot_position[i] != null) {
				Vector2 dep = new Vector2(position).subtract(_last_robot_position[i]);
				if (_body[i].getLocalPoint(_last_robot_position[i]).y <= 0) {
					delta_d = dep.getMagnitude();
				} else {
					delta_d = -dep.getMagnitude();
				}

				double angle = -direction.getAngleBetween(_last_robot_direction[i]);
				delta_a = angle * (_width_mm[i] / 2000.0);
			}
			_last_robot_position[i] = position;
			_last_robot_direction[i] = direction;

			// Ajout a l'intégrateur
			_total_left_value[i] += delta_d - delta_a;
			_total_right_value[i] += delta_d + delta_a;
		}
	}

	public void sendPlayerInputs(Referee referee) {
		for (int i = 0; i < 2; i += 1) {
			String last_taken = "?";
			if (_cupTaken != null) {
				if (_cupTaken[i].size() > 0) {
					Eurobot2020Cup c = _cupTaken[i].peekLast();
					if (c.getType() == Eurobot2020CupType.GREEN) {
						last_taken = "GREEN";
					} else {
						last_taken = "RED";
					}
				}
			}

			String compass = readCompass(referee, i);

			sendInputLine(Math.round(_total_left_value[i] * 10000.0) + " " + Math.round(_total_right_value[i] * 10000.0)
					+ " " + last_taken + " " + compass);
		}
	}

	private String readCompass(Referee referee, int i) {
		String res = "?";

		Vector2 direction = _body[i].getWorldVector(new Vector2(0, 1));
		Ray ray = new Ray(_body[i].getWorldPoint(new Vector2()), direction);
		List<RaycastResult> results = new LinkedList<RaycastResult>();

		referee.getWorld().raycast(ray, 5.0, false, true, results);

		double max_dist = 5.0;
		for (RaycastResult r : results) {
			if (r.getBody() == _body[i]) {
				continue;
			}
			double d = r.getRaycast().getDistance();
			if (d < max_dist) {
				max_dist = d;
				if (r.getBody().getUserData() instanceof CompassArea) {
					res = referee.compassIsNorth() ? "N" : "S";
				} else {
					res = "?";
				}
			}
		}

		return res;
	}

	public void sendGameConfiguration() {
		// send player color
		String color = "BLUE";
		if (getIndex() == 1) {
			color = "YELLOW";
		}

		sendInputLine(color);

		for (int i = 0; i < 2; i += 1) {
			int x = (int) (_body[i].getTransform().getTranslationX() * 1000);
			int y = (int) (_body[i].getTransform().getTranslationY() * 1000);
			int a = (int) (_body[i].getTransform().getRotationAngle() * 180.0 / Math.PI);
			a += 90;
			a %= 360;
			sendInputLine(x + " " + y + " " + a);
		}
	}

	public void sendPlayerIRSensors() {
		for (int i = 0; i < 2; i += 1) {
			String str = null;
			for (IRSensor s : _sensors.get(i)) {
				if (str == null) {
					str = "" + s.getDistance();
				} else {
					str += " " + s.getDistance();
				}
			}
			sendInputLine(str);
		}
	}

	public void sendPlayerLidarSensors() {
		for (int r = 0; r < 2; r += 1) {
			String str = "";
			for (int i = 0; i < 360; i += 1) {
				if (i == 0) {
					str = "" + _lidars[r].getValue(i);
				} else {
					str += " " + _lidars[r].getValue(i);
				}
			}
			sendInputLine(str);
		}
	}

	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		switch (type) {
		case LOW:
			return true;
		case HIGH:
			return true;
		case VERY_HIGH:
			return fixture.isSensor();
		}

		return false;
	}

}
