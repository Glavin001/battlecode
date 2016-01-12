package shazbot;

import battlecode.common.Clock;
import battlecode.common.Team;
import shazbot.RobotPlayer.Debug;
import shazbot.RobotPlayer.Messaging;
import shazbot.RobotPlayer.Sensing;

public class SoldierClass extends RobotPlayer {

  public static void run() {
    while (true) {
      try {
        Debug.emptyIndicatorStrings();
        Sensing.updateNearbyEnemies();
        Messaging.handleMessageQueue();

        // If there are more enemies than allies nearby, retreat to the
        // nearest archon.
        if (attackableTraitors.length + attackableZombies.length > veryCloseAllies.length) {
          Movement.retreatToArchon();
        }

        // Retreat if our health is less than 50%
        if (rc.getHealth() < myRobotType.maxHealth / 2) {
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

        Clock.yield();
      } catch (Exception e) {
        System.out.println(e.getMessage());
        e.printStackTrace();
      }
    }
  }
}
