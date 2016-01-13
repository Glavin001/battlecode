package team181;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.messageConstants;

import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Archon player code
 *
 */
public class ArchonPlayer extends RobotPlayer {

    static RobotType[] unitsToBuild = { RobotType.GUARD,
            RobotType.SOLDIER, /* RobotType.VIPER */ };
    static int nextUnitToBuild = rand.nextInt(unitsToBuild.length);
    static boolean builtLastUnit = false;

    static int defaultBroadcastRange = 2000;
    // Are we waiting on a scout to complete a message?
    static Map<Integer, Boolean> waitingMessageID;
    // What was the last message the scout sent?
    static Map<Integer, Integer> lastTransmissionID;
    
    static ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();

    static class ArchonMessaging {

        public static void handleMessageQueue() throws GameActionException {
            // SUPER
            Messaging.handleMessageQueue();
            // currentSignals[] is now set for this round. Overflow may cause
            // problems.
            if (currentSignals.length > 0) {
                for (Signal signal : currentSignals) {
                    // Make sure this is our team's message
                    if (signal.getTeam() != rc.getTeam()) {
                        continue;
                    }

                    MapLocation loc = signal.getLocation();
                    int msg1 = signal.getMessage()[0];
                    int msg2 = signal.getMessage()[1];
                    int id = signal.getID();
                    switch (msg1) {
                    // Handle Scout messages about map bounds
                    case MessageTags.SMBN:
                        // Propagate the message to nearby scouts and archons
                        rc.broadcastMessageSignal(MessageTags.AMBN, msg2, defaultBroadcastRange);
                        // Set map bounds
                        msg2 = Messaging.adjustBound(msg2);
                        setMapBound(Direction.NORTH, msg2);
                        break;
                    case MessageTags.SMBE:
                        rc.broadcastMessageSignal(MessageTags.AMBE, msg2, defaultBroadcastRange);
                        msg2 = Messaging.adjustBound(msg2);
                        setMapBound(Direction.EAST, msg2);
                        break;
                    case MessageTags.SMBS:
                        rc.broadcastMessageSignal(MessageTags.AMBS, msg2, defaultBroadcastRange);
                        msg2 = Messaging.adjustBound(msg2);
                        setMapBound(Direction.SOUTH, msg2);
                        break;
                    case MessageTags.SMBW:
                        rc.broadcastMessageSignal(MessageTags.AMBW, msg2, defaultBroadcastRange);
                        msg2 = Messaging.adjustBound(msg2);
                        setMapBound(Direction.WEST, msg2);
                        break;

                    // Handle reporting of zombie dens
                    case MessageTags.DENX:
                        storeDenLocation(msg2, id, MessageTags.DENX);
                        break;
                    case MessageTags.DENY:
                        msg2 = Messaging.adjustBound(msg2);
                        storeDenLocation(msg2, id, MessageTags.DENY);
                        break;
                        
                    }

                }
            }

        }

        public static void storeDenLocation(int packedCoordinate, int id, int denXOrDenY) {
            // Unpackage map coordinate.
            int coordinate = Messaging.adjustBound(packedCoordinate);
            // If we have an entry for this scout
            if (waitingMessageID.containsKey(id)) {
                // If we had DENY and just got DENX, complete
                if (waitingMessageID.get(id)) {
                    int oldCoordinate = lastTransmissionID.get(id);
                    // We are no longer waiting on this scout.
                    waitingMessageID.put(id, false);
                    // Check if the den already exists in our list, return if it
                    // does
                    for (MapLocation den : knownDens) {
                        if ((den.x == coordinate && den.y == oldCoordinate)
                                || (den.y == coordinate && den.x == oldCoordinate)) {
                            return;
                        }
                    }
                    // Create a new map location at the reported spot.
                    MapLocation denLoc;
                    if (denXOrDenY == MessageTags.DENX) {
                        denLoc = new MapLocation(coordinate, oldCoordinate);
                    } else {
                        denLoc = new MapLocation(oldCoordinate, coordinate);
                    }
                    knownDens.add(denLoc);
//                    System.out.println("I found a den this turn at: " + denLoc.toString());
                }
            } else {
                // Otherwise put in a new entry
                waitingMessageID.put(id, true);
                lastTransmissionID.put(id, coordinate);
            }
        }

