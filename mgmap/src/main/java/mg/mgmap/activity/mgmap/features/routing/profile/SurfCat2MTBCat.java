package mg.mgmap.activity.mgmap.features.routing.profile;

public class SurfCat2MTBCat {
    public int maxSL = 6; // Surface categories without MTB scale
    public int maxUptoDn = 3; // maximum difference mtbUp - mtbDn considered
    public int maxDn = 6; // maximum downhill category
    public int maxUp = 6; // maximum uphill category
    public int maxScDn = maxSL+1+maxDn +1; // maximum number of factors depending on the surfaced category for downhill calculation
    public int maxScUp = maxSL+1+maxUp +1;         // maximum number of factors depending on the surfaced category for uphill calculation
    public int maxScUpExt = maxScUp + maxDn+1;

    int maxCatUpDn    = maxSL + 1 + (maxUptoDn+1)*(maxDn+1); // all surface categories including those ones without any mtb classification and those ones with up and down classification
    public int maxSurfaceCat = maxCatUpDn + maxDn + 1 ; // includes on top those ones, which have only downhill classification
 // slope which all profiles use


    /*
       SurfaceCat is devided in 3 ranges:
       -the first few ones (SurfaceCat < maxSL ) where there is no mtb classification neither up nor down available.
        In this range the surfaceCat is equal to the surfaceLevel, which starts with very smooth big asphalt streets and ends with trails(path) without any mtb classification.
       -than all values for those where both a down and up classification is available ( maxSL <= SurfaceCat <= maxCatUpDn ).
        SurfaceCat is calculated by the difference of up minus down. However the difference is limited to 3 levels ( E.g. for MtbDn = 1 1<=MtbUp<=4 ) and each combination is represented
        by one SurfaceCat
        -last but not least all values where only down classification is given ( maxCatUpDn < SurfaceCat <= maxSurfaceCat ). Here one surfaceCat is reserved for each MtbDn
     */

    public int getSurfaceCat(int surfaceLevel, int mtbDn, int mtbUp) {
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

    public int getSurfaceLevel(int surfaceCat){
        return Math.min(surfaceCat, maxSL);
    }


    public int getMtbUp(int surfaceCat){
        if (surfaceCat <= maxSL  )           return -1;
        else  if (surfaceCat < maxCatUpDn)   return (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                                 return -1;
    }



    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + uphill classification (mtbUp)
    int getCatUp(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return maxSL;
    }

    public int getCatUpExt(int surfaceCat){
        if (surfaceCat <= maxSL)           return surfaceCat;
        else if (surfaceCat < maxCatUpDn)  return maxSL+1 + (surfaceCat-maxSL-1)%(maxUptoDn+1) + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                               return surfaceCat - maxCatUpDn + maxScUp;
    }


    // calculates uphill category based on surface category. Its either between 0 and 6 for anything without MTB classification or 6 + 1 + downhill classification (mtbDn)
    public int getMtbDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return -1;
        else if (surfaceCat < maxCatUpDn) return (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else                              return surfaceCat - maxCatUpDn;
    }

    public int getCatDn(int surfaceCat){
        if (surfaceCat <= maxSL)          return surfaceCat;
        else if (surfaceCat < maxCatUpDn) return maxSL+1 + (surfaceCat-maxSL-1)/(maxUptoDn+1);
        else
            return maxSL+1 + surfaceCat - maxCatUpDn;
    }


    public boolean isValidSc(int surfaceCat){
        return  getCatUp(surfaceCat) < maxScUp ;
    }


}
