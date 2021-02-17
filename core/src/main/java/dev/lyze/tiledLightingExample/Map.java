package dev.lyze.tiledLightingExample;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.util.ArrayList;
import java.util.Comparator;

public class Map {
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;

    private final TiledMapTileLayer mapLayer;
    private TiledMapTileLayer lightingLayer;

    private Texture lightingTexture;

    public Map(String path) {
        this(new InternalFileHandleResolver(), path);
    }

    public Map(FileHandleResolver resolver, String path) {
        map = new TmxMapLoader(resolver).load(path);
        mapLayer = (TiledMapTileLayer) map.getLayers().get("Map");

        renderer = new OrthogonalTiledMapRenderer(map, 1f / mapLayer.getTileWidth());

        generateLightingLayer();

        lightingLayer.setVisible(false);
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
    }

    private void generateLightingLayer() {
        lightingLayer = new TiledMapTileLayer(mapLayer.getWidth(), mapLayer.getHeight(), mapLayer.getTileWidth(), mapLayer.getTileHeight());
        map.getLayers().add(lightingLayer);

        setupLighting(generateLightingTiles(map.getTileSets().getTileSet("Lighting")));
    }

    private void setupLighting(ArrayList<TiledMapTile> tiles) {
        Pixmap lightingPixmap = new Pixmap(lightingLayer.getWidth(), lightingLayer.getHeight(), Pixmap.Format.RGBA8888);
        lightingPixmap.setFilter(Pixmap.Filter.BiLinear);

        for (int invY = 0; invY <= lightingLayer.getHeight(); invY++) {
            int y = lightingLayer.getHeight() - invY;
            for (int x = 0; x <= lightingLayer.getWidth(); x++) {
                if (mapLayer.getCell(x, y) == null) {
                    System.out.print("  ");
                    continue;
                }

                float average = calculateAverageLightingOfTile(mapLayer, x, y);

                lightingPixmap.setColor(new Color(0, 0, 0, average));
                lightingPixmap.drawPixel(x, y);

                // since the average generates floats from 0.0 to 0.1 in steps of 0.1 (e.g. 0.1XXXX, 0.2XXXX, 0.3XXXX) we can clamp it and figure out the index via that
                int index = (int) (average * 10);

                // create the cell with the appropriately tinted tile
                TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
                cell.setTile(tiles.get(index));
                lightingLayer.setCell(x, y, cell);
            }
        }

        // generate the texture based on the generated pixmap
        lightingTexture = new Texture(lightingPixmap);
        // important part: set it to linear for "smoothness"
        lightingTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
    }

    // calculates lighting average for this one specific tile according to https://gamedev.stackexchange.com/a/126165
    // calculate by how many tiles the current tile is surrounded. coords outside of the map also count as tiles
    private float calculateAverageLightingOfTile(TiledMapTileLayer layer, int x, int y) {
        int averageSum = 0;
        for (int dY = y - 1; dY <= y + 1; dY++) {
            for (int dX = x - 1; dX <= x + 1; dX++) {
                if (dX == x && dY == y) // ignore when it's the current tile
                    continue;

                boolean foundTile = false;
                if (dX < 0 || dY < 0 || dX >= layer.getWidth() || dY >= layer.getHeight())
                    foundTile = true; // out of bounds
                else if (layer.getCell(dX, dY) != null)
                    foundTile = true;

                averageSum += foundTile ? 1 : 0;
            }
        }

        return averageSum / 9f;
    }

    // just creates a ordered list of tiles
    // each tile in the tileset has a "index" property, 0 means it's transparent, 1 means it's black.
    // and since there's 9 different possible combinations, the texture has 9 different tiles
    private ArrayList<TiledMapTile> generateLightingTiles(TiledMapTileSet tileset) {
        ArrayList<TiledMapTile> tiles = new ArrayList<>();
        tileset.iterator().forEachRemaining(tiles::add);
        tiles.sort(Comparator.comparing(o -> o.getProperties().get("index", Integer.class)));
        return tiles;
    }

    public Texture getLightingTexture() {
        return lightingTexture;
    }

    public void toggleLightingLayerVisibility() {
        lightingLayer.setVisible(!lightingLayer.isVisible());
    }

    public boolean isLightingLayerVisible() {
        return lightingLayer.isVisible();
    }

    public int getWidth() {
        return lightingLayer.getWidth();
    }

    public int getHeight() {
        return lightingLayer.getHeight();
    }
}

