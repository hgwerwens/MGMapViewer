/*
 * Copyright 2017 - 2021 mg4gh
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mg.mgmap.graph;

import android.util.Log;

import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.MercatorProjection;

import org.mapsforge.map.datastore.Way;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import mg.mgmap.MGMapApplication;
import mg.mgmap.model.BBox;
import mg.mgmap.model.MultiPointModelImpl;
import mg.mgmap.model.PointModelImpl;
import mg.mgmap.util.LaLo;
import mg.mgmap.util.NameUtil;
import mg.mgmap.util.PointModelUtil;
import mg.mgmap.util.WayProvider;

public class GGraphTileFactory {

    private final int CACHE_LIMIT = 1000;
    private final byte ZOOM_LEVEL = 15;
    private final int TILE_SIZE = 256;

    private static long getKey(int tileX,int tileY){
        return ((long) tileX <<32) + tileY;
    }

    private WayProvider wayProvider = null;
    private LinkedHashMap<Long, GGraphTile> cache = null;

    public GGraphTileFactory(){}

    public GGraphTileFactory onCreate(WayProvider wayProvider){
        this.wayProvider = wayProvider;
        cache = new LinkedHashMap <Long, GGraphTile>(100, 0.6f, true) {
            @Override
            protected boolean removeEldestEntry(Entry<Long, GGraphTile> eldest) {
                boolean bRes = (size() > CACHE_LIMIT);
                if (bRes){
                    GGraphTile old = eldest.getValue();
                    Log.d(MGMapApplication.LABEL, NameUtil.context() + " remove from cache: tile x=" + old.tile.tileX + " y=" + old.tile.tileY + " Cache Size:" + cache.size());
                }
                return bRes;
            }
        };
        return this;
    }

    public void onDestroy(){
        wayProvider = null;
        cache = null;
    }


    public ArrayList<GGraphTile> getGGraphTileList(BBox bBox){
        ArrayList<GGraphTile> tileList = new ArrayList<>();
        try {
            long mapSize = MercatorProjection.getMapSize(ZOOM_LEVEL, TILE_SIZE);
            int tileXMin = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.minLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileXMax = MercatorProjection.pixelXToTileX( MercatorProjection.longitudeToPixelX( bBox.maxLongitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);
            int tileYMin = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.maxLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE); // min and max reversed for tiles
            int tileYMax = MercatorProjection.pixelYToTileY( MercatorProjection.latitudeToPixelY( bBox.minLatitude , mapSize) , ZOOM_LEVEL, TILE_SIZE);

            int totalTiles = (tileXMax-tileXMin+1) * (tileYMax-tileYMin+1);
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" create GGraphTileList with tileXMin="+tileXMin+" tileYMin="+tileYMin+" and tileXMax="+tileXMax+" tileYMax="+tileYMax+
                    " - total tiles="+ totalTiles);
            if (totalTiles < CACHE_LIMIT){
                for (int tileX = tileXMin; tileX <= tileXMax; tileX++) {
                    for (int tileY = tileYMin; tileY <= tileYMax; tileY++) {
                        GGraphTile graph = getGGraphTile(tileX,tileY);
                        tileList.add(graph);
                    }
                }
            } else {
                Log.e(MGMapApplication.LABEL, NameUtil.context()+" totalTiles exceeds CACHE_LIMIT: totalTiles="+ totalTiles+" CACHE_LIMIT="+ CACHE_LIMIT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tileList;
    }

    public ApproachModel validateApproachModel(ApproachModel am){
        GGraphTile gGraphTile = getGGraphTile(am.getTileX(), am.getTileY());
        am.setNode1( gGraphTile.getNode(am.getNode1().getLat(),am.getNode1().getLon()) );
        am.setNode2( gGraphTile.getNode(am.getNode2().getLat(),am.getNode2().getLon()) );
        if (am.getNode1() == null) Log.e(MGMapApplication.LABEL, NameUtil.context()+" node1==null ->rework failed");
        if (am.getNode2() == null) Log.e(MGMapApplication.LABEL, NameUtil.context()+" node2==null ->rework failed");
        return am;
    }


    private GGraphTile getGGraphTile(int tileX, int tileY){
        long key = getKey(tileX,tileY);

        GGraphTile gGraphTile = cache.get(key);
        if (gGraphTile == null){
            Log.d(MGMapApplication.LABEL, NameUtil.context()+" Load tileX="+tileX+" tileY="+tileY+" ("+cache.size()+")");
            Tile tile = new Tile(tileX, tileY, ZOOM_LEVEL, TILE_SIZE);
            gGraphTile = new GGraphTile(tile);
            for (Way way : wayProvider.getWays(tile)) {
                if (wayProvider.isHighway(way)){

                    gGraphTile.addLatLongs( way.latLongs[0]);

                    // now setup rawWays
                    MultiPointModelImpl mpm = new MultiPointModelImpl();
                    for (LatLong latLong : way.latLongs[0] ){
                        // for points inside the tile use the GNodes as already allocated
                        // for points outside use extra Objects, don't pollute the graph with them
                        if (gGraphTile.tbBox.contains(latLong.latitude, latLong.longitude)){
                            mpm.addPoint(gGraphTile.getAddNode(latLong.latitude, latLong.longitude));
                        } else {
                            mpm.addPoint(new PointModelImpl(latLong));
                        }
                    }
                    gGraphTile.getRawWays().add(mpm);
                }
            }
            int latThreshold = LaLo.d2md( PointModelUtil.latitudeDistance(GGraph.CONNECT_THRESHOLD_METER) );
            int lonThreshold = LaLo.d2md( PointModelUtil.longitudeDistance(GGraph.CONNECT_THRESHOLD_METER, tile.getBoundingBox().getCenterPoint().getLatitude()) );
//            Log.v(MGMapApplication.LABEL, NameUtil.context()+" latThreshold="+latThreshold+" lonThreshold="+lonThreshold);
            //all highwas are in the map ... try to correct data ...
            ArrayList<GNode> nodes = gGraphTile.getNodes();
            for (int iIdx=0; iIdx<nodes.size(); iIdx++){
                GNode iNode = nodes.get(iIdx);
                int iNeighbours = iNode.countNeighbours();
                for (int nIdx=iIdx+1; nIdx<nodes.size(); nIdx++ ) {
                    GNode nNode = nodes.get(nIdx);
                    if (iNode.laMdDiff(nNode) >= latThreshold) break; // go to next iIdx
                    if (iNode.loMdDiff(nNode) >= lonThreshold)
                        continue; // goto next mIdx
                    if (PointModelUtil.distance(iNode, nNode) > GGraph.CONNECT_THRESHOLD_METER)
                        continue;
                    if (iNode.hasNeighbour(nNode))
                        continue; // is already neighbour

//This doesn't work well for routing hints
//                    graph.addSegment(iNode, nNode);

//And this didn't work too - removes the resulting point from tile clip process
//                  // Try to simplify the graph by removing node nNode
                    // iterate over al neighbours from nNode
//                    GNeighbour nextNeighbour = nNode.getNeighbour();
//                    while (nextNeighbour.getNextNeighbour() != null) {
//                        nextNeighbour = nextNeighbour.getNextNeighbour();
//                        // remove nNode as a Neighbour
//                        nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
//                        graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
//                    }

//And this is still not good: (Hollmuth,Heiligkreuzsteinach) there are 2 neighbours at one end ... and one on the other ... and this doesn't work
//                    if (nNode.countNeighbours() != 1) continue; // connect only end points
//                    // Third solution approach: connect only point with exactly 1 neighbour
//                    // Therefore this shouldn't be a Problem for routing hints, since both connected points have now 2 neighbours - so they are no routing points
//                    graph.addSegment(iNode, nNode);

                    int nNeighbours = nNode.countNeighbours();
                    if ((iNeighbours == 1) && (nNeighbours == 1)) { // 1:1 connect -> no routing hint problem
                        gGraphTile.addSegment(iNode, nNode);
                        continue;
                    }
                    if (isBorderPoint(gGraphTile.tbBox, nNode) || isBorderPoint(gGraphTile.tbBox, iNode)) { // border points must be kept for MultiTiles; accept potential routing hint problem
                        gGraphTile.addSegment(iNode, nNode);
                        continue;
                    }
                    if ((iNeighbours == 2) && (nNeighbours == 1)) { // 2:1 connect -> might give routing hint problem
                        reduceGraph(gGraphTile, iNode, nNode);  // drop nNode; move neighbour form nNode to iNode
                        continue;
                    }
                    if ((iNeighbours == 1) && (nNeighbours == 2)) { // 1:2 connect -> might give routing hint problem
                        reduceGraph(gGraphTile, nNode, iNode); // drop iNode; move neighbour form iNode to nNode
                        continue;
                    }
                    // else (n:m) accept routing hint issue
                    gGraphTile.addSegment(iNode, nNode);

                }
            }
            cache.put(key,gGraphTile);
        }
        return gGraphTile;
    }

    private boolean isBorderPoint(BBox bBox, GNode node){
        return ((node.getLat() == bBox.maxLatitude) || (node.getLat() == bBox.minLatitude) ||
                (node.getLon() == bBox.maxLongitude) || (node.getLon() == bBox.minLongitude));
    }

    // reduce Graph by dropping nNode, all Neighbours form nNode will get iNode as a Neighbour
    private void reduceGraph(GGraphTile graph, GNode iNode, GNode nNode){
        // iterate over al neighbours from nNode
        GNeighbour nextNeighbour = nNode.getNeighbour();
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            // remove nNode as a Neighbour
            nextNeighbour.getNeighbourNode().removeNeighbourNode(nNode);
            graph.addSegment(iNode, nextNeighbour.getNeighbourNode());
        }
        graph.getNodes().remove(nNode);
    }

}