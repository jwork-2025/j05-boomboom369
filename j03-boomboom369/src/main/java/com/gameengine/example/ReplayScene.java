package com.gameengine.example;

import com.gameengine.components.TransformComponent;
import com.gameengine.core.GameEngine;
import com.gameengine.core.GameObject;
import com.gameengine.graphics.Renderer;
import com.gameengine.input.InputManager;
import com.gameengine.math.Vector2;
import com.gameengine.recording.FileRecordingStorage;
import com.gameengine.recording.RecordingJson;
import com.gameengine.recording.RecordingStorage;
import com.gameengine.scene.Scene;

import java.io.File;
import java.util.*;

/**
 * 回放场景 - 支持读取和回放存档
 */
public class ReplayScene extends Scene {
    private GameEngine engine;
    private Renderer renderer;
    private InputManager inputManager;
    private String recordingPath;
    private float time;
    private List<Keyframe> keyframes;
    private List<GameObject> objectList;
    private final Map<String, GameObject> objectMap = new HashMap<>();
    
    // 文件选择模式
    private List<File> recordingFiles;
    private int selectedFileIndex;
    
    private static class Keyframe {
        static class EntityInfo {
            String id;
            String uid;
            Vector2 pos;
            String rt; // RECTANGLE/CIRCLE/LINE/CUSTOM
            float w, h;
            float r = 0.9f, g = 0.9f, b = 0.2f, a = 1.0f;
        }
        double t;
        List<EntityInfo> entities = new ArrayList<>();
    }
    
    public ReplayScene(GameEngine engine, String path) {
        super("Replay");
        this.engine = engine;
        this.recordingPath = path;
        this.keyframes = new ArrayList<>();
        this.objectList = new ArrayList<>();
        this.time = 0;
    }
    
    @Override
    public void initialize() {
        super.initialize();
        this.renderer = engine.getRenderer();
        this.inputManager = InputManager.getInstance();
        
        if (recordingPath != null) {
            loadRecording(recordingPath);
            buildObjectsFromFirstKeyframe();
        } else {
            // 文件选择模式
            loadRecordingFiles();
        }
    }
    
    @Override
    public void update(float deltaTime) {
        super.update(deltaTime);
        
        // ESC 键返回菜单
        if (inputManager.isKeyJustPressed(27)) {
            engine.setScene(new MenuScene(engine, "MainMenu"));
            return;
        }
        
        // 如果在文件选择模式
        if (recordingPath == null) {
            handleFileSelection();
            return;
        }
        
        // 回放模式
        if (keyframes.isEmpty()) return;
        
        time += deltaTime;
        double lastT = keyframes.get(keyframes.size() - 1).t;
        if (time > lastT) {
            time = (float)lastT;
        }
        
        // 找到相邻的两个关键帧
        Keyframe frameA = keyframes.get(0);
        Keyframe frameB = keyframes.get(keyframes.size() - 1);
        
        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe k1 = keyframes.get(i);
            Keyframe k2 = keyframes.get(i + 1);
            if (time >= k1.t && time <= k2.t) {
                frameA = k1;
                frameB = k2;
                break;
            }
        }
        
        // 线性插值
        double span = Math.max(1e-6, frameB.t - frameA.t);
        double u = Math.min(1.0, Math.max(0.0, (time - frameA.t) / span));
        
