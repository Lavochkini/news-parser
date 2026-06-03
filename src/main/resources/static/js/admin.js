/* ═══════════════════════════════════════
   ADMIN PANEL
═══════════════════════════════════════ */

/* ─── State ─── */
let _usersPage      = 0;
let _newsPage       = 0;
let _analysesPage   = 0;
let _logsPage       = 0;
let _userSearch     = '';
let _newsSearch     = '';
let _analysesSearch = '';
let _newsSource     = '';
let _confirmCb      = null;
let _sourceChart    = null;
let _topChart       = null;
let _avgChart       = null;

/* ─── Selection sets for bulk ops ─── */
let _selectedUsers    = new Set();
let _selectedNews     = new Set();
let _selectedAnalyses = new Set();

/* ═══════════════════════════════════════
   INIT
═══════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.isLoggedIn()) { window.location.href = '/login'; return; }
    if (Auth.getRole() !== 'ADMIN') {
        document.getElementById('accessDenied').style.display = 'block';
        renderNav();
        return;
    }
    document.getElementById('adminContent').style.display = 'block';
    renderNav();
    loadOverview();
});

function renderNav() {
    const nav = document.getElementById('navUser');
    if (!nav) return;
    nav.innerHTML = Auth.isLoggedIn()
        ? `<span class="navbar__username">${escHtml(Auth.getUsername())}</span>
           <button class="btn btn--ghost" onclick="Auth.logout()">Вийти</button>`
        : `<a href="/login" class="btn btn--ghost">Увійти</a>
           <a href="/register" class="btn btn--outline">Реєстрація</a>`;
}

/* ═══════════════════════════════════════
   TABS
═══════════════════════════════════════ */
const PANELS = ['overview','users','news','analytics','analyses','parsing','logs'];

function switchTab(name) {
    PANELS.forEach(p => {
        document.getElementById('panel' + cap(p)).style.display = p === name ? 'block' : 'none';
        document.getElementById('tab'   + cap(p)).classList.toggle('an-tab--active', p === name);
    });
    if (name === 'users'    && _usersPage    === 0) loadUsers();
    if (name === 'news'     && _newsPage     === 0) loadNews();
    if (name === 'analyses' && _analysesPage === 0) loadAnalyses();
    if (name === 'analytics') loadAnalytics();
    if (name === 'logs')      loadLogs(0);
}

function cap(s) { return s.charAt(0).toUpperCase() + s.slice(1); }

/* ═══════════════════════════════════════
   OVERVIEW
═══════════════════════════════════════ */
function loadOverview() {
    apiFetch('/api/admin/stats').then(d => {
        setText('ov-users',    d.totalUsers);
        setText('ov-news',     d.totalNews);
        setText('ov-analyses', d.totalAnalyses);
        setText('ov-avg',      d.avgScore + ' / 100');
        renderSourceChart(d.bySource || {});
    });
}

function renderSourceChart(bySource) {
    const ctx = document.getElementById('sourceChart').getContext('2d');
    if (_sourceChart) _sourceChart.destroy();
    const LABELS = { NEWS_API:'NewsAPI', GUARDIAN:'Guardian', GNEWS:'GNews',
                     REDDIT:'Reddit', DATASET:'Dataset', MANUAL:'Manual' };
    const COLORS  = ['#58a6ff','#dc2626','#059669','#ff4500','#818cf8','#d97706'];
    const keys    = Object.keys(bySource);
    _sourceChart = new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: keys.map(k => LABELS[k] || k),
            datasets: [{ data: keys.map(k => bySource[k]),
                         backgroundColor: COLORS.slice(0, keys.length),
                         borderColor: '#161b22', borderWidth: 2 }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { position: 'right', labels: { color: '#8b949e', boxWidth: 12 } } }
        }
    });
}

/* ═══════════════════════════════════════
   USERS
═══════════════════════════════════════ */
let _userSearchTimer = null;
function searchUsers() {
    clearTimeout(_userSearchTimer);
    _userSearchTimer = setTimeout(() => {
        _usersPage  = 0;
        _userSearch = document.getElementById('userSearch').value.trim();
        clearUserSelection();
        loadUsers();
    }, 350);
}

function loadUsers(page = _usersPage) {
    _usersPage = page;
    const params = new URLSearchParams({ search: _userSearch, page, size: 15 });
    document.getElementById('usersLoading').style.display = 'block';
    document.getElementById('usersBody').innerHTML = '';
    document.getElementById('usersEmpty').style.display = 'none';

    apiFetch('/api/admin/users?' + params).then(d => {
        document.getElementById('usersLoading').style.display = 'none';
        document.getElementById('userTotal').textContent = `Всього: ${d.totalElements}`;
        if (!d.content?.length) { document.getElementById('usersEmpty').style.display = 'block'; return; }
        document.getElementById('usersBody').innerHTML = d.content.map(renderUserRow).join('');
        // Restore checkbox state for current page items
        d.content.forEach(u => {
            const cb = document.querySelector(`.user-cb[data-id="${u.id}"]`);
            if (cb) cb.checked = _selectedUsers.has(u.id);
        });
        renderPagination('usersPagination', d.page, d.totalPages, p => loadUsers(p));
    });
}

