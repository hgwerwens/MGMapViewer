package mg.mapviewer.features.tilestore;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.model.Tile;
import org.mapsforge.core.util.IOUtils;
import org.mapsforge.map.layer.queue.Job;
import org.sqlite.database.sqlite.SQLiteDatabase;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import mg.mapviewer.MGMapApplication;
import mg.mapviewer.util.BgJob;
import mg.mapviewer.util.NameUtil;

public class MGTileStoreDB extends MGTileStore {

    static {
        try {
            System.loadLibrary("sqliteX");
        } catch (Throwable t) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(), t);
        }
    }

    private SQLiteDatabase db;
    private String dbName;
    private GraphicFactory graphicFactory;

    public MGTileStoreDB(File storeDir, String dbName, GraphicFactory graphicFactory){
        super(storeDir, null, graphicFactory);
        this.dbName = dbName;
        this.graphicFactory = graphicFactory;

        db = SQLiteDatabase.openDatabase(storeDir.getAbsolutePath()+File.separator+dbName, null, SQLiteDatabase.OPEN_READWRITE);
    }


    @Override
    public synchronized TileBitmap get(Job key) {
        Tile tile = key.tile;
//        long localTileX = tile.tileX;
//        long localTileY = tile.tileY;

        // conversion needed to fit the MbTiles coordinate system
//        final int[] tmsTileXY = googleTile2TmsTile(localTileX, localTileY, tile.zoomLevel);
//        Log.d(MGMapApplication.LABEL,NameUtil.context()+String.format(" Tile requested %d %d is now %d %d", tile.tileX, tile.tileY, tmsTileXY[0], tmsTileXY[1]));
//        byte[] bytes = getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));
        byte[] bytes = getTileAsBytes(tile);

        InputStream inputStream = null;
        try {
            inputStream = new ByteArrayInputStream(bytes);
            TileBitmap bitmap = this.graphicFactory.createTileBitmap(inputStream, tile.tileSize, key.hasAlpha);
            if (bitmap.getWidth() != tile.tileSize || bitmap.getHeight() != tile.tileSize) {
                bitmap.scaleTo(tile.tileSize, tile.tileSize);
            }
            return bitmap;
        } catch (Exception e) {
            return null;
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }



    @Override
    public synchronized void destroy() {
        try {
            db.close();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }

    @Override
    public synchronized boolean containsKey(Job key) {
        Tile tile = key.tile;
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        return containsKey(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));
    }

    private synchronized boolean containsKey(String x, String y, String z) {
        boolean bRes = false;
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_row from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            if (c.moveToFirst()) {
                bRes = true;
            }
            c.close();
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
        return bRes;

    }


        @Override
    public BgJob getLoaderJob(TileStoreLoader tileStoreLoader, Tile tile) {
        return new MGTileStoreLoaderJobDB(tileStoreLoader, tile);
    }

    byte[] getTileAsBytes(Tile tile){
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        return getTileAsBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel));
    }

    /**
     * queries the database for the data of an raster image
     *
     * @param x
     *            the x coordinate
     * @param y
     *            the y coordinate
     * @param z
     *            the z coordinate
     * @return the data, if available for these coordinates
     */
    private synchronized byte[] getTileAsBytes(String x, String y, String z) {
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            if (!c.moveToFirst()) {
                c.close();
                return null;
            }
            byte[] bb = c.getBlob(c.getColumnIndex("tile_data"));
            c.close();
            return bb;
        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
            return null;
        }
    }

    void saveTileBytes(Tile tile, byte[] tileData){
        final int[] tmsTileXY = googleTile2TmsTile(tile.tileX, tile.tileY, tile.zoomLevel);
        saveTileBytes(String.valueOf(tmsTileXY[0]), String.valueOf(tmsTileXY[1]), Byte.toString(tile.zoomLevel), tileData);
    }


    private synchronized void saveTileBytes(String x, String y, String z, byte[] bb){
        try {
            final Cursor c = this.db.rawQuery(
                    "select tile_data from tiles where tile_column=? and tile_row=? and zoom_level=?", new String[] {
                            x, y, z });
            ContentValues cv = new ContentValues();
            cv.put("zoom_level", z);
            cv.put("tile_column", x);
            cv.put("tile_row", y);
            cv.put("tile_data",bb);

            db.insert("tiles", null, cv);

        } catch (Exception e) {
            Log.e(MGMapApplication.LABEL, NameUtil.context(),e);
        }
    }


    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     * <p>
     * Code copied from: http://code.google.com/p/gmap-tile-generator/
     * </p>
     *
     * @param tx
     *            the x tile number.
     * @param ty
     *            the y tile number.
     * @param zoom
     *            the current zoom level.
     * @return the converted values.
     */

    public static int[] googleTile2TmsTile(long tx, long ty, byte zoom) {
        return new int[] { (int) tx, (int) ((Math.pow(2, zoom) - 1) - ty) };
    }

    @Override
    public float getDefaultAlpha() {
        return 1.0f;
    }
}