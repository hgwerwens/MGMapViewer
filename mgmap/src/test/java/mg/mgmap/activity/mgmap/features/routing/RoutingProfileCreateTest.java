package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfileMTB;

import mg.mgmap.activity.mgmap.features.routing.profile.CubicSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.IfFunction;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextTreckingBike;
import mg.mgmap.activity.mgmap.features.routing.profile.SurfCat2MTBCat;
import mg.mgmap.generic.util.basic.MGLog;

public class RoutingProfileCreateTest {
    
    private static  SurfCat2MTBCat sc2MTBc = new SurfCat2MTBCat();
    private static class SoftAssert {
        private final List<String> errors = new ArrayList<>();
        public void check(boolean condition, String message) {
            if (!condition) errors.add(message);
        }
        public void assertAll() {
            if (!errors.isEmpty()) {
                throw new AssertionError("Multiple failures:\n" + String.join("\n", errors));
            }
        }
    }

    private final static SoftAssert softAssert = new SoftAssert();


    @Test
    public void ProfileVary() throws Exception {
        for (TestContextMTBInternal.dlstrechFac = 2.2f; TestContextMTBInternal.dlstrechFac <= 2.4f; TestContextMTBInternal.dlstrechFac += 0.1f) {
            for (TestContextMTBInternal.facDnStrech = 8f; TestContextMTBInternal.facDnStrech <= 8.25f; TestContextMTBInternal.facDnStrech += 0.25f) {
                for (TestContextMTBInternal.sf2d = 1.03f; TestContextMTBInternal.sf2d <= 1.05f; TestContextMTBInternal.sf2d += 0.01f) {
                    for (int sDn = 00; sDn <= 400; sDn += 100) {
                        for (int sUp = 0; sUp <= 400; sUp += 100) {
                            for (int mtbDn = 0; mtbDn <= 0; mtbDn += 1) {
                                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbDn);
                                CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(sUp, sDn, false));
                                CubicSpline cubicSplineSc = costCalcSplineProfileMTB.getCostSpline(sc);
                                checkCurvature(cubicSplineSc, new StringBuilder(String.format("%s f2d=%3.2f facDnStrech=%3.2f dlstrechFac=%3.2f", costCalcSplineProfileMTB.getSurfaceCatTxt(sc),
                                        TestContextMTBInternal.sf2d,TestContextMTBInternal.facDnStrech,TestContextMTBInternal.dlstrechFac)));
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void singlescCompare() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sUp = 300;
        int sDn1 = 300;
        int sDn2 = sDn1;
        int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 3, 5);//sc2MTBc.maxSL;
        int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 0, 0);
        SplineProfileTestContextMTB.testProfile = false;
        CostCalcSplineProfileMTB costCalcSplineProfileMTB1 = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(sUp, sDn1,false));
        SplineProfileTestContextMTB.testProfile = false;
        CostCalcSplineProfileMTB costCalcSplineProfileMTB2 = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(sUp, sDn2,false));
        try {
            CubicSpline cubicSplineSc1 = costCalcSplineProfileMTB1.getCostSpline(sc1);
            CubicSpline cubicSplineSc2 = costCalcSplineProfileMTB2.getCostSpline(sc2);
            System.out.println(costCalcSplineProfileMTB1.getSurfaceCatTxt(sc1));
            checkCubicSpline(cubicSplineSc1,new StringBuilder(),-0.4f,0.4f);
            System.out.println(costCalcSplineProfileMTB2.getSurfaceCatTxt(sc2));
            checkCubicSpline(cubicSplineSc2,new StringBuilder(),-0.4f,0.4f);
            compareCubicSpline(cubicSplineSc1,cubicSplineSc2,-0.4f,0.4f,
                    new StringBuilder(costCalcSplineProfileMTB1.getSurfaceCatTxt(sc1)).append(costCalcSplineProfileMTB2.getSurfaceCatTxt(sc2)),true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        softAssert.assertAll();
    }


    @Test
    public void singleProfileTr() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        CostCalcSplineProfileMTB costCalcSplineProfileTr = new CostCalcSplineProfileMTB(new SplineProfileContextTreckingBike(true));// new SplineProfileTestContextMTB(100,100));
        checkSurfaceCats(costCalcSplineProfileTr, new StringBuilder(),-0.4f,0.4f);
        softAssert.assertAll();
    }


    @Test
    public void singleProfileMTB() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sUp = 300;
        int sDn = 300;
        CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(sUp, sDn,true));
        checkProfile(costCalcSplineProfileMTB, new StringBuilder(String.format("sUp=%3d sDn=%3d", sUp, sDn)),-0.6f,0.6f);
        softAssert.assertAll();
    }

    @Test
    public void VaryContext_default()  {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);
        SplineProfileTestContextMTB.testProfile = true;
        for (int sUp = 100; sUp <= 300; sUp = sUp + 100) {
            for (int sDn = 100; sDn <= 300; sDn = sDn + 100) {
                CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(sUp, sDn,true));
                checkProfile(costCalcSplineProfileMTB, new StringBuilder(),0f,0f);
             }
        }
        softAssert.assertAll();
    }

    @Test
    public void VaryContext_wide() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);
        for (int sUp = 0; sUp <= 400; sUp = sUp + 100) {
            for (int sDn = 0; sDn <= 400; sDn = sDn + 100) {
                for (int power = 48 + sUp / 100 * 25; power <= 150f; power = power + 25) {

                    CostCalcSplineProfileMTB costCalcSplineProfileMTB = new CostCalcSplineProfileMTB(new SplineProfileTestContextMTB(power, sUp, sDn,false));
                    StringBuilder msg = new StringBuilder();
                    checkProfile(costCalcSplineProfileMTB, msg,0f, 0f);
                }
            }
        }
        softAssert.assertAll();
    }


    private void checkProfile(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder msg,float slStart, float slStop){
        checkSurfaceCats(costCalcSplineProfileMTB, msg,slStart,slStop);
//        compareSurfaceCats(costCalcSplineProfileMTB, msg);
    }

    private void checkSurfaceCats(CostCalcSplineProfile costCalcSplineProfile, StringBuilder rmsg, float slStart, float slStop){
        CubicSpline cubicSpline = null;
        for (int sc = 0; sc < costCalcSplineProfile.getMaxSurfaceCat(); sc++) {
             try {
                cubicSpline = costCalcSplineProfile.getCostSpline(sc);
                if (cubicSpline != null){
                    System.out.println(costCalcSplineProfile.getSurfaceCatTxt(sc));
                    StringBuilder msg = new StringBuilder(rmsg).append(costCalcSplineProfile.getSurfaceCatTxt(sc));
                    checkCubicSpline(cubicSpline,msg,slStart,slStop);
                }
            } catch (Exception e) {
                softAssert.check(false, rmsg+"failure:"+e.getMessage()+e.getStackTrace());
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        CubicSpline cubicHeuristicSpline = costCalcSplineProfile.getCubicHeuristicSpline();
        StringBuilder msg = new StringBuilder(rmsg).append(" heuristic Spline");
        System.out.println("heuristic Spline");
        checkCubicSpline(cubicSpline,msg,slStart,slStop);

        for (int sc = 0; sc < costCalcSplineProfile.getMaxSurfaceCat(); sc++) {
            try {
                cubicSpline = costCalcSplineProfile.getCostSpline(sc);
                if (sc==0 && costCalcSplineProfile.getMinDistFactSC0()!=1f){
                    cubicSpline = cubicSpline.getFactCubicSpline(costCalcSplineProfile.getMinDistFactSC0());
                }
                if (cubicSpline != null){
                    msg = new StringBuilder(rmsg).append(" compare " +costCalcSplineProfile.getSurfaceCatTxt(sc) +" with Heuristic:");
                    ScCompRes compRes = compareCubicSpline(cubicHeuristicSpline,cubicSpline,-0.4f,0.4f,msg,false);
                    softAssert.check(compRes.crossings.size()==0,compRes.msg().append(" 0 crossings expected").toString());
                }
            } catch (Exception e) {
                softAssert.check(false, rmsg+"failure:"+e.getMessage());
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void checkCubicSpline(CubicSpline cubicSpline,StringBuilder msg, float slStart, float slStop){
        checkCurvature(cubicSpline,msg);
        checkSlope(cubicSpline,msg);
        if (slStop > slStart) {
            StringBuilder f = new StringBuilder("curve Points");
            StringBuilder f1 = new StringBuilder("first Deriva");
            StringBuilder f2 = new StringBuilder("secnd Deriva");
            StringBuilder x = new StringBuilder("x     Points");
            for (float sl = slStart; sl < slStop; sl+= 0.01f) {
                x.append(String.format(Locale.ENGLISH, " %+7.2f", sl * 100));
                f.append(String.format(Locale.ENGLISH, " %+7.2f", cubicSpline.calc(sl)));
                f1.append(String.format(Locale.ENGLISH, " %+7.2f", cubicSpline.calcSlope(sl)));
                f2.append(String.format(Locale.ENGLISH, " %+7.2f", cubicSpline.calcCurve(sl)));
            }
            System.out.println(x);
            System.out.println(f);
            System.out.println(f1);
            System.out.println(f2);
        }
    }


    private void checkCurvature(CubicSpline cubicSpline, StringBuilder msg){
        if (cubicSpline != null) {
            ArrayList<CubicSpline.Value> curveRadiusForNegCurvaturePoint = cubicSpline.getCurveRadiusForNegCurvaturePoints();
            StringBuilder critmsg = new StringBuilder(msg);
            if (curveRadiusForNegCurvaturePoint != null){
                boolean noCritical = true;
                for (CubicSpline.Value negCurvature : curveRadiusForNegCurvaturePoint) {
                    float curvature = -negCurvature.y();
                    if (curvature < CostCalcSplineProfileMTB.minNegCurvatureRadius*10) {
                        noCritical = false;
                        String m = String.format(Locale.ENGLISH, " SLOPE=%.2f VERY CRITICAL CURVE RADIUS=%.2f", 100 * negCurvature.x(), curvature);
                        msg.append(m);
                        critmsg.append(m);
                    }
                    else if (curvature < 10f)
                        msg.append(String.format(Locale.ENGLISH, " Slope=%.2f      Critical Curve Radius=%.2f", 100 * negCurvature.x(), curvature));
                    else
                        msg.append(String.format(Locale.ENGLISH, " slope=%.2f curve radius=%.2f", 100 * negCurvature.x(), curvature));
                }
                System.out.println(msg);
                softAssert.check (noCritical,critmsg.toString());
            }
        }
    }

    private void checkSlope( CubicSpline cubicSpline, StringBuilder msg){
        if (cubicSpline != null) {
            float slope = cubicSpline.calcSlope(0f);
            if (slope <= 0f) {
                System.out.println(msg.append(String.format(Locale.ENGLISH, "Curve slope at 0 is negative slope=%2f",100*slope)));
                softAssert.check (true,msg.toString());
            }
        }
    }

    private void compareSurfaceCats(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder msg){

       for (int sc = 0; sc < sc2MTBc.maxSL - 1; sc++){
            compareTwoSc(costCalcSplineProfileMTB, msg,sc,sc+1,sc>1,0,0f,0.5f);
        }
        for (int mtbDn = 0; mtbDn <= sc2MTBc.maxDn; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp < Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp + 1);
                compareTwoSc(costCalcSplineProfileMTB, msg,sc1,sc2,true,0,0f,0.5f);
            }
        }
        for (int mtbDn = 0; mtbDn < sc2MTBc.maxDn; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn+1, mtbUp);
                compareTwoSc(costCalcSplineProfileMTB, msg,sc1,sc2,true,0,-0.5f,0);
            }
        }

        for (int mtbDn = 0; mtbDn < sc2MTBc.maxDn; mtbDn++) {
            int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, -1);
            int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn+1, -1);
            compareTwoSc(costCalcSplineProfileMTB, msg,sc1,sc2,true,0,-0.5f,0.5f);
        }