function renderUserRow(u) {
    const initials  = (u.username || '?').slice(0, 2).toUpperCase();
    const roleBadge = u.role === 'ADMIN'
        ? `<span class="admin-badge admin-badge--admin">ADMIN</span>`
        : `<span class="admin-badge admin-badge--user">USER</span>`;
    const date = u.createdAt ? new Date(u.createdAt).toLocaleDateString('uk') : '—';
    return `<tr>
        <td style="padding:10px 8px 10px 16px">
            <input type="checkbox" class="user-cb" data-id="${u.id}"
                   style="cursor:pointer;width:14px;height:14px"
                   onchange="toggleUser('${u.id}', this)">
        </td>
        <td>
            <div style="display:flex;align-items:center;gap:10px">
                <div class="admin-avatar">${initials}</div>
                <strong style="cursor:pointer;color:var(--accent)" onclick="openUserDetails('${u.id}')">${escHtml(u.username)}</strong>
            </div>
        </td>
        <td style="color:var(--text-muted)">${escHtml(u.email)}</td>
        <td>
            ${roleBadge}
            <select class="admin-role-select" onchange="changeRole('${u.id}', this.value)">
                <option value="USER"  ${u.role==='USER' ?'selected':''}>USER</option>
                <option value="ADMIN" ${u.role==='ADMIN'?'selected':''}>ADMIN</option>
            </select>
        </td>
        <td style="color:var(--text-muted);font-size:12px">${date}</td>
        <td style="color:var(--text-muted)">${u.favoriteCount}</td>
        <td style="text-align:right;white-space:nowrap">
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px"
                    onclick="openUserDetails('${u.id}')">Деталі</button>
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px"
                    onclick="openEditModal('${u.id}','${escHtml(u.username)}','${escHtml(u.email)}')">Редагувати</button>
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px;color:var(--danger)"
                    onclick="confirmDelete('user','${u.id}','${escHtml(u.username)}')">Видалити</button>
        </td>
    </tr>`;
}

/* ─── User selection ─── */
function toggleUser(id, cb) {
    if (cb.checked) _selectedUsers.add(id);
    else _selectedUsers.delete(id);
    updateBulkBar('users');
}

function toggleAllUsers(cb) {
    document.querySelectorAll('.user-cb').forEach(c => {
        c.checked = cb.checked;
        if (cb.checked) _selectedUsers.add(c.dataset.id);
        else _selectedUsers.delete(c.dataset.id);
    });
    updateBulkBar('users');
}

function clearUserSelection() {
    _selectedUsers.clear();
    document.querySelectorAll('.user-cb').forEach(c => c.checked = false);
    const all = document.getElementById('usersAllCb');
    if (all) all.checked = false;
    updateBulkBar('users');
}

function bulkDeleteUsers() {
    if (!_selectedUsers.size) return;
    const ids = [..._selectedUsers];
    confirmDeleteBulk('users', ids, `${ids.length} користувачів`);
}

/* ─── User details ─── */
function openUserDetails(id) {
    const modal = document.getElementById('userDetailsModal');
    document.getElementById('userDetailsContent').innerHTML =
        '<div style="text-align:center;padding:32px;color:var(--text-muted)">Завантаження…</div>';
    modal.style.display = 'flex';

    apiFetch(`/api/admin/users/${id}/details`).then(d => {
        const u        = d.user;
        const analyses = d.recentAnalyses || [];
        const reg      = u.createdAt ? new Date(u.createdAt).toLocaleDateString('uk') : '—';

        document.getElementById('userDetailsContent').innerHTML = `
            <div style="display:grid;grid-template-columns:1fr 1fr;gap:14px;margin-bottom:20px">
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">USERNAME</div><strong>${escHtml(u.username)}</strong></div>
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">EMAIL</div><span style="font-size:13px">${escHtml(u.email)}</span></div>
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">РОЛЬ</div>${u.role==='ADMIN'?'<span class="admin-badge admin-badge--admin">ADMIN</span>':'<span class="admin-badge admin-badge--user">USER</span>'}</div>
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">РЕЄСТРАЦІЯ</div><span style="font-size:13px">${reg}</span></div>
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">АНАЛІЗІВ</div><strong>${d.totalAnalyses}</strong></div>
                <div><div style="font-size:11px;color:var(--text-muted);margin-bottom:3px">ОБРАНИХ</div><strong>${u.favoriteCount}</strong></div>
            </div>
            ${analyses.length ? `
                <div style="border-top:1px solid var(--border);padding-top:16px">
                    <div style="font-size:11px;font-weight:600;color:var(--text-muted);letter-spacing:.06em;margin-bottom:10px">ОСТАННІ АНАЛІЗИ</div>
                    ${analyses.map(a => {
                        const date  = a.analyzedAt ? new Date(a.analyzedAt).toLocaleDateString('uk') : '—';
                        const color = a.credibilityScore >= 60 ? '#3fb950' : a.credibilityScore >= 40 ? '#d29922' : '#f85149';
                        return `<div style="display:flex;justify-content:space-between;align-items:center;
                                            padding:7px 0;border-bottom:1px solid var(--border)">
                            <div style="font-size:13px;flex:1;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;margin-right:12px">
                                ${escHtml(a.newsTitle || a.newsItemId)}
                            </div>
                            <div style="display:flex;align-items:center;gap:10px;flex-shrink:0">
                                <strong style="color:${color}">${a.credibilityScore}</strong>
                                <span style="font-size:11px;color:var(--text-muted)">${date}</span>
                            </div>
                        </div>`;
                    }).join('')}
                </div>
            ` : '<p style="color:var(--text-muted);font-size:13px;margin-top:4px">Аналізів ще немає</p>'}
        `;
    });
}

