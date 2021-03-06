package com.fanboy.game.manager.physics;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.fanboy.entity.ServerEntity;
import com.fanboy.renderer.world.WorldRenderer;

import static com.fanboy.game.manager.physics.CollisionGroup.ALL;
import static com.fanboy.game.manager.physics.CollisionGroup.NONE;

public class Body {
    public final Rectangle rectangle;
    private final ServerEntity entity;

    private BodyType bodyType = BodyType.Static;
    public float restitutionX = 0;
    public float restitutionY = 0;
    private boolean onGround = false;
    public boolean toDestroy = false;

    public CollisionGroup collisionGroup = ALL;
    private final Vector2 velocity = new Vector2(0, 0);
    private final Vector2 temp1 = new Vector2();
    private final Vector2 temp2 = new Vector2();
    private final Vector2 temp4 = new Vector2();
    private final Vector2 temp5 = new Vector2();
    private World world;
    private float gravityScale = 1f;
    public float xDamping = 0;

    public Body(ServerEntity entity, Rectangle rectangle) {
        this.entity = entity;
        this.rectangle = rectangle;
    }

    public Body(ServerEntity entity, World world, float x, float y, float width, float height, BodyType type) {
        this(entity, new Rectangle(x, y, width, height));
        this.world = world;
        this.bodyType = type;
    }

    public Vector2 getPosition() {
        return rectangle.getCenter(new Vector2());
    }

    public Vector2 getVelocity() {
        return new Vector2(velocity);
    }

    public void setVelocity(float x, float y) {
        velocity.set(x, y);
    }

    public void setVelocity(Vector2 velocity) {
        this.velocity.set(velocity);
    }

    public void setTransform(Vector2 position) {
        rectangle.setCenter(position);
    }

    public void setGravityScale(float gravityScale) {
        this.gravityScale = gravityScale;
    }

    public ServerEntity getUserData() {
        return entity;
    }

    public void update(float delta) {
        if (toDestroy) {
            return;
        }
        velocity.x -= (xDamping * delta * velocity.x);

        onGround = false;
        calculateVelocity(delta);

        float offsetXTranslation = getOffsetXTranslationIfCrossedViewport();
        rectangle.x += offsetXTranslation;
        // Update position
        rectangle.getPosition(temp1);
        temp1.add(temp2);
        rectangle.getPosition(temp2);
        rectangle.getPosition(temp4);
        temp2.sub(temp1);

        if (Math.abs(velocity.y) > 5) {
            rectangle.setPosition(rectangle.x, temp1.y);
            world.getBodies().stream()
                    .filter(body -> !shouldNotCollideWith(body))
                    .filter(body -> body.rectangle.overlaps(rectangle))
                    .forEach(body -> solveVerticalCollision(body, temp1));
        }
        rectangle.setPosition(temp1.x, rectangle.y);
        world.getBodies().stream()
                .filter(body -> !shouldNotCollideWith(body))
                .filter(body -> body.rectangle.overlaps(rectangle))
                .forEach(body -> solveHorizontalCollision(body, temp1));

        if (Math.abs(velocity.y) <= 5) {
            rectangle.setPosition(rectangle.x, temp1.y);
            world.getBodies().stream()
                    .filter(body -> !shouldNotCollideWith(body))
                    .filter(body -> body.rectangle.overlaps(rectangle))
                    .forEach(body -> solveVerticalCollision(body, temp1));
        }
        rectangle.x -= offsetXTranslation;
    }

    private float getOffsetXTranslationIfCrossedViewport() {
        if (rectangle.x + rectangle.width > WorldRenderer.VIEWPORT_WIDTH && velocity.x > 0) {
            return -WorldRenderer.VIEWPORT_WIDTH;
        }
        if (rectangle.x < 0 && velocity.x < 0) {
            return WorldRenderer.VIEWPORT_WIDTH;
        }
        return 0;
    }

    private boolean shouldNotCollideWith(Body body) {
        return toDestroy
                || body == this
                || body.toDestroy
                || shouldNotCollideByGroup(body)
                || body.collisionGroup == NONE
                || collisionGroup == NONE && !body.isStatic();
    }

    private boolean shouldNotCollideByGroup(Body body) {
        return body.collisionGroup != ALL && (body.collisionGroup == collisionGroup);
    }

    private void calculateVelocity(float delta) {
        temp2.set(world.getGravity());
        temp2.scl(gravityScale);
        temp2.scl(delta);
        velocity.add(temp2);
        temp2.set(velocity);
        temp2.scl(delta);
    }

    public void applyLinearImpulse(float x, float y) {
        temp1.set(velocity);
        velocity.set(temp1.x + x, temp1.y + y);
    }

    private void solveHorizontalCollision(Body body, Vector2 temp1) {
        if (velocity.x < 0) {
            float x = body.rectangle.x + body.rectangle.width + 0.01f;
            temp1.x = Math.abs(temp4.x - x) < Math.abs(temp4.x - temp2.x) ? x : temp4.x;
            velocity.x *= -restitutionX;
            CollisionProcessor.touchLeftAndRight(this, body);
        } else if (velocity.x > 0) {
            float x = body.rectangle.x - rectangle.width - 0.01f;
            temp1.x = Math.abs(temp4.x - x) < Math.abs(temp4.x - temp2.x) ? x : temp4.x;
            velocity.x *= -restitutionX;
            CollisionProcessor.touchLeftAndRight(this, body);
        }
        rectangle.setPosition(temp1.x, rectangle.y);
    }

    private void solveVerticalCollision(Body body, Vector2 position) {
        temp5.set(position);
        if (velocity.y > 0) {
            float y = body.rectangle.y - rectangle.height - 0.01f;
            position.y = Math.abs(temp4.y - y) < Math.abs(temp4.y - temp2.y) ? y : temp4.y;
            velocity.y *= -restitutionY;
            CollisionProcessor.jumpedOn(this, body);
        } else if (velocity.y <= 0) {
            float y = body.rectangle.y + body.rectangle.height + 0.01f;
            position.y = Math.abs(temp4.y - y) < Math.abs(temp4.y - temp2.y) ? y : temp4.y;
            velocity.y *= -restitutionY;
            onGround = body.bodyType == BodyType.Static;
            CollisionProcessor.jumpOn(this, body);
        }
        rectangle.setPosition(rectangle.x, position.y);
    }

    public boolean isStatic() {
        return bodyType == BodyType.Static;
    }

    public boolean isDynamic() {
        return bodyType == BodyType.Dynamic;
    }

    public boolean isOnGround() {
        return onGround;
    }

    public void setBodyType(BodyType bodyType) {
        this.bodyType = bodyType;
    }
}
