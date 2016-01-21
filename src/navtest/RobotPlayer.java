package navtest;

import battlecode.common.*;

public class RobotPlayer {
    
    public static RobotController rc;
    public static boolean isBugging = false;
    public static int leastDistToTarget = 0;
    public static MapLocation[] previousLocations = {new MapLocation(0,0), new MapLocation(0,0)};
    
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
        previousLocations[0] = rc.getLocation();
    }
    
    public static void bugNav(MapLocation target) throws GameActionException{
        Direction dirToTarget = rc.getLocation().directionTo(target);
        // If we were not able to move, start bugging or just move directly
        if(!isBugging){
            if(!directMove(dirToTarget)){
                isBugging = true;
                //Save where we last were
                leastDistToTarget = getDistToTarget(target);
                rc.setIndicatorString(1, "My previous least dist location was at: " + rc.getLocation().toString());
            }            
        }
        // Need to check for bugging again if we want to move on turn we hit wall after direct move 
        if(isBugging){
            boolean canMove = false;
            Direction nextDirection = dirToTarget.rotateRight();
            int directionsTried = 0;
            // Break when we move, don't get stuck in infinite loop
            while(true){
                MapLocation nextLocation = rc.getLocation().add(nextDirection);
                // Don't move to previous locations
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
                    System.out.println("My two previous locations were: " 
                            + previousLocations[0].toString()
                            + previousLocations[1].toString()
                            + " Going to: " + nextLocation.toString());          
                    updatePreviousLocations();
                    rc.move(nextDirection);
                    // If we get closer to the target, then stop bugging
                    if(getDistToTarget(target) < leastDistToTarget){
                        isBugging = false;
                    }     
                    break;
                }else{
                    nextDirection = nextDirection.rotateRight();
                    directionsTried++;                    
                }
            }       
        }
    }
    
    public static void run(RobotController inrc){
        
        rc = inrc;
        MapLocation target = new MapLocation(16,4);
        previousLocations[0] = rc.getLocation();
        previousLocations[1] = rc.getLocation();
        while(true){
            try{
                rc.setIndicatorString(0, "Am I bugging? " + Boolean.toString(isBugging));
                rc.setIndicatorDot(previousLocations[0], 80, 255, 80);
                rc.setIndicatorDot(previousLocations[1], 80, 255, 80);
                if(rc.isCoreReady()){
                    bugNav(target);
                }
                Clock.yield();
            }catch(GameActionException e){
                
            }
        }            
    }
}
