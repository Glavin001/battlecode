package team001;

import battlecode.common.*;


//Base class for all players
public class Robots {
	public static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	
	public static RobotController rc;
	public static MapLocation here;
	public static MapLocation goal;
	public static MapLocation startBase;
	
	static void init(RobotController theRC) throws GameActionException {
		rc = theRC;
		here = rc.getLocation();
		startBase = rc.getLocation();
		setNextGoalLocation(null);
	}
	
//	public static Direction getNextDirection(MapLocation currentLocation, MapLocation  goal) {
//		
//	}
	
	public static void checkIfFlee(RobotInfo[] alliesWithinRange, RobotInfo[] enemysWithinRange, RobotInfo[] zombiesWithinRange) {
		double[] alliePower = scoreRobots(alliesWithinRange);
		double[] enemyPower = scoreRobots(enemysWithinRange);
		double[] zombiePower = scoreRobots(zombiesWithinRange);
		if (alliePower[0]  - enemyPower[1] - zombiePower[1] < enemyPower[0] +  zombiePower[0]  - alliePower[1]) {
			goal = startBase;
		}
	}
	
	private static double[] scoreRobots(RobotInfo[] robots) {
		double[] score = {0,0};
		for (RobotInfo robot : robots) {
			score[0] += robot.attackPower;
			score[1] += robot.health;
		}
		return score;
	}
	
	public static void setNextGoalLocation(MapLocation nextGoal) {
		if (nextGoal == null) {
			int x = GameConstants.MAP_MAX_WIDTH - here.x,
				y = GameConstants.MAP_MAX_HEIGHT - here.y;
			goal = new MapLocation(x,y);
		} else {
			goal = nextGoal;
		}
		
	}

}
