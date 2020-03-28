import java.util.Scanner;

public class Agent2 {
    public static void main(String[] args) {
    	Scanner in = new Scanner(System.in);
        int i = 0;
        int left = 0, right = 0;

        // game loop
        while (true) {
            int leftencoder = in.nextInt();
            int rightencoder = in.nextInt();

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            if((i % 10) == 0)
            {
            	left = (int) (Math.random() * 200 - 100);
            	right = (int) (Math.random() * 200 - 100);
            }
            
            // Left and right motor setpoint in percentage (intergers)
            System.out.println(left + " " + right);
            
            i += 1;
        }
    }
}
