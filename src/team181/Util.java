package team181;

import battlecode.common.*;

public class Util {

    /**
     * Count the total number of robots of a given type
     * 
     * @param robots
     * @param type
     * @return
     */
    public static int countRobotsByRobotType(RobotInfo[] robots, RobotType type) {
        if (robots.length == 0) {
            return 0;
        } else {
            int count = 0;
            for (int i=0; i<robots.length; i++) {
                if (robots[i].type.equals(type)) {
                    count++;
                }
            }
            return count;
        }
    }
    
    /**
     * Determine the closest MapLocation to a center location
     * 
     * @param center
     * @param locations
     * @return Closest location. Will return null if locations is empty.
     */
    public static MapLocation closestMapLocation(MapLocation center, MapLocation[] locations) {
        if (locations.length > 0) {
            MapLocation closest = locations[0];
            int bestDist = closest.distanceSquaredTo(center);
            int len = locations.length;
            for (int i=1; i<len; i++) {
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
     * @param robots
     * @return Closest robot. Will return null if robots is empty.
     */
    public static RobotInfo closestRobot(MapLocation center, RobotInfo[] robots) {
        if (robots.length > 0) {
            RobotInfo closestRobot = robots[0];
            int bestDist = closestRobot.location.distanceSquaredTo(center);
            int len = robots.length;
            for (int i=1; i<len; i++) {
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
    
    
}
