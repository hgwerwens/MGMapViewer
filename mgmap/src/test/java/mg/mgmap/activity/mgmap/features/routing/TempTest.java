package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.application.util.ElevationProvider;
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

public class TempTest {
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

    private record OffRouteTestResult(double costRatio, boolean isOptRout){}
    private static class OffRouteTestResults extends LinkedHashMap<String,OffRouteTestResult>{  }
    private record TestResult(double costRatio, boolean isOptRout,OffRouteTestResults offRouteCostRatios){
        public TestResult(double costRatio,boolean isOptRout){
            this(costRatio, isOptRout,new OffRouteTestResults());
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
//        GGraphTileFactory gGraphTileFactory = new mg.mgmap.generic.graph.impl.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAstarExt"), new Pref<>(true));


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
            System.out.println("Cost Ratio: " + testResult.costRatio + (testResult.isOptRout? " isOptRoute: ":" for: ") + entry.getKey());
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
                for ( Map.Entry<String,OffRouteTestResult> lineEntry : testResult.offRouteCostRatios.entrySet() ) {
                    System.out.println("Cost Ratio: " + lineEntry.getValue().costRatio + (lineEntry.getValue().isOptRout? " isOptRoute: ":" for: ") +  entry.getKey() + " " + lineEntry.getKey());
                    totalOffCostRatio += lineEntry.getValue().costRatio;
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
//        TrackResult ref_cost = executeSingle(testDef,PointType.on_rt,-1,routingEngine);

        System.out.println("route start/stop");
        TrackResult opt_cost = executeSingle(testDef,PointType.start,0,routingEngine);
        TrackResult ref_cost = opt_cost;
        double ref2opt = ref_cost.cost / opt_cost.cost;
        boolean isOptRoute = ref_cost.length == opt_cost.length;
        System.out.println("Ref2OptCosts: " + ref2opt);
        TestResult testResult = new  TestResult(ref2opt,isOptRoute);

        return testResult;
    }

    private TrackResult executeSingle(TestDef testDef, PointType pointType, int pointNumber, RoutingEngine routingEngine) {
        StringBuilder testName = new StringBuilder(testDef.name);
        if (pointType == PointType.start )
            testName.append("_StartStop");
        else if ( pointType == PointType.on_rt && pointNumber < 0 )
            testName.append("_AllPointsOnRoute");
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

}
