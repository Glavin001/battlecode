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
import battlecode.instrumenter.inject.System;
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
    static ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();;
    static int numKnownDens = 0;
    // Rounds until we are allowed to broadcast again
    static int broadCastCooldown = 0;
    static int incurredCooldownPerBroadcast = 40;

    // Enclosure for all of the exploration functions
    static class Exploration {

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
            //Send north bound
            Message message = new Message(MessageTags.SMBN, new MapLocation(rc.getLocation().x, northBound));
            message.send(rc, distToNearestArchon);
            //East
            message = new Message(MessageTags.SMBE, new MapLocation(eastBound, rc.getLocation().y));
            message.send(rc, distToNearestArchon);
            //South
            message = new Message(MessageTags.SMBS, new MapLocation(rc.getLocation().x, southBound));
            message.send(rc, distToNearestArchon);
            //West
            message = new Message(MessageTags.SMBW, new MapLocation(westBound, rc.getLocation().y));
            message.send(rc, distToNearestArchon);
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
//                explore(myLocation.directionTo(nearestArchon).opposite());
            }
        }
    }

    static class ScoutMessaging {

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
                    case MessageTags.AMBN:
                        // Set map bounds
                        setMapBound(Direction.NORTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBE:
                    case MessageTags.AMBE:
                        setMapBound(Direction.EAST, message.getLocation().x);
                        break;
                    case MessageTags.SMBS:
                    case MessageTags.AMBS:
                        setMapBound(Direction.SOUTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBW:
                    case MessageTags.AMBW:
                        setMapBound(Direction.WEST, message.getLocation().x);
                        break;

                    }

                }
            }

        }

    }

    public static void reportDens() throws GameActionException {
        int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());

        for (RobotInfo robot : nearbyEnemies) {
            // Also check if the den exists in out list of knownDens
            if (robot.type == RobotType.ZOMBIEDEN) {
                // Check known dens so we don't add duplicates
                boolean wasDuplicate = false;
                for (MapLocation den : knownDens) {
                    if ((den.x == robot.location.x && den.y == robot.location.y)) {
                        wasDuplicate = true;
                        continue;
                    }
                }
                // If it was a duplicate, go to next robot and don't broadcast
                if (wasDuplicate) {
                    continue;
                } else {
                    // Otherwise we are dealing with a new den.
                    knownDens.add(robot.location);
                }
                Message message = new Message(MessageTags.ZDEN, robot.location, robot.ID);
                message.send(rc, distToNearestArchon);
//                rc.setIndicatorString(2, "I transmitted denLocation this turn");
            }
        }
    }

    public static void tick() throws GameActionException {
        ScoutMessaging.handleMessageQueue();
        if (Util.countRobotsByRobotType(nearbyEnemies, RobotType.ARCHON) > 0 && broadCastCooldown > 0) {
            for (RobotInfo r : nearbyEnemies) {
                if (r.type.equals(RobotType.ARCHON)) {
                    broadCastCooldown += incurredCooldownPerBroadcast ;
                    int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());
                    Message message = new Message(MessageTags.EARL, r.location, r.ID);
                    message.send(rc, distToNearestArchon);
                    rc.setIndicatorString(2, "I transmitted Enemy Archon Location this turn: "+r.location.toString());
                    break;
                }
            }
        } else if (broadCastCooldown > 0) {
            broadCastCooldown--; 
        }

        Exploration.tryExplore();
        
        // If we have found every bound
        if (numExploredDirections == 4 || allBoundsSet == true) {
            reportDens();
        }

    }
}
