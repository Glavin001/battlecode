package team181;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Signal;
import team181.CommUtil;

public class Message {
    
    private int part1 = 0;
    private int part2 = 0;
    
    /**
     *  Offset that shifts tags and ids past the last 16 bits of the message. 
     */
    private int offset = (int)Math.pow(2.0, 16);
    
    /** 
     * Construct message from incoming signal
     * @param signal
     */
    public Message(Signal signal){
        part1 = signal.getMessage()[0];
        part2 = signal.getMessage()[1];
    }
    
    /** 
     * Construct a new message from scratch
     * @param tag
     * @param loc
     * @param id
     */
    public Message(int tag, MapLocation loc, int id){
        setTag(tag);
        setLocation(loc);
        setID(id);
    }
    
    /** 
     * Construct a new message from scratch without an id
     * @param tag
     * @param loc
     */
    public Message(int tag, MapLocation loc){
        setTag(tag);
        setLocation(loc);
    }    
    
    /** 
     * Construct a new message from scratch without an id or location
     * @param tag
     */
    public Message(int tag){
        setTag(tag);
    }       
    
    public Message(Message m){
        setTag(m.getTag());
        setLocation(m.getLocation());
        setID(m.getID());        
    }
       
    public void setTag(int tag){
        part1 = part1 | (tag * offset);
    }
    
    public void setLocation(MapLocation loc){
        part1 = part1 | CommUtil.packageCoordinate(loc.x);
        part2 = part2 | CommUtil.packageCoordinate(loc.y);
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
        int x = CommUtil.packageCoordinate(part1 % offset);
        int y = CommUtil.packageCoordinate(part2 % offset);
        MapLocation loc = new MapLocation(x, y);
        return loc;
    }
     
    /**
     * Don't use this!!! use Messaging.sendMessage() instead. 
     * @param rc
     * @param radiusSquared
     * @throws GameActionException
     */
    public void send(RobotController rc, int radiusSquared) throws GameActionException{
        rc.broadcastMessageSignal(part1, part2, radiusSquared);
    }
    
}
