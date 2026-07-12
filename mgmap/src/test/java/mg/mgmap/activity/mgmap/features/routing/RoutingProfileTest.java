package mg.mgmap.activity.mgmap.features.routing;

import androidx.annotation.NonNull;

import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import mg.mgmap.activity.mgmap.features.routing.profile.CostCalcSplineProfile;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1_2F;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2_2F;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K3S3;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K3S3_2F;
import mg.mgmap.activity.mgmap.features.routing.profile.SplineProfileContextMTB;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImpl;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
//import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.impl2.RoutingSummary;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.TrackLogStatistic;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxExporter;

public class RoutingProfileTest {
    private static MGLog mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

    private enum PointType {start, on_rt, off_rt, stop}

    private record PointDef(PointType pointType, PointModelImpl pointModelImpl) {
        public PointDef(PointDef ref) {
            this(ref.pointType, ref.pointModelImpl);
        }
    }

    private static class Points extends LinkedHashMap<String, PointDef> {
        public void replace(String key, PointType pointType) {
            PointDef point = get(key);
            if (point == null) {
                throw new RuntimeException("Key " + key + " does not exist");
            }
            replace(key, new PointDef(pointType, point.pointModelImpl));
        }

        public void addNewStart(String key, PointDef pointDef) {
            // Neue Map mit dem Eintrag an erster Stelle
            LinkedHashMap<String, PointDef> reordered = new LinkedHashMap<>();
            reordered.put(key, pointDef);
            reordered.putAll(this);
            // Inhalt ersetzen
            clear();
            super.putAll(reordered);
        }

    }

    private record TestDef(String name, RoutingProfile profile, Points points) {

        public TestDef(TestDef ref, RoutingProfile routingProfile) {
            this(ref.name, routingProfile, new Points());
            for (var entry : ref.points.entrySet()) {
                this.points.put(entry.getKey(), entry.getValue() == null ? null : new PointDef(entry.getValue()));
            }
        }

        public TestDef(TestDef ref, String name, RoutingProfile routingProfile) {
            this(name, routingProfile, new Points());
            for (var entry : ref.points.entrySet()) {
                this.points.put(entry.getKey(), entry.getValue() == null ? null : new PointDef(entry.getValue()));
            }
        }

        public TestDef(String name, RoutingProfile routingProfile) {
            this(name, routingProfile, new Points());
        }

        public String getId() {
            return name + profile.getId().substring(2);
        }

        int getCntType(PointType pointType) {
            int cnt = 0;
            for (PointDef point : points.values()) {
                if (point != null && point.pointType == pointType) cnt++;
            }
            return cnt;
        }

        String getPointName(PointType pointType, int pointNumber) {
            int cnt = 1;
            for (Map.Entry<String, PointDef> pointEntry : points.entrySet()) {
                if (pointEntry.getValue() != null && pointEntry.getValue().pointType == pointType && cnt++ == pointNumber)
                    return pointEntry.getKey();
            }
            return "invalid";
        }
    }

    private record TrackResultValues(double cost, TrackLogStatistic trackLogStatistic,
                                     ArrayList<RoutingSummary> routingSummary) {
        public TrackResultValues {
            if (routingSummary != null)
                routingSummary = new ArrayList<>(routingSummary);
            else
                routingSummary = new ArrayList<>();
        }
    }

    private record TrackResultKey(int up, int dn, String upDnClass, String trackType,
                                  String pointName, String testName, String implementingClass, String addParam) {
        @NonNull
        @Override
        public String toString() {
            return String.format("%s_%s_%s_%s_%s_K%1dS%1d", testName, upDnClass, trackType, pointName, implementingClass, up, dn);
        }

        public String getGroupName() {
            return String.format("%s_%s_K%1dS%1d", testName, upDnClass, up, dn);
        }
    }


    private record TrackResult(TrackResultKey trackResultKey, TrackResultValues trackResultValues) {
    }

    private static class TrackResults extends TreeMap<TrackResultKey, TrackResultValues> {
        private static final Comparator<TrackResultKey> FULL_COMPARATOR =
                Comparator.comparingInt(TrackResultKey::up)
                        .thenComparingInt(TrackResultKey::dn)
                        .thenComparing(TrackResultKey::upDnClass)
                        .thenComparing(TrackResultKey::trackType)
                        .thenComparing(TrackResultKey::pointName)
                        .thenComparing(TrackResultKey::testName)
                        .thenComparing(TrackResultKey::implementingClass)
                        .thenComparing(TrackResultKey::addParam);
        private static final Comparator<TrackResultKey> TESTGROUP_COMPARATOR =
                Comparator.comparingInt(TrackResultKey::up)
                        .thenComparingInt(TrackResultKey::dn)
                        .thenComparing(TrackResultKey::upDnClass)
                        .thenComparing(TrackResultKey::trackType)
                        .thenComparing(TrackResultKey::pointName)
                        .thenComparing(TrackResultKey::testName);

        TrackResults() {
            super(FULL_COMPARATOR);
        }

        static boolean isSameGroup(TrackResultKey a, TrackResultKey b) {
            return a != null && b != null && TESTGROUP_COMPARATOR.compare(a, b) == 0;
        }


    }

    private record TrackResultAggrKey(int up, int dn, String upDnClass, String trackType,
                                      String pointName, String implementingClass,String addParam) {
        TrackResultAggrKey(TrackResultKey trackResultKey) {
            this(trackResultKey.up, trackResultKey.dn, trackResultKey.upDnClass, trackResultKey.trackType, trackResultKey.pointName, trackResultKey.implementingClass,trackResultKey.addParam);
        }

        public String groupKeyString() {
            return String.format("K%1dS%1d %s", up, dn, upDnClass);
        }
    }

    private static class TrackResultAggrVal {
        private double totalDuration;

        public void aggr(TrackResultValues trackResultValues) {
            totalDuration = trackResultValues.trackLogStatistic.getDuration() + totalDuration;
        }

        @NonNull
        public String toString() {
            return String.format("total Duration=%9.3f", totalDuration / 1000);
        }
    }

    private static class TrackResultsAggr extends TreeMap<TrackResultAggrKey, TrackResultAggrVal> {
        private static final Comparator<TrackResultAggrKey> FULL_COMPARATOR =
                Comparator.comparingInt(TrackResultAggrKey::up)
                        .thenComparingInt(TrackResultAggrKey::dn)
                        .thenComparing(TrackResultAggrKey::upDnClass)
                        .thenComparing(TrackResultAggrKey::trackType)
                        .thenComparing(TrackResultAggrKey::pointName)
                        .thenComparing(TrackResultAggrKey::implementingClass)
                        .thenComparing(TrackResultAggrKey::addParam);
        private static final Comparator<TrackResultAggrKey> GROUP_COMPARATOR =
                Comparator.comparingInt(TrackResultAggrKey::up)
                        .thenComparingInt(TrackResultAggrKey::dn)
                        .thenComparing(TrackResultAggrKey::upDnClass)
                        .thenComparing(TrackResultAggrKey::trackType)
                        .thenComparing(TrackResultAggrKey::pointName);

        TrackResultsAggr() {
            super(FULL_COMPARATOR);
        }

        static boolean isNotSameGroup(TrackResultAggrKey a, TrackResultAggrKey b) {
            return a == null || b == null || GROUP_COMPARATOR.compare(a, b) != 0;
        }

        public void aggr(TrackResultKey resKey, TrackResultValues resVal) {
            computeIfAbsent(new TrackResultAggrKey(resKey), k -> new TrackResultAggrVal()).aggr(resVal);
        }
    }


    private record TestResult(double costRatio, double lengthRatio) {
    }

    private static class TestResults extends LinkedHashMap<String, TestResult> {
        public TestResults() {
        }

        public TestResults(TestResults ref, Comparator<TestResult> cmp) {
            super(ref.size());

            ref.entrySet().stream()
                    .sorted(Entry.comparingByValue(cmp))
                    .forEach(e -> put(e.getKey(), e.getValue()));
        }


    }

    private record TestResultDetails(TestResult testResult, TrackResult allPointsOnRouteResult,
                                     TestResults offRouteTestResults) {
        public TestResultDetails(TestResult testResult, TrackResult allPointsOnRouteResult) {
            this(testResult, allPointsOnRouteResult, new TestResults());
        }
    }


    private static class Tests extends LinkedHashMap<String, TestDef> {
        public Tests(Tests ref, String filter) {
            for (String key : ref.keySet()) {
                if (filter == null || key.contains(filter))
                    this.put(key, ref.get(key));
            }
        }

        public Tests() {
            super();
        }
    }

