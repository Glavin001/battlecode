package lectureplayer1;

import battlecode.common.*;

public class RobotPlayer {
	static Direction movingDirection = Direction.NORTH_EAST;
	static RobotController rc;
	
	public static void run(RobotController rcIn) {
		rc = rcIn;
		if (rc.getTeam() == Team.B) {
			movingDirection = Direction.SOUTH_WEST;
		}
		while (true) {
			try {
				repeat();
				Clock.yield();
			} catch (GameActionException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void repeat() throws GameActionException {
		RobotInfo[] zombieEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, Team.ZOMBIE);
		RobotInfo[] normalEnemies = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, rc.getTeam().opponent());
		RobotInfo[] opponentEnemies = Utility.joinRobotInfo(zombieEnemies, normalEnemies);
		
		if (opponentEnemies.length > 0 && rc.getType().canAttack()) {
			if (rc.isWeaponReady()) {
				rc.attackLocation(opponentEnemies[0].location);
			}
		} else {
			if (rc.isCoreReady()) {
				Utility.forwardish(movingDirection);
			}
		}
		
	}
}
