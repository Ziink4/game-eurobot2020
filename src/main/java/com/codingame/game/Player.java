package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.entities.Text.FontWeight;

public class Player extends AbstractMultiplayerPlayer {
	private Body _body;
	private Group _shape;

	private double _width_mm;
	private double _height_mm;
	private Vector2 _last_left_encoder_position = null;
	private Vector2 _last_right_encoder_position = null;
	private int _total_left_value = 0;
	private int _total_right_value = 0;
	private Text _score;

	int getAction() throws NumberFormatException, TimeoutException, ArrayIndexOutOfBoundsException {
		String[] line1 = this.getOutputs().get(0).split(" ");
		double left_motor = Integer.parseInt(line1[0]);
		double right_motor = Integer.parseInt(line1[1]);

		double angularVelocity = (right_motor - left_motor) / 100.0 * 1;
		double velocity = (right_motor + left_motor) / 100.0 * 1;

		_body.setAngularVelocity(angularVelocity);
		Vector2 velocity2D = _body.getTransform().getRotation().rotate90().toVector(velocity);
		_body.setLinearVelocity(velocity2D);

		return 0;
	}

	@Override
	public int getExpectedOutputLines() {
		return 1;
	}

	public Body getBody() {
		return _body;
	}

	public void createBody(GraphicEntityModule graphicEntityModule) {
		int color;
		// Detection de la couleur
		if (getIndex() == 0) {
			color = 0x007cb0;
		} else {
			color = 0xf7b500;
		}

		// Corps pour le moteur physique
		_body = new Body();

		_width_mm = 300;
		_height_mm = 200;

		Rectangle shape = new Rectangle(_width_mm / 1000.0, _height_mm / 1000.0);

		BodyFixture fixtureBody = new BodyFixture(shape);
		fixtureBody.setDensity(20);
		fixtureBody.setRestitution(0);
		fixtureBody.setFriction(1);
		_body.addFixture(fixtureBody);
		_body.translateToOrigin();
		_body.setMass(MassType.NORMAL);
		_body.setAutoSleepingEnabled(false);

		// Dessin sur l'interface
		com.codingame.gameengine.module.entities.Rectangle rectangle = graphicEntityModule.createRectangle();
		rectangle.setWidth((int) _width_mm).setHeight((int) _height_mm);
		rectangle.setLineColor(0x000000);
		rectangle.setLineWidth(2);
		rectangle.setFillColor(color);
		rectangle.setX((int) (-_width_mm / 2));
		rectangle.setY((int) (-_height_mm / 2));

		_shape = graphicEntityModule.createGroup();
		_shape.add(rectangle);

		int offset_w = 1610;
		// Créations des textes
		_score = graphicEntityModule.createText("000").setFillColor(color).setStrokeColor(0xFFFFFF).setFontSize(128)
				.setFontWeight(FontWeight.BOLDER).setX(35 + getIndex() * offset_w ).setY(25);

	}

	public void setPosition(double x, double y, double rotation) {
		_body.rotate(rotation);
		_body.translate(x, y);
	}

	public void render(Referee referee) {
		// Récupération de la position en mètres et la rotation en radians
		Vector2 position = _body.getInitialTransform().getTranslation();
		double rotation = _body.getInitialTransform().getRotationAngle();

		// Converion en mm
		position.x *= 1000;
		position.y *= 1000;

		// Modification de la rotation car le repère de l'écran est indirect
		rotation = 0 - rotation;
		referee.displayShape(_shape, position, rotation, 1);
	}

	public void sendPlayerInputs() {
		Vector2 left_encoder_position = _body.getTransform().getTransformed(new Vector2(-_width_mm / 2000.0, 0));
		Vector2 right_encoder_position = _body.getTransform().getTransformed(new Vector2(_width_mm / 2000.0, 0));

		// calcul de l'encodeur gauche
		int left_value;
		if (_last_left_encoder_position == null) {
			left_value = 0;
		} else {
			left_value = (int) (left_encoder_position.distance(_last_left_encoder_position) * 10000);
			if (_body.getTransform().getInverseTransformed(_last_left_encoder_position).y >= 0) {
				left_value = -left_value;
			}
		}

		_last_left_encoder_position = left_encoder_position;

		// calcul de l'encodeur droit
		int right_value;
		if (_last_right_encoder_position == null) {
			right_value = 0;
		} else {
			right_value = (int) (right_encoder_position.distance(_last_right_encoder_position) * 10000);
			if (_body.getTransform().getInverseTransformed(_last_right_encoder_position).y >= 0) {
				right_value = -right_value;
			}
		}
		_last_right_encoder_position = right_encoder_position;

		// Ajout a l'intégrateur
		_total_left_value += left_value;
		_total_right_value += right_value;

		sendInputLine(_total_left_value + " " + _total_right_value);
		execute();
	}
}
