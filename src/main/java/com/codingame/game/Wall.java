package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;

public class Wall implements ZObject {

	public Wall(Referee referee, int x0, int y0, int w, int h) {
		
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(((double) w) / 1000.0,
				((double) h) / 1000.0);
		BodyFixture fixtureBody = new BodyFixture(shape);
		fixtureBody.setRestitution(0);
		fixtureBody.setFriction(2000);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.translate((x0 + w / 2) / 1000.0, (y0 - h / 2) / 1000.0);
		body.setBullet(true);
		body.setUserData(this);
		
		referee.getWorld().addBody(body);
	}

	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		switch(type) {
		case LOW:
			return true;
		case HIGH:
			return false;
		case VERY_HIGH:
			return false;
		}
		
		return false;
	}

}