function closeUserDetails() {
    document.getElementById('userDetailsModal').style.display = 'none';
}

/* ─── Change role ─── */
function changeRole(userId, newRole) {
    apiFetch(`/api/admin/users/${userId}/role`, 'PUT', { role: newRole })
        .then(d => {
            if (d.error) { showToast(d.error, 'error'); loadUsers(); return; }
            showToast(`Роль змінено на ${newRole}`);
            loadUsers();
        });
}

/* ─── Edit user modal ─── */
function openEditModal(id, username, email) {
    document.getElementById('editUserId').value   = id;
    document.getElementById('editUsername').value = username;
    document.getElementById('editEmail').value    = email;
    document.getElementById('editPassword').value = '';
    document.getElementById('editError').style.display = 'none';
    document.getElementById('editUserModal').style.display = 'flex';
}
function closeEditModal() {
    document.getElementById('editUserModal').style.display = 'none';
}
function saveUser() {
    const id       = document.getElementById('editUserId').value;
    const username = document.getElementById('editUsername').value.trim();
    const email    = document.getElementById('editEmail').value.trim();
    const password = document.getElementById('editPassword').value.trim();
    const errEl    = document.getElementById('editError');
    const btn      = document.getElementById('saveEditBtn');

    btn.disabled = true; btn.textContent = 'Збереження…';
    errEl.style.display = 'none';

    const body = { username, email };
    if (password) body.password = password;

    apiFetch(`/api/admin/users/${id}`, 'PUT', body)
        .then(d => {
            if (d.error) {
                errEl.textContent = d.error;
                errEl.style.display = 'block';
            } else {
                closeEditModal();
                showToast('Дані збережено');
                loadUsers();
            }
        })
        .finally(() => { btn.disabled = false; btn.textContent = 'Зберегти'; });
}

/* ═══════════════════════════════════════
   NEWS
═══════════════════════════════════════ */
let _newsSearchTimer = null;
function searchNews() {
    clearTimeout(_newsSearchTimer);
    _newsSearchTimer = setTimeout(() => {
        _newsPage   = 0;
        _newsSearch = document.getElementById('newsSearch').value.trim();
        _newsSource = document.getElementById('newsSourceFilter').value;
        clearNewsSelection();
        loadNews();
    }, 350);
}

function loadNews(page = _newsPage) {
    _newsPage = page;
    const params = new URLSearchParams({ search: _newsSearch, source: _newsSource, page, size: 15 });
    document.getElementById('newsLoading').style.display = 'block';
    document.getElementById('newsBody').innerHTML = '';
    document.getElementById('newsEmpty').style.display = 'none';

    apiFetch('/api/admin/news?' + params).then(d => {
        document.getElementById('newsLoading').style.display = 'none';
        document.getElementById('newsTotal').textContent = `Всього: ${d.totalElements}`;
        if (!d.content?.length) { document.getElementById('newsEmpty').style.display = 'block'; return; }
        document.getElementById('newsBody').innerHTML = d.content.map(renderNewsRow).join('');
        d.content.forEach(n => {
            const cb = document.querySelector(`.news-cb[data-id="${n.id}"]`);
            if (cb) cb.checked = _selectedNews.has(n.id);
        });
        renderPagination('newsPagination', d.page, d.totalPages, p => loadNews(p));
    });
}

const SOURCE_COLORS = {
    NEWS_API:'#7c3aed', GUARDIAN:'#dc2626', GNEWS:'#059669',
    REDDIT:'#ff4500', DATASET:'#818cf8', MANUAL:'#d97706'
};
const SOURCE_LABELS = {
    NEWS_API:'NewsAPI', GUARDIAN:'Guardian', GNEWS:'GNews',
    REDDIT:'Reddit', DATASET:'Dataset', MANUAL:'Manual'
};

