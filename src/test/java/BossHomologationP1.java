import java.util.Scanner;

public class BossHomologationP1 {
	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			String playerColor = in.next();
			
			int i = 0;
			// game loop
			while (true) {
				for (int k = 0; k < 2; k++) {
					int leftEncoder = in.nextInt();
					int rightEncoder = in.nextInt();
					String lastTakenColor = in.next();
				}

				//Just robot 1 for few seconds
				if(i < 5) {
					System.out.println("10 10 IDLE");
				}
				else {
					System.out.println("0 0 IDLE");
				}
				System.out.println("0 0 IDLE");
				System.out.println("3");
				i += 1;
			}
		}
	}
}

