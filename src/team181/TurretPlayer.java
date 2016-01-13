package team181;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import team181.RobotPlayer.Sensing;

public class TurretPlayer extends RobotPlayer {

    public static void tick() throws GameActionException {
        // If this robot type can attack, check for enemies within range
        // and attack one
        if (rc.isWeaponReady()) {
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

            }
        } else {
            rc.setIndicatorString(2, "Weapon not ready");
        }
    }

}
