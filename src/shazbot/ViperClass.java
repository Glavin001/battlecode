package shazbot;

import battlecode.common.Clock;

public class ViperClass extends RobotPlayer{
	
	public static void move(){
		
	}
	
	public static void attack(){
		
	}

	public static void run(){
		int fate = rand.nextInt(1000);
        while (true) {
            try {
            	updateNearbyEnemies();
            	if(attackableTraitors.length > 0){
            		rc.setIndicatorString(0, "Attacking Traitors");
            		attack(enemyTeam);
            	}else{
            		rc.setIndicatorString(0, "Moving");
            		moveToClosestTraitor();
            	}
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }   
		
	}
}
