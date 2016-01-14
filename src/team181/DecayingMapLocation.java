package team181;

import battlecode.common.MapLocation;

public class DecayingMapLocation {
    public MapLocation location;
    public int ttl;
    public int threatLevel;
    
    public static final int defaultTTL = 40;
    
    public DecayingMapLocation(MapLocation loc, int threatLevel, int ttl) {
        location = loc;
        this.ttl = ttl;
        this.threatLevel = threatLevel;
    }
    
    public DecayingMapLocation(MapLocation loc, int threatLevel) {
        location = loc;
        this.ttl = defaultTTL;
        this.threatLevel = threatLevel;
    }    
}
