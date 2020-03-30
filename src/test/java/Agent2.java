import java.util.Scanner;

public class Agent2 {
    public static void main(String[] args) {
    	try (Scanner in = new Scanner(System.in)) {
			String playerColor = in.next();
			
			int turn = 0;
			int score = 0;
			// game loop
			while (true) {
				for (int i = 0; i < 2; i++) {
					int leftEncoder = in.nextInt();
					int rightEncoder = in.nextInt();
					String lastTakenColor = in.next();
				}
				// Write an action using System.out.println()
				// To debug: 

				System.err.println(playerColor);
				
				String order = "ACTIVATE_FRONT";
				if((turn % 2) == 1)
				{
					order = "TAKE";
				}
				/*
				if(playerColor.equals("BLUE")) {
					if (turn< 5) {
						System.out.println("20 20 " + order);
					} 
					else {
						System.out.println("-2 -2 " + order);
					}
				}
				else {
					if (turn< 5) {
						System.out.println("20 20 " + order);
					} 
					else {
						System.out.println("60 60 " + order);
					}
				}
				*/
				
				System.out.println("100 100 IDLE");
				System.out.println("100 100 IDLE");
				System.out.println(score);
				turn += 1;
			}
    	}
    }
}
