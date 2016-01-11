package team001;

import battlecode.common.*;

import java.util.Random;

public class RobotPlayer {
	private static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, Direction.SOUTH_EAST,
            Direction.SOUTH, Direction.SOUTH_WEST, Direction.WEST, Direction.NORTH_WEST};
	private static Random rand;
	private static RobotController rc;
//	private static 
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
//    @SuppressWarnings("unused")
    public static void run(RobotController rc1) {
        // You can instantiate variables here.
		rc = rc1;
        rand = new Random(rc.getID());
        int myAttackRange = 0;
        Team myTeam = rc.getTeam();
        Team enemyTeam = myTeam.opponent();
        
        if (rc.getType() == RobotType.ARCHON) {
            makeArchonMoves(rc, myAttackRange, enemyTeam);
        } else if (rc.getType() == RobotType.SCOUT) {
        	makeScoutMoves(rc, myAttackRange, enemyTeam);
        } else if (rc.getType() == RobotType.SOLDIER) {
        	makeSoliderMoves(rc, myAttackRange, enemyTeam);
        } else if (rc.getType() == RobotType.GUARD) {
        	makeGuardMoves(rc, myAttackRange, enemyTeam);
        } else if (rc.getType() == RobotType.VIPER) {
        	makeViperMoves(rc, myAttackRange, enemyTeam);
        } else if (rc.getType() == RobotType.TURRET) {
        	makeTurretMoves(rc, myAttackRange, enemyTeam);
        }
    }
    private static void makeArchonMoves(RobotController rc, int myAttackRange, Team enemyTeam) {
    	RobotType[] robotTypes = {RobotType.SCOUT, RobotType.SOLDIER, RobotType.SOLDIER, RobotType.SOLDIER,
                RobotType.GUARD, RobotType.GUARD, RobotType.VIPER, RobotType.TURRET};
    	try {
            // Any code here gets executed exactly once at the beginning of the game.
        } catch (Exception e) {
            // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
            // Caught exceptions will result in a bytecode penalty.
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                int fate = rand.nextInt(1000);
                // Check if this ARCHON's core is ready
                if (fate % 10 == 2) {
                    // Send a message signal containing the data (6370, 6147)
                    rc.broadcastMessageSignal(6370, 6147, 80);
                }
                Signal[] signals = rc.emptySignalQueue();
                if (signals.length > 0) {
                    // Set an indicator string that can be viewed in the client
                    rc.setIndicatorString(0, "I received a signal this turn!");
                } else {
                    rc.setIndicatorString(0, "I don't any signal buddies");
                }
                if (rc.isCoreReady()) {
                    if (fate < 800) {
                        // Choose a random direction to try to move in
                        Direction dirToMove = directions[fate % 8];
                        // Check the rubble in that direction
                        if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                            // Too much rubble, so I should clear it
                            rc.clearRubble(dirToMove);
                            // Check if I can move in this direction
                        } else if (rc.canMove(dirToMove)) {
                            // Move
                            rc.move(dirToMove);
                        }
                    } else {
                        // Choose a random unit to build
                        RobotType typeToBuild = robotTypes[fate % 8];
                        // Check for sufficient parts
                        if (rc.hasBuildRequirements(typeToBuild)) {
                            // Choose a random direction to try to build in
                            Direction dirToBuild = directions[rand.nextInt(8)];
                            for (int i = 0; i < 8; i++) {
                                // If possible, build in this direction
                                if (rc.canBuild(dirToBuild, typeToBuild)) {
                                    rc.build(dirToBuild, typeToBuild);
                                    break;
                                } else {
                                    // Rotate the direction to try
                                    dirToBuild = dirToBuild.rotateLeft();
                                }
                            }
                        }
                    }
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void makeScoutMoves(RobotController rc, int myAttackRange, Team enemyTeam){
    	makeNonTurretMoves(rc, myAttackRange, enemyTeam);
    }
    
    private static void makeSoliderMoves(RobotController rc, int myAttackRange, Team enemyTeam){
    	makeNonTurretMoves(rc, myAttackRange, enemyTeam);
    }
    
    private static void makeGuardMoves(RobotController rc, int myAttackRange, Team enemyTeam){
    	makeNonTurretMoves(rc, myAttackRange, enemyTeam);
    }
    
    private static void makeViperMoves(RobotController rc, int myAttackRange, Team enemyTeam){
    	makeNonTurretMoves(rc, myAttackRange, enemyTeam);
    }

    private static void makeNonTurretMoves(RobotController rc, int myAttackRange, Team enemyTeam) {
    	MapLocation goal;
    	try {
            // Any code here gets executed exactly once at the beginning of the game.
            myAttackRange = rc.getType().attackRadiusSquared;
            MapLocation currentLocation = rc.getLocation();
            
            if(currentLocation.y > (GameConstants.MAP_MAX_HEIGHT/2) + 5) {
            	goal = new MapLocation(GameConstants.MAP_MAX_WIDTH -5, 5);
            } else {
            	goal = new MapLocation(5, GameConstants.MAP_MAX_HEIGHT - 5);
            }
            System.out.println(goal);
        } catch (Exception e) {
            // Throwing an uncaught exception makes the robot die, so we need to catch exceptions.
            // Caught exceptions will result in a bytecode penalty.
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    	
    	int fate = 0;
    	// Choose a random direction to try to move in

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
            	Direction dirToMove = directions[fate % 8];

                if (fate % 5 == 3) {
                    // Send a normal signal
                    rc.broadcastSignal(80);
                }

                boolean shouldAttack = false;

                // If this robot type can attack, check for enemies within range and attack one
                if (myAttackRange > 0) {
                    RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
                    RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
                    if (enemiesWithinRange.length > 0) {
                        shouldAttack = true;
                        // Check if weapon is ready
                        if (rc.isWeaponReady()) {
                            rc.attackLocation(enemiesWithinRange[rand.nextInt(enemiesWithinRange.length)].location);
                        }
                    } else if (zombiesWithinRange.length > 0) {
                        shouldAttack = true;
                        // Check if weapon is ready
                        if (rc.isWeaponReady()) {
                            rc.attackLocation(zombiesWithinRange[rand.nextInt(zombiesWithinRange.length)].location);
                        }
                    }
                }
                if (!shouldAttack) {
                	boolean didMove = false;
                    if (rc.isCoreReady()) {
                        if (fate < 600) {
                            // Check the rubble in that direction
                            if (rc.senseRubble(rc.getLocation().add(dirToMove)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH) {
                                // Too much rubble, so I should clear it
                                rc.clearRubble(dirToMove);
                                didMove = true;
                                // Check if I can move in this direction
                            } else if (rc.canMove(dirToMove)) {
                                // Move
                                rc.move(dirToMove);
                                didMove = true;
                      
                            }
                        }
                    }
                    if (!didMove) {
//                    	fate++;
                    }
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    private static void makeTurretMoves(RobotController rc, int myAttackRange, Team enemyTeam) {
    	try {
            myAttackRange = rc.getType().attackRadiusSquared;
        } catch (Exception e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        while (true) {
            // This is a loop to prevent the run() method from returning. Because of the Clock.yield()
            // at the end of it, the loop will iterate once per game round.
            try {
                // If this robot type can attack, check for enemies within range and attack one
                if (rc.isWeaponReady()) {
                    RobotInfo[] enemiesWithinRange = rc.senseNearbyRobots(myAttackRange, enemyTeam);
                    RobotInfo[] zombiesWithinRange = rc.senseNearbyRobots(myAttackRange, Team.ZOMBIE);
                    if (enemiesWithinRange.length > 0) {
                        for (RobotInfo enemy : enemiesWithinRange) {
                            // Check whether the enemy is in a valid attack range (turrets have a minimum range)
                            if (rc.canAttackLocation(enemy.location)) {
                                rc.attackLocation(enemy.location);
                                break;
                            }
                        }
                    } else if (zombiesWithinRange.length > 0) {
                        for (RobotInfo zombie : zombiesWithinRange) {
                            if (rc.canAttackLocation(zombie.location)) {
                                rc.attackLocation(zombie.location);
                                break;
                            }
                        }
                    }
                }

                Clock.yield();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
