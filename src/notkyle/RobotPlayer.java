package notkyle;

import java.util.ArrayList;
import java.util.Random;

import battlecode.common.*;

public class RobotPlayer{
    
    // Possible improvement: this radius is so big that our scouts and archons gain tons of delay
    // and can't move anymore.
    static int infinity = 10000;
    
    // commands
    static int ELECTION = 73646;
    static int MOVE_X = 182632;
    static int MOVE_Y = 1827371;
    static int FOUND_ARCHON_X = 756736;
    static int FOUND_ARCHON_Y = 256253;
    
    static boolean leader = false;
     
    static Random rnd;
    static RobotController rc;
    static int[] tryDirections = {0,-1,1,-2,2};
    static RobotType[] buildList = new RobotType[]{RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,RobotType.SOLDIER,RobotType.SCOUT};
    
    public static void run(RobotController rcIn) throws GameActionException {
        
        rc = rcIn;
        rnd = new Random(rc.getID());
        
        while(true){
            try{
                if(rc.getType()==RobotType.ARCHON){
                    archonCode();
                }else if(rc.getType()==RobotType.SOLDIER){
                    soldierCode();
                }else if(rc.getType()==RobotType.SCOUT){
                    scoutCode();
                }
            }catch(Exception e){
                e.printStackTrace();
            }

            Clock.yield();
        }
    }
    
    private static void sendInstructions() throws GameActionException {
        // Possible improvement: stop sending the same message over and over again
        // since it will just increase our delay.
        if (!archonFound) {
            MapLocation loc = rc.getLocation();
            rc.broadcastMessageSignal(MOVE_X, loc.x, infinity);
            rc.broadcastMessageSignal(MOVE_Y, loc.y, infinity);
        } else {
            rc.broadcastMessageSignal(MOVE_X, archonX, infinity);
            rc.broadcastMessageSignal(MOVE_Y, archonY, infinity);
        }
    }
    
    static int targetX = -1;
    static int targetY = -1;
    static int archonX = -1;
    static int archonY = -1;
    static boolean archonFound = false;
    private static void readInstructions() throws GameActionException {
        Signal[] signals = rc.emptySignalQueue();
        
        for (Signal s : signals) {
            if (s.getTeam() != rc.getTeam()) {
                continue;
            }
            
            if (s.getMessage() == null) {
                continue;
            }
            
            int command = s.getMessage()[0];
            if (command == MOVE_X) {
                targetX = s.getMessage()[1];
            } else if (command == MOVE_Y) {
                targetY = s.getMessage()[1];
            } else if (command == FOUND_ARCHON_X) {
                archonX = s.getMessage()[1];
            } else if (command == FOUND_ARCHON_Y) {
                archonY = s.getMessage()[1];
                archonFound = true;
            }
        }
    }
    
    /*
     * This code wasn't shown in the lecture.
     * 
     * One downside to this bot is that only one leader is ever elected. Now, we will do a leader
     * election every 100 turns in case the old leader is killed.
     */
    private static void leaderElection() throws GameActionException {
        if (rc.getRoundNum() % 100 == 0) {
            // First step: elect a leader archon
            if (rc.getType() == RobotType.ARCHON) {
                rc.broadcastMessageSignal(ELECTION, 0, infinity);
                
                Signal[] received = rc.emptySignalQueue();
                int numArchons = 0;
                for (Signal s : received) {
                    if (s.getMessage() != null && s.getMessage()[0] == ELECTION) {
                        numArchons++;
                    }
                }
                if (numArchons == 0) {
                    // If you haven't received anything yet, then you're the leader.
                    leader = true;
                    rc.setIndicatorString(0, "I'm the leader!");
                } else {
                    leader = false;
                    rc.setIndicatorString(0, "I'm not the ldaer");
                }
            }
        }
    }

    private static void archonCode() throws GameActionException {
        leaderElection();
        
        readInstructions();
        if (leader) {
            sendInstructions();
        }
        
        if (rc.isCoreReady()) {
            // Building is #1 priority
            Direction randomDir = randomDirection();
            RobotType toBuild = buildList[rnd.nextInt(buildList.length)];
            if (rc.getTeamParts() >= RobotType.SCOUT.partCost) {
                if (rc.canBuild(randomDir,  toBuild)) {
                    rc.build(randomDir, toBuild);
                    return;
                }
            }
            
            MapLocation target = new MapLocation(targetX, targetY);
            Direction dir = rc.getLocation().directionTo(target);
            
            RobotInfo[] enemies = rc.senseHostileRobots(rc.getLocation(), infinity);
            if (enemies.length > 0) {
                Direction away = rc.getLocation().directionTo(enemies[0].location).opposite();
                tryToMove(away);
            } else {
                tryToMove(dir);
            }
        }
    }
    
    private static void soldierCode() throws GameActionException {
        readInstructions();
        
        RobotInfo[] nearbyEnemies = rc.senseHostileRobots(rc.getLocation(), rc.getType().attackRadiusSquared);
        if (nearbyEnemies.length > 0) {
            if (rc.isWeaponReady()) {
                MapLocation toAttack = findWeakest(nearbyEnemies);
                rc.attackLocation(toAttack);
            }
            return;
        }
        
        if (rc.isCoreReady()) {
            MapLocation target = new MapLocation(targetX, targetY);
            Direction dir = rc.getLocation().directionTo(target);
            tryToMove(dir);
        }
    }

    static int turnsLeft = 0; // number of turns to move in scoutDirection
    static Direction scoutDirection = null; // random direction
    private static void pickNewDirection() throws GameActionException {
        scoutDirection = randomDirection();
        turnsLeft = 100;
    }
    private static void scoutCode() throws GameActionException {
        if (rc.isCoreReady()) {
            if (turnsLeft == 0) {
                pickNewDirection();
            } else {
                turnsLeft--;
                if (!rc.onTheMap(rc.getLocation().add(scoutDirection))) {
                    pickNewDirection();
                }
                tryToMove(scoutDirection);
            }
        }
        
        RobotInfo[] enemies = rc.senseNearbyRobots(rc.getLocation(), infinity, rc.getTeam().opponent());
        for (RobotInfo r : enemies) {
            if (r.type == RobotType.ARCHON) {
                rc.broadcastMessageSignal(FOUND_ARCHON_X, r.location.x, infinity);
                rc.broadcastMessageSignal(FOUND_ARCHON_Y, r.location.y, infinity);
                break;
            }
        }
    }

    public static void tryToMove(Direction forward) throws GameActionException{
        if(rc.isCoreReady()){
            for(int deltaD:tryDirections){
                Direction maybeForward = Direction.values()[(forward.ordinal()+deltaD+8)%8];
                if(rc.canMove(maybeForward)){
                    rc.move(maybeForward);
                    return;
                }
            }
            if(rc.getType().canClearRubble()){
                //failed to move, look to clear rubble
                MapLocation ahead = rc.getLocation().add(forward);
                if(rc.senseRubble(ahead)>=GameConstants.RUBBLE_OBSTRUCTION_THRESH){
                    rc.clearRubble(forward);
                }
            }
        }
    }
    
    private static MapLocation findWeakest(RobotInfo[] listOfRobots){
        double weakestSoFar = -100;
        MapLocation weakestLocation = null;
        for(RobotInfo r:listOfRobots){
            double weakness = r.maxHealth-r.health;
            if(weakness>weakestSoFar){
                weakestLocation = r.location;
                weakestSoFar=weakness;
            }
        }
        return weakestLocation;
    }

    private static Direction randomDirection() {
        return Direction.values()[(int)(rnd.nextDouble()*8)];
    }
    
}