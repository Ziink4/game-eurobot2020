package com.codingame.game;

import java.util.LinkedList;
import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.entities.Text.FontWeight;

public class Player extends AbstractMultiplayerPlayer {
	private Body[] _body = { null, null };
	private Group[] _shape = { null, null };

	private double[] _width_mm = { 250, 250 };
	private double[] _height_mm = { 150, 150 };
	private Vector2[] _last_left_encoder_position = { null, null };
	private Vector2[] _last_right_encoder_position = { null, null };
	private int[] _total_left_value = { 0, 0 };
	private int[] _total_right_value = { 0, 0 };
	private Text _scoreArea;
	private boolean _isOutOfStartingArea = false;
	private Text _regularScoreArea;
	private Text _estimatedScoreArea;
	private int _estimatedScore;

	int getAction() throws NumberFormatException, TimeoutException, ArrayIndexOutOfBoundsException {
		// Extract robot 1 and 2 set points
		int i;
		for (i = 0; i < 2; i += 1) {
			String[] line1 = this.getOutputs().get(i).split(" ");
			double left_motor = Integer.parseInt(line1[0]);
			double right_motor = Integer.parseInt(line1[1]);

			double angularVelocity = (right_motor - left_motor) / 100.0 * 1;
			double velocity = (right_motor + left_motor) / 100.0 * 1;

			_body[i].setAngularVelocity(angularVelocity);
			Vector2 velocity2D = _body[i].getTransform().getRotation().rotate90().toVector(velocity);
			_body[i].setLinearVelocity(velocity2D);
		}

		// Extract score data
		_estimatedScore = Integer.parseInt(this.getOutputs().get(2));

		return 0;
	}

	@Override
	public int getExpectedOutputLines() {
		return 3;
	}

	public Body[] getBodies() {
		return _body;
	}

	public void createBodies(Referee referee) {
		int color;
		// Detection de la couleur
		if (getIndex() == 0) {
			color = 0x007cb0;
		} else {
			color = 0xf7b500;
		}

		for (int i = 0; i < 2; i += 1) {
			// Corps pour le moteur physique
			_body[i] = new Body();

			Rectangle shape = new Rectangle(_width_mm[i] / 1000.0, _height_mm[i] / 1000.0);

			BodyFixture fixtureBody = new BodyFixture(shape);
			fixtureBody.setDensity(20);
			fixtureBody.setRestitution(0);
			fixtureBody.setFriction(1);
			_body[i].addFixture(fixtureBody);
			_body[i].translateToOrigin();
			_body[i].setMass(MassType.NORMAL);
			_body[i].setAutoSleepingEnabled(false);
			
			if(getIndex() == 0)
			{
				_body[i].rotate(- Math.PI / 2);
				if(i == 0)
				{
					_body[i].translate(0.4 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				}
				else
				{
					_body[i].translate(0.0 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				}
				
			}
			else
			{
				_body[i].rotate(Math.PI / 2);
				if(i == 0)
				{
					_body[i].translate(3.0 - 0.4 + _height_mm[i] / 2000.0 + 0.03, 2.0 - (0.5 + 1.1) / 2);
				}
				else
				{
					_body[i].translate(3.0 - _height_mm[i] / 2000.0 - 0.03, 2.0 - (0.5 + 1.1) / 2);
				}
				
			}
			
			referee.getWorld().addBody(_body[i]);
			
			
			// Dessin sur l'interface
			com.codingame.gameengine.module.entities.Rectangle rectangle = referee.getGraphicEntityModule().createRectangle();
			rectangle.setWidth((int) _width_mm[i]).setHeight((int) _height_mm[i]);
			rectangle.setLineColor(0x000000);
			rectangle.setLineWidth(2);
			rectangle.setFillColor(color);
			rectangle.setX((int) (-_width_mm[i] / 2));
			rectangle.setY((int) (-_height_mm[i] / 2));
			Text text = referee.getGraphicEntityModule().createText(String.format("%d", i + 1));
			text.setFontSize(64).setFontWeight(FontWeight.BOLD).setStrokeColor(0xFFFFFF).setFillColor(0xFFFFFF).setX(-16).setY(-32);
			
			_shape[i] = referee.getGraphicEntityModule().createGroup();
			_shape[i].add(rectangle);
			_shape[i].add(text);
		}

		int offset_w = 1610;
		// Créations des textes
		_scoreArea = referee.getGraphicEntityModule().createText("000").setFillColor(color).setStrokeColor(0xFFFFFF)
				.setFontSize(128).setFontWeight(FontWeight.BOLDER).setX(35 + getIndex() * offset_w).setY(25);

		_regularScoreArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF)
				.setStrokeColor(0xFFFFFF).setFontSize(32).setX(10 + getIndex() * offset_w).setY(300);
		_estimatedScoreArea = referee.getGraphicEntityModule().createText("").setFillColor(0xFFFFFF)
				.setStrokeColor(0xFFFFFF).setFontSize(32).setX(10 + getIndex() * offset_w).setY(350);
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
			// Récupération de la position en mètres et la rotation en radians
			Vector2 position = _body[i].getInitialTransform().getTranslation();
			double rotation = _body[i].getInitialTransform().getRotationAngle();

			// Converion en mm
			position.x *= 1000;
			position.y *= 1000;

			// Modification de la rotation car le repère de l'écran est indirect
			rotation = 0 - rotation;
			referee.displayShape(_shape[i], position, rotation, 1);
		}
	}

