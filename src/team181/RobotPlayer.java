package team181;

import battlecode.common.*;
import team181.CommUtil.MessageTags;
import team181.RobotPlayer.Debug;
import team181.RobotPlayer.Messaging;
import team181.RobotPlayer.Movement;
import team181.RobotPlayer.Sensing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

/**
 * Base class for RobotPlayer
 *
 */
public class RobotPlayer {

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")

    static RobotController rc;
    static Direction[] directions = { Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST };
    static RobotType[] robotTypes = RobotType.values(); /* { RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
            RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET };*/

    static MapLocation myLocation;
    /**
     * Current health of robot
     */
    static double myHealth = 0.0;
    /**
     * Used internally, do not use.
     */
    static double prevHealth = 0.0;
    /**
     * Whether the robot was attacked in previous turn
     * Updates every turn.
     */
    static boolean wasAttacked = false;
    static RobotInfo[] nearbyEnemies;
    static RobotInfo[] nearbyTraitors;
    static RobotInfo[] nearbyNeutrals;
    static RobotInfo[] nearbyAllies;
    static RobotInfo[] veryCloseAllies;
    static RobotInfo[] attackableZombies;
    static RobotInfo[] attackableTraitors;
    static RobotInfo[] attackableEnemies;
    static MapLocation[] nearbyParts;
    static ArrayList<MapLocation> knownDens = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> knownNeutrals = new ArrayList<MapLocation>();
    static ArrayList<MapLocation> knownParts = new ArrayList<MapLocation>();
    static ArrayList<DecayingMapLocation> knownEnemyClusters = new ArrayList<DecayingMapLocation>();
    static Random rand;
    static int myAttackRange;
    static Team myTeam;
    static Team enemyTeam;
    static RobotType myRobotType;
    /**
     * Nearest ally archon
     */
    static MapLocation nearestAllyArchon;
    /**
     * Nearest enemy archon
     */
    static MapLocation nearestEnemyArchon;
    /**
     * Nearest last known location of an enemy
     */
    static MapLocation nearestEnemyLocation;
    static MapLocation tempLoc;

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
    static int maxID = 16383;
    static int messagesSentThisRound = 0;
    static int minClusterSeparation = 15*15; 
    static int clusterExpiry = 10;
    static int currentExploreCooldown = 0;
    static int baseExploreCooldown = 40; // Explore for 40 rounds
    static int currentBoundaryCooldown = 0; 
    static int baseBoundaryCooldown = 10; // Allow scouts 10 turns to get away from the wall
    static MapLocation myPreviousLocation = new MapLocation(0,0);

    /**
     * Movement
     *
     */
    static class Movement {

        public static Direction randomDirection() {
            int fate = rand.nextInt(8);
            return directions[fate % 8];
        }

        public static boolean randomMove() throws GameActionException {
            //System.out.println("DEPRECATED: randomMove is deprecated. Please use something else, such as moveToClosestEnemyArchon, etc.");
            int fate = rand.nextInt(8);
            if (rc.isCoreReady()) {
                Direction dirToMove;
                for (int i = 0; i < 8; i++) {
                    dirToMove = directions[(fate + i) % 8];
                    return Movement.moveOrClear(dirToMove);
                }
            } else {
                // rc.setIndicatorString(0, "I could not move this turn");
            }
            return false;
        }
        
        public static boolean moveOrClear(Direction dirToMove) throws GameActionException {
            if (rc.canMove(dirToMove)) {
                // Move
                rc.move(dirToMove);
                return true;
            } else if (rc.senseRubble(myLocation.add(dirToMove)) >= GameConstants.RUBBLE_SLOW_THRESH) {
                // Too much rubble, so I should clear it
                rc.clearRubble(dirToMove);
                return true;
                // Check if I can move in this direction
            } else 
            return false;
        }
        
