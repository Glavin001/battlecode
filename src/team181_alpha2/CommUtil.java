package team181_alpha2;

public class CommUtil {
    
    /**
     * Message types
     *
     */
    static class MessageTags {

        // Note that these must be each less than 16384 !
        // Also note that these must have their last digit set to 0.
        
        /**
         *  Nearest Ally Archon Location
         */
        public final static int NAAL = 11110;
        /**
         * Enemy Archon Location
         */
        public final static int EARL = 11120;

        /**
         *  Scout Map Bounds North
         */
        public final static int SMBN = 11130;
        public final static int SMBE = 11140;
        public final static int SMBS = 11150;
        public final static int SMBW = 11160;
        
        //Zombie den location
        public final static int ZDEN = 10070;
        
        //Parts Location
        public final static int PART = 10080;
        
        //Enemy Soldier Cluster
        public final static int CLUS = 10090;
        
        //Neutral Robots Location
        public final static int NEUT = 10100;

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
















