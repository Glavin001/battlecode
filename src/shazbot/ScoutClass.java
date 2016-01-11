package shazbot;

import battlecode.common.Clock;
import battlecode.common.Team;

public class ScoutClass extends RobotPlayer{
	
	
	public static void run(){
	    while (true) {
	        try {
            	updateNearbyEnemies();
            	handleMessageQueue();
            	
            	//Wander out into the wilderness
            	//find anything of interest
            	//report back to archons when we have enough data
            	//Give report, follow squad that gets deployed
            	//Constantly broadcast to squad attack info
            	//Signal troops to retreat when attack is done, or failed, or when reinforcements are needed,
            	// or when zombie spawn is upcoming
            	
            	if(attackableTraitors.length > 0){
            		attack(enemyTeam);
            		rc.setIndicatorString(0, "Attacking Traitors");
            	}else if(attackableZombies.length > 0){
            		attack(Team.ZOMBIE);
            		rc.setIndicatorString(0, "Attacking Zombies");           		
            	}else{
            		moveToClosestEnemy();
            		rc.setIndicatorString(0, "Moving");

            	}
	            Clock.yield();
	        } catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	        }
	    }  		
	}
}
