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
    //Copy of signal queue
    static Signal[] currentSignals;
    //Map bounds
    static int northBound = 0;
    static int eastBound = 0;
    static int southBound = 0;
    static int westBound = 0;
    static boolean nbs = false;
    static boolean ebs = false;
    static boolean sbs = false;
    static boolean wbs = false;
    static boolean allBoundsSet = false;
    
    //Message types
    static class messageConstants{
    	
    	//Nearest Archon Location
    	public final static int NEAL = 55555;
    	
    	//Scout Map Bounds North
    	public final static int SMBN = 12345;
    	public final static int SMBE = 22345;
    	public final static int SMBS = 32345;
    	public final static int SMBW = 42345;
    	//Archon Map Bounds North
    	public final static int AMBN = 54321;
    	public final static int AMBE = 44321;
    	public final static int AMBS = 34321;
    	public final static int AMBW = 24321;
    	
    }
    
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
    
    static class Messaging{
    	//Basic handling for message queuing, nearest archon
        public static void handleMessageQueue(){
       	 currentSignals = rc.emptySignalQueue();
            if (currentSignals.length > 0) {
                //rc.setIndicatorString(0, "I received a signal this turn!");
           	 for(Signal signal : currentSignals){
               	 MapLocation loc = signal.getLocation();   
               	 rc.setIndicatorString(1, "Message Recieved was: " + Integer.toString(signal.getMessage()[0]) + " " + Integer.toString(signal.getMessage()[1]));
               	 //Set the nearest archon location if appropriate message was recieved.
               	 if(signal.getMessage()[0] == messageConstants.NEAL){
               		 nearestArchon = loc;
               	 }
           	 }
            }
       }
        
    	//Fix for transmitting negative bounds.
        //Use this to before transmitting and receiving bounds
    	public static int adjustBound(int bound){
    		int offset = 20000;
    		if(bound > 16000) bound = bound - offset;
    		else if(bound < 0) bound = bound + offset;
    		return bound;
    	}
    }
    
    static class Sensing{
        public static void updateNearbyEnemies(){
        	nearbyEnemies = rc.senseHostileRobots(rc.getLocation(), myRobotType.sensorRadiusSquared);  	
        	nearbyTraitors = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, enemyTeam);
        	nearbyAllies = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, myTeam);
        	veryCloseAllies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, myTeam);
        	attackableTraitors = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, enemyTeam);
        	attackableZombies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, Team.ZOMBIE);
        }   	
    }

    static class Debug{
    	//Prevent indicator string for persisting longer than 1 turn
        public static void emptyIndicatorStrings(){
        	for(int i = 0; i < 3; i++){
        		rc.setIndicatorString(i, "");
        	}
        }   	
    }
    
	public static void setMapBound(Direction dir, int bound){
		if(!allBoundsSet){
			if(dir == Direction.NORTH){
				northBound = bound;
				nbs = true;
			}else if(dir == Direction.EAST){
				eastBound = bound;
				ebs = true;
			}else if(dir == Direction.SOUTH){
				southBound = bound;
				sbs = true;
			}else if(dir == Direction.WEST){
				westBound = bound;
				wbs = true;
			}
			if(nbs && ebs && sbs && wbs){
				allBoundsSet = true;
			}
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
