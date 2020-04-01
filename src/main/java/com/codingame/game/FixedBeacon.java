package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;

public class FixedBeacon implements ZObject {

	public FixedBeacon(Referee referee, int x, int y) {
		// create the body for the physics engine
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(0.1, 0.1);
		BodyFixture fixtureBody = new BodyFixture(shape);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.translate(x / 1000.0, y / 1000.0);
		body.setBullet(true);
		body.setUserData(this);

		// add the body to the world
		referee.getWorld().addBody(body);
	}

	/**
	 * @param fixture Instance of the fixture to be tested
	 * @param type    Type of sensor which try to detected the fixture
	 */
	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		return true;
	}
}
