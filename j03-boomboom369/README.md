[![Review Assignment Due Date](https://classroom.github.com/assets/deadline-readme-button-22041afd0340ce965d47ae6ef1cefeee28c7c493a6346c4f15d667ab976d596c.svg)](https://classroom.github.com/a/aDiFgvK1)

# j03 回放功能概要

为满足“可以录制并回放一整局战斗”的要求，本项目主要做了三点改动：

1. **录制采集**  
   - `RecordingService` 改写为 JSONL 输出：`header / input / keyframe`。  
   - 关键帧记录对象 `id + uid`、位置、渲染形状/颜色，停止录制时会强制再写一帧。  
   - `GameSceneWithRecording` 在进入场景时自动 `start()`，退出或玩家死亡时 `stop()` 并生成 `recordings/session_<timestamp>.jsonl`。

2. **实体还原**  
   - 新增 `EntityFactory`，把玩家、敌人、子弹等外观封装为“预制体”，游戏与回放共用。  
   - 回放时可根据关键帧中的渲染信息/颜色恢复近似外观，确保玩家看到与实战一致的画面。

3. **回放界面**  
   - `ReplayScene` 提供录制文件列表（方向键 + Enter 选择）。  
   - 读取关键帧后：若存在 `uid`，按 `uid` 匹配对象并插值更新；若没有则按索引回退。  
   - 界面底部显示时间进度条与提示文案，按 `ESC` 即可返回主菜单。

运行方式：

```bash
cd j03-boomboom369
./compile.sh
java -cp build/classes com.gameengine.example.GameExample
```

流程：先在菜单选择 `START GAME` 产生录制，再回到菜单选择 `REPLAY`，挑选刚生成的 `session_*.jsonl` 就能看到完整回放。***
