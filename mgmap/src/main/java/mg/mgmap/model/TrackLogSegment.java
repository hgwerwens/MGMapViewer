/*
 * Copyright 2017 - 2020 mg4gh
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
package mg.mgmap.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Iterator;

import mg.mgmap.util.MetaDataUtil;

/** A TrackLogSegment contains additionally to the points in the MultiPointModel a few  more data.
 * These are
 * <ul>
 *     <li>the segment index</li>
 *     <li>metaData with bounding box infos on segment parts</li>
 *     <li>TrackLogStatistic information of this segment</li>
 * </ul>
 * */
public class TrackLogSegment extends MultiPointModelImpl{

    private TrackLog trackLog;
    private final int segmentIdx;
    private ArrayList<MetaData> metaDatas = new ArrayList<>(); //needed for loadFromBB ?? yes, but also for vizualization of loaded tracks
    private TrackLogStatistic statistic;

    public TrackLogSegment(TrackLog trackLog, int idx){
        this.trackLog = trackLog;
        this.segmentIdx = idx;
        statistic = new TrackLogStatistic(idx);
    }

    public MultiPointModelImpl addPoint(int idx, PointModel pointModel){
        statistic.updateWithPoint(pointModel);
        return super.addPoint(idx, pointModel);
    }

    public PointModel getLastPoint(){
        if (points.size() > 0){
            return points.get(points.size()-1);
        }
        return null;
    }



    @Override
    public int size() {
        verifyAvailability();
        return points.size();
    }

    @Override
    public PointModel get(int i) {
        verifyAvailability();
        return points.get(i);
    }


    @NonNull
    @Override
    public Iterator<PointModel> iterator() {
        verifyAvailability();
        return points.iterator();
    }

    public ArrayList<MetaData> getMetaDatas() {
        return metaDatas;
    }

    public TrackLogStatistic getStatistic() {
        return statistic;
    }

    public void setStatistic(TrackLogStatistic statistic) {
        this.statistic = statistic;
    }

    public int getSegmentIdx() {
        return segmentIdx;
    }

    @Override
    public synchronized BBox getBBox() {
        return bBox;
    }

    public void recalcStatistic(){
        statistic.reset();
        for (PointModel pm : this){
            statistic.updateWithPoint(pm);
        }
    }

    public void verifyAvailability(){
        if (!trackLog.isAvailable()){
            MetaDataUtil.loadLaLoBufs(trackLog);
        }
    }
}