function renderNewsRow(n) {
    const color    = SOURCE_COLORS[n.sourceType] || '#8b949e';
    const label    = SOURCE_LABELS[n.sourceType] || (n.sourceType || '—');
    const date     = n.publishedAt ? new Date(n.publishedAt).toLocaleDateString('uk') : '—';
    const title    = n.title ? n.title.slice(0, 55) + (n.title.length > 55 ? '…' : '') : '—';
    const analyzed = n.analyzed
        ? `<span style="color:var(--success);font-size:12px;font-weight:600">Є</span>`
        : `<button class="btn btn--ghost" style="font-size:11px;padding:2px 7px"
                   onclick="analyzeNews('${n.id}', this)">Аналіз</button>`;

    return `<tr>
        <td style="padding:10px 8px 10px 16px">
            <input type="checkbox" class="news-cb" data-id="${n.id}"
                   style="cursor:pointer;width:14px;height:14px"
                   onchange="toggleNews('${n.id}', this)">
        </td>
        <td style="max-width:260px">
            <div style="font-size:13px;font-weight:500" title="${escHtml(n.title || '')}">${escHtml(title)}</div>
            ${n.url ? `<a href="${escHtml(n.url)}" target="_blank" style="font-size:11px;color:var(--text-muted)">посилання</a>` : ''}
        </td>
        <td style="font-size:12px;color:var(--text-muted)">${escHtml(n.source || '—')}</td>
        <td style="font-size:12px;color:var(--text-muted)">${escHtml(n.category || '—')}</td>
        <td><span style="font-size:11px;font-weight:600;color:${color};background:${color}22;
                         border:1px solid ${color}55;border-radius:4px;padding:2px 7px">${label}</span></td>
        <td style="font-size:12px;color:var(--text-muted)">${date}</td>
        <td>${analyzed}</td>
        <td style="text-align:right;white-space:nowrap">
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px"
                    onclick="openEditNewsModal('${n.id}','${escHtml(n.title||'')}','${escHtml(n.source||'')}','${escHtml(n.category||'')}')">Редагувати</button>
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px;color:var(--danger)"
                    onclick="confirmDelete('news','${n.id}','${escHtml((n.title||'').slice(0,40))}')">Видалити</button>
        </td>
    </tr>`;
}

/* ─── Analyze from admin ─── */
function analyzeNews(newsId, btn) {
    btn.disabled = true;
    btn.textContent = '…';
    apiFetch(`/analysis/${newsId}`, 'GET')
        .then(d => {
            if (d.credibilityScore !== undefined) {
                showToast(`Аналіз готовий. Скор: ${d.credibilityScore}`);
                loadNews(_newsPage);
            } else {
                btn.disabled = false;
                btn.textContent = 'Аналіз';
                showToast('Помилка аналізу', 'error');
            }
        });
}

/* ─── News selection ─── */
function toggleNews(id, cb) {
    if (cb.checked) _selectedNews.add(id);
    else _selectedNews.delete(id);
    updateBulkBar('news');
}

function toggleAllNews(cb) {
    document.querySelectorAll('.news-cb').forEach(c => {
        c.checked = cb.checked;
        if (cb.checked) _selectedNews.add(c.dataset.id);
        else _selectedNews.delete(c.dataset.id);
    });
    updateBulkBar('news');
}

function clearNewsSelection() {
    _selectedNews.clear();
    document.querySelectorAll('.news-cb').forEach(c => c.checked = false);
    const all = document.getElementById('newsAllCb');
    if (all) all.checked = false;
    updateBulkBar('news');
}

function bulkDeleteNews() {
    if (!_selectedNews.size) return;
    confirmDeleteBulk('news', [..._selectedNews], `${_selectedNews.size} новин`);
}

/* ─── Edit news modal ─── */
function openEditNewsModal(id, title, source, category) {
    document.getElementById('editNewsId').value       = id;
    document.getElementById('editNewsTitle').value    = title;
    document.getElementById('editNewsSource').value   = source;
    document.getElementById('editNewsCategory').value = category;
    document.getElementById('editNewsError').style.display = 'none';
    document.getElementById('editNewsModal').style.display = 'flex';
}
function closeEditNewsModal() {
    document.getElementById('editNewsModal').style.display = 'none';
}
function saveNews() {
    const id       = document.getElementById('editNewsId').value;
    const title    = document.getElementById('editNewsTitle').value.trim();
    const source   = document.getElementById('editNewsSource').value.trim();
    const category = document.getElementById('editNewsCategory').value.trim();
    const errEl    = document.getElementById('editNewsError');
    const btn      = document.getElementById('saveNewsBtn');

    if (!title) { errEl.textContent = 'Заголовок не може бути порожнім'; errEl.style.display = 'block'; return; }

    btn.disabled = true; btn.textContent = 'Збереження…';
    errEl.style.display = 'none';

    apiFetch(`/api/admin/news/${id}`, 'PUT', { title, source, category })
        .then(d => {
            if (d.error) {
                errEl.textContent = d.error;
                errEl.style.display = 'block';
            } else {
                closeEditNewsModal();
                showToast('Новину збережено');
                loadNews(_newsPage);
            }
        })
        .finally(() => { btn.disabled = false; btn.textContent = 'Зберегти'; });
}

