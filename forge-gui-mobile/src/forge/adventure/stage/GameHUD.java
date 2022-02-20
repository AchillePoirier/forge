package forge.adventure.stage;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad;
import com.badlogic.gdx.scenes.scene2d.ui.Touchpad.TouchpadStyle;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import forge.Forge;
import forge.adventure.player.AdventurePlayer;
import forge.adventure.scene.Scene;
import forge.adventure.scene.SceneType;
import forge.adventure.util.Config;
import forge.adventure.util.Current;
import forge.adventure.util.UIActor;
import forge.adventure.world.WorldSave;
import forge.gui.GuiBase;

/**
 * Stage to handle everything rendered in the HUD
 */
public class GameHUD extends Stage {

    static public GameHUD instance;
    private final GameStage gameStage;
    private final Image avatar;
    private final Image miniMapPlayer;
    private final Label lifePoints;
    private final Label money;
    private Image miniMap;
    private UIActor ui;
    private Touchpad touchpad;
    private TouchpadStyle touchpadStyle;
    private Skin touchpadSkin;
    private Drawable touchBackground;
    private Drawable touchKnob;

    private GameHUD(GameStage gameStage) {
        super(new FitViewport(Scene.GetIntendedWidth(), Scene.GetIntendedHeight()), gameStage.getBatch());
        instance = this;
        this.gameStage = gameStage;

        ui = new UIActor(Config.instance().getFile(GuiBase.isAndroid() ? "ui/hud_mobile.json" : "ui/hud.json"));
        miniMap = ui.findActor("map");


        miniMapPlayer = new Image(new Texture(Config.instance().getFile("ui/minimap_player.png")));

        touchpadSkin = new Skin();
        touchpadSkin.add("touchBackground", new Texture(Config.instance().getFile("ui/touchBackground.png")));
        touchpadSkin.add("touchKnob", new Texture(Config.instance().getFile("ui/touchKnob.png")));
        touchpadStyle = new TouchpadStyle();
        touchBackground = touchpadSkin.getDrawable("touchBackground");
        touchKnob = touchpadSkin.getDrawable("touchKnob");
        touchpadStyle.background = touchBackground;
        touchpadStyle.knob = touchKnob;
        touchpadStyle.knob.setMinWidth(34);
        touchpadStyle.knob.setMinHeight(34);
        touchpad = new Touchpad(10, touchpadStyle);
        touchpad.setBounds(15, 15, 65, 65);
        touchpad.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                if (MapStage.getInstance().isInMap()) {
                    MapStage.getInstance().GetPlayer().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    MapStage.getInstance().GetPlayer().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
                } else {
                    WorldStage.getInstance().GetPlayer().getMovementDirection().x+=((Touchpad) actor).getKnobPercentX();
                    WorldStage.getInstance().GetPlayer().getMovementDirection().y+=((Touchpad) actor).getKnobPercentY();
                }
                if (!((Touchpad) actor).isTouched()) {
                    MapStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
                    WorldStage.getInstance().GetPlayer().setMovementDirection(Vector2.Zero);
                }
            }
        });
        if (GuiBase.isAndroid()) //add touchpad
            ui.addActor(touchpad);

        avatar = ui.findActor("avatar");
        ui.onButtonPress("menu", new Runnable() {
            @Override
            public void run() {
                GameHUD.this.menu();
            }
        });
        ui.onButtonPress("statistic", new Runnable() {
            @Override
            public void run() {
                Forge.switchScene(SceneType.PlayerStatisticScene.instance);
            }
        });
        ui.onButtonPress("deck", new Runnable() {
            @Override
            public void run() {
                GameHUD.this.openDeck();
            }
        });
        lifePoints = ui.findActor("lifePoints");
        lifePoints.setText("20/20");
        AdventurePlayer.current().onLifeChange(new Runnable() {
            @Override
            public void run() {
                lifePoints.setText(AdventurePlayer.current().getLife() + "/" + AdventurePlayer.current().getMaxLife());
            }
        });
        money = ui.findActor("money");
        WorldSave.getCurrentSave().getPlayer().onGoldChange(new Runnable() {
            @Override
            public void run() {
                money.setText(String.valueOf(AdventurePlayer.current().getGold()));
            }
        }) ;
        miniMap = ui.findActor("map");

        addActor(ui);
        addActor(miniMapPlayer);
        WorldSave.getCurrentSave().onLoad(new Runnable() {
            @Override
            public void run() {
                GameHUD.this.enter();
            }
        });
    }

    private void statistic() {
        Forge.switchScene(SceneType.PlayerStatisticScene.instance);
    }

    public static GameHUD getInstance() {
        return instance == null ? instance = new GameHUD(WorldStage.getInstance()) : instance;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        Vector2 c=new Vector2();
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();
        float mMapX = ui.findActor("map").getX();
        float mMapY = ui.findActor("map").getY();
        float mMapT = ui.findActor("map").getTop();
        float mMapR = ui.findActor("map").getRight();
        //map bounds
        if (c.x>=mMapX&&c.x<=mMapR&&c.y>=mMapY&&c.y<=mMapT) {
            if (MapStage.getInstance().isInMap())
                return true;
            WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        return super.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button)
    {
        return setPosition(screenX, screenY, pointer, button);
    }

    boolean setPosition(int screenX, int screenY, int pointer, int button) {
        Vector2 c=new Vector2();
        Vector2 c2=new Vector2();
        screenToStageCoordinates(c.set(screenX, screenY));

        float x=(c.x-miniMap.getX())/miniMap.getWidth();
        float y=(c.y-miniMap.getY())/miniMap.getHeight();

        float deckX = ui.findActor("deck").getX();
        float deckY = ui.findActor("deck").getY();
        float deckR = ui.findActor("deck").getRight();
        float deckT = ui.findActor("deck").getTop();
        //deck button bounds
        if (c.x>=deckX&&c.x<=deckR&&c.y>=deckY&&c.y<=deckT) {
            openDeck();
            stageToScreenCoordinates(c2.set(deckX, deckY));
            return super.touchDown((int)c2.x, (int)c2.y, pointer, button);
        }

        float menuX = ui.findActor("menu").getX();
        float menuY = ui.findActor("menu").getY();
        float menuR = ui.findActor("menu").getRight();
        float menuT = ui.findActor("menu").getTop();
        //menu button bounds
        if (c.x>=menuX&&c.x<=menuR&&c.y>=menuY&&c.y<=menuT) {
            menu();
            stageToScreenCoordinates(c2.set(menuX, menuY));
            return super.touchDown((int)c2.x, (int)c2.y, pointer, button);
        }

        float statsX = ui.findActor("statistic").getX();
        float statsY = ui.findActor("statistic").getY();
        float statsR = ui.findActor("statistic").getRight();
        float statsT = ui.findActor("statistic").getTop();
        //stats button bounds
        if (c.x>=statsX&&c.x<=statsR&&c.y>=statsY&&c.y<=statsT) {
            statistic();
            stageToScreenCoordinates(c2.set(statsX, statsY));
            return super.touchDown((int)c2.x, (int)c2.y, pointer, button);
        }

        float uiX = ui.findActor("gamehud").getX();
        float uiY = ui.findActor("gamehud").getY();
        float uiTop = ui.findActor("gamehud").getTop();
        float uiRight = ui.findActor("gamehud").getRight();
        //gamehud bounds
        if (c.x>=uiX&&c.x<=uiRight&&c.y>=uiY&&c.y<=uiTop) {
            return true;
        }

        float mMapX = ui.findActor("map").getX();
        float mMapY = ui.findActor("map").getY();
        float mMapT = ui.findActor("map").getTop();
        float mMapR = ui.findActor("map").getRight();
        //map bounds
        if (c.x>=mMapX&&c.x<=mMapR&&c.y>=mMapY&&c.y<=mMapT) {
            if (MapStage.getInstance().isInMap())
                return true;
            WorldStage.getInstance().GetPlayer().setPosition(x*WorldSave.getCurrentSave().getWorld().getWidthInPixels(),y*WorldSave.getCurrentSave().getWorld().getHeightInPixels());
            return true;
        }
        return super.touchDown(screenX, screenY, pointer, button);
    }
    @Override
    public void draw() {

        int yPos = (int) gameStage.player.getY();
        int xPos = (int) gameStage.player.getX();
        act(Gdx.graphics.getDeltaTime()); //act the Hud
        super.draw(); //draw the Hud
        int xPosMini = (int) (((float) xPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getWidthInTiles()) * miniMap.getWidth());
        int yPosMini = (int) (((float) yPos / (float) WorldSave.getCurrentSave().getWorld().getTileSize() / (float) WorldSave.getCurrentSave().getWorld().getHeightInTiles()) * miniMap.getHeight());
        miniMapPlayer.setPosition(miniMap.getX() + xPosMini - miniMapPlayer.getWidth()/2, miniMap.getY() + yPosMini -  miniMapPlayer.getHeight()/2);
    }

    Texture miniMapTexture;
    public void enter() {

        if(miniMapTexture!=null)
            miniMapTexture.dispose();
        miniMapTexture=new Texture(WorldSave.getCurrentSave().getWorld().getBiomeImage());

        miniMap.setDrawable(new TextureRegionDrawable(miniMapTexture));
        avatar.setDrawable(new TextureRegionDrawable(Current.player().avatar()));


    }

    private Object openDeck() {

        Forge.switchScene(SceneType.DeckSelectScene.instance);
        return null;
    }

    private Object menu() {
        gameStage.openMenu();
        return null;
    }
}
