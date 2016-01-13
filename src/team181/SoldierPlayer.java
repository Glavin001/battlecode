package team181;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Sensing;

public class SoldierPlayer extends RobotPlayer {

    static double retreatHealthPercent = 0.4;
    static int idealAllyDist = 4;
    
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
                // Enemies in sight, outside of attacking range
                Movement.moveToClosestEnemy();
                rc.setIndicatorString(0, "Moving towards closest enemy");
            }
        } else {
            // no enemies in sight
            // guest where enemy is
//          explore(myLocation.directionTo(nearestArchon).opposite());
            if (nearbyAllies.length > 0) {
                RobotInfo closestAlly = Util.closestRobot(myLocation, nearbyAllies);
                int dist = myLocation.distanceSquaredTo(closestAlly.location);
                if (dist > idealAllyDist) {
                    Movement.moveToClosestAlly();
                    rc.setIndicatorString(0, "Moving towards closest ally");
                    return;
                }
            }
            Movement.randomMove();
            rc.setIndicatorString(0, "Moving randomly");
        }
        
    }
    
    public static void retreatToArchon() throws GameActionException {
        if (nearestArchon != null && myLocation.distanceSquaredTo(nearestArchon) <= RobotType.ARCHON.attackRadiusSquared) {
            // Enemies in sight, outside of attacking range
            Movement.moveToClosestEnemy();
            rc.setIndicatorString(2, "Moving towards closest enemy");
        } else {
            // Not close to Archon yet
            Movement.retreatToArchon();
            rc.setIndicatorString(2, "Retreating to archon @: " + nearestArchon.toString());
        }
    }
    
}