        public static void broadcastMapBounds() throws GameActionException {
            rc.broadcastMessageSignal(MessageTags.AMBN, Messaging.adjustBound(northBound), defaultBroadcastRange);
            rc.broadcastMessageSignal(MessageTags.AMBE, Messaging.adjustBound(eastBound), defaultBroadcastRange);
            rc.broadcastMessageSignal(MessageTags.AMBS, Messaging.adjustBound(southBound), defaultBroadcastRange);
            rc.broadcastMessageSignal(MessageTags.AMBW, Messaging.adjustBound(westBound), defaultBroadcastRange);
        }
    }

    static class Building {
        // Tries to build a given unit in a random adjacent location.
        // Returns true if the unit was successfully build, and false otherwise.
        // Also broadcasts a signal to all nearby robots in a set range with its
        // location.
        public static boolean tryBuildUnit(RobotType rt) throws GameActionException {

            int broadcastDistance = 80;

            if (rc.isCoreReady()) {
                RobotType typeToBuild = rt;
                if (rc.hasBuildRequirements(typeToBuild)) {
                    rc.setIndicatorString(1, "Building: " + typeToBuild.toString());
                    Direction dirToBuild = directions[rand.nextInt(8)];
                    // Build in the same direction as your enemies
                    if (nearbyEnemies.length > 0) {
                        dirToBuild = myLocation.directionTo(nearbyEnemies[0].location);
                    }
                    for (int i = 0; i < 8; i++) {
                        // If possible, build in this direction
                        if (rc.canBuild(dirToBuild, typeToBuild)) {
                            rc.build(dirToBuild, typeToBuild);
                            // Broadcast to nearby units our location, so they
                            // save it.
                            // Just a note that broadcasting increases core
                            // delay, try to minimize it!
                            rc.broadcastMessageSignal(MessageTags.NAAL, (int) 'A', broadcastDistance);
                            if (nearestEnemyArchon != null) {
                                // Broadcast where enemy archon is
                                rc.broadcastMessageSignal(MessageTags.EALX, nearestEnemyArchon.x, broadcastDistance);
                                rc.broadcastMessageSignal(MessageTags.EALY, nearestEnemyArchon.y, broadcastDistance);
//                                System.out.println("Broadcasting enemy archon: "+nearestEnemyArchon.toString());
                            }
                            // If it is a scout, tell it the new bounds that we
                            // already know
                            if (typeToBuild == RobotType.SCOUT && allBoundsSet) {
                                ArchonMessaging.broadcastMapBounds();
                            }
                            builtLastUnit = true;
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

    public static void defendMe() {
        // If we somehow have many nearby enemies and no allies, have everyone
        // retreat.
        if (nearbyEnemies.length > 0) {
            // HELP ME
        }
    }

    public static void initialize() {
        waitingMessageID = new HashMap<Integer, Boolean>();
        lastTransmissionID = new HashMap<Integer, Integer>();
    }

    public static void tick() throws GameActionException {
        ArchonMessaging.handleMessageQueue();
        
        rc.setIndicatorString(2, "All bounds known?: " + " " + Boolean.toString(allBoundsSet)
                + Integer.toString(northBound) + "was nb and eb is: " + Integer.toString(eastBound));
        rc.setIndicatorString(2, "Number of known dens: " + Integer.toString(knownDens.size()));
        if (nearestEnemyArchon != null) {
            rc.setIndicatorString(2, "Enemy Archon location: " + nearestEnemyArchon);
        } else {
//            System.out.println("unknown enemy archon location");
        }
        if (!knownDens.isEmpty()) {
            int fate = rand.nextInt(knownDens.size());
            rc.setIndicatorString(2, "Den: " + Integer.toString(fate + 1) + " " + knownDens.get(fate).toString());
        }

        // Priorities
        // Building
        if (Building.tryBuildUnit(nextRobotTypeToBuild())) {
            return;
        }
        
        // Survival
        if (shouldFlee()) {
            if (rc.isCoreReady()) {
                Direction dirToMove = Direction.NONE;
                if (nearbyAllies.length > 0) {
                    // Get closest ally
                    MapLocation closestAllyLocation = Util.closestRobot(myLocation, nearbyAllies).location;
                    // Direction to closest ally
                    dirToMove = myLocation.directionTo(closestAllyLocation);
                }
                Direction bestDir = leastRiskyDirection(dirToMove);
                if (!bestDir.equals(Direction.NONE)) {
                    rc.move(bestDir);
                    return;
                }
            }
        }
        // Activating Neutrals
        RobotInfo[] neutralRobots = rc.senseNearbyRobots(-1, Team.NEUTRAL);
        if (neutralRobots.length > 0) {
            if (activateNeutrals(neutralRobots)) {
                return;
            }
        }

        // Repairing
        if (repairAllies(nearbyAllies)) {
            return;
        }
        // Pick up Parts
        if (retrieveParts()) {
            return;
        }

        if (neutralRobots.length > 0 && rc.isCoreReady()) {
            // Move to closest
            RobotInfo neutralRobot = Util.closestRobot(myLocation, neutralRobots);
            MapLocation loc = neutralRobot.location;
            Direction dirToMove = myLocation.directionTo(loc);
            Direction bestDir = leastRiskyDirection(dirToMove);
            if (!bestDir.equals(Direction.NONE)) {
                rc.move(bestDir);
                rc.setIndicatorString(1, "Moving towards neutral robot: " + Integer.toString(neutralRobot.ID));
                rc.setIndicatorLine(myLocation, loc, 50, 255, 50);;
                return;
            }
        }

        // Move randomly!
        //Movement.randomMove();

    }

    /**
     * Determine what robot to build next
     * 
     * @return The type of robot to build next
     */
    static RobotType nextRobotTypeToBuild() {
        int roundNumber = rc.getRoundNum();
        // Build Scouts at the start
        if (roundNumber < 20) {//This is not valid at start 
            return RobotType.GUARD;
        } else if (roundNumber < 80
                || (roundNumber > 120 && Util.countRobotsByRobotType(nearbyAllies, RobotType.TURRET) < 3)) {
            return RobotType.TURRET;
        } else if (roundNumber > 120 && Util.countRobotsByRobotType(nearbyAllies, RobotType.GUARD) < 2) {
            return RobotType.GUARD;
        } else if (roundNumber < 120) {
            return RobotType.SCOUT;
        } else if(roundNumber > 150) {
            return RobotType.SOLDIER;
        } else {
            return RobotType.GUARD;
        }

    }

    public static boolean retrieveParts() throws GameActionException {
        if (rc.isCoreReady()) {
            // Find all part locations
            MapLocation[] partLocations = rc.sensePartLocations(-1);
            if (partLocations.length > 0) {
                // Determine best part
                MapLocation loc = bestLocationForParts(partLocations);
                Direction dirToMove = myLocation.directionTo(loc);
                Direction bestDir = leastRiskyDirection(dirToMove);
                if (!bestDir.equals(Direction.NONE)) {
                    rc.move(bestDir);
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean activateNeutrals(RobotInfo[] neutralRobots) throws GameActionException {
        if (rc.isCoreReady()) {
            // Find all neutral locations
            for (RobotInfo neutralRobot : neutralRobots) {
                // Any adjacent?
                if (myLocation.isAdjacentTo(neutralRobot.location)) {
                    rc.activate(neutralRobot.location);
                    rc.setIndicatorString(1, "Activating robot: " + Integer.toString(neutralRobot.ID));
                    return true;
                }
            }
        }
        return false;
    }

    public static MapLocation bestLocationForParts(MapLocation[] partLocations) {
        MapLocation bestLoc = partLocations[0];
        int bestDist = myLocation.distanceSquaredTo(bestLoc);
        double bestVal = rc.senseParts(bestLoc);
        int len = partLocations.length;
        for (int i = 1; i < len; i++) {
            MapLocation loc = partLocations[i];
            double val = rc.senseParts(loc);
            int dist = myLocation.distanceSquaredTo(loc);
            if (
            // Optimize for value
            // (val > bestVal) || // Best value
            // (val == bestVal && bestDist > dist)

            // Optimize for distance
            (bestDist > dist) || (bestDist == dist && val > bestVal)

            ) {
                bestVal = val;
                bestLoc = loc;
                bestDist = dist;
            }
        }
        return bestLoc;
    }

    /**
     * Repair nearby allies
     * 
     * @param allies
     * @return 
     * @throws GameActionException
     */
    static boolean repairAllies(RobotInfo[] allies) throws GameActionException {
        for (RobotInfo robot : nearbyAllies) {
            if (robot.health < robot.maxHealth && robot.type != RobotType.ARCHON) {
                rc.setIndicatorDot(robot.location, 80, 255, 80);
                rc.setIndicatorString(1, "Repairing robot: " + Integer.toString(robot.ID));
                // If we are within repair range of the robot, repair
                // it, then break.
                if (robot.location.distanceSquaredTo(rc.getLocation()) <= myAttackRange) {
                    rc.repair(robot.location);
                    return true;
                }
            }
        }
        return false;
    }
}
