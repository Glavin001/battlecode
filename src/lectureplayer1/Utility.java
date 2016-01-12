package lectureplayer1;

import java.util.ArrayList;

import battlecode.common.*;

public class Utility {
	static int[] possibleDirections = new int[] { 0, 1, -1, 2, -2, 3, -3, 4 };
	static ArrayList<MapLocation> pastLocations = new ArrayList<MapLocation>();
	static boolean patient = true;

	public static void forwardish(Direction ahead) throws GameActionException {
		RobotController rc = RobotPlayer.rc;
		for (int i : possibleDirections) {
			Direction canidateDirection = Direction.values()[(ahead.ordinal() + i + 8) % 8];
			MapLocation canidateLocation = rc.getLocation().add(canidateDirection);
			if (patient) {
				if (rc.canMove(canidateDirection) && !pastLocations.contains(canidateDirection)) {
					pastLocations.add(rc.getLocation());
					if (pastLocations.size() > 20) {
						pastLocations.remove(0);
					}
					rc.move(canidateDirection);
					return;
				}
			} else {
				if (rc.canMove(canidateDirection)) {
					rc.move(canidateDirection);
					return;
				} else {
					// DIG
					if (rc.senseRubble(canidateLocation) > GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
						rc.clearRubble(canidateDirection);
						return;
					}
				}
			}
		}
		patient = false;
	}
	
	public static RobotInfo[] joinRobotInfo(RobotInfo[] a, RobotInfo[] b) {
		RobotInfo[] all = new RobotInfo[a.length + b.length];
		int index = 0;
		for (RobotInfo i:a) {
			all[index] = i;
			index++;
		}
		for (RobotInfo i:b) {
			all[index] = i;
			index++;
		}
		return all;
	}
	
}
