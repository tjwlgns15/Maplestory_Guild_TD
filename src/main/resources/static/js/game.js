'use strict';

/* ================================================================
   game.js — 메이플 길드 TD
   의존: game-data.js (fetchJobConfig, buildTowerConfig,
                       calcAttackDamage, calcCooldown, calcRange)
   ================================================================ */

// ─────────────────────────────────────────────────────────────────
// 0. 유틸
// ─────────────────────────────────────────────────────────────────
const $ = id => document.getElementById(id);

function addLog(msg, type = '') {
    const el = document.createElement('div');
    el.className = `log-entry ${type}`;
    el.textContent = msg;
    $('log-list').prepend(el);
    // 최대 60개
    const items = $('log-list').children;
    if (items.length > 60) items[items.length - 1].remove();
}

function fmtMs(ms) {
    return ms >= 1000 ? (ms / 1000).toFixed(1) + 's' : ms + 'ms';
}

function dist(a, b) {
    const dx = a.x - b.x, dy = a.y - b.y;
    return Math.sqrt(dx * dx + dy * dy);
}

// ─────────────────────────────────────────────────────────────────
// 1. 캔버스 초기화
// ─────────────────────────────────────────────────────────────────
const canvas = $('gameCanvas');
const ctx    = canvas.getContext('2d');

function resizeCanvas() {
    const wrap = $('canvas-wrap');
    canvas.width  = wrap.clientWidth;
    canvas.height = wrap.clientHeight;
}
resizeCanvas();
window.addEventListener('resize', () => { resizeCanvas(); });

// ─────────────────────────────────────────────────────────────────
// 2. 맵 & 경로 정의
// ─────────────────────────────────────────────────────────────────
/**
 * 경로는 캔버스 크기에 대한 비율(0~1)로 정의
 * → 실제 좌표는 getPath()로 변환
 */
const PATH_RATIO = [
    { x: 0,    y: 0.5  },
    { x: 0.18, y: 0.5  },
    { x: 0.18, y: 0.2  },
    { x: 0.45, y: 0.2  },
    { x: 0.45, y: 0.75 },
    { x: 0.72, y: 0.75 },
    { x: 0.72, y: 0.35 },
    { x: 1.0,  y: 0.35 },
];

function getPath() {
    return PATH_RATIO.map(p => ({
        x: p.x * canvas.width,
        y: p.y * canvas.height,
    }));
}

// 경로 위 배치 금지 판정 (타일 폭 = 40px 기준)
const PATH_HALF = 28;
function isOnPath(px, py) {
    const path = getPath();
    for (let i = 0; i < path.length - 1; i++) {
        const a = path[i], b = path[i + 1];
        // 선분에 점을 투영, 가장 가까운 거리 계산
        const dx = b.x - a.x, dy = b.y - a.y;
        const len2 = dx * dx + dy * dy;
        if (len2 === 0) continue;
        const t = Math.max(0, Math.min(1, ((px - a.x) * dx + (py - a.y) * dy) / len2));
        const cx = a.x + t * dx, cy = a.y + t * dy;
        if (Math.hypot(px - cx, py - cy) < PATH_HALF) return true;
    }
    return false;
}

// ─────────────────────────────────────────────────────────────────
// 3. 게임 상태
// ─────────────────────────────────────────────────────────────────
const STATE = {
    BASE_HP:    20,
    baseHp:     20,
    gold:       600,
    kills:      0,
    round:      0,
    MAX_ROUND:  15,
    speed:      1,          // 1 or 2
    phase:      'IDLE',     // IDLE | WAVE | BETWEEN | GAMEOVER | CLEAR
    placingConfig: null,    // 배치 중인 towerConfig
    selectedTower: null,    // 클릭으로 선택된 배치 타워 인스턴스
    activeCooldowns: new Map(), // towerInstance → remainingMs
};

// ─────────────────────────────────────────────────────────────────
// 4. Tower 클래스
// ─────────────────────────────────────────────────────────────────
class Tower {
    constructor(x, y, config) {
        this.x      = x;
        this.y      = y;
        this.config = config;
        this.name   = config.name;
        this.color  = config.color;
        this.image  = config.image ? loadImage(config.image) : null;

        // 패시브 적용 최종 스탯
        this.damage   = calcAttackDamage(config);
        this.range    = calcRange(config);
        this.cooldown = calcCooldown(config);

        this.attackTimer    = 0;
        this.activeCooldown = 0;     // 액티브 스킬 남은 쿨다운 (ms)
        this.target         = null;  // 현재 타겟 Enemy

        // 시각 효과
        this.shootEffect = 0;        // 공격 flash 타이머
        this.skillEffect = null;     // { type, x, y, radius, timer, color }
    }

