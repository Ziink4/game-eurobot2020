package com.codingame.game;

import java.util.List;

import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Vector2;

import com.codingame.gameengine.core.AbstractPlayer.TimeoutException;
import com.codingame.gameengine.core.AbstractReferee;
import com.codingame.gameengine.core.MultiplayerGameManager;
import com.codingame.gameengine.core.Tooltip;
import com.codingame.gameengine.module.entities.Circle;
import com.codingame.gameengine.module.entities.Curve;
import com.codingame.gameengine.module.entities.Entity;
import com.codingame.gameengine.module.entities.GraphicEntityModule;
import com.codingame.gameengine.module.entities.Rectangle;
import com.codingame.view.AnimatedEventModule;
import com.codingame.view.ViewerEvent;
import com.google.inject.Inject;

public class Referee extends AbstractReferee {
	
	private static double FRAME_DURATION_ms = 350.0;
	private static int DUREE_MATCH_s = 100;

	@Inject private MultiplayerGameManager<Player> gameManager;
	@Inject private GraphicEntityModule graphicEntityModule;
	@Inject private AnimatedEventModule animatedEventModule;
	
	private World _world;
	
	@Override
	public void init() {		
		// Configuration du moteur de jeu
		gameManager.setFrameDuration((int) FRAME_DURATION_ms);
		gameManager.setMaxTurns(5000);
		gameManager.setTurnMaxTime(50);
				
		//Configuration du moteur physique
		_world = new World();
		//_world.getSettings().setStepFrequency(FRAME_DURATION_ms / 1000);
		_world.setGravity(World.ZERO_GRAVITY);
        
        //Affichage du fond
        displayShape(
        		graphicEntityModule.createSprite().setImage("Background.jpg"),
        		new Vector2(0, 2000),
        		0,
        		3);
        
        //ajout des murs
        createWall(-22, 0, 3044, 22);
        createWall(-22, 2022, 22, 2044);
        createWall(-22, 2022, 3044, 22);
        createWall(3000, 2022, 22, 2044);
        createWall(889, 150, 22, 172);
        createWall(1489, 300, 22, 322);
        createWall(2089, 150, 22, 172);
        
        
        //Création des robots
		for (Player p : gameManager.getActivePlayers()) {
			p.createBody(graphicEntityModule);
			p.setPosition(0.50 + 1.1 * p.getIndex(), 1.000);
			_world.addBody(p.getBody());
	      }
		
		//Simulation initial du monde
      _world.update(FRAME_DURATION_ms / 1000.0, -1, 1000);
      
        //Mise a jour positions joueurs
	    for (Player p : gameManager.getActivePlayers()) {
	    	p.render(this);
	    }
	}

	private void createWall(int x0, int y0, int w, int h) {
		//Création du rectangle d'affichage
		Rectangle wall = graphicEntityModule.createRectangle();
		wall.setWidth(w).setHeight(h);
		wall.setFillColor(0xb5b0a1);
		displayShape(wall, new Vector2(x0, y0), 0, 1);
		
		org.dyn4j.geometry.Rectangle shape = new org.dyn4j.geometry.Rectangle(((double) w) / 1000.0, ((double) h) / 1000.0);
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

        //Simulation du monde
        _world.update(FRAME_DURATION_ms / 1000.0, -1, 1000);
        
        //Mise a jour positions joueurs
        for (Player p : gameManager.getActivePlayers()) {
        	p.render(this);
        }
       
        //Détection de la fin du match
        if (turn > DUREE_MATCH_s * 1000 / FRAME_DURATION_ms) {
            gameManager.endGame();
        }
	}
	
	  private void sendPlayerInputs() {
	        List<Player> allPlayers = gameManager.getPlayers();
	        for (Player p : allPlayers) {
	            p.sendInputLine(String.valueOf(0) + " " + String.valueOf(0));
	            p.execute();
	        }
	    }
	  
