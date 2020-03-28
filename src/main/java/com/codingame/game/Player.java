package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractMultiplayerPlayer;
import com.codingame.gameengine.module.entities.BufferedGroup;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;

public class Player extends AbstractMultiplayerPlayer {
    private Body _body;
	private Group _shape;
	
	private double _width_mm;
	private double _height_mm;

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
		//Corps pour le moteur physique
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
		
		//Dessin sur l'interface
		com.codingame.gameengine.module.entities.Rectangle rectangle = graphicEntityModule.createRectangle();
		rectangle.setWidth((int) _width_mm).setHeight((int) _height_mm);
		rectangle.setLineColor(0x000000);
		rectangle.setLineWidth(2);
		if(getIndex() == 0)
		{
			rectangle.setFillColor(0x007cb0);
		}
		else
		{
			rectangle.setFillColor(0xf7b500);	
		}
		rectangle.setX((int) (-_width_mm / 2));
		rectangle.setY((int) (-_height_mm / 2));
		
		_shape = graphicEntityModule.createGroup();
		_shape.add(rectangle);
	}

	public void setPosition(double x, double y) {
		_body.translate(x, y);
	}

	public void render(Referee referee) {
		//Récupération de la position en mètres et la rotation en radians
		Vector2 position = _body.getInitialTransform().getTranslation();
		double rotation = _body.getInitialTransform().getRotationAngle();
		/**
		 * @todo transformation coordonnées du centre vers coord du point
		 *  	 haut gauche en prenant en compte la rotation
		 **/
		//fonctionne uniquement avec rotation nulle
		//position.x -= _width_mm / 2000.0;
		//position.y += _height_mm / 2000.0;
		
		//Converion en mm
		position.x *= 1000;
		position.y *= 1000;
		//Modification de la rotation car le repère de l'écran est indirect
		rotation = 0 - rotation;
		referee.displayShape(_shape, position, rotation, 1);
	}
}
