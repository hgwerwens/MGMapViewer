package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl2.GGraphTileFactory;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.gpx.GpxExporter;

public class RoutingProfileTest {
    private enum PointType {start,on_rt,off_rt,stop}
    private record PointDef( PointType pointType,PointModelImpl pointModelImpl){}
    private record TestDef( RoutingProfile profile, LinkedHashMap<String,PointDef> points ){
        int getCntType(PointType pointType) {
            int cnt = 0;
            for ( PointDef point : points.values()) {
                if (point.pointType == pointType) cnt++;
            }
            return cnt;
        }
    }

    @Test
    public void _01_routing()  {
        PointModelUtil.init(32);

        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                true, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                32); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new mg.mgmap.generic.graph.impl2.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>("BidirectionalAstarExt"), new Pref<>(false), new Pref<>("16"));

        LinkedHashMap<String,  PointDef> points = new LinkedHashMap<>();
        TestDef testDef;
        points.put("Bärenbr", new PointDef( PointType.start, new PointModelImpl(49.373489, 8.747295)));
        points.put("11untKiefernWald", new PointDef( PointType.on_rt, new PointModelImpl(49.372785, 8.741959)));
        points.put("11obWaldTrail", new PointDef( PointType.on_rt, new PointModelImpl(49.376634, 8.725377)));
        //points.put("WeißerSteinschlag", new PointDef( PointType.off_rt, new PointModelImpl(49.374345, 8.745763)));
        points.put("3Eichen", new PointDef( PointType.stop,new PointModelImpl(49.379301, 8.723810)));
        testDef = new TestDef(new MTB_K2S2(), points);
        excuteTest(testDef,gGraphTileFactory);

        /*points.put("11obWaldTrail", new PointDef( PointType.off_rt, new PointModelImpl(49.376634, 8.725377)));
        testDef = new TestDef(new MTB_K1S1(), points);
        excuteTest(testDef,gGraphTileFactory);


        points.put("3Eichen", new PointDef( PointType.start,new PointModelImpl(49.380051, 8.724140)));
        points.put("Vulpius", new PointDef( PointType.stop,new PointModelImpl(49.400871, 8.727354)));
        testDef = new TestDef(new MTB_K2S2(), points);
        excuteTest(testDef,gGraphTileFactory);
        testDef = new TestDef(new MTB_K1S1(), points);
        excuteTest(testDef,gGraphTileFactory);*/


    }

    private void excuteTest(TestDef testDef, GGraphTileFactory gGraphTileFactory ){
        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                true, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                32);
        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(testDef.profile);


        System.out.println(testDef.profile.getId());

    /*    System.out.println("route start/stop");
        executeSingle(testDef,PointType.start,0,routingEngine); */

        System.out.println("on route all points");
        executeSingle(testDef,PointType.on_rt,-1,routingEngine);
    /*    int pointNumber = 1;
        int rt_onCnt = testDef.getCntType(PointType.on_rt);
        System.out.println("on route single point");
        while (++pointNumber <= rt_onCnt && rt_onCnt > 1) {
            executeSingle(testDef,PointType.on_rt,pointNumber++,routingEngine);
        }
        int rt_offCnt = testDef.getCntType(PointType.off_rt);

        System.out.println("off route single point");
        while (++pointNumber <= rt_offCnt && rt_offCnt > 1) {
            executeSingle(testDef,PointType.on_rt,pointNumber++,routingEngine);
        } */


    }

    private boolean executeSingle(TestDef testDef, PointType pointType, int pointNumber, RoutingEngine routingEngine) {


        StringBuilder testName = new StringBuilder();
        LinkedList<WriteableTrackLog> mtls = new LinkedList<>();
        long timestamp = 1L;
        WriteableTrackLog mtla = new WriteableTrackLog("test_mtla");;
        {
            int pointCnt = 0;
            PointDef point;
            WriteableTrackLog mtl = null;
            for (Map.Entry<String, PointDef> pointEntry : testDef.points.entrySet()) {
                point = pointEntry.getValue();
                if (point.pointType == PointType.start) {
                    mtl = new WriteableTrackLog("test_mtl");
                    mtl.startTrack(timestamp+=10);
                    mtl.startSegment(timestamp+=10);
                    mtl.addPoint(point.pointModelImpl);

                    mtla.startTrack(timestamp+=10);
                    mtla.startSegment(timestamp+=10);
                    mtla.addPoint(point.pointModelImpl);

                    testName.append(pointEntry.getKey());
                } else if (point.pointType == PointType.stop) {
                    mtl.addPoint(point.pointModelImpl);
                    mtls.add(mtl);

                    mtla.addPoint(point.pointModelImpl);
                    mtla.stopSegment(timestamp+=10);
                    mtla.stopTrack(timestamp+=10);

                    testName.append(pointEntry.getKey());
                } else if (point.pointType == pointType) {
                    pointCnt++;
                    if (pointNumber < 0 || (pointNumber > 1 && pointNumber == pointCnt)) {

                        mtl.addPoint(point.pointModelImpl);
                        mtls.add(mtl);
                        mtl = new WriteableTrackLog("test_mtl");
                        mtl.startTrack(timestamp+=10);
                        mtl.startSegment(timestamp+=10);
                        mtl.addPoint(point.pointModelImpl);

                        mtla.addPoint(point.pointModelImpl);
                        mtla.stopSegment(timestamp+=10);
                        mtla.startSegment(timestamp+=10);
                        mtla.addPoint(point.pointModelImpl);

                        testName.append(pointEntry.getKey());
                    }
                }
            }
        }

        
        String fileName = "src/test/assets/temp_local/" + testName +
                testDef.profile.getId() +
                ".gpx";

        System.out.println(testName);


        for (WriteableTrackLog mtl : mtls) {
            routingEngine.refreshRequired.set(0);
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            StringBuilder statistic = new StringBuilder( rotl.getTrackStatistic().toString());
            System.out.println(statistic);
        }

        routingEngine.refreshRequired.set(0);
        System.out.println("all");
        WriteableTrackLog rotl = routingEngine.updateRouting2(mtla, null);
        StringBuilder statistic = new StringBuilder( rotl.getTrackStatistic().toString());
        System.out.println(statistic);


       /* File gpxFile = new File(fileName);
        try {
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } */

        return true;
    }

}