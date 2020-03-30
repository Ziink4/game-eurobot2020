import com.codingame.gameengine.runner.MultiplayerGameRunner;

public class Main {
    public static void main(String[] args) {

        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
        
        gameRunner.addAgent(Agent2.class);
        gameRunner.addAgent(BossHomologationP1.class);
        
        gameRunner.start();
    }
}
