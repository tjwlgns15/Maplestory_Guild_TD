'use strict';

const API = 'http://localhost:8088';

// EffectType 한글 레이블
const EFFECT_LABELS = {
    DAMAGE_BOOST:       '데미지 증가 (패시브)',
    RANGE_BOOST:        '사거리 증가 (패시브)',
    ATTACK_SPEED_BOOST: '공격속도 증가 (패시브)',
    ON_HIT_SLOW:        '평타 슬로우 (패시브)',
    ON_HIT_DOT:         '평타 지속피해 (패시브)',
    AOE_DAMAGE:         '범위 데미지 (액티브)',
    SINGLE_BURST:       '단일 강타 (액티브)',
    SLOW_FIELD:         '범위 슬로우 (액티브)',
    FREEZE_FIELD:       '범위 빙결 (액티브)',
    DOT_FIELD:          '범위 지속피해 (액티브)',
    BUFF_TOWERS:        '타워 버프 (액티브)',
};

const PASSIVE_EFFECTS = ['DAMAGE_BOOST','RANGE_BOOST','ATTACK_SPEED_BOOST','ON_HIT_SLOW','ON_HIT_DOT'];
const ACTIVE_EFFECTS  = ['AOE_DAMAGE','SINGLE_BURST','SLOW_FIELD','FREEZE_FIELD','DOT_FIELD','BUFF_TOWERS'];

// ─────────────────────────────────────────────────────────
// 인증
// ─────────────────────────────────────────────────────────
let token = sessionStorage.getItem('adminToken');

async function login() {
    const username = document.getElementById('loginUser').value.trim();
    const password = document.getElementById('loginPass').value.trim();
    document.getElementById('loginErr').textContent = '';

    try {
        const res  = await fetch(`${API}/api/auth/login`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ username, password }),
        });
        const body = await res.json();
        if (!body.success) { document.getElementById('loginErr').textContent = body.message; return; }

        token = body.data.token;
        sessionStorage.setItem('adminToken', token);
        showDashboard(username);
    } catch {
        document.getElementById('loginErr').textContent = '서버에 연결할 수 없습니다.';
    }
}

function logout() {
    sessionStorage.removeItem('adminToken');
    token = null;
    document.getElementById('dashWrap').style.display = 'none';
    document.getElementById('loginWrap').style.display = 'flex';
}

function showDashboard(username) {
    document.getElementById('loginWrap').style.display  = 'none';
    document.getElementById('dashWrap').style.display   = 'block';
    document.getElementById('adminUser').textContent    = `👤 ${username}`;
    loadAll();
}

async function authFetch(url, options = {}) {
    const res = await fetch(url, {
        ...options,
        headers: { 'Authorization': `Bearer ${token}`, 'Content-Type': 'application/json', ...(options.headers || {}) },
    });
    if (res.status === 401) { logout(); throw new Error('인증이 만료되었습니다.'); }
    return res.json();
}

// ─────────────────────────────────────────────────────────
// 탭
// ─────────────────────────────────────────────────────────
document.querySelectorAll('.tab-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
        document.querySelectorAll('.tab-content').forEach(c => c.classList.remove('active'));
        btn.classList.add('active');
        document.getElementById(`tab-${btn.dataset.tab}`).classList.add('active');
    });
});

// ─────────────────────────────────────────────────────────
// 데이터 로드
// ─────────────────────────────────────────────────────────
let jobClasses = [], skills = [], jobs = [];

async function loadAll() {
    await loadJobClasses();
    await loadSkills();
    await loadJobs();
}

async function loadJobClasses() {
    const body = await authFetch(`${API}/api/admin/job-classes`);
    jobClasses = body.data || [];
    renderJobClassTable();
    refreshJobClassFilter();
}

async function loadSkills() {
    const kind   = document.getElementById('skillKindFilter')?.value || '';
    const url    = kind ? `${API}/api/admin/skills?kind=${kind}` : `${API}/api/admin/skills`;
    const body   = await authFetch(url);
    skills = body.data || [];
    renderSkillTable();
}

async function loadJobs() {
    const classId = document.getElementById('jobClassFilter')?.value || '';
    const url     = classId ? `${API}/api/admin/jobs?jobClassId=${classId}` : `${API}/api/admin/jobs`;
    const body    = await authFetch(url);
    jobs = body.data || [];
    renderJobTable();
}

