package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Group;

public class LightHouse implements ZObject {

	private boolean _activated;
	private Group _lighthouse;
	private BodyFixture _fixtureBodyLH;

	public LightHouse(Referee referee, int index) {
		_lighthouse = referee.getGraphicEntityModule().createGroup();
		
		_lighthouse.add(referee.getGraphicEntityModule().createSprite().setImage("LightHouse.png").setX(-22).setY(-22));
		
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(0.450, 0.222);
		BodyFixture fixtureBody = new BodyFixture(shape);
		org.dyn4j.geometry.Circle shapeHouse = new org.dyn4j.geometry.Circle(0.022);
		_fixtureBodyLH = new BodyFixture(shapeHouse);
		fixtureBody.setSensor(true);
		_fixtureBodyLH.setSensor(true);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.addFixture(_fixtureBodyLH);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.setUserData(this);

		referee.getWorld().addBody(body);

		
		
		Vector2 position = new Vector2(0, 0);
		switch(index) {
		case 0:
			position.set(450 / 2, 2122);
			body.translate(0.450 / 2, 2.0 + 0.222/2);
			break;
			
		case 1:
			position.set(3000 - 450 / 2, 2122);
			body.translate(3 - 0.450 / 2, 2.0 + 0.222/2);
			break;
		}
		
		_activated = false;
		_lighthouse.setVisible(false);
		referee.displayShape(_lighthouse, position, 0, 3);
	}

	public boolean isActivated() {
		return _activated;
	}

	public void activate() {
		_activated = true;
		_lighthouse.setVisible(true);
	}

	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		return isActivated() && (fixture == _fixtureBodyLH) && (type != SensorType.LOW);
	}

}
