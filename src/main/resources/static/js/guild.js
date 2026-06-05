const API_BASE   = 'http://localhost:8088';
const MIN_SELECT = 3;
const MAX_SELECT = 10;

const apiKey    = sessionStorage.getItem('apiKey');
const guildName = sessionStorage.getItem('guildName');
const worldName = sessionStorage.getItem('worldName');
const guildInfo = JSON.parse(sessionStorage.getItem('guildInfo') || 'null');

if (!apiKey || !guildName || !worldName) location.href = 'index.html';

// ── 상태 ──────────────────────────────────────────
let members      = [];   // 로딩 완료된 캐릭터 목록
let selectedNames = new Set();
let currentSort  = 'combatPower';

// ── 길드 헤더 렌더 ────────────────────────────────
document.getElementById('guildInfo').textContent =
    guildInfo ? `${guildInfo.worldName} · ${guildInfo.guildName} (Lv.${guildInfo.guildLevel}) | 마스터: ${guildInfo.masterName}` : '';

// ── SSE 연결 ──────────────────────────────────────
const url = `${API_BASE}/api/guild/members/stream`
    + `?guildName=${encodeURIComponent(guildName)}`
    + `&worldName=${encodeURIComponent(worldName)}`
    + `&apiKey=${encodeURIComponent(apiKey)}`;

const evtSource = new EventSource(url);

/**
 * ⚠️ 브라우저 EventSource는 커스텀 헤더를 지원하지 않습니다.
 * API Key를 쿼리 파라미터로 전달하도록 백엔드 GuildController를
 * @RequestHeader → @RequestParam 으로 변경해야 합니다.
 */

evtSource.addEventListener('member', (e) => {
    const data = JSON.parse(e.data);

    // 진행률 업데이트
    const pct = Math.round((data.loaded / data.total) * 100);
    document.getElementById('progressFill').style.width = pct + '%';
    document.getElementById('progressText').textContent =
        `길드원 정보 불러오는 중... ${data.loaded} / ${data.total}`;

    // 캐릭터 카드 추가
    if (data.member) {
        members.push(data.member);
        renderCard(data.member);
    }

    // 완료 처리
    if (data.done) {
        evtSource.close();
        document.getElementById('progressWrap').style.display = 'none';
        sortAndRender();
    }
});

evtSource.addEventListener('error', (e) => {
    evtSource.close();
    document.getElementById('progressText').textContent = '로딩 중 오류가 발생했습니다.';
});

// ── 카드 렌더링 ───────────────────────────────────
function renderCard(member) {
    const grid = document.getElementById('memberGrid');
    const card = document.createElement('div');
    card.className = 'member-card';
    card.dataset.name = member.characterName;
    card.innerHTML = `
    <img src="${member.characterImage || 'https://via.placeholder.com/64'}" alt="${member.characterName}" />
    <div class="m-name">${member.characterName}</div>
    <div class="m-job">${member.job}</div>
    <div class="m-cp">⚔️ ${member.combatPower.toLocaleString()}</div>
    <div class="m-level">Lv. ${member.level}</div>
  `;
    card.addEventListener('click', () => toggleSelect(card, member.characterName));
    grid.appendChild(card);
}

function sortAndRender() {
    const compareFn = {
        combatPower: (a, b) => b.combatPower - a.combatPower,
        level:       (a, b) => b.level - a.level,
        name:        (a, b) => a.characterName.localeCompare(b.characterName),
    }[currentSort];

    members.sort(compareFn);

    const grid = document.getElementById('memberGrid');
    grid.innerHTML = '';
    members.forEach(renderCard);

    // 선택 상태 복원
    selectedNames.forEach(name => {
        const card = grid.querySelector(`[data-name="${name}"]`);
        if (card) card.classList.add('selected');
    });
}

// ── 선택 토글 ─────────────────────────────────────
function toggleSelect(card, name) {
    if (selectedNames.has(name)) {
        selectedNames.delete(name);
        card.classList.remove('selected');
    } else {
        if (selectedNames.size >= MAX_SELECT) return;
        selectedNames.add(name);
        card.classList.add('selected');
    }
    updateSelectUI();
}

function updateSelectUI() {
    const count = selectedNames.size;
    document.getElementById('selectedCount').textContent = count;
    document.getElementById('startBtn').disabled = count < MIN_SELECT;
}

// ── 정렬 버튼 ─────────────────────────────────────
document.querySelectorAll('.btn-sort').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.btn-sort').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        currentSort = btn.dataset.sort;
        sortAndRender();
    });
});

// ── 게임 시작 ─────────────────────────────────────
document.getElementById('startBtn').addEventListener('click', () => {
    const selected = members.filter(m => selectedNames.has(m.characterName));
    sessionStorage.setItem('selectedMembers', JSON.stringify(selected));
    location.href = '/games';
});