    update(dt, enemies) {
        // 공격 쿨다운
        this.attackTimer = Math.max(0, this.attackTimer - dt * STATE.speed);

        // 액티브 쿨다운
        if (this.activeCooldown > 0) {
            this.activeCooldown = Math.max(0, this.activeCooldown - dt * STATE.speed);
            if (this.activeCooldown === 0) addLog(`[${this.name}] 스킬 재사용 가능!`, 'info');
        }

        // 버프 타이머 — 만료 시 데미지 원래값으로 복원
        if (this.buffTimer > 0) {
            this.buffTimer = Math.max(0, this.buffTimer - dt * STATE.speed);
            if (this.buffTimer === 0) {
                this.damage = calcAttackDamage(this.config);
                this.buffRatio = 0;
            }
        }

        // 시각 효과 타이머
        if (this.shootEffect > 0) this.shootEffect -= dt * STATE.speed;
        if (this.skillEffect) {
            this.skillEffect.timer -= dt * STATE.speed;
            if (this.skillEffect.timer <= 0) this.skillEffect = null;
        }

        if (this.attackTimer > 0 || enemies.length === 0) return;

        // 타겟 선정: 경로 가장 앞에 있는 적
        this.target = null;
        let maxProgress = -1;
        for (const e of enemies) {
            if (e.isDead) continue;
            if (dist(this, e) <= this.range && e.pathProgress > maxProgress) {
                maxProgress = e.pathProgress;
                this.target = e;
            }
        }

        if (!this.target) return;

        this.attackTimer = this.cooldown;
        this.shootEffect = 180;

        // 패시브 효과 적용
        this.applyPassiveOnHit(this.target);

        // 데미지 적용
        this.target.takeDamage(this.damage);
        if (this.target.isDead) {
            STATE.kills++;
            STATE.gold += this.target.reward;
            updateHUD();
            addLog(`[${this.name}] ${this.target.name} 처치 (+${this.target.reward}G)`, 'good');
        }
    }

    applyPassiveOnHit(enemy) {
        const p = this.config.passiveSkill;
        if (!p || enemy.isDead) return;

        if (p.effectType === 'ON_HIT_SLOW') {
            enemy.applySlow(p.effectValue, p.effectDuration ?? 1500);
        }
        if (p.effectType === 'ON_HIT_DOT') {
            enemy.applyDot(
                this.damage * p.effectValue,
                p.dotTicks    ?? 3,
                p.dotInterval ?? 600
            );
        }
    }

    /**
     * 액티브 스킬 발동
     * @param {Enemy[]} enemies
     */
    useActive(enemies) {
        const skill = this.config.activeSkill;
        if (!skill || this.activeCooldown > 0) return false;

        this.activeCooldown = skill.cooldown;
        addLog(`[${this.name}] ${skill.name} 발동!`, 'warn');

        const cx = this.x, cy = this.y;
        const radius = skill.aoeRadius ?? 0;
        const color  = skill.skillColor ?? '#f6a623';

        // 이펙트 기록
        this.skillEffect = {
            type:   skill.effectType,
            x: cx, y: cy,
            radius: radius || 60,
            timer:  500,
            color,
        };

        switch (skill.effectType) {
            case 'SINGLE_BURST': {
                // 가장 가까운 적 단일 강타
                let closest = null, minD = Infinity;
                for (const e of enemies) {
                    if (!e.isDead) { const d = dist(this, e); if (d < minD) { minD = d; closest = e; } }
                }
                if (closest) {
                    const dmg = this.damage * skill.effectValue;
                    closest.takeDamage(dmg);
                    addLog(`  → ${closest.name}에게 ${dmg.toFixed(1)} 강타`, 'good');
                    if (closest.isDead) { STATE.kills++; STATE.gold += closest.reward; updateHUD(); }
                }
                break;
            }
            case 'AOE_DAMAGE': {
                let hit = 0;
                for (const e of enemies) {
                    if (!e.isDead && dist(this, e) <= radius) {
                        e.takeDamage(this.damage * skill.effectValue);
                        hit++;
                        if (e.isDead) { STATE.kills++; STATE.gold += e.reward; }
                    }
                }
                updateHUD();
                addLog(`  → 범위 ${hit}마리 적중`, 'good');
                break;
            }
            case 'FREEZE_FIELD': {
                let hit = 0;
                for (const e of enemies) {
                    if (!e.isDead && dist(this, e) <= radius) {
                        e.applyFreeze(skill.effectDuration ?? 2000);
                        hit++;
                    }
                }
                addLog(`  → ${hit}마리 빙결 (${fmtMs(skill.effectDuration ?? 2000)})`, 'info');
                break;
            }
            case 'SLOW_FIELD': {
                let hit = 0;
                for (const e of enemies) {
                    if (!e.isDead && dist(this, e) <= radius) {
                        e.applySlow(skill.effectValue, skill.effectDuration ?? 3000);
                        hit++;
                    }
                }
                addLog(`  → ${hit}마리 슬로우`, 'info');
                break;
            }
            case 'DOT_FIELD': {
                let hit = 0;
                for (const e of enemies) {
                    if (!e.isDead && dist(this, e) <= radius) {
                        e.applyDot(
                            this.damage * skill.effectValue,
                            skill.dotTicks    ?? 4,
                            skill.dotInterval ?? 700
                        );
                        hit++;
                    }
                }
                addLog(`  → ${hit}마리에게 지속피해`, 'info');
                break;
            }
            case 'BUFF_TOWERS': {
                const buffVal = skill.effectValue;
                const dur     = skill.effectDuration ?? 5000;
                let  cnt = 0;
                for (const t of placedTowers) {
                    if (t !== this && dist(this, t) <= radius) {
                        t.applyBuff(buffVal, dur);
                        cnt++;
                    }
                }
                addLog(`  → 주변 ${cnt}개 타워 데미지 +${(buffVal*100).toFixed(0)}% (${fmtMs(dur)})`, 'info');
                break;
            }
        }
        return true;
    }

    applyBuff(ratio, duration) {
        // 이미 버프 중이면 타이머만 갱신 (중복 배율 방지)
        this.buffRatio = ratio;
        this.buffTimer = Math.max(this.buffTimer ?? 0, duration);
        // 항상 기본 데미지 기준으로 배율 적용
        this.damage = calcAttackDamage(this.config) * (1 + ratio);
    }

