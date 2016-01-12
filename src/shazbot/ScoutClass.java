package shazbot;

import java.util.Arrays;

import battlecode.common.Clock;
import battlecode.common.Direction;
import battlecode.common.GameActionException;
import battlecode.common.GameConstants;
import battlecode.common.MapLocation;
import battlecode.common.RobotInfo;
import battlecode.common.RobotType;
import battlecode.common.Signal;
import battlecode.common.Team;
import shazbot.RobotPlayer.Debug;
import shazbot.RobotPlayer.Messaging;
import shazbot.RobotPlayer.Sensing;
import shazbot.RobotPlayer.messageConstants;

public class ScoutClass extends RobotPlayer {

  // Get the maximum number of tiles in one direction away in sensor radius
  static int myDv = (int) Math.floor(Math.sqrt(myRobotType.sensorRadiusSquared));
  static Direction[] cardinals = { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };
  static boolean[] haveOffsets = { false, false, false, false };
  static Direction currentExploreDirection = Direction.NORTH;
  static int numExploredDirections = 0;
  static boolean haveBroadCastedMapBounds = false;

  // Enclosure for all of the exploration functions
  static class Exploration {

    // Dumb help function, please kill me and replace with a map
    public static int returnCardinalIndex(Direction dir) throws GameActionException {
      // Switch because no associative arrays, could map this instead, but
      // that might cost more
      switch (dir) {
      case NORTH:
        return 0;
      case EAST:
        return 1;
      case SOUTH:
        return 2;
      case WEST:
        return 3;
      default:
        GameActionException e = new GameActionException(null, "Not a valid cardinal direction.");
        throw e;
      }
    }

