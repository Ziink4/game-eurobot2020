package com.codingame.game;

import java.util.LinkedList;
import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.dynamics.RaycastResult;
import org.dyn4j.geometry.AABB;
import org.dyn4j.geometry.Ray;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.Line;

public class IRSensor {
	private Line _debugLine;

	private Vector2 _local_position;
	private double _max_distance;
	private double _distance;

	private double _rotation;

	private SensorType _type;

	private static int ID = 0;
	
	public static int hsvToRgb(double d, double e, double g) {

	    int h = (int)(d * 6) % 6;
	    double f = d * 6 - h;
	    double p = g * (1 - e);
	    double q = g * (1 - f * e);
	    double t = g * (1 - (1 - f) * e);

	    switch (h) {
	      case 0: return rgbToInt(g, t, p);
	      case 1: return rgbToInt(q, g, p);
	      case 2: return rgbToInt(p, g, t);
	      case 3: return rgbToInt(p, q, g);
	      case 4: return rgbToInt(t, p, g);
	      case 5: return rgbToInt(g, p, q);
	      default: throw new RuntimeException("Something went wrong when converting from HSV to RGB. Input was " + d + ", " + e + ", " + g);
	    }
	}

	private static int rgbToInt(double r, double g, double b) {
		int ri = (int) (255 * r);
		int bi = (int) (255 * g);
		int gi = (int) (255 * b);
		int value = (ri << 16) + (bi << 8) + gi;
		
		if(value > 0xFFFFFF) {
			value = 0xFFFFFF;
		}
		return value;
	}

	public IRSensor(Referee referee, SensorType type, Player player, int robot, double rotation, double max_distance,
			Vector2 local_position) {

		_debugLine = referee.getGraphicEntityModule().createLine();
		_debugLine.setLineWidth(32).setLineAlpha(0.7).setLineColor(hsvToRgb(Math.cos(ID) * 0.5 + 0.5,0.5,0.5));
		ID += 1;
		_debugLine.setVisible(false);

		_distance = max_distance;
		_max_distance = max_distance;
		_local_position = local_position;
		_rotation = rotation * Math.PI / 180.0;
		_type = type;
		
		switch(type) {
			case LOW:
				referee.getToggleModule().displayOnToggleState(_debugLine, "showLowSensor", true);
				break;
				
			case HIGH:
				referee.getToggleModule().displayOnToggleState(_debugLine, "showHighSensor", true);
				break;
		}
	}

	private void updateDrawing(Referee referee, Body body) {
		Vector2 pt = body.getWorldPoint(_local_position);
		
		if (_distance > 0) {
			_debugLine.setVisible(false);

			
			pt.multiply(1000);
			_debugLine.setX((int) (0));
			_debugLine.setY((int) (0));
			_debugLine.setX2((int) (0));
			_debugLine.setY2((int) (-_distance * 1000));
						
			_debugLine.setVisible(true);
			
		} else {
			_debugLine.setVisible(false);
		}
		
		referee.displayLine(_debugLine, pt, -body.getTransform().getRotationAngle() - _rotation, Curve.LINEAR);
	}

	public void compute(Referee referee, Body body) {

		// First check if body is intersected
		AABB bb = new AABB(body.getWorldPoint(_local_position), 0.01);

		List<DetectResult> bbres = new LinkedList<DetectResult>();
		referee.getWorld().detect(bb, bbres);

		_distance = _max_distance;

		for (DetectResult r : bbres) {
			if (validBody(body, r.getBody())) {
				_distance = 0;
			}
		}

		if (_distance != 0) {
			Vector2 direction = body.getWorldVector(new Vector2(0, 1).rotate(_rotation));
			Ray ray = new Ray(body.getWorldPoint(_local_position), direction);
			List<RaycastResult> results = new LinkedList<RaycastResult>();
			
			for(double a = -5; a <= 5; a += 0.5) {
				Vector2 ndir = new Vector2(direction).rotate(a * Math.PI / 180.0);
				Ray nray = new Ray(ray.getStart(), ndir);
			
				referee.getWorld().raycast(nray, _max_distance, false, true, results);
			}

			for (RaycastResult r : results) {
				if (!validBody(body, r.getBody())) {
					continue;
				}
				double d = r.getRaycast().getDistance();
				if (d < _distance) {
					_distance = d;
				}
			}
		}

		updateDrawing(referee, body);
	}

	private boolean validBody(Body self, Body tested) {
		if(self == tested) {
			return false;
		}
		
		if(_type == SensorType.LOW) {
			return true;
		}
		else {
			Object userdata = tested.getUserData();
			
			if(userdata == null) {
				return false;
			}
			else if(userdata instanceof Player) {
				return true;
			}
			else {
				return false;
			}
		}
	}

	public int getDistance() {
		return (int) (_distance * 1000.0);
	}

}
