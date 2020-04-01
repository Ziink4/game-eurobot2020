package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;


/**
 * @brief	Helper class of compass orientation detection by players
 */
public class CompassArea implements ZObject {

	/**
	 * @brief	Create a new area for compass detection
	 * @param	referee		Valid instance to the referee
	 */
	public CompassArea(Referee referee) {
		// create the body for the physics engine
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(0.3, 0.026);
		BodyFixture fixtureBody = new BodyFixture(shape);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.translate(1.5, 2.0 - 0.026 / 2);
		body.setBullet(true);
		body.setUserData(this);

		// add the body to the world
		referee.getWorld().addBody(body);
	}

	/**
	 * @param	fixture Instance of the fixture to be tested
	 * @param	type    Type of sensor which try to detected the fixture
	 */
	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		// the compass in invisible for all sensor
		return false;
	}
}
