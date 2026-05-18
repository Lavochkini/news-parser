/* ═══════════════════════════════════════
   MY DASHBOARD
═══════════════════════════════════════ */
let currentArticle = null;
window._myArticles = [];
let _userFavorites = new Set();

document.addEventListener('DOMContentLoaded', () => {
    if (Auth.isLoggedIn()) {
        fetch('/api/user/favorites', { headers: Auth.getHeaders() })
            .then(r => r.ok ? r.json() : [])
            .then(ids => { _userFavorites = new Set(ids); })
            .catch(() => {});
    }
});

document.addEventListener('DOMContentLoaded', () => {
    renderNav();
    if (!Auth.isLoggedIn()) {
        document.getElementById('notLoggedIn').style.display = 'block';
        return;
    }
    document.getElementById('dashContent').style.display = 'block';
    loadPersonalDashboard();
});

/* ── Nav ── */
function renderNav() {
    const nav = document.getElementById('navUser');
    if (!nav) return;
    if (Auth.isLoggedIn()) {
        nav.innerHTML = `
            <span class="navbar__username">${escHtml(Auth.getUsername())}</span>
            <button class="btn btn--ghost" onclick="Auth.logout()">Вийти</button>`;
    } else {
        nav.innerHTML = `
            <a href="/login"    class="btn btn--ghost">Увійти</a>
            <a href="/register" class="btn btn--outline">Реєстрація</a>`;
    }
}

/* ── Load dashboard ── */
function loadPersonalDashboard() {
    fetch('/api/user/dashboard', { headers: Auth.getHeaders() })
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => renderDashboard(data))
        .catch(err => {
            document.getElementById('dashContent').innerHTML =
                `<div style="text-align:center;padding:40px;color:var(--text-muted)">
                    Помилка завантаження: ${err.message}</div>`;
        });
}

function renderDashboard(data) {
    document.getElementById('myUsername').textContent  = data.username ?? '—';
    document.getElementById('myAnalyzed').textContent  = data.totalAnalyzed ?? 0;
    document.getElementById('myFavorites').textContent = data.totalFavorites ?? 0;
    renderArticleList('analyzedList', data.analyzedArticles ?? [], 'analyzed');
    renderArticleList('favoriteList', data.favoriteArticles ?? [], 'favorite');
}

/* ── Render list ── */
function renderArticleList(containerId, articles, mode) {
    const container = document.getElementById(containerId);
    if (!articles.length) return;

    // Зберігаємо в глобальний масив за ключем
    const storeKey = `_my_${mode}`;
    window[storeKey] = articles;

    container.innerHTML = articles.map((a, i) => `
        <div class="personal-card">
            <div class="personal-card__clickable" onclick="openArticleModal('${mode}', ${i})">
                ${a.imageUrl
                    ? `<img class="personal-card__img" src="${escHtml(a.imageUrl)}" alt="" onerror="this.style.display='none'">`
                    : sourcePlaceholderHtml(a.sourceType)}
                <div class="personal-card__body">
                    <div class="personal-card__meta">
                        ${sourceBadge(a.sourceType)}
                        ${a.category ? `<span class="badge badge--category">${escHtml(a.category)}</span>` : ''}
                        <span style="color:var(--text-muted);font-size:12px">${formatDate(a.publishedAt)}</span>
                    </div>
                    <div class="personal-card__title">${escHtml(a.title || 'Без заголовку')}</div>
                    <div class="personal-card__footer">
                        <span style="color:var(--text-muted);font-size:12px">${escHtml(a.source || '')}</span>
                        ${a.credibilityScore != null
                            ? `<span class="cred-badge ${credClass(a.credibilityScore)}">${a.credibilityScore}/100</span>`
                            : ''}
                        ${mode === 'analyzed' && a.analyzedAt
                            ? `<span style="color:var(--text-muted);font-size:11px">Аналіз: ${formatDate(a.analyzedAt)}</span>`
                            : ''}
                    </div>
                </div>
            </div>
            <button class="personal-card__delete" title="Видалити"
                    onclick="askDelete('${mode}','${escHtml(a.newsItemId || a.id)}',this)">✕</button>
        </div>`).join('');
}

/* ── Delete ── */
function askDelete(mode, newsItemId, btn) {
    // Якщо вже показується підтвердження — ігноруємо
    if (btn.classList.contains('confirming')) return;

    btn.classList.add('confirming');
    btn.innerHTML = `
        <span class="del-confirm__label">Видалити?</span>
        <button class="del-confirm__yes" onclick="confirmDelete(event,'${mode}','${newsItemId}',this)">Так</button>
        <button class="del-confirm__no"  onclick="cancelDelete(event,this)">Ні</button>`;
}

