package mg.mgmap.features.routing;

import android.util.Log;

import org.mapsforge.map.datastore.MapDataStore;

import java.util.HashMap;
import java.util.Locale;

import mg.mgmap.MGMapApplication;
import mg.mgmap.graph.ApproachModel;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModel;
import mg.mgmap.model.TrackLog;
import mg.mgmap.model.TrackLogSegment;
import mg.mgmap.util.Assert;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;

public class RouteOptimizer {

    FSRouting fsRouting;
    MapDataStore mapFile;

    public RouteOptimizer(FSRouting fsRouting, MapDataStore mapFile){
        this.fsRouting = fsRouting;
        this.mapFile = mapFile;
    }

    private RoutePointModel getRoutePointModel(PointModel pm){
        return fsRouting.getRoutePointModel( mapFile, pm );
    }

    private void replaceNode(MultiPointModelImpl mpmi, int idx, PointModel pm){
        mpmi.removePoint(idx);
        mpmi.addPoint(idx,pm);
    }

    public boolean checkRoute(TrackLogSegment segment, int startIdx, int endIdx,
                              HashMap<PointModel, ApproachModel> matchingApproachMap){

        matchingApproachMap.clear();

        RoutePointModel rpmSource =  getRoutePointModel(segment.get(startIdx) );
        RoutePointModel rpmTarget =  getRoutePointModel(segment.get(endIdx) );

        MultiPointModelImpl route = fsRouting.calcRouting(mapFile, rpmSource, rpmTarget);
        if (!route.isRoute()) return false;

        Assert.check(rpmSource.getApproachNode() == route.get(0));
        Assert.check(rpmTarget.getApproachNode() == route.get(route.size()-1));

        Log.i(MGMapApplication.LABEL, NameUtil.context());

        if (rpmSource.getApproach().getNode1() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode2());
        if (rpmSource.getApproach().getNode2() == route.get(1)) replaceNode(route, 0, rpmSource.getApproach().getNode1());
        if (rpmTarget.getApproach().getNode1() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode2());
        if (rpmTarget.getApproach().getNode2() == route.get(route.size()-2)) replaceNode(route, route.size()-1, rpmTarget.getApproach().getNode1());





        for (int idx=startIdx+1; idx < endIdx; idx++){
            RoutePointModel rpm = fsRouting.getRoutePointModel( mapFile, segment.get(idx) );

            ApproachModel match = null;

            for (int rIdx = 1; rIdx<route.size(); rIdx++){ //iterate over route parts - rIdx determines endpoint of part
                if (rpm.approachBBox.contains(route.get(rIdx))){ // ok, it might give a route match
                    for (ApproachModel approachModel : rpm.getApproaches()){
                        if (approachModel == match) break; // can't improve
                        if (((approachModel.getNode1() == route.get(rIdx)) && (approachModel.getNode2() == route.get(rIdx-1))) ||
                            ((approachModel.getNode2() == route.get(rIdx)) && (approachModel.getNode1() == route.get(rIdx-1))))   {
                            match = approachModel;
                            break;
                        }
                    }
                }
            }
            if (match == null) return false;
            matchingApproachMap.put(rpm.mtlp, match);
        }
        return true;
    }



    public void optimize(TrackLog trackLog){
        for (int idx=0; idx<trackLog.getNumberOfSegments(); idx++){
            TrackLogSegment segment = trackLog.getTrackLogSegment(idx);
            optimize(segment);
        }
    }

    private class Scorer{
        private final int hops;
        private final int jump;
        private final double max;
        private final HashMap<PointModel, ApproachModel> checkResults = new HashMap<>();

        int next=0;
        private Scorer(int hops, int jump, double max) {
            this.hops = hops;
            this.jump = jump;
            this.max = max;
        }

        private void score(TrackLogSegment segment, int idx){
            if (idx == next){ // do scoring
                Log.i(MGMapApplication.LABEL, NameUtil.context()+" "+hops+" "+idx);
                int end = idx;
                double dist = 0;
                for (int cnt=1; (cnt<=hops) && (idx+cnt<segment.size()); cnt++){
                    double d = getRoutePointModel(segment.get(idx+cnt)).currentDistance;
                    if (dist + d < max) {
                        dist += d;
                        end = idx+cnt;
                    }
                }
                if (end > idx){
                    checkResults.clear();
                    double avgApproachDistance = 0;
                    if (checkRoute(segment, idx, end, checkResults)){
                        for (PointModel pm : checkResults.keySet()){
                            avgApproachDistance += checkResults.get(pm).getApproachNode().getNeighbour().getCost();
                        }
                        avgApproachDistance /= checkResults.keySet().size();

                        double score = 1 - (0.5 * avgApproachDistance / PointModelUtil.getCloseThreshold()); // 0.5 .. 1 score point depending an avgApproachDistance

                        for (int i=idx+1; i<=end; i++){
                            if (!getRoutePointModel(segment.get(i)).currentMPM.isRoute()){
                                score *= 10; // solution for a jumpLine? => set a high score
                            }
                        }

                        for (PointModel pm : checkResults.keySet()){
                            ApproachModel am = checkResults.get(pm);
                            double amScore  = getScore(am);
                            scoreMap.put(am, amScore+score);
                        }
                    }
                }
                next += jump;
            }

        }
    }


    private final HashMap<ApproachModel, Double> scoreMap = new HashMap<>();
    private double getScore(ApproachModel am){
        Double dScore = scoreMap.get(am);
        return (dScore != null)? dScore : 0;
    }

    public void optimize(TrackLogSegment segment){
        Scorer s1 = new Scorer(3,2,500);
        Scorer s2 = new Scorer(7,4,1000);
        Scorer s3 = new Scorer(17,7,2000);

        for (int idx=0; idx<segment.size(); idx++){
            s1.score(segment, idx);
            s2.score(segment, idx);
            s3.score(segment, idx);
            RoutePointModel rpm = getRoutePointModel(segment.get(idx));
            double highScore = 0;
            StringBuilder log = new StringBuilder();
            for (ApproachModel am : rpm.getApproaches()){
                double amScore  = getScore(am);
                log.append(String.format(Locale.ENGLISH, " %.2f", amScore));
                if (amScore > highScore){
                    highScore = amScore;
                    rpm.selectedApproach = am;
                    log.append("+");
                }
            }
            Log.i(MGMapApplication.LABEL, NameUtil.context()+"idx="+idx+" "+segment.get(idx)+ log);
        }
    }

}