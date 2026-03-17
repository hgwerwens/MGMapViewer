package mg.mgmap.activity.mgmap.features.routing;

import static mg.mgmap.activity.mgmap.features.routing.SplineProfileTestContextMTB.factory;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;

import mg.mgmap.activity.mgmap.features.routing.profile.IfFunction;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfIsFuncWithDeriv;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.IfSpline;
import mg.mgmap.activity.mgmap.features.routing.profile.ProfileUtil;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextTrekkingBike;
import mg.mgmap.activity.mgmap.features.routing.profile.splinefunc.SplineUtil;
import mg.mgmap.activity.mgmap.features.routing.profile.SurfCat2MTBCat;
import mg.mgmap.activity.mgmap.features.routing.profile.TestContextMTBInternal;
import mg.mgmap.generic.util.basic.MGLog;

public class RoutingProfileCreateTest {
    
    private static  SurfCat2MTBCat sc2MTBc = new SurfCat2MTBCat();
    private static class SoftAssert {
        private final List<String> errors = new ArrayList<>();
        private final List<String> longErrors = new ArrayList<>();
        public void check(boolean condition, String message) {
            if (!condition)
                errors.add(message);
        }

        public void check(boolean condition, String message, String longMessage) {
            if (!condition) {
                errors.add(message);
                if (!longMessage.isEmpty()) longErrors.add(longMessage);
            }
        }

        public void assertAll() {
            if (!errors.isEmpty()) {
                System.out.println( "================================Multiple failures:======================================================================================================\n" +
                                    "================================Multiple failures:======================================================================================================\n" +
                                    String.join("\n", longErrors));
                throw new AssertionError("Multiple failures:\n" + String.join("\n", errors));
            }
        }
    }

    private final static SoftAssert softAssert = new SoftAssert();


    @Test
    public void ProfileVary() {
        for (TestContextMTBInternal.fs[2] = 2.2f; TestContextMTBInternal.fs[2] <= 2.4f; TestContextMTBInternal.fs[2] += 0.1f) {
            for (TestContextMTBInternal.fs[1] = 8f; TestContextMTBInternal.fs[1] <= 8.25f; TestContextMTBInternal.fs[1] += 0.25f) {
                for (TestContextMTBInternal.fs[0] = 1.03f; TestContextMTBInternal.fs[0] <= 1.05f; TestContextMTBInternal.fs[0] += 0.01f) {
                    for (int sDn = 00; sDn <= 400; sDn += 100) {
                        for (int sUp = 0; sUp <= 400; sUp += 100) {
                            for (int mtbDn = 0; mtbDn <= 0; mtbDn += 1) {
                                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbDn);
                                CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn, false));
                                IfSpline cubicSplineSc = costCalcSplineProfile.getCostSpline(sc);
                                checkCurvature(cubicSplineSc, new StringBuilder(String.format("%s f2d=%3.2f facDnStrech=%3.2f dlstrechFac=%3.2f", costCalcSplineProfile.getSurfaceCatTxt(sc),
                                        TestContextMTBInternal.fs[0],TestContextMTBInternal.fs[1],TestContextMTBInternal.fs[2])),false);
                            }
                        }
                    }
                }
            }
        }
    }



    @Test
    public void CompareSC2Heuristic() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sc1 = 3;
        for (int sUp = 100; sUp<=100;sUp+=100)
            for (int sDn = 100; sDn <=100;sDn+=100) {
//                factory = TestContextMTBInternal::new;
                CostCalcSplineProfile costCalcSplineProfile1 = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn, false));
                compareWithHeuristic(costCalcSplineProfile1, sc1,new StringBuilder(""),-0.4f, 0.3f);
            }
        softAssert.assertAll();
    }

    record ProfSc (int sUp, int sDn, int sc) { };
    @Test
    public void SCCompareOldNew() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);

        factory = TestContextMTBInternal::new;
        ArrayList<ProfSc> scs = new ArrayList<>();
//        scs.add(new ProfSc(200, 100,7));//sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 3, 5))
//        scs.add(new ProfSc(200, 200,7));
//        scs.add(new ProfSc(200, 300,7));

//        scs.add(new ProfSc(300, 300,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 2, 2)));
//        scs.add(new ProfSc(300, 300,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, -1)));
//        scs.add(new ProfSc(100, 100,2));
        scs.add(new ProfSc(100, 100,6));