    private File getFile() {
        //    File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam_20251201.osm.map"); // !!! map is not uploaded to git (due to map size)
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam_20260301.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());
        return mapFile;
     }

     private GGraphTileFactory getGGraphTileFactory(){
         File mapFile = getFile();
         PointModelUtil.init(32);
         Pref<Boolean> useQubicInterpolation = new Pref<>(true);
         Pref<Boolean> useQubicSplineInterpolation = new Pref<>(true);
         ElevationProvider elevationProvider = new ElevationProviderImpl( new HgtProvider2(), useQubicInterpolation, useQubicSplineInterpolation );
         MapDataStore mds = new MapFile(mapFile, "de");
         WayProvider wayProvider = new WayProviderHelper(mds);
         GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAstarExt"), new Pref<>(true), new Pref<>("12"));
         return gGraphTileFactory;
     }

    @Test
    public void _01_routing()  {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

        String testFilter = null;
        SplineProfileTestContextMTB.factory = SplineProfileContextMTB::new;
        if (testFilter!=null)  MGLog.setUnittest(true);
        GGraphTileFactory gGraphTileFactory = getGGraphTileFactory();

        RoutingProfile mtb_k1s1 =  new MTB_K1S1_2F();
        RoutingProfile mtb_k2s2 =  new MTB_K2S2_2F(); //
        RoutingProfile mtb_k3s3 =  new MTB_K3S3_2F();
        RoutingProfile treckingBike = new TrekkingBike(-1);
        Tests tests = defineTests(mtb_k1s1,mtb_k2s2,mtb_k3s3,treckingBike);
        if (testFilter!=null) tests = new Tests(tests,"nix");
        TrackResults trackResults = new TrackResults();
        for ( Map.Entry<String,TestDef> entry : tests.entrySet() ){
            TestDef testDef = entry.getValue();
            TrackResult trackResult = executeSingle(testDef,PointType.on_rt,-1,gGraphTileFactory);
            trackResults.put(trackResult.trackResultKey,trackResult.trackResultValues);
        }

        mtb_k1s1 =  new MTB_K1S1();
        mtb_k2s2 =  new MTB_K2S2(); //
        mtb_k3s3 =  new MTB_K3S3();
        tests = defineTests(mtb_k1s1,mtb_k2s2,mtb_k3s3,treckingBike);
        if (testFilter!=null) tests = new Tests(tests,"nix");
        for ( Map.Entry<String,TestDef> entry : tests.entrySet() ){
            TestDef testDef = entry.getValue();
            TrackResult trackResult = executeSingle(testDef,PointType.on_rt,-1,gGraphTileFactory);
            trackResults.put(trackResult.trackResultKey,trackResult.trackResultValues);
        }

       mtb_k1s1 =  new xMTBProf(100,100);
       mtb_k2s2 =  new xMTBProf(200,200);
       mtb_k3s3 =  new xMTBProf(300,300);
/*        mtb_k1s1 =  new MTB_K1S1_2F();
        mtb_k2s2 =  new MTB_K2S2_2F(); //
        mtb_k3s3 =  new MTB_K3S3_2F();// */
        tests = defineTests(mtb_k1s1,mtb_k2s2,mtb_k3s3,treckingBike);

        tests = new Tests(tests,testFilter);
        TestResults testResults = new TestResults();
        TestResults offRouteTestResults = new TestResults();

        long t1 = System.nanoTime();
//      Execute Tests
        for ( Map.Entry<String,TestDef> entry : tests.entrySet() ){
            TestDef testDef = entry.getValue();
            TestResultDetails testResult = excuteTest(testDef,gGraphTileFactory);
            trackResults.put(testResult.allPointsOnRouteResult.trackResultKey,testResult.allPointsOnRouteResult.trackResultValues);
            testResults.put(entry.getKey(),testResult.testResult);
            offRouteTestResults.putAll(testResult.offRouteTestResults);
        }
        long t2 = System.nanoTime();

        System.out.printf(Locale.ENGLISH, "Runtime[ms]=%d%n",(t2-t1)/1000000);

        TrackResultKey previouskey = null;
        TrackResultsAggr trackResultsAggr = new TrackResultsAggr();
        double reflength = 0;
        StringBuilder output = new StringBuilder();
        for ( Map.Entry<TrackResultKey,TrackResultValues> entry : trackResults.entrySet() ){
            TrackResultValues trackResultValues = entry.getValue();
            TrackLogStatistic trackLogStatistic = trackResultValues.trackLogStatistic;
            TrackResultKey key = entry.getKey();
            if (!TrackResults.isSameGroup(previouskey,key)) {
                previouskey = key;
                if (output !=null) System.out.println(output);
                output = new StringBuilder(String.format("%-60s Prof_%s_Dur=%9.3f",key.getGroupName(), key.implementingClass, trackLogStatistic.getDuration() /1000f));
                reflength = trackLogStatistic.getTotalLength();
            } else {
                double n2cRatio = trackLogStatistic.getTotalLength() / reflength;
                double logN2RRatio = n2cRatio == 1d ? -16 : Math.log10(Math.abs(n2cRatio - 1));
                output.append(String.format(" Prof_%s_Dur=%9.3f logLengthRatio=%3.0f", key.implementingClass,  trackLogStatistic.getDuration() / 1000f, logN2RRatio));
            }
            trackResultsAggr.aggr(key,trackResultValues);
        }
        System.out.println(output);

        output = null;
        TrackResultAggrKey previousaggrkey = null;
        for ( Map.Entry<TrackResultAggrKey,TrackResultAggrVal> entry : trackResultsAggr.entrySet()){
            TrackResultAggrKey key = entry.getKey();
            TrackResultAggrVal val = entry.getValue();
            if (TrackResultsAggr.isNotSameGroup(previousaggrkey, key)) {
                if (output !=null) System.out.println(output);
                previousaggrkey = key;
                output = new StringBuilder(String.format("%-20s %s %s", key.groupKeyString(), key.implementingClass(),val));
            } else {
                output.append(String.format("    %s  %s", key.implementingClass(),  val));
            }
        }
        System.out.println(output);


        testResults = new TestResults(testResults,Comparator.comparing(TestResult::costRatio));
        offRouteTestResults = new TestResults(offRouteTestResults,Comparator.comparing(TestResult::costRatio));

//      print Results for not intended route to acutal route
        System.out.println("Off route details");
        int cntOff = 0;
        int cntOptRoute = 0;
        double totalOffCostRatio = 0;
        double weigthtedCostRatio = 0;
        for ( Map.Entry<String,TestResult> entry : offRouteTestResults.entrySet() ){
            TestResult offRTR = entry.getValue();
            int logLengthR = offRTR.lengthRatio == 1.0 ? -16 : (int) Math.log10(offRTR.lengthRatio - 1);
            boolean isOptRoute = logLengthR<=-3;
            if (isOptRoute) cntOptRoute ++;
            System.out.printf("Cost Ratio=%1.10f %sOptRoute(%3d) for: %s%n",offRTR.costRatio, (isOptRoute?"is" : "no"), logLengthR , entry.getKey());

            totalOffCostRatio += offRTR.costRatio;
            weigthtedCostRatio += getWeight(offRTR.costRatio);
            cntOff++;
        }

        totalOffCostRatio = totalOffCostRatio/cntOff;


//      print Results for intended route to acutal route
        System.out.println("On route details");
        double totalCostRatio = 0.0;
        double cntOn = 0;
        int cntNotOptRoute = 0;
        for ( Map.Entry<String,TestResult> entry : testResults.entrySet() ){
            TestResult testResult = entry.getValue();
            int logLengthR = testResult.lengthRatio == 1.0 ? -16 : (int) Math.log10(testResult.lengthRatio - 1);
            int logCostR = testResult.costRatio == 1.0 ? -16 : (int) Math.log10(Math.abs(testResult.costRatio - 1));
            boolean isOptRoute = logLengthR<=-4 || logLengthR <=-3 && logCostR <=-4;
            if (!isOptRoute) cntNotOptRoute ++;
            System.out.printf("Cost Ratio=%1.10f %sOptRoute(%3d) for: %s%n",testResult.costRatio, (isOptRoute?"is" : "no"), logLengthR , entry.getKey());
            totalCostRatio += testResult.costRatio;
            weigthtedCostRatio += getWeight(testResult.costRatio);
            cntOn++;
        }
        totalCostRatio = totalCostRatio/cntOn;
        weigthtedCostRatio = weigthtedCostRatio/ (cntOff+cntOff);
        System.out.println("Total Cost Ratio: " + totalCostRatio);

        System.out.printf(Locale.ENGLISH, "Total Off Cost Ratio=%.7f weighted Off Cost Ratio=%.7f %n", totalOffCostRatio,weigthtedCostRatio);
        System.out.printf(Locale.ENGLISH, "onRouteDeviationNumber=%s offRouteMatchNumber=%s%n",cntNotOptRoute,cntOptRoute);

    }

    @Test
    public void _01_DurationVary() {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
        mgLog = new MGLog(MethodHandles.lookup().lookupClass().getName());

        String testFilter = "_MTB_"; //"Rodelweg1Trailkreuzung2Schwabenweg";// "HangkantenwegAbErbsweg2EndeGaisbergtrSteigerweg_MTB_K3S3";// "Leopoldstein2Vulpius";//
        SplineProfileTestContextMTB.factory = SplineProfileContextMTB::new;
        if (!testFilter.equals("_MTB_")) MGLog.setUnittest(true);
        GGraphTileFactory gGraphTileFactory = getGGraphTileFactory();

        RoutingProfile mtb_k1s1 ;
        RoutingProfile mtb_k2s2 ;
        RoutingProfile mtb_k3s3 ;
        TrackResults trackResults = new TrackResults();
        RoutingProfile treckingBike = new TrekkingBike(-1);

        for (int durationCor = -5;durationCor<=20;durationCor+=1) {
            mtb_k1s1 = getDurTestProfile(100,100,durationCor);
            mtb_k2s2 = getDurTestProfile(200,200,durationCor);
            mtb_k3s3 = getDurTestProfile(300,300,durationCor);
            Tests tests = defineTests(mtb_k1s1, mtb_k2s2, mtb_k3s3, treckingBike);
            if (testFilter != null) tests = new Tests(tests, testFilter);
            for (Map.Entry<String, TestDef> entry : tests.entrySet()) {
                TestDef testDef = entry.getValue();
                TrackResult trackResult = executeSingle(testDef, PointType.on_rt, -1, gGraphTileFactory);
                trackResults.put(trackResult.trackResultKey, trackResult.trackResultValues);
            }
        }

        TrackResultsAggr trackResultsAggr = new TrackResultsAggr();

        for ( Map.Entry<TrackResultKey,TrackResultValues> entry : trackResults.entrySet() ){
            TrackResultValues trackResultValues = entry.getValue();
            TrackResultKey key = entry.getKey();
            trackResultsAggr.aggr(key,trackResultValues);
        }
//        System.out.println(output);

        StringBuilder output = null;
        StringBuilder header = new StringBuilder(String.format(Locale.ENGLISH,"%-20s"," "));
        boolean firstline = true;
        TrackResultAggrKey previousaggrkey = null;
        for ( Map.Entry<TrackResultAggrKey,TrackResultAggrVal> entry : trackResultsAggr.entrySet()){
            TrackResultAggrKey key = entry.getKey();
            TrackResultAggrVal val = entry.getValue();
            if (TrackResultsAggr.isNotSameGroup(previousaggrkey, key)) {
                if (firstline && output!=null) {
                    System.out.println(header);
                    firstline = false;
                }
                if (output !=null) System.out.println(output);
                previousaggrkey = key;
                output = new StringBuilder(String.format("%-20s %6.0f", key.groupKeyString(),val.totalDuration/1000f));
            } else {
                output.append(String.format(" %6.0f",  val.totalDuration/1000f));
            }
            if (firstline)
                header.append(key.addParam);
        }
        System.out.println(output);
        RoutingProfileCreateTest.softAssert.assertAllshort();
    }

    private RoutingProfile getDurTestProfile(int sUp, int sDn, int durationCorr){
        float powerDur = getPowerDur(sUp,durationCorr);
        CostCalcSplineProfile costCalculator = new CostCalcSplineProfile( new SplineProfileContextMTB(-1,powerDur,sUp,sDn,true));
        xMTBProf mtb = new xMTBProf(costCalculator);
        mtb.setID(String.format(Locale.ENGLISH,"%s_DP%6.2f",mtb.calcID(sUp,sDn),powerDur));
        RoutingProfileCreateTest routingProfileCreateTest = new RoutingProfileCreateTest();
        routingProfileCreateTest.checkProfile( costCalculator, new StringBuilder(String.format("sUp=%3d sDn=%3d", 200,200)),-0.6f,0.6f);
        return mtb;
    }

    private float getPowerDur(int sUp, int durationCorr){
        float powerDur = SplineProfileContextMTB.getDurPower(sUp);
        powerDur = powerDur * ( 1 + 10*durationCorr/100f);
        return powerDur;
    }

     private RoutingEngine getRoutingEngine(GGraphTileFactory gGraphTileFactory,RoutingProfile profile){
         RoutingContext interactiveRoutingContext = new RoutingContext(
                 1000000,
                 true, // no extra snap, since FSMarker snaps point zoom level dependent
                 10, // accept long detours in interactive mode
                 32);
         RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
         routingEngine.setRoutingProfile(profile);
         return routingEngine;
     }

     private TrackResult executeSingle(TestDef testDef, PointType pointType, int pointNumber, GGraphTileFactory gGraphTileFactory ){
        return executeSingle(testDef, pointType, pointNumber,getRoutingEngine(gGraphTileFactory, testDef.profile));
     }
    private TestResultDetails excuteTest(TestDef testDef, GGraphTileFactory gGraphTileFactory ){
        RoutingEngine routingEngine = getRoutingEngine(gGraphTileFactory, testDef.profile);
        mgLog.d(testDef.getId()+ " on route all points");
        TrackResult ref_cost = executeSingle(testDef,PointType.on_rt,-1,routingEngine);
//        printSlopes(ref_cost.routingSummary);

        mgLog.d("route start/stop");
        TrackResult opt_cost = executeSingle(testDef,PointType.start,0,routingEngine);
//        printSlopes(opt_cost.routingSummary);

        double ref2opt = ref_cost.trackResultValues.cost / opt_cost.trackResultValues.cost;
        double lengthRatio =  ref_cost.trackResultValues.trackLogStatistic.getTotalLength() / opt_cost.trackResultValues.trackLogStatistic.getTotalLength();
        mgLog.d("Ref2OptCosts: " + ref2opt);
        TestResultDetails testResult = new  TestResultDetails(new TestResult(ref2opt,lengthRatio),ref_cost);

/*        int pointNumber = 0;
        int rt_onCnt = testDef.getCntType(PointType.on_rt);
        System.out.println("on route single point");
        while (++pointNumber <= rt_onCnt && rt_onCnt > 1) {
            executeSingle(testDef,PointType.on_rt,pointNumber,routingEngine);
        } */
        int rt_offCnt = testDef.getCntType(PointType.off_rt);
        mgLog.d("off route single point");
        int pointNumber = 0;


        while (++pointNumber <= rt_offCnt ) {
            TrackResult off_cost = executeSingle(testDef,PointType.off_rt,pointNumber,routingEngine);
            double ref2off = ref_cost.trackResultValues.cost / off_cost.trackResultValues.cost;
            lengthRatio = off_cost.trackResultValues.trackLogStatistic.getTotalLength() / opt_cost.trackResultValues.trackLogStatistic.getTotalLength() ;
            testResult.offRouteTestResults.put(testDef.getId() + "_" + testDef.getPointName(PointType.off_rt,pointNumber),new TestResult(ref2off,lengthRatio));
            mgLog.d("Ref2OffCosts: " + ref2off);
        }
        return testResult;
    }

    private TrackResult executeSingle(TestDef testDef, PointType pointType, int pointNumber, RoutingEngine routingEngine) {
        StringBuilder testName = new StringBuilder(testDef.name);
        String trackType = "";
        if (pointType == PointType.start )
            trackType="StartStop";
        else if ( pointType == PointType.on_rt && pointNumber < 0 )
            trackType="AllPointsOnRoute";
        else if ( pointType == PointType.off_rt && pointNumber < 0 )
            trackType="AllPointsOffRoute";
        else if ( pointType == PointType.on_rt && pointNumber > 0 )
            trackType="OnRoute";
        else if ( pointType == PointType.off_rt && pointNumber > 0 )
            trackType="OffRoute";

        long timestamp = 1L;
        WriteableTrackLog mtlb = new WriteableTrackLog("test_mtlb");


        int pointCnt = 0;
        PointDef point;
        String pointName ="";
        for (Map.Entry<String, PointDef> pointEntry : testDef.points.entrySet()) {
            point = pointEntry.getValue();
            if (point != null)
                if (point.pointType == PointType.start) {
                    mtlb.startTrack(timestamp+=10);
                    mtlb.startSegment(timestamp+=10);
                    mtlb.addPoint(point.pointModelImpl);
                } else if (point.pointType == PointType.stop) {
                    mtlb.addPoint(point.pointModelImpl);
                    mtlb.stopSegment(timestamp+=10);
                    mtlb.stopTrack(timestamp+=10);
                } else if (point.pointType == pointType) {
                    pointCnt++;
                    if (pointNumber < 0 || (pointNumber > 0 && pointNumber == pointCnt)) {
                        mtlb.addPoint(point.pointModelImpl);
                        if (pointNumber > 0 )
                            pointName = pointEntry.getKey();
                    }
                }
        }

        testName.append(pointName);
        testName.append(routingEngine.routingProfile.getId());


        routingEngine.refreshRequired.set(0);
        mgLog.d("\r"+testName);
        WriteableTrackLog rotl = routingEngine.updateRouting2(mtlb, null);

        double cost = 0.0;
        for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
            cost = cost + routingSummary.getCost();
        }

        TrackLogStatistic trackStatisic = rotl.getTrackStatistic();

        String upDnClass;
        if ( trackStatisic.getGain()/ trackStatisic.getTotalLength() < 0.03 && trackStatisic.getLoss()/ trackStatisic.getTotalLength() < 0.03 ){
            upDnClass = "--";
        } else
            upDnClass = ( trackStatisic.getGain() > trackStatisic.getLoss() ) ?  "up" :  "dn";

        String profileID = testDef.profile.getId();
        String implemented;
        String    up="0";
        String    dn="0";
        String addParam = "";
        if (profileID.regionMatches(3,"MTB",0,3)){
            implemented = profileID.substring(0,6);
            up          = profileID.substring(8,9);
            dn          = profileID.substring(10,11);
            addParam    = profileID.length() > 13 ? profileID.substring(13) : "";
        } else {
            implemented = profileID;
        }
//        TrackResultValues trackResult = new TrackResultValues(cost, trackStatisic, RoutingSummary.routingSummaries);
        TrackResult trackResult = new TrackResult(
                new TrackResultKey(Integer.parseInt(up),Integer.parseInt(dn),upDnClass,trackType,pointName, testDef.name, implemented,addParam),
                new TrackResultValues(cost, trackStatisic, RoutingSummary.routingSummaries) );

        String testname = trackResult.trackResultKey.toString();

        mgLog.d("\r"+testname + ": " + cost);
        mgLog.d(trackStatisic.toString());
        for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
            mgLog.d(routingSummary);
        }

        RoutingSummary.routingSummaries.clear();

        String fileName = "src/test/assets/temp_local/" + testname  + ".gpx";
        File gpxFile = new File(fileName);
        try {
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return trackResult;
    }

    private void printSlopes( ArrayList<RoutingSummary>  routingSummaries){

        for (RoutingSummary routingSummary : routingSummaries) {
           PointModel p1 = null;
           for (PointModel p2 : routingSummary.getMpm()){
              if ( p1 != null && !p1.equals(p2)) {
                  double dist = PointModelUtil.distance(p1,p2);
                  double vertDist = PointModelUtil.verticalDistance(p1,p2);
                  double slope = dist != 0 ? vertDist/dist * 100d : 1e10;
                  mgLog.d( String.format(Locale.ENGLISH, "dist=%.2f vertDist=%.2f slope=%.2f  ele1=%.2f ele2=%.2f", dist, vertDist, slope, p1.getEle(),p2.getEle()));
              }
              p1 = p2;
           }
        }
    }

    private double getWeight(double x){
            return 1./(1.+Math.exp((1d-x)*10d));
    }


    private Tests defineTests(RoutingProfile mtb_k1s1,RoutingProfile mtb_k2s2, RoutingProfile mtb_k3s3, RoutingProfile treckingBike){
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);

        Points points;
        TestDef testDef;
        Tests tests = new Tests();

        testDef = new TestDef("Bärenbr3Eichen",mtb_k2s2);
        points = testDef.points;
        points.put("Bärenbr", new PointDef( PointType.start, new PointModelImpl(49.373489, 8.747295)));
        points.put("00untKiefernWald", new PointDef( PointType.on_rt, new PointModelImpl(49.372785, 8.741959)));
//        points.put("3Pf11", new PointDef( PointType.on_rt, new PointModelImpl(49.373684, 8.736040)));
        points.put("00obWaldTrail", new PointDef( PointType.on_rt, new PointModelImpl(49.376634, 8.725377)));
        points.put("WeißerSteinschlag1", new PointDef( PointType.off_rt, new PointModelImpl(49.374345, 8.745763)));
