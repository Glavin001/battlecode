package team181;

import battlecode.common.*;

public class Util {

    public static int countRobotsByRobotType(RobotInfo[] infos, RobotType type) {
        if (infos.length == 0) {
            return 0;
        } else {
            int count = 0;
            for (int i=0; i<infos.length; i++) {
                if (infos[i].type.equals(type)) {
                    count++;
                }
            }
            return count;
        }
    }
}
