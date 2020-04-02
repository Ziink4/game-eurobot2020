package com.codingame.game;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.dyn4j.collision.broadphase.BroadphaseDetector;
import org.dyn4j.collision.broadphase.Sap;
import org.dyn4j.collision.narrowphase.Sat;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.Entity;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Group;
import com.codingame.gameengine.module.entities.Line;
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.gameengine.module.entities.Sprite;
import com.codingame.gameengine.module.entities.Text;
import com.codingame.gameengine.module.toggle.ToggleModule;
import com.codingame.view.AnimatedEventModule;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {

	private static double FRAME_DURATION_ms = 350.0;
	private static int DUREE_MATCH_s = 100;

	@Inject
	private MultiplayerGameManager<Player> gameManager;
	@Inject
	private GraphicEntityModule graphicEntityModule;
	@Inject
	private AnimatedEventModule animatedEventModule;
	@Inject
	ToggleModule toggleModule;

	private World _world;
	private Text _time;
	private LinkedList<Eurobot2020Cup> _cups = new LinkedList<Eurobot2020Cup>();
	private int _elapsedTime = 0;
	private Group _compass;
	private boolean _compassIsNorth;
	private double _compassEndRotation;

	@Override
	public void init() {
		// Configuration du moteur de jeu
		gameManager.setFrameDuration((int) FRAME_DURATION_ms);
		gameManager.setMaxTurns(5000);
		gameManager.setTurnMaxTime(50);

		// Configuration du moteur physique
		_world = new World();
		// _world.getSettings().setStepFrequency(FRAME_DURATION_ms / 1000);
		_world.setGravity(World.ZERO_GRAVITY);
		//_world.setBroadphaseDetector(new Sap<Body, BodyFixture>());
		//_world.setNarrowphaseDetector(new Sat());

		// Display compass
		_compass = graphicEntityModule.createGroup(graphicEntityModule.createSprite().setImage("Compass.png")
				.setScale(0.25).setX(-150 / 4).setY(-150 / 4));

		_compass.setX(graphicEntityModule.getWorld().getWidth() / 2).setY(150);

		_compassIsNorth = (Math.random() < 0.5);
		_compassEndRotation = 15000 + Math.random() * 10000;
		//System.out.println("COMPASS: " + (_compassIsNorth ? "N" : "S") + " - " + _compassEndRotation + "ms");

		// Affichage du fond
		displayShape(graphicEntityModule.createSprite().setImage("Table.png"), new Vector2(0 - 156, 2000 + 222), 0, 3);

		// ajout des murs
		new Wall(this, -22, 0, 3044, 22);
		new Wall(this, -22, 2022, 22, 2044);
		new Wall(this, -22, 2022, 3044, 22);
		new Wall(this, 3000, 2022, 22, 2044);
		new Wall(this, 889, 150, 22, 172);
		new Wall(this, 1489, 300, 22, 322);
		new Wall(this, 2089, 150, 22, 172);
		new FixedBeacon(this, -50 - 22, 2000 - 50);
		new FixedBeacon(this, -50 - 22, 2000 - 1000);
		new FixedBeacon(this, -50 - 22, 50);
		new FixedBeacon(this, 3000 + 50 + 22, 2000 - 50);
		new FixedBeacon(this, 3000 + 50 + 22, 2000 - 1000);
		new FixedBeacon(this, 3000 + 50 + 22, 50);
		
		new CompassArea(this);

		// Création des robots
		for (Player p : gameManager.getPlayers()) {
			p.createBodies(this);
			p.sendGameConfiguration();
		}

		// Ajout des verres sur la table
		_cups.add(new Eurobot2020Cup(this, 0.67, 2 - 0.1, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 2.33, 2 - 0.1, Eurobot2020CupType.GREEN));

		_cups.add(new Eurobot2020Cup(this, 0.30, 2 - 0.4, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 0.95, 2 - 0.4, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 2.05, 2 - 0.4, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 2.70, 2 - 0.4, Eurobot2020CupType.GREEN));

		_cups.add(new Eurobot2020Cup(this, 0.45, 2 - 0.51, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 2.55, 2 - 0.51, Eurobot2020CupType.RED));

		_cups.add(new Eurobot2020Cup(this, 1.10, 2 - 0.8, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 1.90, 2 - 0.8, Eurobot2020CupType.GREEN));

		_cups.add(new Eurobot2020Cup(this, 0.45, 2 - 1.08, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 2.55, 2 - 1.08, Eurobot2020CupType.GREEN));

		_cups.add(new Eurobot2020Cup(this, 0.30, 2 - 1.2, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 0.95, 2 - 1.2, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 2.05, 2 - 1.2, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 2.70, 2 - 1.2, Eurobot2020CupType.RED));

		_cups.add(new Eurobot2020Cup(this, 1.065, 2 - 1.65, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 1.335, 2 - 1.65, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 1.665, 2 - 1.65, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 1.935, 2 - 1.65, Eurobot2020CupType.RED));

		_cups.add(new Eurobot2020Cup(this, 1.005, 2 - 1.955, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 1.395, 2 - 1.955, Eurobot2020CupType.GREEN));
		_cups.add(new Eurobot2020Cup(this, 1.605, 2 - 1.955, Eurobot2020CupType.RED));
		_cups.add(new Eurobot2020Cup(this, 1.995, 2 - 1.955, Eurobot2020CupType.GREEN));

		for (int i = 0; i < 2; i += 1) {
			for (int j = 0; j < 5; j += 1) {
				Eurobot2020CupType color = Eurobot2020CupType.GREEN;
				if (((i + j) % 2) == 0) {
					color = Eurobot2020CupType.RED;
				}
				_cups.add(new Eurobot2020Cup(this, -0.067 + i * (0.067 * 2 + 3.0), 2 - 1.450 - j * 0.075, color));
			}
		}

		int config = new Random().nextInt(3);
		for (int j = 0; j < 5; j += 1) {
			Eurobot2020CupType color = Eurobot2020CupType.GREEN;
			Eurobot2020CupType ncolor = Eurobot2020CupType.GREEN;

			switch (config) {
			case 0:
				if ((j == 1) || (j == 4)) {
					color = Eurobot2020CupType.RED;
				}
				break;
			case 1:
				if ((j == 2) || (j == 4)) {
					color = Eurobot2020CupType.RED;
				}
				break;
			case 2:
				if ((j == 3) || (j == 4)) {
					color = Eurobot2020CupType.RED;
				}
				break;
			}

			if (color == Eurobot2020CupType.GREEN) {
				ncolor = Eurobot2020CupType.RED;
			}

			_cups.add(new Eurobot2020Cup(this, 0.7 + j * 0.075, 2 + 0.067, color));
			_cups.add(new Eurobot2020Cup(this, 3 - 0.7 - j * 0.075, 2 + 0.067, ncolor));

		}

		// Simulation initial du monde
		_world.update(FRAME_DURATION_ms / 1000.0, -1, 1000);

		// Mise a jour positions joueurs
		for (Player p : gameManager.getPlayers()) {
			p.render(this);
		}

		// Mise a jour positions des verres
		for (Eurobot2020Cup c : _cups) {
			c.render(this);
		}

		// Affichage du temps
		_time = graphicEntityModule.createText("000 s").setFillColor(0xb5b0a1).setStrokeColor(0xFFFFFF).setFontSize(64)
				.setX(graphicEntityModule.getWorld().getWidth() / 2 - 60).setY(10);
	}

	@Override
	public void gameTurn(int turn) {
		_elapsedTime += FRAME_DURATION_ms;

		// Simulation du monde
		double delta_t = FRAME_DURATION_ms / 1000.0;
		for (Player p : gameManager.getPlayers()) {
			p.updateSetpoints();
		}
		while (_world.update(delta_t, -1, 1)) {
			delta_t = 0;
			for (Player p : gameManager.getPlayers()) {
				p.updateSetpoints();
				p.compute();
			}
		}

		if (Double.isNaN(gameManager.getPlayer(1).getBodies()[1].getTransform().getTranslationX())) {
			gameManager.endGame();
		}

		// set compass
		if (compassRotationEnded()) {
			if (_compassIsNorth) {
				_compass.setRotation(0);
			} else {
				_compass.setRotation(Math.PI);
			}
		} else {
			_compass.setRotation(_elapsedTime);
		}

		// Mise a jour positions joueurs
		for (Player p : gameManager.getPlayers()) {
			p.compute();
			p.render(this);
		}

		// Mise a jour positions des verres
		for (Eurobot2020Cup c : _cups) {
			c.render(this);
		}

		// Mise a jour du texte
		_time.setText(String.format("%03d s", (int) (_elapsedTime / 1000)));

		// Envoi des entrées aux joueurs
		sendPlayerInputs();
		readPlayerOutputs();

		// Détection de la fin du match
		if (_elapsedTime > DUREE_MATCH_s * 1000) {
			gameManager.endGame();
		}
	}

	private boolean compassRotationEnded() {
		return _elapsedTime >= _compassEndRotation;
	}

	private void sendPlayerInputs() {
		for (Player p : gameManager.getActivePlayers()) {
			p.sendPlayerInputs(this);
		}
		for (Player p : gameManager.getActivePlayers()) {
			p.sendPlayerIRSensors();
		}
		for (Player p : gameManager.getActivePlayers()) {
			p.sendPlayerLidarSensors();
			p.execute();
		}
	}

	private void readPlayerOutputs() {
		for (Player p : gameManager.getActivePlayers()) {
			try {
				p.getAction(this);
			} catch (NumberFormatException e) {
				p.deactivateAndReset(this, "Bad response (NumberFormatException)");
			} catch (TimeoutException e) {
				p.deactivateAndReset(this, "Bad response (TimeoutException)");
			} catch (ArrayIndexOutOfBoundsException e) {
				p.deactivateAndReset(this, "Bad response (ArrayIndexOutOfBoundsException)");
			}
		}
	}

	public void displayShape(Entity<?> shape, Vector2 position, double rotation, double generic_scale) {
		final int MARGIN_X = 100;
		final int MARGIN_Y_TOP = 190;
		final int MARGIN_Y_BOT = 10;

		// Lecture de la taille du monde
		double w = graphicEntityModule.getWorld().getWidth() - 2 * MARGIN_X;
		double h = graphicEntityModule.getWorld().getHeight() - MARGIN_Y_TOP - MARGIN_Y_BOT;

		// calcul de l'echelle et des offsets en position
		double scale_w = w / (3000.0 + 22.0 + 22.0);
		double scale_h = h / (2022.0 + 22.0 + 22.0);
		double scale;
		if (scale_w > scale_h) {
			scale = scale_h;
		} else {
			scale = scale_w;
		}

		// calcul pour que le centre soit bien au centre
		int offset_x = (int) (graphicEntityModule.getWorld().getWidth() / 2 - 1500 * scale);
		int offset_y = MARGIN_Y_TOP;

		shape.setScale(scale * generic_scale, Curve.NONE);
		shape.setX((int) (position.x * scale) + offset_x, Curve.LINEAR);
		shape.setY((int) ((2022 - position.y) * scale) + offset_y, Curve.LINEAR);
		shape.setRotation(rotation, Curve.LINEAR);
	}

	public void displayLine(Line line, Vector2 position, double rotation, Curve curve) {
		final int MARGIN_X = 100;
		final int MARGIN_Y_TOP = 190;
		final int MARGIN_Y_BOT = 10;

		// Lecture de la taille du monde
		double w = graphicEntityModule.getWorld().getWidth() - 2 * MARGIN_X;
		double h = graphicEntityModule.getWorld().getHeight() - MARGIN_Y_TOP - MARGIN_Y_BOT;

		// calcul de l'echelle et des offsets en position
		double scale_w = w / (3000.0 + 22.0 + 22.0);
		double scale_h = h / (2022.0 + 22.0 + 22.0);
		double scale;
		if (scale_w > scale_h) {
			scale = scale_h;
		} else {
			scale = scale_w;
		}

		// calcul pour que le centre soit bien au centre
		int offset_x = (int) (graphicEntityModule.getWorld().getWidth() / 2 - 1500 * scale);
		int offset_y = MARGIN_Y_TOP;

		int oldx = line.getX();
		int oldy = line.getY();

		line.setScale(scale, Curve.NONE);
		line.setX((int) (position.x * scale) + offset_x, Curve.LINEAR);
		line.setY((int) ((2022 - position.y) * scale) + offset_y, Curve.LINEAR);

		line.setX2(line.getX2() + (line.getX() - oldx), curve);
		line.setY2((line.getY2() + (line.getY() - oldy)), curve);
		line.setRotation(rotation, Curve.LINEAR);
	}

	public World getWorld() {
		return _world;
	}

	public GraphicEntityModule getGraphicEntityModule() {
		return graphicEntityModule;
	}

	public int getElapsedTime() {
		return _elapsedTime;
	}

	public ToggleModule getToggleModule() {
		return toggleModule;
	}

	public boolean compassIsNorth() {
		int rotation = (int) (_compass.getRotation() * 180.0 / Math.PI);
		rotation %= 360;
		return (rotation < 90) || (rotation >= 270);
	}
}
