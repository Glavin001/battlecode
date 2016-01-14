package team181;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.Team;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Movement;
import team181.RobotPlayer.Offensive;
import team181.RobotPlayer.Sensing;

/**
 * Guard Player code
 *
 */
public class GuardPlayer extends SoldierPlayer {
    //probably will not need cause guards are already at archon
    static double retreatHealthPercent = 0.0;
    
    //this is distance squared
    static final int MAX_DISTANCE_FROM_ARCON = 25;
    static final int MIN_DISTANCE_FROM_ARCON = 1;

    
    public static void tick() throws GameActionException {
        Messaging.handleMessageQueue();

        // If there are more enemies than allies nearby, retreat to the
        // nearest archon.
        if (shouldFlee()) {
            retreatToArchon();
        }

        // Retreat if our health is less than retreatHealthPercent
        if (rc.getHealth() < (myRobotType.maxHealth * retreatHealthPercent)) {
            retreatToArchon();
        }

        // This is redundant checking...
        if (nearbyEnemies.length > 0) {
            if (attackableTraitors.length > 0) {
                Offensive.attack(enemyTeam);
                rc.setIndicatorString(0, "Attacking Traitors");
            } else if (attackableZombies.length > 0) {
                Offensive.attack(Team.ZOMBIE);
                rc.setIndicatorString(0, "Attacking Zombies");
            } else {
                Movement.randomMoveAroundArcon(MIN_DISTANCE_FROM_ARCON, MAX_DISTANCE_FROM_ARCON);
                rc.setIndicatorString(0, "Moving randomly");
            }
        } else {
            Movement.randomMoveAroundArcon(MIN_DISTANCE_FROM_ARCON, MAX_DISTANCE_FROM_ARCON);
            rc.setIndicatorString(0, "Moving randomly");
        }
    }
    
    
}
