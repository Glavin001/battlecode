package team181;

public class CommUtil {
    
    /**
     * Message types
     *
     */
    static class MessageTags {

        //Note that these must be each less than 16384 !
        
        /**
         *  Nearest Ally Archon Location
         */
        public final static int NAAL = 11111;
        /**
         * Enemy Archon Location.x
         */
        public final static int EALX = 11112;
        /**
         * Enemy Archon Location.y
         */
        public final static int EALY = 11113;

        /**
         *  Scout Map Bounds North
         */
        public final static int SMBN = 11114;
        public final static int SMBE = 11115;
        public final static int SMBS = 11116;
        public final static int SMBW = 11117;
        /**
         *  Archon Map Bounds North
         */
        public final static int AMBN = 11121;
        public final static int AMBE = 11122;
        public final static int AMBS = 11123;
        public final static int AMBW = 11124;
        
        
        public final static int ZDEN = 20000;

    }
    
    // Fix for transmitting negative map coordinates.
    // Use message constructor instead of this.
    public static int packageCoordinate(int coordinate) {
        int offset = 34000;
        if (coordinate > 17000)
            coordinate = coordinate - offset;
        else if (coordinate < 0)
            coordinate = coordinate + offset;
        return coordinate;
    }

}
















