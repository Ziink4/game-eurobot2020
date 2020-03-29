package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Circle;
import com.codingame.gameengine.module.entities.Group;

public class Eurobot2020Cup {

	private Group _shape;
	private Body _body;
	private Eurobot2020CupType _type;

	public Eurobot2020Cup(Referee referee, double x, double y, Eurobot2020CupType type) {
		
		_type = type;
		
		// Corps pour le moteur physique
		_body = new Body();
	
		org.dyn4j.geometry.Circle shape = new org.dyn4j.geometry.Circle(0.072/2);
		BodyFixture fixtureBody = new BodyFixture(shape);
		fixtureBody.setDensity(2);
		fixtureBody.setRestitution(0.1);
		fixtureBody.setFriction(0.8);
		_body.addFixture(fixtureBody);
		_body.translateToOrigin();
		_body.setMass(MassType.NORMAL);
		_body.setAutoSleepingEnabled(false);
		_body.translate(x, y);
		_body.setUserData(this);
		_body.setLinearDamping(500);
		referee.getWorld().addBody(_body);
		
		// Dessin sur l'interface
		Circle circle = referee.getGraphicEntityModule().createCircle();
		circle.setRadius(72/2);
		circle.setLineColor(0x000000);
		circle.setLineWidth(16);
		switch(type)
		{
		case GREEN:
			circle.setFillColor(0x006f3d);
			break;
		case RED:
			circle.setFillColor(0xbb1e10);
			break;
		}
		

		_shape = referee.getGraphicEntityModule().createGroup();
		_shape.add(circle);
	}

	public void render(Referee referee) {
		// Récupération de la position en mètres
		Vector2 position = _body.getTransform().getTranslation();

		// Converion en mm
		position.x *= 1000;
		position.y *= 1000;

		referee.displayShape(_shape, position, 0, 1);
	}

	public Eurobot2020CupType getType() {
		return _type;
	}

	public void removeFromTable(Referee referee, double x, double y) {
		referee.getWorld().removeBody(_body);
		_body.translate(_body.getTransform().getTranslation().negate().add(x, y));
		_body.setAngularVelocity(0);
		_body.setLinearVelocity(new Vector2(0, 0));
	}

	public void addToTable(Referee referee, Vector2 position) {
		_body.translate(_body.getTransform().getTranslation().negate().add(position));
		_body.setAngularVelocity(0);
		_body.setLinearVelocity(new Vector2(0, 0));
		referee.getWorld().addBody(_body);
	}

}