function cancelDelete(e, noBtn) {
    e.stopPropagation();
    const btn = noBtn.closest('.personal-card__delete');
    btn.classList.remove('confirming');
    btn.innerHTML = '✕';
}

function confirmDelete(e, mode, newsItemId, yesBtn) {
    e.stopPropagation();
    const btn = yesBtn.closest('.personal-card__delete');
    btn.innerHTML = '…';
    btn.disabled = true;

    const url = mode === 'analyzed'
        ? `/analysis/${newsItemId}/manual`
        : `/api/user/favorites/${newsItemId}`;

    fetch(url, { method: 'DELETE', headers: Auth.getHeaders() })
        .then(r => { if (!r.ok && r.status !== 204) throw new Error('HTTP ' + r.status); })
        .then(() => {
            const card = btn.closest('.personal-card');
            card.style.transition = 'opacity .2s, max-height .3s';
            card.style.opacity = '0';
            card.style.maxHeight = '0';
            card.style.overflow  = 'hidden';
            setTimeout(() => { card.remove(); updateCounters(); }, 300);
        })
        .catch(err => {
            btn.disabled = false;
            btn.classList.remove('confirming');
            btn.innerHTML = '✕';
            showToast('Помилка: ' + err.message, 'error');
        });
}

function updateCounters() {
    const analyzed = document.querySelectorAll('#analyzedList .personal-card').length;
    const favorites = document.querySelectorAll('#favoriteList .personal-card').length;
    const el = document.getElementById('myAnalyzed');
    const el2 = document.getElementById('myFavorites');
    if (el) el.textContent = analyzed;
    if (el2) el2.textContent = favorites;
}

function showToast(msg, type = 'info') {
    const t = document.createElement('div');
    t.className = `toast toast--${type}`;
    t.textContent = msg;
    document.body.appendChild(t);
    setTimeout(() => t.classList.add('toast--visible'), 10);
    setTimeout(() => { t.classList.remove('toast--visible'); setTimeout(() => t.remove(), 300); }, 3000);
}

/* ═══════════════════════════════════════
   MODAL
═══════════════════════════════════════ */
function openArticleModal(mode, index) {
    const article = (window[`_my_${mode}`] || [])[index];
    if (!article) return;
    currentArticle = article;

    // Badges
    document.getElementById('modalBadges').innerHTML =
        sourceBadge(article.sourceType) +
        (article.category ? `<span class="badge badge--category">${escHtml(article.category)}</span>` : '');

    // Image
    const img = document.getElementById('modalImg');
    setModalImage(img, article.imageUrl, article.sourceType);

    document.getElementById('modalTitle').textContent   = article.title || 'Без заголовку';
    document.getElementById('modalSource').textContent  = article.source || '';
    document.getElementById('modalDate').textContent    = formatDate(article.publishedAt);
    document.getElementById('modalDesc').textContent    = article.description || '';
    document.getElementById('modalReadLink').href       = article.url || '#';

    const fullEl = document.getElementById('modalFullText');
    if (article.fullContent && article.fullContent !== article.description) {
        const text = article.fullContent.length > 1200
            ? article.fullContent.substring(0, 1200) + '…' : article.fullContent;
        fullEl.textContent = text;
        fullEl.style.display = 'block';
    } else { fullEl.style.display = 'none'; }

    // Скидаємо результати
    document.getElementById('analysisResults').style.display = 'none';
    document.getElementById('analyzeBtn').disabled = false;
    document.getElementById('analyzeBtn').textContent = 'Аналізувати';

    // Якщо є збережений результат — показуємо одразу
    const id = article.newsItemId || article.id;
    if (id && article.credibilityScore != null) {
        fetch(`/analysis/${id}`, { headers: Auth.getHeaders() })
            .then(r => r.ok ? r.json() : null)
            .then(r => { if (r) renderAnalysisResults(r); })
            .catch(() => {});
    }

    updateStarBtn();
    document.getElementById('articleModal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function updateStarBtn() {
    const btn = document.getElementById('starBtn');
    if (!btn || !currentArticle) return;
    const id = currentArticle.newsItemId || currentArticle.id;
    const isFav = id && _userFavorites.has(id);
    btn.textContent = isFav ? '★' : '☆';
    btn.classList.toggle('star-btn--active', !!isFav);
}

function toggleFavorite() {
    if (!Auth.isLoggedIn()) { window.location.href = '/login'; return; }
    if (!currentArticle) return;
    const id = currentArticle.newsItemId || currentArticle.id;
    if (!id) return;
    const btn = document.getElementById('starBtn');
    btn.disabled = true;
    fetch(`/api/user/favorites/${id}`, { method: 'POST', headers: Auth.getHeaders() })
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => {
            if (data.added) _userFavorites.add(id);
            else            _userFavorites.delete(id);
            updateStarBtn();
        })
        .catch(err => showToast('Помилка: ' + err.message, 'error'))
        .finally(() => { btn.disabled = false; });
}

