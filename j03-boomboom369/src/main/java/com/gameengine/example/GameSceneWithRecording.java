package com.gameengine.example;

import com.gameengine.components.*;
import com.gameengine.core.GameObject;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameLogic;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.recording.RecordingConfig;
import com.gameengine.recording.RecordingService;
import com.gameengine.scene.Scene;

import java.util.Random;

/**
 * 带有存档回放功能的游戏场景
 */
public class GameSceneWithRecording extends Scene {
    private static final int INITIAL_ENEMY_COUNT = 6;
    private static final int ENEMIES_PER_SPAWN = 2;
    private GameEngine engine;
    private Renderer renderer;
    private InputManager inputManager;
    private Random random;
    private float time;
    private GameLogic gameLogic;
    private float bulletTimer;
    private RecordingService recordingService;
    
    public GameSceneWithRecording(GameEngine engine) {
        super("GameScene");
        this.engine = engine;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.inputManager = InputManager.getInstance();
        this.random = new Random();
        this.time = 0;
        this.bulletTimer = 0;
        this.gameLogic = new GameLogic(this);
        
        // 创建游戏对象
        createPlayer();
        createEnemies();
        createDecorations();
        
        // 启用存档功能
        try {
            String path = "recordings/session_" + System.currentTimeMillis() + ".jsonl";
            RecordingConfig cfg = new RecordingConfig(path);
            recordingService = new RecordingService(cfg);
            recordingService.start(this, 800, 600);
            System.out.println("游戏录制已启动: " + path);
        } catch (Exception e) {
            System.err.println("录制启动失败: " + e.getMessage());
        }
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        time += deltaTime;
        bulletTimer += deltaTime;
        
        // 使用游戏逻辑类处理游戏规则
        gameLogic.handlePlayerInput();
        gameLogic.updatePhysics();
        gameLogic.checkCollisions();
        
        // 生成新敌人
        if (time > 2.0f) {
            spawnEnemyWave(ENEMIES_PER_SPAWN);
            time = 0;
        }

        // 自动发射子弹：每隔1秒发射一次
        if (bulletTimer >= 1.0f) {
            spawnBullet();
            bulletTimer = 0;
        }

        // 清理越界子弹
        for (GameObject obj : getGameObjects()) {
            if (obj.getName().equals("Bullet")) {
                TransformComponent t = obj.getComponent(TransformComponent.class);
                if (t != null) {
                    Vector2 pos = t.getPosition();
                    if (pos.x < -50 || pos.x > 850 || pos.y < -50 || pos.y > 650) {
                        obj.destroy();
                    }
                }
            }
        }

        // 检查玩家HP并在HP<=0时返回菜单
        GameObject player = findFirstGameObjectByName("Player");
        if (player != null) {
            com.gameengine.components.HealthComponent health = player.getComponent(com.gameengine.components.HealthComponent.class);
            if (health != null && health.getHp() <= 0) {
                // 停止录制并返回菜单
                if (recordingService != null) {
                    recordingService.stop();
                    System.out.println("游戏录制已保存");
                }
                engine.setScene(new MenuScene(engine, "MainMenu"));
                return;
            }
        }
        
        // ESC 键返回菜单
        if (inputManager.isKeyJustPressed(27)) {
            if (recordingService != null) {
                recordingService.stop();
                System.out.println("游戏录制已保存");
            }
            engine.setScene(new MenuScene(engine, "MainMenu"));
            return;
        }
        
        // 更新录制服务
        if (recordingService != null && recordingService.isRecording()) {
            recordingService.update(deltaTime, this, inputManager);
        }
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 渲染所有对象
        super.render();

        // 绘制玩家HP条
        GameObject player = findFirstGameObjectByName("Player");
        if (player != null) {
            com.gameengine.components.HealthComponent health = player.getComponent(com.gameengine.components.HealthComponent.class);
            if (health != null) {
                float hpRatio = Math.max(0, Math.min(1, health.getHp() / 5.0f));
                float x = 10, y = 10, w = 200, h = 16;
                renderer.drawRect(x, y, w, h, 0.2f, 0.2f, 0.2f, 1.0f);
                renderer.drawRect(x, y, w * hpRatio, h, 1.0f - hpRatio, hpRatio, 0.0f, 1.0f);
            }
        }
        
        // 显示录制提示
        if (recordingService != null && recordingService.isRecording()) {
            renderer.drawRect(10, 570, 400, 30, 1.0f, 0.0f, 0.0f, 0.3f);
        }
    }
    
    private void createPlayer() {
        // 创建葫芦娃 - 所有部位都在一个GameObject中
        GameObject player = new GameObject("Player") {
            private Vector2 basePosition;
            
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
                
                // 更新所有部位的位置
                updateBodyParts();
            }
            
            @Override
            public void render() {
                // 渲染所有部位
                renderBodyParts();
            }
            
            private void updateBodyParts() {
                TransformComponent transform = getComponent(TransformComponent.class);
                if (transform != null) {
                    basePosition = transform.getPosition();
                }
            }
            
            private void renderBodyParts() {
                if (basePosition == null) return;
                
                // 渲染身体
                renderer.drawRect(
                    basePosition.x - 8, basePosition.y - 10, 16, 20,
                    1.0f, 0.0f, 0.0f, 1.0f  // 红色
                );
                
                // 渲染头部
                renderer.drawRect(
                    basePosition.x - 6, basePosition.y - 22, 12, 12,
                    1.0f, 0.5f, 0.0f, 1.0f  // 橙色
                );
                
                // 渲染左臂
                renderer.drawRect(
                    basePosition.x - 13, basePosition.y - 5, 6, 12,
                    1.0f, 0.8f, 0.0f, 1.0f  // 黄色
                );
                
                // 渲染右臂
                renderer.drawRect(
                    basePosition.x + 7, basePosition.y - 5, 6, 12,
                    0.0f, 1.0f, 0.0f, 1.0f  // 绿色
                );
            }
        };
        