    draw(ctx) {
        const { x, y, range, color, shootEffect, skillEffect, config } = this;
        const isSelected = STATE.selectedTower === this;

        // 사거리 원 (선택 시 or 호버)
        if (isSelected) {
            ctx.save();
            ctx.beginPath();
            ctx.arc(x, y, range, 0, Math.PI * 2);
            ctx.strokeStyle = color + '88';
            ctx.lineWidth = 1.5;
            ctx.setLineDash([4, 4]);
            ctx.stroke();
            ctx.restore();
        }

        // 스킬 이펙트
        if (skillEffect) {
            const alpha = Math.min(1, skillEffect.timer / 200);
            ctx.save();
            ctx.globalAlpha = alpha * 0.4;
            ctx.beginPath();
            ctx.arc(x, y, skillEffect.radius, 0, Math.PI * 2);
            ctx.fillStyle = skillEffect.color;
            ctx.fill();
            ctx.globalAlpha = alpha * 0.8;
            ctx.strokeStyle = skillEffect.color;
            ctx.lineWidth = 2;
            ctx.stroke();
            ctx.restore();
        }

        // 공격 flash
        if (shootEffect > 0 && this.target && !this.target.isDead) {
            ctx.save();
            ctx.strokeStyle = color;
            ctx.lineWidth   = 2;
            ctx.globalAlpha = shootEffect / 180;
            ctx.beginPath();
            ctx.moveTo(x, y);
            ctx.lineTo(this.target.x, this.target.y);
            ctx.stroke();
            ctx.restore();
        }

        // 타워 본체
        const r = 18;
        ctx.save();

        // 선택 강조
        if (isSelected) {
            ctx.shadowColor = color;
            ctx.shadowBlur  = 14;
        }

        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.fillStyle = color + '33';
        ctx.fill();
        ctx.strokeStyle = color;
        ctx.lineWidth   = isSelected ? 2.5 : 1.8;
        ctx.stroke();

        // 캐릭터 이미지 or 이름 첫 글자
        if (this.image && this.image.complete && this.image.naturalWidth > 0) {
            ctx.save();
            ctx.beginPath();
            ctx.arc(x, y, r - 2, 0, Math.PI * 2);
            ctx.clip();
            ctx.drawImage(this.image, x - r + 2, y - r + 2, (r - 2) * 2, (r - 2) * 2);
            ctx.restore();
        } else {
            ctx.fillStyle = '#fff';
            ctx.font      = `bold 13px 'Noto Sans KR'`;
            ctx.textAlign = 'center';
            ctx.textBaseline = 'middle';
            ctx.fillText(this.name.charAt(0), x, y);
        }

        ctx.restore();

        // 이름 레이블
        ctx.save();
        ctx.font      = '9px Noto Sans KR';
        ctx.fillStyle = '#e6edf3cc';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(this.name, x, y + r + 3);
        ctx.restore();

        // 쿨다운 바 (작은 아크)
        if (this.attackTimer > 0) {
            const frac = 1 - this.attackTimer / this.cooldown;
            ctx.save();
            ctx.strokeStyle = '#ffffff44';
            ctx.lineWidth   = 3;
            ctx.beginPath();
            ctx.arc(x, y, r + 4, -Math.PI / 2, -Math.PI / 2 + frac * Math.PI * 2);
            ctx.stroke();
            ctx.restore();
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 5. Enemy 클래스
// ─────────────────────────────────────────────────────────────────
const ENEMY_TYPES = [
    { name: '슬라임',    color: '#3fb950', hp: 60,   speed: 60,  reward: 10, radius: 12 },
    { name: '주황버섯',  color: '#f6a623', hp: 100,  speed: 55,  reward: 15, radius: 13 },
    { name: '뿔버섯',    color: '#e74c3c', hp: 160,  speed: 50,  reward: 20, radius: 14 },
    { name: '스텀프',    color: '#8e44ad', hp: 240,  speed: 45,  reward: 25, radius: 15 },
    { name: '드레이크',  color: '#c0392b', hp: 400,  speed: 40,  reward: 35, radius: 16 },
    { name: '다크드레이크', color:'#74b9ff',hp:600,  speed: 38,  reward: 45, radius: 17 },
    { name: '발록',      color: '#fd79a8', hp: 1000, speed: 35,  reward: 60, radius: 18 },
    { name: '자쿰',      color: '#fdcb6e', hp: 2000, speed: 30,  reward: 100,radius: 22 },
];

let enemyId = 0;

class Enemy {
    constructor(type, pathPoints) {
        this.id           = enemyId++;
        this.name         = type.name;
        this.color        = type.color;
        this.maxHp        = type.hp;
        this.hp           = type.hp;
        this.baseSpeed    = type.speed;
        this.speed        = type.speed;
        this.reward       = type.reward;
        this.radius       = type.radius;
        this.path         = pathPoints;
        this.pathIndex    = 1;
        this.pathProgress = 0;  // 0~1 (경로 전체 중 위치)
        this.x            = pathPoints[0].x;
        this.y            = pathPoints[0].y;
        this.isDead       = false;
        this.reachedEnd   = false;

        // 상태 효과
        this.slowTimer    = 0;
        this.freezeTimer  = 0;
        this.dotQueue     = [];  // [{damage, ticks, interval, timer}]
    }

    get totalPathLen() {
        let len = 0;
        for (let i = 1; i < this.path.length; i++) {
            len += Math.hypot(this.path[i].x - this.path[i-1].x,
                this.path[i].y - this.path[i-1].y);
        }
        return len;
    }

    update(dt) {
        if (this.isDead || this.reachedEnd) return;

        const realDt = dt * STATE.speed / 1000;

        // 상태 효과 타이머
        this.slowTimer   = Math.max(0, this.slowTimer   - dt * STATE.speed);
        this.freezeTimer = Math.max(0, this.freezeTimer - dt * STATE.speed);

        const frozen = this.freezeTimer > 0;
        const slowed = this.slowTimer   > 0;
        this.speed    = frozen ? 0
            : slowed ? this.baseSpeed * (1 - this._slowRatio)
                : this.baseSpeed;

        // DoT 처리
        for (const dot of this.dotQueue) {
            dot.timer -= dt * STATE.speed;
            if (dot.timer <= 0 && dot.ticks > 0) {
                this.takeDamage(dot.damage, true);
                dot.ticks--;
                dot.timer = dot.interval;
            }
        }
        this.dotQueue = this.dotQueue.filter(d => d.ticks > 0);

        if (frozen) return;

        // 이동
        const target = this.path[this.pathIndex];
        const dx = target.x - this.x;
        const dy = target.y - this.y;
        const d  = Math.hypot(dx, dy);
        const move = this.speed * realDt;

        if (move >= d) {
            this.x = target.x;
            this.y = target.y;
            this.pathIndex++;
            if (this.pathIndex >= this.path.length) {
                this.reachedEnd = true;
                return;
            }
        } else {
            this.x += (dx / d) * move;
            this.y += (dy / d) * move;
        }

        // 진행도 계산 (타겟 선정용)
        this.pathProgress = this.pathIndex / this.path.length
            + (1 - d / (this.totalPathLen / this.path.length)) * 0.01;
    }

    takeDamage(amount, isDot = false) {
        if (this.isDead) return;
        this.hp -= amount;
        if (this.hp <= 0) {
            this.hp     = 0;
            this.isDead = true;
        }
    }

    applySlow(ratio, duration) {
        this._slowRatio = ratio;
        this.slowTimer  = Math.max(this.slowTimer, duration);
    }

    applyFreeze(duration) {
        this.freezeTimer = Math.max(this.freezeTimer, duration);
        this.slowTimer   = 0;
    }

    applyDot(dmg, ticks, interval) {
        this.dotQueue.push({ damage: dmg, ticks, interval, timer: interval });
    }

    draw(ctx) {
        const { x, y, radius, color, hp, maxHp, freezeTimer, slowTimer } = this;
        if (this.isDead) return;

        const frozen = freezeTimer > 0;
        const slowed = slowTimer   > 0;

        ctx.save();

        // 상태 효과 외곽선
        if (frozen) {
            ctx.shadowColor = '#74b9ff';
            ctx.shadowBlur  = 8;
        } else if (slowed) {
            ctx.shadowColor = '#3498db';
            ctx.shadowBlur  = 5;
        }

        // 적 본체
        ctx.beginPath();
        ctx.arc(x, y, radius, 0, Math.PI * 2);
        ctx.fillStyle = frozen ? '#a8d8ff' : color;
        ctx.fill();
        ctx.strokeStyle = '#ffffff44';
        ctx.lineWidth   = 1;
        ctx.stroke();
        ctx.restore();

        // 이름
        ctx.save();
        ctx.font         = '9px Noto Sans KR';
        ctx.fillStyle    = '#e6edf3cc';
        ctx.textAlign    = 'center';
        ctx.textBaseline = 'bottom';
        ctx.fillText(this.name, x, y - radius - 1);
        ctx.restore();

        // HP 바
        const barW = radius * 2 + 4;
        const barH = 4;
        const bx   = x - barW / 2;
        const by   = y - radius - 12;
        ctx.fillStyle = '#30363d';
        ctx.fillRect(bx, by, barW, barH);
        ctx.fillStyle = hp / maxHp > 0.5 ? '#3fb950'
            : hp / maxHp > 0.25 ? '#f6a623' : '#f85149';
        ctx.fillRect(bx, by, barW * (hp / maxHp), barH);

        // DoT 표시 (초록 점)
        if (this.dotQueue.length > 0) {
            ctx.fillStyle = '#2ecc71';
            ctx.beginPath();
            ctx.arc(x + radius - 2, y - radius + 2, 3, 0, Math.PI * 2);
            ctx.fill();
        }
    }
}

// ─────────────────────────────────────────────────────────────────
// 6. 웨이브 정의
// ─────────────────────────────────────────────────────────────────
function buildWave(round) {
    const enemies = [];
    const typeIdx = Math.min(Math.floor((round - 1) / 2), ENEMY_TYPES.length - 1);
    const base    = ENEMY_TYPES[typeIdx];
    const hpMult  = 1 + (round - 1) * 0.18;
    const count   = 8 + round * 2;

    for (let i = 0; i < count; i++) {
        const type = {
            ...base,
            name:   round >= 10 ? '강화 ' + base.name : base.name,
            hp:     Math.round(base.hp * hpMult),
            reward: Math.round(base.reward * (1 + (round - 1) * 0.05)),
        };
        enemies.push({ type, delay: i * 900 });
    }

    // 보스 (5라운드 배수)
    if (round % 5 === 0) {
        const bossType = ENEMY_TYPES[Math.min(typeIdx + 1, ENEMY_TYPES.length - 1)];
        enemies.push({
            type: { ...bossType, name: '⚔️ BOSS ' + bossType.name, hp: bossType.hp * 4 * hpMult, reward: bossType.reward * 3 },
            delay: count * 900 + 1000,
        });
        addLog(`⚠️ 라운드 ${round}: 보스 등장!`, 'bad');
    }

    return enemies;
}

// ─────────────────────────────────────────────────────────────────
// 7. 게임 변수
// ─────────────────────────────────────────────────────────────────
let towerConfigs  = [];   // buildTowerConfig 결과 배열
let placedTowers  = [];   // Tower 인스턴스 배열
let activeEnemies = [];   // Enemy 인스턴스 배열
let waveQueue     = [];   // 현재 웨이브 스폰 대기열 [{type, delay}]
let waveTimer     = 0;    // 스폰 딜레이 타이머
let totalInWave   = 0;    // 현재 웨이브 총 적 수
let spawnedCount  = 0;    // 스폰된 적 수
let diedOrEndCount = 0;   // 처치 + 기지 도달 합산

let lastTime      = 0;
let rafId         = null;

// ─────────────────────────────────────────────────────────────────
// 8. 이미지 캐시
// ─────────────────────────────────────────────────────────────────
const _imgCache = {};
function loadImage(src) {
    if (!src) return null;
    if (_imgCache[src]) return _imgCache[src];
    const img = new Image();
    img.src = src;
    _imgCache[src] = img;
    return img;
}

// ─────────────────────────────────────────────────────────────────
// 9. HUD 업데이트
// ─────────────────────────────────────────────────────────────────
function updateHUD() {
    $('hud-round').textContent  = STATE.round;
    $('hud-gold').textContent   = STATE.gold;
    $('hud-kills').textContent  = STATE.kills;
    $('hud-hp-text').textContent = STATE.baseHp;

    const hpPct = (STATE.baseHp / STATE.BASE_HP) * 100;
    $('hud-hp-bar').style.width = hpPct + '%';
}

function updateWaveProgress() {
    if (totalInWave === 0) return;
    const done = diedOrEndCount;
    const pct  = (done / totalInWave) * 100;
    $('wave-prog-bar').style.width = pct + '%';
    $('wave-prog-text').textContent = `${done} / ${totalInWave}`;
}

// ─────────────────────────────────────────────────────────────────
// 10. 타워 목록 UI 렌더
// ─────────────────────────────────────────────────────────────────
function renderTowerList() {
    const list = $('tower-list');
    list.innerHTML = '';

    towerConfigs.forEach((cfg, idx) => {
        const placed = placedTowers.filter(t => t.name === cfg.name).length;
        const cost   = getTowerCost(cfg);

        const item = document.createElement('div');
        item.className = 'tower-item' + (placed ? ' placed' : '');
        item.dataset.idx = idx;
        item.innerHTML = `
            <div class="tower-icon" style="background:${cfg.color}33; border-color:${cfg.color}88">
                ${cfg.image
            ? `<img src="${cfg.image}" alt="${cfg.name}" />`
            : `<span style="color:${cfg.color}">${cfg.name.charAt(0)}</span>`}
            </div>
            <div class="tower-info">
                <div class="tower-name">${cfg.name}</div>
                <div class="tower-job">${cfg.jobClassName ?? cfg.job}</div>
            </div>
            <div class="tower-cost">${cost}G</div>
            ${placed ? `<div class="placed-badge">✓</div>` : ''}
        `;
        item.addEventListener('click', () => startPlacing(idx));
        list.appendChild(item);
    });
}

function getTowerCost(cfg) {
    // 기본 비용: 전투력 기반 (최소 30, 최대 200)
    const base = Math.round((cfg.baseDamage / 10) * 50 + 30);
    return Math.min(Math.max(base, 30), 200);
}

// ─────────────────────────────────────────────────────────────────
// 11. 배치 모드
// ─────────────────────────────────────────────────────────────────
function startPlacing(configIdx) {
    if (STATE.phase === 'WAVE') {
        addLog('웨이브 진행 중에는 타워를 배치할 수 없습니다.', 'bad');
        return;
    }
    const cfg  = towerConfigs[configIdx];
    const cost = getTowerCost(cfg);
    if (STATE.gold < cost) {
        addLog(`골드가 부족합니다. (필요: ${cost}G)`, 'bad');
        return;
    }
    STATE.placingConfig  = { cfg, cost };
    STATE.selectedTower  = null;
    canvas.style.cursor  = 'crosshair';
    $('place-hint').classList.add('visible');
    updateDetailPanel(null);
    addLog(`[${cfg.name}] 배치 위치를 클릭하세요`, 'info');
}

function cancelPlacing() {
    STATE.placingConfig = null;
    canvas.style.cursor = 'default';
    $('place-hint').classList.remove('visible');
}

canvas.addEventListener('click', e => {
    const rect = canvas.getBoundingClientRect();
    const mx   = e.clientX - rect.left;
    const my   = e.clientY - rect.top;

    // 배치 모드
    if (STATE.placingConfig) {
        const { cfg, cost } = STATE.placingConfig;

        // 경로 위 배치 불가
        if (isOnPath(mx, my)) {
            addLog('경로 위에는 배치할 수 없습니다.', 'bad');
            return;
        }
        // 기존 타워와 겹침 방지
        for (const t of placedTowers) {
            if (Math.hypot(mx - t.x, my - t.y) < 40) {
                addLog('다른 타워와 너무 가깝습니다.', 'bad');
                return;
            }
        }

        STATE.gold -= cost;
        const tower = new Tower(mx, my, cfg);
        placedTowers.push(tower);
        cancelPlacing();
        renderTowerList();
        updateHUD();
        addLog(`[${cfg.name}] 배치 완료 (-${cost}G)`, 'good');
        $('btn-wave').disabled = placedTowers.length < 3;
        return;
    }

    // 타워 선택
    let clicked = null;
    for (const t of placedTowers) {
        if (Math.hypot(mx - t.x, my - t.y) <= 22) { clicked = t; break; }
    }
    STATE.selectedTower = clicked;
    updateDetailPanel(clicked);
});

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') cancelPlacing();
});

// ─────────────────────────────────────────────────────────────────
// 12. 타워 상세 패널
// ─────────────────────────────────────────────────────────────────
function updateDetailPanel(tower) {
    const empty   = $('detail-empty');
    const content = $('detail-content');
    const noTower = $('skill-no-tower');
    const skillBtn = $('active-skill-btn');

    if (!tower) {
        empty.style.display   = '';
        content.style.display = 'none';
        noTower.style.display = '';
        skillBtn.style.display = 'none';
        return;
    }

    empty.style.display   = 'none';
    content.style.display = '';

    // 아이콘
    const iconEl = $('detail-icon');
    iconEl.style.background = tower.color + '33';
    iconEl.style.borderColor = tower.color;
    if (tower.image && tower.image.complete && tower.image.naturalWidth > 0) {
        iconEl.innerHTML = `<img src="${tower.image.src}" style="width:100%;height:100%;object-fit:cover;border-radius:50%">`;
    } else {
        iconEl.textContent = tower.name.charAt(0);
    }

    $('detail-name').textContent = tower.name;
    $('detail-job').textContent  = tower.config.jobClassName ?? tower.config.job;
    $('stat-dmg').textContent    = tower.damage.toFixed(1);
    $('stat-range').textContent  = tower.range + 'px';
    $('stat-cd').textContent     = fmtMs(tower.cooldown);
    $('stat-class').textContent  = tower.config.jobClassName ?? '—';

    // 패시브 태그
    const passWrap = $('passive-tag-wrap');
    passWrap.innerHTML = '';
    const passive = tower.config.passiveSkill;
    if (passive) {
        const tag = document.createElement('div');
        tag.className = 'passive-tag';
        tag.innerHTML = `<span class="passive-dot" style="background:${passive.skillColor ?? '#3fb950'}"></span>
                         ${passive.name} — ${passive.description}`;
        passWrap.appendChild(tag);
    }

    // 액티브 스킬 버튼
    const active = tower.config.activeSkill;
    if (active) {
        noTower.style.display  = 'none';
        skillBtn.style.display = '';
        $('skill-btn-name').textContent = active.name;
        $('skill-btn-desc').textContent = active.description ?? '';
        $('skill-icon-circle').style.background = (active.skillColor ?? '#f6a623') + '33';
        $('skill-icon-circle').style.borderColor = active.skillColor ?? '#f6a623';
        $('skill-icon-circle').textContent = getSkillEmoji(active.effectType);
        updateSkillCooldownUI(tower);
    } else {
        noTower.style.display   = '타워에 액티브 스킬이 없습니다';
        noTower.style.display   = '';
        noTower.textContent     = '이 타워에는 액티브 스킬이 없습니다';
        skillBtn.style.display  = 'none';
    }
}

function getSkillEmoji(effectType) {
    const map = {
        SINGLE_BURST: '💥', AOE_DAMAGE: '🔥',
        FREEZE_FIELD: '❄️', SLOW_FIELD: '🕸️',
        DOT_FIELD:    '☠️', BUFF_TOWERS: '✨',
    };
    return map[effectType] ?? '⚡';
}

function updateSkillCooldownUI(tower) {
    if (!tower) return;
    const active  = tower.config.activeSkill;
    if (!active)  return;
    const skillBtn = $('active-skill-btn');
    const overlay  = $('skill-cd-overlay');
    const cdText   = $('skill-cd-text');
    const remaining = tower.activeCooldown;

    if (remaining > 0) {
        skillBtn.disabled = true;
        overlay.classList.add('visible');
        cdText.textContent = (remaining / 1000).toFixed(1) + 's';
    } else {
        skillBtn.disabled = false;
        overlay.classList.remove('visible');
    }
}

// 액티브 스킬 버튼 클릭
$('active-skill-btn').addEventListener('click', () => {
    const tower = STATE.selectedTower;
    if (!tower) return;
    tower.useActive(activeEnemies);
    updateSkillCooldownUI(tower);
});

// ─────────────────────────────────────────────────────────────────
// 13. 맵 렌더
// ─────────────────────────────────────────────────────────────────
function drawMap(ctx) {
    // 배경
    ctx.fillStyle = '#0d1117';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // 격자
    ctx.save();
    ctx.strokeStyle = '#161b22';
    ctx.lineWidth   = 1;
    const GRID = 40;
    for (let x = 0; x < canvas.width; x += GRID) {
        ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, canvas.height); ctx.stroke();
    }
    for (let y = 0; y < canvas.height; y += GRID) {
        ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(canvas.width, y); ctx.stroke();
    }
    ctx.restore();

