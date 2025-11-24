package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GameLogic {
    private Scene scene;
    private InputManager inputManager;
    private ExecutorService physicsExecutor;
    private ExecutorService collisionExecutor;
    
    // 性能监测
    private long physicsUpdateTime = 0;
    private long collisionCheckTime = 0;
    private int frameCount = 0;
    
    public GameLogic(Scene scene) {
        this.scene = scene;
        this.inputManager = InputManager.getInstance();
        
        // ===== 线程池初始化 (并行优化) =====
        // 创建两个线程池：一个用于物理计算，一个用于碰撞检测
        int threadCount = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
        this.physicsExecutor = Executors.newFixedThreadPool(threadCount);
        this.collisionExecutor = Executors.newFixedThreadPool(Math.max(2, threadCount / 2));
        // ===== 线程池初始化结束 =====
    }
    
    // 游戏结束时清理线程池资源
    public void cleanup() {
        if (physicsExecutor != null && !physicsExecutor.isShutdown()) {
            physicsExecutor.shutdown();
            try {
                if (!physicsExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    physicsExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                physicsExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        if (collisionExecutor != null && !collisionExecutor.isShutdown()) {
            collisionExecutor.shutdown();
            try {
                if (!collisionExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    collisionExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                collisionExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    // 获取性能指标
    public long getPhysicsUpdateTime() {
        return physicsUpdateTime;
    }
    
    public long getCollisionCheckTime() {
        return collisionCheckTime;
    }
    
    public int getFrameCount() {
        return frameCount;
    }
    
    public void incrementFrameCount() {
        frameCount++;
    }
    
    public void handlePlayerInput() {
        GameObject player = scene.findFirstGameObjectByName("Player");
        if (player == null) {
            return;
        }
        TransformComponent transform = player.getComponent(TransformComponent.class);
        PhysicsComponent physics = player.getComponent(PhysicsComponent.class);
        
        if (transform == null || physics == null) return;
        
        Vector2 movement = new Vector2();
        
        if (inputManager.isKeyPressed(87) || inputManager.isKeyPressed(38)) { // W或上箭头
            movement.y -= 1;
        }
        if (inputManager.isKeyPressed(83) || inputManager.isKeyPressed(40)) { // S或下箭头
            movement.y += 1;
        }
        if (inputManager.isKeyPressed(65) || inputManager.isKeyPressed(37)) { // A或左箭头
            movement.x -= 1;
        }
        if (inputManager.isKeyPressed(68) || inputManager.isKeyPressed(39)) { // D或右箭头
            movement.x += 1;
        }
        
        if (movement.magnitude() > 0) {
            movement = movement.normalize().multiply(200);
            physics.setVelocity(movement);
        }
        
        // 边界检查
        Vector2 pos = transform.getPosition();
        if (pos.x < 0) pos.x = 0;
        if (pos.y < 0) pos.y = 0;
        if (pos.x > 800 - 20) pos.x = 800 - 20;
        if (pos.y > 600 - 20) pos.y = 600 - 20;
        transform.setPosition(pos);
    }
    
    public void updatePhysics() {
        long startTime = System.nanoTime();
        
        List<PhysicsComponent> physicsComponents = scene.getComponents(PhysicsComponent.class);
        if (physicsComponents.isEmpty()) {
            return;
        }
        
        // ===== 并行物理计算 (多线程) =====
        int threadCount = Runtime.getRuntime().availableProcessors() - 1;
        threadCount = Math.max(2, threadCount);
        int batchSize = Math.max(1, physicsComponents.size() / threadCount + 1);
        
        List<Future<?>> futures = new ArrayList<>();
        
        // 将物理组件按批次分配给不同线程处理
        for (int i = 0; i < physicsComponents.size(); i += batchSize) {
            final int start = i;
            final int end = Math.min(i + batchSize, physicsComponents.size());
            
            // 当对象较少时，直接处理避免线程开销
            if (physicsComponents.size() < 10) {
                for (int j = start; j < end; j++) {
                    updateSinglePhysics(physicsComponents.get(j));
                }
            } else {
                // 对象较多时使用线程池
                Future<?> future = physicsExecutor.submit(() -> {
                    for (int j = start; j < end; j++) {
                        updateSinglePhysics(physicsComponents.get(j));
                    }
                });
                futures.add(future);
            }
        }
        
        // 等待所有线程完成
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // ===== 并行物理计算结束 =====
        
        physicsUpdateTime = System.nanoTime() - startTime;
    }
    
    private void updateSinglePhysics(PhysicsComponent physics) {
        TransformComponent transform = physics.getOwner().getComponent(TransformComponent.class);
        if (transform != null) {
            Vector2 pos = transform.getPosition();
            Vector2 velocity = physics.getVelocity();

            // 如果是子弹，跳过反弹逻辑
            if (physics.getOwner() != null && "Bullet".equals(physics.getOwner().getName())) {
                transform.setPosition(pos);
                return;
            }

            if (pos.x <= 0 || pos.x >= 800 - 15) {
                velocity.x = -velocity.x;
                physics.setVelocity(velocity);
            }
            if (pos.y <= 0 || pos.y >= 600 - 15) {
                velocity.y = -velocity.y;
                physics.setVelocity(velocity);
            }

            // 确保在边界内
            if (pos.x < 0) pos.x = 0;
            if (pos.y < 0) pos.y = 0;
            if (pos.x > 800 - 15) pos.x = 800 - 15;
            if (pos.y > 600 - 15) pos.y = 600 - 15;
            transform.setPosition(pos);
        }
    }
    

    public void checkCollisions() {
        long startTime = System.nanoTime();
        
        // 直接查找玩家对象
        GameObject player = scene.findFirstGameObjectByName("Player");
        if (player == null) {
            return;
        }
        TransformComponent playerTransform = player.getComponent(TransformComponent.class);
        if (playerTransform == null) return;
        
        // 获取所有子弹和敌人
        List<GameObject> bullets = new ArrayList<>();
        List<GameObject> enemies = new ArrayList<>();
        
        for (GameObject obj : scene.getGameObjects()) {
            if (obj.getName().equals("Bullet")) {
                bullets.add(obj);
            } else if (obj.getName().equals("Enemy")) {
                enemies.add(obj);
            }
        }
        
        // ===== 并行子弹与敌人碰撞检测 =====
        if (!bullets.isEmpty() && !enemies.isEmpty()) {
            checkBulletEnemyCollisionsParallel(bullets, enemies);
        }
        // ===== 并行敌人与玩家碰撞检测 =====
        checkPlayerEnemyCollisionsParallel(playerTransform, player, enemies);
        // ===== 碰撞检测结束 =====
        
        collisionCheckTime = System.nanoTime() - startTime;
    }

    private void checkBulletEnemyCollisionsParallel(List<GameObject> bullets, List<GameObject> enemies) {
        if (bullets.size() < 5) {
            // 对象较少时使用串行处理
            checkBulletEnemyCollisionsSerial(bullets, enemies);
        } else {
            // 对象较多时使用并行处理
            int threadCount = Runtime.getRuntime().availableProcessors() - 1;
            threadCount = Math.max(1, threadCount);
            int batchSize = Math.max(1, bullets.size() / threadCount + 1);
            
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < bullets.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, bullets.size());
                
                Future<?> future = collisionExecutor.submit(() -> {
                    for (int j = start; j < end; j++) {
                        checkBulletEnemyCollision(bullets.get(j), enemies);
                    }
                });
                futures.add(future);
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void checkBulletEnemyCollisionsSerial(List<GameObject> bullets, List<GameObject> enemies) {
        for (GameObject bullet : bullets) {
            checkBulletEnemyCollision(bullet, enemies);
        }
    }
    
    private void checkBulletEnemyCollision(GameObject bullet, List<GameObject> enemies) {
        TransformComponent bulletTransform = bullet.getComponent(TransformComponent.class);
        if (bulletTransform == null || !bullet.isActive()) return;
        
        for (GameObject enemy : enemies) {
            if (!enemy.isActive()) continue;
            
            TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
            if (enemyTransform == null) continue;
            
            float d = bulletTransform.getPosition().distance(enemyTransform.getPosition());
            if (d < 20) {
                bullet.destroy();
                enemy.destroy();
                break;
            }
        }
    }
    

    private void checkPlayerEnemyCollisionsParallel(TransformComponent playerTransform, GameObject player, List<GameObject> enemies) {
        if (enemies.size() < 10) {
            // 对象较少时使用串行处理
            checkPlayerEnemyCollisionsSerial(playerTransform, player, enemies);
        } else {
            // 对象较多时使用并行处理
            int threadCount = Runtime.getRuntime().availableProcessors() - 1;
            threadCount = Math.max(1, threadCount);
            int batchSize = Math.max(1, enemies.size() / threadCount + 1);
            
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < enemies.size(); i += batchSize) {
                final int start = i;
                final int end = Math.min(i + batchSize, enemies.size());
                
                Future<?> future = collisionExecutor.submit(() -> {
                    for (int j = start; j < end; j++) {
                        checkPlayerEnemyCollision(playerTransform, player, enemies.get(j));
                    }
                });
                futures.add(future);
            }
            
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    private void checkPlayerEnemyCollisionsSerial(TransformComponent playerTransform, GameObject player, List<GameObject> enemies) {
        for (GameObject enemy : enemies) {
            checkPlayerEnemyCollision(playerTransform, player, enemy);
        }
    }
    
    private void checkPlayerEnemyCollision(TransformComponent playerTransform, GameObject player, GameObject enemy) {
        if (!enemy.isActive()) return;
        
        TransformComponent enemyTransform = enemy.getComponent(TransformComponent.class);
        if (enemyTransform == null) return;
        
        float distance = playerTransform.getPosition().distance(enemyTransform.getPosition());
        if (distance < 25) {
            com.gameengine.components.HealthComponent health = player.getComponent(com.gameengine.components.HealthComponent.class);
            if (health != null) {
                health.takeDamage(1);
                enemy.destroy();
            } else {
                playerTransform.setPosition(new Vector2(400, 300));
            }
        }
    }
}
