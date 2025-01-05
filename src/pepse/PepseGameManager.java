package pepse;

import danogl.GameManager;
import danogl.GameObject;
import danogl.collisions.Layer;
import danogl.components.CoordinateSpace;
import danogl.components.Transition;
import danogl.gui.ImageReader;
import danogl.gui.SoundReader;
import danogl.gui.UserInputListener;
import danogl.gui.WindowController;
import danogl.gui.rendering.Camera;
import danogl.gui.rendering.OvalRenderable;
import danogl.util.Vector2;
import pepse.world.*;
import pepse.world.daynight.Cloud;
import pepse.world.daynight.Night;
import pepse.world.daynight.Sun;
import pepse.world.daynight.SunHalo;
import pepse.world.trees.Flora;
import pepse.world.trees.Fruit;
import pepse.world.trees.TreeInfo;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PepseGameManager extends GameManager {

    private static final int AVATAR_TERRAIN_DIST = 100;
    private static final float GRAVITY = 600f;
    private static final int RAIN_DROP_SIZE = 10;
    private static final Color RAIN_DROP_COLOR = new Color(173, 216, 230);
    private static final float CYCLE_LENGTH = 30;


    private UserInputListener userInputListener;
    private WindowController windowController;
    private Terrain terrain;
    private float currentCameraCenterX;
    private float currentMinX;
    private float currentMaxX;
    private Flora flora;
    private int seed;
    private ArrayList<ArrayList<GameObject>> allClouds;
    private Vector2 windowDimensions;


    @Override
    public void initializeGame(ImageReader imageReader, SoundReader soundReader, UserInputListener inputListener,
                               WindowController windowController) {

        seed = new Random().nextInt();
        this.userInputListener = inputListener;
        this.windowController = windowController;

        super.initializeGame(imageReader, soundReader, inputListener, windowController);
        windowController.setTargetFramerate(50);
        this.windowDimensions = windowController.getWindowDimensions();

        createDayNight();

        terrain = new Terrain(windowDimensions, seed);
        makeBlocks(0, (int) windowDimensions.x());

        flora = new Flora(terrain, windowDimensions, seed);
        makeTrees(0, (int) windowDimensions.x());

        createClouds();

        createAvatar(inputListener, imageReader);

    }

    private void createAvatar(UserInputListener inputListener, ImageReader imageReader) {

        float xInitialAvatarLocation = windowDimensions.mult(0.5f).x();
        float yInitialAvatarLocation = terrain.groundHeightAt(xInitialAvatarLocation) - AVATAR_TERRAIN_DIST;
        Vector2 initialAvatarLocation = new Vector2(xInitialAvatarLocation, yInitialAvatarLocation);

        Avatar avatar = new Avatar(initialAvatarLocation, inputListener, imageReader);
//        avatar.setCloud(allClouds);
        avatar.setRainCallback(() -> createRain(allClouds));

        createDisplayEnergy(avatar);

        gameObjects().addGameObject(avatar);

        this.currentCameraCenterX = avatar.getCenter().x();
        this.currentMinX = 0;
        this.currentMaxX = windowDimensions.x();

        setCamera(new Camera(avatar, Vector2.ZERO,
                windowController.getWindowDimensions(),
                windowController.getWindowDimensions()));

    }

    private void createDisplayEnergy(Avatar avatar) {
        EnergyDisplay energyDisplay = new EnergyDisplay(
                new Vector2(windowDimensions.x() - 300, 100),
                new Vector2(100, 30));
        gameObjects().addGameObject(energyDisplay);
        avatar.setEnergyUpdateCallback(energyDisplay::run);
    }

    private void createClouds() {
        allClouds = new ArrayList<>();
        ArrayList<GameObject> cloud;

        for (int i = 0; i < 6; i++) {
            cloud = Cloud.createCloud(new Vector2(-200 * (i), 100 * (i % 3 + 1)), seed);
            allClouds.add(cloud);
            for (GameObject obj : cloud) {
                gameObjects().addGameObject(obj, Layer.BACKGROUND);
                obj.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);
            }
        }
    }

    private void createDayNight(){

        gameObjects().addGameObject(Sky.create(windowDimensions), Layer.BACKGROUND);

        GameObject night = Night.create(windowDimensions, CYCLE_LENGTH);
        gameObjects().addGameObject(night, Layer.BACKGROUND);

        GameObject sun = Sun.create(windowDimensions, CYCLE_LENGTH);
        gameObjects().addGameObject(sun, Layer.BACKGROUND);

        GameObject sunHalo = SunHalo.create(sun);
        gameObjects().addGameObject(sunHalo, Layer.BACKGROUND);

    }