function closeModal() {
    document.getElementById('articleModal').classList.remove('open');
    document.body.style.overflow = '';
    currentArticle = null;
}

function modalOverlayClick(e) {
    if (e.target.id === 'articleModal') closeModal();
}

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

/* ── Run analysis ── */
function runManualAnalysis() {
    if (!currentArticle) return;
    const id = currentArticle.newsItemId || currentArticle.id;
    if (!id) return;

    const options = {
        runLinguistic:  document.getElementById('optLinguistic').checked,
        runCrossSource: document.getElementById('optCrossSource').checked,
        runFactCheck:   document.getElementById('optFactCheck').checked,
        runSentiment:   document.getElementById('optSentiment').checked,
        runReadability: document.getElementById('optReadability').checked,
    };

    const btn = document.getElementById('analyzeBtn');
    btn.disabled = true;
    btn.textContent = 'Аналізується...';

    fetch(`/analysis/${id}/manual`, {
        method: 'POST',
        headers: Auth.getHeaders(),
        body: JSON.stringify(options),
    })
    .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
    .then(result => { renderAnalysisResults(result); btn.textContent = 'Переаналізувати'; btn.disabled = false; loadPersonalDashboard(); })
    .catch(err => { btn.textContent = 'Помилка: ' + err.message; btn.disabled = false; });
}

/* ── Render results ── */
function renderAnalysisResults(r) {
    document.getElementById('analysisResults').style.display = 'block';
    const score = r.credibilityScore ?? 0;
    document.getElementById('scoreValue').textContent = score;
    document.getElementById('scoreCircle').className = 'score-display__circle ' + scoreClass(score);
    const lbls = ['Дуже низька', 'Низька', 'Помірна', 'Висока', 'Дуже висока'];
    document.getElementById('scoreLabel').textContent = lbls[Math.min(Math.floor(score / 21), 4)];

    document.getElementById('scoreLayers').innerHTML = `
        <div class="score-layer">
            <span>Лінгвістика</span>
            <div class="score-bar"><div class="score-bar__fill score-bar__fill--linguistic" style="width:${(r.linguisticScore/35)*100}%"></div></div>
            <span>${r.linguisticScore ?? 0}/35</span>
        </div>
        <div class="score-layer">
            <span>Перехресна перевірка</span>
            <div class="score-bar"><div class="score-bar__fill score-bar__fill--cross" style="width:${(r.crossSourceScore/35)*100}%"></div></div>
            <span>${r.crossSourceScore ?? 0}/35</span>
        </div>
        <div class="score-layer">
            <span>Факт-чекінг</span>
            <div class="score-bar"><div class="score-bar__fill score-bar__fill--fact" style="width:${(r.factCheckScore/30)*100}%"></div></div>
            <span>${r.factCheckScore ?? 0}/30</span>
        </div>`;

    const details = [];
    if (r.hedgeWordsFound?.length)        details.push(`<li>Хедж-слова: <em>${escHtml(r.hedgeWordsFound.join(', '))}</em></li>`);
    if (r.clickbaitIndicators?.length)    details.push(`<li>Клікбейт: <em>${escHtml(r.clickbaitIndicators.join(', '))}</em></li>`);
    if (r.emotionalWordsFound?.length)    details.push(`<li>Емоційна лексика: <em>${escHtml(r.emotionalWordsFound.join(', '))}</em></li>`);
    if (r.manipulationIndicators?.length) details.push(`<li>Маніпуляція: <em>${escHtml(r.manipulationIndicators.join(', '))}</em></li>`);
    if (r.conspiracyIndicators?.length)   details.push(`<li>Конспірологія: <em>${escHtml(r.conspiracyIndicators.join(', '))}</em></li>`);
    if (r.confirmedSources?.length)       details.push(`<li>Підтверджено: <em>${escHtml(r.confirmedSources.join(', '))}</em></li>`);
    if (r.sourceDomain)                   details.push(`<li>Домен: <em>${escHtml(r.sourceDomain)}</em> — ${escHtml(r.sourceReputationTier)}</li>`);

    const sentUk = { POSITIVE: 'Позитивна', NEUTRAL: 'Нейтральна', NEGATIVE: 'Негативна' };
    const readUk = { VERY_EASY: 'Дуже легко', EASY: 'Легко', STANDARD: 'Стандарт', DIFFICULT: 'Складно', VERY_DIFFICULT: 'Дуже складно' };

    document.getElementById('scoreDetails').innerHTML = `
        <div class="score-extras">
            ${r.sentimentLabel && r.sentimentLabel !== 'N/A' ? `<span>Тональність: <strong>${sentUk[r.sentimentLabel] || r.sentimentLabel} (${r.sentimentScore?.toFixed(2)})</strong></span>` : ''}
            ${r.readabilityLevel && r.readabilityLevel !== 'N/A' ? `<span>Читабельність: <strong>${readUk[r.readabilityLevel] || r.readabilityLevel}</strong></span>` : ''}
            ${r.wordCount ? `<span>Слів: <strong>${r.wordCount}</strong></span>` : ''}
        </div>
        ${details.length ? `<ul class="score-indicators">${details.join('')}</ul>` : ''}`;

    document.getElementById('scoreReasoning').innerHTML = r.reasoning
        ? `<div class="score-reasoning__text">${escHtml(r.reasoning)}</div>` : '';
}

