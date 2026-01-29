package mg.mgmap.activity.mgmap.features.routing;

public interface IfProfileContextMTB extends IfProfileContext{

    int maxSL = 6; // Surface categories without MTB scale
    int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered
    int maxDn = 6; // maximum downhill category
    int maxUp = 6; // maximum uphill category
    int maxScDn = maxSL+1+maxDn +1; // maximum number of factors depending on the surfaced category for downhill calculation
    int maxScUp = maxSL+1+maxUp +1;         // maximum number of factors depending on the surfaced category for uphill calculation
    int maxScUpExt = maxScUp + maxDn+1;
    // Heuristic is derived from a path with mtbDn = 0 and mtbUp = 0. All other surface categories have higher costs, either because they are disfavored like for anything without mtb classification or because they more difficult
    int HeuristicRefSurfaceCat = 7;
    float[] sdistFactforCostFunct = {  3.0f   ,2.4f ,2.0f  ,1.80f ,1.5f  ,1.4f }; //factors to increase costs compared to durations to get better routing results
    float[] ssrelSlope            = {  1.4f   ,1.2f ,1f    ,1f    ,1f    ,1f  , 0f    ,1.2f  ,1.2f  ,1.2f  ,1.2f  ,1.2f ,1f   ,1f }; //slope of auxiliary function for duration function at 0% slope to get to -4% slope

    int maxCatUpDn    = maxSL + 1 + (maxUptoDn+1)*(maxDn+1); // all surface categories including those ones without any mtb classification and those ones with up and down classification
    int maxSurfaceCat = maxCatUpDn + maxDn + 1 ; // includes on top those ones, which have only downhill classification
    float refDnSlopeOpt = -0.04f; // slope at which cost function is optimized against slope of reference function
    float refDnSlope = -0.2f; // slope which all profiles use

    static float sig(double base){
        return (float) (1./(1.+Math.exp(base)));};

    static float dSM20scDnLow(int scDn){
        return 0.2f * ( sig(1.5 * (scDn - 2.)) - 0.5f);
    }

    static int getSurfaceCat(int surfaceLevel, int mtbDn, int mtbUp) {
        if (surfaceLevel < 0 || surfaceLevel > maxSL) throw new RuntimeException("invalid Surface Level");
        if (surfaceLevel == maxSL ) {
            int scUp;
            int scDn;
            if (mtbDn > -1)
                if (mtbUp > -1) {
                    scUp = mtbDn-mtbUp>=0 ? 0 : (Math.min(mtbUp - mtbDn, maxUptoDn));
                    scDn = mtbDn;
                } else {
                    return maxCatUpDn+mtbDn;
//                    scUp = 1; scDn = mtbDn;
                }
            else if (mtbUp > -1) {
                scUp = mtbUp == 0?0:1;
                scDn = mtbUp == 0?0:mtbUp-1;
            } else {
                scUp = -1;
                scDn =  0;
            }
            return maxSL + 1 + (maxUptoDn+1)*scDn + scUp;
        } else
            return surfaceLevel;
    }

    static int getSurfaceLevel(int surfaceCat){
        return Math.min(surfaceCat, maxSL);
    }


    public static int getMtbUp(int surfaceCat){
        if (surfaceCat <= maxSL  )           return -1;
        else  if (surfaceCat < maxCatUpDn)   return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                                 return -1;
    }



    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + uphill classification (mtbUp)
    static int getCatUp(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return maxSL;
    }

    static int getCatUpExt(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return surfaceCat - maxCatUpDn + maxScUp;
    }


    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + downhill classification (mtbDn)
    public static int getMtbDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return -1;
        else if (surfaceCat < maxCatUpDn) return (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                              return surfaceCat - maxCatUpDn;
    }

    public static int getCatDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return surfaceCat;
        else if (surfaceCat < maxCatUpDn) return maxSL+1 + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else
            return maxSL+1 + surfaceCat - maxCatUpDn;
    }

    default int getScProfileSpline(){
        return maxSL;
    }
    default int getScHeuristicRefSpline(){
        return HeuristicRefSurfaceCat;
    };
    default boolean isValidSc(int surfaceCat){
        return  getCatUp(surfaceCat) < maxScUp ;
    }

    default int getMaxSurfaceCat(){
        return maxSurfaceCat;
    }

}