        public static void randomMoveAroundArcon(int squaredMin, int squaredMax) throws GameActionException {
            int fate = rand.nextInt(999);
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
                        MapLocation newLocation = myLocation.add(dirToMove);
                        if (nearestAllyArchon != null) {
                            rc.setIndicatorLine(myLocation, nearestAllyArchon, 0, 100, 100);
                            int distance = (newLocation.x - nearestAllyArchon.x) * (newLocation.x - nearestAllyArchon.x) + 
                                    (newLocation.y - nearestAllyArchon.y) * (newLocation.y - nearestAllyArchon.y);
                            if (distance >= squaredMin && distance <= squaredMax) {
                                rc.move(dirToMove);
                                return;
                            }
                        }
                    }
                }
                if (nearestAllyArchon != null) {
                    retreatToArchon();
                }
            } else {
                // rc.setIndicatorString(0, "I could not move this turn");
            }
        }
        
        /**
         * Move robot around a center location with a minimum and maximum distance
         * @param center Center location
         * @param minDist Minimum distance squared from center
         * @param maxDist Maximum distance squared from center
         * @return Whether robot moved or not
         * @throws GameActionException 
         */
        public static boolean moveAroundLocation(MapLocation center, int minDist, int maxDist) throws GameActionException {
            // Move about enemies
            if (rc.isCoreReady()) {
                int dist = myLocation.distanceSquaredTo(center);
                if (dist > maxDist) {
                    // Move closer!
                    rc.setIndicatorString(2, "Moving closer to: " + center.toString());
                    moveOrClear(myLocation.directionTo(center));
                } else {
                    // Move away!
                    rc.setIndicatorString(2, "Moving away from: " + center.toString());
                    moveOrClear(myLocation.directionTo(center).opposite());
                }
            }
            return false;
        }
        
        private static MapLocation getNewSpotToMove(Direction direction) {
            MapLocation myLocation = rc.getLocation();
            myLocation.add(direction);
            return myLocation;
            
        }

        public static boolean retreatToArchon() throws GameActionException {
            if (nearestAllyArchon != null) {
                return pathToLocation(nearestAllyArchon);
            } else {
                return moveToClosestAlly();
            }
        }

        public static boolean moveToClosestEnemy() throws GameActionException {
            if (nearbyEnemies.length > 0) {
                return pathToLocation(Util.closestRobot(myLocation, nearbyEnemies).location);
            } else {
                return moveToClosestEnemyArchon();
            }
        }

        public static boolean moveToClosestEnemyArchon() throws GameActionException {
            if (nearestEnemyArchon != null) {
                rc.setIndicatorString(2, "Moving towards closest enemy archon: "+nearestEnemyArchon.toString());
                return pathToLocation(nearestEnemyArchon);
            } else {
                return randomMove();
            }
        }

        
        public static boolean moveToClosestTraitor() throws GameActionException {
            if (nearbyTraitors.length > 0) {
                return pathToLocation(Util.closestRobot(myLocation, nearbyTraitors).location);
            } else {
                return randomMove();
            }
        }
        
        public static boolean moveToClosestAlly() throws GameActionException {
            if (nearbyAllies.length > 0) {
                return pathToLocation(Util.closestRobot(myLocation, nearbyAllies).location);
            } else {
                return randomMove();
            }
        }


        // Make newly created units stay within some default range of the
        // nearest archon.
        // Essentially, don't allow freedom of movement unless if takes us far
        // away from 'mother'
        public static void swarmNearestArchon() {

        }

        public static boolean pathToLocation(MapLocation loc) throws GameActionException {
            if (rc.isCoreReady()) {
                Direction dirToMove = myLocation.directionTo(loc);
                rc.setIndicatorString(0, "Direction to path is: " + dirToMove.toString());
                double rubbleAmount = rc.senseRubble(myLocation.add(dirToMove));
                double turnsToClearToMove = Util.turnsToClearRubbleToMove(rubbleAmount);
                if (!myRobotType.equals(RobotType.SCOUT) && turnsToClearToMove >= 1.0 && turnsToClearToMove <= 5.0) {
                    // Too much rubble, so I should clear it
                    rc.clearRubble(dirToMove);
                    return true;
                    // Check if I can move in this direction
                } else if (rc.canMove(dirToMove)) {
                    rc.move(dirToMove);
                    return true;
                } else if (rc.canMove(dirToMove.rotateLeft())) {
                    rc.move(dirToMove.rotateLeft());
                    return true;
                } else if (rc.canMove(dirToMove.rotateRight())) {
                    rc.move(dirToMove.rotateRight());
                    return true;
                }
            }
            return false;
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
                        rc.attackLocation(bestRobotToAttack(attackableTraitors).location);
                    } else if (attackableZombies.length > 0) {
                        rc.attackLocation(bestRobotToAttack(attackableZombies).location);
                    }
                    // Attack first zombie in list
                } else if (team == Team.ZOMBIE) {
                    if (attackableZombies.length > 0) {
                        rc.attackLocation(bestRobotToAttack(attackableZombies).location);
                    }
                    // Attack first enemy on other team
                } else if (team == enemyTeam) {
                    if (attackableTraitors.length > 0) {
                        rc.attackLocation(bestRobotToAttack(attackableTraitors).location);
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
                    // Make sure signal is from our team
                    if (signal.getTeam() != rc.getTeam()) {
                        continue;
                    }
                    Message message = new Message(signal);
                    MapLocation loc = signal.getLocation();

                    int switchTag = processTag(message.getTag());
                    switch(switchTag){
                        // Handle Scout messages about map bounds
                        case MessageTags.SMBN:
                            // Propagate the message to nearby scouts and archons
                            setMapBound(Direction.NORTH, message.getLocation().y);
                            break;
                        case MessageTags.SMBE:
                            setMapBound(Direction.EAST, message.getLocation().x);
                            break;
                        case MessageTags.SMBS:
                            setMapBound(Direction.SOUTH, message.getLocation().y);
                            break;
                        case MessageTags.SMBW:
                            setMapBound(Direction.WEST, message.getLocation().x);
                            break;
    
                        // Handle reporting of zombie dens
                        case MessageTags.ZDEN:
                            storeDenLocation(message.getLocation());
                            break;
                        
                        // Nearest ally archon
                        case (MessageTags.NAAL) :
                            if (nearestAllyArchon == null || (myLocation.distanceSquaredTo(loc) < myLocation.distanceSquaredTo(nearestAllyArchon))) {
                                nearestAllyArchon = loc;
                            }
                            break;
                        // Handle reporting of enemy archon locations
                        case MessageTags.EARL:
//                            System.out.println("Enemy Archon location: "+message.getLocation().toString());
                            if (nearestEnemyArchon == null || (myLocation.distanceSquaredTo(message.getLocation()) < myLocation.distanceSquaredTo(nearestEnemyArchon))) {
                                nearestEnemyArchon = message.getLocation();
                            }
                            break;
                            
                        // Store enemy clusters
                        case MessageTags.CLUS:
//                            System.out.println("The threat was: " + message.getThreat() + " ttl: " + message.getTTL());
                            storeCluster(new DecayingMapLocation(message.getLocation(), message.getThreat(), message.getTTL()));
                            break;
                    }

                }
            }
        }
        
        // Use this to send messages that will not be relayed.
        public static void sendMessage(Message message, int radiusSquared) throws GameActionException{
            messagesSentThisRound++;
            if(messagesSentThisRound > GameConstants.MESSAGE_SIGNALS_PER_TURN){
                //System.out.println("ERROR: TOO MANY MESSAGES SENT THIS ROUND: " + Integer.toString(messagesSentThisRound) + " " + message.getTag());
            }else{
                //System.out.println("DEBUG: MESSAGES SENT THIS ROUND: " + Integer.toString(messagesSentThisRound) + " " + message.getTag() + " " + message.getLocation().toString());
                message.send(rc, radiusSquared);
            }
        }
        
        // Use this to send messages that can be relayed.
        public static void sendRelayableMessage(Message message, int radiusSquared) throws GameActionException{
            message.setTag(message.getTag() + 1);
            sendMessage(message, radiusSquared);
        }
        
        // Implementation of relaying system
        public static int processTag(int tag){
            if(tag % 2 == 1){
                tag--;
            }
            return tag;
        }
        
        public static boolean isRelayable(int tag){
            if(tag % 2 == 1){
                return true;
            }else{
                return false;
            }
        }
        
        
        public static void storeDenLocation(MapLocation loc) {
            for (MapLocation den : knownDens) {
                // Don't bother to add dens we already know about
                if (den.equals(loc)) {
                    return;
                }
            }
            // Create a new map location at the reported spot.
            knownDens.add(loc);
            // System.out.println("I found a den this turn at: " +
            // denLoc.toString());
        }
        
        public static boolean storeCluster(DecayingMapLocation dloc){
            for (DecayingMapLocation knownCluster : knownEnemyClusters) {
                int clusterSeparation = knownCluster.location.distanceSquaredTo(dloc.location);
                if (clusterSeparation < minClusterSeparation) {
                    // If we have a larger threat, replace old cluster with new one
                    // Also replace if the old threat has expired
                    if(knownCluster.threatLevel < dloc.threatLevel || knownCluster.ttl <= clusterExpiry){
//                        System.out.println("I replaced a cluster with at " + knownCluster.location.toString()
//                        + " threat: " + knownCluster.threatLevel + " ttl: " + knownCluster.ttl);
//                        System.out.println(" with the cluster " + dloc.location.toString()
//                        + " threat: " + dloc.threatLevel + " ttl: " + dloc.ttl);
                        knownEnemyClusters.remove(knownCluster);
                        knownEnemyClusters.add(dloc);
                        return true;
                    }
                    // If it was too close to any one known, don't add it
                    return false;
                }
            }    
            //If we get here, we are close to none of the existing clusters, so add it.
//            System.out.println("I added a  new cluster at " + dloc.location.toString()
//            + " threat: " + dloc.threatLevel + " ttl: " + dloc.ttl);
            knownEnemyClusters.add(dloc);
            return true;
        }
        
        public static boolean storeParts(MapLocation loc){
            int size = knownParts.size();
            for(int i = 0; i < size; i++){
                MapLocation otherLoc = knownParts.get(i);
                if(otherLoc.equals(loc)){
                    return false;
                }
            }
            System.out.println("I added a new part!");
            knownParts.add(loc);
            return true;
        }
        
        public static boolean storeNeutral(MapLocation loc){
            int size = knownNeutrals.size();
            for(int i = 0; i < size; i++){
                MapLocation otherLoc = knownNeutrals.get(i);
                if(otherLoc.equals(loc)){
                    return false;
                }
            }
            System.out.println("I added a new neutral!");
            knownNeutrals.add(loc);
            return true;            
        }

    }

    static class Sensing {
        public static void updateNearbyEnemies() {
            myLocation = rc.getLocation();
            nearbyNeutrals = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, Team.NEUTRAL);
            nearbyParts = rc.sensePartLocations(myRobotType.sensorRadiusSquared);
            nearbyEnemies = rc.senseHostileRobots(myLocation, myRobotType.sensorRadiusSquared);
            nearbyTraitors = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, enemyTeam);
            nearbyAllies = rc.senseNearbyRobots(myRobotType.sensorRadiusSquared, myTeam);
            veryCloseAllies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, myTeam);
            attackableTraitors = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, enemyTeam);
            attackableZombies = rc.senseNearbyRobots(myRobotType.attackRadiusSquared, Team.ZOMBIE);