/*        for (int sc = 0; sc <= sc2MTBc.maxSL - 1; sc++){
           compareTwoSc(costCalcSplineProfileMTB, msg,sc,6,false,1,0f,0.5f);
        }

       for (int mtbDn = 0; mtbDn <= sc2MTBc.maxDn+1; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                compareTwoSc(costCalcSplineProfileMTB, msg,sc,6,mtbDn+mtbUp<=1,mtbDn+mtbUp>1?1:0,0f,0.5f);
            }
        } */

/*        for (int mtbDn = 2; mtbDn <= 2; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                compareTwoSc(costCalcSplineProfileMTB, msg,sc,6,mtbDn+mtbUp<=1,1,-0.5f,0.0f);
            }
        }


       for (int mtbDn1 = 0; mtbDn1 < CostCalcSplineProfileMTB.maxDn; mtbDn1++) {
            int sc1 = CostCalcSplineProfileMTB.getSurfaceCat(CostCalcSplineProfileMTB.maxSL, mtbDn1, -1);
            for (int mtbDn = 0; mtbDn <= CostCalcSplineProfileMTB.maxDn+1; mtbDn++) {
                for (int mtbUp = mtbDn; mtbUp < Math.min(mtbDn + CostCalcSplineProfileMTB.maxUptoDn,CostCalcSplineProfileMTB.maxUp); mtbUp++) {
                    int sc2 = CostCalcSplineProfileMTB.getSurfaceCat(CostCalcSplineProfileMTB.maxSL, mtbDn, mtbUp);
                    boolean sc1Smallersc2at0slope = mtbDn1 <= mtbUp-2 || mtbDn1+1==mtbDn && mtbDn1+1==mtbUp;
                    int expectedCrossings = sc1Smallersc2at0slope && mtbDn1+2<=mtbUp?0:1;
                    compareTwoSc(costCalcSplineProfileMTB, new StringBuilder(msg).append(sc1Smallersc2at0slope),sc1,sc2,sc1Smallersc2at0slope ,expectedCrossings);
                }
            }
        } */