    // 경로
    const path = getPath();
    ctx.save();
    ctx.strokeStyle = '#30363d';
    ctx.lineWidth   = PATH_HALF * 2;
    ctx.lineCap     = 'round';
    ctx.lineJoin    = 'round';
    ctx.beginPath();
    ctx.moveTo(path[0].x, path[0].y);
    for (let i = 1; i < path.length; i++) ctx.lineTo(path[i].x, path[i].y);
    ctx.stroke();

    // 경로 중앙선
    ctx.strokeStyle = '#484f5844';
    ctx.lineWidth   = 2;
    ctx.setLineDash([8, 8]);
    ctx.beginPath();
    ctx.moveTo(path[0].x, path[0].y);
    for (let i = 1; i < path.length; i++) ctx.lineTo(path[i].x, path[i].y);
    ctx.stroke();
    ctx.restore();

    // 시작점 & 도착점
    ctx.save();
    // 시작
    ctx.fillStyle   = '#3fb95066';
    ctx.strokeStyle = '#3fb950';
    ctx.lineWidth   = 2;
    ctx.beginPath();
    ctx.arc(path[0].x, path[0].y, 18, 0, Math.PI * 2);
    ctx.fill(); ctx.stroke();
    ctx.fillStyle    = '#3fb950';
    ctx.font         = 'bold 11px Noto Sans KR';
    ctx.textAlign    = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('START', path[0].x, path[0].y);

