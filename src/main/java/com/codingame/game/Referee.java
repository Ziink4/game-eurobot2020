package com.codingame.game;

import java.util.LinkedList;
import java.util.List;

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
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.gameengine.module.entities.Text;
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

	private World _world;
	private Text _time;
	private LinkedList<Eurobot2020Cup> _cups = new LinkedList<Eurobot2020Cup>();
	private int _elapsedTime = 0;

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

		// Affichage du fond
		displayShape(graphicEntityModule.createSprite().setImage("Background.jpg"), new Vector2(0, 2000), 0, 3);

		// ajout des murs
		createWall(-22, 0, 3044, 22);
		createWall(-22, 2022, 22, 2044);
		createWall(-22, 2022, 3044, 22);
		createWall(3000, 2022, 22, 2044);
		createWall(889, 150, 22, 172);
		createWall(1489, 300, 22, 322);
		createWall(2089, 150, 22, 172);

		// Création des robots
		for (Player p : gameManager.getPlayers()) {
			p.sendGameConfiguration();
			p.createBodies(this);
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

	private void createWall(int x0, int y0, int w, int h) {
		// Création du rectangle d'affichage
		Rectangle wall = graphicEntityModule.createRectangle();
		wall.setWidth(w).setHeight(h);
		wall.setFillColor(0xb5b0a1);
		displayShape(wall, new Vector2(x0, y0), 0, 1);

		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(((double) w) / 1000.0,
				((double) h) / 1000.0);
		BodyFixture fixtureBody = new BodyFixture(shape);
		Body body = new Body();
		body.addFixture(fixtureBody);
		body.translateToOrigin();
		body.setMass(MassType.INFINITE);
		body.translate((x0 + w / 2) / 1000.0, (y0 - h / 2) / 1000.0);

		_world.addBody(body);
	}

	@Override
	public void gameTurn(int turn) {
		// Envoi des entrées aux joueurs
		sendPlayerInputs();
		readPlayerOutputs();

		// Simulation du monde
		_world.update(FRAME_DURATION_ms / 1000.0, -1, 1000);
		_elapsedTime += FRAME_DURATION_ms;

		// Mise a jour positions joueurs
		for (Player p : gameManager.getPlayers()) {
			p.render(this);
		}

		// Mise a jour positions des verres
		for (Eurobot2020Cup c : _cups) {
			c.render(this);
		}

		// Mise a jour du texte
		_time.setText(String.format("%03d s", (int) (_elapsedTime / 1000)));

		// Détection de la fin du match
		if (_elapsedTime > DUREE_MATCH_s * 1000) {
			gameManager.endGame();
		}
	}

	private void sendPlayerInputs() {
		List<Player> allPlayers = gameManager.getPlayers();
		for (Player p : allPlayers) {
			p.sendPlayerInputs();
		}
	}

	private void readPlayerOutputs() {
		List<Player> allPlayers = gameManager.getPlayers();
		for (Player p : allPlayers) {
			try {
				p.getAction(this);
			} catch (NumberFormatException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				p.deactivate("Bad response (NumberFormatException)");
			} catch (TimeoutException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				p.deactivate("Bad response (TimeoutException)");
			} catch (ArrayIndexOutOfBoundsException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				p.deactivate("Bad response (ArrayIndexOutOfBoundsException)");
			}
		}
	}

	public void displayShape(Entity<?> shape, Vector2 position, double rotation, double generic_scale) {
		final int MARGIN_X = 100;
		final int MARGIN_Y = 100;

		// Lecture de la taille du monde
		double w = graphicEntityModule.getWorld().getWidth() - 2 * MARGIN_X;
		double h = graphicEntityModule.getWorld().getHeight() - 2 * MARGIN_Y;

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
		int offset_y = MARGIN_Y;

		shape.setScale(scale * generic_scale, Curve.NONE);
		shape.setX((int) (position.x * scale) + offset_x, Curve.LINEAR);
		shape.setY((int) ((2022 - position.y) * scale) + offset_y, Curve.LINEAR);
		shape.setRotation(rotation, Curve.LINEAR);
	}

	public World getWorld() {
		return _world;
	}

	public GraphicEntityModule getGraphicEntityModule() {
		return graphicEntityModule;
	}

	public int getElapsedTime() {
		return _elapsedTime ;
	}
}
