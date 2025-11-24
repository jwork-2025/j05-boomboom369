package com.gameengine.components;

import com.gameengine.core.Component;

/**
 * 简单的生命值组件
 */
public class HealthComponent extends Component<HealthComponent> {
    private int hp;

    public HealthComponent(int hp) {
        this.hp = Math.max(0, hp);
    }

    @Override
    public void initialize() {
        // nothing
    }

    @Override
    public void update(float deltaTime) {
        // nothing
    }

    @Override
    public void render() {
        // nothing
    }

    public int getHp() {
        return hp;
    }

    public void setHp(int hp) {
        this.hp = Math.max(0, hp);
    }

    public void takeDamage(int dmg) {
        this.hp = Math.max(0, this.hp - Math.max(0, dmg));
    }

    public void heal(int amount) {
        this.hp = Math.max(0, this.hp + Math.max(0, amount));
    }
}