    // 도착 (기지)
    const end = path[path.length - 1];
    ctx.fillStyle   = '#f8514966';
    ctx.strokeStyle = '#f85149';
    ctx.lineWidth   = 2;
    ctx.beginPath();
    ctx.arc(end.x, end.y, 20, 0, Math.PI * 2);
    ctx.fill(); ctx.stroke();
    ctx.fillStyle    = '#f85149';
    ctx.font         = 'bold 11px Noto Sans KR';
    ctx.textAlign    = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('BASE', end.x, end.y);
    ctx.restore();
}

// ─────────────────────────────────────────────────────────────────
// 14. 게임 루프
// ─────────────────────────────────────────────────────────────────
function gameLoop(timestamp) {
    const dt = Math.min(timestamp - lastTime, 50); // 최대 50ms (탭 비활성 방지)
    lastTime = timestamp;

    // 클리어
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    drawMap(ctx);

    // 타워 업데이트 & 렌더
    for (const tower of placedTowers) {
        tower.update(dt, activeEnemies);
        tower.draw(ctx);
    }

    // 선택된 타워 쿨다운 UI 갱신
    if (STATE.selectedTower) {
        updateSkillCooldownUI(STATE.selectedTower);
    }

    // 웨이브 진행 중
    if (STATE.phase === 'WAVE') {
        // 스폰
        if (waveQueue.length > 0) {
            waveTimer -= dt * STATE.speed;
            if (waveTimer <= 0) {
                const next = waveQueue.shift();
                const path = getPath();
                activeEnemies.push(new Enemy(next.type, path));
                spawnedCount++;
                // 다음 항목의 interval 만큼 대기 (없으면 0)
                waveTimer = waveQueue.length > 0 ? waveQueue[0].interval : 0;
            }
        }

        // 적 업데이트 & 렌더
        for (const enemy of activeEnemies) {
            if (enemy.isDead || enemy.reachedEnd) continue;
            enemy.update(dt);
        }

        // 기지 도달 처리
        for (const enemy of activeEnemies) {
            if (enemy.reachedEnd && !enemy._counted) {
                enemy._counted = true;
                STATE.baseHp--;
                diedOrEndCount++;
                updateHUD();
                updateWaveProgress();
                addLog(`⚠️ ${enemy.name}이(가) 기지에 도달! (HP: ${STATE.baseHp})`, 'bad');
                if (STATE.baseHp <= 0) {
                    triggerGameOver();
                    return;
                }
            }
            if (enemy.isDead && !enemy._counted) {
                enemy._counted = true;
                diedOrEndCount++;
                updateWaveProgress();
            }
        }

        for (const enemy of activeEnemies) enemy.draw(ctx);

        // 웨이브 클리어 판정
        const allSpawned = waveQueue.length === 0;
        const allDone    = activeEnemies.every(e => e.isDead || e.reachedEnd || e._counted);
        if (allSpawned && allDone && activeEnemies.length > 0) {
            onWaveClear();
        }
    } else {
        // IDLE / BETWEEN: 적 잔여 렌더
        for (const enemy of activeEnemies) enemy.draw(ctx);
    }

    // 배치 중 미리보기
    if (STATE.placingConfig) {
        // 마우스 따라다니는 미리보기는 mousemove로 처리
    }

    rafId = requestAnimationFrame(gameLoop);
}

