package team181;

import battlecode.common.Clock;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import team181.RobotPlayer.Sensing;

public class TurretClass extends RobotPlayer {

    public static void run() {
        while (true) {
            try {
                // If this robot type can attack, check for enemies within range
                // and attack one
                Sensing.updateNearbyEnemies();
                if (rc.isWeaponReady()) {
                    if (attackableTraitors.length > 0) {
                        for (RobotInfo enemy : attackableTraitors) {
                            // Check whether the enemy is in a valid attack
                            // range (turrets have a minimum range)
                            if (rc.canAttackLocation(enemy.location)) {
                                rc.attackLocation(enemy.location);
                                rc.setIndicatorDot(enemy.location, 255, 10, 10);
                                rc.setIndicatorString(2, "Attacking Enemy at "+enemy.location.toString());
                                break;
                            }
                        }
                    } else if (attackableZombies.length > 0) {
                        for (RobotInfo zombie : attackableZombies) {
                            if (rc.canAttackLocation(zombie.location)) {
                                rc.attackLocation(zombie.location);
                                rc.setIndicatorDot(zombie.location, 255, 10, 10);
                                rc.setIndicatorString(2, "Attacking Zombie at "+zombie.location.toString());
                                break;
                            }
                        }
                    }
                } else {
                    rc.setIndicatorString(2, "Weapon not ready");
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
