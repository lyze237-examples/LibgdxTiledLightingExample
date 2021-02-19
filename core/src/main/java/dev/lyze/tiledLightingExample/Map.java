package dev.lyze.tiledLightingExample;

import com.badlogic.gdx.assets.loaders.FileHandleResolver;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.maps.tiled.*;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;

import java.util.ArrayList;
import java.util.Comparator;

public class Map {
    private final TiledMap map;
    private final OrthogonalTiledMapRenderer renderer;

    private final TiledMapTileLayer mapLayer;
    private final TiledMapTileLayer lightingLayer;
    private final ArrayList<TiledMapTile> lightingTiles;

    private final Texture pixel;
    private final SpriteBatch lightingBatch;
    private final FrameBuffer lightingFrameBuffer;
    private final TextureRegion lightingTexture;

    private final int maxCaveHeight;
    private final int lightingTickSpeed;

    private int currentLightingCoordinate = 0;

    public Map(String path) {
        this(path, 4, 10);
    }

    public Map(String path, int maxCaveHeight, int lightingTickSpeed) {
        this(new InternalFileHandleResolver(), path, maxCaveHeight, lightingTickSpeed);
    }

    public Map(FileHandleResolver resolver, String path, int maxCaveHeight, int lightingTickSpeed) {
        this.maxCaveHeight = maxCaveHeight;
        this.lightingTickSpeed = lightingTickSpeed;

        map = new TmxMapLoader(resolver).load(path);
        mapLayer = (TiledMapTileLayer) map.getLayers().get("Map");

        renderer = new OrthogonalTiledMapRenderer(map, 1f / mapLayer.getTileWidth());

        pixel = generatePixel();
        lightingBatch = new SpriteBatch();
        lightingBatch.getProjectionMatrix().setToOrtho2D(0, 0, mapLayer.getWidth(), mapLayer.getHeight());
        lightingFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, mapLayer.getWidth(), mapLayer.getHeight(), false);
        lightingTexture = new TextureRegion(lightingFrameBuffer.getColorBufferTexture());
        lightingTexture.flip(false, true);

        lightingLayer = new TiledMapTileLayer(mapLayer.getWidth(), mapLayer.getHeight(), mapLayer.getTileWidth(), mapLayer.getTileHeight());
        lightingTiles = generateLightingTiles(map.getTileSets().getTileSet("Lighting"));
        map.getLayers().add(lightingLayer);

        lightingLayer.setVisible(false);
    }

    // might be better to load a texture instead of creating one
    private Texture generatePixel() {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.WHITE);
        pixmap.fill();
        return new Texture(pixmap);
    }

    // generate lighting during every frame for a certain amount of tiles to not overload the gpu. (=> basically "async")
    public void update() {
        lightingFrameBuffer.begin();
        lightingBatch.begin();

        for (int i = 0; i < lightingTickSpeed && currentLightingCoordinate < lightingLayer.getWidth() * lightingLayer.getHeight(); i++, currentLightingCoordinate++) {
            int x = currentLightingCoordinate / lightingLayer.getHeight();
            int invY = currentLightingCoordinate % lightingLayer.getHeight();
            int y = lightingLayer.getHeight() - 1 - invY;

            if (y > maxCaveHeight && mapLayer.getCell(x, y) == null) {
                continue;
            }

            float average = calculateAverageLightingOfTile(mapLayer, x, y);

            lightingBatch.setColor(new Color(0, 0, 0, average));
            lightingBatch.draw(pixel, x, y);

            // since the average generates floats from 0.0 to 0.1 in steps of 0.1 (e.g. 0.1XXXX, 0.2XXXX, 0.3XXXX) we can clamp it and figure out the index via that
            int index = (int) (average * 10);

            // create the cell with the appropriately tinted tile
            TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
            cell.setTile(lightingTiles.get(index));
            lightingLayer.setCell(x, y, cell);
        }

        lightingBatch.end();
        lightingFrameBuffer.end();
    }

    public void render(OrthographicCamera camera) {
        renderer.setView(camera);
        renderer.render();
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
                else if (y <= maxCaveHeight)
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

    public TextureRegion getLightingTexture() {
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

    public void restartLightingGeneration() {
        currentLightingCoordinate = 0;
    }
}

