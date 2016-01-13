package team181;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Signal;

public class CommUtil {
    
    /**
     * Message types
     *
     */
    static class MessageTags {

        /**
         *  Nearest Ally Archon Location
         */
        public final static int NAAL = 55555;
        /**
         * Enemy Archon Location.x
         */
        public final static int EALX = 55565;
        /**
         * Enemy Archon Location.y
         */
        public final static int EALY = 55655;

        /**
         *  Scout Map Bounds North
         */
        public final static int SMBN = 12345;
        public final static int SMBE = 22345;
        public final static int SMBS = 32345;
        public final static int SMBW = 42345;
        /**
         *  Archon Map Bounds North
         */
        public final static int AMBN = 54321;
        public final static int AMBE = 44321;
        public final static int AMBS = 34321;
        public final static int AMBW = 24321;
        
        
        public final static int DENX = 666660;
        public final static int DENY = 666661;

    }
    
    public class Message {
        
        private int part1 = 0;
        private int part2 = 0;
        
        // Offset that shifts tags and ids past the last 16 bits of the message. 
        private int offset = (int)Math.pow(2.0, 16);
        
        //Construct message from incoming signal
        public Message(Signal signal){
            part1 = signal.getMessage()[0];
            part2 = signal.getMessage()[1];
        }
        
        //Construct a new message from scratch
        public Message(int tag, MapLocation loc, int id){
            setTag(tag);
            setLocation(loc);
            setID(id);
        }
           
        public void setTag(int tag){
            part1 = part1 | (tag * offset);
        }
        
        public void setLocation(MapLocation loc){
            part1 = part1 | packageCoordinate(loc.x);
            part2 = part2 | packageCoordinate(loc.y);
        }
        
        public void setID(int id){
            part2 = part2 | (id * offset);
        }
        
        public int getTag(){
            return (int)Math.floor(part1 / offset);
        }
        
        public int getID(){
            return (int)Math.floor(part2 / offset);
        }
        
        public MapLocation getLocation(){
            int x = packageCoordinate(part1 % offset);
            int y = packageCoordinate(part2 % offset);
            MapLocation loc = new MapLocation(x, y);
            return loc;
        }
         
        //Use this instead of broadcastMessageSignal
        public void send(RobotController rc, int radiusSquared) throws GameActionException{
            rc.broadcastMessageSignal(part1, part2, radiusSquared);
        }
        
    }
    
    // Fix for transmitting negative map coordinates.
    // Use this to before transmitting and receiving coordinates.
    public static int packageCoordinate(int coordinate) {
        int offset = 34000;
        if (coordinate > 17000)
            coordinate = coordinate - offset;
        else if (coordinate < 0)
            coordinate = coordinate + offset;
        return coordinate;
    }

}
















