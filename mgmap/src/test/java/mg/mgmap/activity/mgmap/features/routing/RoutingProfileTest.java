package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
import mg.mgmap.generic.graph.impl2.RoutingSummary;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
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

    private record TestResult(double costRatio,LinkedHashMap<String,Double> offRouteCostRatios){
        public TestResult(double costRatio){
            this(costRatio,new LinkedHashMap<>());
        }
    }

    private static class Tests extends LinkedHashMap<String,TestDef>{  }
    private static class TestResults extends LinkedHashMap<String,TestResult>{  }


    @Test
    public void _01_routing()  {
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.VERBOSE);
//        MGLog.setUnittest(true);

        PointModelUtil.init(32);


        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new mg.mgmap.generic.graph.impl2.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAstarExt"), new Pref<>(true), new Pref<>("16"));


        Tests tests = defineTests();
        TestResults testResults = new TestResults();

//      Execute Tests
        for ( Map.Entry<String,TestDef> entry : tests.entrySet() ){
            TestDef testDef = entry.getValue();
            TestResult testResult = excuteTest(testDef,gGraphTileFactory);
            testResults.put(entry.getKey(),testResult);
        }
//      print Results for intended route to acutal route
        double totalCostRatio = 0.0;
        int cnt = 0;
        for ( Map.Entry<String,TestResult> entry : testResults.entrySet() ){
            TestResult testResult = entry.getValue();
            System.out.println("Cost Ratio: " + testResult.costRatio + " for " + entry.getKey());
            totalCostRatio += testResult.costRatio;
            cnt++;
        }
        totalCostRatio = totalCostRatio/cnt;
        System.out.println("Total Cost Ratio: " + totalCostRatio);


//      print Results for not intended route to acutal route
        cnt = 0;
        double totalOffCostRatio = 0;
        for ( Map.Entry<String,TestResult> entry : testResults.entrySet() ){
            TestResult testResult = entry.getValue();
            if (testResult.offRouteCostRatios != null)
                for ( Map.Entry<String,Double> lineEntry : testResult.offRouteCostRatios.entrySet() ) {
                    System.out.println("Cost Ratio: " + lineEntry.getValue() + " for " + entry.getKey() + " " + lineEntry.getKey());
                    totalOffCostRatio += lineEntry.getValue();
                    cnt++;
                }
        }
        totalOffCostRatio = totalOffCostRatio/cnt;
        System.out.println("Total Cost Ratio: " + totalOffCostRatio);

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
        points.put("kurzerPf", new PointDef( PointType.on_rt, new PointModelImpl(49.372153, 8.742763)));
        points.put("Anfang00untKiefernWald", new PointDef( PointType.stop,new PointModelImpl(49.372128, 8.742574)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef("Bärenbr3Eichen3_11Pf",mtb_k2s2);
        points = testDef.points;
        points.put("WeisserSteinschlagUnten", new PointDef( PointType.start,new PointModelImpl(49.373600, 8.736291)));
        points.put("kurzer11Pf", new PointDef( PointType.on_rt, new PointModelImpl(49.373681, 8.736048)));
        points.put("WeisserSteinschlagOben", new PointDef( PointType.stop,new PointModelImpl(49.373710, 8.735737)));
        tests.put(testDef.getId(),testDef);


        testDef = new TestDef("3EichenVulpius",mtb_k2s2);
        points = testDef.points;
        points.put("3Eichen", new PointDef( PointType.start,new PointModelImpl(49.380051, 8.724140)));
        points.put("1Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.384575, 8.722852)));
        points.put("2Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.387458, 8.722325)));
        points.put("3Seg0", new PointDef( PointType.on_rt, new PointModelImpl(49.389915, 8.721471)));
        points.put("4Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.393342, 8.724873)));
        points.put("6Seg11", new PointDef( PointType.on_rt, new PointModelImpl(49.398255, 8.726851)));
        points.put("Vulpius", new PointDef( PointType.stop,new PointModelImpl(49.400871, 8.727354)));
        tests.put(testDef.getId(),testDef);

        testDef = new TestDef(testDef,mtb_k1s1);
        points = testDef.points;
        points.remove("3Seg0");points.remove("6Seg11");
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
        testDef = new TestDef("obSteigerweg2Sprunghöhe",mtb_k2s2, points);
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

        return tests;
    }
    private TestResult excuteTest(TestDef testDef, GGraphTileFactory gGraphTileFactory ){
        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                true, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                32);
        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(testDef.profile);

        System.out.println(testDef.profile.getId());
        System.out.println("on route all points");
        double ref_cost = executeSingle(testDef,PointType.on_rt,-1,routingEngine);
        System.out.println("route start/stop");
        double opt_cost = executeSingle(testDef,PointType.start,0,routingEngine);

        double ref2opt = ref_cost / opt_cost;
        System.out.println("Ref2OptCosts: " + ref2opt);
        TestResult testResult = new  TestResult(ref2opt);


/*        int pointNumber = 0;
        int rt_onCnt = testDef.getCntType(PointType.on_rt);
        System.out.println("on route single point");
        while (++pointNumber <= rt_onCnt && rt_onCnt > 1) {
            executeSingle(testDef,PointType.on_rt,pointNumber,routingEngine);
        } */
        int rt_offCnt = testDef.getCntType(PointType.off_rt);
        System.out.println("off route single point");
        int pointNumber = 0;
        double off_cost;

        while (++pointNumber <= rt_offCnt ) {
            off_cost = executeSingle(testDef,PointType.off_rt,pointNumber,routingEngine);
            double ref2off = ref_cost / off_cost;
            testResult.offRouteCostRatios.put(testDef.getPointName(PointType.off_rt,pointNumber),ref2off);
            System.out.println("Ref2OffCosts: " + ref2off);
        }
        return testResult;
    }

    private double executeSingle(TestDef testDef, PointType pointType, int pointNumber, RoutingEngine routingEngine) {
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
        System.out.println(testName + ": " + cost);

        System.out.println(rotl.getTrackStatistic().toString());

        for (RoutingSummary routingSummary : RoutingSummary.routingSummaries){
            System.out.println(routingSummary);
        }

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

        return cost;
    }

}