//    private void addDelayedMovement(GameObject block, float delay) {
//        new ScheduledTask(
//                block,
//                delay,
//                false,
//                () -> rainMovement(block)
//        );
//    }

    private void rainMovement(GameObject rainDrop){
        // Transition for transparency and removal
        new Transition<>(
                rainDrop,
                alpha -> {
                    if (alpha <= 0) {
                        gameObjects().removeGameObject(rainDrop, Layer.UI);
                    } else {
                        rainDrop.renderer().setRenderable(
                                new OvalRenderable(
                                        new Color(RAIN_DROP_COLOR.getRed(), RAIN_DROP_COLOR.getGreen(),
                                                RAIN_DROP_COLOR.getBlue(), (int) (alpha * 255))
                                )
                        );
                    }
                },
                1f, // Starting alpha
                0f, // Ending alpha
                Transition.LINEAR_INTERPOLATOR_FLOAT,
                10f, // Duration of fade-out
                Transition.TransitionType.TRANSITION_ONCE,
                null
        );
    }

    private void createRain(ArrayList<ArrayList<GameObject>> finalCloud) {
        Random rainRand = new Random(seed);
//        float delay = rainRand.nextFloat();
        for (ArrayList<GameObject> arr : finalCloud) {
            for ( GameObject obj : arr) {
                GameObject rainDrop = new GameObject(
                        obj.getTopLeftCorner(),
                        new Vector2(RAIN_DROP_SIZE, RAIN_DROP_SIZE),
                        new OvalRenderable(RAIN_DROP_COLOR)
                );

                //addDelayedMovement(rainDrop, delay);
                rainMovement(rainDrop);
                //delay = rainRand.nextFloat();

                rainDrop.transform().setAccelerationY(GRAVITY);

                rainDrop.setCoordinateSpace(CoordinateSpace.CAMERA_COORDINATES);

                gameObjects().addGameObject(rainDrop, Layer.UI); // Use a layer guaranteed to be visible
            }

        }
    }

    @Override
    public void update(float deltaTime) {

        super.update(deltaTime);

        if (userInputListener.isKeyPressed(KeyEvent.VK_ESCAPE)) windowController.closeWindow();

        if (camera().getCenter().x() != currentCameraCenterX) {

            float space = (currentCameraCenterX - camera().getCenter().x()); //checking how much the camera moved

            if (space != 0) {
                if (space < 0) { //means avatar went right

                    makeBlocks((int) currentMaxX, (int) (currentMaxX - space));

                    makeTrees((int) currentMaxX, (int) (currentMaxX - space));

                    currentMaxX += Block.SIZE;
                }
                else { //means avatar went left

                    makeBlocks((int)(currentMinX - space), (int)currentMinX);

                    makeTrees((int)(currentMinX - space), (int)currentMinX);

                    currentMinX -= Block.SIZE;
                }

            }
            currentCameraCenterX = camera().getCenter().x();
        }

        removeObjects();
    }

    private void removeObjects() {
        for(GameObject gameObject: gameObjects()){ //deleting unnecessary game objects
            if(gameObject.getCenter().x() > currentMaxX | gameObject.getCenter().x() < currentMinX |
                    gameObject.getCenter().y() > this.windowController.getWindowDimensions().y() |
                    (gameObject.getCenter().y() < 0 && !gameObject.getTag().equals(Fruit.FRUIT))) {
                gameObjects().removeGameObject(gameObject);
            }
        }
    }

    private void makeTrees(int min, int max) {

        ArrayList<TreeInfo> trees = flora.createInRange(min, max);

        //TODO : check layers - The fruits should collide with the player, the player should be stopped by the trunk,
        // the leaves should NOT collide with the player

        for (TreeInfo tree : trees) {
            gameObjects().addGameObject(tree.getTree(), Layer.STATIC_OBJECTS);
            for (GameObject leaf : tree.getLeaves()) {
                gameObjects().addGameObject(leaf, Layer.BACKGROUND);
            }
            for (GameObject fruit : tree.getFruits()) {
                gameObjects().addGameObject(fruit, Layer.STATIC_OBJECTS);
            }
        }
    }

    private void makeBlocks(int min, int max) {
        List<Block> blocks = terrain.createInRange(min, max);
        for (Block b : blocks) {
            gameObjects().addGameObject(b, Layer.STATIC_OBJECTS);
        }
    }

    public void run() {
        super.run();
    }

    public static void main(String[] args) {
        new PepseGameManager().run();
    }

}