// ─────────────────────────────────────────────────────────────────
// 15. 웨이브 시작 / 클리어 / 게임오버
// ─────────────────────────────────────────────────────────────────
function startWave() {
    if (placedTowers.length < 3) {
        addLog('타워를 최소 3개 이상 배치하세요.', 'bad');
        return;
    }
    if (STATE.phase === 'WAVE') return;

    STATE.round++;
    STATE.phase = 'WAVE';

    // 웨이브 생성
    const wave       = buildWave(STATE.round);
    totalInWave      = wave.length;
    spawnedCount     = 0;
    diedOrEndCount   = 0;
    activeEnemies    = [];

    // wave[i].delay 는 절대 시간(ms) — 각 항목 간 간격(interval)으로 변환해서 큐에 저장
    // 첫 번째 적은 즉시 스폰, 나머지는 이전 항목과의 차이(interval)만큼 대기
    waveQueue = wave.slice(1).map((w, i) => ({
        type:     w.type,
        interval: wave[i + 1].delay - wave[i].delay, // 앞 항목 기준 상대 간격
    }));
    waveTimer = 0; // 첫 스폰은 즉시

    const path  = getPath();
    activeEnemies.push(new Enemy(wave[0].type, path));
    spawnedCount = 1;

    $('btn-wave').disabled  = true;
    $('wave-prog-bar').style.width  = '0%';
    $('wave-prog-text').textContent = `0 / ${totalInWave}`;

    updateHUD();
    addLog(`── 라운드 ${STATE.round} 시작 ── (${totalInWave}마리)`, 'warn');
}

