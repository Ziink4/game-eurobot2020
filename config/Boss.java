import java.util.Scanner;

class Player {
	public static void main(String[] args) {
    	Scanner in = new Scanner(System.in);
        int i = 0;
        int left1 = 0, right1 = 0;
        int left2 = 0, right2 = 0;

        // game loop
        while (true) {
            int leftencoder = in.nextInt();
            int rightencoder = in.nextInt();
            in.nextInt();
            in.nextInt();

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");

            if((i % 10) == 0)
            {
            	left1 = (int) (Math.random() * 200 - 100);
            	right1 = (int) (Math.random() * 200 - 100);
            	left2 = (int) (Math.random() * 200 - 100);
            	right2 = (int) (Math.random() * 200 - 100);
            }
            
            // Left and right motor setpoint in percentage (intergers)
            System.out.println(left1 + " " + right1);
            System.out.println(left1 + " " + right2);
            System.out.println("3");
            
            i += 1;
        }
    }
}
