package racksgore;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer{
	
	static Random rnd;
	static RobotController rc;
	
	public static void run(RobotController rcIn){
		
		rc = rcIn;
		rnd = new Random(rc.getID());
		
		while(true){
			try{
				if(rc.getType()==RobotType.ARCHON){
					archonCode();
				}else if(rc.getType()==RobotType.GUARD){
					guardCode();
				}
			}catch(Exception e){
				e.printStackTrace();
			}

			Clock.yield();
		}
	}

	private static void guardCode() throws GameActionException {
		RobotInfo[] enemyArray = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared,Team.ZOMBIE);
		if(enemyArray.length>0){
			if(rc.isWeaponReady()){
				//look for adjacent enemies to attack
				for(RobotInfo oneEnemy:enemyArray){
					if(rc.canAttackLocation(oneEnemy.location)){
						rc.setIndicatorString(0,"trying to attack");
						rc.attackLocation(oneEnemy.location);
						break;
					}
				}
			}
			//could not find any enemies adjacent to attack
			//try to move toward them
			if(rc.isCoreReady()){
				MapLocation goal = enemyArray[0].location;
				Direction toEnemy = rc.getLocation().directionTo(goal);
				if(rc.canMove(toEnemy)){
					rc.setIndicatorString(0,"moving to enemy");
					rc.move(toEnemy);
				}else{
					MapLocation ahead = rc.getLocation().add(toEnemy);
					if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
						rc.clearRubble(toEnemy);
					}
				}
			}
		}
	}

	private static void archonCode() throws GameActionException {
		if(rc.isCoreReady()){
			Direction randomDir = randomDirection();
			if(rc.canBuild(randomDir, RobotType.GUARD)){
				rc.build(randomDir,RobotType.GUARD);
			}
		}
	}

	private static Direction randomDirection() {
		return Direction.values()[(int)(rnd.nextDouble()*8)];
	}
	
}