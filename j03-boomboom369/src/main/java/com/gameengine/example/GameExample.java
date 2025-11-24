package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.scene.Scene;

/**
 * 游戏示例 - 支持存档回放
 */
public class GameExample {
    public static void main(String[] args) {
        System.out.println("启动游戏引擎...");
        
        try {
            // 创建游戏引擎
            GameEngine engine = new GameEngine(800, 600, "游戏引擎 - 支持存档回放");
            
            // 创建菜单场景
            Scene menuScene = new MenuScene(engine, "MainMenu");
            
            // 设置初始场景为菜单
            engine.setScene(menuScene);
            
            // 运行游戏（此时Timer在后台运行）
            engine.run();
            
            // 保持主线程运行，直到游戏结束
            // Swing的EventDispatchThread会继续运行Timer
            Thread.currentThread().join();
            
        } catch (InterruptedException e) {
            System.out.println("游戏中断");
        } catch (Exception e) {
            System.err.println("游戏运行出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("游戏结束");
    }
}
