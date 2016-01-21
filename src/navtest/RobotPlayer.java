package navtest;

import java.util.Random;

import battlecode.common.*;

public class RobotPlayer {
    
    public static RobotController rc;
    public static boolean isBugging = false;
    public static int leastDistToTarget = 0;
    public static MapLocation[] previousLocations = {new MapLocation(0,0), new MapLocation(0,0)};
    public static MapLocation myCurrentLocation;
    public static boolean firstBug = false;
    public static int myPatience = 150;
    public static int basePatience = 150;
    public static boolean goingRight = true;

    public static boolean directMove(Direction dir) throws GameActionException{
        if(rc.canMove(dir)){
            updatePreviousLocations();
            rc.move(dir);
        }else{
            return false;
        }
        return true;
    }
    
    public static int getDistToTarget(MapLocation target){
        return rc.getLocation().distanceSquaredTo(target);
    }
    
    public static void updatePreviousLocations(){
        previousLocations[1] = previousLocations[0];
        previousLocations[0] = myCurrentLocation;
    }
    
    // Better than directionTo(); returns diagonals
    public static Direction getDirToTarget(MapLocation start, MapLocation target){
        if(start.x > target.x){
            if(start.y > target.y){
                return Direction.NORTH_WEST;
            }else if(start.y < target.y){
                return Direction.SOUTH_WEST;
            }else{
                return Direction.WEST;
            }
        }else if(start.x < target.x){
            if(start.y > target.y){
                return Direction.NORTH_EAST;
            }else if(start.y < target.y){
                return Direction.SOUTH_EAST;
            }else{
                return Direction.EAST;
            }
        }else{
            if(start.y > target.y){
                return Direction.NORTH;
            }else{
                return Direction.SOUTH;
            }
        }
    }
    
    public static boolean bugNav(MapLocation target) throws GameActionException{
        //Don't bother to move if we are already there!
        if(target.equals(myCurrentLocation)){
            return true;
        }
        Direction dirToTarget = getDirToTarget(myCurrentLocation, target);
        rc.setIndicatorString(2, "Target direction is: " + dirToTarget.toString());
        // If we were not able to move, start bugging or just move directly
        rc.setIndicatorString(0, "Am I bugging? " + Boolean.toString(isBugging));
        if(!isBugging){
            if(!directMove(dirToTarget)){
                isBugging = true;
                firstBug = true;
                myPatience = basePatience;
                //Save where we last were
                leastDistToTarget = getDistToTarget(target);
//                rc.setIndicatorString(1, "My previous least dist location was at: " + myCurrentLocation.toString());
            }            
        }
        // Need to check for bugging again if we want to move on turn we hit wall after direct move 
        if(isBugging){
            myPatience--;
            if(myPatience > 0){
                boolean canMove = false;
                //Next direction depends upon whether this is our first bug move or not
                Direction nextDirection;
                
                if(firstBug){
                    //Decide which wall to stick to.
//                    MapLocation rightSquare = myCurrentLocation.add(getDirToTarget(myCurrentLocation, target).rotateRight());
//                    MapLocation leftSquare = myCurrentLocation.add(getDirToTarget(myCurrentLocation, target).rotateLeft());
//                    if(rightSquare.distanceSquaredTo(target) < leftSquare.distanceSquaredTo(target)){
//                        goingRight = true;
//                    }else{
//                        goingRight = false;
//                    }
//                    if(goingRight){
                        nextDirection = getDirToTarget(myCurrentLocation, target).rotateRight();                                
//                    }else{
//                        nextDirection = getDirToTarget(myCurrentLocation, target).rotateLeft();                                
//                    }
                }else{
//                    if(goingRight){
                        nextDirection = getDirToTarget(myCurrentLocation, previousLocations[0]).rotateRight();                                                        
//                    }else{
//                        nextDirection = getDirToTarget(myCurrentLocation, previousLocations[0]).rotateLeft();                                                        
//                    }
                }
                int directionsTried = 0;
                // Break when we move, don't get stuck in infinite loop
                while(true){
                    MapLocation nextLocation = myCurrentLocation.add(nextDirection);
                    // Don't move to previous locations
                    rc.setIndicatorDot(nextLocation, 255, 60, 60);
//                    System.out.println("I checked location: " + nextLocation.toString());
                    if(rc.canMove(nextDirection)){ 
                            if(nextLocation.equals(previousLocations[0]) || nextLocation.equals(previousLocations[1])){
                                if(directionsTried < 7){
                                    canMove = false;
                                }else{
                                    canMove = true;                             
                                }
                            }else{
                                  canMove = true;
                            }
                    }else{
                        canMove = false;
                    }
                    
                    if(canMove){
//                        System.out.println("My two previous locations were: " 
//                                + previousLocations[0].toString()
//                                + previousLocations[1].toString()
//                                + " Going to: " + nextLocation.toString());          
                        updatePreviousLocations();
                        rc.move(nextDirection);
                        firstBug = false;
                        // If we get closer to the target, then stop bugging
                        if(getDistToTarget(target) <= leastDistToTarget){
                            isBugging = false;
//                            System.out.println("I reset to normal pathfinding next turn.");
                        }     
                        break;
                    }else{
                        nextDirection = nextDirection.rotateRight();
                        directionsTried++;                    
                    }
                }       
            }else{
                isBugging = false;
            }
        }
        return false;   
    }
    
    public static void run(RobotController inrc){
        
        rc = inrc;
        MapLocation target = new MapLocation(16,4);
        myCurrentLocation = rc.getLocation();
        previousLocations[0] = myCurrentLocation;
        previousLocations[1] = myCurrentLocation;
        boolean atTarget = false;
        Random rand = new Random(rc.getID());
        while(true){
            try{
                myCurrentLocation = rc.getLocation();
                rc.setIndicatorDot(previousLocations[0], 80, 255, 80);
                rc.setIndicatorDot(previousLocations[1], 80, 255, 80);
                rc.setIndicatorDot(target, 200, 60, 200);
//                rc.setIndicatorString(1, "My target is at: " + target.toString());
                rc.setIndicatorString(1, "My patience is: " + Integer.toString(myPatience));
                if(rc.isCoreReady()){
                    atTarget = bugNav(target);
                }
                if(atTarget){
                    target = new MapLocation(rand.nextInt(30), rand.nextInt(30));
                }
                if(myPatience <= 0){
                    target = new MapLocation(rand.nextInt(30), rand.nextInt(30));
                }
                Clock.yield();
            }catch(GameActionException e){
                
            }
        }            
    }
}
