import java.util.Scanner;

public class Agent1 {
	private static final int WIDTH = 1920;
	private static final int HEIGHT = 1080;
	private static final int BALL_RADIUS = 20;
	private static final int PADDLE_WIDTH = 15;

	public static void main(String[] args) {
		Scanner in = new Scanner(System.in);
		int i = 0;
		// game loop
		while (true) {
			in.nextInt();
			in.nextInt();
			in.nextInt();
			in.nextInt();

			// Write an action using System.out.println()
			// To debug: System.err.println("Debug messages...");
			
			if ((i % 45) < 30) {
				// Left and right motor setpoint in percentage (intergers)
				System.out.println("20 20 PRE_TAKE_FLOOR");
				System.out.println("10 30 IDLE");
			} else {
				System.out.println("-10 -15 PRE_TAKE_FLOOR");
				System.out.println("-15 15 IDLE");
			}
			System.out.println("3");
			i += 1;
		}
	}
}