	private void computeScore(Referee referee) {
		int score = 0;
		int classical_score = 0;

		if (_isOutOfStartingArea) {
			score = 5;

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
				p1r = new AABB(0.0, 2.0 - 0.53, 0.4, 2 - 0.50);

				p2g = new AABB(1.65, 0, 1.75, 2 - 1.7);
				p2r = new AABB(1.85, 0, 1.95, 2 - 1.7);
			} else {
				p1 = new AABB(3.0 - 0.4, 2.0 - 1.1, 3.0, 2.0 - 0.5);
				p2 = new AABB(1.05, 0, 1.35, 2.0 - 1.7);

				p1r = new AABB(3 - 0.4, 2.0 - 0.53, 3.0, 2 - 0.50);
				p1g = new AABB(3 - 0.4, 2.0 - 0.53, 3.0, 2 - 0.50);

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

			int bonus = (int) (Math.ceil(0.3 * classical_score) - Math.abs(_estimatedScore - classical_score));
			if (bonus < 0) {
				bonus = 0;
			}
			score += bonus;

			score += classical_score;
		}

		_regularScoreArea.setText(String.format("Regular points: %d", classical_score));
		_estimatedScoreArea.setText(String.format("Est. points: %d", _estimatedScore));
		_scoreArea.setText(String.format("%03d", getScore()));
		setScore(score);
	}

	public void sendPlayerInputs() {
		for (int i = 0; i < 2; i += 1) {
			Vector2 left_encoder_position = _body[i].getTransform().getTransformed(new Vector2(-_width_mm[i] / 2000.0, 0));
			Vector2 right_encoder_position = _body[i].getTransform().getTransformed(new Vector2(_width_mm[i] / 2000.0, 0));

			// calcul de l'encodeur gauche
			int left_value;
			if (_last_left_encoder_position[i] == null) {
				left_value = 0;
			} else {
				left_value = (int) (left_encoder_position.distance(_last_left_encoder_position[i]) * 10000);
				if (_body[i].getTransform().getInverseTransformed(_last_left_encoder_position[i]).y >= 0) {
					left_value = -left_value;
				}
			}

			_last_left_encoder_position[i] = left_encoder_position;

			// calcul de l'encodeur droit
			int right_value;
			if (_last_right_encoder_position[i] == null) {
				right_value = 0;
			} else {
				right_value = (int) (right_encoder_position.distance(_last_right_encoder_position[i]) * 10000);
				if (_body[i].getTransform().getInverseTransformed(_last_right_encoder_position[i]).y >= 0) {
					right_value = -right_value;
				}
			}
			_last_right_encoder_position[i] = right_encoder_position;

			// Ajout a l'intégrateur
			_total_left_value[i] += left_value;
			_total_right_value[i] += right_value;

			sendInputLine(_total_left_value[i] + " " + _total_right_value[i]);
		}
		execute();
	}
}