    // Given a direction, try to move that way, but avoid taking damage or
    // going far into enemy LOF.
    // General Scout exploration driver.
    // Don't let scout get stuck in concave areas, or near swarms of allies.
    public static void explore(Direction dirToMove) throws GameActionException {
      if (rc.isCoreReady()) {
        if (nearbyEnemies.length == 0) {
          // There are no known enemy threats
          if (rc.canMove(dirToMove)) {
            rc.move(dirToMove);
          } else if (rc.canMove(dirToMove.rotateLeft())) {
            rc.move(dirToMove.rotateLeft());
          } else if (rc.canMove(dirToMove.rotateRight())) {
            rc.move(dirToMove.rotateRight());
          }
        } else {
          // There are enemies within sight!
          // Calculate least risking direction to move
          MapLocation currLoc = rc.getLocation();
          double leastRisk = Double.MAX_VALUE; // risks[0];
          Direction leastRiskyDirection = Direction.NONE;
          Direction[] allDirs = Direction.values();
          MapLocation goalLoc = currLoc.add(dirToMove);
          MapLocation leastRiskyLoc = goalLoc;
          double[] risks = new double[allDirs.length];
          for (int i = 0; i < allDirs.length; i++) {
            // Check if can move in this direction
            Direction currDir = allDirs[i];
            if (rc.canMove(currDir)) {
              // Can move in this direction
              // Check attack risk value
              MapLocation nextLoc = currLoc.add(currDir);
              double risk = attackRisk(nextLoc);
              risks[i] = risk;
              // System.out.println("At location" +
              // currLoc.toString() + " risk in direction " +
              // currDir.toString() + " is: " +
              // Double.toString(risk));
              // Is this better?
              // Bias towards moving the same direction, dirToMove
              if (currDir.equals(dirToMove) && risk <= leastRisk) {
                // At least as good
                leastRisk = risk;
                leastRiskyDirection = dirToMove;
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
          if (!leastRiskyDirection.equals(Direction.NONE)) {
            // rc.setIndicatorString(2, "Retreating towards: " +
            // leastRiskyDirection.toString());
            rc.setIndicatorString(2, "Risk this turn is: " + Arrays.toString(risks));
            rc.move(leastRiskyDirection);
          }

        }
      }
    }

    /*
     * Calculation the Attack risk value of moving to this location given
     * the direction.
     *
     * Attack risk is currently the count of the number of enemies which
     * will be within attack range of this location Ideal attack risk is 0
     * (no risk of attack).
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
          if ((distAway + offsetSquared) <= enemyType.attackRadiusSquared) {
            // If enemy has 0 attack power then risk = 0
            // If Core delay is 0 then risk = numerator, and will be
            // divided by each turn/core delay
            double risk = ((enemyType.attackRadiusSquared - (distAway + offsetSquared)) * r.attackPower)
                / (r.coreDelay + 1.0);
            totalRisk += risk;
          }
        }
        // rc.setIndicatorString(2, "Risk this turn is: " +
        // Double.toString(totalRisk));
        System.out.println("The total risk for possible location " + loc.toString() + " was: "
            + Double.toString(totalRisk));
        return totalRisk;
      } else {
        return 0.0;
      }
    }

    // Tells us if a point in a given cardinal direction at our maximum
    // sight
    // range is on the map
    // This should only take in north,south,east,west
    public static boolean checkCardinalOnMap(Direction dir) throws GameActionException {
      MapLocation offsetLocation = rc.getLocation().add(dir, myDv);
      rc.setIndicatorDot(offsetLocation, 255, 80, 80);
      boolean onMap = rc.onTheMap(offsetLocation);
      rc.setIndicatorString(0, dir.toString() + " point on map?: " + Boolean.toString(onMap));
      return onMap;
    }

    // This function sets the value of a given direction bound, if it can at
    // all
    // this round.
    // Call this after checkCardinalOnMap returns false.
    public static void findExactOffset(Direction dir) throws GameActionException {
      for (int i = myDv; i > 0; i--) {
        MapLocation temp = rc.getLocation().add(dir, i);
        if (rc.onTheMap(temp)) {
          int bound = (dir == Direction.NORTH || dir == Direction.SOUTH) ? temp.y : temp.x;
          setMapBound(dir, bound);
          haveOffsets[returnCardinalIndex(dir)] = true;
          // rc.setIndicatorString(0, dir.toString() + " bound value
          // is :
          // " + Integer.toString(offsets[returnCardinalIndex(dir)]));
          numExploredDirections++;
          break;
        }
      }
    }

    public static void broadcastMapBounds() throws GameActionException {
      int distToNearestArchon = nearestArchon.distanceSquaredTo(rc.getLocation());
      rc.broadcastMessageSignal(messageConstants.SMBN, Messaging.adjustBound(northBound), distToNearestArchon);
      rc.broadcastMessageSignal(messageConstants.SMBE, Messaging.adjustBound(eastBound), distToNearestArchon);
      rc.broadcastMessageSignal(messageConstants.SMBS, Messaging.adjustBound(southBound), distToNearestArchon);
      rc.broadcastMessageSignal(messageConstants.SMBW, Messaging.adjustBound(westBound), distToNearestArchon);
      haveBroadCastedMapBounds = true;
    }

    public static void tryExplore() throws GameActionException {
      // If we have not found every bound
      if (numExploredDirections != 4 && allBoundsSet != true) {
        // If we don't already have a bound for this direction
        if (!haveOffsets[returnCardinalIndex(currentExploreDirection)]) {
          // If we go off the map in sight range for the given
          // direction,
          // we can get the offset
          if (!checkCardinalOnMap(currentExploreDirection)) {
            findExactOffset(currentExploreDirection);
            currentExploreDirection = cardinals[numExploredDirections % 4];
            // Otherwise go explore in that direction.
          } else {
            explore(currentExploreDirection);
          }
        }
      } else if (!haveBroadCastedMapBounds) {
        broadcastMapBounds();
      } else {
        // Movement.randomMove();
        explore(Movement.randomDirection());
      }
    }
  }

  static class ScoutMessaging {

    public static void handleMessageQueue() throws GameActionException {
      // SUPER
      Messaging.handleMessageQueue();
      // currentSignals[] is now set for this round. Overflow may cause
      // problems.
      if (currentSignals.length > 0) {
        for (Signal signal : currentSignals) {
          MapLocation loc = signal.getLocation();
          int msg1 = signal.getMessage()[0];
          int msg2 = signal.getMessage()[1];
          switch (msg1) {
          // Handle Scout messages that about map bounds
          case messageConstants.SMBN:
          case messageConstants.AMBN:
            // Set map bounds
            msg2 = Messaging.adjustBound(msg2);
            setMapBound(Direction.NORTH, msg2);
            break;
          case messageConstants.SMBE:
          case messageConstants.AMBE:
            msg2 = Messaging.adjustBound(msg2);
            setMapBound(Direction.EAST, msg2);
            break;
          case messageConstants.SMBS:
          case messageConstants.AMBS:
            msg2 = Messaging.adjustBound(msg2);
            setMapBound(Direction.SOUTH, msg2);
            break;
          case messageConstants.SMBW:
          case messageConstants.AMBW:
            msg2 = Messaging.adjustBound(msg2);
            setMapBound(Direction.WEST, msg2);
            break;

          }

        }
      }

    }

  }

  public static void reportDens() {
    // for(RobotInfo robot : nearbyEnemies){
    // if(robot.type == RobotType.ZOMBIEDEN &&){
    // rc.broadcastMessageSignal(messageConstants.DENX, , radiusSquared);
    // }
    // }
  }

  // Given a direction, try to move that way, but avoid taking damage or going
  // far into enemy LOF.
  // General Scout exploration driver.
  // Don't let scout get stuck in concave areas, or near swarms of allies.
  public static void pathDirectionAvoidEnemies(Direction dir) {

  }

  public static void run() {
    while (true) {
      try {
        Debug.emptyIndicatorStrings();
        Sensing.updateNearbyEnemies();
        ScoutMessaging.handleMessageQueue();

        // Wander out into the wilderness
        // find anything of interest
        // report back to archons when we have enough data
        // Give report, follow squad that gets deployed
        // Constantly broadcast to squad attack info
        // Signal troops to retreat when attack is done, or failed, or
        // when reinforcements are needed,
        // or when zombie spawn is upcoming
        Exploration.tryExplore();

        Clock.yield();
      } catch (Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
