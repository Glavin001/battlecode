package shazbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;

public class ArchonClass extends RobotPlayer{

	
	//Tries to build a given unit in a random adjacent location.
	//Returns true if the unit was successfully build, and false otherwise.
	//Also broadcasts a signal to all nearby robots in a set range with its location.
	public static boolean tryBuildUnit(RobotType rt) throws GameActionException{
		
		int broadcastDistance = 80;
		
        if (rc.isCoreReady()) {
        	RobotType typeToBuild = rt;
        	if(rc.hasBuildRequirements(typeToBuild)){
        		rc.setIndicatorString(1, "Building: " + typeToBuild.toString());
                Direction dirToBuild = directions[rand.nextInt(8)];
                for (int i = 0; i < 8; i++) {
                    // If possible, build in this direction
                    if (rc.canBuild(dirToBuild, typeToBuild)) {
                        rc.build(dirToBuild, typeToBuild);
                        // Broadcast to nearby units our location, so they save it.
                        // Just a note that broadcasting increases core delay, try to minimize it!
                        rc.broadcastMessageSignal((int)'A',(int)'A', broadcastDistance);
                        return true;
                    } else {
                        // Rotate the direction to try
                        dirToBuild = dirToBuild.rotateLeft();
                    }
                }
        	}
        }
        return false;
	}
	
	public static void defendMe(){
		if(nearbyEnemies.length > 0){
			//HELP ME
		}
	}
	
	public static void run(){	
		
		RobotType[] unitsToBuild = {RobotType.GUARD, RobotType.SCOUT};
		int nextUnitToBuild = rand.nextInt(unitsToBuild.length);
		boolean canChooseNextUnit = false;
		
        while (true) {
            try {
            	emptyIndicatorStrings();
            	updateNearbyEnemies();
            	handleMessageQueue();
            	if(canChooseNextUnit){
            		nextUnitToBuild = rand.nextInt(unitsToBuild.length);
            		canChooseNextUnit = false;
            	}
            	
            	//Build turrets at the start, remove this later
                if(rc.getRoundNum() < 120){
                	tryBuildUnit(RobotType.TURRET);
                //Otherwise build a random unit from list above
                }else{
                	//If we successfully built a unit, we get to choose another one.
                	//If we don't use this, we end up never building expensive units.
                	if(tryBuildUnit(unitsToBuild[nextUnitToBuild])){
                		canChooseNextUnit = true;
                	}
                }
                
                //Repair nearby allies.
                for(RobotInfo robot : nearbyAllies){
                	if(robot.health < robot.maxHealth && robot.type != RobotType.ARCHON){
                		rc.setIndicatorDot(robot.location, 80, 255, 80);
                		rc.setIndicatorString(1, "Repairing robot: " + Integer.toString(robot.ID));
                		//If we are within repair range of the robot, repair it, then break.
                		if(robot.location.distanceSquaredTo(rc.getLocation()) <= myAttackRange){
                			rc.repair(robot.location);
                			break;
                		}
                	}
                }
                
                
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }    			
	}
}