/* ═══════════════════════════════════════
   ANALYSES
═══════════════════════════════════════ */
let _analysesSearchTimer = null;
function searchAnalyses() {
    clearTimeout(_analysesSearchTimer);
    _analysesSearchTimer = setTimeout(() => {
        _analysesPage   = 0;
        _analysesSearch = document.getElementById('analysesSearch').value.trim();
        clearAnalysisSelection();
        loadAnalyses();
    }, 350);
}

function loadAnalyses(page = _analysesPage) {
    _analysesPage = page;
    const params = new URLSearchParams({ search: _analysesSearch, page, size: 15 });
    document.getElementById('analysesLoading').style.display = 'block';
    document.getElementById('analysesBody').innerHTML = '';
    document.getElementById('analysesEmpty').style.display = 'none';

    apiFetch('/api/admin/analyses?' + params).then(d => {
        document.getElementById('analysesLoading').style.display = 'none';
        document.getElementById('analysesTotal').textContent = `Всього: ${d.totalElements}`;
        if (!d.content?.length) { document.getElementById('analysesEmpty').style.display = 'block'; return; }
        document.getElementById('analysesBody').innerHTML = d.content.map(renderAnalysisRow).join('');
        d.content.forEach(a => {
            const cb = document.querySelector(`.analysis-cb[data-id="${a.id}"]`);
            if (cb) cb.checked = _selectedAnalyses.has(a.id);
        });
        renderPagination('analysesPagination', d.page, d.totalPages, p => loadAnalyses(p));
    });
}

function renderAnalysisRow(a) {
    const date      = a.analyzedAt ? new Date(a.analyzedAt).toLocaleDateString('uk') : '—';
    const title     = (a.newsTitle || '—').slice(0, 50) + ((a.newsTitle || '').length > 50 ? '…' : '');
    const color     = a.credibilityScore >= 60 ? '#3fb950' : a.credibilityScore >= 40 ? '#d29922' : '#f85149';
    const typeLabel = a.isManual ? 'Ручний' : 'Авто';
    const typeBg    = a.isManual ? '#818cf822' : '#30363d';
    const sentiment = a.sentimentLabel
        ? { POSITIVE:'позит.', NEUTRAL:'нейтр.', NEGATIVE:'негат.' }[a.sentimentLabel] || a.sentimentLabel
        : '';

    return `<tr>
        <td style="padding:10px 8px 10px 16px">
            <input type="checkbox" class="analysis-cb" data-id="${a.id}"
                   style="cursor:pointer;width:14px;height:14px"
                   onchange="toggleAnalysis('${a.id}', this)">
        </td>
        <td style="max-width:240px">
            <div style="font-size:13px" title="${escHtml(a.newsTitle||'')}">${escHtml(title)}</div>
            ${sentiment ? `<span style="font-size:11px;color:var(--text-muted)">${sentiment}</span>` : ''}
        </td>
        <td style="font-size:12px;color:var(--text-muted)">${escHtml(a.analyzedByUsername || '—')}</td>
        <td><strong style="color:${color}">${a.credibilityScore}</strong></td>
        <td style="font-size:12px;color:var(--text-muted)">${a.linguisticScore}</td>
        <td style="font-size:12px;color:var(--text-muted)">${a.crossSourceScore}</td>
        <td style="font-size:12px;color:var(--text-muted)">${a.factCheckScore}</td>
        <td><span style="font-size:11px;padding:2px 6px;border-radius:4px;background:${typeBg};color:var(--text-muted)">${typeLabel}</span></td>
        <td style="font-size:12px;color:var(--text-muted);white-space:nowrap">${date}</td>
        <td style="text-align:right">
            <button class="btn btn--ghost" style="font-size:11px;padding:3px 8px;color:var(--danger)"
                    onclick="confirmDelete('analysis','${a.id}','${escHtml(title)}')">Видалити</button>
        </td>
    </tr>`;
}

/* ─── Analysis selection ─── */
function toggleAnalysis(id, cb) {
    if (cb.checked) _selectedAnalyses.add(id);
    else _selectedAnalyses.delete(id);
    updateBulkBar('analyses');
}

function toggleAllAnalyses(cb) {
    document.querySelectorAll('.analysis-cb').forEach(c => {
        c.checked = cb.checked;
        if (cb.checked) _selectedAnalyses.add(c.dataset.id);
        else _selectedAnalyses.delete(c.dataset.id);
    });
    updateBulkBar('analyses');
}