function onWaveClear() {
    STATE.phase = 'BETWEEN';
    activeEnemies = [];

    // 보너스 골드
    const bonus = 30 + STATE.round * 5;
    STATE.gold += bonus;
    updateHUD();
    addLog(`✅ 라운드 ${STATE.round} 클리어! (+${bonus}G 보너스)`, 'good');

    // 전체 클리어
    if (STATE.round >= STATE.MAX_ROUND) {
        triggerClear();
        return;
    }

    $('btn-wave').disabled = false;
    showCanvasMsg('웨이브 클리어!', '다음 웨이브를 준비하세요');
    setTimeout(() => hideCanvasMsg(), 2500);
}

function triggerGameOver() {
    STATE.phase = 'GAMEOVER';
    if (rafId) cancelAnimationFrame(rafId);
    rafId = null;

    $('go-round').textContent = STATE.round;
    showStateOverlay('gameover');
    addLog('💀 게임 오버', 'bad');
}

function triggerClear() {
    STATE.phase = 'CLEAR';
    $('cl-round').textContent = STATE.round;
    showStateOverlay('clear');
    addLog('🏆 전체 클리어!', 'good');
}

// ─────────────────────────────────────────────────────────────────
// 16. 오버레이 제어
// ─────────────────────────────────────────────────────────────────
function showStateOverlay(state) {
    $('game-state-overlay').classList.remove('hidden');
    $('state-loading').style.display  = state === 'loading'  ? '' : 'none';
    $('state-ready').style.display    = state === 'ready'    ? '' : 'none';
    $('state-gameover').style.display = state === 'gameover' ? '' : 'none';
    $('state-clear').style.display    = state === 'clear'    ? '' : 'none';
}

