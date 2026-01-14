package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImpl;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
//import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.impl2.RoutingSummary;
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
    private enum PointType {start,on_rt,off_rt,stop}
    private record PointDef( PointType pointType,PointModelImpl pointModelImpl){
        public PointDef(PointDef ref){
            this(ref.pointType,ref.pointModelImpl);
        }
    }

    private static class Points extends LinkedHashMap<String,PointDef>{
        public void replace(String key, PointType pointType){
            replace(key, new PointDef( pointType,get(key).pointModelImpl));
        }
    }

    private record TestDef( String name, RoutingProfile profile, Points points ){
        public TestDef(TestDef ref, RoutingProfile routingProfile){
            this(ref.name, routingProfile,new Points());
            for (var entry : ref.points.entrySet()){
                this.points.put(entry.getKey(),entry.getValue() == null ? null : new PointDef(entry.getValue()));
            }
        }

        public TestDef(String name, RoutingProfile routingProfile){
            this(name, routingProfile,new Points());
        }
        public String getId(){
            return name+profile.getId().substring(2);
        }
        int getCntType(PointType pointType) {
            int cnt = 0;
            for ( PointDef point : points.values()) {
                if (point!=null && point.pointType == pointType) cnt++;
            }
            return cnt;
        }
        String getPointName(PointType pointType,int pointNumber) {
            int cnt = 1;
            for ( Map.Entry<String, PointDef> pointEntry : points.entrySet()) {
                if (pointEntry.getValue()!=null && pointEntry.getValue().pointType == pointType && cnt++==pointNumber)
                    return pointEntry.getKey();
            }
            return "invalid";
        }

    }

    private record TrackResult(double cost, double length, ArrayList<RoutingSummary> routingSummary){
        public TrackResult{
            if (routingSummary!=null)
              routingSummary = new ArrayList<>(routingSummary);
            else
                routingSummary = new ArrayList<>();
        }
    }

    private record TestResult(double costRatio, double lengthRatio){}
    private static class TestResults extends LinkedHashMap<String,TestResult>{
        public TestResults() {}
        public TestResults(TestResults ref, Comparator<TestResult> cmp) {
            super(ref.size());

            ref.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(cmp))
                    .forEach(e -> put(e.getKey(), e.getValue()));
        }


    }
    private record TestResultDetails(TestResult testResult,TestResults offRouteTestResults ){
        public TestResultDetails(TestResult testResult){
            this(testResult,new TestResults());
        }
    }


    private static class Tests extends LinkedHashMap<String,TestDef>{
        public Tests(Tests ref, String filter){
            for (String key : ref.keySet()){
                if (key.contains(filter))
                    this.put(key,ref.get(key));
            }
        }
        public Tests() {
            super();
        }
    }



    @Test
    public void _01_routing()  {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);

        PointModelUtil.init(32);

        Pref<Boolean> useQubicInterpolation = new Pref<>(true);
        Pref<Boolean> useQubicSplineInterpolation = new Pref<>(true);
        ElevationProvider elevationProvider = new ElevationProviderImpl( new HgtProvider2(), useQubicInterpolation, useQubicSplineInterpolation );
//        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new mg.mgmap.generic.graph.impl2.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAstarExt"), new Pref<>(true), new Pref<>("12"));


        Tests tests = defineTests();
//        tests = new Tests(tests,"BoxbergForstQuelle2MiErlensumpfweg");
        TestResults testResults = new TestResults();
        TestResults offRouteTestResults = new TestResults();

//      Execute Tests
        for ( Map.Entry<String,TestDef> entry : tests.entrySet() ){
            TestDef testDef = entry.getValue();
            TestResultDetails testResult = excuteTest(testDef,gGraphTileFactory);
            testResults.put(entry.getKey(),testResult.testResult);
            offRouteTestResults.putAll(testResult.offRouteTestResults);
        }
//      print Results for intended route to acutal route

        testResults = new TestResults(testResults,Comparator.comparing(TestResult::costRatio));
        offRouteTestResults = new TestResults(offRouteTestResults,Comparator.comparing(TestResult::costRatio));

        double totalCostRatio = 0.0;
        int cnt = 0;
        for ( Map.Entry<String,TestResult> entry : testResults.entrySet() ){
            TestResult testResult = entry.getValue();
            int logLengthR = testResult.lengthRatio == 1.0 ? -16 : (int) Math.log10(testResult.lengthRatio - 1);
            System.out.println("Cost Ratio: " + testResult.costRatio + (logLengthR<=-5?" isOptRoute(" : " noOptRoute(") + logLengthR + ") for: " + entry.getKey());
            totalCostRatio += testResult.costRatio;
            cnt++;
        }
        totalCostRatio = totalCostRatio/cnt;
        System.out.println("Total Cost Ratio: " + totalCostRatio);


