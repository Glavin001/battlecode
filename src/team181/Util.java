package team181;

import battlecode.common.*;

/**
 * Helpful Utility methods
 *
 */
public class Util {

    /**
     * Count the total number of robots of a given type
     * 
     * @param robots
     *            Robots to check
     * @param type
     *            The desired robot type to count
     * @return The number of robots that are the desired type
     */
    public static int countRobotsByRobotType(RobotInfo[] robots, RobotType type) {
        RobotType[] types = {type};
        return countRobotsByRobotTypes(robots, types);
    }

    /**
     * 
     * @param robots
     * @param type
     * @return
     */
    public static int countRobotsByRobotTypes(RobotInfo[] robots, RobotType[] types) {
        if (robots.length == 0) {
            return 0;
        } else {
            int count = 0;
            int ilen = robots.length;
            int rlen = types.length;
            for (int i = 0; i < ilen; i++) {
                for (int t = 0; t < rlen; t++) {
                    if (robots[i].type.equals(types[t])) {
                        count++;
                        break; // Break this inner loop
                    }
                }
            }
            return count;
        }
    }

    /**
     * Determine the closest MapLocation to a center location
     * 
     * @param center
     *            Center location to find closest location to
     * @param locations
     *            All locations to check for closest
     * @return Closest location. Will return null if locations is empty.
     */
    public static MapLocation closestMapLocation(MapLocation center, MapLocation[] locations) {
        if (locations.length > 0) {
            MapLocation closest = locations[0];
            int bestDist = closest.distanceSquaredTo(center);
            int len = locations.length;
            for (int i = 1; i < len; i++) {
                MapLocation loc = locations[i];
                int dist = loc.distanceSquaredTo(center);
                if (dist < bestDist) {
                    closest = loc;
                    bestDist = dist;
                }
            }
            return closest;
        } else {
            return null;
        }
    }

    /**
     * Determine the closest Robot to a center location
     * 
     * @param center
     *            Center location to find closest location to
     * @param robots
     *            All robots to check
     * @return Closest robot. Will return null if robots is empty.
     */
    public static RobotInfo closestRobot(MapLocation center, RobotInfo[] robots) {
        if (robots.length > 0) {
            RobotInfo closestRobot = robots[0];
            int bestDist = closestRobot.location.distanceSquaredTo(center);
            int len = robots.length;
            for (int i = 1; i < len; i++) {
                RobotInfo robot = robots[i];
                MapLocation loc = robot.location;
                int dist = loc.distanceSquaredTo(center);
                if (dist < bestDist) {
                    closestRobot = robot;
                    bestDist = dist;
                }
            }
            return closestRobot;
        } else {
            return null;
        }
    }

    /**
     * Join two arrays of RobotInfos together
     * 
     * @param robotsA
     *            First group of robots
     * @param robotsB
     *            Second group of robots
     * @return Combined arrays of RobotInfos
     */
    public static RobotInfo[] joinRobotInfo(RobotInfo[] robotsA, RobotInfo[] robotsB) {
        RobotInfo[] all = new RobotInfo[robotsA.length + robotsB.length];
        int index = 0;
        int len = robotsA.length;
        for (int i = 0; i < len; i++) {
            all[index] = robotsA[i];
            index++;
        }
        len = robotsB.length;
        for (int i = 0; i < len; i++) {
            all[index] = robotsB[i];
            index++;
        }
        return all;
    }
    
    /**
     * Number of turns it will take to clear
     * @param rubble
     * @return
     */
    public static double turnsToClearRubble(double rubble) {
//        Amount left after clearing = max(95%*(original amount)-20,0)
        double turns = rubble / GameConstants.RUBBLE_CLEAR_FLAT_AMOUNT;
        return Math.max(0, turns);
    }
    
    /**
     * Number of turns it will take to clear enough rubble to move through it
     * @param rubble
     * @return
     */
    public static double turnsToClearRubbleToMove(double rubble) {
        return turnsToClearRubble(rubble - GameConstants.RUBBLE_OBSTRUCTION_THRESH);
    }
    
}
