package pepse.world;

import danogl.GameObject;
import danogl.collisions.Collision;
import danogl.gui.ImageReader;
import danogl.gui.UserInputListener;
import danogl.gui.rendering.AnimationRenderable;
import danogl.gui.rendering.OvalRenderable;
import danogl.gui.rendering.Renderable;
import danogl.util.Vector2;
import java.awt.Color;
import java.awt.event.KeyEvent;


public class Avatar  extends GameObject {

    private static final Color AVATAR_COLOR = Color.DARK_GRAY;
    private static final float GRAVITY = 600;
    private static final float VELOCITY_X = 400;
    private static final float VELOCITY_Y = -650;
    public static final float RUN_ENERGY_LOSS = 0.5F;
    public static final int JUMP_ENERGY_LOSS = 11;
    public static final int IDLE_ENERGY_GAIN = 1;
    public static final int MAX_ENERGY = 100;
    public static final int MIN_ENERGY = 0;

    private final UserInputListener inputListener;
    private final ImageReader imageReader;
    private boolean touchingTerrain;


    private float energy;
    Renderable[] idleAnimations;
    Renderable[] runAnimations;
    Renderable[] jumpAnimations;

    AnimationRenderable idleAnimation;
    AnimationRenderable runAnimation;
    AnimationRenderable jumpAnimation;



    public Avatar(Vector2 topLeftCorner,
                  UserInputListener inputListener,
                  ImageReader imageReader) {

        super(topLeftCorner, Vector2.ONES.mult(50), new OvalRenderable(AVATAR_COLOR));
        this.inputListener = inputListener;
        this.imageReader = imageReader;
        physics().preventIntersectionsFromDirection(Vector2.ZERO);
        transform().setAccelerationY(GRAVITY);
        readImages();
        energy = MAX_ENERGY;
        touchingTerrain = false;

    }

    @Override
    public void onCollisionEnter(GameObject other, Collision collision) {
        super.onCollisionEnter(other, collision);
        if(other.getTag().equals(Block.BLOCK_TAG)) {
            this.transform().setVelocityY(MIN_ENERGY);
        }
        if (other.getTag().equals(Terrain.GROUND)){
            this.touchingTerrain = true;
        }
    }

    @Override
    public void onCollisionStay(GameObject other, Collision collision) {
        super.onCollisionStay(other, collision);
        if (other.getTag().equals(Terrain.GROUND)) {
            this.touchingTerrain = true;

        }
    }

    @Override
    public void onCollisionExit(GameObject other) {
        super.onCollisionExit(other);
        if (other.getTag().equals(Terrain.GROUND)) {
            this.touchingTerrain = false;
        }

    }


    private void readImages() {

         idleAnimations = new Renderable[]{
                imageReader.readImage("src/assets/idle_0.png", true),
                imageReader.readImage("src/assets/idle_1.png", true),
                imageReader.readImage("src/assets/idle_2.png", true),
                imageReader.readImage("src/assets/idle_3.png", true)
        };
        runAnimations = new Renderable[]{

                imageReader.readImage("src/assets/run_0.png", true),
                imageReader.readImage("src/assets/run_1.png", true),
                imageReader.readImage("src/assets/run_2.png", true),
                imageReader.readImage("src/assets/run_3.png", true),
                imageReader.readImage("src/assets/run_4.png", true),
                imageReader.readImage("src/assets/run_5.png", true)
        };
        jumpAnimations = new Renderable[]{
                imageReader.readImage("src/assets/jump_0.png", true),
        imageReader.readImage("src/assets/jump_1.png", true),
        imageReader.readImage("src/assets/jump_2.png", true),
        imageReader.readImage("src/assets/jump_3.png", true)
        };

        idleAnimation = new AnimationRenderable(idleAnimations, 1F);
        runAnimation = new AnimationRenderable(runAnimations, 0.5F);
        jumpAnimation = new AnimationRenderable(jumpAnimations, 1F);

    }
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        float xVel = MIN_ENERGY;

        if (inputListener.isKeyPressed(KeyEvent.VK_LEFT) && !inputListener.isKeyPressed(KeyEvent.VK_RIGHT) && energy >= RUN_ENERGY_LOSS) {
            handleRunning(true);
            xVel -= VELOCITY_X;
        } else if (inputListener.isKeyPressed(KeyEvent.VK_RIGHT) && !inputListener.isKeyPressed(KeyEvent.VK_LEFT) && energy >= RUN_ENERGY_LOSS) {
            handleRunning(false);
            xVel += VELOCITY_X;
        } else if (inputListener.isKeyPressed(KeyEvent.VK_SPACE) && touchingTerrain && energy >= JUMP_ENERGY_LOSS && getVelocity().y() == MIN_ENERGY) {
            handleJumping();
        } else {
            handleIdle();
        }
        transform().setVelocityX(xVel);
        System.out.println(energy);
    }


    private void decreaseEnergy(float amount) {
        energy = Math.max(MIN_ENERGY, energy - amount);
    }

    private void increaseEnergy() {
        energy = Math.min(MAX_ENERGY, energy + IDLE_ENERGY_GAIN);
    }

    private void handleRunning(boolean isLeft) {
        float velocityChange = isLeft ? -VELOCITY_X : VELOCITY_X;
        transform().setVelocityX(velocityChange);
        renderer().setIsFlippedHorizontally(isLeft);
        decreaseEnergy(RUN_ENERGY_LOSS);
        updateAvatarRunImage();
    }

    private void handleJumping() {
        transform().setVelocityY(VELOCITY_Y * 0.5F);
        decreaseEnergy(JUMP_ENERGY_LOSS);
        updateAvatarJumpImage();
    }

    private void handleIdle() {
        if (touchingTerrain) {
            increaseEnergy();
        }
        updateAvatarIdleImage();
    }


    private void updateAvatarIdleImage() {
        renderer().setRenderable(idleAnimation);
    }

    private void updateAvatarJumpImage() {
        renderer().setRenderable(jumpAnimation);
    }

    private void updateAvatarRunImage() {
        renderer().setRenderable(runAnimation);
    }

    public void setEnergy(float energy) {
        this.energy = energy;
    }
    public float getEnergy() {
        return energy;
    }

}
