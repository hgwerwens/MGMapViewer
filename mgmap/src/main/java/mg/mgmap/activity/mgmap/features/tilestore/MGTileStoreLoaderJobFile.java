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
package mg.mgmap.activity.mgmap.features.tilestore;

import org.mapsforge.core.model.Tile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import mg.mgmap.application.util.PersistenceManager;

public class MGTileStoreLoaderJobFile extends MGTileStoreLoaderJob{

    public MGTileStoreLoaderJobFile(TileStoreLoader tileStoreLoader, Tile tile){
        super(tileStoreLoader,tile);
    }

    @Override
    protected void doJobNow() throws Exception {
        PersistenceManager pm = tileStoreLoader.application.getPersistenceManager();
        File zoomDir = pm.createIfNotExists(tileStoreLoader.storeDir,Byte.toString(tile.zoomLevel));
        File xDir = pm.createIfNotExists(zoomDir,Integer.toString(tile.tileX));
        File yFile = new File(xDir,Integer.toString(tile.tileY)+".png");

        conn = tileStoreLoader.xmlTileSource.getURLConnection(tile.zoomLevel, tile.tileX, tile.tileY);
        debug = conn.getURL() + " "+conn.getRequestProperties();
        InputStream is = conn.getInputStream();
        OutputStream os = new FileOutputStream(yFile);

        byte[] b = new byte[2048];
        int length;

        while ((length = is.read(b)) != -1) {
            os.write(b, 0, length);
        }

        is.close();
        os.close();


    }


}