//        scs.add(new ProfSc(200, 200,6));
//        scs.add(new ProfSc(100, 100,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, -1)));
//        scs.add(new ProfSc(200, 200,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, 2)));
//        scs.add(new ProfSc(100, 100,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, 3)));
//        scs.add(new ProfSc(200, 200,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, 3)));
//        scs.add(new ProfSc(200, 200,sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, 2)));
//        scs.add(sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, 1, 2));
        for (ProfSc profSc: scs ) {
            float fromSlope = -0.20f;
            float toSlope = 0.1f;
            factory = SplineProfileContextMTB::new;
            CostCalcSplineProfile costCalcSplineProfile1 = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(148,profSc.sUp, profSc.sDn, false));
            factory = TestContextMTBInternal::new;
            CostCalcSplineProfile costCalcSplineProfile2 = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(148,profSc.sUp, profSc.sDn, false));
            singleScCompare(costCalcSplineProfile1, profSc.sc, costCalcSplineProfile2, profSc.sc, fromSlope,toSlope);
        }
        softAssert.assertAll();
    }

    @Test
    public void SCCompare() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        float fromSlope = -0.10f;
        float toSlope = 0.2f;
        factory = TestContextMTBInternal::new;
        for (int i=0;i<2;i++) {
            CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(100, 100, false));
            singleScCompare(costCalcSplineProfile, sc2MTBc.getSurfaceCat(6, 1, 1), costCalcSplineProfile, 2, fromSlope, toSlope);
            factory = SplineProfileContextMTB::new;
        }
        softAssert.assertAll();
    }

    void singleScCompare(CostCalcSplineProfile costCalcSplineProfile1, int sc1, CostCalcSplineProfile costCalcSplineProfile2, int sc2, float fromSlope, float toSlope){
        try {
            IfSpline cubicSplineSc1 = costCalcSplineProfile1.getCostSpline(sc1);//getCostSpline(sc1);
            IfSpline cubicSplineSc2 = costCalcSplineProfile2.getCostSpline(sc2);
            System.out.println(costCalcSplineProfile1.getSurfaceCatTxt(sc1));
            checkCubicSpline(cubicSplineSc1, new StringBuilder(costCalcSplineProfile1.getSurfaceCatTxt(sc1)), fromSlope, toSlope);
            System.out.println(costCalcSplineProfile2.getSurfaceCatTxt(sc2));
            checkCubicSpline(cubicSplineSc2, new StringBuilder(costCalcSplineProfile2.getSurfaceCatTxt(sc2)), fromSlope, toSlope);
            compareCubicSpline(cubicSplineSc1, cubicSplineSc2, fromSlope, toSlope,
                    new StringBuilder(costCalcSplineProfile1.getSurfaceCatTxt(sc1)).append(costCalcSplineProfile2.getSurfaceCatTxt(sc1)), true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void singleProfCompare() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sUp = 100;
        int sDn = 100;
        CostCalcSplineProfile costCalcSplineProfile1 = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn,false));
        factory = TestContextMTBInternal::new;
        CostCalcSplineProfile costCalcSplineProfile2 = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn,false));
        for (int sc =0; sc < sc2MTBc.maxSurfaceCat;sc++)
        try {
            IfSpline cubicSplineSc1 = costCalcSplineProfile1.getCostSpline(sc);
            IfSpline cubicSplineSc2 = costCalcSplineProfile2.getCostSpline(sc);
            if (cubicSplineSc1!=null&&cubicSplineSc2!=null) {
                System.out.println(costCalcSplineProfile1.getSurfaceCatTxt(sc));
                checkCubicSpline(cubicSplineSc1, new StringBuilder(costCalcSplineProfile1.getSurfaceCatTxt(sc)), -0.4f, 0.4f);
                System.out.println(costCalcSplineProfile2.getSurfaceCatTxt(sc));
                checkCubicSpline(cubicSplineSc2, new StringBuilder(costCalcSplineProfile2.getSurfaceCatTxt(sc)), -0.4f, 0.4f);
                compareCubicSpline(cubicSplineSc1, cubicSplineSc2, -0.4f, 0.4f,
                        new StringBuilder(costCalcSplineProfile1.getSurfaceCatTxt(sc)).append(costCalcSplineProfile2.getSurfaceCatTxt(sc)), true);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        softAssert.assertAll();
    }




    @Test
    public void singleProfileTr() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        factory = TestContextMTBInternal::new;
        CostCalcSplineProfile costCalcSplineProfileTr = new CostCalcSplineProfile(new SplineProfileContextTrekkingBike(true));// new SplineProfileTestContextMTB(100,100));
        checkSurfaceCats(costCalcSplineProfileTr, new StringBuilder(),-0.4f,0.4f);
        softAssert.assertAll();
    }

    @Test
    public void testVelocity() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        double Cr = 0.055;
        double slope = 0.05;
        double watt = 150;
        double df =slope*0.005;
        double v0 = ProfileUtil.getFloatFrictionBasedVelocity(slope-df,watt,Cr);
        double v1 = ProfileUtil.getFloatFrictionBasedVelocity(slope+df,watt,Cr);
        double vm = ProfileUtil.getFloatFrictionBasedVelocity(slope,watt,Cr);
        double dvdsl = (v1-v0)/(2.*df);
        double dvds =  ProfileUtil.getSlopeDerivative(slope,vm,Cr);

        System.out.printf("dvdsl=%f  dvds=%f%n",dvdsl,dvds);
    }

    @Test
    public void singleProfileMTB() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sUp = 100;
        int sDn = 300;
        factory = TestContextMTBInternal::new;
        CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn,  false));
        checkProfile(costCalcSplineProfile, new StringBuilder(String.format("sUp=%3d sDn=%3d", sUp, sDn)),-0.6f,0.6f);
        softAssert.assertAll();
    }

    @Test
    public void singleSCMTB() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        int sUp = 100;
        int sDn = 300;
        int sc = 2;
        factory = TestContextMTBInternal::new;
        CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn,  false));
        IfSpline spline = costCalcSplineProfile.getCostSpline(sc);
        checkCubicSpline(spline,new StringBuilder(String.format("sUp=%3d sDn=%3d", sUp, sDn)),-0.6f,0.6f);
        softAssert.assertAll();
    }


    @Test
    public void VaryContext_default()  {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        MGLog.setUnittest(true);
        factory = TestContextMTBInternal::new;
        for (int sUp = 100; sUp <= 300; sUp = sUp + 100) {
            for (int sDn = 100; sDn <= 300; sDn = sDn + 100) {
                CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile(SplineProfileTestContextMTB.get(sUp, sDn,true));
                checkProfile(costCalcSplineProfile, new StringBuilder(),0f,0f);
             }
        }
        softAssert.assertAll();
    }

    @Test
    public void VaryContext_wide() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);
        factory = TestContextMTBInternal::new;
        for (int sUp = 100; sUp <= 300; sUp = sUp + 100) {
            for (int sDn = 100; sDn <= 300; sDn = sDn + 100) {
                for (int power = 48 ; power <= 150f; power = power + 25) {
                    CostCalcSplineProfile costCalcSplineProfile = new CostCalcSplineProfile( SplineProfileTestContextMTB.get(power, sUp, sDn,false));
                    StringBuilder msg = new StringBuilder();
                    checkProfile(costCalcSplineProfile, msg,0f, 0f);
                }
            }
        }
        softAssert.assertAll();
    }


    private void checkProfile(CostCalcSplineProfile costCalcSplineProfile, StringBuilder msg, float slStart, float slStop){
        checkSurfaceCats(costCalcSplineProfile, msg,slStart,slStop);
//        compareSurfaceCats(costCalcSplineProfile, msg);
    }

    private void checkSurfaceCats(CostCalcSplineProfile costCalcSplineProfile, StringBuilder rmsg, float slStart, float slStop){
        IfSpline cubicSpline = null;
        for (int sc = 0; sc < costCalcSplineProfile.getMaxSurfaceCat(); sc++) {
             try {
                cubicSpline = costCalcSplineProfile.getCostSpline(sc);
                if (cubicSpline != null){
                    System.out.println(costCalcSplineProfile.getSurfaceCatTxt(sc));
                    StringBuilder msg = new StringBuilder(rmsg).append(costCalcSplineProfile.getSurfaceCatTxt(sc));
                    checkCubicSpline(cubicSpline,msg,slStart,slStop);
                }
                costCalcSplineProfile.getDurationFunc(sc);
            } catch (Exception e) {
                softAssert.check(false, rmsg+"failure:"+e.getMessage()+ Arrays.toString(e.getStackTrace()));
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        IfSpline cubicHeuristicSpline = costCalcSplineProfile.getCubicHeuristicSpline();
        if (cubicHeuristicSpline==null) {
            softAssert.check(false, "Heuristic Spline was not created: " + costCalcSplineProfile.getHeuristicHasNegativeCurvatureMsg());
            return;
        }

        StringBuilder msg = new StringBuilder(costCalcSplineProfile.getContext().toString()).append(" heuristic Spline");
        System.out.println(msg);
        checkCubicSpline(cubicHeuristicSpline,msg,slStart,slStop);

        for (int sc = 0; sc < costCalcSplineProfile.getMaxSurfaceCat(); sc++) {
            compareWithHeuristic(costCalcSplineProfile, sc,msg,0f, 0f);
        }
    }

    private void compareWithHeuristic(CostCalcSplineProfile costCalcSplineProfile, int sc,StringBuilder rmsg,float slStart, float slStop){
        IfSpline cubicHeuristicSpline = costCalcSplineProfile.getCubicHeuristicSpline();
        if (cubicHeuristicSpline==null) {
            softAssert.check(false, costCalcSplineProfile.getContext() + " Heuristic Spline was not created");
            return;
        }
        StringBuilder msg = new StringBuilder(rmsg).append(" heuristic Spline");
//        System.out.println("heuristic Spline");
        IfSpline cubicSpline = costCalcSplineProfile.getCostSpline(sc);
        if ( cubicSpline == null ) return;
        try {
            if (sc != costCalcSplineProfile.getContext().getScHeuristicRefSpline()) {
                cubicSpline = costCalcSplineProfile.getCostSpline(sc);
                if ( costCalcSplineProfile.getMinDistFactSC(sc)!=1f){
                    cubicSpline =  cubicSpline.transformY(costCalcSplineProfile.getMinDistFactSC(sc),0f);
                }}
            if (cubicSpline != null ){
                msg = new StringBuilder(rmsg).append(" compare ").append(costCalcSplineProfile.getSurfaceCatTxt(sc)).append(" with Heuristic:");
               ScCompRes compRes = compareCubicSpline( cubicSpline,cubicHeuristicSpline,-0.4f,0.4f,msg,slStop > slStart);
                if (!compRes.crossings.isEmpty()) {
                    float xmax = Float.NEGATIVE_INFINITY;
                    float xmin = Float.POSITIVE_INFINITY;
                    for (Point point : compRes.crossings) {
                        xmin = Math.min(xmin, point.x);
                        xmax = Math.max(xmax, point.x);
                    }
                    compRes.msg().append(" 0 crossings expected").toString();
                    String plot = plotToString(cubicHeuristicSpline::valueAt, cubicSpline::valueAt, null, xmin-0.03f, xmax+0.03f, 200, 80);
                    softAssert.check(compRes.crossings.isEmpty(), compRes.msg().toString(),compRes.msg().append(plot).toString());
                }
            }
        } catch (Exception e) {
            softAssert.check(false, rmsg+"failure:"+e.getMessage());
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkCubicSpline(IfSpline cubicSpline,StringBuilder msg, float slStart, float slStop){
        checkCurvature(cubicSpline,msg,slStart<slStop);
        checkSlope(cubicSpline,msg);
        if (slStart<slStop) {
            System.out.println(SplineUtil.valuesToString(cubicSpline, slStart, slStop, 0.01f));
            System.out.println(SplineUtil.toDetailedString(cubicSpline));
        }
    }


    private void checkCurvature(IfSpline spline, StringBuilder msg,boolean detailed){
        if (spline != null) {
            ArrayList<Float> pointsWithNegativeCurvature = SplineUtil.getPointsWithNegativeCurvature(spline);
            StringBuilder critmsg = new StringBuilder(msg);
            if (!pointsWithNegativeCurvature.isEmpty()){
                boolean noCritical = true;
                String secant = SplineUtil.secantMonotonous(spline);
                for (Float negCurvaturePoint : pointsWithNegativeCurvature) {
                    float curvature = -SplineUtil.curveRadiusAt(negCurvaturePoint,spline);
                    if (curvature < .2f )  { /*CostCalcSplineProfile.minNegCurvatureRadius*10*/
                        noCritical = false;
                        String m = String.format(Locale.ENGLISH, " SLOPE=%.2f VERY CRITICAL CURVE RADIUS=%.2f", 100 * negCurvaturePoint, curvature);
                        msg.append(m).append(secant);
                        critmsg.append(m).append(secant).append(" ").append(SplineUtil.toDetailedString(spline)).append("\n").append(SplineUtil.valuesToString(spline,-0.5f,0.3f,0.01f));
                        if( detailed)
                            critmsg.append(plotToString(spline::valueAt, null, null, negCurvaturePoint - 0.05f, negCurvaturePoint + 0.05f, 200, 100));
                    }
                    else if (curvature < 10f)
                        msg.append(String.format(Locale.ENGLISH, " Slope=%.2f      Critical Curve Radius=%.2f", 100 * negCurvaturePoint, curvature));
                    else
                        msg.append(String.format(Locale.ENGLISH, " slope=%.2f curve radius=%.2f", 100 * negCurvaturePoint, curvature));
                }
                System.out.println(msg);
                System.out.print(secant);
                softAssert.check (noCritical,msg.toString(),critmsg.toString());
            }
        }
    }

    private void checkSlope(IfSpline cubicSpline, StringBuilder msg){
        if (cubicSpline != null) {
            float slope = cubicSpline.derivativeAt(0f);
            if (slope <= 0f) {
                System.out.println(msg.append(String.format(Locale.ENGLISH, "Curve slope at 0 is negative slope=%2f",100*slope)));
                softAssert.check (true,msg.toString());
            }
        }
    }

    private void compareSurfaceCats(CostCalcSplineProfile costCalcSplineProfile, StringBuilder msg){

       for (int sc = 0; sc < sc2MTBc.maxSL - 1; sc++){
            compareTwoSc(costCalcSplineProfile, msg,sc,sc+1,sc>1,0,0f,0.5f);
        }
        for (int mtbDn = 0; mtbDn <= sc2MTBc.maxDn; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp < Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp + 1);
                compareTwoSc(costCalcSplineProfile, msg,sc1,sc2,true,0,0f,0.5f);
            }
        }
        for (int mtbDn = 0; mtbDn < sc2MTBc.maxDn; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn+1, mtbUp);
                compareTwoSc(costCalcSplineProfile, msg,sc1,sc2,true,0,-0.5f,0);
            }
        }

        for (int mtbDn = 0; mtbDn < sc2MTBc.maxDn; mtbDn++) {
            int sc1 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, -1);
            int sc2 = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn+1, -1);
            compareTwoSc(costCalcSplineProfile, msg,sc1,sc2,true,0,-0.5f,0.5f);
        }

