package team181_alpha2;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team181_alpha2.RobotPlayer.Messaging;
import team181_alpha2.RobotPlayer.Sensing;

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
                    rc.setIndicatorString(2, "Attacking Enemy at " + enemy.location.toString());
                    return;
                }
            } else if (attackableZombies.length > 0) {
                RobotInfo zombie = bestRobotToAttack(attackableZombies);
                if (rc.canAttackLocation(zombie.location)) {
                    rc.attackLocation(zombie.location);
                    rc.setIndicatorDot(zombie.location, 255, 10, 10);
                    rc.setIndicatorString(2, "Attacking Zombie at " + zombie.location.toString());
                    return;
                }
            } else {
                rc.setIndicatorString(2, "Nothing to attack");
            }
        } else {
            rc.setIndicatorString(2, "Weapon not ready");
        }

        // Not currently attacking
        if (nearbyEnemies.length == 0) {
            // No enemies nearby!
            // Can I move to nearest ally archon?
            if (nearestArchon != null) {
                // I know where he is!
                // Is he close?
                rc.setIndicatorLine(myLocation, nearestArchon, 0, 100, 100);
                int dist = myLocation.distanceSquaredTo(nearestArchon);
                if (dist > maxArchonDistSquared) {
                    // Too far! Let's move closer
                    rc.setIndicatorString(2, "Too far from Ally Archon");
                    // Can we move?
                    if (myRobotType.equals(RobotType.TURRET)) {
                        // Nope, can't move
                        rc.pack();
                        return;
                    } else {
                        // Yup, we can move
                        // Move closer!
                        if (explore(myLocation.directionTo(nearestArchon))) {
                            return;
                        }
                    }
                } else if (dist < minArchonDistSquared) {
                    // too close! Give the archon some space!
                    rc.setIndicatorString(2, "Too close to Ally Archon");
                    // Can we move?
                    if (myRobotType.equals(RobotType.TURRET)) {
                        // Nope, can't move
                        rc.pack();
                        return;
                    } else {
                        // Yup, we can move
                        // Move away!
                        if (explore(myLocation.directionTo(nearestArchon).opposite())) {
                            return;
                        }
                    }
                } else {
                    // All good
                    // Are we ready to attack?
                    if (myRobotType.equals(RobotType.TTM)) {
                        // Nope, need to unpack
                        rc.unpack();
                        return;
                    } else {
                        // Ready to kick some butt!
                    }
                }

            }
        } else {
            // Enemies!
            // Are we ready to attack?
            if (myRobotType.equals(RobotType.TTM)) {
                // Nope, we need to unpack
                rc.unpack();
                return;
            }
        }

    }
    
    public static boolean explore(Direction dirToMove) throws GameActionException {
        if (rc.isCoreReady()) {
            Direction bestDir = leastRiskyDirection(dirToMove);
            if (!bestDir.equals(Direction.NONE)) {
                rc.move(bestDir);
                return true;
            }
        }
        return false;
    }

}