//      print Results for not intended route to acutal route
        cnt = 0;
        double totalOffCostRatio = 0;
        for ( Map.Entry<String,TestResult> entry : offRouteTestResults.entrySet() ){
            TestResult offRTR = entry.getValue();
            int logLengthR = offRTR.lengthRatio == 1.0 ? -16 : (int) Math.log10(offRTR.lengthRatio - 1);
            System.out.println("Cost Ratio: " + offRTR.costRatio + (logLengthR<=-5?" isOptRoute(" : " noOptRoute(") + logLengthR + ") for: " +  entry.getKey() );
            totalOffCostRatio += offRTR.costRatio;
            cnt++;
        }

        totalOffCostRatio = totalOffCostRatio/cnt;
        System.out.println("Total Cost Ratio: " + totalOffCostRatio);

    }


    private TestResultDetails excuteTest(TestDef testDef, GGraphTileFactory gGraphTileFactory ){
        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                true, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                32);
        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(testDef.profile);

        System.out.println(testDef.profile.getId());
        System.out.println("on route all points");
        TrackResult ref_cost = executeSingle(testDef,PointType.on_rt,-1,routingEngine);
        System.out.println("route start/stop");
        TrackResult opt_cost = executeSingle(testDef,PointType.start,0,routingEngine);

        double ref2opt = ref_cost.cost / opt_cost.cost;
        double lengthRatio =  ref_cost.length / opt_cost.length;
        System.out.println("Ref2OptCosts: " + ref2opt);
        TestResultDetails testResult = new  TestResultDetails(new TestResult(ref2opt,lengthRatio));


