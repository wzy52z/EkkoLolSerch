package com.wly.lol.context;

import com.wly.lol.model.PlayerAnalysisResult;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 全局容器：存放当前对局的分析结果，供前端读取
 */
@Component
public class GameContext {
    // 使用线程安全的 List，防止写入和读取冲突
    private final List<PlayerAnalysisResult> currentPlayers = new CopyOnWriteArrayList<>();

    public void addPlayer(PlayerAnalysisResult player) {
        // 如果列表中已经有这个人，先移除旧的（防止重复显示）
        currentPlayers.removeIf(p -> p.getName().equals(player.getName()));
        currentPlayers.add(player);
    }

    public List<PlayerAnalysisResult> getPlayers() {
        return new ArrayList<>(currentPlayers);
    }

    public void clear() {
        currentPlayers.clear();
    }
}