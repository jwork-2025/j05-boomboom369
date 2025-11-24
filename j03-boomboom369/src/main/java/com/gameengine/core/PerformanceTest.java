package com.gameengine.core;

import com.gameengine.components.TransformComponent;
import com.gameengine.components.PhysicsComponent;
import com.gameengine.math.Vector2;
import com.gameengine.scene.Scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class PerformanceTest {
    
    public static class PerformanceResult {
        public int objectCount;
        public long physicsUpdateTime;  // 纳秒
        public long collisionCheckTime; // 纳秒
        public int frameCount;
        public long totalTime;
        
        public PerformanceResult(int objectCount) {
            this.objectCount = objectCount;
            this.physicsUpdateTime = 0;
            this.collisionCheckTime = 0;
            this.frameCount = 0;
            this.totalTime = 0;
        }
        
        public double getPhysicsMs() {
            return physicsUpdateTime / 1_000_000.0;
        }
        
        public double getCollisionMs() {
            return collisionCheckTime / 1_000_000.0;
        }
        
        public double getAverageFPS() {
            if (frameCount == 0) return 0;
            return frameCount / (totalTime / 1_000_000_000.0);
        }
        
        @Override
        public String toString() {
            return String.format(
                "Objects: %d | Physics: %.2fms | Collision: %.2fms | Frames: %d | AvgFPS: %.1f",
                objectCount,
                getPhysicsMs(),
                getCollisionMs(),
                frameCount,
                getAverageFPS()
            );
        }
    }
    
    /**
     * 测试单个循环的性能
     */
    public static PerformanceResult testSingleFrame(int enemyCount, int bulletCount) {
        PerformanceResult result = new PerformanceResult(enemyCount + bulletCount);
        
        // 创建测试场景
        Scene testScene = new Scene("PerformanceTestScene");
        GameLogic gameLogic = new GameLogic(testScene);
        
        // 创建敌人游戏对象
        Random random = new Random(42); // 使用固定种子以保证可重复性
        for (int i = 0; i < enemyCount; i++) {
            GameObject enemy = new GameObject("Enemy");
            enemy.addComponent(new TransformComponent(
                new Vector2(random.nextFloat() * 800, random.nextFloat() * 600)
            ));
            PhysicsComponent physics = enemy.addComponent(new PhysicsComponent(0.5f));
            physics.setVelocity(new Vector2(
                (random.nextFloat() - 0.5f) * 100,
                (random.nextFloat() - 0.5f) * 100
            ));
            testScene.addGameObject(enemy);
        }
        
        // 创建子弹游戏对象
        for (int i = 0; i < bulletCount; i++) {
            GameObject bullet = new GameObject("Bullet");
            bullet.addComponent(new TransformComponent(
                new Vector2(random.nextFloat() * 800, random.nextFloat() * 600)
            ));
            PhysicsComponent physics = bullet.addComponent(new PhysicsComponent(0.1f));
            physics.setVelocity(new Vector2(
                (random.nextFloat() - 0.5f) * 200,
                (random.nextFloat() - 0.5f) * 200
            ));
            testScene.addGameObject(bullet);
        }
        
        testScene.initialize();
        
        // 运行性能测试 - 执行多帧以获得平均值
        int testFrames = 10;
        long totalTime = 0;
        long totalPhysics = 0;
        long totalCollision = 0;
        
        for (int frame = 0; frame < testFrames; frame++) {
            long frameStart = System.nanoTime();
            
            // 更新物理
            gameLogic.updatePhysics();
            totalPhysics += gameLogic.getPhysicsUpdateTime();
            
            // 检查碰撞
            gameLogic.checkCollisions();
            totalCollision += gameLogic.getCollisionCheckTime();
            
            long frameEnd = System.nanoTime();
            totalTime += (frameEnd - frameStart);
        }
        
        result.physicsUpdateTime = totalPhysics / testFrames;
        result.collisionCheckTime = totalCollision / testFrames;
        result.frameCount = testFrames;
        result.totalTime = totalTime;
        
        gameLogic.cleanup();
        
        return result;
    }
    
    /**
     * 运行性能对比测试
     */
    public static void runPerformanceComparison() {
        System.out.println("\n===== 游戏性能测试报告 =====\n");
        System.out.println("并行优化版本 - 物理更新和碰撞检测性能测试\n");
        
        // 测试不同数量的敌人和子弹
        int[][] testCases = {
            {10, 5},   // 10个敌人，5个子弹
            {30, 10},  // 30个敌人，10个子弹
            {50, 15},  // 50个敌人，15个子弹
            {100, 20}, // 100个敌人，20个子弹
            {150, 30}, // 150个敌人，30个子弹
        };
        
        System.out.println("测试场景配置和结果：\n");
        System.out.println("对象数量  | 物理更新耗时 | 碰撞检测耗时 | 总体性能");
        System.out.println("---------|------------|------------|----------");
        
        for (int[] testCase : testCases) {
            int enemyCount = testCase[0];
            int bulletCount = testCase[1];
            
            PerformanceResult result = testSingleFrame(enemyCount, bulletCount);
            
            System.out.printf("%3d+%-3d | %8.2fms | %8.2fms | FPS: %.1f\n",
                enemyCount, bulletCount,
                result.getPhysicsMs(),
                result.getCollisionMs(),
                result.getAverageFPS()
            );
        }
        
        System.out.println("\n===== 性能测试完成 =====\n");
        System.out.println("说明：");
        System.out.println("- 物理更新耗时：执行updatePhysics()方法的平均时间（毫秒）");
        System.out.println("- 碰撞检测耗时：执行checkCollisions()方法的平均时间（毫秒）");
        System.out.println("- FPS：基于总执行时间估算的平均帧率");
        System.out.println("- 并行优化通过ExecutorService线程池提高多核CPU利用率");
        System.out.println("- 当对象数量较少（<10）时，使用串行处理避免线程开销\n");
    }
    
    public static void main(String[] args) {
        System.out.println("开始游戏性能测试...\n");
        runPerformanceComparison();
    }
}
