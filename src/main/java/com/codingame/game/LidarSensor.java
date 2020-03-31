package com.codingame.game;

import java.util.LinkedList;
import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.Line;

public class LidarSensor {
	public static boolean DEBUG = false;
	private Line[] _debugLines = null;
	private int _id;
	private double _max_distance;
	private double[] _distances = new double[360];
	private static int ID = 0;

	public LidarSensor(Referee referee, Player player, int robot, double max_distance) {
		_id = ID;
		ID += 1;
		if (_debugLines == null && DEBUG) {
			_debugLines = new Line[360];
			for (int i = 0; i < 360; i += 1) {
				_debugLines[i] = referee.getGraphicEntityModule().createLine();
				_debugLines[i].setLineWidth(8).setLineAlpha(0.4).setLineColor(0xFF00FF);
			}
		}
		_max_distance = max_distance;
	}

	private void updateDrawing(Referee referee, Body body) {
		if (_id != 0 || !DEBUG)
			return;

		Vector2 pt = body.getWorldPoint(new Vector2(0, 0));
		Curve curve;
		pt.multiply(1000);

		for (int i = 0; i < 360; i += 1) {

			_debugLines[i].setX((int) (0));
			_debugLines[i].setY((int) (0));
			_debugLines[i].setX2((int) (0));
			_debugLines[i].setY2((int) (-getValue(i)));
			curve = Curve.LINEAR;
			referee.displayLine(_debugLines[i], pt, -body.getTransform().getRotationAngle() - (i * Math.PI) / 180.0,
					curve);
		}
	}

	public int getValue(int angle) {
		angle %= 360;
		return (int) (_distances[angle] * 1000.0);
	}

	public void compute(Referee referee, Body body) {
		Vector2 direction = body.getWorldVector(new Vector2(0, 1));
		Ray ray = new Ray(body.getWorldPoint(new Vector2(0, 0)), direction);

		for (int i = 0; i < 360; i += 1) {
			List<RaycastResult> results = new LinkedList<RaycastResult>();

			for (double a = -0.33; a <= 0.33; a += 0.33) {
				Vector2 ndir = new Vector2(direction).rotate((((double) i) + a) * Math.PI / 180.0);
				Ray nray = new Ray(ray.getStart(), ndir);

				referee.getWorld().raycast(nray, _max_distance, false, true, results);
			}

			_distances[i] = _max_distance;
			for (RaycastResult r : results) {
				if (!validBody(body, r.getBody(), r.getFixture())) {
					continue;
				}
				double d = r.getRaycast().getDistance();
				if (d < _distances[i]) {
					_distances[i] = d;
				}
			}
		}
		updateDrawing(referee, body);
	}

	private boolean validBody(Body self, Body tested, BodyFixture fixture) {
		if (self == tested) {
			return false;
		}

		if (tested.getUserData() == null) {
			System.out.println("No userdata ?????");
			return false;
		} else {
			ZObject obj = (ZObject) tested.getUserData();

			if (obj.isVisibleBySensor(fixture, SensorType.VERY_HIGH)) {
				return true;
			} else {
				return false;
			}
		}
	}
}
