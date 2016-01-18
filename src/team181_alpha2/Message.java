package team181_alpha2;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;
import battlecode.common.Signal;
import team181_alpha2.CommUtil;

public class Message {
    
    private int part1 = 0;
    private int part2 = 0;
    
    // Messages have a capacity of 31 bits each; the last 16 are occupied by the coordinates,
    // The first 15 in part1 are used for the tag, and the first 15 in part2 used for ID or extra information.
    
    /**
     *  Offset that shifts tags and ids past the last 16 bits of the message. 
     */
    private int offset = (int)Math.pow(2.0, 16);
    private int ttlOffset = (int)Math.pow(2.0, 16);
    private int threatOffset = (int)Math.pow(2.0, 23);
    
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
        this(tag, loc, 0);
    }    
    
    /** 
     * Construct a new message from scratch without an id or location
     * @param tag
     */
    public Message(int tag){
        this(tag, new MapLocation(0,0), 0);
    }       
    
    public Message(Message m){
        this(m.getTag(), m.getLocation(), m.getID());        
    }
    
    public Message(int tag, DecayingMapLocation dloc){
        setTag(tag);
        setLocation(dloc.location);
        setTTL(dloc.ttl);
        setThreat(dloc.threatLevel);
    }
    
    public void setTTL(int ttl){
        part2 = part2 | (ttl * ttlOffset);
    }
    
    public void setThreat(int threat){
        part2 = part2 | (threat * threatOffset);
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
    
    public int getTTL(){
        return (int)Math.floor(part2 / ttlOffset);
    }
    
    public int getThreat(){
        return (int)Math.floor(part2 / threatOffset);
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