// ─────────────────────────────────────────────────────────
// 직업군 렌더링
// ─────────────────────────────────────────────────────────
function renderJobClassTable() {
    const tbody = document.getElementById('jobClassTable');
    if (!jobClasses.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty">등록된 직업군이 없습니다</td></tr>'; return; }
    tbody.innerHTML = jobClasses.map(jc => `
    <tr>
      <td>${jc.name}</td>
      <td>${jc.range} px</td>
      <td>${jc.cooldown} ms</td>
      <td><span class="color-dot" style="background:${jc.color}"></span>${jc.color}</td>
      <td>
        <button class="btn btn-sm btn-edit" onclick="openJobClassModal(${jc.id})">수정</button>
        <button class="btn btn-sm btn-delete" onclick="deleteJobClass(${jc.id})">삭제</button>
      </td>
    </tr>`).join('');
}

function refreshJobClassFilter() {
    const sel = document.getElementById('jobClassFilter');
    const cur = sel.value;
    sel.innerHTML = '<option value="">전체</option>' +
        jobClasses.map(jc => `<option value="${jc.id}" ${cur == jc.id ? 'selected' : ''}>${jc.name}</option>`).join('');
}

// ─────────────────────────────────────────────────────────
// 스킬 렌더링
// ─────────────────────────────────────────────────────────
function renderSkillTable() {
    const tbody = document.getElementById('skillTable');
    if (!skills.length) { tbody.innerHTML = '<tr><td colspan="6" class="empty">등록된 스킬이 없습니다</td></tr>'; return; }
    tbody.innerHTML = skills.map(s => `
    <tr>
      <td>${s.name}</td>
      <td><span class="badge ${s.skillKind === 'PASSIVE' ? 'badge-passive' : 'badge-active'}">${s.skillKind === 'PASSIVE' ? '패시브' : '액티브'}</span></td>
      <td>${EFFECT_LABELS[s.effectType] || s.effectType}</td>
      <td>${s.effectValue}</td>
      <td>${s.cooldown != null ? s.cooldown + ' ms' : '-'}</td>
      <td>
        <button class="btn btn-sm btn-edit" onclick="openSkillModal(${s.id})">수정</button>
        <button class="btn btn-sm btn-delete" onclick="deleteSkill(${s.id})">삭제</button>
      </td>
    </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
// 직업 렌더링
// ─────────────────────────────────────────────────────────
function renderJobTable() {
    const tbody = document.getElementById('jobTable');
    if (!jobs.length) { tbody.innerHTML = '<tr><td colspan="5" class="empty">등록된 직업이 없습니다</td></tr>'; return; }
    tbody.innerHTML = jobs.map(j => `
    <tr>
      <td>${j.name}</td>
      <td>${j.jobClassName}</td>
      <td>${j.passiveSkill ? j.passiveSkill.name : '<span style="color:var(--muted)">없음</span>'}</td>
      <td>${j.activeSkill  ? j.activeSkill.name  : '<span style="color:var(--muted)">없음</span>'}</td>
      <td>
        <button class="btn btn-sm btn-edit" onclick="openJobModal(${j.id})">수정</button>
        <button class="btn btn-sm btn-delete" onclick="deleteJob(${j.id})">삭제</button>
      </td>
    </tr>`).join('');
}

// ─────────────────────────────────────────────────────────
// 모달
// ─────────────────────────────────────────────────────────
let modalConfirmFn = null;

function openModal(title, bodyHtml, onConfirm) {
    document.getElementById('modalTitle').textContent   = title;
    document.getElementById('modalBody').innerHTML      = bodyHtml;
    document.getElementById('modalConfirm').onclick     = onConfirm;
    document.getElementById('modalBg').classList.add('open');
}

function closeModal() {
    document.getElementById('modalBg').classList.remove('open');
}

document.getElementById('modalBg').addEventListener('click', e => {
    if (e.target === document.getElementById('modalBg')) closeModal();
});

// ─────────────────────────────────────────────────────────
// 직업군 CRUD
// ─────────────────────────────────────────────────────────
function openJobClassModal(id = null) {
    const jc   = id ? jobClasses.find(j => j.id === id) : null;
    const html = `
    <div class="form-group"><label>직업군 이름</label>
      <input id="f_jcName" value="${jc?.name || ''}" ${jc ? 'readonly' : ''} placeholder="예: 전사" />
    </div>
    <div class="form-group"><label>사거리 (px)</label>
      <input id="f_jcRange" type="number" value="${jc?.range ?? ''}" placeholder="예: 90" />
    </div>
    <div class="form-group"><label>쿨다운 (ms)</label>
      <input id="f_jcCooldown" type="number" value="${jc?.cooldown ?? ''}" placeholder="예: 2000" />
    </div>
    <div class="form-group"><label>색상 (hex)</label>
      <input id="f_jcColor" value="${jc?.color || '#c0392b'}" placeholder="#c0392b" />
    </div>
    <p class="err-text" id="jcErr"></p>`;

    openModal(jc ? '직업군 수정' : '직업군 추가', html, async () => {
        const payload = {
            name:     document.getElementById('f_jcName').value.trim(),
            range:    Number(document.getElementById('f_jcRange').value),
            cooldown: Number(document.getElementById('f_jcCooldown').value),
            color:    document.getElementById('f_jcColor').value.trim(),
        };
        try {
            const method = jc ? 'PUT' : 'POST';
            const url    = jc ? `${API}/api/admin/job-classes/${id}` : `${API}/api/admin/job-classes`;
            const body   = await authFetch(url, { method, body: JSON.stringify(payload) });
            if (!body.success) { document.getElementById('jcErr').textContent = body.message; return; }
            closeModal(); loadJobClasses();
        } catch (e) { document.getElementById('jcErr').textContent = e.message; }
    });
}

async function deleteJobClass(id) {
    if (!confirm('삭제하시겠습니까? 소속 직업이 있으면 삭제되지 않습니다.')) return;
    const body = await authFetch(`${API}/api/admin/job-classes/${id}`, { method: 'DELETE' });
    if (!body.success) { alert(body.message); return; }
    loadJobClasses();
}

// ─────────────────────────────────────────────────────────
// 스킬 CRUD
// ─────────────────────────────────────────────────────────
function buildEffectOptions(kind, selected) {
    const list = kind === 'PASSIVE' ? PASSIVE_EFFECTS : kind === 'ACTIVE' ? ACTIVE_EFFECTS : [...PASSIVE_EFFECTS, ...ACTIVE_EFFECTS];
    return list.map(e => `<option value="${e}" ${e === selected ? 'selected' : ''}>${EFFECT_LABELS[e]}</option>`).join('');
}

function openSkillModal(id = null) {
    const sk   = id ? skills.find(s => s.id === id) : null;
    const html = `
    <div class="form-group"><label>스킬 이름</label>
      <input id="f_skName" value="${sk?.name || ''}" placeholder="예: 다크 임팩트" />
    </div>
    <div class="form-group"><label>종류</label>
      <select id="f_skKind" onchange="onSkillKindChange()">
        <option value="PASSIVE" ${sk?.skillKind === 'PASSIVE' ? 'selected' : ''}>패시브</option>
        <option value="ACTIVE"  ${sk?.skillKind === 'ACTIVE'  ? 'selected' : ''}>액티브</option>
      </select>
    </div>
    <div class="form-group"><label>효과 타입</label>
      <select id="f_skEffect">${buildEffectOptions(sk?.skillKind || 'PASSIVE', sk?.effectType)}</select>
    </div>
    <div class="form-group"><label>효과값 <small style="color:var(--muted)">(배율: 0.2 = 20%, 범위: px)</small></label>
      <input id="f_skValue" type="number" step="0.01" value="${sk?.effectValue ?? ''}" placeholder="예: 0.2" />
    </div>
    <div class="form-group"><label>효과 지속시간 (ms, 선택)</label>
      <input id="f_skDuration" type="number" value="${sk?.effectDuration ?? ''}" placeholder="예: 2000" />
    </div>
    <div class="form-group"><label>AOE 반경 (px, 선택)</label>
      <input id="f_skAoe" type="number" value="${sk?.aoeRadius ?? ''}" placeholder="예: 80" />
    </div>
    <div class="form-group"><label>스킬 색상 (hex)</label>
      <input id="f_skColor" value="${sk?.skillColor || '#e74c3c'}" placeholder="#e74c3c" />
    </div>
    <!-- 액티브 전용 -->
    <div id="activeFields" class="${sk?.skillKind === 'ACTIVE' ? '' : 'conditional'}">
      <div class="form-group"><label>쿨타임 (ms) <span style="color:var(--danger)">*</span></label>
        <input id="f_skCooldown" type="number" value="${sk?.cooldown ?? ''}" placeholder="예: 8000" />
      </div>
      <div class="form-group"><label>DOT 틱 횟수 (선택)</label>
        <input id="f_skDotTicks" type="number" value="${sk?.dotTicks ?? ''}" placeholder="예: 3" />
      </div>
      <div class="form-group"><label>DOT 틱 간격 (ms, 선택)</label>
        <input id="f_skDotInterval" type="number" value="${sk?.dotInterval ?? ''}" placeholder="예: 600" />
      </div>
    </div>
    <div class="form-group"><label>설명</label>
      <textarea id="f_skDesc" rows="2" placeholder="스킬 설명">${sk?.description || ''}</textarea>
    </div>
    <p class="err-text" id="skErr"></p>`;

    openModal(sk ? '스킬 수정' : '스킬 추가', html, async () => {
        const kind = document.getElementById('f_skKind').value;
        const payload = {
            name:           document.getElementById('f_skName').value.trim(),
            skillKind:      kind,
            effectType:     document.getElementById('f_skEffect').value,
            effectValue:    Number(document.getElementById('f_skValue').value),
            effectDuration: num('f_skDuration'),
            aoeRadius:      num('f_skAoe'),
            skillColor:     document.getElementById('f_skColor').value.trim(),
            cooldown:       num('f_skCooldown'),
            dotTicks:       num('f_skDotTicks'),
            dotInterval:    num('f_skDotInterval'),
            description:    document.getElementById('f_skDesc').value.trim(),
        };
        try {
            const method = sk ? 'PUT' : 'POST';
            const url    = sk ? `${API}/api/admin/skills/${id}` : `${API}/api/admin/skills`;
            const body   = await authFetch(url, { method, body: JSON.stringify(payload) });
            if (!body.success) { document.getElementById('skErr').textContent = body.message; return; }
            closeModal(); loadSkills();
        } catch (e) { document.getElementById('skErr').textContent = e.message; }
    });
}

function onSkillKindChange() {
    const kind = document.getElementById('f_skKind').value;
    document.getElementById('activeFields').className = kind === 'ACTIVE' ? '' : 'conditional';
    document.getElementById('f_skEffect').innerHTML = buildEffectOptions(kind, '');
}

async function deleteSkill(id) {
    if (!confirm('스킬을 삭제하시겠습니까?')) return;
    const body = await authFetch(`${API}/api/admin/skills/${id}`, { method: 'DELETE' });
    if (!body.success) { alert(body.message); return; }
    loadSkills();
}

// ─────────────────────────────────────────────────────────
// 직업 CRUD
// ─────────────────────────────────────────────────────────
function openJobModal(id = null) {
    const job = id ? jobs.find(j => j.id === id) : null;

    const jcOptions = jobClasses.map(jc =>
        `<option value="${jc.id}" ${jc.id === job?.jobClassId ? 'selected' : ''}>${jc.name}</option>`).join('');

    const passiveList = skills.filter(s => s.skillKind === 'PASSIVE');
    const activeList  = skills.filter(s => s.skillKind === 'ACTIVE');

    const passiveOptions = `<option value="">없음</option>` +
        passiveList.map(s => `<option value="${s.id}" ${s.id === job?.passiveSkill?.id ? 'selected' : ''}>${s.name}</option>`).join('');
    const activeOptions  = `<option value="">없음</option>` +
        activeList.map(s => `<option value="${s.id}" ${s.id === job?.activeSkill?.id ? 'selected' : ''}>${s.name}</option>`).join('');

    const html = `
    <div class="form-group"><label>직업 이름</label>
      <input id="f_jName" value="${job?.name || ''}" placeholder="예: 히어로" />
    </div>
    <div class="form-group"><label>직업군</label>
      <select id="f_jClass">${jcOptions}</select>
    </div>
    <div class="form-group"><label>패시브 스킬</label>
      <select id="f_jPassive">${passiveOptions}</select>
    </div>
    <div class="form-group"><label>액티브 스킬</label>
      <select id="f_jActive">${activeOptions}</select>
    </div>
    <p class="err-text" id="jobErr"></p>`;

    openModal(job ? '직업 수정' : '직업 추가', html, async () => {
        const payload = {
            name:          document.getElementById('f_jName').value.trim(),
            jobClassId:    Number(document.getElementById('f_jClass').value),
            passiveSkillId: numOrNull('f_jPassive'),
            activeSkillId:  numOrNull('f_jActive'),
        };
        try {
            const method = job ? 'PUT' : 'POST';
            const url    = job ? `${API}/api/admin/jobs/${id}` : `${API}/api/admin/jobs`;
            const body   = await authFetch(url, { method, body: JSON.stringify(payload) });
            if (!body.success) { document.getElementById('jobErr').textContent = body.message; return; }
            closeModal(); loadJobs();
        } catch (e) { document.getElementById('jobErr').textContent = e.message; }
    });
}

async function deleteJob(id) {
    if (!confirm('직업을 삭제하시겠습니까?')) return;
    const body = await authFetch(`${API}/api/admin/jobs/${id}`, { method: 'DELETE' });
    if (!body.success) { alert(body.message); return; }
    loadJobs();
}

// ─────────────────────────────────────────────────────────
// 유틸
// ─────────────────────────────────────────────────────────
const num        = id => { const v = document.getElementById(id)?.value; return v ? Number(v) : null; };
const numOrNull  = id => { const v = document.getElementById(id)?.value; return v ? Number(v) : null; };

// ─────────────────────────────────────────────────────────
// 초기화
// ─────────────────────────────────────────────────────────
if (token) {
    // 저장된 토큰이 있으면 바로 대시보드 진입
    showDashboard(sessionStorage.getItem('adminUser') || 'admin');
}

// 엔터키 로그인
document.getElementById('loginPass').addEventListener('keydown', e => {
    if (e.key === 'Enter') login();
});