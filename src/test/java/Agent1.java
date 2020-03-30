import java.util.Scanner;

public class Agent1 {
	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int turn = 0;
			int score = 0;
			
			System.err.println(in.nextLine());
			for (int i = 0; i < 2; i++) {
				System.err.println(in.nextLine());
			}

			// game loop
			while (true) {
				for (int i = 0; i < 2; i++) {
					System.err.println(in.nextLine());
				}
				for (int i = 0; i < 2; i++) {
					System.err.println(in.nextLine());
				}
				for (int i = 0; i < 2; i++) {
					System.err.println(in.nextLine());
				}

				if (turn < 5) {
					System.out.println("20 20 ACTIVATE_FRONT");
				} else if (turn < 6) {
					System.out.println("0 0 TAKE");
				} else if (turn < 11) {
					System.out.println("-20 -20 IDLE");
				} else if (turn < 21) {
					System.out.println("30 -30 ACTIVATE_FRONT");
				} else if (turn < 25) {
					System.out.println("0 0 RELEASE");
				} else if (turn < 26) {
					System.out.println("0 0 IDLE");
					score += 1;
				} else if (turn < 28) {
					System.out.println("10 10 IDLE");
				} else if (turn < 29) {
					System.out.println("0 0 IDLE");
					score += 1;
				} else {
					System.out.println("0 0 IDLE");
				}
				System.out.println("0 0 IDLE");
				System.out.println(score);
				turn += 1;
			}
		}
	}
}
