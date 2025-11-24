package com.gameengine.example;

import com.gameengine.core.GameEngine;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.scene.Scene;

public class MenuScene extends Scene {
    private GameEngine engine;
    private Renderer renderer;
    private InputManager inputManager;
    private int selectedIndex;
    private String[] options;
    private boolean selectionMade;
    private long lastKeyPress;
    
    public MenuScene(GameEngine engine, String name) {
        super(name);
        this.engine = engine;
        this.selectedIndex = 0;
        this.options = new String[]{"START GAME", "REPLAY", "EXIT"};
        this.selectionMade = false;
        this.lastKeyPress = 0;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = (Renderer)engine.getRenderer();
        this.inputManager = InputManager.getInstance();
        this.selectedIndex = 0;
        this.selectionMade = false;
        System.out.println("菜单场景已初始化，按↑↓键选择，按Enter确认");
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        long now = System.currentTimeMillis();
        if (now - lastKeyPress < 200) return; // 防抖
        
        // 键盘导航
        if (inputManager.isKeyJustPressed(38)) { // UP
            selectedIndex = (selectedIndex - 1 + options.length) % options.length;
            lastKeyPress = now;
            System.out.println("选择: " + options[selectedIndex]);
        } else if (inputManager.isKeyJustPressed(40)) { // DOWN
            selectedIndex = (selectedIndex + 1) % options.length;
            lastKeyPress = now;
            System.out.println("选择: " + options[selectedIndex]);
        } else if (inputManager.isKeyJustPressed(10) || inputManager.isKeyJustPressed(32)) { // ENTER or SPACE
            selectionMade = true;
            lastKeyPress = now;
        }
        
        if (selectionMade) {
            System.out.println("选中菜单项: " + options[selectedIndex]);
            switch (selectedIndex) {
                case 0: // START GAME
                    engine.setScene(new GameSceneWithRecording(engine));
                    break;
                case 1: // REPLAY
                    engine.setScene(new ReplayScene(engine, null));
                    break;
                case 2: // EXIT
                    engine.stop();
                    break;
            }
        }
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, 800, 600, 0.1f, 0.1f, 0.2f, 1.0f);
        
        // 绘制标题区域背景
        renderer.drawRect(100, 40, 600, 80, 0.2f, 0.3f, 0.5f, 0.9f);
        renderer.drawRect(110, 50, 580, 60, 0.25f, 0.35f, 0.55f, 0.8f);
        
        // 绘制标题文本
        renderer.drawText("GAME MENU", 400, 85, 40, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // 绘制菜单选项
        float startY = 200;
        for (int i = 0; i < options.length; i++) {
            float optY = startY + i * 90;
            
            if (i == selectedIndex) {
                // 高亮选中项 - 使用更醒目的颜色
                renderer.drawRect(120, optY - 30, 560, 70, 1.0f, 1.0f, 0.0f, 0.3f);
                renderer.drawRect(125, optY - 25, 550, 60, 0.5f, 0.8f, 1.0f, 0.4f);
                
                // 绘制选中指示器（左边的箭头）
                renderer.drawRect(80, optY - 15, 30, 40, 1.0f, 0.6f, 0.0f, 1.0f);
            } else {
                renderer.drawRect(120, optY - 30, 560, 70, 0.2f, 0.2f, 0.3f, 0.3f);
                renderer.drawRect(125, optY - 25, 550, 60, 0.3f, 0.3f, 0.4f, 0.2f);
            }
            
            // 绘制菜单项文本
            float textColor = (i == selectedIndex) ? 1.0f : 0.8f;
            renderer.drawText(options[i], 400, optY + 5, 28, textColor, textColor, textColor, 1.0f);
        }
        
        // 绘制提示信息区域
        renderer.drawRect(30, 520, 740, 60, 0.05f, 0.05f, 0.1f, 0.7f);
        renderer.drawText("Use UP/DOWN to select, PRESS ENTER to confirm", 400, 555, 16, 0.6f, 0.8f, 1.0f, 0.9f);
    }
}