//        points.put("WeißerSteinschlag2", new PointDef( PointType.off_rt, new PointModelImpl(49.375169, 8.740072)));
        points.put("3Eichen", new PointDef( PointType.stop,new PointModelImpl(49.379322, 8.723820)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("3Eichen");
        points.put("11to3Eichen", new PointDef( PointType.stop,new PointModelImpl(49.379004, 8.724086)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Bärenbr3Eichen2Pf",mtb_k2s2);
        points = testDef.points;
        points.put("Viehtriebteichweg", new PointDef( PointType.start,new PointModelImpl(49.372187, 8.742910)));
        points.put("kurzerPf", new PointDef( PointType.on_rt, new PointModelImpl(49.372115, 8.742594)));
//        points.put("kurzerPf", new PointDef( PointType.on_rt, new PointModelImpl(49.372153, 8.742763)));
        points.put("Anfang00untKiefernWald", new PointDef( PointType.stop,new PointModelImpl(49.372128, 8.742574)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Bärenbr3Eichen3_11Pf",mtb_k2s2);
        points = testDef.points;
        points.put("WeisserSteinschlagUnten", new PointDef( PointType.start,new PointModelImpl(49.373600, 8.736291)));
        points.put("kurzer11Pf", new PointDef( PointType.on_rt, new PointModelImpl(49.373679, 8.736045)));
        points.put("WeisserSteinschlagOben", new PointDef( PointType.stop,new PointModelImpl(49.373710, 8.735737)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("3Eichen2Leopoldstein",mtb_k2s2);
        points = testDef.points;
        points.put("3Eichen", new PointDef( PointType.start,new PointModelImpl(49.380051, 8.724140)));
        points.put("1Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.384575, 8.722852)));
        points.put("2Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.387458, 8.722325)));
        points.put("3Seg0", new PointDef( PointType.on_rt, new PointModelImpl(49.389915, 8.721471)));
        points.put("Schneiderschere", new PointDef( PointType.off_rt, new PointModelImpl(49.387003, 8.719445)));
        points.put("HöhenWegVorLeo", new PointDef( PointType.off_rt, new PointModelImpl(49.390047, 8.721883)));
        points.put("Leopoldstein", new PointDef( PointType.stop,new PointModelImpl(49.391983, 8.722035)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("3Seg0",PointType.off_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Leopoldstein2Vulpius",mtb_k2s2);
        points = testDef.points;
        points.put("Leopoldstein", new PointDef( PointType.start,new PointModelImpl(49.391983, 8.722035)));
        points.put("HöhenWegNachLeo", new PointDef( PointType.off_rt, new PointModelImpl(49.395111, 8.724165)));
        points.put("4Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.393342, 8.724873)));
        points.put("6Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.398255, 8.726851)));
        points.put("RoteSuhlWeg", new PointDef( PointType.off_rt, new PointModelImpl(49.398232, 8.728652)));
        points.put("AlterHilsbacherWeg", new PointDef( PointType.off_rt, new PointModelImpl(49.399085, 8.727676)));
        points.put("Vulpius", new PointDef( PointType.stop,new PointModelImpl(49.400871, 8.727354)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("6Seg11",PointType.off_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("FalknereiFlowUpDrittesSeg",mtb_k3s3);
        points = testDef.points;
        points.put("SchwabenwegUnterSeg", new PointDef( PointType.start,new PointModelImpl(49.409164, 8.729769)));
        points.put("Rodelweg", new PointDef( PointType.off_rt, new PointModelImpl(49.408569, 8.728617)));
        points.put("Verbindungsweg", new PointDef( PointType.off_rt, new PointModelImpl(49.408763, 8.726771)));
        points.put("Trail2", new PointDef( PointType.on_rt, new PointModelImpl(49.408019, 8.729566)));
        points.put("BlockhauswegÜeberSeg", new PointDef( PointType.stop,new PointModelImpl(49.406211, 8.729933)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Rodelweg",PointType.on_rt);
        points.replace("Trail2",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Rodelweg", PointType.off_rt);
        points.replace("Verbindungsweg", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Riesenstein2Rondell",mtb_k2s2);
        points = testDef.points;
        points.put("AbzeigSchotterInRiesensteinweg", new PointDef( PointType.start,new PointModelImpl(49.407011, 8.701040)));
        points.put("2Seg1rechts", new PointDef( PointType.on_rt, new PointModelImpl(49.406325, 8.699979)));
        points.put("2Seg1geradeaus", new PointDef( PointType.off_rt, new PointModelImpl(49.406741, 8.702646)));
        points.put("KreuzungRiesensteinwegJohannesHoopsWeg", new PointDef( PointType.off_rt, new PointModelImpl(49.406036, 8.706284)));
        points.put("EndeObGaisbergerWegVorRondellHütte", new PointDef( PointType.stop,new PointModelImpl(49.405641, 8.699810)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Sprunghöhe2Leopold",mtb_k1s1);
        points = testDef.points;
        points.put("Sprunghöhe", new PointDef( PointType.start,new PointModelImpl(49.401376, 8.709106)));
        points.put("EndeNikolaustrail", new PointDef( PointType.on_rt, new PointModelImpl(49.381552, 8.721497)));
        points.put("KohlhöferSteige", new PointDef( PointType.off_rt, new PointModelImpl(49.399172, 8.716641)));
        points.put("AnDerRodelBuche", new PointDef( PointType.off_rt, new PointModelImpl(49.395225, 8.716570)));
        points.put("Leopoldstein", new PointDef( PointType.stop,new PointModelImpl(49.391994, 8.722037)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.remove("EndeNikolaustrail");
        points.replace("KohlhöferSteige", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("obSteigerweg2Sprunghöhe",mtb_k1s1);
        points = testDef.points;
        points.put("obSteigerweg", new PointDef( PointType.start,new PointModelImpl(49.396727, 8.698018)));
        points.put("HutzelwaldH", new PointDef( PointType.on_rt, new PointModelImpl(49.400510, 8.698911)));
        points.put("KreuzungJohannesHoops", new PointDef( PointType.off_rt, new PointModelImpl(49.398437, 8.708084)));
        points.put("Sprunghöhe", new PointDef( PointType.stop,new PointModelImpl(49.401311, 8.709110)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.remove("HutzelwaldH");
        points.replace("KreuzungJohannesHoops", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Birkenbank2Gaisberg",mtb_k1s1);
        points = testDef.points;
        points.put("Birkenbank", new PointDef( PointType.start,new PointModelImpl(49.405938, 8.704010)));
        points.put("Sprunghöhe", new PointDef( PointType.on_rt,new PointModelImpl(49.401376, 8.709106)));
        points.put("Ende10Dachswegle", new PointDef( PointType.off_rt, new PointModelImpl(49.403313, 8.700691)));
        points.put("Geschützstellung", new PointDef( PointType.off_rt, new PointModelImpl(49.405235, 8.703792)));
        points.put("Gaisberg", new PointDef( PointType.stop,new PointModelImpl(49.403221, 8.704465)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Sprunghöhe", PointType.off_rt);
        points.replace("Geschützstellung", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("WasserspeicherJohannesHoops2Birkenbank",mtb_k1s1);
        points = testDef.points;
        points.put("Wasserspeicher", new PointDef( PointType.start,new PointModelImpl(49.405633, 8.707340)));
        points.put("1Pf11Kreuz", new PointDef( PointType.off_rt,new PointModelImpl(49.404744, 8.707791)));
        points.put("ueberRondell", new PointDef( PointType.on_rt, new PointModelImpl(49.405658, 8.700322)));
        points.put("Birkenbank", new PointDef( PointType.stop,new PointModelImpl(49.405930, 8.704055)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("1Pf11Kreuz", PointType.on_rt);
        points.replace("ueberRondell", PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Blockhütte2KSPlattform",mtb_k1s1);
        points = testDef.points;
        points.put("Blockhütte", new PointDef( PointType.start,new PointModelImpl(49.401232, 8.713246)));
        points.put("KammerforsterHohle", new PointDef( PointType.off_rt,new PointModelImpl(49.400982, 8.724789)));
        points.put("StrasseRodelbuche", new PointDef( PointType.off_rt,new PointModelImpl(49.395217, 8.716633)));
        points.put("KrHohleKästebaumWegBlockhausweg", new PointDef( PointType.off_rt,new PointModelImpl(49.401832, 8.738319)));
        points.put("Rodelweg", new PointDef( PointType.on_rt, new PointModelImpl(49.405902, 8.729062)));
        points.put("KSPlattform", new PointDef( PointType.stop,new PointModelImpl(49.403918, 8.727342)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("WiesenkreuzWaldhilsbach2Stefanshütte",mtb_k1s1);
        points = testDef.points;
        points.put("Wiesenkreuz", new PointDef( PointType.start,new PointModelImpl(49.377758, 8.755563)));
        points.put("AlterHeuweg", new PointDef( PointType.off_rt,new PointModelImpl(49.380941, 8.749345)));
        points.put("WiesenbacherWeg11", new PointDef( PointType.on_rt, new PointModelImpl(49.381682, 8.750940)));
        points.put("Stefanshütte", new PointDef( PointType.stop,new PointModelImpl(49.381680, 8.746783)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("OdenwälderHütte2KrStephanswZollstockw",mtb_k1s1);
        points = testDef.points;
        points.put("OdenwälderHütte", new PointDef( PointType.start,new PointModelImpl(49.419911, 8.710987)));
        points.put("AusichtsturmwSL2", new PointDef( PointType.on_rt,new PointModelImpl(49.425130, 8.711604)));
        points.put("Zollstockweg1", new PointDef( PointType.off_rt, new PointModelImpl(49.421362, 8.709809)));
        points.put("KrStephanswZollstockw", new PointDef( PointType.stop,new PointModelImpl(49.423198, 8.710118)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("AusichtsturmwSL2", PointType.off_rt);
        points.replace("Zollstockweg1", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("OdenwälderHütte2Schlossblick",mtb_k1s1);
        points = testDef.points;
        points.put("OdenwälderHütte", new PointDef( PointType.start,new PointModelImpl(49.419911, 8.710987)));
        points.put("OdenwälderWegvorKührund", new PointDef( PointType.on_rt,new PointModelImpl(49.425223, 8.711807)));
        points.put("OdenwälderWegNachKührund", new PointDef( PointType.on_rt,new PointModelImpl(49.426526, 8.713099)));
        points.put("OdenwälderWegSerpentine", new PointDef( PointType.off_rt, new PointModelImpl(49.426290, 8.717622)));
        points.put("Heiligenbergweg", new PointDef( PointType.off_rt, new PointModelImpl(49.427223, 8.713200)));
        points.put("ZollstockwegUnten1", new PointDef( PointType.off_rt, new PointModelImpl(49.421362, 8.709809)));
        points.put("ZollstockwegOben11", new PointDef( PointType.off_rt, new PointModelImpl(49.427568, 8.712532)));
        points.put("SchlossblickHütte", new PointDef( PointType.stop,new PointModelImpl(49.429134, 8.715338)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("OdenwälderWegvorKührund",  PointType.off_rt);
        points.replace("OdenwälderWegNachKührund", PointType.off_rt);
        points.replace("ZollstockwegUnten1", PointType.on_rt );
        points.replace("ZollstockwegOben11",  PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("BoxbergForstQuelle2MiErlensumpfweg",mtb_k1s1);
        points = testDef.points;
        points.put("startObererNeuerWeg", new PointDef( PointType.start,new PointModelImpl(49.381263, 8.706486)));
        points.put("ObererNeuerWeg", new PointDef( PointType.on_rt,new PointModelImpl(49.380912, 8.706909)));
        points.put("Rebmannspfad", new PointDef( PointType.off_rt, new PointModelImpl(49.381202, 8.708179)));
        points.put("MiErlensumpfweg", new PointDef( PointType.stop,new PointModelImpl(49.380628, 8.707274)));//49.380649, 8.707409))); Point changed from crossing to before crossing, so that opt route!
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("KühlerGrundBrückeRohrbach2PfadAmHangKurveHoch12",mtb_k1s1);
        points = testDef.points;
        points.put("AbzweigBrücke", new PointDef( PointType.start,new PointModelImpl(49.379375, 8.700524)));
        points.put("Talweg01", new PointDef( PointType.off_rt,new PointModelImpl(49.382093, 8.701988)));
        points.put("PfadAmHangNachBrücke", new PointDef( PointType.on_rt, new PointModelImpl(49.381549, 8.702624)));
        points.put("PfadAmHangKurveHoch12", new PointDef( PointType.stop,new PointModelImpl(49.383134, 8.702513)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("KurveUntSanktNikolausWeg2LeopoldSt",mtb_k2s2);
        points = testDef.points;
        points.put("KurveUntSnktNikolaus", new PointDef( PointType.start,new PointModelImpl(49.390841, 8.712170)));
        points.put("SchneiderScheere12", new PointDef( PointType.on_rt,new PointModelImpl(49.390408, 8.714062)));
        points.put("SchneiderScheere23", new PointDef( PointType.on_rt, new PointModelImpl(49.386050, 8.718701)));
        points.put("NikolausTrailVorn00", new PointDef( PointType.off_rt,new PointModelImpl(49.392596, 8.714945)));
        points.put("NikolausTrailEnde00", new PointDef( PointType.off_rt, new PointModelImpl(49.381489, 8.721578)));
        points.put("StraßeSnktNik", new PointDef( PointType.off_rt, new PointModelImpl(49.389170, 8.713084)));
        points.put("LeopoldSt", new PointDef( PointType.stop,new PointModelImpl(49.391994, 8.722037)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("SchneiderScheere12", PointType.off_rt);
        points.replace("SchneiderScheere23", PointType.off_rt);
        points.replace("NikolausTrailVorn00", PointType.on_rt);
        points.replace("NikolausTrailEnde00", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Blockhütte2KSFalknerei",mtb_k1s1);
        points = testDef.points;
        points.put("Blockhütte", new PointDef( PointType.start,new PointModelImpl(49.401235, 8.713260)));
        points.put("AbzweigRodelweg", new PointDef( PointType.on_rt,new PointModelImpl(49.407584, 8.728162)));
    //    points.put("KönigsforsterHohleP1",new PointDef( PointType.off_rt,new PointModelImpl(49.400524, 8.723204)));
        points.put("AbzweigKSweg", new PointDef( PointType.off_rt, new PointModelImpl(49.401889, 8.724593)));
        points.put("KönigsforsterHohleP2",new PointDef( PointType.off_rt,new PointModelImpl(49.401448, 8.725744)));
        points.put("VulpiusHütte", new PointDef( PointType.off_rt,new PointModelImpl(49.400834, 8.727354)));
        points.put("Falknerei", new PointDef( PointType.stop,new PointModelImpl(49.403808, 8.728194)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("AbzweigRodelweg", PointType.off_rt);
        points.replace("AbzweigKSweg", PointType.on_rt);
//        points.replace("VulpiusHütte", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("VulpiusHütte", PointType.off_rt);
        points.replace("AbzweigKSweg", PointType.off_rt);
    //    points.replace("KönigsforsterHohleP1", PointType.on_rt);
        points.replace("KönigsforsterHohleP2", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

/*        testDef = new TestDef("Kriegsweg2KreuzungKSwegKönigsforsterHohle",mtb_k3s3);
        points = testDef.points;
        points.put("AbzweigZurKönigsforsterHohleVomKriegsweg", new PointDef( PointType.start,new PointModelImpl(49.400603, 8.722113)));
        points.put("AbzweigKSweg", new PointDef( PointType.off_rt, new PointModelImpl(49.401889, 8.724593)));
        points.put("KönigsforsterHohleP1",new PointDef( PointType.on_rt,new PointModelImpl(49.400524, 8.723204)));
        points.put("KreuzungKSwegKönigsforsterHohle", new PointDef( PointType.stop,new PointModelImpl(49.400849, 8.724528)));
        tests.put(testDef.getId(),testDef); */


        testDef = new TestDef("Hochstr2HeiligenBergKlost",mtb_k1s1);
        points = testDef.points;
        points.put("EndeHochstr", new PointDef( PointType.start,new PointModelImpl(49.427174, 8.710157)));
        points.put("linkerTrail11", new PointDef( PointType.off_rt,new PointModelImpl(49.425702, 8.708751)));
        points.put("ObererBitterbrunnenweg", new PointDef( PointType.on_rt, new PointModelImpl(49.427452, 8.707226)));
        points.put("AbzweigTrack2Kloster", new PointDef( PointType.stop,new PointModelImpl(49.424704, 8.705870)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("linkerTrail11", PointType.on_rt);
        points.replace("ObererBitterbrunnenweg", PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Steigerweg2SteigerwegAmPfadvorbei",mtb_k1s1);
        points = testDef.points;
        points.put("SteigerwegVorTreppe", new PointDef( PointType.start,new PointModelImpl(49.396804, 8.695270)));
        points.put("Pfad23", new PointDef( PointType.off_rt,new PointModelImpl(49.396506, 8.697440)));
        points.put("SteigerwegSerpentine", new PointDef( PointType.on_rt, new PointModelImpl(49.396273, 8.698883)));
        points.put("SteigerwegNachPfad", new PointDef( PointType.stop,new PointModelImpl(49.396267, 8.697167)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("SteigerwegSerpentine",PointType.off_rt);
        points.replace("Pfad23",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("SteubenStr2TheodorHeussBruecke",mtb_k1s1);
        points = testDef.points;
        points.put("SteubenStr", new PointDef( PointType.start,new PointModelImpl(49.421564, 8.689055)));
        points.put("HandschuhsheimerLandStrHaltestelle", new PointDef( PointType.on_rt,new PointModelImpl(49.418641, 8.690552)));
        points.put("Brückenstr", new PointDef( PointType.on_rt, new PointModelImpl(49.415520, 8.692009)));
        points.put("BergStrEckeKussmaul", new PointDef( PointType.off_rt, new PointModelImpl(49.418668, 8.692200)));
        points.put("BergStrEckeSchroeder", new PointDef( PointType.off_rt, new PointModelImpl(49.415945, 8.693222)));
        points.put("AuffahrtTheodorHeuss", new PointDef( PointType.stop,new PointModelImpl(49.413683, 8.692206)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,treckingBike);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("UnterHampen2DreiEichen",mtb_k1s1);
        points = testDef.points;
        points.put("AlterSteinbruchSchlagweg", new PointDef( PointType.start,new PointModelImpl(49.379343, 8.716802)));
        points.put("SpitzkehreGossenbrunnenweg", new PointDef( PointType.on_rt,new PointModelImpl(49.376556, 8.714878)));
        points.put("GossenbrunnenwegVorGabeleichweg", new PointDef( PointType.on_rt, new PointModelImpl(49.378664, 8.718863)));
        points.put("HangwegTeil1", new PointDef( PointType.off_rt, new PointModelImpl(49.379901, 8.718148)));
        points.put("HangwegTeil2", new PointDef( PointType.off_rt, new PointModelImpl(49.378814, 8.717741)));
        points.put("GabeleichwegSpitzkehre", new PointDef( PointType.off_rt,new PointModelImpl(49.382216, 8.717772)));
    //    points.put("DreiEichenPfadSeg1_31", new PointDef( PointType.off_rt,new PointModelImpl(49.379447, 8.717306)));
    //    points.put("RechtsVonDreiEichenPfadSeg1_31", new PointDef( PointType.off_rt,new PointModelImpl(49.379323, 8.717168)));
        points.put("DreiEichenPfadSeg1_21", new PointDef( PointType.off_rt,new PointModelImpl(49.379401, 8.717521)));
        points.put("DreiEichenPfadSeg2_21", new PointDef( PointType.off_rt,new PointModelImpl(49.379824, 8.719031)));
        points.put("DreiEichenPfadSeg3_21", new PointDef( PointType.off_rt,new PointModelImpl(49.380187, 8.720865)));
        points.put("DreiEichen", new PointDef( PointType.stop,new PointModelImpl(49.379856, 8.722989)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("SpitzkehreGossenbrunnenweg", PointType.off_rt);
        points.replace("GossenbrunnenwegVorGabeleichweg", PointType.off_rt);
        points.replace("GabeleichwegSpitzkehre", PointType.off_rt);
        points.replace("DreiEichenPfadSeg2_21", PointType.on_rt);
        points.replace("DreiEichenPfadSeg3_21", PointType.on_rt);
        points.replace("HangwegTeil1", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("DreiEichenPfadSeg1_21", PointType.on_rt);
        points.replace("HangwegTeil1", PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("UnterHampen2DreiEichenPfadSeg1_21",mtb_k3s3);
        points = testDef.points;
        points.put("AlterSteinbruchSchlagweg", new PointDef( PointType.start,new PointModelImpl(49.379343, 8.716802)));
        points.put("DreiEichenPfadSeg1_31", new PointDef( PointType.on_rt,new PointModelImpl(49.379439, 8.717213)));
        points.put("RechtsVonDreiEichenPfadSeg1_31", new PointDef( PointType.off_rt,new PointModelImpl(49.379323, 8.717168)));
        points.put("DreiEichenPfadSeg1_21", new PointDef( PointType.stop,new PointModelImpl(49.379392, 8.717485)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        // Gelöscht, weil Artefakt in Höhendaten, Steigung am Anfang des RechtsVonDreiEichenPfadSeg1_31 nicht korrekt wieder gegeben und damit sUp=3 schwerer als sc=6 für K1
        /*testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef); */



        testDef = new TestDef("DarkStoneEtappe0Up",mtb_k1s1);
        points = testDef.points;
        points.put("Pf23zumRodelweg", new PointDef( PointType.start,new PointModelImpl(49.403979, 8.731975)));
        points.put("Darkstone", new PointDef( PointType.off_rt,new PointModelImpl(49.403337, 8.732288)));
        points.put("RodelwegKreuzung", new PointDef( PointType.on_rt, new PointModelImpl(49.403949, 8.730729)));
        points.put("Rodelweg", new PointDef( PointType.off_rt, new PointModelImpl(49.404065, 8.728970)));
        points.put("HohlerKästebaumweg", new PointDef( PointType.stop,new PointModelImpl(49.402884, 8.731847)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Friesenweg2Molkenkur",mtb_k2s2);
        points = testDef.points;
        points.put("Friesenweg1Pf1", new PointDef( PointType.start,new PointModelImpl(49.408172, 8.712609)));
        points.put("Pf1letzteKurve", new PointDef( PointType.on_rt,new PointModelImpl(49.407334, 8.712434)));
        points.put("Pf1UmSchanzenhügel", new PointDef( PointType.off_rt, new PointModelImpl(49.407501, 8.711650)));
        points.put("KurveMolkenkurweg", new PointDef( PointType.off_rt, new PointModelImpl(49.408661, 8.720893)));
        points.put("zwischenAbzweigungenAussicht", new PointDef( PointType.on_rt,new PointModelImpl(49.406867, 8.712723)));
        points.put("AbzweigAufStrasse", new PointDef( PointType.stop,new PointModelImpl(49.406560, 8.713886)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Pf1letzteKurve",PointType.off_rt);
        points.replace("Pf1UmSchanzenhügel",PointType.on_rt);
        points.replace("zwischenAbzweigungenAussicht",PointType.stop);
        points.remove("AbzweigAufStrasse");
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("HirschgasseViaEngelswiesenweg2Philoweg",mtb_k2s2);
        points = testDef.points;
        points.put("Hirschgasse", new PointDef( PointType.start,new PointModelImpl(49.418887, 8.713536)));
        points.put("Engelswiesenweg12Part1", new PointDef( PointType.on_rt,new PointModelImpl(49.420697, 8.713906)));
        points.put("HirschgasseOben", new PointDef( PointType.off_rt,new PointModelImpl(49.422982, 8.713097)));
        points.put("PhiloswegAmEndeDesPart1", new PointDef( PointType.stop,new PointModelImpl(49.422069, 8.714820)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Engelswiesenweg12Part1",PointType.off_rt);
        points.replace("HirschgasseOben",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("PhilowegViaEngelswiesenweg2Kühruhe",mtb_k2s2);
        points = testDef.points;
        points.put("PhiloswegAmEndeDesPart1", new PointDef( PointType.start,new PointModelImpl(49.422069, 8.714820)));
        points.put("Engelswiesenweg12Part2", new PointDef( PointType.on_rt, new PointModelImpl(49.423487, 8.714997)));
        points.put("Engelswiesenweg22Part3", new PointDef( PointType.on_rt, new PointModelImpl(49.422533, 8.717203)));
        points.put("Webersbrunnenweg1", new PointDef( PointType.off_rt,new PointModelImpl(49.421337, 8.718462)));
        points.put("Moltkehütte", new PointDef( PointType.off_rt,new PointModelImpl(49.421518, 8.721593)));
        points.put("KühruheKreuzung", new PointDef( PointType.stop,new PointModelImpl(49.422300, 8.718283)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Engelswiesenweg12Part2",PointType.off_rt);
        points.replace("Engelswiesenweg22Part3",PointType.off_rt);
        points.replace("Moltkehütte",PointType.on_rt);
        tests.put(testDef.getId(),testDef);



/*        testDef = new TestDef("gelbesXabGrenzweg2Weissenstein",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungVorEinstiegBeiGrenzweg", new PointDef( PointType.start,new PointModelImpl(49.466739, 8.700422)));
        points.put("Part1_12mitKurvencombi", new PointDef( PointType.on_rt, new PointModelImpl(49.465865, 8.703302)));
        points.put("VogelherdwegParallelzuPart1_12mitKurvencombi", new PointDef( PointType.off_rt,new PointModelImpl(49.466065, 8.704453)));
        points.put("Part2_11flachDurchjungenWald", new PointDef( PointType.on_rt, new PointModelImpl(49.462043, 8.707908)));
        points.put("VogelherdwegParallelzuPart2_11flachDurchjungenWald", new PointDef( PointType.off_rt,new PointModelImpl(49.462044, 8.708954)));
        points.put("Part3_12_1esHohlwegseg", new PointDef( PointType.on_rt,new PointModelImpl(49.459859, 8.710467)));
        points.put("VogelherdwegParallelzuPart3_12_1esHohlwegseg", new PointDef( PointType.off_rt,new PointModelImpl(49.459673, 8.710566)));
        points.put("Part4_12_2esHohlwegseg", new PointDef( PointType.on_rt,new PointModelImpl(49.457619, 8.714009)));
        points.put("KreuzungZurStrasse(Hauptweg)", new PointDef( PointType.off_rt,new PointModelImpl(49.455989, 8.714593)));
        points.put("AufstiegSteilerNeuerTrail_2WeissenSt_12", new PointDef( PointType.on_rt,new PointModelImpl(49.453936, 8.718253)));
        points.put("ObererDarmutswegPart1", new PointDef( PointType.off_rt,new PointModelImpl(49.454010, 8.718530)));
        points.put("ObererDarmutswegPart2", new PointDef( PointType.on_rt,new PointModelImpl(49.453802, 8.721095)));
        points.put("EinfacherGeraderTrailzuWeissenSt_01", new PointDef( PointType.off_rt,new PointModelImpl(49.453426, 8.718033)));
        points.put("WeisserSteinTurmBank", new PointDef( PointType.stop,new PointModelImpl(49.453064, 8.723055)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Part4_12_2esHohlwegseg",PointType.off_rt);
        points.replace("KreuzungZurStrasse(Hauptweg)",PointType.on_rt);
        points.replace("Part4_12_2esHohlwegseg",PointType.off_rt);
        points.replace("AufstiegSteilerNeuerTrail_2WeissenSt_12",PointType.off_rt);
        points.replace("ObererDarmutswegPart2",PointType.off_rt);
        points.replace("EinfacherGeraderTrailzuWeissenSt_01",PointType.on_rt);
        tests.put(testDef.getId(),testDef); */




        testDef = new TestDef("gelbesXabGrenzweg2WeissensteinPart1",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungVorEinstiegBeiGrenzweg", new PointDef( PointType.start,new PointModelImpl(49.466739, 8.700422)));
        points.put("Part1_12mitKurvencombi", new PointDef( PointType.on_rt, new PointModelImpl(49.465865, 8.703302)));
        points.put("VogelherdwegParallelzuPart1_12mitKurvencombi", new PointDef( PointType.off_rt,new PointModelImpl(49.466065, 8.704453)));
        points.put("Part2_11flachDurchjungenWald", new PointDef( PointType.on_rt, new PointModelImpl(49.462043, 8.707908)));
        points.put("VogelherdwegParallelzuPart2_11flachDurchjungenWald", new PointDef( PointType.off_rt,new PointModelImpl(49.462044, 8.708954)));
        points.put("Part3_12_1esHohlwegseg", new PointDef( PointType.on_rt,new PointModelImpl(49.459859, 8.710467)));
        points.put("VogelherdwegParallelzuPart3_12_1esHohlwegseg", new PointDef( PointType.off_rt,new PointModelImpl(49.459673, 8.710566)));
        points.put(";BerührungVogelherwegMitTrail", new PointDef( PointType.stop,new PointModelImpl(49.458988, 8.712053)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("gelbesXabGrenzweg2WeissensteinPart2",mtb_k3s3);
        points = testDef.points;
        points.put(";BerührungVogelherwegMitTrail", new PointDef( PointType.start,new PointModelImpl(49.458988, 8.712053)));
        points.put("Part4_12_2esHohlwegseg", new PointDef( PointType.on_rt,new PointModelImpl(49.457619, 8.714009)));
        points.put("KreuzungZurStrasse(Hauptweg)", new PointDef( PointType.off_rt,new PointModelImpl(49.455989, 8.714594)));
        points.put("AufstiegSteilerNeuerTrail_2WeissenSt_12", new PointDef( PointType.on_rt,new PointModelImpl(49.453936, 8.718253)));
        points.put("ObererDarmutswegPart1", new PointDef( PointType.off_rt,new PointModelImpl(49.454010, 8.718530)));
        points.put("ObererDarmutswegPart2", new PointDef( PointType.on_rt,new PointModelImpl(49.453802, 8.721095)));
        points.put("EinfacherGeraderTrailzuWeissenSt_01", new PointDef( PointType.off_rt,new PointModelImpl(49.453426, 8.718033)));
        points.put("WeisserSteinTurmBank", new PointDef( PointType.stop,new PointModelImpl(49.453064, 8.723055)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.remove("WeisserSteinTurmBank");
        points.put("WeisserSteinAmTrailEingangWeißerSteinTrail", new PointDef( PointType.stop,new PointModelImpl(49.453233, 8.722874)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Part4_12_2esHohlwegseg",PointType.off_rt);
        points.replace("KreuzungZurStrasse(Hauptweg)",PointType.on_rt);
        points.replace("Part4_12_2esHohlwegseg",PointType.off_rt);
        points.replace("AufstiegSteilerNeuerTrail_2WeissenSt_12",PointType.off_rt);
        points.replace("ObererDarmutswegPart2",PointType.off_rt);
        points.replace("EinfacherGeraderTrailzuWeissenSt_01",PointType.on_rt);
        tests.put(testDef.getId(),testDef);



        /*testDef = new TestDef("MiniTestMitDreiPunkten",mtb_k2s2);
        points = testDef.points;
        points.put("P1", new PointDef( PointType.start,new PointModelImpl(49.379431, 8.717175)));
        points.put("Approach", new PointDef( PointType.on_rt,new PointModelImpl(49.379439, 8.717211)));
        points.put("P2", new PointDef( PointType.stop,new PointModelImpl(49.379439, 8.717213)));
        tests.put(testDef.getId(),testDef);*/

        // downhill tests
        testDef = new TestDef("Falknerei2Rodelweg1Trailkreuzung",mtb_k2s2);
        points = testDef.points;
        points.put("FalknereiEingangTrail", new PointDef( PointType.start,new PointModelImpl(49.404017, 8.728643)));
        points.put("RodelwegKurve", new PointDef( PointType.off_rt,new PointModelImpl(49.403965, 8.730738)));
        points.put("MittenImTrail", new PointDef( PointType.on_rt,new PointModelImpl(49.404543, 8.728682)));
        points.put("KreuzgungRodelwegTrail", new PointDef( PointType.stop,new PointModelImpl(49.404893, 8.729666)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("MittenImTrail",PointType.off_rt);
        points.replace("RodelwegKurve",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Rodelweg1Trailkreuzung2Schwabenweg",mtb_k2s2);
        points = testDef.points;
        points.put("EingangTrail15", new PointDef( PointType.start,new PointModelImpl(49.404893, 8.729666)));
        points.put("KreuzungBlockhausweg", new PointDef( PointType.on_rt,new PointModelImpl(49.406216, 8.729923)));
        points.put("KreuzungBlockhauswegRodelweg", new PointDef( PointType.off_rt,new PointModelImpl(49.407562, 8.728199)));
        points.put("RodelwegUntererTeil", new PointDef( PointType.off_rt,new PointModelImpl(49.408048, 8.728302)));
        points.put("HakaKakaAbBlockhausweg",new PointDef( PointType.off_rt,new PointModelImpl(49.408470, 8.727858)) );
        points.put("Trail25eingangBismarkhöhenweg", new PointDef( PointType.stop,new PointModelImpl(49.409790, 8.728947)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("KreuzungBlockhausweg",PointType.off_rt);
        points.replace("KreuzungBlockhauswegRodelweg",PointType.on_rt);
        points.replace("HakaKakaAbBlockhausweg",PointType.on_rt);
        points.replace("RodelwegUntererTeil",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Falknerei2Bismarkhöhenweg",mtb_k3s3);
        points = testDef.points;
        points.put("FalknereiEingangTrail", new PointDef( PointType.start,new PointModelImpl(49.404017, 8.728643)));
        points.put("RodelwegKurve", new PointDef( PointType.off_rt,new PointModelImpl(49.403965, 8.730738)));
        points.put("KreuzgungRodelwegTrail", new PointDef( PointType.off_rt,new PointModelImpl(49.404893, 8.729666)));
        points.put("KreuzungBlockhauswegRechterTrail", new PointDef( PointType.off_rt,new PointModelImpl(49.406216, 8.729923)));
        points.put("LinkerTrail2Seg1", new PointDef( PointType.on_rt,new PointModelImpl(49.406671, 8.728084)));
        points.put("LinkerTrail2Seg2", new PointDef( PointType.on_rt,new PointModelImpl(49.408471, 8.727856)));
        points.put("LinkerTrail2KreuzungSchwabenweg", new PointDef( PointType.on_rt,new PointModelImpl(49.409705, 8.728569)));
        points.put("Trail25eingangBismarkhöhenweg", new PointDef( PointType.stop,new PointModelImpl(49.409790, 8.728947)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Jägertrail2Leopoldstein",mtb_k2s2);
        points = testDef.points;
        points.put("AlterHilsbacherWegEingangTrail11", new PointDef( PointType.start,new PointModelImpl(49.396925, 8.726293)));
        points.put("MittenImJägertrail", new PointDef( PointType.on_rt,new PointModelImpl(49.393939, 8.725174)));
        points.put("Höhenweg", new PointDef( PointType.off_rt,new PointModelImpl(49.394198, 8.723542)));
        points.put("Leopoldstein", new PointDef( PointType.stop,new PointModelImpl(49.391975, 8.722021)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Leopoldstein2StaubfreierDirekteLinie",mtb_k3s3);
        points = testDef.points;
        points.put("Leopoldstein", new PointDef( PointType.start,new PointModelImpl(49.391975, 8.722021)));
        points.put("Trail35NachKreuzungObNikolausWeg", new PointDef( PointType.on_rt,new PointModelImpl(49.391187, 8.717241)));
        points.put("SchneiderscherenTrail", new PointDef( PointType.off_rt,new PointModelImpl(49.386051, 8.718700)));
        points.put("Trail35ausgangAufNikolaustrail", new PointDef( PointType.stop,new PointModelImpl(49.390886, 8.715603)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Trail35NachKreuzungObNikolausWeg",PointType.off_rt);
        points.replace("SchneiderscherenTrail",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("UntermHampen2SteilrunterAbKreuzung",mtb_k3s3);
        points = testDef.points;
        points.put("KreuzungUntermHampenVorTrail11Eingang", new PointDef( PointType.start,new PointModelImpl(49.379328, 8.716803)));
        points.put("Trail35NachKreuzungNeuerSternbruchschlagweg", new PointDef( PointType.on_rt,new PointModelImpl(49.380794, 8.714995)));
        points.put("Trail01GewannlinieEingang", new PointDef( PointType.off_rt,new PointModelImpl(49.385957, 8.712795)));
        points.put("Trail11endeVorLützelbrunnenweg", new PointDef( PointType.off_rt,new PointModelImpl(49.375643, 8.710657)));
        points.put("Trail35ausgangNäheBergmannslochquelle", new PointDef( PointType.stop,new PointModelImpl(49.380796, 8.712766)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Trail11endeVorLützelbrunnenweg",PointType.on_rt);
        points.replace("Trail35NachKreuzungNeuerSternbruchschlagweg",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

/*        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);*/

        testDef = new TestDef(testDef,"UntermHampen2SteilRunterAbTrail35eingang",mtb_k1s1);
        points = testDef.points;
        points.remove("KreuzungUntermHampenVorTrail11Eingang");
        points.addNewStart("Traileingang35",new PointDef( PointType.start,new PointModelImpl(49.379846, 8.717033)) );
        points.replace("Trail11endeVorLützelbrunnenweg",PointType.off_rt);
        points.replace("Trail01GewannlinieEingang",PointType.on_rt);
        tests.put(testDef.getId(),testDef);



        testDef = new TestDef("DurchDenErlensumpf",mtb_k2s2);
        points = testDef.points;
        points.put("Trail11nachKreuzungGewannlinieLützelbrunnenweg", new PointDef( PointType.start,new PointModelImpl(49.382343, 8.712013)));
        points.put("Trail2UntererErlensumpfweg", new PointDef( PointType.on_rt,new PointModelImpl(49.381103, 8.709465)));
        points.put("KreuzungKatzensteigRebmanspfad", new PointDef( PointType.off_rt,new PointModelImpl(49.382415, 8.708809)));
        points.put("Trail2ausgangAufRebmnannspfad", new PointDef( PointType.stop,new PointModelImpl(49.381370, 8.708344)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Trail2UntererErlensumpfweg",PointType.off_rt);
        points.replace("KreuzungKatzensteigRebmanspfad",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Schwippschwapp",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungHoherMitMittlererNistlerweg", new PointDef( PointType.start,new PointModelImpl(49.444645, 8.695056)));
        points.put("KreuzungSchwippschwappBuchbrunnenweg", new PointDef( PointType.on_rt,new PointModelImpl(49.443551, 8.694446)));
        points.put("KreuzungSchwippschwappUntererNistlerweg", new PointDef( PointType.on_rt,new PointModelImpl(49.441648, 8.692963)));
        points.put("Trail0EingangVomSchrägerWegZumHellenbachbrunnen", new PointDef( PointType.off_rt,new PointModelImpl(49.441478, 8.701421)));
        points.put("Trail2ausgangAufHellebachtalweg", new PointDef( PointType.stop,new PointModelImpl(49.440915, 8.692267)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("KreuzungSchwippschwappBuchbrunnenweg",PointType.off_rt);
        points.replace("KreuzungSchwippschwappUntererNistlerweg",PointType.off_rt);
        points.replace("Trail0EingangVomSchrägerWegZumHellenbachbrunnen",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("WeißerSteinTrailErsterTeil",mtb_k2s2);
        points = testDef.points;
        points.put("Trail12EingangAbWeißenstein", new PointDef( PointType.start,new PointModelImpl(49.453215, 8.722877)));
        points.put("WeißerSteinTrail", new PointDef( PointType.on_rt,new PointModelImpl(49.453319, 8.720844)));
        points.put("ObererDarmuthswegOben", new PointDef( PointType.off_rt,new PointModelImpl(49.453797, 8.720646)));
        points.put("ObererDarmuthswegVorKreuzung", new PointDef( PointType.off_rt,new PointModelImpl(49.453799, 8.719710)));
        points.put("WeißerSteinTrailNachSteilkurve", new PointDef( PointType.stop,new PointModelImpl(49.453696, 8.719017)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("HeiligenbergTrailAbFuchsrondell",mtb_k3s3);
        points = testDef.points;
        points.put("Trail25EingangAbBismarksäulenweg", new PointDef( PointType.start,new PointModelImpl(49.453224, 8.722876)));
        points.put("VorKreuzungMitBismarksäulenweg", new PointDef( PointType.on_rt,new PointModelImpl(49.417662, 8.698329)));
        points.put("Trail01EingangbeiMönchsberghütte", new PointDef( PointType.off_rt,new PointModelImpl(49.419570, 8.697584)));
        points.put("KreuzungHeiligenbergTrailMitMönchsbergFußweg", new PointDef( PointType.stop,new PointModelImpl(49.417906, 8.696898)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("VorKreuzungMitBismarksäulenweg",PointType.off_rt);
        points.replace("Trail01EingangbeiMönchsberghütte",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("HakkaKakkaDue(links steil)",mtb_k2s2);
        points = testDef.points;
        points.put("VorAbzweigAufLinkenTrail3", new PointDef( PointType.start,new PointModelImpl(49.404957, 8.728335)));
        points.put("UntereKreuzungTrail2ObSchlosshangweg", new PointDef( PointType.on_rt,new PointModelImpl(49.407229, 8.727651)));
        points.put("ObereKreuzungTrail3ObSchlosshangweg", new PointDef( PointType.off_rt,new PointModelImpl(49.405849, 8.726312)));
        points.put("Trail3LinksAusgangBlockhausweg", new PointDef( PointType.stop,new PointModelImpl(49.406513, 8.725379)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("UntereKreuzungTrail2ObSchlosshangweg",PointType.off_rt);
        points.replace("ObereKreuzungTrail3ObSchlosshangweg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("RocknRollLowerletztesSeg2UntSnktNikolauswegKurve",mtb_k3s3);
        points = testDef.points;
        points.put("KreuzungTrail12mitRocknRollLower", new PointDef( PointType.start,new PointModelImpl(49.390982, 8.713487)));
        points.put("mittenImRocknRoller", new PointDef( PointType.on_rt,new PointModelImpl(49.390816, 8.713254)));
        points.put("KreuzungTr12Gutselbankwweg", new PointDef( PointType.off_rt,new PointModelImpl(49.392125, 8.712285)));
        points.put("RocknRollAusgangGutselbankweg", new PointDef( PointType.stop,new PointModelImpl(49.390878, 8.712198)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("mittenImRocknRoller",PointType.off_rt);
        points.replace("KreuzungTr12Gutselbankwweg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("RocknRollAbLeopoldBisNikolaustrail00",mtb_k2s2);
        points = testDef.points;
        points.put("AbzweigZumRocknRollLower", new PointDef( PointType.start,new PointModelImpl(49.391304, 8.720920)));
        points.put("KurveTrail23nachSchneiderschere", new PointDef( PointType.on_rt,new PointModelImpl(49.386050, 8.718701)));
        points.put("RocknRoll2Seg", new PointDef( PointType.off_rt,new PointModelImpl(49.391298, 8.716664)));
        points.put("AufNikolaustrailZwischenAusgangRocknRollUndTrailSchneiderschere", new PointDef( PointType.stop,new PointModelImpl(49.390060, 8.715939)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("KurveTrail23nachSchneiderschere",PointType.off_rt);
        points.replace("RocknRoll2Seg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("RocknRollAbLeopold2KreuzungTrail12nachGaibergerWeg",mtb_k3s3);
        points = testDef.points;
        points.put("AbzweigZumRocknRollLower", new PointDef( PointType.start,new PointModelImpl(49.391304, 8.720920)));
        points.put("KurveTrail23nachSchneiderschere", new PointDef( PointType.off_rt,new PointModelImpl(49.386050, 8.718701)));
        points.put("RocknRoll2Seg", new PointDef( PointType.on_rt,new PointModelImpl(49.391298, 8.716664)));
        points.put("AufNikolaustrailAbzweigPfadZurStrasse", new PointDef( PointType.on_rt,new PointModelImpl(49.390601, 8.715719)));
        points.put("AbzweigInRocknRollVomGaibergerWeg", new PointDef( PointType.on_rt,new PointModelImpl(49.391401, 8.714884)));
        points.put("SchneiderscherenTrailkreuztGaibergerWeg", new PointDef( PointType.off_rt,new PointModelImpl(49.388383, 8.716030)));
        points.put("KreuzungSchneiderscherenTrail12RocknRoll", new PointDef( PointType.stop,new PointModelImpl(49.390982, 8.713487)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("KurveTrail23nachSchneiderschere",PointType.on_rt);
        points.replace("RocknRoll2Seg",PointType.off_rt);
        points.replace("AufNikolaustrailAbzweigPfadZurStrasse",PointType.off_rt);
        points.replace("SchneiderscherenTrailkreuztGaibergerWeg", PointType.off_rt);
        points.replace("AbzweigInRocknRollVomGaibergerWeg",PointType.off_rt);
        tests.put(testDef.getId(),testDef);



        testDef = new TestDef("Dossenheim2Handschuhsheim",mtb_k2s2);
        points = testDef.points;
        points.put("EingangInsFeldBeiChilli", new PointDef( PointType.start,new PointModelImpl(49.445735, 8.677127)));
        points.put("ErsteKurve", new PointDef( PointType.on_rt,new PointModelImpl(49.442731, 8.678377)));
        points.put("KreuzungHöllenbachweg", new PointDef( PointType.on_rt,new PointModelImpl(49.438463, 8.680897)));
        points.put("DossenheimerLandStr", new PointDef( PointType.off_rt,new PointModelImpl(49.441184, 8.677819)));
        points.put("KrezungZumSteinberg", new PointDef( PointType.stop,new PointModelImpl(49.434137, 8.683404)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

/*        testDef = new TestDef("HebertTrailAbKreuzungPipelinePfad2Strasse",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungPipelinePfad", new PointDef( PointType.start,new PointModelImpl(49.443190, 8.968595)));
        points.put("AufDemHebert", new PointDef( PointType.on_rt,new PointModelImpl(49.444141, 8.969492)));
        points.put("PipelinePfad", new PointDef( PointType.off_rt,new PointModelImpl(49.444272, 8.968929)));
        points.put("KreuzungRüdenwaldwegStrasse", new PointDef( PointType.stop,new PointModelImpl(49.445093, 8.969311)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);*/


        testDef = new TestDef("ZweiarmigerBanditVorLetztesUndLetztesSegment",mtb_k3s3);
        points = testDef.points;
        points.put("EingangTrailsegmentMitKurzerVerbindungZumLudwigsPfad", new PointDef( PointType.start,new PointModelImpl(49.460704, 9.000151)));
        points.put("AufDemZweiarmigerBanditKreuzungVorVorletzemSeg", new PointDef( PointType.on_rt,new PointModelImpl(49.461107, 8.998830)));
        points.put("AufDemZweiarmigerBandit", new PointDef( PointType.on_rt,new PointModelImpl(49.462121, 8.998568)));
        points.put("AufdemPfad1Daneben", new PointDef( PointType.off_rt,new PointModelImpl(49.462172, 8.998216)));
        points.put("EndeLudwigspfad", new PointDef( PointType.off_rt,new PointModelImpl(49.461296, 8.997313)));
        points.put("EndeZweiarmigerBandit", new PointDef( PointType.stop,new PointModelImpl(49.462394, 8.998455)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("ZweiarmigerBanditVorLetztesUndLetztesSegBisEppStr",mtb_k3s3);
        points = testDef.points;
        points.put("EingangTrailsegmentMitKurzerVerbindungZumLudwigsPfad", new PointDef( PointType.start,new PointModelImpl(49.460681, 9.000181)));
        points.put("AufDemZweiarmigerBanditKreuzungVorVorletzemSeg", new PointDef( PointType.on_rt,new PointModelImpl(49.461107, 8.998830)));
        points.put("EndeLudwigspfad", new PointDef( PointType.off_rt,new PointModelImpl(49.461296, 8.997313)));
        points.put("TrailEndeRudolfEppStr", new PointDef( PointType.stop,new PointModelImpl(49.462487, 8.998270)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Darkstone2SchwabenwegBeiHütte",mtb_k3s3);
        points = testDef.points;
        points.put("EingangDarkstone", new PointDef( PointType.start,new PointModelImpl(49.402884, 8.731825)));
        points.put("Darkstone1Seg", new PointDef( PointType.on_rt,new PointModelImpl(49.403512, 8.732308)));
        points.put("Darkstone2Seg35", new PointDef( PointType.on_rt,new PointModelImpl(49.404621, 8.731857)));
        points.put("AufTrailVorAuerhahnhütte", new PointDef( PointType.off_rt,new PointModelImpl(49.404063, 8.736623)));
        points.put("Darkstone3Seg35NachBlockhausweg", new PointDef( PointType.on_rt,new PointModelImpl(49.405845, 8.731758)));

        points.put("TrailausgangSchwabenweg", new PointDef( PointType.stop,new PointModelImpl(49.406258, 8.732099)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("TrailVomHeidelochSeg1",mtb_k3s3);
        points = testDef.points;
        points.put("EingangKeltenweg", new PointDef( PointType.start,new PointModelImpl(49.420862, 8.704491)));
        points.put("ImSteilenTrail25", new PointDef( PointType.on_rt,new PointModelImpl(49.419150, 8.702862)));
        points.put("KreuzungAussichtsturmfahrwegChaisenweg", new PointDef( PointType.off_rt,new PointModelImpl(49.420655, 8.703688)));
        points.put("TrailAusgangAufAussichtsturmfahrweg", new PointDef( PointType.stop,new PointModelImpl(49.418552, 8.702183)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("ForstwegeUntermWaidhaus",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungNeuerGrenzwegFichtenpfad", new PointDef(PointType.start,new PointModelImpl(49.366877, 8.728592)));
        points.put("KreuzungNeuerGrenzwegWaidhausweg", new PointDef(PointType.off_rt, new PointModelImpl(49.371356, 8.724859)));
        points.put("KreuzungMittlererWegFichtenpfad", new PointDef( PointType.on_rt, new PointModelImpl(49.367711, 8.725901)));
        points.put("KreuzungMittlererWegWaidhausweg", new PointDef(PointType.stop,new PointModelImpl(49.372329, 8.723106)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("WiesenaussichtNachLampenheim2Trail0ParallelStraße",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungWaldrand", new PointDef(PointType.start,new PointModelImpl(49.492574, 8.759262)));
        points.put("AufVerbindungstrail0", new PointDef(PointType.on_rt, new PointModelImpl(49.491630, 8.758857)));
        points.put("AufStraßeVonLampenheim", new PointDef( PointType.off_rt, new PointModelImpl(49.491147, 8.759739)));
        points.put("AufTrailParallelStraße", new PointDef(PointType.stop,new PointModelImpl(49.488621, 8.760076)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Trail11VomHauptweg(WSStraße)2Nistlerweg",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungHauptwegTrail11", new PointDef(PointType.start,new PointModelImpl(49.451815, 8.709768)));
        points.put("Trail11Seg1", new PointDef(PointType.off_rt, new PointModelImpl(49.451892, 8.710657)));
        points.put("KreuzungTrail11mitTrail02", new PointDef(PointType.off_rt, new PointModelImpl(49.452464, 8.711422)));
        points.put("Trail11Seg2", new PointDef(PointType.off_rt, new PointModelImpl(49.453398, 8.712116)));
        points.put("KreuzungHauptwegNistlerweg", new PointDef( PointType.on_rt, new PointModelImpl(49.455553, 8.711398)));
        points.put("Trail11EndeAufNistlerweg", new PointDef(PointType.stop,new PointModelImpl(49.454087, 8.712756)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Trail11Seg1",PointType.on_rt);
        points.replace("KreuzungTrail11mitTrail02",PointType.on_rt);
        points.replace("Trail11Seg2",PointType.on_rt);
        points.replace("KreuzungHauptwegNistlerweg",PointType.off_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("BärenbrunnenRichtungKohlhofwiese",mtb_k3s3);
        points = testDef.points;
        points.put("AbzweigSteilerTrail12", new PointDef(PointType.start,new PointModelImpl(49.373695, 8.747528)));
        points.put("Trail12", new PointDef(PointType.on_rt, new PointModelImpl(49.374226, 8.747718)));
        points.put("KreuzungUntererSandwegWeißerSteinschlag", new PointDef( PointType.off_rt, new PointModelImpl(49.374404, 8.749379)));
        points.put("Trail12EndeAufWeißerSteinschlag", new PointDef(PointType.stop,new PointModelImpl(49.374738, 8.747645)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Trail12",PointType.off_rt);
        points.replace("KreuzungUntererSandwegWeißerSteinschlag",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Bismarkhöhe2KreuzungRodelwegBlockhausweg",mtb_k3s3);
        points = testDef.points;
        points.put("KreuzungSchwabenwegRodelweg", new PointDef(PointType.start,new PointModelImpl(49.409456, 8.729452)));
        points.put("HakaKakaParallelRodelweg2", new PointDef(PointType.off_rt, new PointModelImpl(49.408617, 8.728070)));
        points.put("Rodelweg", new PointDef( PointType.on_rt, new PointModelImpl(49.408578, 8.728632)));
        points.put("KreuzungVerbindungswegBlockhausweg", new PointDef( PointType.off_rt, new PointModelImpl(49.406662, 8.725651)));
        points.put("KreuzungRodelwegBlockhausweg", new PointDef(PointType.stop,new PointModelImpl(49.407562, 8.728200)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Rodelweg",PointType.off_rt);
        points.replace("KreuzungVerbindungswegBlockhausweg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("ParkplatzEhrenfriedh2AusichtDreitrögeweg",mtb_k1s1);
        points = testDef.points;
        points.put("ParkplatzEhrenfried", new PointDef(PointType.start,new PointModelImpl(49.390955, 8.701738)));
        points.put("Dreitrögehütte", new PointDef(PointType.off_rt, new PointModelImpl(49.394941, 8.701868)));
        points.put("hinterRhododendronpark", new PointDef( PointType.off_rt, new PointModelImpl(49.388516, 8.697659)));
        points.put("Waldpiraten", new PointDef( PointType.off_rt, new PointModelImpl(49.386808, 8.697917)));
        points.put("flacherTrail01zumDreitrögeweg", new PointDef( PointType.on_rt, new PointModelImpl(49.390104, 8.696048)));
        points.put("AusichtDreitrögeweg", new PointDef(PointType.stop,new PointModelImpl(49.395333, 8.695417)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("hinterRhododendronpark",PointType.on_rt);
        points.replace("Waldpiraten",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("HangkantenwegAbErbsweg2EndeGaisbergtrSteigerweg",mtb_k1s1);
        points = testDef.points;
        points.put("HangkantenwegKrezungErbsweg", new PointDef(PointType.start,new PointModelImpl(49.396271, 8.705378)));
        points.put("AufHangkantenweg", new PointDef(PointType.on_rt, new PointModelImpl(49.395545, 8.702753)));
        points.put("Dreitrögehütte", new PointDef(PointType.off_rt, new PointModelImpl(49.394941, 8.701868)));
        points.put("BierhelderSteige", new PointDef(PointType.off_rt, new PointModelImpl(49.395708, 8.700246)));
        points.put("VorletzteKurveHutzelwaldpfad", new PointDef( PointType.on_rt, new PointModelImpl(49.396882, 8.698677)));
        points.put("SteilerPfadParallelHutzelwaldpfad", new PointDef( PointType.off_rt, new PointModelImpl(49.397229, 8.698786)));
        points.put("EndeGaisbergtrailAufSteigerweg", new PointDef(PointType.stop,new PointModelImpl(49.396725, 8.698134)));//49.396782, 8.698137)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("AufHangkantenweg", PointType.off_rt);
        points.replace("VorletzteKurveHutzelwaldpfad",PointType.off_rt);
        points.replace("SteilerPfadParallelHutzelwaldpfad",PointType.on_rt);
        points.remove("EndeGaisbergtrailAufSteigerweg");
        points.put("EndeGaisbergtrailVorSteigerweg", new PointDef(PointType.stop,new PointModelImpl(49.396782, 8.698137)));

        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("HangkantenwegJohannesHoops2EndeGaisbergtrSteigerweg",mtb_k1s1);
        points = testDef.points;
        points.put("KreugzungJohannesHoopsWeg", new PointDef(PointType.start,new PointModelImpl(49.398338, 8.708015)));
        points.put("AufHangkantenweg", new PointDef(PointType.on_rt, new PointModelImpl(49.395545, 8.702753)));
        points.put("Dreitrögehütte", new PointDef(PointType.off_rt, new PointModelImpl(49.394941, 8.701868)));
        points.put("BierhelderSteige", new PointDef(PointType.off_rt, new PointModelImpl(49.395708, 8.700246)));
        points.put("VorletzteKurveHutzelwaldpfad", new PointDef( PointType.on_rt, new PointModelImpl(49.396882, 8.698677)));
        points.put("SteilerPfadParallelHutzelwaldpfad", new PointDef( PointType.off_rt, new PointModelImpl(49.397229, 8.698786)));
        points.put("EndeGaisbergtrailAufSteigerweg", new PointDef(PointType.stop,new PointModelImpl(49.396725, 8.698134)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SteinernerWeg2PavillionwegBeiLeutershausen",mtb_k1s1);
        points = testDef.points;
        points.put("AbzweigInPfad", new PointDef(PointType.start,new PointModelImpl(49.504514, 8.682480)));
        points.put("AbzweigaufSC5Weg", new PointDef(PointType.off_rt, new PointModelImpl(49.506110, 8.680878)));
        points.put("AbzweigSteinernerWegAufBuchenweg", new PointDef( PointType.on_rt, new PointModelImpl(49.503580, 8.683963)));
        points.put("KreuzungBuchenwegPavillonweg", new PointDef(PointType.stop,new PointModelImpl(49.505368, 8.684186)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("PfadSc6vsWegSc3UunterGeiersberg",mtb_k1s1);
        points = testDef.points;
        points.put("AbzweigInPfadVomObererGeiersbergweg", new PointDef(PointType.start,new PointModelImpl(49.534260, 8.679459)));
        points.put("AufSc6Pfad", new PointDef(PointType.on_rt, new PointModelImpl(49.534571, 8.684164)));
        points.put("ObererGeiersbergweg", new PointDef( PointType.off_rt, new PointModelImpl(49.533858, 8.684484)));
        points.put("EndeSc6Pfad", new PointDef(PointType.stop,new PointModelImpl(49.532809, 8.687623)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SteilerPfadRunterSüdöstlichWachenberg",mtb_k2s2);
        points = testDef.points;
        points.put("EinstiegAufS3Pfad", new PointDef(PointType.start,new PointModelImpl(49.549549, 8.695886)));
        points.put("AufSc6Pfad", new PointDef(PointType.on_rt, new PointModelImpl(49.549703, 8.697191)));
        points.put("AufSc6PfadletztesSeg", new PointDef(PointType.on_rt, new PointModelImpl(49.549317, 8.698809)));
        points.put("Wachenburg", new PointDef( PointType.off_rt, new PointModelImpl(49.548956, 8.685553)));
        points.put("EndeSc6PfadAufWegRichtungBuchklingenhütte", new PointDef(PointType.stop,new PointModelImpl(49.549032, 8.699494)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("AufSc6PfadletztesSeg");
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("PfadAnVfLBirkenauSporthalleVorbei",mtb_k1s1);
        points = testDef.points;
        points.put("EinstiegKurzhinterBankAufPfad", new PointDef(PointType.start,new PointModelImpl(49.556338, 8.694335)));
        points.put("AufPfadHinterSporthalle", new PointDef(PointType.on_rt, new PointModelImpl(49.558286, 8.695252)));
        points.put("AufSchotterwegAbzweigZumTrail", new PointDef( PointType.off_rt, new PointModelImpl(49.555151, 8.692809)));
        points.put("KreuzungZumLetztenPfadsegment", new PointDef(PointType.stop,new PointModelImpl(49.557370, 8.693153)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("KreuzbergTrail",mtb_k2s2);
        points = testDef.points;
        points.put("Kreuzberg", new PointDef(PointType.start,new PointModelImpl(49.602682, 8.672619)));
        points.put("ErstesSeg", new PointDef(PointType.on_rt, new PointModelImpl(49.599321, 8.669597)));
        points.put("KreuzungGeiersbergwegPf00", new PointDef(PointType.off_rt, new PointModelImpl(49.600203, 8.664690)));
        points.put("LetztesSegOben", new PointDef( PointType.on_rt, new PointModelImpl(49.596837, 8.665279)));
        points.put("LetztesSegUnten", new PointDef( PointType.on_rt, new PointModelImpl(49.596558, 8.663551)));
        points.put("KreuzungEppenweg", new PointDef(PointType.stop,new PointModelImpl(49.596779, 8.660711)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("AlleFarbenTrailEberbach",mtb_k3s3);
        points = testDef.points;
        points.put("AbzweigInTrailVomAussichtsweg", new PointDef(PointType.start,new PointModelImpl(49.459554, 8.993943)));
        points.put("AufAlleFarben", new PointDef(PointType.on_rt, new PointModelImpl(49.459583, 8.993316)));
        points.put("PfadLinksDaneben", new PointDef( PointType.off_rt, new PointModelImpl(49.459539, 8.993341)));
        points.put("EinstiegUnteresSegAlleFarben", new PointDef(PointType.stop,new PointModelImpl(49.459781, 8.991989)));
        tests.put(testDef.getId(),testDef);
        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("HutzelwaldHütte2AbzweigTrail12NachDachsbaumweg",mtb_k3s3);
        points = testDef.points;
        points.put("KreuzungMitJohannesHoopsWeg", new PointDef(PointType.start,new PointModelImpl(49.400439, 8.698712)));
        points.put("Trail1Seg1", new PointDef(PointType.on_rt, new PointModelImpl(49.399992, 8.698791)));
        points.put("Trail2Seg2", new PointDef(PointType.on_rt, new PointModelImpl(49.399182, 8.698798)));
        points.put("Trail26Seg3", new PointDef(PointType.on_rt, new PointModelImpl(49.398928, 8.699361)));
        points.put("Grenzpfad", new PointDef( PointType.off_rt, new PointModelImpl(49.399661, 8.698099)));
        points.put("AbzweigTrail12NachDachsbaumweg", new PointDef(PointType.stop,new PointModelImpl(49.398653, 8.699335)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Stickelsplatz2MühltalPart2",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungSchneebergswegBreitwaldFußweg", new PointDef(PointType.start,new PointModelImpl(49.430967, 8.713128)));
        points.put("KreuzungBeiSchneeberg", new PointDef(PointType.off_rt, new PointModelImpl(49.427903, 8.699584)));
        points.put("BreitwaldFußwegPart1", new PointDef(PointType.on_rt, new PointModelImpl(49.430344, 8.709149)));
        points.put("BreitwaldFußwegPart2", new PointDef(PointType.on_rt, new PointModelImpl(49.430990, 8.707399)));
        points.put("EndeBreitwaldFußwegMühltal", new PointDef(PointType.stop,new PointModelImpl(49.431104, 8.705020)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("ElenaTrailUphill",mtb_k2s2);
        points = testDef.points;
        points.put("TrailAusgangAufBuchenweg", new PointDef(PointType.start,new PointModelImpl(49.498650, 8.678422)));
        points.put("AufGrenzpfad", new PointDef(PointType.on_rt, new PointModelImpl(49.502392, 8.682523)));
        points.put("AufTrail", new PointDef(PointType.off_rt, new PointModelImpl(49.499194, 8.684575)));
        points.put("Kanzelbergostweg", new PointDef(PointType.stop,new PointModelImpl(49.501136, 8.685170))); // new PointModelImpl(49.501234, 8.685921)
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("AufGrenzpfad",PointType.off_rt);
        points.replace("AufTrail",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SteigerwegEckeObGaisbergweg2Materpfahl",mtb_k3s3);
        points = testDef.points;
        points.put("SteigerwegEckeObGaisbergweg", new PointDef(PointType.start,new PointModelImpl(49.397605, 8.693775)));
        points.put("Rondellhütte", new PointDef(PointType.off_rt, new PointModelImpl(49.405574, 8.700274)));
        points.put("ObererDachswegVorHutzelwaldhütte", new PointDef(PointType.off_rt, new PointModelImpl(49.399737, 8.698473)));
        points.put("Dreitrögehütte", new PointDef(PointType.off_rt, new PointModelImpl(49.394941, 8.701868)));
        points.put("VorletzteKurveHutzelwaldpfad", new PointDef( PointType.on_rt, new PointModelImpl(49.396882, 8.698677)));
        points.put("KühruhpfadEndeJohannesHoops", new PointDef(PointType.on_rt, new PointModelImpl(49.398304, 8.707980)));//(49.395369, 8.705392
        points.put("Materpfahl", new PointDef(PointType.on_rt,new PointModelImpl(49.401355, 8.710173)));
        points.put("AbzweigTrailBlockhaus", new PointDef(PointType.stop,new PointModelImpl(49.401390, 8.710339)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("VorletzteKurveHutzelwaldpfad",PointType.off_rt);
        points.replace("Dreitrögehütte",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Rondellhütte",PointType.off_rt);
        points.replace("KühruhpfadEndeJohannesHoops",PointType.off_rt);
        points.replace("Dreitrögehütte",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Zollstockbrunnen2Heidenknörzel",mtb_k3s3);
        points = testDef.points;
        points.put("Zollstockbrunnen", new PointDef(PointType.start,new PointModelImpl(49.429105, 8.715362)));
        points.put("Stickelsplatz", new PointDef(PointType.off_rt, new PointModelImpl(49.431455, 8.720468)));
        points.put("HeidenknörzeltrailSteilstück", new PointDef(PointType.on_rt, new PointModelImpl(49.429240, 8.717022)));
        points.put("HeidenknörzeltrailEndeSteilstück", new PointDef(PointType.stop,new PointModelImpl(49.429379, 8.718693)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("SchneiderpfadAbSpälterbrunnen2Kohlhofwiese",mtb_k1s1);
        points = testDef.points;
        points.put("Spälterbrunnen", new PointDef(PointType.start,new PointModelImpl(49.379543, 8.746256)));
        points.put("KreuzungWeißtannenWegWaldhilsbacherWeg", new PointDef(PointType.on_rt, new PointModelImpl(49.380817, 8.753301)));
        points.put("SchneiderpfadZwischenSchmerschlagteichwegObererSandweg", new PointDef(PointType.off_rt, new PointModelImpl(49.381192, 8.743884)));
        points.put("StefansHütte", new PointDef(PointType.on_rt, new PointModelImpl(49.381673, 8.746796)));
        points.put("SchneiderpfadZwischenAlterHeuwegUKohlhofwiese", new PointDef(PointType.off_rt, new PointModelImpl(49.382745, 8.743593)));
        points.put("Hilsbachertorweg", new PointDef(PointType.off_rt, new PointModelImpl(49.384367, 8.744410)));
        points.put("NördlicheAlternativeZuHilsbachertorweg", new PointDef(PointType.off_rt, new PointModelImpl(49.386303, 8.743421)));
        points.put("NördlicheBank", new PointDef(PointType.stop,new PointModelImpl(49.384955, 8.741575)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("KreuzungWeißtannenWegWaldhilsbacherWeg",PointType.off_rt);
        points.replace("NördlicheAlternativeZuHilsbachertorweg",PointType.off_rt);
        points.replace("SchneiderpfadZwischenAlterHeuwegUKohlhofwiese",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Michelsbrunnen2Kohlhofwiese",mtb_k1s1);
        points = testDef.points;
        points.put("AusgangTrailVorMichelsbrunnen", new PointDef(PointType.start,new PointModelImpl(49.387875, 8.744807)));
        points.put("Hilsbachertorweg", new PointDef(PointType.off_rt, new PointModelImpl(49.384367, 8.744410)));
        points.put("NördlicheAlternativeZuHilsbachertorweg", new PointDef(PointType.on_rt, new PointModelImpl(49.386303, 8.743421)));
        points.put("NördlicheBank", new PointDef(PointType.stop,new PointModelImpl(49.384955, 8.741575)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("KleinesTrailDreieckNordöstlichHooheLoog",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungTrailDn1mitTrackDn1Norden", new PointDef(PointType.start,new PointModelImpl(49.335476, 8.099696)));
        points.put("AufTrailDn1", new PointDef(PointType.on_rt, new PointModelImpl(49.334827, 8.099681)));
        points.put("TackTrackKreuzung", new PointDef(PointType.off_rt, new PointModelImpl(49.334817, 8.097056)));
        points.put("KreuzungTrail10mitTrack10Süden", new PointDef(PointType.stop,new PointModelImpl(49.334067, 8.099410)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("TrailVsTrackSSüdOstLambrecht",mtb_k1s1);
        points = testDef.points;
        points.put("Trail11KreuztTrack3", new PointDef(PointType.start,new PointModelImpl(49.358734, 8.058616)));
        points.put("BeimTisch", new PointDef(PointType.off_rt, new PointModelImpl(49.360361, 8.062884)));
        points.put("AbzweigVormKaisergarten", new PointDef(PointType.on_rt, new PointModelImpl(49.355720, 8.054613)));
        points.put("ObenAufGipfelAbzweigPfad", new PointDef(PointType.stop,new PointModelImpl(49.357591, 8.061158)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Totenkopf2HellerLetzesTrailSeg",mtb_k2s2);
        points = testDef.points;
        points.put("EinstiegTrail", new PointDef(PointType.start,new PointModelImpl(49.334891, 8.050226)));
        points.put("AufTrail", new PointDef(PointType.on_rt, new PointModelImpl(49.335478, 8.051178)));
        points.put("AufForstweg", new PointDef(PointType.off_rt, new PointModelImpl(49.335681, 8.051029)));
        points.put("Hellerhütte", new PointDef(PointType.stop,new PointModelImpl(49.336026, 8.053136)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("FelsenmeerBeimLamertskopf",mtb_k1s1);
        points = testDef.points;
        points.put("EinstiegInDenSC6Trail", new PointDef(PointType.start, new PointModelImpl(49.402337, 8.786259)));
        points.put("EinstiegInTrailOberhalbSteinbruch", new PointDef(PointType.off_rt, new PointModelImpl(49.407516, 8.782942)));
        points.put("NachErsterQueerungFelsenmeer", new PointDef(PointType.on_rt,new PointModelImpl(49.407521, 8.785701)));
        points.put("NachZweiterQueerungFelsenmeer", new PointDef(PointType.on_rt, new PointModelImpl(49.405449, 8.788424)));
        points.put("SchildSteinbruchNeckarhalde", new PointDef(PointType.off_rt, new PointModelImpl(49.413268, 8.781722)));
        points.put("EndePfadBei4Forstwegen", new PointDef(PointType.stop,new PointModelImpl(49.409448, 8.785783)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Wolfsgrube2Eichköpfel",mtb_k3s3);
        points = testDef.points;
        points.put("Wolfsgrube", new PointDef(PointType.start,new PointModelImpl(49.462690, 8.823620)));
        points.put("HöhenwegLangesSeg", new PointDef(PointType.on_rt, new PointModelImpl(49.477503, 8.809044)));
        points.put("Schwarzerweg", new PointDef(PointType.off_rt, new PointModelImpl(49.477528, 8.808549)));
        points.put("ObererHackwaldweg", new PointDef(PointType.off_rt, new PointModelImpl(49.478390, 8.810195)));
        points.put("Pfad1ÜberGipfel", new PointDef(PointType.on_rt, new PointModelImpl(49.484354, 8.812321)));
        points.put("Kreuzeichenweg", new PointDef(PointType.off_rt, new PointModelImpl(49.485275, 8.811254)));
        points.put("TrailausfahrtVorEichköpfel", new PointDef(PointType.stop,new PointModelImpl(49.488160, 8.813255)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("Pfad1ÜberGipfel",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SchwabenwegBeiGaibergerWegZumTunnelKSBahn",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungSchwabenwegTrailZumBlockhaus", new PointDef(PointType.start,new PointModelImpl(49.402837, 8.720541)));
        points.put("KreuzungSchwabenwegKammerforstweg", new PointDef(PointType.on_rt, new PointModelImpl(49.405229, 8.719440)));
        points.put("KreuzungTrailBlockhauswegBeiBlockhaus", new PointDef(PointType.off_rt, new PointModelImpl(49.401510, 8.713823)));
        points.put("KreuzungKammerforstwegBlockhausweg", new PointDef(PointType.stop,new PointModelImpl(49.403945, 8.722499)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("KreuzungSchwabenwegKammerforstweg",PointType.off_rt);
        points.replace("KreuzungTrailBlockhauswegBeiBlockhaus",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("FlacherS1TrailZwischenNeuerHilsbacherWegUndMichelsbr",mtb_k2s2);
        points = testDef.points;
        points.put("EinstiegTrail", new PointDef(PointType.start,new PointModelImpl(49.391504, 8.744444)));
        points.put("AufS1Trail", new PointDef(PointType.on_rt, new PointModelImpl(49.388734, 8.745389)));
        points.put("Lochwiesenweg", new PointDef(PointType.off_rt, new PointModelImpl(49.389092, 8.746103)));
        points.put("AusgangTrailVorMichelsbrunne", new PointDef(PointType.stop,new PointModelImpl(49.387875, 8.744807)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("AufS1Trail",PointType.off_rt);
        points.replace("Lochwiesenweg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("S1K1TrailHochZurStefanshütte",mtb_k2s2);
        points = testDef.points;
        points.put("KreuzungAlterHeuwegS1K1WaldhilsbacherWegTrail", new PointDef(PointType.start,new PointModelImpl(49.379168, 8.753512)));
        points.put("AlterHeuweg", new PointDef(PointType.off_rt, new PointModelImpl(49.380762, 8.750582)));
        points.put("WaldhilsbacherWeg", new PointDef(PointType.on_rt, new PointModelImpl(49.381499, 8.751497)));
        points.put("Stefanshütte", new PointDef(PointType.stop,new PointModelImpl(49.381673, 8.746796)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("S1TrailHochZumDachsbauwegBeiEmmertsgrund",mtb_k3s3);
        points = testDef.points;
        points.put("SpitzeKreuzungTrails", new PointDef(PointType.start,new PointModelImpl(49.359326, 8.706204)));
        points.put("KreuzungFlacherTrailDachsbauweg", new PointDef(PointType.off_rt, new PointModelImpl(49.360991, 8.707579)));
        points.put("S1TrailSteilhoch", new PointDef(PointType.on_rt, new PointModelImpl(49.359812, 8.707260)));
        points.put("TrailendeAufDachsbauweg", new PointDef(PointType.stop,new PointModelImpl(49.359871, 8.708678)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.replace("KreuzungFlacherTrailDachsbauweg",PointType.on_rt);
        points.replace("S1TrailSteilhoch",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Klostergarten2Stickelsplatz",mtb_k1s1);
        points = testDef.points;
        points.put("WeggableungNachWiesenweg", new PointDef(PointType.start,new PointModelImpl(49.421586, 8.737997)));
        points.put("EngelswiesenwegBeiKühruhe", new PointDef(PointType.on_rt, new PointModelImpl(49.422290, 8.718283)));
        points.put("NebenMausbachquelle", new PointDef(PointType.off_rt, new PointModelImpl(49.431009, 8.727382)));
        points.put("Stickelssplatz", new PointDef(PointType.stop,new PointModelImpl(49.431568, 8.720757))); //49.431455, 8.720468
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("EngelswiesenwegBeiKühruhe",PointType.off_rt);
        points.replace("NebenMausbachquelle",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("ZiegelhausenAuweg2Aukopfhütte",mtb_k1s1);
        points = testDef.points;
        points.put("TrailEinstiegOchsenwegle", new PointDef(PointType.start,new PointModelImpl(49.411420, 8.763550)));
        points.put("Ochsenwegle", new PointDef(PointType.off_rt, new PointModelImpl(49.409775, 8.762565)));
        points.put("WolfsbrunnerAukopfweg", new PointDef(PointType.on_rt, new PointModelImpl(49.409371, 8.748842)));
        points.put("KreuzungBeiAukopfhütte", new PointDef(PointType.stop,new PointModelImpl(49.411316, 8.757689)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("WolfsbrunnerAukopfweg",PointType.off_rt);
        points.replace("Ochsenwegle",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("TrailGroßsachsen2Kunzmühle",mtb_k2s2);
        points = testDef.points;
        points.put("AbzweigHindenburgweg2Kohlbachweg", new PointDef(PointType.start,new PointModelImpl(49.516342, 8.697618)));
        points.put("StartTrail", new PointDef(PointType.off_rt, new PointModelImpl(49.514653, 8.694995)));
        points.put("BelzholKommtVonRechts", new PointDef(PointType.on_rt, new PointModelImpl(49.514913, 8.692844)));
        points.put("AbzweigVonBelzhohlZurHütte", new PointDef(PointType.on_rt, new PointModelImpl(49.514450, 8.686355)));
        points.put("Teuerbrunnenweg", new PointDef(PointType.off_rt, new PointModelImpl(49.513760, 8.686431)));
        points.put("RunterZurOdenwaldstr", new PointDef(PointType.off_rt, new PointModelImpl(49.511075, 8.687347)));
        points.put("Kunzmühle", new PointDef(PointType.stop,new PointModelImpl(49.510054, 8.678618)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("Kunzmühle");
        points.put("TrailausfahrtAufTalstr", new PointDef(PointType.stop,new PointModelImpl(49.509885, 8.678008)));

        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Posseltslust2Leopoldstein",mtb_k2s2);
        points = testDef.points;
        points.put("AbzweigInPosseltslustpfad", new PointDef(PointType.start,new PointModelImpl(49.385576, 8.731245)));
        points.put("Posseltslustpfad", new PointDef(PointType.on_rt, new PointModelImpl(49.387974, 8.729255)));
        points.put("AufJägerpfad", new PointDef(PointType.on_rt, new PointModelImpl(49.391290, 8.726399)));
        points.put("AufKohlhöferweg", new PointDef(PointType.off_rt, new PointModelImpl(49.390479, 8.725705)));
        points.put("AufPosseltsweg", new PointDef(PointType.off_rt, new PointModelImpl(49.389804, 8.724568)));
        points.put("EndeJägerpfad", new PointDef(PointType.stop,new PointModelImpl(49.391954, 8.722524)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SKastanine2Bismarkhöhenweg",mtb_k1s1);
        points = testDef.points;
        points.put("ElisabethenwegTraileinstieg", new PointDef(PointType.start,new PointModelImpl(49.412763, 8.723805)));
        points.put("ElisabethenwegNachErsterSerpentine", new PointDef(PointType.off_rt,new PointModelImpl(49.412617, 8.723952)));
        points.put("Rindenhäuschen", new PointDef(PointType.off_rt, new PointModelImpl(49.410567, 8.723291)));
        points.put("Molkenkur", new PointDef(PointType.on_rt, new PointModelImpl(49.407038, 8.715009)));
        points.put("KreuzungTrailMitBismarkhöhenweg", new PointDef(PointType.stop,new PointModelImpl(49.408600, 8.723583)));
        tests.put(testDef.getId(),testDef);

        TestDef testDeftmp = new TestDef(testDef,mtb_k2s2);
        points =testDeftmp.points;
        points.replace("ElisabethenwegNachErsterSerpentine",PointType.on_rt);
        points.replace("Rindenhäuschen",PointType.on_rt);
        points.replace("Molkenkur",PointType.off_rt);
        tests.put(testDeftmp.getId(),testDeftmp);

        testDef = new TestDef(testDef,"SKastanine2BismarkhöhenwegStartImTrail",mtb_k1s1);
        points =testDef.points;
        points.remove("ElisabethenwegTraileinstieg");
        points.replace("ElisabethenwegNachErsterSerpentine",PointType.start);
        points.replace("Rindenhäuschen",PointType.on_rt);
        points.replace("Molkenkur",PointType.off_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SpeierheldhütteRichtungHoheLoog",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungSpeierheldhütte", new PointDef(PointType.start,new PointModelImpl(49.338317, 8.111724)));
        points.put("Direkter23Trail", new PointDef(PointType.off_rt,new PointModelImpl(49.333550, 8.108029)));
        points.put("Nördlicher11Trail", new PointDef(PointType.off_rt, new PointModelImpl(49.336620, 8.105987)));
        points.put("NördlicherSC4Track", new PointDef(PointType.on_rt, new PointModelImpl(49.334529, 8.102775)));
        points.put("Trail1HochüberRückennachNorden", new PointDef(PointType.on_rt, new PointModelImpl(49.336009, 8.099721)));
        points.put("KreuzungDirekterUndNördlicherTrailHoheLoogEbene", new PointDef(PointType.stop,new PointModelImpl(49.332571, 8.101318)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points =testDef.points;
        points.replace("Nördlicher11Trail",PointType.on_rt);
        points.replace("NördlicherSC4Track",PointType.off_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef(testDef,mtb_k3s3);
        points =testDef.points;
        points.replace("Direkter23Trail",PointType.on_rt);
        points.replace("Nördlicher11Trail",PointType.off_rt);
        points.replace("Trail1HochüberRückennachNorden",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("DrachensteinPfalzSüdlicheAuffahrtBisSpitzkehre",mtb_k1s1);
        points = testDef.points;
        points.put("LetzteKeuzungVor11Trail", new PointDef(PointType.start,new PointModelImpl(49.417774, 8.054475)));
        points.put("KurzVorKreuzung11TrailMitSC4Track", new PointDef(PointType.on_rt,new PointModelImpl(49.417927, 8.054233)));
        points.put("AufSC4Track", new PointDef(PointType.off_rt,new PointModelImpl(49.418968, 8.053690)));
        points.put("Auf11Trail", new PointDef(PointType.on_rt, new PointModelImpl(49.420500, 8.053964)));
        points.put("SpitzkehreAuf23TrailZumPlateau", new PointDef(PointType.stop,new PointModelImpl(49.421339, 8.052987)));
        tests.put(testDef.getId(),testDef);

        testDeftmp = testDef;

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points = testDef.points;
        points.replace("Auf11Trail",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDeftmp,"DrachensteinPfalzSüdlicheAuffahrtBisEndeTrack",mtb_k1s1);
        points = testDef.points;
        points.remove("SpitzkehreAuf23TrailZumPlateau");
        points.put("EndeSC4Track", new PointDef(PointType.stop,new PointModelImpl(49.420884, 8.052877)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points =testDef.points;
        points.replace("AufSC4Track",PointType.off_rt);
        points.replace("Auf11Trail",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points =testDef.points;
        points.replace("AufSC4Track",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Steinbergsattel2LambertskreuzPfalz",mtb_k1s1);
        points = testDef.points;
        points.put("Steinbergsattel", new PointDef(PointType.start,new PointModelImpl(49.400652, 8.065014)));
        points.put("SteinbergsattelKreuzungTracks", new PointDef(PointType.on_rt,new PointModelImpl(49.400880, 8.065017)));
        points.put("SteinbergsattelKreuzungZumTrail", new PointDef(PointType.off_rt,new PointModelImpl(49.401365, 8.064746)));
        points.put("AufSC2Track", new PointDef(PointType.off_rt,new PointModelImpl(49.406011, 8.072242)));
        points.put("Auf11Trail", new PointDef(PointType.on_rt, new PointModelImpl(49.405181, 8.070475)));
        points.put("Lambertskreuz", new PointDef(PointType.stop,new PointModelImpl(49.410481, 8.067531)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Auf11Trail",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,"Steinbergsattel2LambertskreuzPfalzAbTraileingang",mtb_k1s1);
        points = testDef.points;
        points.remove("Steinbergsattel");
        points.remove("SteinbergsattelKreuzungTracks");
        points.replace("SteinbergsattelKreuzungZumTrail", PointType.start);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("DurchDenBuschHochZurWeißenHohle",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungRichtstattwegSC4Track", new PointDef(PointType.start,new PointModelImpl(49.326791, 8.703686)));
        points.put("SC4Track", new PointDef(PointType.on_rt,new PointModelImpl(49.327098, 8.704955)));
        points.put("SC6Trail", new PointDef(PointType.off_rt,new PointModelImpl(49.326668, 8.706187)));
        points.put("EingangObenWeißenHohle",new PointDef(PointType.stop,new PointModelImpl(49.327408, 8.709161)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("SteilerTrailVonSüdwest2Weissenstein",mtb_k1s1);
        points = testDef.points;
        points.put("KreuzungHochstrMit23Trail", new PointDef(PointType.start,new PointModelImpl(49.452383, 8.725245)));
        points.put("Direkter23Trail", new PointDef(PointType.off_rt,new PointModelImpl(49.452751, 8.724223)));
        points.put("KreuzungSC6MitTurmfahrweg", new PointDef(PointType.on_rt, new PointModelImpl(49.454054, 8.725029)));
        points.put("WeisserSteinTurmSüdseite", new PointDef(PointType.stop,new PointModelImpl(49.453069, 8.723145)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points =testDef.points;
        points.replace("Direkter23Trail",PointType.on_rt);
        points.replace("KreuzungSC6MitTurmfahrweg",PointType.off_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("PfadZurKlammbrückeNordOstMaisbach",mtb_k1s1);
        points = testDef.points;
        points.put("EinstiegIn11Trail", new PointDef(PointType.start,new PointModelImpl(49.329697, 8.723759)));
        points.put("AmerikanerwegSeg1", new PointDef(PointType.off_rt,new PointModelImpl(49.332371, 8.722490)));
        points.put("11TrailSeg1", new PointDef(PointType.on_rt, new PointModelImpl(49.332282, 8.722969)));
        points.put("AmerikanerwegSeg2", new PointDef(PointType.off_rt,new PointModelImpl(49.335003, 8.720473)));
        points.put("TrailSeg2", new PointDef(PointType.off_rt, new PointModelImpl(49.335571, 8.720747)));
        points.put("TrailEndeAufAmerikanerweg", new PointDef(PointType.stop,new PointModelImpl(49.335681, 8.719773)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points =testDef.points;
        points.replace("TrailSeg2",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("VulpiusHütte2StartSternwartenTrail",mtb_k1s1);
        points = testDef.points;
        points.put("VulpiusHütte", new PointDef(PointType.start,new PointModelImpl(49.400748, 8.727478)));
        points.put("flacher00Verbindungstrail", new PointDef(PointType.on_rt,new PointModelImpl(49.399693, 8.727260)));
        points.put("SteilerSC4TrackHoch", new PointDef(PointType.off_rt, new PointModelImpl(49.398791, 8.726228)));
        points.put("11TrailAmZaunParallelZurStrasse", new PointDef(PointType.off_rt,new PointModelImpl(49.397290, 8.726142)));
        points.put("AlterHilsbacherWegParallelZaum", new PointDef(PointType.on_rt, new PointModelImpl(49.397156, 8.726572)));
        points.put("EinstiegTrail", new PointDef(PointType.stop,new PointModelImpl(49.396925, 8.726293)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k3s3);
        points =testDef.points;
        points.replace("flacher00Verbindungstrail",PointType.off_rt);
        points.replace("SteilerSC4TrackHoch",PointType.on_rt);
        points.replace("AlterHilsbacherWegParallelZaum",PointType.off_rt);
        points.replace("11TrailAmZaunParallelZurStrasse",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("HirschhofPassageKarlsruhe",treckingBike);
        points = testDef.points;
        points.put("EingangVonHirschhofstr", new PointDef(PointType.start,new PointModelImpl(49.010468, 8.391818)));//49.010566, 8.391830)));
        points.put("InPassage", new PointDef(PointType.off_rt,new PointModelImpl(49.010504, 8.393243)));
        points.put("Akademiestr", new PointDef(PointType.on_rt, new PointModelImpl(49.010883, 8.392840)));
        points.put("Karlshof", new PointDef(PointType.off_rt,new PointModelImpl(49.010491, 8.394080)));
        points.put("AkademiestrNebenAusgang", new PointDef(PointType.on_rt, new PointModelImpl(49.010821, 8.394232)));//49.010818, 8.394291)));
        points.put("AusgangKarlshof", new PointDef(PointType.stop,new PointModelImpl(49.010807, 8.394554)));//49.010814, 8.394384)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("ProblemHirschhofPassageKarlsruhe",treckingBike);
        points = testDef.points;
        points.put("EingangVonHirschhofstr", new PointDef(PointType.start,new PointModelImpl(49.010566, 8.391830)));
        points.put("Karlshof", new PointDef(PointType.off_rt,new PointModelImpl(49.010491, 8.394080)));
        points.put("AkademiestrNebenAusgang", new PointDef(PointType.on_rt, new PointModelImpl(49.010818, 8.394291)));
        points.put("AusgangKarlshof", new PointDef(PointType.stop,new PointModelImpl(49.010814, 8.394384)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("EingangMarkTwainVomBahndamm2HeinrFuchsEckeR.HellStr",treckingBike);
        points = testDef.points;
        points.put("EingangMarkTwain", new PointDef(PointType.start,new PointModelImpl(49.390375, 8.681948)));
        points.put("alterBahndamm", new PointDef(PointType.on_rt,new PointModelImpl(49.386616, 8.677293)));
        points.put("JohnZengerStr", new PointDef(PointType.off_rt, new PointModelImpl(49.386573, 8.682442)));
        points.put("HeinrFuchsEckeR.HellStr", new PointDef(PointType.stop,new PointModelImpl(49.379489, 8.679325)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("EingangMarkTwainVomBahndamm2HeinrFuchsEckeFrKruckenbergStr",treckingBike);
        points = testDef.points;
        points.put("EingangMarkTwain", new PointDef(PointType.start,new PointModelImpl(49.390375, 8.681948)));
        points.put("alterBahndamm", new PointDef(PointType.off_rt,new PointModelImpl(49.386616, 8.677293)));
        points.put("JohnZengerStr", new PointDef(PointType.on_rt, new PointModelImpl(49.384800, 8.682429)));
        points.put("HeinrFuchsEckeFrKruckenbergStr", new PointDef(PointType.stop,new PointModelImpl(49.379505, 8.680616)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("PestalozziGrabenNeudorf",treckingBike);
        points = testDef.points;
        points.put("EingangPestaVonPestataStr", new PointDef(PointType.start,new PointModelImpl(49.164619, 8.487623)));
        points.put("Pesta2Fichtestr", new PointDef(PointType.on_rt,new PointModelImpl(49.163688, 8.486680)));
        points.put("WegNWumPesta", new PointDef(PointType.off_rt, new PointModelImpl(49.164954, 8.486271)));
        points.put("AusgangFichtestr", new PointDef(PointType.stop,new PointModelImpl(49.164085, 8.485468)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("SteigerwegAbBahnbrückeBisTraubenweg",treckingBike);
        points = testDef.points;
        points.put("EndeBahnbrücke", new PointDef(PointType.start,new PointModelImpl(49.399324, 8.692091)));
        points.put("Steigerweg", new PointDef(PointType.on_rt,new PointModelImpl(49.398812, 8.692400)));
        points.put("Traubenweg", new PointDef(PointType.off_rt, new PointModelImpl(49.399120, 8.692973)));
        points.put("KreuzungTraubenweg", new PointDef(PointType.stop,new PointModelImpl(49.398389, 8.692831)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("FöhrlenbergBeiAnweilerVonBank2Gipfeltrail",mtb_k3s3);
        points = testDef.points;
        points.put("TrailKreuzungVorBank", new PointDef(PointType.start,new PointModelImpl(49.194263, 7.996704)));
        points.put("direkterTrailUp3", new PointDef(PointType.on_rt,new PointModelImpl(49.193479, 7.997895)));
        points.put("Trail23AbBlickAufMünz", new PointDef(PointType.off_rt, new PointModelImpl(49.191273, 7.996810)));
        points.put("AufTrackSOBogen", new PointDef(PointType.off_rt, new PointModelImpl(49.190348, 7.998212)));
        points.put("KreuzungMitTrail23", new PointDef(PointType.stop,new PointModelImpl(49.191367, 7.997256)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points =testDef.points;
        points.replace("direkterTrailUp3",PointType.off_rt);
        points.replace("AufTrackSOBogen",PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        return tests;

    }

}