/*        int pointNumber = 0;
        int rt_onCnt = testDef.getCntType(PointType.on_rt);
        System.out.println("on route single point");
        while (++pointNumber <= rt_onCnt && rt_onCnt > 1) {
            executeSingle(testDef,PointType.on_rt,pointNumber,routingEngine);
        } */
        int rt_offCnt = testDef.getCntType(PointType.off_rt);
        System.out.println("off route single point");
        int pointNumber = 0;


        while (++pointNumber <= rt_offCnt ) {
            TrackResult off_cost = executeSingle(testDef,PointType.off_rt,pointNumber,routingEngine);
            double ref2off = ref_cost.cost / off_cost.cost;
            lengthRatio = off_cost.length / opt_cost.length ;
            testResult.offRouteTestResults.put(testDef.getId() + "_" + testDef.getPointName(PointType.off_rt,pointNumber),new TestResult(ref2off,lengthRatio));
            System.out.println("Ref2OffCosts: " + ref2off);
        }
        return testResult;
    }

    private TrackResult executeSingle(TestDef testDef, PointType pointType, int pointNumber, RoutingEngine routingEngine) {
        StringBuilder testName = new StringBuilder(testDef.name);
        if (pointType == PointType.start )
            testName.append("StartStop");
        else if ( pointType == PointType.on_rt && pointNumber < 0 )
            testName.append("AllPointsOnRoute");
        else if ( pointType == PointType.off_rt && pointNumber < 0 )
            testName.append("AllPointsOffRoute");
        else if ( pointType == PointType.on_rt && pointNumber > 0 )
            testName.append("OnRoute");
        else if ( pointType == PointType.off_rt && pointNumber > 0 )
            testName.append("OffRoute");

        long timestamp = 1L;
        WriteableTrackLog mtlb = new WriteableTrackLog("test_mtlb");

        {
            int pointCnt = 0;
            PointDef point;
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
                                testName.append(pointEntry.getKey());
                        }
                    }
            }
        }

        

        routingEngine.refreshRequired.set(0);
        WriteableTrackLog rotl = routingEngine.updateRouting2(mtlb, null);

        double cost = 0.0;
        for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
            cost = cost + routingSummary.getCost();
        }

        TrackLogStatistic trackStatisic = rotl.getTrackStatistic();
        System.out.println(testName + ": " + cost);
        System.out.println(trackStatisic.toString());

        for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
            System.out.println(routingSummary);
        }

        TrackResult trackResult = new TrackResult(cost, trackStatisic.getTotalLength(),RoutingSummary.routingSummaries);

        RoutingSummary.routingSummaries.clear();

        String fileName = "src/test/assets/temp_local/" + testName +
                testDef.profile.getId() +
                ".gpx";
        File gpxFile = new File(fileName);
        try {
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        return trackResult;
    }

    private Tests defineTests(){
        RoutingProfile mtb_k1s1 = new MTB_K1S1();
        RoutingProfile mtb_k2s2 = new MTB_K2S2();

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
        points.put("WeißerSteinschlag2", new PointDef( PointType.off_rt, new PointModelImpl(49.375169, 8.740072)));
        points.put("3Eichen", new PointDef( PointType.stop,new PointModelImpl(49.379322, 8.723820)));
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

        testDef = new TestDef("Bärenbr3Eichen3_11Pf",mtb_k2s2);
        points = testDef.points;
        points.put("WeisserSteinschlagUnten", new PointDef( PointType.start,new PointModelImpl(49.373600, 8.736291)));
        points.put("kurzer11Pf", new PointDef( PointType.on_rt, new PointModelImpl(49.373679, 8.736045)));
        points.put("WeisserSteinschlagOben", new PointDef( PointType.stop,new PointModelImpl(49.373710, 8.735737)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("3EichenVulpius",mtb_k2s2);
        points = testDef.points;
        points.put("3Eichen", new PointDef( PointType.start,new PointModelImpl(49.380051, 8.724140)));
        points.put("1Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.384575, 8.722852)));
        points.put("2Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.387458, 8.722325)));
        points.put("HöhenWegVorLeo", new PointDef( PointType.off_rt, new PointModelImpl(49.390047, 8.721883)));
        points.put("HöhenWegNachLeo", new PointDef( PointType.off_rt, new PointModelImpl(49.395111, 8.724165)));
        points.put("3Seg0", new PointDef( PointType.on_rt, new PointModelImpl(49.389915, 8.721471)));
        points.put("4Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.393342, 8.724873)));
        points.put("6Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.398255, 8.726851)));
        points.put("RoteSuhlWeg", new PointDef( PointType.off_rt, new PointModelImpl(49.398232, 8.728652)));
        points.put("Vulpius", new PointDef( PointType.stop,new PointModelImpl(49.400871, 8.727354)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("3Seg0");
        points.remove("HöhenWegVorLeo");
        points.replace("6Seg11",PointType.off_rt);
        points.replace("RoteSuhlWeg",PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("FalkFlowUpSeg3",mtb_k2s2);
        points = testDef.points;
        points.put("unter1Seg", new PointDef( PointType.start,new PointModelImpl(49.409164, 8.729769)));
        points.put("up2Track", new PointDef( PointType.on_rt, new PointModelImpl(49.408569, 8.728617)));
        points.put("Verbindungsw", null);
        points.put("up2Path", new PointDef( PointType.off_rt, new PointModelImpl(49.408019, 8.729566)));
        points.put("ueber1Seg", new PointDef( PointType.stop,new PointModelImpl(49.406211, 8.729933)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("up2Track");
        points.replace("Verbindungsw", new PointDef( PointType.on_rt, new PointModelImpl(49.408763, 8.726771)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Riesenstein2Rondell",mtb_k2s2);
        points = testDef.points;
        points.put("1Seg0nachKreuzSchotter", new PointDef( PointType.start,new PointModelImpl(49.406862, 8.700686)));
        points.put("2Seg1rechts", new PointDef( PointType.on_rt, new PointModelImpl(49.406325, 8.699979)));
        points.put("2Seg1geradeaus", new PointDef( PointType.off_rt, new PointModelImpl(49.406741, 8.702646)));
        points.put("ueberRondellunten", new PointDef( PointType.stop,new PointModelImpl(49.405756, 8.700372)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("ueberRondellunten");
        points.put("ueberRondelloben", new PointDef( PointType.stop, new PointModelImpl(49.405574, 8.700274))); // ein wenig weiter oben
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

        testDef = new TestDef("obSteigerweg2Sprunghöhe",mtb_k1s1);
        points = testDef.points;
        points.put("obSteigerweg", new PointDef( PointType.start,new PointModelImpl(49.396727, 8.698018)));
        points.put("HutzelwaldH", new PointDef( PointType.on_rt, new PointModelImpl(49.400510, 8.698911)));
        points.put("KreuzungJohannesHoops", new PointDef( PointType.off_rt, new PointModelImpl(49.398437, 8.708084)));
        points.put("Sprunghöhe", new PointDef( PointType.stop,new PointModelImpl(49.391994, 8.722037)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.remove("HutzelwaldH");
        points.replace("KreuzungJohannesHoops", PointType.on_rt);
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("Birkenbank2Gaisberg",mtb_k1s1);
        points = testDef.points;
        points.put("Birkenbank", new PointDef( PointType.start,new PointModelImpl(49.405938, 8.704010)));
        points.put("Sprunghöhe", new PointDef( PointType.on_rt,new PointModelImpl(49.401376, 8.709106)));
        points.put("Geschützstellung", new PointDef( PointType.off_rt, new PointModelImpl(49.405235, 8.703792)));
        points.put("Gaisberg", new PointDef( PointType.stop,new PointModelImpl(49.403221, 8.704465)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("Sprunghöhe", PointType.off_rt);
        points.replace("Geschützstellung", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("WasserspeicherJohannesHoops2Birkenbank",mtb_k1s1);
        points = testDef.points;
        points.put("Wasserspeicher", new PointDef( PointType.start,new PointModelImpl(49.405633, 8.707340)));
        points.put("1Pf11Kreuz", new PointDef( PointType.off_rt,new PointModelImpl(49.404744, 8.707791)));
        points.put("ueberRondell", new PointDef( PointType.on_rt, new PointModelImpl(49.405658, 8.700322)));
        points.put("Birkenbank", new PointDef( PointType.stop,new PointModelImpl(49.405930, 8.704055)));
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

        testDef = new TestDef("OdenwälderHütte2Schlossblick",mtb_k1s1);
        points = testDef.points;
        points.put("OdenwälderHütte", new PointDef( PointType.start,new PointModelImpl(49.419911, 8.710987)));
        points.put("OdenwälderWegvorKührund", new PointDef( PointType.on_rt,new PointModelImpl(49.425223, 8.711807)));
        points.put("OdenwälderWegSerpentine", new PointDef( PointType.on_rt, new PointModelImpl(49.426290, 8.717622)));
        points.put("ZollstockwegUnten1", new PointDef( PointType.off_rt, new PointModelImpl(49.421362, 8.709809)));
        points.put("ZollstockwegOben11", new PointDef( PointType.off_rt, new PointModelImpl(49.427568, 8.712532)));
        points.put("SchlossblickHütte", new PointDef( PointType.stop,new PointModelImpl(49.429134, 8.715338)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("OdenwälderWegvorKührund",  PointType.off_rt);
        points.replace("OdenwälderWegSerpentine", PointType.off_rt);
        points.replace("ZollstockwegUnten1", PointType.on_rt );
        points.replace("ZollstockwegOben11",  PointType.on_rt);
        tests.put(testDef.getId(),testDef);



        testDef = new TestDef("BoxbergForstQuelle2MiErlensumpfweg",mtb_k1s1);
        points = testDef.points;
        points.put("startObererNeuerWeg", new PointDef( PointType.start,new PointModelImpl(49.381263, 8.706486)));
        points.put("ObererNeuerWeg", new PointDef( PointType.on_rt,new PointModelImpl(49.380912, 8.706909)));
        points.put("Rebmannspfad", new PointDef( PointType.off_rt, new PointModelImpl(49.381202, 8.708179)));
        points.put("MiErlensumpfweg", new PointDef( PointType.stop,new PointModelImpl(49.380649, 8.707409)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
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


        testDef = new TestDef("KurveUntSnktNikolausW2LeopoldSt",mtb_k2s2);
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
        points.put("AbzweigKSweg", new PointDef( PointType.off_rt, new PointModelImpl(49.401889, 8.724593)));
        points.put("VulpiusHütte", new PointDef( PointType.off_rt,new PointModelImpl(49.400834, 8.727354)));
        points.put("Falknerei", new PointDef( PointType.stop,new PointModelImpl(49.403808, 8.728194)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
        points = testDef.points;
        points.replace("AbzweigRodelweg", PointType.off_rt);
        points.replace("AbzweigKSweg", PointType.on_rt);
        points.replace("VulpiusHütte", PointType.on_rt);
        tests.put(testDef.getId(),testDef);

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


        testDef = new TestDef("Steigerweg2SteigerwegAmPfadvorbei",mtb_k1s1);
        points = testDef.points;
        points.put("SteigerwegVorTreppe", new PointDef( PointType.start,new PointModelImpl(49.396804, 8.695270)));
        points.put("Pfad23", new PointDef( PointType.off_rt,new PointModelImpl(49.396506, 8.697440)));
        points.put("SteigerwegSerpentine", new PointDef( PointType.on_rt, new PointModelImpl(49.396273, 8.698883)));
        points.put("SteigerwegNachPfad", new PointDef( PointType.stop,new PointModelImpl(49.396267, 8.697167)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k2s2);
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



        return tests;
    }

}