	  private void readPlayerOutputs() {
	        List<Player> allPlayers = gameManager.getPlayers();
	        for (Player p : allPlayers) {
	            try {
					p.getAction();
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					p.deactivate("Bad response (NumberFormatException)");
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					p.deactivate("Bad response (TimeoutException)");
				}
	            catch (ArrayIndexOutOfBoundsException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					p.deactivate("Bad response (ArrayIndexOutOfBoundsException)");
				}
	        }
	    }

	public void displayShape(Entity<?> shape, Vector2 position, double rotation, double generic_scale) {
		final int MARGIN_X = 100;
		final int MARGIN_Y = 100;
		
		//Lecture de la taille du monde
		double w = graphicEntityModule.getWorld().getWidth() - 2 * MARGIN_X;
		double h = graphicEntityModule.getWorld().getHeight() - 2 * MARGIN_Y;
		
		//calcul de l'echelle et des offsets en position
		double scale_w = w / (3000.0 + 22.0 + 22.0);
		double scale_h = h / (2022.0 + 22.0 + 22.0);
		double scale;
		if(scale_w > scale_h)
		{
			scale = scale_h;			
		}
		else
		{
			scale = scale_w;
		}
		
		//calcul pour que le centre soit bien au centre
		int offset_x = (int) (graphicEntityModule.getWorld().getWidth() / 2 - 1500 * scale);
		int offset_y = MARGIN_Y;
		
		shape.setScale(scale * generic_scale, Curve.NONE);
		shape.setX((int) (position.x * scale) + offset_x, Curve.LINEAR);
		shape.setY((int) ((2022 - position.y) * scale) + offset_y, Curve.LINEAR);		
		shape.setRotation(rotation, Curve.LINEAR);
	}
	