/*        for (int sc = 0; sc <= sc2MTBc.maxSL - 1; sc++){
           compareTwoSc(costCalcSplineProfile, msg,sc,6,false,1,0f,0.5f);
        }

       for (int mtbDn = 0; mtbDn <= sc2MTBc.maxDn+1; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                compareTwoSc(costCalcSplineProfile, msg,sc,6,mtbDn+mtbUp<=1,mtbDn+mtbUp>1?1:0,0f,0.5f);
            }
        } */

/*        for (int mtbDn = 2; mtbDn <= 2; mtbDn++) {
            for (int mtbUp = mtbDn; mtbUp <= Math.min(mtbDn + sc2MTBc.maxUptoDn, sc2MTBc.maxUp); mtbUp++) {
                int sc = sc2MTBc.getSurfaceCat(sc2MTBc.maxSL, mtbDn, mtbUp);
                compareTwoSc(costCalcSplineProfile, msg,sc,6,mtbDn+mtbUp<=1,1,-0.5f,0.0f);
            }
        }


       for (int mtbDn1 = 0; mtbDn1 < CostCalcSplineProfile.maxDn; mtbDn1++) {
            int sc1 = CostCalcSplineProfile.getSurfaceCat(CostCalcSplineProfile.maxSL, mtbDn1, -1);
            for (int mtbDn = 0; mtbDn <= CostCalcSplineProfile.maxDn+1; mtbDn++) {
                for (int mtbUp = mtbDn; mtbUp < Math.min(mtbDn + CostCalcSplineProfile.maxUptoDn,CostCalcSplineProfile.maxUp); mtbUp++) {
                    int sc2 = CostCalcSplineProfile.getSurfaceCat(CostCalcSplineProfile.maxSL, mtbDn, mtbUp);
                    boolean sc1Smallersc2at0slope = mtbDn1 <= mtbUp-2 || mtbDn1+1==mtbDn && mtbDn1+1==mtbUp;
                    int expectedCrossings = sc1Smallersc2at0slope && mtbDn1+2<=mtbUp?0:1;
                    compareTwoSc(costCalcSplineProfile, new StringBuilder(msg).append(sc1Smallersc2at0slope),sc1,sc2,sc1Smallersc2at0slope ,expectedCrossings);
                }
            }
        } */