function clearAnalysisSelection() {
    _selectedAnalyses.clear();
    document.querySelectorAll('.analysis-cb').forEach(c => c.checked = false);
    const all = document.getElementById('analysesAllCb');
    if (all) all.checked = false;
    updateBulkBar('analyses');
}

function bulkDeleteAnalyses() {
    if (!_selectedAnalyses.size) return;
    confirmDeleteBulk('analyses', [..._selectedAnalyses], `${_selectedAnalyses.size} аналізів`);
}

/* ─── Bulk bar ─── */
function updateBulkBar(type) {
    const set   = type === 'users' ? _selectedUsers : type === 'news' ? _selectedNews : _selectedAnalyses;
    const barId = type + 'BulkBar';
    const cntId = type + 'BulkCount';
    const bar   = document.getElementById(barId);
    const cnt   = document.getElementById(cntId);
    if (!bar) return;
    if (set.size > 0) {
        bar.style.display = 'flex';
        cnt.textContent = `Обрано: ${set.size}`;
    } else {
        bar.style.display = 'none';
    }
}

/* ═══════════════════════════════════════
   ANALYTICS
═══════════════════════════════════════ */
function loadAnalytics() {
    document.getElementById('analyticsLoading').style.display = 'block';
    document.getElementById('analyticsBody').innerHTML = '';

    apiFetch('/api/admin/analytics').then(d => {
        document.getElementById('analyticsLoading').style.display = 'none';
        const users = d.users || [];

        document.getElementById('analyticsBody').innerHTML = users.map((u, i) => {
            const role = u.role === 'ADMIN'
                ? `<span class="admin-badge admin-badge--admin">ADMIN</span>`
                : `<span class="admin-badge admin-badge--user">USER</span>`;
            const last = u.lastActivity ? new Date(u.lastActivity).toLocaleDateString('uk') : '—';
            const reg  = u.createdAt   ? new Date(u.createdAt).toLocaleDateString('uk')    : '—';
            return `<tr>
                <td style="color:var(--text-muted);font-size:12px">${i + 1}</td>
                <td><strong>${escHtml(u.username)}</strong><br>
                    <span style="font-size:11px;color:var(--text-muted)">${escHtml(u.email)}</span></td>
                <td>${role}</td>
                <td><strong>${u.analysesCount}</strong></td>
                <td>${u.avgScore > 0 ? scoreSpan(u.avgScore) : '—'}</td>
                <td>${u.favoriteCount}</td>
                <td style="font-size:12px;color:var(--text-muted)">${last}</td>
                <td style="font-size:12px;color:var(--text-muted)">${reg}</td>
            </tr>`;
        }).join('');

        const top = users.slice(0, 10);
        renderTopChart(top);
        renderAvgChart(top);
    });
}

function scoreSpan(score) {
    const color = score >= 60 ? '#3fb950' : score >= 40 ? '#d29922' : '#f85149';
    return `<strong style="color:${color}">${score}</strong>`;
}

function renderTopChart(users) {
    const ctx = document.getElementById('topUsersChart').getContext('2d');
    if (_topChart) _topChart.destroy();
    _topChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: users.map(u => u.username),
            datasets: [{ label: 'Аналізів', data: users.map(u => u.analysesCount),
                         backgroundColor: '#58a6ff88', borderColor: '#58a6ff', borderWidth: 1 }]
        },
        options: { responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { ticks: { color: '#8b949e' } }, y: { ticks: { color: '#8b949e' }, beginAtZero: true } } }
    });
}

function renderAvgChart(users) {
    const ctx    = document.getElementById('avgScoreChart').getContext('2d');
    if (_avgChart) _avgChart.destroy();
    const colors = users.map(u => u.avgScore >= 60 ? '#3fb950' : u.avgScore >= 40 ? '#d29922' : '#f85149');
    _avgChart = new Chart(ctx, {
        type: 'bar',
        data: {
            labels: users.map(u => u.username),
            datasets: [{ label: 'Сер. скор', data: users.map(u => u.avgScore),
                         backgroundColor: colors.map(c => c + '88'), borderColor: colors, borderWidth: 1 }]
        },
        options: { responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false } },
            scales: { x: { ticks: { color: '#8b949e' } },
                      y: { ticks: { color: '#8b949e' }, min: 0, max: 100, beginAtZero: true } } }
    });
}

/* ═══════════════════════════════════════
   PARSING
═══════════════════════════════════════ */
function fetchSource(source) {
    const btn      = document.getElementById(`fetch-btn-${source}`);
    const statusEl = document.getElementById(`fetch-status-${source}`);

    btn.disabled   = true;
    btn.textContent = 'Завантаження…';
    statusEl.style.display = 'none';

    apiFetch(`/api/admin/fetch/${source}`, 'POST').then(d => {
        btn.disabled    = false;
        btn.textContent = 'Отримати новини';
        statusEl.style.display = 'block';
        if (d.error) {
            statusEl.style.color   = 'var(--danger)';
            statusEl.textContent   = 'Помилка: ' + d.error;
        } else {
            statusEl.style.color   = 'var(--success)';
            statusEl.textContent   = `Готово — збережено: ${d.saved} новин`;
        }
    });
}

