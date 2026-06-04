const API_BASE = 'http://localhost:8088';

const resultData = JSON.parse(sessionStorage.getItem('gameResult') || 'null');
if (!resultData) location.href = 'index.html';

// ── 내 결과 렌더 ──────────────────────────────────
document.getElementById('resPlayer').textContent = resultData.playerName;
document.getElementById('resGuild').textContent  = `${resultData.worldName} · ${resultData.guildName}`;
document.getElementById('resRound').textContent  = `${resultData.clearRound} 라운드`;

const towerWrap = document.getElementById('resTowers');
resultData.selectedMembers.forEach(name => {
    const chip = document.createElement('span');
    chip.className = 'tower-chip';
    chip.textContent = name;
    towerWrap.appendChild(chip);
});

// ── 랭킹 조회 ─────────────────────────────────────
async function loadRanking() {
    try {
        const res  = await fetch(`${API_BASE}/api/game/ranking`);
        const body = await res.json();
        if (!body.success) return;

        const tbody = document.getElementById('rankingBody');
        body.data.forEach((row, idx) => {
            const tr = document.createElement('tr');
            if (row.id === resultData.savedId) tr.classList.add('my-row');
            tr.innerHTML = `
                <td>${idx + 1}</td>
                <td>${row.playerName}</td>
                <td>${row.worldName} · ${row.guildName}</td>
                <td>${row.clearRound} 라운드</td>
                <td>${new Date(row.playedAt).toLocaleString('ko-KR')}</td>
              `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('랭킹 조회 실패', err);
    }
}

loadRanking();