        // 添加变换组件
        player.addComponent(new TransformComponent(new Vector2(400, 300)));
        
        // 添加物理组件
        PhysicsComponent physics = player.addComponent(new PhysicsComponent(1.0f));
        physics.setFriction(0.95f);
        
        // 添加生命值组件（初始HP=5）
        player.addComponent(new com.gameengine.components.HealthComponent(5));
        
        addGameObject(player);
    }
    
    private void createEnemies() {
        spawnEnemyWave(INITIAL_ENEMY_COUNT);
    }
    
    private void spawnEnemyWave(int count) {
        for (int i = 0; i < count; i++) {
            createEnemy();
        }
    }
    
    private void createEnemy() {
        GameObject enemy = new GameObject("Enemy") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                // 追踪玩家
                GameObject targetPlayer = GameSceneWithRecording.this.findFirstGameObjectByName("Player");
                if (targetPlayer != null) {
                    TransformComponent pt = targetPlayer.getComponent(TransformComponent.class);
                    TransformComponent myt = getComponent(TransformComponent.class);
                    PhysicsComponent myp = getComponent(PhysicsComponent.class);
                    if (pt != null && myt != null && myp != null) {
                        Vector2 dir = pt.getPosition().subtract(myt.getPosition());
                        if (dir.magnitude() > 1) {
                            dir = dir.normalize().multiply(60);
                            myp.setVelocity(dir);
                        }
                    }
                }
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        // 随机位置
        Vector2 position = new Vector2(
            random.nextFloat() * 800,
            random.nextFloat() * 600
        );
        
        // 添加变换组件
        enemy.addComponent(new TransformComponent(position));
        
        // 添加渲染组件 - 改为矩形，使用橙色
        RenderComponent render = enemy.addComponent(new RenderComponent(
            RenderComponent.RenderType.RECTANGLE,
            new Vector2(20, 20),
            new RenderComponent.Color(1.0f, 0.5f, 0.0f, 1.0f)  // 橙色
        ));
        render.setRenderer(renderer);
        
        // 添加物理组件
        PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
        physics.setVelocity(new Vector2(
            (random.nextFloat() - 0.5f) * 100,
            (random.nextFloat() - 0.5f) * 100
        ));
        physics.setFriction(0.98f);
        
        addGameObject(enemy);
    }
    
    private void createDecorations() {
        for (int i = 0; i < 5; i++) {
            createDecoration();
        }
    }
    
    private void createDecoration() {
        GameObject decoration = new GameObject("Decoration") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }
            
            @Override
            public void render() {
                renderComponents();
            }
        };
        
        // 随机位置
        Vector2 position = new Vector2(
            random.nextFloat() * 800,
            random.nextFloat() * 600
        );
        
        // 添加变换组件
        decoration.addComponent(new TransformComponent(position));
        
        // 添加渲染组件
        RenderComponent render = decoration.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(5, 5),
            new RenderComponent.Color(0.5f, 0.5f, 1.0f, 0.8f)
        ));
        render.setRenderer(renderer);
        
        addGameObject(decoration);
    }

    private void spawnBullet() {
        // 自动朝上发射子弹
        GameObject player = findFirstGameObjectByName("Player");
        if (player == null) return;
        TransformComponent pt = player.getComponent(TransformComponent.class);
        if (pt == null) return;

        GameObject bullet = new GameObject("Bullet") {
            @Override
            public void update(float deltaTime) {
                super.update(deltaTime);
                updateComponents(deltaTime);
            }

            @Override
            public void render() {
                renderComponents();
            }
        };

        // 朝最近的敌人发射子弹
        Vector2 playerPos = pt.getPosition();
        GameObject closestEnemy = null;
        float closestDist = Float.MAX_VALUE;
        for (GameObject obj : getGameObjects()) {
            if (!"Enemy".equals(obj.getName())) continue;
            TransformComponent et = obj.getComponent(TransformComponent.class);
            if (et == null) continue;
            float d = playerPos.distance(et.getPosition());
            if (d < closestDist) {
                closestDist = d;
                closestEnemy = obj;
            }
        }

        Vector2 fireDir = new Vector2(0, -1);
        // 如果找到最近敌人则指向敌人
        if (closestEnemy != null) {
            TransformComponent et = closestEnemy.getComponent(TransformComponent.class);
            if (et != null) {
                fireDir = et.getPosition().subtract(playerPos);
                if (fireDir.magnitude() > 0.001f) {
                    fireDir = fireDir.normalize();
                } else {
                    fireDir = new Vector2(0, -1);
                }
            }
        } else {
            // 回退：使用玩家移动方向（若存在），否则向上
            PhysicsComponent playerPhys = player.getComponent(PhysicsComponent.class);
            if (playerPhys != null) {
                Vector2 pv = playerPhys.getVelocity();
                if (pv.magnitude() > 0.1f) {
                    fireDir = pv.normalize();
                }
            }
        }

        float bulletSpeed = 300f;
        Vector2 start = playerPos.add(fireDir.multiply(20));
        bullet.addComponent(new TransformComponent(start));
        PhysicsComponent bp = bullet.addComponent(new PhysicsComponent(0.1f));
        bp.setFriction(1.0f);
        bp.setVelocity(fireDir.multiply(bulletSpeed));
        RenderComponent br = bullet.addComponent(new RenderComponent(
            RenderComponent.RenderType.CIRCLE,
            new Vector2(6, 6),
            new RenderComponent.Color(1.0f, 1.0f, 0.0f, 1.0f)
        ));
        br.setRenderer(renderer);
        addGameObject(bullet);
    }
}
