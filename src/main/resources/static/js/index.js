const API_BASE = 'http://localhost:8088';

document.getElementById('guildForm').addEventListener('submit', async (e) => {
    e.preventDefault();

    const apiKey    = document.getElementById('apiKey').value.trim();
    const guildName = document.getElementById('guildName').value.trim();
    const worldName = document.getElementById('worldName').value;
    const errorMsg  = document.getElementById('errorMsg');
    const submitBtn = e.target.querySelector('button[type="submit"]');

    errorMsg.textContent = '';
    submitBtn.disabled = true;
    submitBtn.textContent = '조회 중...';

    try {
        const res = await fetch(
            `${API_BASE}/api/guild/info?guildName=${encodeURIComponent(guildName)}&worldName=${encodeURIComponent(worldName)}&apiKey=${encodeURIComponent(apiKey)}`
        );
        const body = await res.json();

        if (!body.success) {
            errorMsg.textContent = body.message || '길드 조회에 실패했습니다.';
            return;
        }

        // 다음 페이지에서 사용할 데이터를 sessionStorage에 저장
        sessionStorage.setItem('apiKey',    apiKey);
        sessionStorage.setItem('guildName', guildName);
        sessionStorage.setItem('worldName', worldName);
        sessionStorage.setItem('guildInfo', JSON.stringify(body.data));

        location.href = '/guild/members';
    } catch (err) {
        errorMsg.textContent = '서버에 연결할 수 없습니다.';
    } finally {
        submitBtn.disabled = false;
        submitBtn.textContent = '길드 조회';
    }
});