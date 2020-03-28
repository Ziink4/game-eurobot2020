import java.util.Scanner;

public class BossHomologationP1 {
	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int i = 0;
			// game loop
			while (true) {
				in.nextInt();
				in.nextInt();
				in.nextInt();
				in.nextInt();

				//Just robot 1 for few seconds
				if(i < 5) {
					System.out.println("10 10");
				}
				else {
					System.out.println("0 0");
				}
				System.out.println("0 0");
				System.out.println("3");
				i += 1;
			}
		}
	}
}

