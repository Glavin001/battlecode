package team181;

import java.util.ArrayList;
import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team181.CommUtil.MessageTags;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Sensing;

/**
 * Scout player
 *
 */
public class ScoutPlayer extends RobotPlayer {

    // Get the maximum number of tiles in one direction away in sensor radius
    static int myDv = (int) Math.floor(Math.sqrt(myRobotType.sensorRadiusSquared));
    static Direction[] cardinals = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
    static boolean[] haveOffsets = { false, false, false, false };
    static Direction currentExploreDirection = Direction.NORTH;
    static int numExploredDirections = 0;
    static int numFoundArcons = 0;
    static boolean haveBroadCastedMapBounds = false;
    // Rounds until we are allowed to broadcast again
    static int broadCastCooldown = 0;
    static int incurredCooldownPerBroadcast = 40;
    static int minClusterSeparation = 15*15; 

    private static class Exploration {

        // Dumb help function, please kill me and replace with a map
        public static int returnCardinalIndex(Direction dir) throws GameActionException {
            // Switch because no associative arrays, could map this instead, but
            // that might cost more
            switch (dir) {
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

        // Tells us if a point in a given cardinal direction at our maximum
        // sight
        // range is on the map
        // This should only take in north,south,east,west
        public static boolean checkCardinalOnMap(Direction dir) throws GameActionException {
            MapLocation offsetLocation = rc.getLocation().add(dir, myDv);
            rc.setIndicatorDot(offsetLocation, 255, 80, 80);
            boolean onMap = rc.onTheMap(offsetLocation);
            rc.setIndicatorString(0, dir.toString() + " point on map?: " + Boolean.toString(onMap));
            return onMap;
        }

        // This function sets the value of a given direction bound, if it can at
        // all
        // this round.
        // Call this after checkCardinalOnMap returns false.
        public static void findExactOffset(Direction dir) throws GameActionException {
            for (int i = myDv; i > 0; i--) {
                MapLocation temp = rc.getLocation().add(dir, i);
                if (rc.onTheMap(temp)) {
                    int bound = (dir == Direction.NORTH || dir == Direction.SOUTH) ? temp.y : temp.x;
                    setMapBound(dir, bound);
                    haveOffsets[returnCardinalIndex(dir)] = true;
                    // rc.setIndicatorString(0, dir.toString() + " bound value
                    // is :
                    // " + Integer.toString(offsets[returnCardinalIndex(dir)]));
                    numExploredDirections++;
                    break;
                }
            }
        }

        public static void broadcastMapBounds() throws GameActionException {
            int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());
            rc.setIndicatorString(2, "I am broadcasting map coordinates now.");
            // Send north bound
            Message message = new Message(MessageTags.SMBN, new MapLocation(rc.getLocation().x, northBound));
            Messaging.sendMessage(message, distToNearestArchon);
            // East
            message = new Message(MessageTags.SMBE, new MapLocation(eastBound, rc.getLocation().y));
            Messaging.sendMessage(message, distToNearestArchon);
            // South
            message = new Message(MessageTags.SMBS, new MapLocation(rc.getLocation().x, southBound));
            Messaging.sendMessage(message, distToNearestArchon);
            // West
            message = new Message(MessageTags.SMBW, new MapLocation(westBound, rc.getLocation().y));
            Messaging.sendMessage(message, distToNearestArchon);
        }

        public static void tryExplore() throws GameActionException {
            // If we have not found every bound
            if (numExploredDirections != 4 && allBoundsSet != true) {
                // If we don't already have a bound for this direction
                if (!haveOffsets[returnCardinalIndex(currentExploreDirection)]) {
                    // If we go off the map in sight range for the given
                    // direction,
                    // we can get the offset
                    if (!checkCardinalOnMap(currentExploreDirection)) {
                        findExactOffset(currentExploreDirection);
                        currentExploreDirection = cardinals[numExploredDirections % 4];
                        // Otherwise go explore in that direction.
                    } else {
                        explore(currentExploreDirection);
                    }
                }
            } else if (!haveBroadCastedMapBounds) {
                broadcastMapBounds();
                haveBroadCastedMapBounds = true;
            } else {
                explore(Movement.randomDirection());
                // explore(myLocation.directionTo(nearestArchon).opposite());
            }
        }
    }

    private static class ScoutMessaging {

