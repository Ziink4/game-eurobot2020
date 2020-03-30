package com.codingame.game;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Rectangle;

public class Wall implements ZObject {

	public Wall(Referee referee, int x0, int y0, int w, int h) {
		// Création du rectangle d'affichage
		Rectangle wall = referee.getGraphicEntityModule().createRectangle();
		wall.setWidth(w).setHeight(h);
		wall.setFillColor(0xb5b0a1);
		referee.displayShape(wall, new Vector2(x0, y0), 0, 1);

		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(((double) w) / 1000.0,
				((double) h) / 1000.0);
		BodyFixture fixtureBody = new BodyFixture(shape);
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
