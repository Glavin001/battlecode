package shazbot;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    
    // You can instantiate variables here.
    static RobotController rc;
    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
    static RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
    static RobotInfo[] nearbyEnemies;
    static RobotInfo[] nearbyTraitors;
    static RobotInfo[] nearbyAllies;
    static RobotInfo[] veryCloseAllies;
    static RobotInfo[] attackableZombies;
    static RobotInfo[] attackableTraitors;
    static Random rand;
    static int myAttackRange;
    static Team myTeam;
    static Team enemyTeam;
    static RobotType myRobotType;
    static MapLocation nearestArchon;
    
    static class Movement{
    	
    	 public static void randomMove() throws GameActionException{
    	    	int fate = rand.nextInt(8);
    	    	if(rc.isCoreReady()){
    	    		Direction dirToMove;
    	    		for(int i  = 0; i < 8; i++){
    	    			dirToMove = directions[(fate + i) % 8];
    	                if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_SLOW_THRESH) {
    	                    // Too much rubble, so I should clear it
    	                    rc.clearRubble(dirToMove);
    	                    break;
    	                    // Check if I can move in this direction
    	                } else if (rc.canMove(dirToMove)) {
    	                    // Move
    	                    rc.move(dirToMove);
    	                    break;
    	                }
    	    		}
    	    	}else{
    	    		//rc.setIndicatorString(0, "I could not move this turn");
    	    	}
    	    }
    	    
    	    
    	    public static void retreatToArchon() throws GameActionException{
    	    	pathToLocation(nearestArchon);
    	    }   	
    	    public static void moveToClosestEnemy() throws GameActionException{
    	    	if(nearbyEnemies.length > 0){
    	    		pathToLocation(nearbyEnemies[0].location);
    	    	}else{
    	    		randomMove();
    	    	}
    	    }
    	    
    	    public static void moveToClosestTraitor() throws GameActionException{
    	    	if(nearbyTraitors.length > 0){
    	    		pathToLocation(nearbyTraitors[0].location);
    	    	}else{
    	    		randomMove();
    	    	}    	
    	    }
    	    
    	    public static void pathToLocation(MapLocation loc) throws GameActionException{
    	    	if(rc.isCoreReady()){
    	    		MapLocation myLoc = rc.getLocation();
    	    		Direction dirToMove = myLoc.directionTo(loc);
    	    		rc.setIndicatorString(0, "Direction to path is: " + dirToMove.toString());
    	            if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_SLOW_THRESH) {
    	                // Too much rubble, so I should clear it
    	                rc.clearRubble(dirToMove);
    	                // Check if I can move in this direction
    	            } else if (rc.canMove(dirToMove)) {
    	                rc.move(dirToMove);
    	            } else if (rc.canMove(dirToMove.rotateLeft())){
    	            	rc.move(dirToMove.rotateLeft());
    	            } else if (rc.canMove(dirToMove.rotateRight())){
    	            	rc.move(dirToMove.rotateRight());
    	            }
    	    	}
    	    }
    }

    
    static class Offensive{
    	public static void attack() throws GameActionException{
        	attack(null);
        }
        
        public static void attack(Team team) throws GameActionException{
    		if(rc.isCoreReady() && rc.isWeaponReady()){
    			//If unspecified, attack first enemy in list
    			if(team == null){
    				//Prioritize attacking enemy units
    				if(attackableTraitors.length > 0){
    					rc.attackLocation(attackableTraitors[0].location);
    				}else if(attackableZombies.length > 0){
    					rc.attackLocation(attackableZombies[0].location);
    				}
        		//Attack first zombie in list
    			}else if(team == Team.ZOMBIE){
    				if(attackableZombies.length > 0){
    					rc.attackLocation(attackableZombies[0].location);
    				}
    			//Attack first enemy on other team
    			}else if(team == enemyTeam){
    				if(attackableTraitors.length > 0){
    					rc.attackLocation(attackableTraitors[0].location);
    				}				
    			}
    		}
        }    	
    }
    
    
    public static void updateNearbyEnemies(){
    	nearbyEnemies = rc.senseHostileRobots(rc.getLocation(), myRobotType.sensorRadiusSquared);  	
    	nearbyTraitors = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, enemyTeam);
    	nearbyAllies = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, myTeam);
    	veryCloseAllies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, myTeam);
    	attackableTraitors = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, enemyTeam);
    	attackableZombies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, Team.ZOMBIE);
    }
    
    public static void handleMessageQueue(){
    	 Signal[] signals = rc.emptySignalQueue();
         if (signals.length > 0) {
             //rc.setIndicatorString(0, "I received a signal this turn!");
        	 for(Signal signal : signals){
            	 MapLocation loc = signal.getLocation();   
            	 //rc.setIndicatorString(1, "The location recieved was:" + loc.toString());
            	 //Assume that the transmitted signal was for the archon location
            	 nearestArchon = loc;
        	 }
         }
    }
    
    public static void emptyIndicatorStrings(){
    	for(int i = 0; i < 3; i++){
    		rc.setIndicatorString(i, "");
    	}
    }
    
    public static void run(RobotController inrc) {
        // You can instantiate variables here.
    	rc = inrc;
        rand = new Random(rc.getID());
        myAttackRange = 0;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
		myAttackRange = rc.getType().attackRadiusSquared;
		myRobotType = rc.getType();	
        
        switch(rc.getType()){
        	case ARCHON:
        		ArchonClass.run();
        		break;
        	case TURRET:
        		TurretClass.run();
        		break;
        	case SOLDIER:
        		SoldierClass.run();
        		break;
        	case GUARD:
        		GuardClass.run();
        		break;
        	case VIPER:
        		ViperClass.run();
        		break;
        	case SCOUT:
        		ScoutClass.run();
        		break;	
        }
    }
}