	/*
    private static int WIDTH = 1920;
    private static int HEIGHT = 1080;
    private static int BALL_RADIUS = 20;
    private static int PADDLE_WIDTH = 15;
    private static int PADDLE_HEIGHT = 150;

    
    
    

    private int ballX, ballY;
    private int ballVX, ballVY;
    private Circle ball;

    private int clamp(int val, int min, int max) {
        return Math.max(min, Math.min(max, val));
    }

  

    private double min(double... values) {
        double m = values[0];
        for (double v : values) {
            m = Math.min(m, v);
        }
        return m;
    }

    private void moveBall() {
        double t = 0;
        while (t < 1) {
            double timeCollisionTop = ballVY < 0 ? (BALL_RADIUS - ballY) / (double) ballVY : 1;
            double timeCollisionBottom = ballVY > 0 ? (HEIGHT - BALL_RADIUS - ballY) / (double) ballVY : 1;
            double timeCollisionLeft = ballVX < 0 ? (BALL_RADIUS + PADDLE_WIDTH - ballX) / (double) ballVX : 1;
            double timeCollisionRight = ballVX > 0 ? (WIDTH - PADDLE_WIDTH - BALL_RADIUS - ballX) / (double) ballVX : 1;

            if (ballX <= BALL_RADIUS + PADDLE_WIDTH && gameManager.getPlayer(0).lost) {
                timeCollisionLeft = 1;
            }
            if (ballX >= WIDTH - BALL_RADIUS - PADDLE_WIDTH && gameManager.getPlayer(1).lost) {
                timeCollisionRight = 1;
            }

            double delta = min(timeCollisionTop, timeCollisionBottom, timeCollisionLeft, timeCollisionRight, 1 - t);
            t += delta;

            ballX += ballVX * delta;
            ballY += ballVY * delta;

            if (ballVY < 0 && ballY <= BALL_RADIUS || ballVY > 0 && this.ballY >= HEIGHT - BALL_RADIUS) {
                this.ballVY *= -1;
            }

            if (ballVX < 0 && ballX <= BALL_RADIUS + PADDLE_WIDTH) {
                Player p = gameManager.getPlayer(0);
                double paddleY = (p.previousY * (1 - t)) + p.y * t;
                if (ballY > paddleY - PADDLE_HEIGHT / 2 && ballY < paddleY + PADDLE_HEIGHT / 2) {
                    ballVX *= -1;

                    gameManager.addTooltip(new Tooltip(p.getIndex(), "Ping"));
                    
                    ViewerEvent ev = animatedEventModule.createAnimationEvent("Ping", t);
                    ev.params.put("player", 0);
                    ev.params.put("x", ballX);
                    ev.params.put("y", ballY);

                } else {
                    p.lost = true;
                }
            }
            if (ballVX > 0 && ballX >= WIDTH - BALL_RADIUS - PADDLE_WIDTH) {
                Player p = gameManager.getPlayer(1);
                double paddleY = (p.previousY * (1 - t)) + p.y * t;
                if (ballY > paddleY - PADDLE_HEIGHT / 2 && ballY < paddleY + PADDLE_HEIGHT / 2) {
                    ballVX *= -1;
                    gameManager.addTooltip(new Tooltip(p.getIndex(), "Pong"));

                    ViewerEvent ev = animatedEventModule.createAnimationEvent("Ping", t);
                    ev.params.put("player", 1);
                    ev.params.put("x", ballX);
                    ev.params.put("y", ballY);
                } else {
                    p.lost = true;
                }
            }

            ball.setX(ballX).setY(ballY);
            graphicEntityModule.commitEntityState(t, ball);
        }
    }

    @Override
    public void init() {
        gameManager.setFrameDuration(300);

        ballX = WIDTH / 2;
        ballY = HEIGHT / 2;
        ballVX = 148;
        ballVY = 132;

        
        
        graphicEntityModule.createSprite().setImage("Background.jpg").setAnchor(0);

        for (Player p : gameManager.getPlayers()) {
            p.previousY = p.y = HEIGHT / 2;

            p.paddle = graphicEntityModule.createLine()
                    .setLineWidth(PADDLE_WIDTH)
                    .setX(p.getIndex() == 0 ? PADDLE_WIDTH / 2 : WIDTH - PADDLE_WIDTH / 2)
                    .setY(p.y - PADDLE_HEIGHT / 2)
                    .setX2(p.getIndex() == 0 ? PADDLE_WIDTH / 2 : WIDTH - PADDLE_WIDTH / 2)
                    .setY2(p.y + PADDLE_HEIGHT / 2)
                    .setLineColor(0xffffff);
        }

        ball = graphicEntityModule.createCircle()
                .setRadius(BALL_RADIUS)
                .setFillColor(0xffffff)
                .setX(ballX)
                .setY(ballY);
    }

    @Override
    public void gameTurn(int turn) {
        // Send new inputs with the updated positions
        sendPlayerInputs();

        // Update new positions
        for (Player p : gameManager.getActivePlayers()) {
            int deltaMove;
            try {
                deltaMove = p.getAction() - p.y;

                deltaMove = clamp(deltaMove, -120, 120);
                int newPosY = p.y + deltaMove;

                p.previousY = p.y;
                p.y = clamp(newPosY, PADDLE_HEIGHT / 2, HEIGHT - PADDLE_HEIGHT / 2);
                p.paddle.setY(p.y - PADDLE_HEIGHT / 2);
                p.paddle.setY2(p.y + PADDLE_HEIGHT / 2);
            } catch (NumberFormatException | TimeoutException e) {
                p.deactivate("Eliminated!");
            }
        }

        moveBall();

        if (ballX < 0) {
            gameManager.getPlayer(0).deactivate();
        } else if (ballX >= WIDTH) {
            gameManager.getPlayer(1).deactivate();
        }

        if (gameManager.getActivePlayers().size() < 2) {
            gameManager.endGame();
        }
    }

    @Override
    public void onEnd() {
        for (Player p : gameManager.getPlayers()) {
            p.setScore(p.isActive() ? 1 : 0);
        }
    }
    */
}
