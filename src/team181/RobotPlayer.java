package team181;

import battlecode.common.*;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Sensing;

import java.util.Arrays;
import java.util.Random;

public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")

    static RobotController rc;
    static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
    static RobotType[] robotTypes = { RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET };

    static MapLocation myLocation;
    static RobotInfo[] nearbyEnemies;
    static RobotInfo[] nearbyTraitors;
    static RobotInfo[] nearbyAllies;
    static RobotInfo[] veryCloseAllies;
    static RobotInfo[] attackableZombies;
    static RobotInfo[] attackableTraitors;
    static Random rand;
    static int myAttackRange;
    static Team myTeam;
    static Team enemyTeam;
    static RobotType myRobotType;
    static MapLocation nearestArchon;
    /**
     * Copy of signal queue
     */
    static Signal[] currentSignals;
    /**
     * Map bounds
     */
    static int northBound = 0;
    static int eastBound = 0;
    static int southBound = 0;
    static int westBound = 0;
    static boolean nbs = false;
    static boolean ebs = false;
    static boolean sbs = false;
    static boolean wbs = false;
    static boolean allBoundsSet = false;
    static int maxID = 32000;

    /**
     * Message types
     *
     */
    static class messageConstants {

        /**
         *  Nearest Archon Location
         */
        public final static int NEAL = 55555;

        /**
         *  Scout Map Bounds North
         */
        public final static int SMBN = 12345;
        public final static int SMBE = 22345;
        public final static int SMBS = 32345;
        public final static int SMBW = 42345;
        /**
         *  Archon Map Bounds North
         */
        public final static int AMBN = 54321;
        public final static int AMBE = 44321;
        public final static int AMBS = 34321;
        public final static int AMBW = 24321;

        public final static int DENX = 666660;
        public final static int DENY = 666661;

    }

    /**
     * Movement
     *
     */
    static class Movement {

        public static Direction randomDirection() {
            int fate = rand.nextInt(8);
            return directions[fate % 8];
        }

        public static void randomMove() throws GameActionException {
            int fate = rand.nextInt(8);
            if (rc.isCoreReady()) {
                Direction dirToMove;
                for (int i = 0; i < 8; i++) {
                    dirToMove = directions[(fate + i) % 8];
                    if (rc.senseRubble(myLocation.add(dirToMove)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                        // Too much rubble, so I should clear it
                        rc.clearRubble(dirToMove);
                        break;
                        // Check if I can move in this direction
                    } else if (rc.canMove(dirToMove)) {
                        // Move
                        rc.move(dirToMove);
                        break;
                    }
                }
            } else {
                // rc.setIndicatorString(0, "I could not move this turn");
            }
        }

        public static void retreatToArchon() throws GameActionException {
            pathToLocation(nearestArchon);
        }

        public static void moveToClosestEnemy() throws GameActionException {
            if (nearbyEnemies.length > 0) {
                pathToLocation(Util.closestRobot(myLocation, nearbyEnemies).location);
            } else {
                randomMove();
            }
        }

        public static void moveToClosestTraitor() throws GameActionException {
            if (nearbyTraitors.length > 0) {
                pathToLocation(Util.closestRobot(myLocation, nearbyTraitors).location);
            } else {
                randomMove();
            }
        }

        // Make newly created units stay within some default range of the
        // nearest archon.
        // Essentially, don't allow freedom of movement unless if takes us far
        // away from 'mother'
        public static void swarmNearestArchon() {

        }

        public static void pathToLocation(MapLocation loc) throws GameActionException {
            if (rc.isCoreReady()) {
                Direction dirToMove = myLocation.directionTo(loc);
                rc.setIndicatorString(0, "Direction to path is: " + dirToMove.toString());
                if (rc.senseRubble(myLocation.add(dirToMove)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(dirToMove);
                    // Check if I can move in this direction
                } else if (rc.canMove(dirToMove)) {
                    rc.move(dirToMove);
                } else if (rc.canMove(dirToMove.rotateLeft())) {
                    rc.move(dirToMove.rotateLeft());
                } else if (rc.canMove(dirToMove.rotateRight())) {
                    rc.move(dirToMove.rotateRight());
                }
            }
        }
    }

    static class Offensive {
        public static void attack() throws GameActionException {
            attack(null);
        }

        public static void attack(Team team) throws GameActionException {
            if (rc.isCoreReady() && rc.isWeaponReady()) {
                // If unspecified, attack first enemy in list
                if (team == null) {
                    // Prioritize attacking enemy units
                    if (attackableTraitors.length > 0) {
                        rc.attackLocation(attackableTraitors[0].location);
                    } else if (attackableZombies.length > 0) {
                        rc.attackLocation(attackableZombies[0].location);
                    }
                    // Attack first zombie in list
                } else if (team == Team.ZOMBIE) {
                    if (attackableZombies.length > 0) {
                        rc.attackLocation(attackableZombies[0].location);
                    }
                    // Attack first enemy on other team
                } else if (team == enemyTeam) {
                    if (attackableTraitors.length > 0) {
                        rc.attackLocation(attackableTraitors[0].location);
                    }
                }
            }
        }

        // For soldiers and guards and such. An algorithm that moves soldiers
        // and the like as a cohesive unit towards hostile territory.
        // Don't want units to move out alone with this.
        public static void attackMove(MapLocation loc) {

        }
    }

    static class Messaging {
        // Basic handling for message queuing, nearest archon
        public static void handleMessageQueue() {
            currentSignals = rc.emptySignalQueue();
            if (currentSignals.length > 0) {
                // rc.setIndicatorString(0, "I received a signal this turn!");
                for (Signal signal : currentSignals) {
                    MapLocation loc = signal.getLocation();
                    rc.setIndicatorString(1, "Message Recieved was: " + Integer.toString(signal.getMessage()[0]) + " "
                            + Integer.toString(signal.getMessage()[1]));
                    // Set the nearest archon location if appropriate message
                    // was recieved.
                    if (signal.getMessage()[0] == messageConstants.NEAL) {
                        nearestArchon = loc;
                    }
                }
            }
        }

        // Fix for transmitting negative bounds.
        // Use this to before transmitting and receiving bounds
        public static int adjustBound(int bound) {
            int offset = 40000;
            if (bound > 16000)
                bound = bound - offset;
            else if (bound < 0)
                bound = bound + offset;
            return bound;
        }
    }

    static class Sensing {
        public static void updateNearbyEnemies() {
            myLocation = rc.getLocation();
            nearbyEnemies = rc.senseHostileRobots(myLocation, myRobotType.sensorRadiusSquared);
            nearbyTraitors = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, enemyTeam);
            nearbyAllies = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, myTeam);
            veryCloseAllies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, myTeam);
            attackableTraitors = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, enemyTeam);
            attackableZombies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, Team.ZOMBIE);
        }
    }

    static class Debug {
        // Prevent indicator string for persisting longer than 1 turn
        public static void emptyIndicatorStrings() {
            for (int i = 0; i < 3; i++) {
                rc.setIndicatorString(i, "");
            }
        }
    }

    public static void setMapBound(Direction dir, int bound) {
        if (!allBoundsSet) {
            if (dir == Direction.NORTH) {
                northBound = bound;
                nbs = true;
            } else if (dir == Direction.EAST) {
                eastBound = bound;
                ebs = true;
            } else if (dir == Direction.SOUTH) {
                southBound = bound;
                sbs = true;
            } else if (dir == Direction.WEST) {
                westBound = bound;
                wbs = true;
            }
            if (nbs && ebs && sbs && wbs) {
                allBoundsSet = true;
            }
        }
    }

    /**
     * Let's get this robot started!
     * 
     * @param inrc
     */
    public static void run(RobotController inrc) {
        // You can instantiate variables here.
        rc = inrc;
        rand = new Random(rc.getID());
        myAttackRange = 0;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myAttackRange = rc.getType().attackRadiusSquared;
        myRobotType = rc.getType();
        loop();
    }

    /**
     * Run loop for the robot. Calls RobotPlayer.tick() for specific RobotPlayer
     * type.
     * 
     */
    public static void loop() {
        switch (myRobotType) {
        case ARCHON:
            ArchonPlayer.initialize();
            break;
        case TURRET:
            TurretPlayer.initialize();
            break;
        case SOLDIER:
            SoldierPlayer.initialize();
            break;
        case GUARD:
            GuardPlayer.initialize();
            break;
        case VIPER:
            ViperPlayer.initialize();
            break;
        case SCOUT:
            ScoutPlayer.initialize();
            break;
        default:
            System.out.println("Unknown Robot Type");
            return;
        }

        while (true) {
            try {
                Debug.emptyIndicatorStrings();
                Sensing.updateNearbyEnemies();
                
                switch (myRobotType) {
                    case ARCHON:
                        ArchonPlayer.tick();
                        break;
                    case TURRET:
                        TurretPlayer.tick();
                        break;
                    case SOLDIER:
                        SoldierPlayer.tick();
                        break;
                    case GUARD:
                        GuardPlayer.tick();
                        break;
                    case VIPER:
                        ViperPlayer.tick();
                        break;
                    case SCOUT:
                        ScoutPlayer.tick();
                        break;
                    default:
                        System.out.println("Unknown Robot Type");
                        return;
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Initialize the robot
     */
    public static void initialize() {
    }

    /**
     * Run the robot one turn
     * 
     * @throws GameActionException
     */
    public static void tick() throws GameActionException {
        System.out.println("Subclasses should implement their own tick method");
    }

    /**
     * Determine if robot should flee (fight or flight)
     * 
     * @return Whether or not robot should flee
     */
    public static boolean shouldFlee() {
        return shouldFlee(nearbyAllies, nearbyEnemies);
    }
    
    /**
     * Determine if robot should flee (fight or flight)
     * 
     * @param alliesWithinRange
     * @param enemysWithinRange
     * @param zombiesWithinRange
     * @return Whether or not robot should flee
     */
    public static boolean shouldFlee(RobotInfo[] alliesWithinRange, RobotInfo[] enemysWithinRange) {
        double alliePower = scoreRobots(alliesWithinRange);
        double enemyPower = scoreRobots(enemysWithinRange);
        return (alliePower < enemyPower);
    }

    /**
     * Calculate the attack potential of an array of Robots
     * 
     * @param robots
     * @return Total attack score for all robots
     */
    private static double scoreRobots(RobotInfo[] robots) {
        double score = 0.0;
        int len = robots.length;
        for (int r=0; r<len; r++) {
            RobotInfo robot = robots[r];
            score += scoreRobot(robot);
        }
        return score;
    }
    
    /**
     * Calculate the attack potential of a robot
     * @param robot
     * @return Total Score for single robot
     */
    private static double scoreRobot(RobotInfo robot) {
        return robot.attackPower + robot.health;
    }

    /*
     * Calculation the Attack risk value of moving to this location given the
     * direction.
     *
     * Attack risk is currently the count of the number of enemies which will be
     * within attack range of this location Ideal attack risk is 0 (no risk of
     * attack).
     *
     */
    public static double attackRisk(MapLocation loc) {
        if (nearbyEnemies.length > 0) {
            double totalRisk = 0.0;
            for (RobotInfo r : nearbyEnemies) {
                MapLocation enemyLoc = r.location;
                RobotType enemyType = r.type;
                int distAway = loc.distanceSquaredTo(enemyLoc);
                int offsetBlocks = 2;
                int offsetSquared = offsetBlocks * offsetBlocks;
                int safeDist = (enemyType.attackRadiusSquared + offsetSquared);
                if (enemyType.equals(RobotType.TURRET)) {
                    safeDist = RobotType.TURRET.sensorRadiusSquared + offsetSquared;
                }
                if (distAway <= safeDist) {
                    // If enemy has 0 attack power then risk = 0
                    // If Core delay is 0 then risk = numerator, and will be
                    // divided by each turn/core delay
                    double risk = ((safeDist - distAway) * r.attackPower) / (r.weaponDelay + 1.0);
                    totalRisk += risk;
                }
            }
            // rc.setIndicatorString(2, "Risk this turn is: " +
            // Double.toString(totalRisk));
            System.out.println(
                    "The total risk for possible location " + loc.toString() + " was: " + Double.toString(totalRisk));
            return totalRisk;
        } else {
            return 0.0;
        }
    }
    
    /**
     * Compare robots and determine the best one to attack
     * @param robots
     * @return Robot you should attack
     */
    public static RobotInfo bestRobotToAttack(RobotInfo[] robots) {
        // TODO: make this better
        return Util.closestRobot(myLocation, robots);
    }
    
    /**
     * Determine the best direction while considering risk of each direction
     * 
     * @param idealDir Ideal direction, if all things equal
     * @return The best direction
     */
    public static Direction leastRiskyDirection(Direction idealDir) {
        // There are enemies within sight!
        // Calculate least risking direction to move
        double leastRisk = Double.MAX_VALUE; // risks[0];
        Direction leastRiskyDirection = Direction.NONE;
        Direction[] allDirs = Direction.values();
        MapLocation goalLoc = myLocation.add(idealDir);
        MapLocation leastRiskyLoc = goalLoc;
        double[] risks = new double[allDirs.length];
        for (int i = 0; i < allDirs.length; i++) {
            // Check if can move in this direction
            Direction currDir = allDirs[i];
            if (rc.canMove(currDir)) {
                // Can move in this direction
                // Check attack risk value
                MapLocation nextLoc = myLocation.add(currDir);
                double risk = attackRisk(nextLoc);
                risks[i] = risk;
                rc.setIndicatorDot(nextLoc, (int) Math.min(255, risk), 0, 0);
                // System.out.println("At location" +
                // currLoc.toString() + " risk in direction " +
                // currDir.toString() + " is: " +
                // Double.toString(risk));
                // Is this better?
                // Bias towards moving the same direction, dirToMove
                if (currDir.equals(idealDir) && risk <= leastRisk) {
                    // At least as good
                    leastRisk = risk;
                    leastRiskyDirection = idealDir;
                    leastRiskyLoc = nextLoc;
                } else if (risk < leastRisk) {
                    // Better
                    leastRisk = risk;
                    leastRiskyDirection = allDirs[i];
                    leastRiskyLoc = nextLoc;
                } else if (risk == leastRisk && (goalLoc.distanceSquaredTo(nextLoc) < goalLoc
                        .distanceSquaredTo(leastRiskyLoc))) {
                    // Equally as good but closer to the original
                    // dirToMove direction
                    leastRisk = risk;
                    leastRiskyDirection = allDirs[i];
                    leastRiskyLoc = nextLoc;
                }
            }
        }
        rc.setIndicatorString(2, "Risk this turn is: " + Arrays.toString(risks));
        return leastRiskyDirection;
    }

}
