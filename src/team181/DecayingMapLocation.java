package team181;

import battlecode.common.MapLocation;

public class DecayingMapLocation {
    public MapLocation location;
    public int ttl;
    public int threatLevel;
    
    public static final int defaultTTL = 20;
    //Max values that can be stored in 7 bits
    public static final int maxTTL = 127;
    public static final int maxThreat = 127;
    
    public DecayingMapLocation(MapLocation loc, int threatLevel, int ttl) {
        this.location = loc;
        this.ttl = Math.min(ttl, maxTTL);
        this.threatLevel = Math.min(ttl, maxTTL);
    }
    
    public DecayingMapLocation(MapLocation loc, int threatLevel) {
        this(loc, threatLevel, defaultTTL);
    }    
}
