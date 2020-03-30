package com.codingame.game;

import org.dyn4j.dynamics.BodyFixture;

public interface ZObject {
	public boolean isVisibleBySensor(BodyFixture fixture, SensorType type);
}
