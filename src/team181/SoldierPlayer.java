package team181;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.Team;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Sensing;

public class SoldierPlayer extends RobotPlayer {

    static double retreatHealthPercent = 0.5;
    
    public static void tick() throws GameActionException {
        Messaging.handleMessageQueue();

        // If there are more enemies than allies nearby, retreat to the
        // nearest archon.
        if (shouldFlee()) {
            Movement.retreatToArchon();
        }

        // Retreat if our health is less than retreatHealthPercent
        if (rc.getHealth() < (myRobotType.maxHealth * retreatHealthPercent)) {
            Movement.retreatToArchon();
            rc.setIndicatorString(2, "Retreating to archon @: " + nearestArchon.toString());
        }

        // This is redundant checking...
        if (attackableTraitors.length > 0) {
            Offensive.attack(enemyTeam);
            rc.setIndicatorString(0, "Attacking Traitors");
        } else if (attackableZombies.length > 0) {
            Offensive.attack(Team.ZOMBIE);
            rc.setIndicatorString(0, "Attacking Zombies");
        } else {
            Movement.moveToClosestEnemy();
            rc.setIndicatorString(0, "Moving");
        }
        
    }
    
}
