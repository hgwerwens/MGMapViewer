package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.activity.mgmap.features.routing.profile.TrekkingBike;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.ApproachModel;
import mg.mgmap.generic.graph.impl.AStar;
import mg.mgmap.generic.graph.impl.ApproachModelImpl;
import mg.mgmap.generic.graph.impl.BidirectionalAStar;
import mg.mgmap.generic.graph.impl.GGraphMulti;
import mg.mgmap.generic.graph.impl.GGraphAlgorithm;
import mg.mgmap.generic.graph.impl.GGraphTile;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.graph.impl.GNode;
import mg.mgmap.generic.model.BBox;
import mg.mgmap.generic.model.MultiPointModel;
import mg.mgmap.generic.model.PointModel;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.PointModelUtil;
import mg.mgmap.generic.model.WriteablePointModelImpl;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.basic.MGLog;
import mg.mgmap.generic.util.gpx.GpxExporter;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RoutingTest {

    @Test
    public void _00_routing() {

//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
//        MGLog.setUnittest(true);

        PointModelUtil.init(32);
        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        WriteablePointModelImpl wpmi1 = new WriteablePointModelImpl(49.392927, 8.707837);
        elevationProvider.setElevation(wpmi1);

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(true));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new MTB_K2S2());
        routingEngine.refreshRequired.set(0);

        {   // einfache Strecke Nahe Spyrer Hof
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);


            mtl.addPoint(new PointModelImpl(49.402665, 8.686337));
            mtl.addPoint(new PointModelImpl(49.392902, 8.707811));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);

        }
    }

    @Test
    public void _01_routing() {

        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new ShortestDistance());
        routingEngine.refreshRequired.set(0);

        {   // einfache Strecke Nahe Spyrer Hof
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.396434,8.708706));
            mtl.addPoint(new PointModelImpl(49.397147,8.705246));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(476.68, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(52, rotl.getTrackStatistic().getNumPoints());
        }
        {   // Problemfall Hollmuth
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.365779,8.794608));
            mtl.addPoint(new PointModelImpl(49.371844,8.795191));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(680.29, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(7, rotl.getTrackStatistic().getNumPoints());
        }
        {   // Problemfall B45 Bammentaler Strasse
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.382485,8.788726));
            mtl.addPoint(new PointModelImpl(49.361620,8.789067));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(2412.19, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(74, rotl.getTrackStatistic().getNumPoints());
        }

        {   // Problemfall Heiligkreuzsteinach
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.485806,8.791403));
            mtl.addPoint(new PointModelImpl(49.485658,8.789728));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            Assert.assertEquals(211.55, rotl.getTrackStatistic().getTotalLength(), 0.01);
            Assert.assertEquals(12, rotl.getTrackStatistic().getNumPoints());
        }
    }

    @Test
    public void _02_routing() {
        PointModelUtil.init(32);
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );

            RoutingProfile rp = new TrekkingBike();
            ArrayList<GGraphTile> gGraphTiles;
            GGraphMulti multi;
            GNode gStart;
            GNode gEnd;
            int tileXStart = 17084;
            int tileYStart = 11160;
            int tileXEnd = 17102;
            int tileYEnd = 11179;

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm1 = performSearch(new BidirectionalAStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm2 = performSearch(new AStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
                Assert.assertEquals(mpm1.get(i),mpm2.get(i));
            }
        }

    }

    @Test
    public void _03_routing() {
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );

            RoutingProfile rp = new TrekkingBike();
            ArrayList<GGraphTile> gGraphTiles;
            GGraphMulti multi;
            GNode gStart;
            GNode gEnd;
            int tileXStart = 17184;
            int tileYStart = 11160;
            int tileXEnd = 17202;
            int tileYEnd = 11179;

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm1 = performSearch(new BidirectionalAStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            gStart = gGraphTileFactory.getGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
            gEnd = gGraphTileFactory.getGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(gStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(gEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);
            MultiPointModel mpm2 = performSearch(new AStar(multi, rp), gStart, gEnd);
            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
                Assert.assertEquals(mpm1.get(i),mpm2.get(i));
            }
        }
    }



    private MultiPointModel performSearch(GGraphAlgorithm routingAlg, GNode gStart, GNode gEnd){
        ArrayList<PointModel> relaxed = new ArrayList<>();
        ApproachModelImpl amStart = new ApproachModelImpl(0,0,gStart,null,null, null, gStart, 0);
        ApproachModelImpl amEnd = new ApproachModelImpl(0,0,gEnd,null,null,null, gEnd, 0);
        MultiPointModel mpm = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
        System.out.println(routingAlg.getResult().replaceAll("\n","    "));
        return mpm;
    }


    @Test
    public void _04_routing() throws Exception{
        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        RoutingContext interactiveRoutingContext = new RoutingContext(
                1000000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper();
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new TrekkingBike());

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.405697,8.679110));
            mtl.addPoint(new PointModelImpl(48.817059,9.060983));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            System.out.println( statistic);
            File gpxFile = new File("src/test/assets/temp_local/test.gpx"); // !!! gpx is not uploaded to git (test result)

            GpxExporter.export(new PrintWriter(gpxFile), rotl);
        }
    }



    @Test
    public void _06_routing_compare_impl_vs_impl2() {

        PointModelUtil.init(32);
        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);


        RoutingProfile rp = new MTB_K2S2();

