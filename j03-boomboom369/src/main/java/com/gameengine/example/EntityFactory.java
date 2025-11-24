package com.gameengine.example;

import com.gameengine.components.RenderComponent;
import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.math.Vector2;

/**
 * 复用的实体建造工厂，便于游戏与回放共享外观
 */
public final class EntityFactory {
    private EntityFactory() {}

    public static GameObject createPlayerVisual(Renderer renderer) {
        GameObject player = new GameObject("Player") {
            private Vector2 basePosition;

            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform != null) {
                    basePosition = transform.getPosition();
                }
            }

            @Override
            public void render() {
                if (basePosition == null) return;
                renderer.drawRect(basePosition.x - 8, basePosition.y - 10, 16, 20,
                        1.0f, 0.0f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 6, basePosition.y - 22, 12, 12,
                        1.0f, 0.5f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x - 13, basePosition.y - 5, 6, 12,
                        1.0f, 0.8f, 0.0f, 1.0f);
                renderer.drawRect(basePosition.x + 7, basePosition.y - 5, 6, 12,
                        0.0f, 1.0f, 0.0f, 1.0f);
            }
        };
        player.addComponent(new TransformComponent(new Vector2(0, 0)));
        return player;
    }

    public static GameObject createRectangle(Renderer renderer, String name, Vector2 size, RenderComponent.Color color) {
        GameObject obj = new GameObject(name);
        obj.addComponent(new TransformComponent(new Vector2(0, 0)));
        RenderComponent rc = obj.addComponent(new RenderComponent(
                RenderComponent.RenderType.RECTANGLE,
                new Vector2(Math.max(1f, size.x), Math.max(1f, size.y)),
                color
        ));
        rc.setRenderer(renderer);
        return obj;
    }

    public static GameObject createCircle(Renderer renderer, String name, Vector2 size, RenderComponent.Color color) {
        GameObject obj = new GameObject(name);
        obj.addComponent(new TransformComponent(new Vector2(0, 0)));
        RenderComponent rc = obj.addComponent(new RenderComponent(
                RenderComponent.RenderType.CIRCLE,
                new Vector2(Math.max(1f, size.x), Math.max(1f, size.y)),
                color
        ));
        rc.setRenderer(renderer);
        return obj;
    }
}

