package team181;

import battlecode.common.Clock;
import battlecode.common.GameActionException;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Team;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Movement;
import team181.RobotPlayer.Offensive;

/**
 * Viper player
 *
 */
public class ViperPlayer extends SoldierPlayer {

    public static boolean hasOffensiveAlliesNearby = false;
    public static final int minOffensiveAlliesCount = 1;
    public static double retreatHealthPercent = 0.3;
    public static final int MIN_INFECTED_THRESHOLD = (int) RobotType.VIPER.attackDelay;

    public static void tick() throws GameActionException {
        Messaging.handleMessageQueue();

        attackableEnemies = Util.joinRobotInfo(attackableTraitors, attackableZombies);
        // Check on allies attack power
        hasOffensiveAlliesNearby = offensiveAlliesNearby(nearbyAllies);

        // If there are more enemies than allies nearby, retreat to the
        // nearest archon.
//        if (shouldFlee()) {
//            if (retreatToArchon()) {
//                return;
//            }
//        }

        // Retreat if our health is less than retreatHealthPercent
        if (rc.getHealth() < (myRobotType.maxHealth * retreatHealthPercent)) {
            retreatToArchon();
        }

        // Check for enemies
        if (nearbyEnemies.length > 0) {
            rc.setIndicatorString(0, "Sees nearby enemies");
            // Enemies are within sight
            if (attackableEnemies.length > 0) {
                rc.setIndicatorString(0, "Can attack nearby enemies");
                // Enemies within attack range
                RobotInfo enemy = bestRobotToAttack(attackableEnemies);
                // Attack this dude!
                if (rc.isWeaponReady() && rc.canAttackLocation(enemy.location)) {
                    rc.setIndicatorString(0, "Attacked enemy: "+enemy.toString());
                    rc.attackLocation(enemy.location);
                }
                // Check if this robot is already infected
                if (enemy.viperInfectedTurns != 0 && rc.isCoreReady()) {
                    // Enemy not infected
                    // Check if moving would help attack an not infected enemy
                    RobotInfo infectedRobot = findFirstNotInfectedRobot(nearbyEnemies);
                    if (infectedRobot != null) {
                        // Found non-infected robot!
                        Movement.pathToLocation(infectedRobot.location);
                    } else {
                        // No non-infected robots found!
                        Movement.moveAroundLocation(enemy.location, enemy.type.attackRadiusSquared, myRobotType.attackRadiusSquared);
                    }
                } else {
                    // Enemy was already infected
                    Movement.moveAroundLocation(enemy.location, enemy.type.attackRadiusSquared, myRobotType.attackRadiusSquared);
                }
            } else {
                if (rc.isCoreReady()) {
                    // Enemies are visible but out of attack range
                    // Move closer
                     RobotInfo enemy = bestRobotToAttack(nearbyEnemies);
                     Movement.moveAroundLocation(enemy.location, enemy.type.attackRadiusSquared, myRobotType.attackRadiusSquared);
//                     Movement.pathToLocation(enemy.location);
//                     rc.move(myLocation.directionTo(enemy.location));
//                    if (Movement.moveToClosestEnemy()) {
//                        return;
//                    }
                }
            }
        } else {
            rc.setIndicatorString(0, "No enemies in sight");
            if (rc.isCoreReady()) {
                // No visible enemies
                // Move towards where enemies might be
                Movement.moveToClosestEnemyArchon();
            }
        }
        
    }

    /**
     * Check if there are offensive allies
     * 
     * Offensive allies should meet the following criteria: - Ally robot can
     * attack - Ally robot is not another Viper
     * 
     * @param robots
     *            Allies to check
     * @return Whether there are offensive allies
     */
    public static boolean offensiveAlliesNearby(RobotInfo[] robots) {
        if (robots.length < minOffensiveAlliesCount) {
            return false;
        }
        RobotType[] offensiveTypes = { RobotType.SOLDIER, RobotType.GUARD, RobotType.TURRET };
        return Util.countRobotsByRobotTypes(robots, offensiveTypes) >= minOffensiveAlliesCount;
    }

    /**
     * Rank robot in terms of attack priority for Viper
     * 
     * @param robot
     *            Robot to rank
     * @return Robot attack priority ranking. Higher the more likely you should
     *         attack it.
     */
    public static double rankRobotAttackPriority(RobotInfo robot) {

        double rank = 1.0;

        // Prioritize Archons
        int infectedTurns = robot.viperInfectedTurns;
        if (robot.type.equals(RobotType.ARCHON) && infectedTurns <= MIN_INFECTED_THRESHOLD) {
            // Archon that is not infected
            rank *= 10.0;
        } else if (robot.team.equals(Team.ZOMBIE)) {
            // Decrease Zombie priority
            rank *= 0.1;
        }
        
        if (infectedTurns >= MIN_INFECTED_THRESHOLD) {
            // Is infected
            // Lower priority
            rank *= -infectedTurns;
        }

        int dist = myLocation.distanceSquaredTo(robot.location);

        if (hasOffensiveAlliesNearby) {
            // Allies are in sight
            /*
             * Priorities for attacking enemies: 1. Those who have not been
             * viper infected 2. Highest health 3. Closest
             */
            // Strongest
            rank *= scoreRobot(robot);
        } else {
            // Allies are out of sight
            /*
             * Priorities for attacking enemies: 1. Those who have not been
             * viper infected 2. Lowest health 3. Closest
             */
            // Weakest
            rank /= scoreRobot(robot);
        }

        // Closest
        rank /= dist;

        return rank;

    }

    public static RobotInfo findFirstNotInfectedRobot(RobotInfo[] robots) {
        if (robots.length > 0) {
            int len = robots.length;
            for (int i = 0; i < len; i++) {
                if (robots[i].viperInfectedTurns == 0) {
                    // Not Infected yet
                    return robots[i];
                }
            }
        }
        return null;
    }

}
