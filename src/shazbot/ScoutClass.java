package shazbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.Team;

public class ScoutClass extends RobotPlayer{
	
	//Get the maximum number of tiles in one direction away in sensor radius
	static int myDv = (int)Math.floor(Math.sqrt(myRobotType.sensorRadiusSquared));
	static Direction[] cardinals = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
	static boolean[] haveOffsets = {false, false, false, false};
	static int[] offsets = {0,0,0,0};
	static Direction currentExploreDirection = Direction.NORTH;
	static int numExploredDirections = 0;
	
	//Enclosure for all of the exploration functions
	static class Exploration{
		//Dumb help function, please kill me and replace with a map
		public static int returnCardinalIndex(Direction dir) throws GameActionException{
			//Switch because no associative arrays, could map this instead, but that might cost more
			switch(dir){
			case NORTH:
				return 0;
			case EAST:
				return 1;
			case SOUTH:
				return 2;
			case WEST:
				return 3;
			default:
				GameActionException e = new GameActionException(null, "Not a valid cardinal direction.");
				throw e;
			}
		}
		
		public static void explore(Direction dirToMove) throws GameActionException{
			if(rc.isCoreReady()){
				if (rc.canMove(dirToMove)) {
		            rc.move(dirToMove);
		        } else if (rc.canMove(dirToMove.rotateLeft())){
		        	rc.move(dirToMove.rotateLeft());
		        } else if (rc.canMove(dirToMove.rotateRight())){
		        	rc.move(dirToMove.rotateRight());
		        }			
			}
		}
		
		//Tells us if a point in a given cardinal direction at our maximum sight range is on the map
		//This should only take in north,south,east,west
		public static boolean checkCardinalOnMap(Direction dir) throws GameActionException{
	    	MapLocation offsetLocation = rc.getLocation().add(dir, myDv);
	    	rc.setIndicatorDot(offsetLocation, 255, 80, 80);
	    	boolean onMap = rc.onTheMap(offsetLocation);
	    	rc.setIndicatorString(0, dir.toString() + " point on map?: " + Boolean.toString(onMap));
	    	return onMap;
		}
		
		//This function sets the value of a given direction bound, if it can at all this round.
		//Call this after checkCardinalOnMap returns false. 
		public static void findExactOffset(Direction dir) throws GameActionException{
			for(int i = myDv; i > 0; i--){
				MapLocation temp = rc.getLocation().add(dir, i);
				if(rc.onTheMap(temp)){
					offsets[returnCardinalIndex(dir)] = (dir == Direction.NORTH || dir == Direction.SOUTH)? temp.y : temp.x;
					haveOffsets[returnCardinalIndex(dir)] = true;
					rc.setIndicatorString(0, dir.toString() + " bound value is : " + Integer.toString(offsets[returnCardinalIndex(dir)]));
					numExploredDirections++;
					break;
				}
			}		
		}
		
		public static void tryExplore() throws GameActionException{
	    	//If we have not found every bound
	    	if(numExploredDirections != 4){
	    		//If we don't already have a bound for this direction
	    		if(!haveOffsets[returnCardinalIndex(currentExploreDirection)]){
	    			//If we go off the map in sight range for the given direction, we can get the offset
	    			if(!checkCardinalOnMap(currentExploreDirection)){
	    				findExactOffset(currentExploreDirection);
	    				currentExploreDirection = cardinals[numExploredDirections];
	    			//Otherwise go explore in that direction.
	    			}else{
	    				explore(currentExploreDirection);
	    			}
	    		}           		
	    	}		
		}		
	}
	
	public static void run(){
	    while (true) {
	        try {
	        	emptyIndicatorStrings();
            	updateNearbyEnemies();
            	handleMessageQueue();
            	
            	//Wander out into the wilderness
            	//find anything of interest
            	//report back to archons when we have enough data
            	//Give report, follow squad that gets deployed
            	//Constantly broadcast to squad attack info
            	//Signal troops to retreat when attack is done, or failed, or when reinforcements are needed,
            	// or when zombie spawn is upcoming
            	Exploration.tryExplore();
            	
	            Clock.yield();
	        } catch (Exception e) {
	            System.out.println(e.getMessage());
	            e.printStackTrace();
	        }
	    }  		
	}
}
