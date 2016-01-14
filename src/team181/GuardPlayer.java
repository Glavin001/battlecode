package team181;

import battlecode.common.GameActionException;
import battlecode.common.Team;
import team181.Debug;
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
