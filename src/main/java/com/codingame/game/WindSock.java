package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Group;

public class WindSock implements ZObject {

	private boolean _activated;
	private Group _windsock;
	private Group _windsockActivated;

	public WindSock(Referee referee, int index, int i) {
		_windsock = referee.getGraphicEntityModule().createGroup();	
		_windsockActivated = referee.getGraphicEntityModule().createGroup();
		switch(index) {
		case 0:
			_windsock.add(referee.getGraphicEntityModule().createRectangle().setWidth(165).setHeight(22).setX(-65).setFillColor(0xFFFFFF));
			break;
			
		case 1:
			_windsock.add(referee.getGraphicEntityModule().createRectangle().setWidth(165).setHeight(22).setX(-100).setFillColor(0xFFFFFF));
			break;
		}
		
		
		
		
		for(int k = 0; k < 165; k += 2*165/5) {
			_windsockActivated.add(referee.getGraphicEntityModule().createRectangle().setWidth(33).setHeight(22).setX(-65 + k).setFillColor(0xFF0000));
		}
		_windsockActivated.setAlpha(0);
			
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(0.165, 0.022);
		BodyFixture fixtureBody = new BodyFixture(shape);		
		fixtureBody.setSensor(true);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.setUserData(this);

		referee.getWorld().addBody(body);

		_activated = false;

		Vector2 position = new Vector2(0, 0);
		switch(index) {
		case 0:
			position.set(230 + i * 405, -22);
			body.translate(0.230 + i * 0.405 - 0.065 + 0.165/2, -0.022 - 0.011);
			break;
			
		case 1:
			position.set(3000 - 230 - i * 405, -22);
			body.translate(3.0 - (0.230 + i * 0.405 - 0.065 + 0.165/2), -0.022 - 0.011);
			break;
		}
		
		referee.displayShape(_windsock, position, 0, 1);
		referee.displayShape(_windsockActivated, position, 0, 1);
	}

	public boolean isActivated() {
		return _activated;
	}

	public void activate() {
		_activated = true;
		_windsockActivated.setAlpha(1.0);
	}

	@Override
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type) {
		return isActivated() && (type != SensorType.VERY_HIGH);
	}

}