//        System.out.println(imsg);

    }

    private void compareTwoSc(CostCalcSplineProfile costCalcSplineProfile, StringBuilder msg, int sc1, int sc2, boolean sc1LTsc2, int numcross, float startSlope, float endSlope){
        StringBuilder imsg = new StringBuilder(msg).append(String.format(Locale.ENGLISH," compare  between %3.0f°|%3.0f° %s with %s",startSlope*100f, endSlope*100f, costCalcSplineProfile.getSurfaceCatTxt(sc1), costCalcSplineProfile.getSurfaceCatTxt(sc2)));
        try {
            float delAt0 = costCalcSplineProfile.getCostSpline(sc2).valueAt(0f) - costCalcSplineProfile.getCostSpline(sc1).valueAt(0f);
            softAssert.check(delAt0>=-0.002f&&sc1LTsc2 || delAt0<=0.002f&&!sc1LTsc2 ,new String(new StringBuilder(imsg).append(String.format(Locale.ENGLISH, " wrong costSequence sc=%.3f",delAt0))));
            if ((delAt0<0 && delAt0>=-0.002f&&sc1LTsc2) || (delAt0>0 &&delAt0<=0.002f&&!sc1LTsc2)) numcross = numcross-1;
            ScCompRes scCompRes = compareCubicSpline(costCalcSplineProfile.getCostSpline(sc1), costCalcSplineProfile.getCostSpline(sc2),startSlope,endSlope,imsg,true);
            softAssert.check(scCompRes.crossings.size()==numcross , scCompRes.msg().append(String.format(": %1d crossings expected",numcross)).toString());
        } catch (Exception e) {
            softAssert.check(false, imsg+"failure:"+e.getMessage()+e.getStackTrace());
            System.out.println(imsg+e.getMessage());
            e.printStackTrace();
        }
    }

    record Point(float x, float y0,float y1, float y2){}
    record ScCompRes(ArrayList<Point> crossings, ArrayList<Point> extrema, StringBuilder msg){}


    private ScCompRes compareCubicSpline(IfSpline cubicSplinelow, IfSpline cubicSplinehigh, float startSlope, float endSlope, StringBuilder msg, boolean allDetails)  {
        return compareCubicSpline( cubicSplinelow, 1f,  cubicSplinehigh,  startSlope,  endSlope,  msg,  allDetails)  ;
    }

    private ScCompRes compareCubicSpline(IfSpline cubicSplinelow, float f1, IfSpline cubicSplinehigh, float startSlope, float endSlope, StringBuilder msg, boolean allDetails)  {
        if (cubicSplinehigh == null || cubicSplinelow == null) return null;
        IfFunction y0 = x -> f1*cubicSplinehigh.valueAt(x) - cubicSplinelow.valueAt(x);
        IfFunction y1 = x -> f1*cubicSplinehigh.derivativeAt(x) - cubicSplinelow.derivativeAt(x);
        IfFunction y2 = x -> f1*cubicSplinehigh.secondDerivativeAt(x) - cubicSplinelow.secondDerivativeAt(x);

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
                    float cross = ProfileUtil.newton(sl - step / 2f, 1e-5f, 10, y0, y1);
                    if ( cross <= sl && cross > sl-step)
                        crossings.add(new Point(cross,y0.calc(cross),y1.calc(cross),y2.calc(cross)));
                    else
                        System.out.println( msg + "crossing out of range");
                } catch (RuntimeException e){
                    softAssert.check(false, msg+" failure for crossing:"+e.getMessage()+ Arrays.toString(e.getStackTrace()));
                    System.out.printf("%s for slope %3.2f%n",e.getMessage() , (sl - step/2f)*100f);
                }
            } else if (d2*d1 < 1e-4f && d1!=0f) {
                try {
                    float cross1 = ProfileUtil.newton(sl - step , 1e-5f, 10, y0, y1);
                    float cross2 = ProfileUtil.newton(sl, 1e-5f, 10, y0, y1);
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
                        float extrem = ProfileUtil.newton(sl - step / 2f, 1e-5f, 10, y1, y2);
                        if ( extrem <= sl && extrem > sl-step)
                            extrema.add(new Point(extrem,y0.calc(extrem),y1.calc(extrem),y2.calc(extrem)));
//                        else
//                            System.out.println( msg + "extrema out of range");
                    }
                } catch (RuntimeException e){
//                    softAssert.check(false, msg+" failure for extrema:"+e.getMessage()+ Arrays.toString(e.getStackTrace()));
                    System.out.printf("%s failure for extrema %s for slope %3.2f%n",msg,e.getMessage() , (sl - step/2f)*100f);
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
            System.out.println("Delta of " + msg);
            System.out.println(
            SplineUtil.valuesToString( (new IfIsFuncWithDeriv() {
                @Override
                public float valueAt(float x) {             return y0.calc(x);            }
                @Override
                public float derivativeAt(float x) {        return y1.calc(x);            }
                @Override
                public float secondDerivativeAt(float x) {  return y2.calc(x);            }
            }) ,startSlope,endSlope,0.01f));
            System.out.println(plotToString(x->cubicSplinelow.valueAt(x)*f1, cubicSplinehigh::valueAt, null,startSlope,endSlope,250,100));
        }
        return new ScCompRes(crossings,extrema,imsg);
    }



    private static String plotToString(
            IfFunction f1,
            IfFunction f2,
            IfFunction f3,
            float xMin, float xMax,
            int width, int height
    ) {
        IfFunction[] fs = { f1, f2, f3 };
        char[] chars = { '*', '+', 'o' };


        float min = Float.POSITIVE_INFINITY;
        float max = Float.NEGATIVE_INFINITY;

        double step = (xMax-xMin)/width;
        double exp = Math.pow(10d,Math.floor(Math.log10(step)));
        step = step/exp;
        if ( step<=2 )
            step = 1;
        else if (step <=5 ) {
            step = 2;
        }else
            step = 5;
        step = step*exp;
        width = (int) ((xMax - xMin)/step);
        xMin = (float) (exp*Math.floor(xMin/exp));
        float[][] ys = new float[3][width];
        float xx;
        int xAxis = -1;
        float prevxx;
        // sample
        for (int i = 0; i < 3; i++) {
            if (fs[i] == null) continue;
            xx = xMin;
            prevxx = Float.NEGATIVE_INFINITY;
            for (int x = 0; x < width; x++) {
                xx = xx + (float) step;
                float y = fs[i].calc(xx);
                ys[i][x] = y;
                if (xx*prevxx < 0)
                    xAxis = x;
                prevxx = xx;
                if (y < min) min = y;
                if (y > max) max = y;
            }
        }

        if (max == min) {
            max += 1f;
            min -= 1f;
        }

        StringBuilder sb = new StringBuilder(height * (width + 1));

        for (int row = 0; row < height; row++) {
            float yTop = max - (max - min) * row / (height - 1f);
            float yBottom = max - (max - min) * (row + 1f) / (height - 1f);

            for (int col = 0; col < width; col++) {
                char c = ' ';

                // functions
                for (int i = 0; i < 3; i++) {
                    if (fs[i] == null) continue;
                    float y = ys[i][col];
                    if (y <= yTop && y >= yBottom) {
                        c = chars[i];
                        break;
                    }
                }
                // axes
                boolean onXAxis = (0f <= yTop && 0f >= yBottom);
                boolean onYAxis = (col==xAxis);

                if (c == ' ') {
                    if (onXAxis && onYAxis) c = '+';
                    else if (onXAxis) c = '-';
                    else if (onYAxis) c = '|';
                }
                sb.append(c);
            }
            sb.append('\n');
        }
        char[] label = null;
        xx = xMin + 3f*(float)step;
        for (int col = 0; col < width; col++) {
            char c = ' ';
            int mod20 = col%20;
            if (col%20==0) {
                label = String.format(Locale.ENGLISH, "%5.1f", xx *100).toCharArray();
                c = label[0];
            } else if(mod20<label.length)
                c = label[mod20];
            xx = xx + (float) step;
            sb.append(c);
        }

        return sb.toString();
    }
    
    

}

