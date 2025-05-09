package mg.mgmap.activity.mgmap.features.routing.profile;

import android.util.Log;

import mg.mgmap.generic.graph.WayAttributs;

public class TagEval {
    public static final double minDistfSc0 = 1.21;

    private TagEval(){}

    protected static boolean getNoAccess(WayAttributs wayTagEval) {
        return wayTagEval.highway == null ||
                "motorway".equals(wayTagEval.highway) || "trunk".equals(wayTagEval.highway) ||
                "private".equals(wayTagEval.bicycle)  ||
                ( "bic_no".equals(wayTagEval.bicycle) &&  !( "path".equals(wayTagEval.highway) || "track".equals(wayTagEval.highway)|| "steps".equals(wayTagEval.highway) ||
                   wayTagEval.mtbScale != null) ) ||
                ( ( "private".equals(wayTagEval.access) || "acc_no".equals(wayTagEval.access) )  &&
                       !(  "bic_yes".equals(wayTagEval.bicycle)         || "bic_designated".equals(wayTagEval.bicycle) ||
                            "bic_permissive".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network)            ||
                             "rcn".equals(wayTagEval.network)           || "icn".equals(wayTagEval.network)            ||
                             wayTagEval.mtbScaleUp != null              || wayTagEval.mtbScale != null
                       )
                );
    }

    protected static short getSurfaceCat(WayAttributs wayTagEval){
        short surfaceCat = 0;
        if (wayTagEval.surface != null) {
            switch (wayTagEval.surface) {
                case "asphalt":
                case "smooth_paved":
                case "paved":
                    surfaceCat = 1;
                    break;
                case "compacted":
                case "fine_gravel":
                case "paving_stones":
                    surfaceCat = 2;
                    break;
                case "rough_paved":
                case "gravel":
                    surfaceCat = 3;
                    break;
                case "raw":
                case "unpaved":
                case "winter":
                    surfaceCat = 4;
                    break;
                default:
                    surfaceCat = 5;
            }
        }
        short type = 0;
        if ("track".equals(wayTagEval.highway) ) {
            if (wayTagEval.trackType != null) {
                switch (wayTagEval.trackType) {
                    case "grade1":
                        type = 1;
                        break;
                    case "grade2":
                        type = 2;
                        break;
                    case "grade3":
                        type = 3;
                        break;
                    case "grade4":
                        type = 4;
                        break;
                    default:
                        type = 5;
                }
            }
        } else if ("unclassified".equals(wayTagEval.highway))
            type = 1;
        if ( "unclassified".equals(wayTagEval.highway) && wayTagEval.trackType != null){
            Log.e("TagEval","tracktype && unclassified");
        }
//        double sc = (type + surfaceCat)/2.0;
//        double sc1 = Math.ceil(sc);
//        short scs = (short) sc1;
        return (type>0 && surfaceCat>0) ? (short) Math.ceil ((double) (type + surfaceCat)/2.0): (type>0)?type:surfaceCat;
//        return  (short) Math.ceil ((double) (type + surfaceCat)/2.0); //(type > 0) ? type : surfaceCat;
    }
    protected static Factors getFactors(WayAttributs wayTagEval, short surfaceCat, boolean mtb) {
        double distFactor ;
        if ("cycleway".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat>0) ? surfaceCat :1;
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network))
                distFactor = 1.0;
            else
                distFactor = 1.3;
        } else if ("primary".equals(wayTagEval.highway) || "primary_link".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat <= 1) ? 0 : surfaceCat;
            if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 10;
            else if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.45;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network)  )
                distFactor = 1.8;
            else
                distFactor = 2.5;
        } else if ("secondary".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat <= 1) ? 0 : surfaceCat;
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.4;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network) )
                distFactor = 1.6;
            else
                distFactor = 1.8;
        } else if ("tertiary".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat <= 1) ? 0 : surfaceCat;
            if ( "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || ( wayTagEval.cycleway != null && "lcn".equals(wayTagEval.network)))
                distFactor = 1.21;
            else if (wayTagEval.cycleway != null || "lcn".equals(wayTagEval.network))
                distFactor = 1.3;
            else
                distFactor = 1.5;
        } else if ("residential".equals(wayTagEval.highway)||"living_street".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat>0) ? surfaceCat :1;
            if ("bic_destination".equals(wayTagEval.bicycle) || "lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) ) {
                distFactor = 1.0;
 /*           } else if( surfaceCat > 2){
                distFactor = 1.5; */
            } else  distFactor = 1.15;
        } else if ("footway".equals(wayTagEval.highway) || "pedestrian".equals(wayTagEval.highway)) {
            if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network) || "bic_yes".equals(wayTagEval.bicycle))
                distFactor = 1.0;
            else if  ((wayTagEval.mtbScale != null || wayTagEval.mtbScaleUp != null) && mtb && ( surfaceCat == 4|| surfaceCat == -1 )) {
                distFactor = 1.0;
                surfaceCat = 6;
            }
            else if ("bic_no".equals(wayTagEval.bicycle))
                distFactor = 4.0;
            else
                distFactor = 3.0;
            surfaceCat = (surfaceCat < 1) ? 2 : surfaceCat;
        } else if ("lcn".equals(wayTagEval.network) || "rcn".equals(wayTagEval.network) || "icn".equals(wayTagEval.network)) {
            distFactor = 1;
            surfaceCat = (surfaceCat < 1) ? 2 : surfaceCat;
        } else if ("service".equals(wayTagEval.highway)) {
            surfaceCat = (surfaceCat < 1) ? 2 : surfaceCat;
            if ("bic_destination".equals(wayTagEval.bicycle))
                distFactor = 1.0;
            else if ( "no".equals(wayTagEval.access) || "bic_no".equals(wayTagEval.bicycle))
                distFactor = 15;
            else
                distFactor = 1.5;
        } else if ("construction".equals(wayTagEval.highway)){
            distFactor = 10;
            surfaceCat = 4;
        } else {
            distFactor = 1.3;
            surfaceCat = (surfaceCat <= 1) ? 2 : surfaceCat;
        }
        if ( surfaceCat == 0 && distFactor < minDistfSc0 )
            throw new RuntimeException("distFactor for surfaceLevel 0 too small");
        return new Factors(distFactor, surfaceCat);
    }

    public static class Factors{
        public final double  distFactor;
        public final short surfaceCat;
        Factors( double distFactor, short surfaceCat){
            this.distFactor = distFactor;
            this.surfaceCat = surfaceCat;
        }

    }
}