//            attackableEnemies = Util.joinRobotInfo(attackableTraitors, attackableZombies);

            if (nearestEnemyArchon != null) {
                // rc.setIndicatorString(3, "Enemy Archon @
                // "+nearestEnemyArchon.toString());
                // System.out.println("Enemy archon @
                // "+nearestEnemyArchon.toString());
            } else {
                // System.out.println("Unknown enemy archon location");
                // Nearest Enemy Archons
                nearestEnemyArchon = Util.closestMapLocation(myLocation, rc.getInitialArchonLocations(enemyTeam));
                // System.out.println("Enemy archon @
                // "+nearestEnemyArchon.toString());
            }

            // Nearest Ally Archons
            if (nearestAllyArchon == null) {
                nearestAllyArchon = Util.closestMapLocation(myLocation, rc.getInitialArchonLocations(myTeam));
            }

            boolean seesEnemyArchon = false;
            if (nearbyEnemies.length > 0) {
                // Update nearest enemy location
                int bestDist = myRobotType.sensorRadiusSquared + 1;
                if (nearestEnemyLocation != null) {
                    bestDist = myLocation.distanceSquaredTo(nearestEnemyLocation);
                }
                for (RobotInfo robot : nearbyEnemies) {
                    int dist = myLocation.distanceSquaredTo(robot.location);
                    if (dist < bestDist) {
                        bestDist = dist;
                        nearestEnemyLocation = robot.location;
                    }
                    // Nearest Enemy Archon
                    if (robot.type.equals(RobotType.ARCHON)) {
                        seesEnemyArchon = true;
                        if (dist < myLocation.distanceSquaredTo(nearestEnemyArchon)) {
                            nearestEnemyArchon = robot.location;
                        }
                    }
                }
            }
            
            // Check if Enemy Archon is gone!
            if (nearestEnemyArchon != null && !seesEnemyArchon && rc.canSenseLocation(nearestEnemyArchon)) {
                // Cannot see enemy archon however there should be one!
                MapLocation[] enemyInitialArchonLocations = rc.getInitialArchonLocations(enemyTeam);
                int idx = Util.indexOfMapLocation(enemyInitialArchonLocations, nearestEnemyArchon);
                if (idx == -1) {
                    // Not found
                    // Start at beginning
                    nearestEnemyArchon = enemyInitialArchonLocations[0];
                } else {
                    // Found, go to next
                    idx++;
                    if (idx < enemyInitialArchonLocations.length) {
                        // Has next
                        nearestEnemyArchon = enemyInitialArchonLocations[idx];
                    } else {
                        // At end
                        nearestEnemyArchon = null;
                    }
                }
            }
            
            // Nearest ally archon
            if (!myRobotType.equals(RobotType.ARCHON) && nearbyAllies.length > 0) {
                // check for nearest ally archon
                int bestDist = myRobotType.sensorRadiusSquared + 1;
                for (RobotInfo r : nearbyAllies) {
                    int dist = myLocation.distanceSquaredTo(r.location);
                    if (r.type.equals(RobotType.ARCHON) && dist < bestDist) {
                        bestDist = dist;
                        nearestAllyArchon = r.location;
                    }
                }
            }
            
        }
    }

    static class Debug {
        // Prevent indicator string for persisting longer than 1 turn
        public static void emptyIndicatorStrings() {
            for (int i = 0; i < 3; i++) {
                rc.setIndicatorString(i, "");
            }
        }
        
        public static void displayClusters(){
            for(DecayingMapLocation dloc : knownEnemyClusters){
                int red = (int)(255 * ((double)dloc.ttl / 127));
                rc.setIndicatorDot(dloc.location, red, 85, 65);
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
     * @param inrc RobotController
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
            case TTM:
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
                updateDecays();
                Debug.displayClusters();
                Sensing.updateNearbyEnemies();
                
                messagesSentThisRound = 0;
                

                // Check if health has decreased
                myHealth = rc.getHealth();
                if (prevHealth != 0.0 && prevHealth > myHealth) {
                    // Health has decreased
                    wasAttacked = true;
                } else {
                    // Health has not decreased
                    wasAttacked = false;
                }
                prevHealth = myHealth;
                
                switch (myRobotType) {
                    case ARCHON:
                        ArchonPlayer.tick();
                        break;
                    case TURRET:
                    case TTM:
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
     * @throws GameActionException A exception throw from within the game
     */
    public static void tick() throws GameActionException {
        System.out.println("Subclasses should implement their own tick method");
    }
    
    public static void updateDecays(){
        ArrayList<DecayingMapLocation> temp = new ArrayList<DecayingMapLocation>(knownEnemyClusters);
        for(DecayingMapLocation dloc : temp){
            if(dloc.ttl > 0){
                dloc.ttl--;
            }else{
                knownEnemyClusters.remove(dloc);
            }
        }
    }
    
    public static boolean safeToBroadcast(){
        for(int i = 0; i < nearbyEnemies.length; i++){
            RobotInfo robot = nearbyEnemies[i];
            switch(robot.type){
                case ZOMBIEDEN:
                case SCOUT:
                case ARCHON:
                    break;
                default:
                    // If the distance between them is less than the other robot's attack radius, don't broadcast!
                    if(robot.location.distanceSquaredTo(rc.getLocation()) <= robot.type.attackRadiusSquared + 1){
                        return false;
                    }
                    break;            
            }  
        }
        return true;
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
     * @param alliesWithinRange Ally robots
     * @param enemysWithinRange Enemy robots
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
    protected static double scoreRobot(RobotInfo robot) {
        return (robot.attackPower + robot.health) / (robot.weaponDelay + 1.0);
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
            // Enemy attack potential
            int elen = nearbyEnemies.length;
            for (int i=0; i<elen; i++) {
                RobotInfo r = nearbyEnemies[i];
                totalRisk += attackRisk(loc, r);
            }
            // Allied assistance attack potential
            // Ally attack potential
            int alen = nearbyAllies.length;
            for (int i=0; i<alen; i++) {
                RobotInfo r = nearbyAllies[i];
                totalRisk -= attackRisk(loc, r);
            }
            // Neutral assistance
            if (myRobotType.equals(RobotType.ARCHON)) {
                int nlen = nearbyNeutrals.length;
                for (int i=0; i<nlen; i++) {
                    RobotInfo r = nearbyNeutrals[i];
                    totalRisk -= attackRisk(loc, r);
                }
            }
            // Consider Rubble protection
            else if (myRobotType.equals(RobotType.SCOUT)) {
                totalRisk -= rubbleCover(loc) * 0.1;
            }

            // rc.setIndicatorString(2, "Risk this turn is: " +
            // Double.toString(totalRisk));
//            System.out.println(
//                    "The total risk for possible location " + loc.toString() + " was: " + Double.toString(totalRisk));
            return totalRisk;
        } else {
            return 0.0;
        }
    }
    
    /**
     * Additional distance from attack radius to be considered safe.
     * Used in attackRisk calculation.
     */
    public static int safeDistFromAttackOffsetSquared = 4;
    /**
     * 
     * @param r robot
     * @return Total attack risk for robot
     */
    public static double attackRisk(MapLocation loc, RobotInfo r) {
        // Ignore Neutral robots
        if (r.team.equals(Team.NEUTRAL)) {
            return 0.0;
        }
        // Ignore Archons
        if (r.type.equals(RobotType.ARCHON)) {
            return 0.0;
        }
        // Ignore Scouts
        if (r.type.equals(RobotType.SCOUT)) {
            return 0.0;
        }
        // Ignore TTM
        if (r.type.equals(RobotType.TTM)) {
            return 0.0;
        }
        // Ignore zombie Den
        if (r.type.equals(RobotType.ZOMBIEDEN)) {
            return 0.0;
        }
        MapLocation enemyLoc = r.location;
        RobotType enemyType = r.type;
        int distAway = loc.distanceSquaredTo(enemyLoc);
        int safeDist = (enemyType.attackRadiusSquared + safeDistFromAttackOffsetSquared);
        if (enemyType.equals(RobotType.TURRET)) {
            safeDist = RobotType.TURRET.sensorRadiusSquared + safeDistFromAttackOffsetSquared;
        }
        if (distAway <= safeDist) {
            // If enemy has 0 attack power then risk = 0
            // If Core delay is 0 then risk = numerator, and will be
            // divided by each turn/core delay
            return (safeDist - distAway) * scoreRobot(r);
        }
        return 0.0;
    }
    
    public static double rubbleCover(MapLocation loc) {
        double totalRubble = 0;
        double maxTurnsToMove = 3.0;
        for (Direction dir : directions) {
            double rubble = rc.senseRubble(loc.add(dir));
            double bonus = 1.0;
            if (rubble >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                // They cannot move through it
                // Count turns to clear
                // How long until they could move?
                double turnsToMove = Util.turnsToClearRubbleToMove(rubble);
                // Maximum number of turns
                turnsToMove = turnsToMove > maxTurnsToMove ? maxTurnsToMove : turnsToMove;
                rubble *= turnsToMove;
            }
            totalRubble += rubble * bonus * GameConstants.RUBBLE_SLOW_THRESH;
        }
        return totalRubble;
    }
    
    /**
     * Compare robots and determine the best one to attack
     * 
     * @param robots Robots to evaluate
     * @return Robot you should attack. Will return null if there are no robots given.
     */
    public static RobotInfo bestRobotToAttack(RobotInfo[] robots) {
        if (robots.length > 0) {
            RobotInfo bestRobot = robots[0];
            double bestRank = rankRobotAttackPriority(bestRobot);
            int len = robots.length;
            for (int i=1; i<len; i++) {
                RobotInfo robot = robots[i];
                double rank = rankRobotAttackPriority(robot);
//                int color = (int) Math.max(0, Math.min(255, rank));
                rc.setIndicatorLine(myLocation, robot.location, (int) ((rank >= 0) ? Math.min(255, rank) : 0), (int) ((rank < 0) ? Math.min(255, -rank) : 0), 100);
//                System.out.println("Rank: "+rank+" @ "+robot.toString());
                if (rank > bestRank) {
                    bestRobot = robot;
                    bestRank = rank;
                }
            }
            return bestRobot;
        } else {
            return null;
        }
    }
    
    /**
     * Rank a robot in terms of attack priority
     * 
     * Priorities
     * #1. Archons
     * #2. Wearkest Robot
     * 
     * @param robot Robot to rank
     * @return The total robot ranking
     */
    public static double rankRobotAttackPriority(RobotInfo robot) {
        
        // Subclasses
        if(myRobotType.equals(RobotType.VIPER)) {
            return ViperPlayer.rankRobotAttackPriority(robot);
        }
        
        if (robot.type.equals(RobotType.ARCHON)) {
            // double archonCoef = 1000.0;
            // Distance to the archon
            // Find the closest archon!
            return myRobotType.sensorRadiusSquared / myLocation.distanceSquaredTo(robot.location);
        }
        // Weakest
        return -scoreRobot(robot);
    }
    
    /**
     * Determine the best direction robot *can* move while considering risk of each direction
     * 
     * Will return Direction.NONE if either no move should be made or there are no moves to be made.
     * 
     * @param idealDir Ideal direction, if all things equal
     * @return The best direction
     */
    public static Direction leastRiskyDirection(Direction idealDir) {
        return leastRiskyDirection(idealDir, false);
    }
    
    /**
     * Determine the best direction robot can move or should clear rubble, while considering risk of each direction
     * 
     * Will return Direction.NONE if either no move should be made or there are no moves to be made.
     * 
     * Note: Scouts will exclude considering rubble
     * 
     * @param idealDir Ideal direction, if all things equal
     * @param allowClearingRubble TODO: Allowing direction to have rubble that must be cleared first before moving
     * @return The best direction to move or clear
     */
    public static Direction leastRiskyDirection(Direction idealDir, boolean allowClearingRubble) {
        // There are enemies within sight!
        // Calculate least risking direction to move
        double leastRisk = attackRisk(myLocation);
        Direction leastRiskyDirection = Direction.NONE;
        Direction[] allDirs = Direction.values();
        MapLocation goalLoc = myLocation.add(idealDir);
        MapLocation leastRiskyLoc = goalLoc;
        double[] risks = new double[allDirs.length];
        for (int i = 0; i < allDirs.length; i++) {
            // Check if can move in this direction
            Direction currDir = allDirs[i];
            MapLocation nextLoc = myLocation.add(currDir);
            if (rc.canMove(currDir) || (allowClearingRubble && rc.senseRubble(nextLoc) > GameConstants.RUBBLE_OBSTRUCTION_THRESH)) {
                // Can move in this direction
                // Check attack risk value
                double risk = attackRisk(nextLoc);
                risks[i] = risk;
                if (risk != 0.0) {
                    rc.setIndicatorDot(nextLoc, (int) ((risk >= 0) ? Math.min(255, risk) : 0), (int) ((risk < 0) ? Math.min(255, -risk) : 0), 0);
                }
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
        rc.setIndicatorString(2, "Ideal dir: "+idealDir.toString()+", Leasty Risky Dir: "+leastRiskyDirection.toString());
//        rc.setIndicatorString(2, "Risk this turn is: " + Arrays.toString(risks));
        return leastRiskyDirection;
    }
    
    
    /**
     *      Given a direction, try to move that way, but avoid taking damage or
     *      going far into enemy LOF.
     *      General Scout exploration driver.
     *      Don't let scout get stuck in concave areas, or near swarms of allies.
     *      
     * @param dirToMove The preferred direction to move, if all things equal
     * @throws GameActionException An exception throw from the game
     * @return Whether we did move
     */
     public static boolean explore(Direction dirToMove) throws GameActionException {
        if (rc.isCoreReady()) {
            if (nearbyEnemies.length == 0) {
                // There are no known enemy threats
                if (rc.canMove(dirToMove)) {
                    rc.move(dirToMove);
                    return true;
                } else if (rc.canMove(dirToMove.rotateLeft())) {
                    rc.move(dirToMove.rotateLeft());
                    return true;
                } else if (rc.canMove(dirToMove.rotateRight())) {
                    rc.move(dirToMove.rotateRight());
                    return true;
                }
            } 
            else {
                //rc.setIndicatorString(1, "The core delay before is : " + rc.getCoreDelay());
                Direction bestDir = leastRiskyDirection(dirToMove);
                //rc.setIndicatorString(2, "The core delay after is : " + rc.getCoreDelay());
                if (!bestDir.equals(Direction.NONE)) {
                    return Movement.moveOrClear(bestDir);
                }
            }
        }
        return false;
    }

}
