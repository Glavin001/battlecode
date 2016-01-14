package team181;

import battlecode.common.MapLocation;

public class DecayingMapLocation {
    public MapLocation location;
    public int ttl;
    
    public DecayingMapLocation(MapLocation loc, int decay) {
        location = loc;
        ttl = decay;
    }
}
