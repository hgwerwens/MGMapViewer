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
package mg.mgmap.generic.graph;

import mg.mgmap.generic.model.PointModelImpl;

/**
 * This class is the basis for a node in a graph.
 */

public class GNode extends PointModelImpl {

    /**
     * This is the entry of a queue of neighbours. The first link refers to the node itself.
     */
    private final GNeighbour neighbour;

    // for Routing algorithms
    private boolean connected = false;
    private GNodeRef nodeRef = null;

    public GNode(double latitude, double longitude, float ele, double cost){
        super(latitude, longitude, ele);
        this.neighbour = new GNeighbour(this, cost);
    }

    public GNeighbour getNeighbour() {
        return neighbour;
    }

    public void addNeighbour(GNeighbour neighbour) {
        getLastNeighbour().setNextNeighbour(neighbour);
    }

    GNeighbour getLastNeighbour(){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
        }
        return nextNeighbour;
    }

    int countNeighbours(){
        int cnt = 0;
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            cnt++;
        }
        return cnt;
    }

    boolean hasNeighbour(GNode oNode){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            nextNeighbour = nextNeighbour.getNextNeighbour();
            if (nextNeighbour.getNeighbourNode() == oNode) return true;
        }
        return false;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public GNodeRef getNodeRef() {
        return nodeRef;
    }

    public void setNodeRef(GNodeRef nodeRef) {
        this.nodeRef = nodeRef;
    }

    public void removeNeighbourNode(GNode neighbourNode){
        GNeighbour nextNeighbour = this.neighbour;
        while (nextNeighbour.getNextNeighbour() != null) {
            GNeighbour lastNeighbour = nextNeighbour;
            nextNeighbour = nextNeighbour.getNextNeighbour();
            if (nextNeighbour.getNeighbourNode() == neighbourNode){
                lastNeighbour.setNextNeighbour(nextNeighbour.getNextNeighbour());
            }
        }
    }


}