import java.util.Scanner;


public class Agent1 {
	public static void main(String[] args) {
		
		String replay = 
				"Sortie standard :\n" + 
				"674 490 IDLE\n" + 
				"-280 280 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"490 674 IDLE\n" + 
				"280 -280 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"404 -107 IDLE\n" + 
				"0 0 ACTIVATE_FRONT\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"-107 404 IDLE\n" + 
				"0 0 ACTIVATE_FRONT\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 ACTIVATE_LEFT\n" + 
				"1029 1029 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 ACTIVATE_RIGHT\n" + 
				"1029 1028 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"-52 52 IDLE\n" + 
				"0 0 LIGHT\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"52 -52 IDLE\n" + 
				"0 0 LIGHT\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"1246 1247 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"1247 1246 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"246 248 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"248 246 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"266 -266 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"-266 266 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 WIND\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 WIND\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 ACTIVATE_LEFT\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 ACTIVATE_RIGHT\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"581 588 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"588 581 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 WIND\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 WIND\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"383 -383 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"-383 383 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50\n" + 
				"Sortie standard :\n" + 
				"0 0 IDLE\n" + 
				"0 0 IDLE\n" + 
				"50";
		
		String[] rep = replay.split("\n");
		
		try (Scanner in = new Scanner(System.in)) {
			String playerColor = in.next();
			for (int i = 0; i < 2; i++) {
				int x = in.nextInt();
				int y = in.nextInt();
				int angle = in.nextInt();
			}
			int r = 0;
			
			if(playerColor.equals("YELLOW")) {
				r += 4;
			}

			// game loop
			while (true) {
				for (int i = 0; i < 2; i++) {
					int leftEncoder = in.nextInt();
					int rightEncoder = in.nextInt();
					String lastTakenColor = in.next();
					String detectedCompass = in.next();
				}

				for (int i = 0; i < 2; i++) {
					int frontLowSensor = in.nextInt();
					int rightLowSensor = in.nextInt();
					int backLowSensor = in.nextInt();
					int leftLowSensor = in.nextInt();
					int frontLeftHighSensor = in.nextInt();
					int frontRightHighSensor = in.nextInt();
					int backRightHighSensor = in.nextInt();
					int backLeftHighSensor = in.nextInt();
				}
				for (int i = 0; i < 2; i++) {
					for (int j = 0; j < 360; j++) {
						int lidarData = in.nextInt();
					}
				}
				
				for(int i = 0; i < 3; i += 1) {
					System.out.println(rep[r + i + 1]);
				}
				r += 8;
			}
		}
	}
}
