import java.util.Scanner;

public class AgentAsserv {
	public static class Robot {
		private static final double TICK_TO_MM = 0.05;
		private static final double TICK_TO_DEG = (360.0 / (2 * Math.PI * 250.0 /(2 * TICK_TO_MM)));
		
		private double _x;
		private double _angle;
		private double _y;
		private int _leftEncoder;
		private int _rightEncoder;
		private int _distance;

		public void setPosition(int x, int y, int angle) {
			_x = x;
			_y = y;
			_angle = Math.toRadians(angle);
		}

		public String debugPosition() {
			return Math.round(_distance) + " " + Math.round(_x) + " " + Math.round(_y) + " " + Math.round(Math.toDegrees(_angle));
		}

		public void setEncoders(int leftEncoder, int rightEncoder) {
			int delta_left = leftEncoder - _leftEncoder;
			int delta_right = rightEncoder - _rightEncoder;
			_leftEncoder = leftEncoder;
			_rightEncoder = rightEncoder;
			int _deltaDistance = delta_left + delta_right;
			
			_distance += _deltaDistance * TICK_TO_MM;
			_angle += Math.toRadians((-delta_left + delta_right) * TICK_TO_DEG);
			_x += _deltaDistance * TICK_TO_MM * Math.cos(_angle);
			_y += _deltaDistance * TICK_TO_MM * Math.sin(_angle);
		}
		
	}
	
	public static void main(String[] args) {
		try (Scanner in = new Scanner(System.in)) {
			int turn = 0;
			int score = 0;
			Robot[] robots = {null, null};
			
			String playerColor = in.next();
	        for (int i = 0; i < 2; i++) {
	            int x = in.nextInt();
	            int y = in.nextInt();
	            int angle = in.nextInt();
	            
	            robots[i] = new Robot();
	            robots[i].setPosition(x, y, angle);
	        }
	        
			// game loop
			while (true) {
				for (int i = 0; i < 2; i++) {
	                int leftEncoder = in.nextInt();
	                int rightEncoder = in.nextInt();
	                String lastTakenColor = in.next();
	                String detectedCompass = in.next();
	                robots[i].setEncoders(leftEncoder, rightEncoder);
	                System.err.println(leftEncoder+ " " + rightEncoder);
	                System.err.println(robots[i].debugPosition());
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

	            if (turn < 10) {
					System.out.println("20 20 ACTIVATE_FRONT");
				}
	            else {
	            	System.out.println("-20 20 ACTIVATE_FRONT");
	            }
	            
	            /*
				if (turn < 5) {
					System.out.println("20 20 ACTIVATE_FRONT");
				} else if (turn < 6) {
					System.out.println("0 0 TAKE");
				} else if (turn < 11) {
					System.out.println("-20 -20 IDLE");
				} else if (turn < 21 && playerColor.equals("BLUE")) {
					System.out.println("30 -30 ACTIVATE_FRONT");
				}else if (turn < 21) {
					System.out.println("-30 30 ACTIVATE_FRONT");
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
				*/
				System.out.println("0 0 IDLE");
				System.out.println(score);
				turn += 1;
			}
		}
	}
}
