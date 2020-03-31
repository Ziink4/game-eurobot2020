package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;

public class CompassArea implements ZObject {

	public CompassArea(Referee referee) {
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(0.3, 0.026);
		BodyFixture fixtureBody = new BodyFixture(shape);
		fixtureBody.setSensor(true);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.translate(1.5, 2.0-0.026/2);
		body.setBullet(true);
		body.setUserData(this);

		referee.getWorld().addBody(body);

	}

	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {

		return false;
	}
}
