package team181;

import battlecode.common.*;

public class Debug {
    private static boolean debuggerOn = false;
    private static RobotController rc;
    private static final String NAMES[] = {"none", "shawn", "glavin", "justin", "evan", "cameron"};
    private static int currentNameIndex = 0;
    
    
    public static void init(RobotController rc1, String name) {
        rc = rc1;
        changeActiveUser(name);
    }
    
    public static void on() {
        debuggerOn = true;
    }
    
    public static void off() {
        debuggerOn = false;
    }
    
    public static void error(String message) {
        System.out.println(message);
    }
    
    public static void changeActiveUser(String name) {
        for(int i =0; i < NAMES.length; i++) {
            if(name == NAMES[i]) {
                currentNameIndex = i;
                return;
            }
        }
        error("Invalid name setting! Use your first name all lowercase!");
    }
    
    public static void log(String message, String name) {
        if(debuggerOn && currentNameIndex > -1 && name == NAMES[currentNameIndex]) {
            System.out.println(message);
        }
    }
    
    public static void emptyIndicatorStrings() {
        for (int i = 0; i < 3; i++) {
            rc.setIndicatorString(i, "");
        }
    }

}
