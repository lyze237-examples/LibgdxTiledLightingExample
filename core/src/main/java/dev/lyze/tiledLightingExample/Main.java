package dev.lyze.tiledLightingExample;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL30;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Map map;

    private Viewport gameViewport;

    private Stage ui;

    @Override
    public void create() {
        VisUI.load();
        this.batch = new SpriteBatch();

        map = new Map("Map.tmx");

        this.gameViewport = new ExtendViewport(map.getWidth(), map.getHeight());
        this.ui = new Stage(new ExtendViewport(1280, 720));

        initUi();
    }

    // setup cool button to toggle mode
    private void initUi() {
        Gdx.input.setInputProcessor(ui);

        VisTable root = new VisTable();
        root.setFillParent(true);
        VisTextButton changeModeButton = new VisTextButton("Change Lighting to Tilemap Layer");
        changeModeButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                changeModeButton.setText("Change Lighting to " + (map.isLightingLayerVisible() ? "Tilemap Layer" : "Smooth texture"));
                map.toggleLightingLayerVisibility();
            }
        });
        root.add(changeModeButton).top().left().padTop(8).padLeft(8).expand();

        ui.addActor(root);
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0.706f * 0.25f, 0.851f * 0.25f, 0.847f * 0.25f, 1);
        Gdx.gl.glClear(GL30.GL_COLOR_BUFFER_BIT);

        // render map
        gameViewport.apply();
        map.update();
        map.render((OrthographicCamera) gameViewport.getCamera());

        // render pixmap/texture
        if (!map.isLightingLayerVisible()) {
            gameViewport.apply();
            batch.setProjectionMatrix(gameViewport.getCamera().combined);
            batch.begin();
            batch.draw(map.getLightingTexture(), 0, 0, map.getWidth(), map.getHeight());
            batch.end();
        }

        // render cool button
        ui.getViewport().apply();
        ui.act();
        ui.draw();
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        ui.getViewport().update(width, height, true);
    }
}