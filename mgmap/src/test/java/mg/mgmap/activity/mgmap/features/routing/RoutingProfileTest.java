package mg.mgmap.activity.mgmap.features.routing;

import org.junit.Assert;
import org.junit.Test;
import org.mapsforge.map.datastore.MapDataStore;
import org.mapsforge.map.reader.MapFile;

import java.io.File;
import java.io.PrintWriter;

import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K1S1;
import mg.mgmap.activity.mgmap.features.routing.profile.MTB_K2S2;
import mg.mgmap.activity.mgmap.features.routing.profile.ShortestDistance;
import mg.mgmap.application.util.ElevationProvider;
import mg.mgmap.application.util.ElevationProviderImplHelper;
import mg.mgmap.application.util.ElevationProviderImplHelper2;
import mg.mgmap.application.util.HgtProvider2;
import mg.mgmap.application.util.WayProviderHelper;
import mg.mgmap.generic.graph.impl.GGraphTileFactory;
import mg.mgmap.generic.model.PointModelImpl;
import mg.mgmap.generic.model.WriteableTrackLog;
import mg.mgmap.generic.util.ObservableImpl;
import mg.mgmap.generic.util.Pref;
import mg.mgmap.generic.util.WayProvider;
import mg.mgmap.generic.util.gpx.GpxExporter;

public class RoutingProfileTest {
    @Test
    public void _01_routing() throws Exception {

        RoutingContext interactiveRoutingContext = new RoutingContext(
                10000,
                false, // no extra snap, since FSMarker snaps point zoom level dependent
                10, // accept long detours in interactive mode
                1); // approachLimit 1 is ok, since FSMarker snaps point zoom level dependent

        ElevationProvider elevationProvider = new ElevationProviderImplHelper2( new HgtProvider2() );
        File mapFile = new File("src/test/assets/map_local/Baden-Wuerttemberg_oam.osm.map"); // !!! map is not uploaded to git (due to map size)
        System.out.println(mapFile.getAbsolutePath() + " " + mapFile.exists());

        MapDataStore mds = new MapFile(mapFile, "de");
        WayProvider wayProvider = new WayProviderHelper(mds);
        GGraphTileFactory gGraphTileFactory = new GGraphTileFactory().onCreate(wayProvider, elevationProvider, false, new Pref<>(""), new Pref<>(false));

        RoutingEngine routingEngine = new RoutingEngine(gGraphTileFactory, interactiveRoutingContext, new ObservableImpl());
        routingEngine.setRoutingProfile(new MTB_K2S2());
        routingEngine.refreshRequired.set(0);

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.373489, 8.747295));
            mtl.addPoint(new PointModelImpl(49.379373, 8.723840));

            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            File gpxFile = new File("src/test/assets/temp_local/Bärenbrunnen2DreiEichen_K2S2.gpx");
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            System.out.println(statistic);
            Assert.assertEquals(2610, rotl.getTrackStatistic().getTotalLength(), 5);
            Assert.assertEquals(138, rotl.getTrackStatistic().getNumPoints());
        }

        routingEngine.setRoutingProfile(new MTB_K1S1());
        routingEngine.refreshRequired.set(0);

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            File gpxFile = new File("src/test/assets/temp_local/Bärenbrunnen2DreiEichen_K2S2.gpx");
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            System.out.println(statistic);
            Assert.assertEquals(2610, rotl.getTrackStatistic().getTotalLength(), 5);
            Assert.assertEquals(138, rotl.getTrackStatistic().getNumPoints());
        }

        {
            WriteableTrackLog mtl = new WriteableTrackLog("test_mtl");
            mtl.startTrack(1L);
            mtl.startSegment(2L);
            mtl.addPoint(new PointModelImpl(49.380051, 8.724140));
            mtl.addPoint(new PointModelImpl(49.400871, 8.727354));
            WriteableTrackLog rotl = routingEngine.updateRouting2(mtl, null);
            String statistic = rotl.getTrackStatistic().toString();
            File gpxFile = new File("src/test/assets/temp_local/DreiEichen2Vulpius_K2S2.gpx");
            GpxExporter.export(new PrintWriter(gpxFile), rotl);
            System.out.println(statistic);
            Assert.assertEquals(2690, rotl.getTrackStatistic().getTotalLength(), 5);
            Assert.assertEquals(149, rotl.getTrackStatistic().getNumPoints());
        }
    }


}