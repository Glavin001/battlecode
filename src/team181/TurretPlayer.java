package team181;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Sensing;

/**
 * Turret player
 *
 */
public class TurretPlayer extends RobotPlayer {

    protected static int minArchonDistSquared = 4;
    protected static int maxArchonDistSquared = RobotType.TURRET.sensorRadiusSquared - 1;

    public static void tick() throws GameActionException {
        Messaging.handleMessageQueue();
        // Update robot type
        myRobotType = rc.getType();

        // If this robot type can attack, check for enemies within range
        // and attack one
        if (myRobotType.equals(RobotType.TURRET) && rc.isWeaponReady()) {
            if (attackableTraitors.length > 0) {
                RobotInfo enemy = bestRobotToAttack(attackableTraitors);
                // Check whether the enemy is in a valid attack
                // range (turrets have a minimum range)
                if (rc.canAttackLocation(enemy.location)) {
                    rc.attackLocation(enemy.location);
                    rc.setIndicatorDot(enemy.location, 255, 10, 10);
                    rc.setIndicatorString(1, "Attacking Enemy at " + enemy.location.toString());
                }
            } else if (attackableZombies.length > 0) {
                RobotInfo zombie = bestRobotToAttack(attackableZombies);
                if (rc.canAttackLocation(zombie.location)) {
                    rc.attackLocation(zombie.location);
                    rc.setIndicatorDot(zombie.location, 255, 10, 10);
                    rc.setIndicatorString(1, "Attacking Zombie at " + zombie.location.toString());
                }
            } else {
                rc.setIndicatorString(1, "Nothing to attack");
            }
        } else {
            rc.setIndicatorString(1, "Weapon not ready");
        }

        // Not currently attacking
        if (nearbyEnemies.length == 0) {
            // No enemies nearby!
            // Can I move to nearest ally archon?
            if (nearestAllyArchon != null) {
                // I know where he is!
                // Is he close?
                rc.setIndicatorLine(myLocation, nearestAllyArchon, 0, 100, 100);
                int dist = myLocation.distanceSquaredTo(nearestAllyArchon);
                if (dist > maxArchonDistSquared) {
                    // Too far! Let's move closer
                    rc.setIndicatorString(1, "Too far from Ally Archon");
                    // Can we move?
                    if (myRobotType.equals(RobotType.TURRET)) {
                        // Nope, can't move
                        rc.pack();
                    } else {
                        // Yup, we can move
                        // Move closer!
                        explore(myLocation.directionTo(nearestAllyArchon));
                    }
                } else if (dist < minArchonDistSquared) {
                    // too close! Give the archon some space!
                    rc.setIndicatorString(1, "Too close to Ally Archon");
                    // Can we move?
                    if (myRobotType.equals(RobotType.TURRET)) {
                        // Nope, can't move
                        rc.pack();
                    } else {
                        // Yup, we can move
                        // Move away!
                        explore(myLocation.directionTo(nearestAllyArchon).opposite());
                    }
                } else {
                    // All good
                    rc.setIndicatorString(1, "Good distance away from Ally Archon");
                    // Are we ready to attack?
                    if (myRobotType.equals(RobotType.TTM)) {
                        // Nope, need to unpack
                        rc.unpack();
                    } else {
                        // Ready to kick some butt!
                    }
                }

            }
        } else {
            // Enemies!
            rc.setIndicatorString(1, "Enemies! Prepare to attack!");
            // Are we ready to attack?
            if (myRobotType.equals(RobotType.TTM)) {
                // Nope, we need to unpack
                rc.unpack();
            }
        }

    }
    
    public static boolean explore(Direction dirToMove) throws GameActionException {
        if (rc.isCoreReady()) {
            Direction bestDir = leastRiskyDirection(dirToMove);
            if (!bestDir.equals(Direction.NONE)) {
                rc.setIndicatorString(2, "Moving in direction: "+bestDir.toString());
                rc.move(bestDir);
                return true;
            }
        }
        return false;
    }

}