        updateInterpolatedPositions(frameA, frameB, (float)u);
    }
    
    @Override
    public void render() {
        // 绘制背景
        renderer.drawRect(0, 0, renderer.getWidth(), renderer.getHeight(), 0.1f, 0.1f, 0.2f, 1.0f);
        
        if (recordingPath == null) {
            renderFileList();
        } else {
            // 回放场景渲染
            super.render();
            // 绘制时间指示条
            float total = keyframes.isEmpty() ? 1f : (float) keyframes.get(keyframes.size() - 1).t;
            float progress = total <= 0.0001f ? 0f : (time / total);
            progress = Math.max(0, Math.min(1, progress));
            float barX = 20f;
            float barY = renderer.getHeight() - 40f;
            float barW = renderer.getWidth() - 40f;
            float barH = 18f;
            renderer.drawRect(barX, barY, barW, barH, 0.2f, 0.2f, 0.2f, 0.8f);
            renderer.drawRect(barX, barY, barW * progress, barH, 0.0f, 1.0f, 0.0f, 0.8f);
            renderer.drawText(String.format("t=%.2fs / %.2fs", time, total),
                    renderer.getWidth() / 2.0f, barY - 12f, 14,
                    0.8f, 0.8f, 0.8f, 1.0f);
        }
    }
    
    private void loadRecording(String path) {
        keyframes.clear();
        RecordingStorage storage = new FileRecordingStorage();
        try {
            for (String line : storage.readLines(path)) {
                if (line.contains("\"type\":\"keyframe\"")) {
                    Keyframe kf = new Keyframe();
                    kf.t = RecordingJson.parseDouble(RecordingJson.field(line, "t"));
                    
                    int idx = line.indexOf("\"entities\":[");
                    if (idx >= 0) {
                        int bracket = line.indexOf('[', idx);
                        String arr = bracket >= 0 ? RecordingJson.extractArray(line, bracket) : "";
                        String[] parts = RecordingJson.splitTopLevel(arr);
                        
                        for (String p : parts) {
                            Keyframe.EntityInfo ei = new Keyframe.EntityInfo();
                            ei.id = RecordingJson.stripQuotes(RecordingJson.field(p, "id"));
                            ei.uid = RecordingJson.stripQuotes(RecordingJson.field(p, "uid"));
                            double x = RecordingJson.parseDouble(RecordingJson.field(p, "x"));
                            double y = RecordingJson.parseDouble(RecordingJson.field(p, "y"));
                            ei.pos = new Vector2((float)x, (float)y);
                            ei.rt = RecordingJson.stripQuotes(RecordingJson.field(p, "rt"));
                            ei.w = (float)RecordingJson.parseDouble(RecordingJson.field(p, "w"));
                            ei.h = (float)RecordingJson.parseDouble(RecordingJson.field(p, "h"));
                            
                            String colorArr = RecordingJson.field(p, "color");
                            if (colorArr != null && colorArr.startsWith("[")) {
                                String c = colorArr.substring(1, Math.max(1, colorArr.indexOf(']')));
                                String[] cs = c.split(",");
                                if (cs.length >= 3) {
                                    try {
                                        ei.r = Float.parseFloat(cs[0].trim());
                                        ei.g = Float.parseFloat(cs[1].trim());
                                        ei.b = Float.parseFloat(cs[2].trim());
                                        if (cs.length >= 4) ei.a = Float.parseFloat(cs[3].trim());
                                    } catch (Exception ignored) {}
                                }
                            }
                            kf.entities.add(ei);
                        }
                    }
                    sortEntitiesByStableId(kf);
                    keyframes.add(kf);
                }
            }
        } catch (Exception e) {
            System.err.println("读取录制文件失败: " + e.getMessage());
        }
        keyframes.sort(Comparator.comparingDouble(k -> k.t));
    }
    
    private void buildObjectsFromFirstKeyframe() {
        if (keyframes.isEmpty()) return;
        Keyframe kf0 = keyframes.get(0);
        objectList.clear();
        objectMap.clear();
        clear();
        
        for (int i = 0; i < kf0.entities.size(); i++) {
            GameObject obj = buildObjectFromEntity(kf0.entities.get(i), i);
            addGameObject(obj);
            objectList.add(obj);
            String key = stableKeyForEntity(kf0.entities.get(i), i);
            objectMap.put(key, obj);
        }
        time = 0;
    }
    
    private void updateInterpolatedPositions(Keyframe frameA, Keyframe frameB, float u) {
        if (hasStableIds(frameA) && hasStableIds(frameB)) {
            updateWithStableIds(frameA, frameB, u);
        } else {
            updateByIndex(frameA, frameB, u);
        }
    }
    
    private void updateByIndex(Keyframe frameA, Keyframe frameB, float u) {
        int n = Math.min(frameA.entities.size(), frameB.entities.size());
        ensureListSize(n);
        for (int i = 0; i < n; i++) {
            Vector2 posA = frameA.entities.get(i).pos;
            Vector2 posB = frameB.entities.get(i).pos;
            Vector2 pos = interpolate(posA, posB, u);
            GameObject obj = objectList.get(i);
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc != null) {
                tc.setPosition(pos);
            }
        }
    }

    private void updateWithStableIds(Keyframe frameA, Keyframe frameB, float u) {
        Map<String, Keyframe.EntityInfo> mapA = new HashMap<>();
        for (int i = 0; i < frameA.entities.size(); i++) {
            Keyframe.EntityInfo info = frameA.entities.get(i);
            if (info.uid != null) {
                mapA.put(info.uid, info);
            }
        }

        Set<String> alive = new HashSet<>();
        for (int i = 0; i < frameB.entities.size(); i++) {
            Keyframe.EntityInfo infoB = frameB.entities.get(i);
            String key = stableKeyForEntity(infoB, i);
            alive.add(key);
            Keyframe.EntityInfo infoA = infoB.uid != null ? mapA.get(infoB.uid) : null;
            Vector2 start = infoA != null ? infoA.pos : infoB.pos;
            Vector2 pos = interpolate(start, infoB.pos, u);
            GameObject obj = ensureReplayObject(key, infoB, i);
            TransformComponent tc = obj.getComponent(TransformComponent.class);
            if (tc != null) {
                tc.setPosition(pos);
            }
        }

        deactivateMissingObjects(alive);
    }
    
    private GameObject buildObjectFromEntity(Keyframe.EntityInfo ei, int index) {
        GameObject obj;
        String name = ei.id == null ? "Entity" : ei.id;
        com.gameengine.components.RenderComponent.Color color =
            new com.gameengine.components.RenderComponent.Color(ei.r, ei.g, ei.b, ei.a);
        float width = Math.max(1f, ei.w > 0 ? ei.w : 10f);
        float height = Math.max(1f, ei.h > 0 ? ei.h : 10f);
        Vector2 size = new Vector2(width, height);

        if ("Player".equalsIgnoreCase(name)) {
            obj = EntityFactory.createPlayerVisual(renderer);
        } else if ("Bullet".equalsIgnoreCase(name) || "Decoration".equalsIgnoreCase(name)
                || "CIRCLE".equalsIgnoreCase(ei.rt)) {
            obj = EntityFactory.createCircle(renderer, name, size, color);
        } else {
            obj = EntityFactory.createRectangle(renderer, name, size, color);
        }

        TransformComponent tc = obj.getComponent(TransformComponent.class);
        if (tc == null) {
            tc = obj.addComponent(new TransformComponent(new Vector2(ei.pos)));
        } else {
            tc.setPosition(new Vector2(ei.pos));
        }
        return obj;
    }

    private GameObject ensureReplayObject(String key, Keyframe.EntityInfo entity, int index) {
        GameObject existing = objectMap.get(key);
        if (existing != null && existing.isActive()) {
            return existing;
        }
        GameObject created = buildObjectFromEntity(entity, index);
        objectMap.put(key, created);
        objectList.add(created);
        addGameObject(created);
        return created;
    }

    private void deactivateMissingObjects(Set<String> aliveKeys) {
        Iterator<Map.Entry<String, GameObject>> iterator = objectMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, GameObject> entry = iterator.next();
            if (!aliveKeys.contains(entry.getKey())) {
                entry.getValue().setActive(false);
                iterator.remove();
            }
        }
        objectList.removeIf(obj -> !obj.isActive());
    }

    private boolean hasStableIds(Keyframe frame) {
        if (frame == null || frame.entities.isEmpty()) {
            return false;
        }
        for (Keyframe.EntityInfo info : frame.entities) {
            if (info.uid == null) {
                return false;
            }
        }
        return true;
    }

    private void ensureListSize(int n) {
        while (objectList.size() < n) {
            GameObject placeholder = EntityFactory.createRectangle(
                    renderer,
                    "ReplayObj#" + objectList.size(),
                    new Vector2(10, 10),
                    new com.gameengine.components.RenderComponent.Color(0.7f, 0.7f, 0.7f, 1.0f)
            );
            addGameObject(placeholder);
            objectList.add(placeholder);
        }
        while (objectList.size() > n) {
            GameObject obj = objectList.remove(objectList.size() - 1);
            obj.setActive(false);
        }
    }

    private Vector2 interpolate(Vector2 a, Vector2 b, float u) {
        float x = (1.0f - u) * a.x + u * b.x;
        float y = (1.0f - u) * a.y + u * b.y;
        return new Vector2(x, y);
    }

    private String stableKeyForEntity(Keyframe.EntityInfo info, int index) {
        if (info.uid != null) {
            return info.uid;
        }
        String base = info.id == null ? "Entity" : info.id;
        return base + "@idx" + index;
    }

    private void sortEntitiesByStableId(Keyframe keyframe) {
        if (keyframe == null || keyframe.entities.isEmpty()) {
            return;
        }
        boolean allUid = keyframe.entities.stream().allMatch(e -> e.uid != null);
        if (allUid) {
            keyframe.entities.sort(Comparator.comparing(e -> e.uid));
        }
    }
    
    // ========== 文件列表模式 ==========
    private void loadRecordingFiles() {
        RecordingStorage storage = new FileRecordingStorage();
        recordingFiles = storage.listRecordings();
        selectedFileIndex = 0;
    }
    
    private void handleFileSelection() {
        if (recordingFiles == null) {
            loadRecordingFiles();
        }
        if (recordingFiles.isEmpty()) {
            if (inputManager.isKeyJustPressed(27)) {
                engine.setScene(new MenuScene(engine, "MainMenu"));
            }
            return;
        }
        
        if (inputManager.isKeyJustPressed(38)) { // UP
            selectedFileIndex = (selectedFileIndex - 1 + recordingFiles.size()) % recordingFiles.size();
        } else if (inputManager.isKeyJustPressed(40)) { // DOWN
            selectedFileIndex = (selectedFileIndex + 1) % recordingFiles.size();
        } else if (inputManager.isKeyJustPressed(10) || inputManager.isKeyJustPressed(32)) { // ENTER or SPACE
            recordingPath = recordingFiles.get(selectedFileIndex).getAbsolutePath();
            clear();
            initialize();
        }
    }
    
    private void renderFileList() {
        float w = renderer.getWidth();
        float h = renderer.getHeight();
        renderer.drawRect(80, 40, w - 160, 60, 0.2f, 0.3f, 0.5f, 0.8f);
        renderer.drawText("SELECT RECORDING", w / 2.0f, 70, 28,
                1.0f, 1.0f, 1.0f, 1.0f);
        
        if (recordingFiles == null || recordingFiles.isEmpty()) {
            renderer.drawRect(w / 2f - 180, h / 2f - 40, 360, 80, 0.2f, 0.2f, 0.2f, 0.7f);
            renderer.drawText("NO RECORDINGS FOUND", w / 2.0f, h / 2.0f, 20,
                    0.9f, 0.8f, 0.2f, 1.0f);
            renderer.drawText("PRESS ESC TO RETURN", w / 2.0f, h - 60, 18,
                    0.7f, 0.7f, 0.7f, 1.0f);
            return;
        }
        
        float startY = 140;
        float rowH = 32;
        for (int i = 0; i < recordingFiles.size(); i++) {
            float y = startY + i * rowH;
            if (i == selectedFileIndex) {
                renderer.drawRect(90, y - 12, w - 180, rowH, 0.3f, 0.5f, 0.7f, 0.6f);
            } else {
                renderer.drawRect(90, y - 12, w - 180, rowH, 0.15f, 0.15f, 0.2f, 0.4f);
            }
            String name = recordingFiles.get(i).getName();
            renderer.drawText(name, w / 2.0f, y + 2, 18,
                    0.95f, 0.95f, 0.95f, 1.0f);
        }
        
        renderer.drawRect(70, h - 90, w - 140, 50, 0.1f, 0.1f, 0.1f, 0.6f);
        renderer.drawText("UP/DOWN SELECT • ENTER CONFIRM • ESC RETURN",
                w / 2.0f, h - 65, 16,
                0.7f, 0.7f, 0.7f, 1.0f);
    }
}
