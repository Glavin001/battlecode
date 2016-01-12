package shazbot;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import shazbot.RobotPlayer.Messaging;
import shazbot.RobotPlayer.messageConstants;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

public class ArchonClass extends RobotPlayer{
	
	static int defaultBroadcastRange = 2000;
	//Are we waiting on a scout to complete a message?
	static Map<Integer, Boolean> waitingMessageID;
	//What was the last message the scout sent?
	static Map<Integer, Integer> lastTransmissionID;
	
	static ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();


	static class ArchonMessaging{
		
		public static void handleMessageQueue() throws GameActionException{
			//SUPER
			Messaging.handleMessageQueue();
			//currentSignals[] is now set for this round. Overflow may cause problems.
            if (currentSignals.length > 0) {
				for(Signal signal : currentSignals){
				   	 MapLocation loc = signal.getLocation();   
				   	 int msg1 = signal.getMessage()[0];
				   	 int msg2 = signal.getMessage()[1];
				   	 int id = signal.getID();
				   	 switch(msg1){
				   	 	//Handle Scout messages about map bounds
				   	 	case messageConstants.SMBN:
				   	 		//Propagate the message to nearby scouts and archons
				   	 		rc.broadcastMessageSignal(messageConstants.AMBN, msg2, defaultBroadcastRange);
				   	 		//Set map bounds
				   	 		msg2 = Messaging.adjustBound(msg2);
				   	 		setMapBound(Direction.NORTH, msg2);
				   	 		break;
				   	 	case messageConstants.SMBE:
				   	 		rc.broadcastMessageSignal(messageConstants.AMBE, msg2, defaultBroadcastRange);
				   	 		msg2 = Messaging.adjustBound(msg2);
				   	 		setMapBound(Direction.EAST, msg2);		
				   	 		break;
				   	 	case messageConstants.SMBS:
				   	 		rc.broadcastMessageSignal(messageConstants.AMBS, msg2, defaultBroadcastRange);
				   	 		msg2 = Messaging.adjustBound(msg2);				   	 		
				   	 		setMapBound(Direction.SOUTH, msg2);
				   	 		break;
				   	 	case messageConstants.SMBW:
				   	 		rc.broadcastMessageSignal(messageConstants.AMBW, msg2, defaultBroadcastRange);
				   	 		msg2 = Messaging.adjustBound(msg2);
				   	 		setMapBound(Direction.WEST, msg2);
				   	 		break;
				   	 		
				   	 	//Handle reporting of zombie dens
				   	 	case messageConstants.DENX:
				   	 		storeDenLocation(msg2, id, messageConstants.DENX);
				   	 		break;
				   	 	case messageConstants.DENY:
				   	 		msg2 = Messaging.adjustBound(msg2);	
				   	 		storeDenLocation(msg2, id, messageConstants.DENY);
				   	 		break;
				   	 		
				   	 }

				}
            }			
			
		}
		
		public static void storeDenLocation(int packedCoordinate, int id, int denXOrDenY){
   	 		//Unpackage map coordinate.
   	 		int coordinate = Messaging.adjustBound(packedCoordinate);
   	 		//If we have an entry for this scout
   	 		if(waitingMessageID.containsKey(id)){
   	 			//If we had DENY and just got DENX, complete
   	 			if(waitingMessageID.get(id)){
   	 				int oldCoordinate = lastTransmissionID.get(id);
   	 				//We are no longer waiting on this scout.
   	 				waitingMessageID.put(id, false);
   	 				//Check if the den already exists in our list, return if it does
   	 				for(MapLocation den : knownDens){
   	 					if((den.x == coordinate && den.y == oldCoordinate) || (den.y == coordinate && den.x == oldCoordinate)){
   	 						return;
   	 					}
   	 				}
   	 				//Create a new map location at the reported spot.
   	 				MapLocation denLoc;
   	 				if(denXOrDenY == messageConstants.DENX){
   	 					denLoc = new MapLocation(coordinate, oldCoordinate);
   	 				}else{
   	 					denLoc = new MapLocation(oldCoordinate, coordinate);
   	 				}
   	 				knownDens.add(denLoc);
   	 			}
   	 		}else{
   	 			//Otherwise put in a new entry
   	 			waitingMessageID.put(id, true);
   	 			lastTransmissionID.put(id, coordinate);
   	 		}			
		}
		
		public static void broadcastMapBounds() throws GameActionException{
			rc.broadcastMessageSignal(messageConstants.AMBN, Messaging.adjustBound(northBound), defaultBroadcastRange);
			rc.broadcastMessageSignal(messageConstants.AMBE, Messaging.adjustBound(eastBound), defaultBroadcastRange);
			rc.broadcastMessageSignal(messageConstants.AMBS, Messaging.adjustBound(southBound), defaultBroadcastRange);
			rc.broadcastMessageSignal(messageConstants.AMBW, Messaging.adjustBound(westBound), defaultBroadcastRange);
		}
	}
	

	
	static class Building{
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
	                        rc.broadcastMessageSignal(messageConstants.NEAL,(int)'A', broadcastDistance);
	                        //If it is a scout, tell it the new bounds that we already know
	                        if(typeToBuild == RobotType.SCOUT && allBoundsSet){
	                        	ArchonMessaging.broadcastMapBounds();
	                        }
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
	}
	
	public static void defendMe(){
		//If we somehow have many nearby enemies and no allies, have everyone retreat.
		if(nearbyEnemies.length > 0){
			//HELP ME
		}
	}
	
	public static void initialize(){
		waitingMessageID = new HashMap<Integer, Boolean>();
		lastTransmissionID = new HashMap<Integer, Integer>();
	}
	
	
	public static void run(){	
		
		RobotType[] unitsToBuild = {RobotType.GUARD, RobotType.SCOUT};
		int nextUnitToBuild = rand.nextInt(unitsToBuild.length);
		boolean canChooseNextUnit = false;
		initialize();
		
        while (true) {
            try {
            	Debug.emptyIndicatorStrings();
            	Sensing.updateNearbyEnemies();
            	ArchonMessaging.handleMessageQueue();
            	if(canChooseNextUnit){
            		nextUnitToBuild = rand.nextInt(unitsToBuild.length);
            		canChooseNextUnit = false;
            	}
            	
            	rc.setIndicatorString(2, "All bounds known?: " + " " + Boolean.toString(allBoundsSet) + Integer.toString(northBound) + "was nb and eb is: " + Integer.toString(eastBound));
            	
            	//Build turrets at the start, remove this later
                if(rc.getRoundNum() < 120){
                	Building.tryBuildUnit(RobotType.TURRET);
                //Otherwise build a random unit from list above
                }else{
                	//If we successfully built a unit, we get to choose another one.
                	//If we don't use this, we end up never building expensive units.
                	if(Building.tryBuildUnit(unitsToBuild[nextUnitToBuild])){
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
