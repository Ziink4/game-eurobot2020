import com.codingame.gameengine.runner.MultiplayerGameRunner;

public class Main {
    public static void main(String[] args) {

        MultiplayerGameRunner gameRunner = new MultiplayerGameRunner();
        
        gameRunner.addAgent(Boss.class);
        gameRunner.addAgent(AgentAsserv.class);
       
        
        gameRunner.start();
    }
}