function triggerBatchAnalyze(force) {
    const btn      = document.getElementById(force ? 'batchForceBtn' : 'batchAnalyzeBtn');
    const statusEl = document.getElementById('batchStatus');
    const url      = force ? '/analysis/batch/force' : '/analysis/batch';
    const label    = force ? 'Примусово переаналізувати всі' : 'Аналізувати нові';

    btn.disabled    = true;
    btn.textContent = 'Запускається…';
    statusEl.style.display = 'none';

    // /analysis/batch endpoints return plain text, not JSON
    fetch(url, { method: 'POST', headers: Auth.getHeaders() })
        .then(() => {
            btn.disabled    = false;
            btn.textContent = label;
            statusEl.style.display = 'block';
            statusEl.style.color   = 'var(--success)';
            statusEl.textContent   = 'Batch-аналіз запущено у фоні';
        })
        .catch(err => {
            btn.disabled    = false;
            btn.textContent = label;
            showToast('Помилка: ' + err.message, 'error');
        });
}

/* ═══════════════════════════════════════
   ВИБІРКОВИЙ BATCH-АНАЛІЗ
═══════════════════════════════════════ */
async function runPartialBatch() {
    const btn    = document.getElementById('pbRunBtn');
    const result = document.getElementById('pbResult');

    // Збираємо параметри
    const limit    = parseInt(document.getElementById('pbLimit').value)    || 50;
    const source   = document.getElementById('pbSource').value;
    const category = document.getElementById('pbCategory').value.trim();
    const dateFrom = document.getElementById('pbDateFrom').value;
    const dateTo   = document.getElementById('pbDateTo').value;
    const force    = document.getElementById('pbForce').checked;

    // Валідація
    if (limit < 1 || limit > 1000) {
        showToast('Кількість статей: від 1 до 1000', 'error');
        return;
    }

    btn.disabled    = true;
    btn.textContent = 'Аналізується…';
    result.style.display = 'none';

    try {
        const data = await apiFetch('/api/admin/analyses/batch/partial', 'POST', {
            limit:         limit,
            sourceType:    source   || null,
            category:      category || null,
            dateFrom:      dateFrom || null,
            dateTo:        dateTo   || null,
            forceReanalyze: force,
        });

        result.style.display = 'block';

        const rows = [
            { label: 'Знайдено у вибірці',    val: data.total,     color: 'var(--text-muted)' },
            { label: 'Проаналізовано',         val: data.processed, color: 'var(--success)' },
            { label: 'Пропущено (вже є)',      val: data.skipped,   color: 'var(--text-muted)' },
            { label: 'Помилок',                val: data.failed,    color: data.failed > 0 ? 'var(--danger)' : 'var(--text-muted)' },
        ];

        result.innerHTML = `
            <div style="border:1px solid var(--border);border-radius:8px;overflow:hidden;">
                ${rows.map(r => `
                    <div style="display:flex;justify-content:space-between;align-items:center;
                                padding:8px 14px;border-bottom:1px solid var(--border);">
                        <span style="font-size:12px;color:var(--text-muted)">${r.label}</span>
                        <span style="font-size:14px;font-weight:600;color:${r.color}">${r.val}</span>
                    </div>`).join('')}
            </div>`;

        const msg = `Готово: ${data.processed} проаналізовано, ${data.skipped} пропущено`;
        showToast(msg, data.failed > 0 ? 'error' : 'success');

    } catch (e) {
        showToast('Помилка вибіркового аналізу: ' + e.message, 'error');
    } finally {
        btn.disabled    = false;
        btn.textContent = 'Запустити вибірковий аналіз';
    }
}

/* ═══════════════════════════════════════
   LOGS
═══════════════════════════════════════ */
function loadLogs(page = _logsPage) {
    _logsPage = page;
    document.getElementById('logsLoading').style.display = 'block';
    document.getElementById('logsBody').innerHTML = '';
    document.getElementById('logsEmpty').style.display = 'none';

    apiFetch(`/api/admin/logs?page=${page}&size=25`).then(d => {
        document.getElementById('logsLoading').style.display = 'none';
        if (!d.content?.length) { document.getElementById('logsEmpty').style.display = 'block'; return; }

        const ACTION_LABELS = {
            CHANGE_ROLE:          'Зміна ролі',
            DELETE_USER:          'Видалення юзера',
            EDIT_USER:            'Редагування юзера',
            BULK_DELETE_USERS:    'Масове вид. юзерів',
            DELETE_NEWS:          'Видалення новини',
            EDIT_NEWS:            'Редагування новини',
            BULK_DELETE_NEWS:     'Масове вид. новин',
            DELETE_ANALYSIS:      'Видалення аналізу',
            BULK_DELETE_ANALYSES: 'Масове вид. аналізів',
            FETCH:                'Парсинг',
        };

        document.getElementById('logsBody').innerHTML = d.content.map(log => {
            const time  = log.createdAt ? new Date(log.createdAt).toLocaleString('uk') : '—';
            const label = ACTION_LABELS[log.action] || log.action;
            return `<tr>
                <td style="font-size:12px;color:var(--text-muted);white-space:nowrap">${time}</td>
                <td style="font-size:13px;font-weight:500">${escHtml(log.adminUsername || '—')}</td>
                <td><span style="font-size:11px;background:rgba(88,166,255,.12);color:#58a6ff;
                               border-radius:4px;padding:2px 7px;white-space:nowrap">${escHtml(label)}</span></td>
                <td style="font-size:13px;color:var(--text-muted)">${escHtml(log.details || '—')}</td>
            </tr>`;
        }).join('');

        renderPagination('logsPagination', d.page, d.totalPages, p => loadLogs(p));
    });
}

