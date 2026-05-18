/* ═══════════════════════════════════════
   CHART DEFAULTS
═══════════════════════════════════════ */
Chart.defaults.color          = '#8b949e';
Chart.defaults.borderColor    = '#30363d';
Chart.defaults.font.family    = 'system-ui, sans-serif';
Chart.defaults.font.size      = 12;
Chart.defaults.plugins.legend.labels.boxWidth = 12;

const COLORS = {
    accent:   '#58a6ff',
    purple:   '#7c3aed',
    green:    '#059669',
    red:      '#dc2626',
    orange:   '#d97706',
    teal:     '#0891b2',
    pink:     '#db2777',
    gray:     '#6b7280',
};

/* ═══════════════════════════════════════
   INIT
═══════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    renderNav();
    loadStats();
});

function renderNav() {
    const nav = document.getElementById('navUser');
    if (!nav) return;
    if (Auth.isLoggedIn()) {
        nav.innerHTML = `<span class="navbar__username">${escHtml(Auth.getUsername())}</span>
                         <button class="btn btn--ghost" onclick="Auth.logout()">Logout</button>`;
    } else {
        nav.innerHTML = `<a href="/login" class="btn btn--ghost">Login</a>
                         <a href="/register" class="btn btn--outline">Register</a>`;
    }
}

/* ═══════════════════════════════════════
   LOAD & RENDER
═══════════════════════════════════════ */
function loadStats() {
    fetch('/api/dashboard/stats')
        .then(r => r.json())
        .then(stats => {
            renderCards(stats);
            renderLayerBars(stats.layerAverages);
            renderCredibilityChart(stats.credibilityBuckets);
            renderSentimentChart(stats.sentimentBreakdown);
            renderReadabilityChart(stats.readabilityBreakdown);
            renderReputationChart(stats.reputationBreakdown);
            renderDomainChart(stats.avgScoreByDomain);
            renderHedgeWords(stats.topHedgeWords);
        })
        .catch(err => console.error('Dashboard load error:', err));
}

/* ── Stat cards ── */
function renderCards(s) {
    setText('totalArticles', s.totalArticles ?? '—');
    setText('totalAnalyzed', s.totalAnalyzed ?? '—');
    setText('avgScore',      s.avgCredibilityScore != null ? s.avgCredibilityScore + ' / 100' : '—');
    const pct = s.totalArticles
        ? Math.round((s.totalAnalyzed / s.totalArticles) * 100) + '%'
        : '—';
    setText('analyzedPct', pct);
}

/* ── Layer bars ── */
function renderLayerBars(layers) {
    if (!layers) return;
    const container = document.getElementById('layerBars');
    const defs = [
        { key: 'linguistic',  label: 'Лінгвістика',        max: 35, cls: 'linguistic'  },
        { key: 'crossSource', label: 'Перехресна перевірка', max: 35, cls: 'crosssource' },
        { key: 'factCheck',   label: 'Факт-чекінг',        max: 30, cls: 'factcheck'   },
    ];
    container.innerHTML = defs.map(d => {
        const val = layers[d.key] ?? 0;
        const pct = Math.round((val / d.max) * 100);
        return `
        <div class="layer-bar-row">
            <span class="layer-bar-label">${d.label} (/${d.max})</span>
            <div class="layer-bar-track">
                <div class="layer-bar-fill layer-bar-fill--${d.cls}" style="width:${pct}%"></div>
            </div>
            <span class="layer-bar-value">${val}</span>
        </div>`;
    }).join('');
}