        public static void handleMessageQueue() throws GameActionException {
            // SUPER
            Messaging.handleMessageQueue();
            // currentSignals[] is now set for this round. Overflow may cause
            // problems.
            if (currentSignals.length > 0) {
                for (Signal signal : currentSignals) {
                    Message message = new Message(signal);
                    switch (message.getTag()) {
                    // Handle Scout messages that about map bounds
                    case CommUtil.MessageTags.SMBN:
                        // Set map bounds
                        setMapBound(Direction.NORTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBE:
                        setMapBound(Direction.EAST, message.getLocation().x);
                        break;
                    case MessageTags.SMBS:
                        setMapBound(Direction.SOUTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBW:
                        setMapBound(Direction.WEST, message.getLocation().x);
                        break;

                    }

                }
            }

        }

    }
    
    private static class ScoutReporting {
        // Base function for reporting fixed objects
        public static void reportFixedObject(Message message, ArrayList<MapLocation> knowns, int squaredRadius) throws GameActionException{
            // Check known unit so we don't add duplicates
            MapLocation loc = message.getLocation();
            boolean wasDuplicate = false;
            for (MapLocation fixedUnit : knowns) {
                if (fixedUnit.equals(loc)) {
                    wasDuplicate = true;
                    // If it was a duplicate, go to next object and don't broadcast
                    return;
                }
            }
            // Otherwise we are dealing with a new object.
            rc.setIndicatorDot(loc, 255, 100, 255);
            knowns.add(loc);
            Messaging.sendMessage(message, squaredRadius);;
        }
        
        //Report Dens and neutrals
        public static void reportFixedRobot(int tag, RobotInfo robot, ArrayList<MapLocation> knowns, int squaredRadius) throws GameActionException{
            Message m = new Message(tag, robot.location, robot.ID);
            reportFixedObject(m, knowns, squaredRadius);
        }
        
        //Report Parts
        public static void reportParts(MapLocation loc, ArrayList<MapLocation> knowns, int squaredRadius) throws GameActionException{
            Message m = new Message(MessageTags.PART, loc, 0);
            reportFixedObject(m, knowns, squaredRadius);
        }    
        
        // Report enemy clusters if a new one is found
        public static void reportEnemyCluster(MapLocation loc, int threatLevel, ArrayList<DecayingMapLocation> knowns, int squaredRadius) throws GameActionException{
            // Check known unit so we don't add duplicates
            boolean wasTooClose = false;
            for (DecayingMapLocation knownCluster : knowns) {
                int clusterSeparation = knownCluster.location.distanceSquaredTo(loc);
                if (clusterSeparation < minClusterSeparation) {
                    wasTooClose = true;
                    // If it was a duplicate, go to next object and don't broadcast
                    return;
                }
            }
            // Otherwise we are dealing with a new object.
            // Maximum transmissible information for non-locations
            if(threatLevel > maxID){
                threatLevel = maxID;
            }
            rc.setIndicatorDot(loc, 80, 80, 255);
            DecayingMapLocation cluster = new DecayingMapLocation(loc, threatLevel);
            knowns.add(cluster);
            Message message = new Message(MessageTags.CLUS, cluster.location, cluster.ttl);
            Messaging.sendMessage(message, squaredRadius);
        }

        // Broadcasts a report on all nearby interesting objects.
        public static void report() throws GameActionException {
            int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());

            // Report Zombie dens and enemy Clusters
            int threatLevel = 0;
            int averageEnemyX = 0;
            int averageEnemyY = 0;
            for (RobotInfo robot : nearbyEnemies) {
                switch(robot.type){
                    case ZOMBIEDEN:
                        reportFixedRobot(MessageTags.ZDEN, robot, knownDens, distToNearestArchon);
                        break;
                    case STANDARDZOMBIE:
                    case BIGZOMBIE:
                    case FASTZOMBIE:
                    case RANGEDZOMBIE:
                    case SOLDIER:
                    case GUARD:
                    case VIPER:
                        threatLevel += 1;
                        averageEnemyX += robot.location.x;
                        averageEnemyY += robot.location.y;
                        break;
                    default:
                        break;
                }
            }
            // If we detected any enemies, then report their cluster location
            if(nearbyEnemies.length != 0){
                averageEnemyX = averageEnemyX / nearbyEnemies.length;
                averageEnemyY = averageEnemyY / nearbyEnemies.length;
                MapLocation loc = new MapLocation(averageEnemyX, averageEnemyY);
                reportEnemyCluster(loc, threatLevel, knownEnemyClusters, distToNearestArchon);
            }
            
            for (RobotInfo robot : nearbyNeutrals) {
                reportFixedRobot(MessageTags.NEUT, robot, knownNeutrals, distToNearestArchon);
            }
            
            for (MapLocation loc : knownParts){
                reportParts(loc, knownParts, distToNearestArchon);
            }
        }
       
    }
    
    public static void tick() throws GameActionException {
        ScoutMessaging.handleMessageQueue();
        if (Util.countRobotsByRobotType(nearbyEnemies, RobotType.ARCHON) > 0 && broadCastCooldown > 0) {
            for (RobotInfo r : nearbyEnemies) {
                if (r.type.equals(RobotType.ARCHON)) {
                    broadCastCooldown += incurredCooldownPerBroadcast;
                    int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());
                    Message message = new Message(MessageTags.EARL, r.location, r.ID);
                    Messaging.sendMessage(message, distToNearestArchon);
                    rc.setIndicatorString(2, "I transmitted Enemy Archon Location this turn: " + r.location.toString());
                    break;
                }
            }
        } else if (broadCastCooldown > 0) {
            broadCastCooldown--;
        }

        Exploration.tryExplore();

        // If we have found every bound
        if (numExploredDirections == 4 || allBoundsSet == true) {
            ScoutReporting.report();
        }

    }
}