/* ═══════════════════════════════════════
   CONFIRM DELETE
═══════════════════════════════════════ */
function confirmDelete(type, id, name) {
    const isUser     = type === 'user';
    const isAnalysis = type === 'analysis';
    document.getElementById('confirmTitle').textContent = isUser
        ? 'Видалити користувача?' : isAnalysis ? 'Видалити аналіз?' : 'Видалити новину?';
    document.getElementById('confirmText').textContent  = isUser
        ? `Користувача "${name}" буде видалено назавжди.`
        : isAnalysis
        ? `Аналіз "${name}" буде видалено назавжди.`
        : `Новину "${name}" та її аналіз буде видалено назавжди.`;

    _confirmCb = () => {
        const url = isUser ? `/api/admin/users/${id}` : isAnalysis ? `/api/admin/analyses/${id}` : `/api/admin/news/${id}`;
        apiFetch(url, 'DELETE').then(() => {
            closeConfirm();
            showToast('Видалено');
            if (isUser) loadUsers();
            else if (isAnalysis) loadAnalyses();
            else loadNews();
        });
    };
    document.getElementById('confirmOkBtn').onclick = _confirmCb;
    document.getElementById('confirmModal').style.display = 'flex';
}

function confirmDeleteBulk(type, ids, label) {
    document.getElementById('confirmTitle').textContent = `Видалити ${label}?`;
    document.getElementById('confirmText').textContent  = `Обрані елементи (${ids.length}) буде видалено назавжди.`;

    _confirmCb = () => {
        apiFetch(`/api/admin/${type}/bulk-delete`, 'POST', { ids }).then(() => {
            closeConfirm();
            showToast(`Видалено: ${ids.length}`);
            if (type === 'users')    { clearUserSelection();    loadUsers();    }
            if (type === 'news')     { clearNewsSelection();     loadNews();     }
            if (type === 'analyses') { clearAnalysisSelection(); loadAnalyses(); }
        });
    };
    document.getElementById('confirmOkBtn').onclick = _confirmCb;
    document.getElementById('confirmModal').style.display = 'flex';
}

function closeConfirm() {
    document.getElementById('confirmModal').style.display = 'none';
    _confirmCb = null;
}

/* ═══════════════════════════════════════
   PAGINATION
═══════════════════════════════════════ */
function renderPagination(containerId, current, total, cb) {
    const el = document.getElementById(containerId);
    if (!el) return;
    if (total <= 1) { el.innerHTML = ''; return; }

    let html  = '';
    const from = Math.max(0, current - 2);
    const to   = Math.min(total - 1, current + 2);

    if (current > 0)
        html += `<button class="btn btn--ghost" onclick="(${cb})(${current - 1})">‹</button>`;
    for (let i = from; i <= to; i++)
        html += `<button class="btn ${i === current ? 'btn--primary' : 'btn--ghost'}"
                         onclick="(${cb})(${i})">${i + 1}</button>`;
    if (current < total - 1)
        html += `<button class="btn btn--ghost" onclick="(${cb})(${current + 1})">›</button>`;

    el.innerHTML = html;
}

/* ═══════════════════════════════════════
   HELPERS
═══════════════════════════════════════ */
function apiFetch(url, method = 'GET', body = null) {
    const opts = { method, headers: Auth.getHeaders() };
    if (body) { opts.headers['Content-Type'] = 'application/json'; opts.body = JSON.stringify(body); }
    return fetch(url, opts).then(r => r.json()).catch(err => {
        showToast('Помилка: ' + err.message, 'error');
        return {};
    });
}

function showToast(msg, type = 'success') {
    const t = document.getElementById('toast');
    t.className = `toast${type === 'error' ? ' toast--error' : ''}`;
    t.textContent = msg;
    t.classList.add('toast--visible');
    setTimeout(() => t.classList.remove('toast--visible'), 3000);
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val ?? '—';
}

function escHtml(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