function hideStateOverlay() {
    $('game-state-overlay').classList.add('hidden');
}

function showCanvasMsg(title, body) {
    const msg = $('canvas-msg');
    $('canvas-msg-title').textContent = title;
    $('canvas-msg-body').textContent  = body;
    msg.style.display = '';
}
function hideCanvasMsg() {
    $('canvas-msg').style.display = 'none';
}

// ─────────────────────────────────────────────────────────────────
// 17. 결과 저장
// ─────────────────────────────────────────────────────────────────
async function saveResult() {
    const guildInfo = JSON.parse(sessionStorage.getItem('guildInfo') || '{}');
    const members   = JSON.parse(sessionStorage.getItem('selectedMembers') || '[]');

    const payload = {
        playerName:      guildInfo.masterName ?? '플레이어',
        guildName:       guildInfo.guildName  ?? '알 수 없음',
        worldName:       guildInfo.worldName  ?? '알 수 없음',
        clearRound:      STATE.round,
        selectedMembers: members.map(m => m.characterName),
    };

    try {
        $('btn-save-result')?.setAttribute('disabled', true);
        $('btn-save-clear')?.setAttribute('disabled', true);

        const res  = await fetch(`${API_BASE}/api/game/result`, {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body:    JSON.stringify(payload),
        });
        const body = await res.json();

        if (body.success) {
            sessionStorage.setItem('gameResult', JSON.stringify({
                ...payload,
                savedId: body.data,
            }));
            $('btn-go-result') && ($('btn-go-result').style.display = '');
            addLog('결과 저장 완료!', 'good');
        } else {
            addLog('결과 저장 실패: ' + body.message, 'bad');
        }
    } catch (e) {
        addLog('서버 연결 오류: 결과를 저장할 수 없습니다.', 'bad');
        console.error(e);
    }
}

// ─────────────────────────────────────────────────────────────────
// 18. 버튼 이벤트
// ─────────────────────────────────────────────────────────────────
$('btn-wave').addEventListener('click', () => {
    hideStateOverlay();
    startWave();
});

$('btn-close-overlay').addEventListener('click', () => {
    hideStateOverlay();
    $('btn-wave').disabled = placedTowers.length < 3;
    addLog('타워를 배치하고 웨이브를 시작하세요!', 'info');
});

$('btn-speed').addEventListener('click', () => {
    STATE.speed = STATE.speed === 1 ? 2 : 1;
    $('btn-speed').textContent = STATE.speed === 2 ? '⏩ x2' : '⏩ x1';
    $('btn-speed').classList.toggle('active', STATE.speed === 2);
});

$('btn-save-result')?.addEventListener('click', saveResult);
$('btn-save-clear')?.addEventListener('click', saveResult);
$('btn-go-result')?.addEventListener('click', () => { location.href = '/games/result'; });

// 마우스 미리보기
canvas.addEventListener('mousemove', e => {
    if (!STATE.placingConfig) return;
    const rect = canvas.getBoundingClientRect();
    const mx   = e.clientX - rect.left;
    const my   = e.clientY - rect.top;

    // 경로 위면 붉게, 아니면 기본
    const onPath = isOnPath(mx, my);
    canvas.style.cursor = onPath ? 'not-allowed' : 'crosshair';
});

// ─────────────────────────────────────────────────────────────────
// 19. 초기화 — sessionStorage → fetchJobConfig → buildTowerConfig
// ─────────────────────────────────────────────────────────────────
async function init() {
    const members = JSON.parse(sessionStorage.getItem('selectedMembers') || 'null');
    if (!members || members.length === 0) {
        location.href = '/guild/members';
        return;
    }

    showStateOverlay('loading');

    const bar  = $('init-bar');
    const text = $('init-text');

    for (let i = 0; i < members.length; i++) {
        const m   = members[i];
        text.textContent = `${m.characterName} (${i + 1} / ${members.length})`;
        bar.style.width  = ((i / members.length) * 100) + '%';

        const jobConfig = await fetchJobConfig(m.job);
        const cfg       = buildTowerConfig(m, jobConfig);
        towerConfigs.push(cfg);

        // 이미지 프리로드
        if (cfg.image) loadImage(cfg.image);
    }

    bar.style.width  = '100%';
    text.textContent = `${members.length} / ${members.length} 완료`;

    await new Promise(r => setTimeout(r, 400));

    renderTowerList();
    updateHUD();

    showStateOverlay('ready');

    // 게임 루프 시작
    lastTime = performance.now();
    rafId    = requestAnimationFrame(gameLoop);
}

init();