/* ── Credibility histogram ── */
function renderCredibilityChart(buckets) {
    if (!buckets) return;
    new Chart(document.getElementById('credChart'), {
        type: 'bar',
        data: {
            labels: Object.keys(buckets),
            datasets: [{
                label: 'Articles',
                data: Object.values(buckets),
                backgroundColor: [
                    COLORS.red, COLORS.orange, COLORS.teal, COLORS.green, COLORS.accent
                ],
                borderRadius: 6,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: {
                y: { beginAtZero: true, ticks: { stepSize: 1 } }
            }
        }
    });
}

/* ── Sentiment pie ── */
function renderSentimentChart(data) {
    if (!data) return;
    const order   = ['POSITIVE', 'NEUTRAL', 'NEGATIVE'];
    const ukLabels = { POSITIVE: 'Позитивна', NEUTRAL: 'Нейтральна', NEGATIVE: 'Негативна' };
    const colors  = [COLORS.green, COLORS.accent, COLORS.red];
    const keys     = order.filter(k => data[k] != null);
    const labels   = keys.map(k => ukLabels[k] || k);
    const values   = keys.map(k => data[k]);
    const bgColors = keys.map(k => colors[order.indexOf(k)]);

    new Chart(document.getElementById('sentimentChart'), {
        type: 'doughnut',
        data: { labels, datasets: [{ data: values, backgroundColor: bgColors, borderWidth: 2 }] },
        options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    });
}

/* ── Readability bar ── */
function renderReadabilityChart(data) {
    if (!data) return;
    const order  = ['VERY_EASY', 'EASY', 'STANDARD', 'DIFFICULT', 'VERY_DIFFICULT'];
    const ukRead = { VERY_EASY: 'Дуже легко', EASY: 'Легко', STANDARD: 'Стандарт', DIFFICULT: 'Складно', VERY_DIFFICULT: 'Дуже складно' };
    const labels = order.filter(k => data[k] != null);
    const values = labels.map(k => data[k]);

    new Chart(document.getElementById('readabilityChart'), {
        type: 'bar',
        data: {
            labels: labels.map(l => ukRead[l] || l),
            datasets: [{
                label: 'Articles',
                data: values,
                backgroundColor: COLORS.purple,
                borderRadius: 6,
            }]
        },
        options: {
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { y: { beginAtZero: true } }
        }
    });
}

/* ── Reputation doughnut ── */
function renderReputationChart(data) {
    if (!data) return;
    const order  = ['HIGH', 'GOOD', 'UNKNOWN', 'LOW'];
    const ukRep  = { HIGH: 'Висока', GOOD: 'Добра', UNKNOWN: 'Невідома', LOW: 'Низька' };
    const colors = [COLORS.green, COLORS.teal, COLORS.gray, COLORS.red];
    const keys    = order.filter(k => data[k] != null);
    const labels  = keys.map(k => ukRep[k] || k);
    const values  = keys.map(k => data[k]);
    const bgColors = keys.map(k => colors[order.indexOf(k)]);

    new Chart(document.getElementById('reputationChart'), {
        type: 'doughnut',
        data: { labels, datasets: [{ data: values, backgroundColor: bgColors, borderWidth: 2 }] },
        options: { responsive: true, plugins: { legend: { position: 'bottom' } } }
    });
}

/* ── Domain avg score horizontal bar ── */
function renderDomainChart(data) {
    if (!data) return;
    const labels = Object.keys(data);
    const values = Object.values(data);

    new Chart(document.getElementById('domainChart'), {
        type: 'bar',
        data: {
            labels,
            datasets: [{
                label: 'Avg Credibility',
                data: values,
                backgroundColor: COLORS.accent,
                borderRadius: 6,
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            plugins: { legend: { display: false } },
            scales: { x: { beginAtZero: true, max: 100 } }
        }
    });
}

/* ── Hedge words chips ── */
function renderHedgeWords(data) {
    if (!data) return;
    const container = document.getElementById('hedgeList');
    container.innerHTML = Object.entries(data).map(([word, count]) => `
        <div class="hedge-chip">
            ${escHtml(word)}
            <span class="hedge-chip__count">${count}</span>
        </div>`).join('');
}

/* ═══════════════════════════════════════
   SEARCH
═══════════════════════════════════════ */
let _searchPage = 0;
const _searchSize = 20;
let _searchResults = [];
let _currentArticle = null;
let _userFavorites = new Set();

document.addEventListener('DOMContentLoaded', () => {
    if (Auth.isLoggedIn()) {
        fetch('/api/user/favorites', { headers: Auth.getHeaders() })
            .then(r => r.ok ? r.json() : [])
            .then(ids => { _userFavorites = new Set(ids); })
            .catch(() => {});
    }
});

function runSearch(page = 0) {
    _searchPage = page;

    const params = new URLSearchParams({
        scoreMin:        document.getElementById('sfScoreMin').value   || 0,
        scoreMax:        document.getElementById('sfScoreMax').value   || 100,
        sortBy:          document.getElementById('sfSort').value       || 'credibility',
        page:            page,
        size:            _searchSize,
    });

    const search      = document.getElementById('sfSearch').value.trim();
    const sourceType  = document.getElementById('sfSourceType').value;
    const sentiment   = document.getElementById('sfSentiment').value;
    const readability = document.getElementById('sfReadability').value;
    const reputation  = document.getElementById('sfReputation').value;

    if (search)      params.set('search',           search);
    if (sourceType)  params.set('sourceType',       sourceType);
    if (sentiment)   params.set('sentimentLabel',   sentiment);
    if (readability) params.set('readabilityLevel', readability);
    if (reputation)  params.set('reputationTier',   reputation);

    document.getElementById('searchResults').style.display  = 'none';
    document.getElementById('searchEmpty').style.display    = 'none';
    document.getElementById('searchLoading').style.display  = 'block';

    fetch('/api/dashboard/search?' + params)
        .then(r => { if (!r.ok) throw new Error('HTTP ' + r.status); return r.json(); })
        .then(data => renderSearchResults(data))
        .catch(err => {
            document.getElementById('searchLoading').style.display = 'none';
            document.getElementById('searchEmpty').style.display   = 'block';
            document.getElementById('searchEmpty').textContent     = 'Помилка: ' + err.message;
        });
}

function resetSearch() {
    document.getElementById('sfSearch').value      = '';
    document.getElementById('sfSort').value        = 'credibility';
    document.getElementById('sfScoreMin').value    = '0';
    document.getElementById('sfScoreMax').value    = '100';
    document.getElementById('sfSourceType').value  = '';
    document.getElementById('sfSentiment').value   = '';
    document.getElementById('sfReadability').value = '';
    document.getElementById('sfReputation').value  = '';
    document.getElementById('searchResults').style.display = 'none';
    document.getElementById('searchEmpty').style.display   = 'none';
    document.getElementById('searchLoading').style.display = 'none';
}

function renderSearchResults(data) {
    document.getElementById('searchLoading').style.display = 'none';

    if (!data.results || data.results.length === 0) {
        document.getElementById('searchEmpty').style.display = 'block';
        return;
    }

    document.getElementById('searchResults').style.display = 'block';
    document.getElementById('searchEmpty').style.display   = 'none';

    _searchResults = data.results;

    const from = _searchPage * _searchSize + 1;
    const to   = Math.min((_searchPage + 1) * _searchSize, data.total);
    document.getElementById('searchMeta').textContent =
        `Знайдено: ${data.total} статей, показано ${from}–${to}`;

    const sentUk = { POSITIVE: 'Позитивна', NEUTRAL: 'Нейтральна', NEGATIVE: 'Негативна' };
    const readUk = { VERY_EASY: 'Дуже легко', EASY: 'Легко', STANDARD: 'Стандарт',
                     DIFFICULT: 'Складно', VERY_DIFFICULT: 'Дуже складно' };
    const repUk  = { HIGH: 'Висока', GOOD: 'Добра', UNKNOWN: 'Невідома', LOW: 'Низька' };

    const rows = data.results.map((r, i) => {
        const title = escHtml(r.title || 'Без заголовку');
        const score = r.credibilityScore ?? '—';
        return `<tr class="search-row-clickable" onclick="openSearchModal(${i})">
            <td class="col-title">${title}</td>
            <td>${escHtml(r.source || r.sourceDomain || '')}</td>
            <td class="col-num"><span class="cred-badge ${credBadgeClass(r.credibilityScore)}">${score}</span></td>
            <td class="col-num">${r.linguisticScore  ?? '—'}</td>
            <td class="col-num">${r.crossSourceScore ?? '—'}</td>
            <td class="col-num">${r.factCheckScore   ?? '—'}</td>
            <td>${sentUk[r.sentimentLabel] || (r.sentimentLabel ?? '—')}</td>
            <td>${readUk[r.readabilityLevel] || (r.readabilityLevel ?? '—')}</td>
            <td>${repUk[r.reputationTier]   || (r.reputationTier ?? '—')}</td>
        </tr>`;
    }).join('');

    document.getElementById('searchTableBody').innerHTML = rows;
    renderPagination(data.page, data.totalPages);
}

function renderPagination(current, total) {
    const el = document.getElementById('searchPagination');
    if (total <= 1) { el.innerHTML = ''; return; }

    const parts = [];
    parts.push(`<button class="page-btn" onclick="runSearch(${current - 1})" ${current === 0 ? 'disabled' : ''}>‹</button>`);

    const maxVisible = 7;
    let start = Math.max(0, current - 3);
    let end   = Math.min(total - 1, start + maxVisible - 1);
    if (end - start < maxVisible - 1) start = Math.max(0, end - maxVisible + 1);

    if (start > 0) {
        parts.push(`<button class="page-btn" onclick="runSearch(0)">1</button>`);
        if (start > 1) parts.push(`<span class="page-info">…</span>`);
    }
    for (let i = start; i <= end; i++) {
        parts.push(`<button class="page-btn ${i === current ? 'page-btn--active' : ''}" onclick="runSearch(${i})">${i + 1}</button>`);
    }
    if (end < total - 1) {
        if (end < total - 2) parts.push(`<span class="page-info">…</span>`);
        parts.push(`<button class="page-btn" onclick="runSearch(${total - 1})">${total}</button>`);
    }

    parts.push(`<button class="page-btn" onclick="runSearch(${current + 1})" ${current >= total - 1 ? 'disabled' : ''}>›</button>`);
    parts.push(`<span class="page-info">${current + 1} / ${total}</span>`);

    el.innerHTML = parts.join('');
}

function credBadgeClass(score) {
    if (score == null) return '';
    if (score <= 20) return 'cred-badge--very-low';
    if (score <= 40) return 'cred-badge--low';
    if (score <= 60) return 'cred-badge--medium';
    if (score <= 80) return 'cred-badge--high';
    return 'cred-badge--very-high';
}

/* also trigger search on Enter in text field */
document.addEventListener('DOMContentLoaded', () => {
    const sfSearch = document.getElementById('sfSearch');
    if (sfSearch) sfSearch.addEventListener('keydown', e => { if (e.key === 'Enter') runSearch(); });
});

/* ═══════════════════════════════════════
   MODAL
═══════════════════════════════════════ */
function openSearchModal(index) {
    const article = _searchResults[index];
    if (!article) return;
    _currentArticle = article;

    const sentUk = { POSITIVE: 'Позитивна', NEUTRAL: 'Нейтральна', NEGATIVE: 'Негативна' };
    const readUk = { VERY_EASY: 'Дуже легко', EASY: 'Легко', STANDARD: 'Стандарт',
                     DIFFICULT: 'Складно', VERY_DIFFICULT: 'Дуже складно' };
    const srcMap = {
        NEWS_API: ['badge--newsapi','NewsAPI'], GUARDIAN: ['badge--guardian','Guardian'],
        GNEWS:    ['badge--gnews','GNews'],     REDDIT:   ['badge--reddit','Reddit'],
        DATASET:  ['badge--dataset','Датасет'],
    };
    const [cls, label] = srcMap[article.sourceType] || ['badge--category', article.sourceType || ''];
    let badges = `<span class="badge ${cls}">${label}</span>`;
    if (article.category) badges += ` <span class="badge badge--category">${escHtml(article.category)}</span>`;
    document.getElementById('modalBadges').innerHTML = badges;

    const img = document.getElementById('modalImg');
    setModalImage(img, article.imageUrl, article.sourceType);

    document.getElementById('modalTitle').textContent  = article.title || 'Без заголовку';
    document.getElementById('modalSource').textContent = article.source || article.sourceDomain || '';
    document.getElementById('modalDate').textContent   = article.publishedAt
        ? new Date(article.publishedAt).toLocaleDateString('uk-UA', { day: 'numeric', month: 'short', year: 'numeric' })
        : '';
    document.getElementById('modalDesc').textContent   = article.description || '';
    document.getElementById('modalReadLink').href      = article.url || '#';

    const fullEl = document.getElementById('modalFullText');
    if (article.fullContent && article.fullContent !== article.description) {
        const text = article.fullContent.length > 1200
            ? article.fullContent.substring(0, 1200) + '…' : article.fullContent;
        fullEl.textContent = text;
        fullEl.style.display = 'block';
    } else {
        fullEl.style.display = 'none';
    }

    // Reset analysis UI
    document.getElementById('analysisResults').style.display = 'none';
    const btn = document.getElementById('analyzeBtn');
    btn.disabled = false;
    btn.textContent = 'Аналізувати';

    // If already analyzed — show existing results
    const id = article.newsItemId;
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
    if (!btn || !_currentArticle) return;
    const id = _currentArticle.newsItemId;
    const isFav = id && _userFavorites.has(id);
    btn.textContent = isFav ? '★' : '☆';
    btn.classList.toggle('star-btn--active', !!isFav);
}

function toggleFavorite() {
    if (!Auth.isLoggedIn()) { window.location.href = '/login'; return; }
    if (!_currentArticle) return;
    const id = _currentArticle.newsItemId;
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
        .catch(err => console.error('Favorites error:', err))
        .finally(() => { btn.disabled = false; });
}

function closeModal() {
    document.getElementById('articleModal').classList.remove('open');
    document.body.style.overflow = '';
    _currentArticle = null;
}

function modalOverlayClick(e) {
    if (e.target.id === 'articleModal') closeModal();
}

document.addEventListener('keydown', e => { if (e.key === 'Escape') closeModal(); });

function runManualAnalysis() {
    if (!_currentArticle) return;
    const id = _currentArticle.newsItemId;
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
    .then(result => {
        renderAnalysisResults(result);
        btn.textContent = 'Переаналізувати';
        btn.disabled = false;
    })
    .catch(err => {
        btn.textContent = 'Помилка: ' + err.message;
        btn.disabled = false;
    });
}

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

function scoreClass(s) {
    if (s <= 20) return 'score--very-low';
    if (s <= 40) return 'score--low';
    if (s <= 60) return 'score--medium';
    if (s <= 80) return 'score--high';
    return 'score--very-high';
}

/* ═══════════════════════════════════════
   UTILS
═══════════════════════════════════════ */
const SOURCE_PLACEHOLDER = {
    REDDIT:  { icon: '🤖', label: 'Reddit',  cls: 'img-placeholder--reddit'  },
    DATASET: { icon: '🗂️', label: 'Dataset', cls: 'img-placeholder--dataset' },
};

function sourcePlaceholderHtml(sourceType, extraCls = '') {
    const p = SOURCE_PLACEHOLDER[sourceType];
    if (!p) return '';
    return `<div class="img-placeholder ${p.cls} ${extraCls}">
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
                sourcePlaceholderHtml(sourceType, 'modal__img-placeholder img-placeholder'));
        };
    } else if (p) {
        imgEl.style.display = 'none';
        imgEl.insertAdjacentHTML('beforebegin',
            sourcePlaceholderHtml(sourceType, 'modal__img-placeholder img-placeholder'));
    } else {
        imgEl.style.display = 'none';
    }
}

function setText(id, val) {
    const el = document.getElementById(id);
    if (el) el.textContent = val;
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}