/* ── Source placeholders ── */
const SOURCE_PLACEHOLDER = {
    REDDIT:  { icon: '🤖', label: 'Reddit',  cls: 'img-placeholder--reddit'  },
    DATASET: { icon: '🗂️', label: 'Dataset', cls: 'img-placeholder--dataset' },
};

function sourcePlaceholderHtml(sourceType, extraCls = '') {
    const p = SOURCE_PLACEHOLDER[sourceType];
    if (!p) return `<div class="personal-card__img personal-card__img--placeholder"></div>`;
    return `<div class="personal-card__img img-placeholder ${p.cls} ${extraCls}">
                <span class="img-placeholder__icon">${p.icon}</span>${p.label}
            </div>`;
}

function setModalImage(imgEl, imageUrl, sourceType) {
    const p = SOURCE_PLACEHOLDER[sourceType];
    const prev = imgEl.previousElementSibling;
    if (prev && prev.classList.contains('img-placeholder')) prev.remove();

    if (imageUrl) {
        imgEl.src = imageUrl;
        imgEl.style.display = 'block';
        imgEl.onerror = () => {
            imgEl.style.display = 'none';
            if (p) imgEl.insertAdjacentHTML('beforebegin',
                `<div class="img-placeholder modal__img-placeholder ${p.cls}"><span class="img-placeholder__icon">${p.icon}</span>${p.label}</div>`);
        };
    } else if (p) {
        imgEl.style.display = 'none';
        imgEl.insertAdjacentHTML('beforebegin',
            `<div class="img-placeholder modal__img-placeholder ${p.cls}"><span class="img-placeholder__icon">${p.icon}</span>${p.label}</div>`);
    } else {
        imgEl.style.display = 'none';
    }
}

/* ── Helpers ── */
function scoreClass(s) {
    if (s <= 20) return 'score--very-low';
    if (s <= 40) return 'score--low';
    if (s <= 60) return 'score--medium';
    if (s <= 80) return 'score--high';
    return 'score--very-high';
}

function credClass(score) {
    if (score <= 20) return 'cred-badge--very-low';
    if (score <= 40) return 'cred-badge--low';
    if (score <= 60) return 'cred-badge--medium';
    if (score <= 80) return 'cred-badge--high';
    return 'cred-badge--very-high';
}

function sourceBadge(type) {
    const map = {
        NEWS_API: ['badge--newsapi','NewsAPI'], GUARDIAN: ['badge--guardian','Guardian'],
        GNEWS:    ['badge--gnews','GNews'],     REDDIT:   ['badge--reddit','Reddit'],
        DATASET:  ['badge--dataset','Датасет'],
    };
    const [cls, label] = map[type] || ['badge--category', type || ''];
    return `<span class="badge ${cls}">${label}</span>`;
}

function formatDate(iso) {
    if (!iso) return '';
    return new Date(iso).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short', year: 'numeric' });
}

function escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

window.openArticleModal  = openArticleModal;
window.closeModal        = closeModal;
window.modalOverlayClick = modalOverlayClick;
window.runManualAnalysis = runManualAnalysis;
window.toggleFavorite    = toggleFavorite;
window.askDelete         = askDelete;
window.cancelDelete      = cancelDelete;
window.confirmDelete     = confirmDelete;