//        PointModel pmStart = new PointModelImpl(49.402665, 8.686337);

        PointModel pmStart = new PointModelImpl(49.596619, 8.837210);


//        PointModel pmEnd = new PointModelImpl(49.401881, 8.687912);
//        PointModel pmEnd = new PointModelImpl(49.400780, 8.692002);
//        PointModel pmEnd = new PointModelImpl(49.396697, 8.697632);
//        PointModel pmEnd = new PointModelImpl(49.396879, 8.706517);
//        PointModel pmEnd = new PointModelImpl(49.396136, 8.708723);
//        PointModel pmEnd = new PointModelImpl(49.392785, 8.707633);

        PointModel pmEnd = new PointModelImpl(49.461036, 9.036647);

        MultiPointModel mpm1;
        {
            GGraphTileFactory gGraphTileFactory;
            ArrayList<GGraphTile> gGraphTiles;
            GGraphMulti multi;
            ApproachModel amStart;
            ApproachModel amEnd;

            gGraphTileFactory = new mg.mgmap.generic.graph.impl.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(true));
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(pmStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(pmEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);

            amStart = gGraphTileFactory.calcApproach(pmStart, PointModelUtil.getCloseThreshold());
            gGraphTileFactory.connectApproach2Graph(multi, amStart);
            amEnd = gGraphTileFactory.calcApproach(pmEnd, PointModelUtil.getCloseThreshold());
            gGraphTileFactory.connectApproach2Graph(multi, amEnd);
            ArrayList<PointModel> relaxed = new ArrayList<>();
            GGraphAlgorithm routingAlg = new BidirectionalAStar(multi, rp);
            mpm1 = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
            System.out.println(routingAlg.getResult().replaceAll("\n","    "));

            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();
            gGraphTileFactory.disconnectApproach2Graph(multi, amStart);
            gGraphTileFactory.disconnectApproach2Graph(multi, amEnd);
        }

        MultiPointModel mpm2;
        {
            mg.mgmap.generic.graph.impl2.GGraphTileFactory gGraphTileFactory;
            ArrayList<mg.mgmap.generic.graph.impl2.GGraphTile> gGraphTiles;
            mg.mgmap.generic.graph.impl2.GGraphMulti multi;
            ApproachModel amStart;
            ApproachModel amEnd;

            gGraphTileFactory = new mg.mgmap.generic.graph.impl2.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(true));
            gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(pmStart).extend(PointModelUtil.getCloseThreshold()));
            gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(pmEnd).extend(PointModelUtil.getCloseThreshold())));
            multi = new mg.mgmap.generic.graph.impl2.GGraphMulti(gGraphTileFactory, gGraphTiles);

            amStart = gGraphTileFactory.calcApproach(pmStart, PointModelUtil.getCloseThreshold());
            gGraphTileFactory.connectApproach2Graph(multi, amStart);
            amEnd = gGraphTileFactory.calcApproach(pmEnd, PointModelUtil.getCloseThreshold());
            gGraphTileFactory.connectApproach2Graph(multi, amEnd);
            ArrayList<PointModel> relaxed = new ArrayList<>();
            mg.mgmap.generic.graph.impl2.GGraphAlgorithm routingAlg = new mg.mgmap.generic.graph.impl2.BidirectionalAStar(multi, rp);

            mpm2 = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
            System.out.println(routingAlg.getResult().replaceAll("\n","    "));

            multi.finalizeUsage();
            gGraphTileFactory.serviceCache();
            gGraphTileFactory.disconnectApproach2Graph(multi, amStart);
            gGraphTileFactory.disconnectApproach2Graph(multi, amEnd);
        }

        for (int i=0; i<Math.min(mpm1.size(), mpm2.size()); i++){
            System.out.println("F i="+i+ " mpm1="+mpm1.get(i)+ " mpm2="+mpm2.get(i)+"  "+ Objects.equals(mpm1.get(i),mpm2.get(i)));
        }

        for (int i=0; i<Math.min(mpm1.size(), mpm2.size()); i++){
            System.out.println("R i="+i+ " mpm1="+mpm1.get(mpm1.size()-1-i)+ " mpm2="+mpm2.get(mpm2.size()-1-i)+"  "+Objects.equals(mpm1.get(mpm1.size()-1-i),mpm2.get(mpm2.size()-1-i) ));
        }


        Assert.assertEquals(mpm1.size(), mpm2.size());
        for (int i=0; i<mpm1.size(); i++){
//            System.out.println("check mpm idx: "+i);
            Assert.assertEquals(mpm1.get(i).getLat(),mpm2.get(i).getLat(),0);
            Assert.assertEquals(mpm1.get(i).getLon(),mpm2.get(i).getLon(),0);
            Assert.assertEquals(mpm1.get(i).getEle(),mpm2.get(i).getEle(),0.1);
        }
    }

    @Test
    public void _07_routing_compare_impl_vs_impl2() {

        PointModelUtil.init(32);
//        MGLog.logConfig.put("mg.mgmap", MGLog.Level.DEBUG);
//        MGLog.setUnittest(true);

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath()+" "+mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);


        RoutingProfile rp = new MTB_K2S2();
        int tileXStart = 17184;
        int tileYStart = 11160;
        int tileXEnd = 17202;
        int tileYEnd = 11179;


        SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy HH:mm:ss.SSS");
        for (int x=0; x<100; x++){
            System.out.printf(Locale.ENGLISH, "%s x=%03d started%n",sdf.format(new Date()),x );

            PointModel pmStart;
            PointModel pmEnd;

            MultiPointModel mpm1;
            {
                GGraphTileFactory gGraphTileFactory;
                ArrayList<GGraphTile> gGraphTiles;
                GGraphMulti multi;
                ApproachModel amStart;
                ApproachModel amEnd;

                gGraphTileFactory = new mg.mgmap.generic.graph.impl.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(true));
                PointModel sPoint = gGraphTileFactory.loadGGraphTile(tileXStart+x/2, tileYStart+x).getGNodes().get(5);
                PointModel ePoint = gGraphTileFactory.loadGGraphTile(tileXEnd+x/2, tileYEnd+x).getGNodes().get(5);
                pmStart = new PointModelImpl(sPoint.getLat()+0.000011, sPoint.getLon()-0.000022);
                pmEnd = new PointModelImpl(ePoint.getLat()-0.000011, ePoint.getLon()+0.000022);
                System.out.println("pmStart="+pmStart);
                System.out.println("pmEnd=  "+pmEnd);

                gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(pmStart).extend(PointModelUtil.getCloseThreshold()));
                gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(pmEnd).extend(PointModelUtil.getCloseThreshold())));
                multi = new GGraphMulti(gGraphTileFactory, gGraphTiles);

                amStart = gGraphTileFactory.calcApproach(pmStart, PointModelUtil.getCloseThreshold());
                gGraphTileFactory.connectApproach2Graph(multi, amStart);
                amEnd = gGraphTileFactory.calcApproach(pmEnd, PointModelUtil.getCloseThreshold());
                gGraphTileFactory.connectApproach2Graph(multi, amEnd);
                ArrayList<PointModel> relaxed = new ArrayList<>();
                GGraphAlgorithm routingAlg = new BidirectionalAStar(multi, rp);
                mpm1 = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
                System.out.println(routingAlg.getResult().replaceAll("\n","    "));

                multi.finalizeUsage();
                gGraphTileFactory.serviceCache();
                gGraphTileFactory.disconnectApproach2Graph(multi, amStart);
                gGraphTileFactory.disconnectApproach2Graph(multi, amEnd);
            }

            MultiPointModel mpm2;
            {
                mg.mgmap.generic.graph.impl2.GGraphTileFactory gGraphTileFactory;
                ArrayList<mg.mgmap.generic.graph.impl2.GGraphTile> gGraphTiles;
                mg.mgmap.generic.graph.impl2.GGraphMulti multi;
                ApproachModel amStart;
                ApproachModel amEnd;

                gGraphTileFactory = new mg.mgmap.generic.graph.impl2.GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(true));
                gGraphTiles = gGraphTileFactory.getGGraphTileList (new BBox().extend(pmStart).extend(PointModelUtil.getCloseThreshold()));
                gGraphTiles.addAll(gGraphTileFactory.getGGraphTileList(new BBox().extend(pmEnd).extend(PointModelUtil.getCloseThreshold())));
                multi = new mg.mgmap.generic.graph.impl2.GGraphMulti(gGraphTileFactory, gGraphTiles);

                amStart = gGraphTileFactory.calcApproach(pmStart, PointModelUtil.getCloseThreshold());
                gGraphTileFactory.connectApproach2Graph(multi, amStart);
                amEnd = gGraphTileFactory.calcApproach(pmEnd, PointModelUtil.getCloseThreshold());
                gGraphTileFactory.connectApproach2Graph(multi, amEnd);
                ArrayList<PointModel> relaxed = new ArrayList<>();
                mg.mgmap.generic.graph.impl2.GGraphAlgorithm routingAlg = new mg.mgmap.generic.graph.impl2.BidirectionalAStar(multi, rp);

                mpm2 = routingAlg.perform(amStart, amEnd, 100000, new AtomicInteger(),relaxed);
                System.out.println(routingAlg.getResult().replaceAll("\n","    "));

                multi.finalizeUsage();
                gGraphTileFactory.serviceCache();
                gGraphTileFactory.disconnectApproach2Graph(multi, amStart);
                gGraphTileFactory.disconnectApproach2Graph(multi, amEnd);
            }

            Assert.assertEquals(mpm1.size(), mpm2.size());
            for (int i=0; i<mpm1.size(); i++){
//                System.out.println("check mpm idx: "+i);
                Assert.assertEquals(mpm1.get(i).getLat(),mpm2.get(i).getLat(),0);
                Assert.assertEquals(mpm1.get(i).getLon(),mpm2.get(i).getLon(),0);
                Assert.assertEquals(mpm1.get(i).getEle(),mpm2.get(i).getEle(),0.1);
            }


        }





    }


}
