package team181;

import static org.junit.Assert.*;
import org.junit.Test;

import battlecode.common.*;

public class RobotPlayerTest {

    @Test
    public void testSanity() {
        assertEquals(2, 1 + 1);
    }

    @Test
    public void testViperRankAttackPriority() {
        ViperPlayer.myLocation = new MapLocation(0, 0);
        
        MapLocation closeLoc = new MapLocation(1,1);
        MapLocation farLoc = new MapLocation(10,10);
        
        RobotInfo infectedSoldier = new RobotInfo(0, Team.A, RobotType.SOLDIER, farLoc, 0, 0,
                RobotType.SOLDIER.attackPower, RobotType.SOLDIER.maxHealth, RobotType.SOLDIER.maxHealth, 0, 20);
        RobotInfo soldier = new RobotInfo(0, Team.A, RobotType.SOLDIER, farLoc, 0, 0,
                RobotType.SOLDIER.attackPower, RobotType.SOLDIER.maxHealth, RobotType.SOLDIER.maxHealth, 0, 0);
        RobotInfo closeSoldier = new RobotInfo(0, Team.A, RobotType.SOLDIER, closeLoc, 0, 0,
                RobotType.SOLDIER.attackPower, RobotType.SOLDIER.maxHealth, RobotType.SOLDIER.maxHealth, 0, 0);
        RobotInfo zombieSoldier = new RobotInfo(0, Team.ZOMBIE, RobotType.SOLDIER, farLoc, 0, 0,
                RobotType.SOLDIER.attackPower, RobotType.SOLDIER.maxHealth, RobotType.SOLDIER.maxHealth, 0, 0);
        
        double rankInfectedSoldier = ViperPlayer.rankRobotAttackPriority(infectedSoldier);
        double rankSoldier = ViperPlayer.rankRobotAttackPriority(soldier);
        double rankCloseSoldier = ViperPlayer.rankRobotAttackPriority(closeSoldier);
        double rankZombieSoldier = ViperPlayer.rankRobotAttackPriority(zombieSoldier);
        
        System.out.println(rankInfectedSoldier);
        System.out.println(rankSoldier);
        System.out.println(rankCloseSoldier);
        System.out.println(rankZombieSoldier);
        
        assertEquals(rankInfectedSoldier < rankSoldier, true);
        assertEquals(rankCloseSoldier > rankSoldier, true);
        assertEquals(rankZombieSoldier < rankSoldier, true);

    }

}
