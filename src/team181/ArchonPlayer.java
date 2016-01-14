package team181;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import team181.CommUtil.MessageTags;
import team181.RobotPlayer.Messaging;

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

                    Message message = new Message(signal);
                    MapLocation loc = signal.getLocation();
                    Message relayMessage;
                    switch (message.getTag()) {
                    // Handle Scout messages about map bounds
                    case MessageTags.SMBN:
                        // Propagate the message to nearby scouts and archons
                        relayMessage = new Message(MessageTags.AMBN, message.getLocation());
                        Messaging.sendMessage(relayMessage, defaultBroadcastRange);
                        setMapBound(Direction.NORTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBE:
                        relayMessage = new Message(MessageTags.AMBE, message.getLocation());
                        Messaging.sendMessage(relayMessage, defaultBroadcastRange);
                        setMapBound(Direction.EAST, message.getLocation().x);
                        break;
                    case MessageTags.SMBS:
                        relayMessage = new Message(MessageTags.AMBS, message.getLocation());
                        Messaging.sendMessage(relayMessage, defaultBroadcastRange);
                        setMapBound(Direction.SOUTH, message.getLocation().y);
                        break;
                    case MessageTags.SMBW:
                        relayMessage = new Message(MessageTags.AMBW, message.getLocation());
                        Messaging.sendMessage(relayMessage, defaultBroadcastRange);
                        setMapBound(Direction.WEST, message.getLocation().x);
                        break;

                    // Handle reporting of zombie dens
                    case MessageTags.ZDEN:
                        storeDenLocation(message.getLocation());
                        break;

                    }

                }
            }

        }

        public static void storeDenLocation(MapLocation loc) {
            for (MapLocation den : knownDens) {
                // Don't bother to add dens we already know about
                if (den.x == loc.x && den.y == loc.y) {
                    return;
                }
            }
            // Create a new map location at the reported spot.
            knownDens.add(loc);
            // System.out.println("I found a den this turn at: " +
            // denLoc.toString());
        }

        public static void broadcastMapBounds() throws GameActionException {

            // Send northBound
            Message message = new Message(MessageTags.AMBN, new MapLocation(0, northBound));
            Messaging.sendMessage(message, defaultBroadcastRange);
            // East
            message = new Message(MessageTags.AMBE, new MapLocation(eastBound, 0));
            Messaging.sendMessage(message, defaultBroadcastRange);
            // South
            message = new Message(MessageTags.AMBS, new MapLocation(0, southBound));
            Messaging.sendMessage(message, defaultBroadcastRange);
            // West
            message = new Message(MessageTags.AMBW, new MapLocation(westBound, 0));
            Messaging.sendMessage(message, defaultBroadcastRange);
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
                            Message message = new Message(MessageTags.NAAL);
                            Messaging.sendMessage(message, broadcastDistance);
                            if (nearestEnemyArchon != null) {
                                // Broadcast where enemy archon is
                                message = new Message(MessageTags.EARL, nearestArchon);
                                Messaging.sendMessage(message, broadcastDistance);
                                // System.out.println("Broadcasting enemy
                                // archon: "+nearestEnemyArchon.toString());
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
            // System.out.println("unknown enemy archon location");
        }
        
        for (MapLocation loc : knownDens) {
            rc.setIndicatorDot(loc, 180, 80, 180);
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
                rc.setIndicatorLine(myLocation, loc, 50, 255, 50);
                ;
                return;
            }
        }

        // Move randomly!
        // Movement.randomMove();

    }

    /**
     * Determine what robot to build next
     * 
     * @return The type of robot to build next
     */
    static RobotType nextRobotTypeToBuild() {
        int roundNumber = rc.getRoundNum();
        // Build Scouts at the start
        if (roundNumber < 20) {// This is not valid at start
            return RobotType.GUARD;
        } else if (roundNumber < 80
                || (roundNumber > 120 && Util.countRobotsByRobotType(nearbyAllies, RobotType.TURRET) < 3)) {
            // If there are no enemies, we do not need offensive yet
//            if (nearbyEnemies.length > 0) {
                return RobotType.TURRET;
//            } else {
//                return RobotType.TTM;
//            }
        } else if (roundNumber > 120 && Util.countRobotsByRobotType(nearbyAllies, RobotType.GUARD) < 2) {
            return RobotType.GUARD;
        } else if (roundNumber < 120) {
            return RobotType.SCOUT;
        } else if (roundNumber > 150) {
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