//        System.out.println(imsg);

    }

    private Point compareTwoSc(CostCalcSplineProfileMTB costCalcSplineProfileMTB, StringBuilder msg, int sc1, int sc2, boolean sc1LTsc2, int numcross,float startSlope, float endSlope){
        StringBuilder imsg = new StringBuilder(msg).append(String.format(Locale.ENGLISH," compare  between %3.0f°|%3.0f° %s with %s",startSlope*100f, endSlope*100f,costCalcSplineProfileMTB.getSurfaceCatTxt(sc1),costCalcSplineProfileMTB.getSurfaceCatTxt(sc2)));
        Point crossingAt = null;
        try {
            float delAt0 = costCalcSplineProfileMTB.getCostSpline(sc2).calc(0f) - costCalcSplineProfileMTB.getCostSpline(sc1).calc(0f);
            softAssert.check(delAt0>=-0.002f&&sc1LTsc2 || delAt0<=0.002f&&!sc1LTsc2 ,new String(new StringBuilder(imsg).append(String.format(Locale.ENGLISH, " wrong costSequence sc=%.3f",delAt0))));
            if ((delAt0<0 && delAt0>=-0.002f&&sc1LTsc2) || (delAt0>0 &&delAt0<=0.002f&&!sc1LTsc2)) numcross = numcross-1;
            ScCompRes scCompRes = compareCubicSpline(costCalcSplineProfileMTB.getCostSpline(sc1),costCalcSplineProfileMTB.getCostSpline(sc2),startSlope,endSlope,imsg,false);
            softAssert.check(scCompRes.crossings.size()==numcross , scCompRes.msg().append(String.format(": %1d crossings expected",numcross)).toString());
            crossingAt = scCompRes.crossings.isEmpty() ? null : scCompRes.crossings.get(0);
        } catch (Exception e) {
            softAssert.check(false, imsg+"failure:"+e.getMessage()+e.getStackTrace());
            System.out.println(imsg+e.getMessage());
            e.printStackTrace();
        }
        return crossingAt;
    }

    record Point(float x, float y0,float y1, float y2){}
    record ScCompRes(ArrayList<Point> crossings, ArrayList<Point> extrema, StringBuilder msg){}

    private ScCompRes compareCubicSpline(CubicSpline cubicSplinelow, CubicSpline cubicSplinehigh, float startSlope, float endSlope, StringBuilder msg, boolean allDetails) throws Exception {
        if (cubicSplinehigh == null || cubicSplinelow == null) throw new Exception(msg+" CubicSpline null");
        IfFunction y0 = x -> cubicSplinehigh.calc(x) - cubicSplinelow.calc(x);
        IfFunction y1 = x -> cubicSplinehigh.calcSlope(x) - cubicSplinelow.calcSlope(x);
        IfFunction y2 = x -> cubicSplinehigh.calcCurve(x) - cubicSplinelow.calcCurve(x);
        ArrayList<Point> crossings = new ArrayList<>();
        ArrayList<Point> extrema = new ArrayList<>();
        float d1 = 0f;
        float d2 ;
        float da1 = 0f;
        float da2 ;
        float step = 0.01f;
        for ( float sl = startSlope; sl <= endSlope; sl += step ){
            d2 = y0.calc(sl);
            da2 = y1.calc(sl);
            if (d2*d1 < 0f  ){
                try {
                    float cross = newton(sl - step / 2f, 1e-5f, 10, y0, y1);
                    if ( cross <= sl && cross > sl-step)
                        crossings.add(new Point(cross,y0.calc(cross),y1.calc(cross),y2.calc(cross)));
                    else
                        System.out.println( msg + "crossing out of range");
                } catch (RuntimeException e){
                    softAssert.check(false, msg+" failure for crossing:"+e.getMessage()+e.getStackTrace());
                    System.out.println(String.format( "%s for slope %3.2f",e.getMessage() , (sl - step/2f)*100f));
                }
            } else if (d2*d1 < 1e-4f && d1!=0f) {
                try {
                    float cross1 = newton(sl - step , 1e-5f, 10, y0, y1);
                    float cross2 = newton(sl, 1e-5f, 10, y0, y1);
                    if ( cross1 != cross2 && cross1 <= sl && cross1 > sl-step && cross2 <= sl && cross2 > sl-step) {
                        crossings.add(new Point(cross1, y0.calc(cross1),y1.calc(cross1),y2.calc(cross1)));
                        crossings.add(new Point(cross1, y0.calc(cross2),y1.calc(cross2),y2.calc(cross2)));
                        System.out.println(msg + " for slope=" + Math.round(sl*100) + ": two crossings added ");
                    }
                } catch (RuntimeException e) {
//                    System.out.println(msg + " for slope=" + Math.round(sl*100) + ":" +e.getMessage() );
                }
            }
            if (da2*da1 < 0f  ){
                try {
                    if (y2.calc(sl-step/2f)!=0f){
                        float extrem = newton(sl - step / 2f, 1e-5f, 10, y1, y2);
                        if ( extrem <= sl && extrem > sl-step)
                            extrema.add(new Point(extrem,y0.calc(extrem),y1.calc(extrem),y2.calc(extrem)));
                        else
                            System.out.println( msg + "extrema out of range");
                    }
                } catch (RuntimeException e){
                    softAssert.check(false, msg+" failure for extrema:"+e.getMessage()+e.getStackTrace());
                    System.out.println(String.format( "%s for slope %3.2f",e.getMessage() , (sl - step/2f)*100f));
                }
            }
            da1 = da2;
            d1 = d2;
        }
        StringBuilder imsg = crossings.isEmpty() ? new StringBuilder(msg) : new StringBuilder(msg).append(" cross at");
        for (Point cross : crossings) {
            imsg.append(String.format(Locale.ENGLISH, " %.2f%s(%.2f)", cross.x * 100,cross.y1 >0?"⇧":"⇩",10*cross.y1));
        }
        if (!extrema.isEmpty() ) imsg.append(" extrema at");
        for (Point ext : extrema) {
            imsg.append(String.format(Locale.ENGLISH, " %.2f%s(%.2f)", ext.x * 100,ext.y2 <0?"⇧":"⇩",10*ext.y0));
        }

//        if (!extrema.isEmpty()||!crossings.isEmpty()) {
            System.out.println(imsg);
//        }
        if (allDetails) {
            StringBuilder fl  = new StringBuilder("curve scLow ");
            StringBuilder fh  = new StringBuilder("curve scHigh");
            StringBuilder f   = new StringBuilder("curve Points");
            StringBuilder f1  = new StringBuilder("first Deriva");
            StringBuilder f2  = new StringBuilder("secnd Deriva");
            StringBuilder x   = new StringBuilder("x     Points");
            for ( float sl = startSlope; sl <= endSlope; sl += step ) {
                fl.append(String.format(Locale.ENGLISH, " %+7.3f",cubicSplinelow.calc(sl)));
                fh.append(String.format(Locale.ENGLISH, " %+7.3f",cubicSplinehigh.calc(sl)));
                f.append(String.format(Locale.ENGLISH, " %+7.3f",y0.calc(sl)));
                f1.append(String.format(Locale.ENGLISH, " %+7.3f",y1.calc(sl)));
                f2.append(String.format(Locale.ENGLISH, " %+7.3f",y2.calc(sl)));
                x.append(String.format(Locale.ENGLISH, " %+7.2f",sl*100));
            }
            System.out.println(x);
            System.out.println(fl);
            System.out.println(fh);
            System.out.println(f);
            System.out.println(f1);
            System.out.println(f2);
            float[] xl = cubicSplinelow.getX();
            float[] yl = cubicSplinelow.getY();
            float[] xh = cubicSplinehigh.getX();
            float[] yh = cubicSplinehigh.getY();
            StringBuilder xbl = new StringBuilder("xbv scLow ");
            StringBuilder ybl = new StringBuilder("ybv scLow ");
            StringBuilder xbh = new StringBuilder("xbv scHigh");
            StringBuilder ybh = new StringBuilder("ybv scHigh");
            for (int i = 0; i < xl.length; i++) {
                xbl.append(String.format(Locale.ENGLISH, " %+8.4f", xl[i] * 100));
                ybl.append(String.format(Locale.ENGLISH, " %+8.4f", yl[i]));
            }
            for (int i = 0; i < xh.length; i++) {
                xbh.append(String.format(Locale.ENGLISH, " %+8.4f", xh[i] * 100));
                ybh.append(String.format(Locale.ENGLISH, " %+8.4f", yh[i]));
            }
            System.out.println(xbl);
            System.out.println(ybl);
            System.out.println(xbh);
            System.out.println(ybh);
        }
        return new ScCompRes(crossings,extrema,imsg);
    }

     public static float newton(float start, float minval, int maxIter, IfFunction f, IfFunction fs) throws RuntimeException{
        float a;
        float na = start;
        float nb;
        float sa;
        float fa;
        float nfa;
        float nfb;
        float sfa;
        int i = 0;
        int j;
        boolean numLimit = false;
        nfa = f.calc(start);
        do {
            i = i+1;
            if ( i >= maxIter)
                throw new RuntimeException("Too many Newton iterations= " + maxIter);
            a = na;
            fa = nfa;
            float fsv = fs.calc(a);
            if ( fsv == 0f)
                throw new RuntimeException("Newton iteration - First derivative is 0");
            na = a - fa / fsv;
            nfa = f.calc(na);
            nfb = fa;
            nb = a;
            j  = 0;

            while ( Math.abs(nfa) >= 0.5f*Math.abs(fa) && Math.abs(nfa) > minval &&!numLimit ) { // fallback to regula falsi
                j = j+1;
                if ( j >= maxIter)
                    throw new RuntimeException("Too many Regula Falsi iterations= " + maxIter);
                sa = ( na * nfb - nb * nfa) / ( nfb -nfa );
                if (sa==na) {
                    System.out.println("Numerical Limit of Newton iteration reached");
                    numLimit = true;
                }
                sfa = f.calc(sa) - minval;
                if (Math.signum(sfa) == Math.signum(nfa)) {
                    na = sa;
                    nfa = sfa;
                } else {
                    nb = na;
                    nfb = nfa;
                    na = sa;
                    nfa = sfa;
                }
            }
        } while ( Math.abs(nfa) > minval &&!numLimit);
        return na;
    }


}

