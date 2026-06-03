/* ═══════════════════════════════════════
   STATE
═══════════════════════════════════════ */
const state = {
    page:       0,
    size:       15,
    source:     '',
    category:   '',
    search:     '',
    totalPages: 0,
    totalElements: 0,
};

/* ═══════════════════════════════════════
   DOM REFS
═══════════════════════════════════════ */
const newsGrid          = document.getElementById('newsGrid');
const pagination        = document.getElementById('pagination');
const totalCountEl      = document.getElementById('totalCount');
const activeFiltersEl   = document.getElementById('activeFiltersLabel');
const categoryFilters   = document.getElementById('categoryFilters');
const clearFiltersBtn   = document.getElementById('clearFilters');
const searchInput       = document.getElementById('searchInput');
const searchBtn         = document.getElementById('searchBtn');
const clearSearchBtn    = document.getElementById('clearSearch');

/* ═══════════════════════════════════════
   INIT
═══════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    renderNav();
    loadCategories();
    loadNews();
    bindEvents();
});

/* ═══════════════════════════════════════
   NAVBAR
═══════════════════════════════════════ */
function renderNav() {
    const nav = document.getElementById('navUser');
    if (!nav) return;
    if (Auth.isLoggedIn()) {
        const adminLink = Auth.getRole() === 'ADMIN'
            ? `<a href="/admin" class="btn btn--ghost" style="color:#f87171">Адмін</a>` : '';
        nav.innerHTML = `
            ${adminLink}
            <span class="navbar__username">${escHtml(Auth.getUsername())}</span>
            <button class="btn btn--ghost" onclick="Auth.logout()">Вийти</button>`;
    } else {
        nav.innerHTML = `
            <a href="/login"    class="btn btn--ghost">Увійти</a>
            <a href="/register" class="btn btn--outline">Реєстрація</a>`;
    }
}

/* ═══════════════════════════════════════
   EVENTS
═══════════════════════════════════════ */
function bindEvents() {
    // Source radio
    document.getElementById('sourceFilters').addEventListener('change', e => {
        state.source = e.target.value;
        state.page   = 0;
        updateSourceChips();
        loadNews();
    });

    // Search button
    searchBtn.addEventListener('click', triggerSearch);
    searchInput.addEventListener('keydown', e => { if (e.key === 'Enter') triggerSearch(); });

    // Show/hide clear-search X
    searchInput.addEventListener('input', () => {
        clearSearchBtn.classList.toggle('visible', searchInput.value.length > 0);
    });
    clearSearchBtn.addEventListener('click', () => {
        searchInput.value = '';
        clearSearchBtn.classList.remove('visible');
        state.search = '';
        state.page   = 0;
        loadNews();
    });

    // Mobile sidebar toggle
    const sidebar         = document.getElementById('sidebar');
    const sidebarBackdrop = document.getElementById('sidebarBackdrop');
    const filterToggle    = document.getElementById('filterToggle');

    function closeSidebar() {
        sidebar.classList.remove('open');
        sidebarBackdrop.classList.remove('open');
    }
    filterToggle.addEventListener('click', () => {
        sidebar.classList.toggle('open');
        sidebarBackdrop.classList.toggle('open');
    });
    sidebarBackdrop.addEventListener('click', closeSidebar);

    // Clear all filters
    clearFiltersBtn.addEventListener('click', () => {
        state.source   = '';
        state.category = '';
        state.search   = '';
        state.page     = 0;
        searchInput.value = '';
        clearSearchBtn.classList.remove('visible');
        document.querySelectorAll('input[name="source"]')[0].checked = true;
        updateSourceChips();
        updateCategoryChips();
        loadNews();
    });
}

function triggerSearch() {
    state.search = searchInput.value.trim();
    state.page   = 0;
    loadNews();
}

/* ═══════════════════════════════════════
   LOAD CATEGORIES
═══════════════════════════════════════ */
function loadCategories() {
    fetch('/news/categories')
        .then(r => r.json())
        .then(cats => {
            categoryFilters.innerHTML = '';

            const allChip = makeChip('All', true);
            allChip.addEventListener('click', () => {
                state.category = '';
                state.page     = 0;
                updateCategoryChips(null);
                loadNews();
            });
            categoryFilters.appendChild(allChip);

            cats.forEach(cat => {
                const chip = makeChip(cat, false);
                chip.dataset.cat = cat;
                chip.addEventListener('click', () => {
                    state.category = cat;
                    state.page     = 0;
                    updateCategoryChips(cat);
                    loadNews();
                });
                categoryFilters.appendChild(chip);
            });
        })
        .catch(() => {
            categoryFilters.innerHTML = '<span style="color:var(--text-muted);font-size:12px">No categories</span>';
        });
}

function makeChip(label, active) {
    const el = document.createElement('div');
    el.className = 'filter-chip' + (active ? ' filter-chip--active' : '');
    el.textContent = label;
    return el;
}

/* ═══════════════════════════════════════
   LOAD NEWS
═══════════════════════════════════════ */
function loadNews() {
    renderSkeletons();
    updateActiveFiltersLabel();

    const params = new URLSearchParams({
        page: state.page,
        size: state.size,
    });
    if (state.source)   params.set('source',   state.source);
    if (state.category) params.set('category', state.category);
    if (state.search)   params.set('search',   state.search);

    fetch(`/news?${params}`)
        .then(r => {
            if (!r.ok) throw new Error('HTTP ' + r.status);
            return r.json();
        })
        .then(page => {
            state.totalPages    = page.totalPages;
            state.totalElements = page.totalElements;
            renderCards(page.content || []);
            renderPagination();
            updateTotalCount();
        })
        .catch(err => {
            newsGrid.innerHTML = `
                <div class="feed-message">
                    <div class="feed-message__icon"></div>
                    <div class="feed-message__text">Failed to load news: ${err.message}</div>
                </div>`;
            pagination.innerHTML = '';
            totalCountEl.textContent = '';
        });
}

/* ═══════════════════════════════════════
   RENDER SKELETONS
═══════════════════════════════════════ */
function renderSkeletons() {
    newsGrid.innerHTML = Array.from({ length: 15 }, () => `
        <div class="card-skeleton">
            <div class="card-skeleton__img"></div>
            <div class="card-skeleton__body">
                <div class="card-skeleton__line card-skeleton__line--short"></div>
                <div class="card-skeleton__line"></div>
                <div class="card-skeleton__line"></div>
                <div class="card-skeleton__line card-skeleton__line--short"></div>
            </div>
        </div>`).join('');
    pagination.innerHTML = '';
}

/* ═══════════════════════════════════════
   RENDER CARDS
═══════════════════════════════════════ */
function renderCards(articles) {
    if (articles.length === 0) {
        newsGrid.innerHTML = `
            <div class="feed-message">
                <div class="feed-message__icon"></div>
                <div class="feed-message__text">No news found for selected filters.</div>
            </div>`;
        return;
    }

    // Зберігаємо статті в глобальному масиві — безпечна альтернатива JSON у onclick
    window._articles = articles;

    newsGrid.innerHTML = articles.map((a, i) => `
        <article class="news-card" style="animation-delay:${i * 40}ms" onclick="openModal(${i})">
            ${a.imageUrl
                ? `<img class="news-card__img" src="${escHtml(a.imageUrl)}" alt="" loading="lazy" onerror="this.replaceWith(placeholder('${a.sourceType}'))">`
                : sourcePlaceholderHtml(a.sourceType, 'news-card__img-placeholder')
            }
            <div class="news-card__body">
                <div class="news-card__meta">
                    ${sourceBadge(a.sourceType)}
                    ${a.category ? `<span class="badge badge--category">${escHtml(a.category)}</span>` : ''}
                    <span class="news-card__date">${formatDate(a.publishedAt)}</span>
                </div>
                <h3 class="news-card__title">${escHtml(a.title || 'Untitled')}</h3>
                ${a.description ? `<p class="news-card__desc">${escHtml(a.description)}</p>` : ''}
                <div class="news-card__footer">
                    <span class="news-card__source">${escHtml(a.source || '')}</span>
                    <div style="display:flex;gap:8px;align-items:center;">
                        <a class="news-card__link" href="/article/${a.id}" onclick="event.stopPropagation()">
                            Детальніше
                        </a>
                        <a class="news-card__link" href="${escHtml(a.url)}" target="_blank" onclick="event.stopPropagation()" style="color:var(--text-muted);font-size:12px;">
                            Джерело →
                        </a>
                    </div>
                </div>
            </div>
        </article>`).join('');
}

/* ═══════════════════════════════════════
   RENDER PAGINATION
═══════════════════════════════════════ */
function renderPagination() {
    const { page, totalPages } = state;
    if (totalPages <= 1) { pagination.innerHTML = ''; return; }

    let html = '';

    // Prev
    html += `<button class="page-btn" ${page === 0 ? 'disabled' : ''} onclick="goPage(${page - 1})">← Prev</button>`;

    // Page numbers with ellipsis
    const pages = getPageRange(page, totalPages);
    let prev = null;
    for (const p of pages) {
        if (prev !== null && p - prev > 1) html += `<span class="page-dots">…</span>`;
        html += `<button class="page-btn ${p === page ? 'page-btn--active' : ''}"
                    ${p === page ? '' : `onclick="goPage(${p})"`}>${p + 1}</button>`;
        prev = p;
    }

    // Next
    html += `<button class="page-btn" ${page >= totalPages - 1 ? 'disabled' : ''} onclick="goPage(${page + 1})">Next →</button>`;

    pagination.innerHTML = html;
}

function goPage(p) {
    state.page = p;
    loadNews();
    window.scrollTo({ top: 0, behavior: 'smooth' });
}

function getPageRange(current, total) {
    if (total <= 7) return Array.from({ length: total }, (_, i) => i);
    const pages = new Set([0, total - 1, current]);
    if (current > 0) pages.add(current - 1);
    if (current < total - 1) pages.add(current + 1);
    return [...pages].sort((a, b) => a - b);
}

/* ═══════════════════════════════════════
   UI HELPERS
═══════════════════════════════════════ */
function updateTotalCount() {
    totalCountEl.textContent = `${state.totalElements} article${state.totalElements !== 1 ? 's' : ''}`;
}

function updateActiveFiltersLabel() {
    const tags = [];
    if (state.source)   tags.push(`<span class="filter-tag"> ${state.source}</span>`);
    if (state.category) tags.push(`<span class="filter-tag"> ${state.category}</span>`);
    if (state.search)   tags.push(`<span class="filter-tag"> "${state.search}"</span>`);
    activeFiltersEl.innerHTML = tags.join('');
}

function updateSourceChips() {
    document.querySelectorAll('#sourceFilters .filter-chip').forEach(chip => {
        const radio = chip.querySelector('input[name="source"]');
        chip.classList.toggle('filter-chip--active', radio && radio.checked);
    });
}

function updateCategoryChips(activeCat) {
    document.querySelectorAll('#categoryFilters .filter-chip').forEach(chip => {
        const isAll = !chip.dataset.cat;
        chip.classList.toggle('filter-chip--active',
            activeCat == null ? isAll : chip.dataset.cat === activeCat);
    });
}

function sourceBadge(type) {
    const map = {
        NEWS_API:  ['badge--newsapi',  'NewsAPI'],
        GUARDIAN:  ['badge--guardian', 'Guardian'],
        GNEWS:     ['badge--gnews',    'GNews'],
        REDDIT:    ['badge--reddit',   'Reddit'],
        DATASET:   ['badge--dataset',  'Датасет'],
    };
    const [cls, label] = map[type] || ['badge--category', type || ''];
    return `<span class="badge ${cls}">${label}</span>`;
}

function formatDate(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function setModalImage(imgEl, imageUrl, sourceType) {
    const p = SOURCE_PLACEHOLDER[sourceType];
    // Remove any existing placeholder sibling
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

const SOURCE_PLACEHOLDER = {
    REDDIT:  { icon: '🤖', label: 'Reddit',  cls: 'img-placeholder--reddit'  },
    DATASET: { icon: '🗂️', label: 'Dataset', cls: 'img-placeholder--dataset' },
};

function sourcePlaceholderHtml(sourceType, extraCls = '') {
    const p = SOURCE_PLACEHOLDER[sourceType];
    if (!p) return `<div class="img-placeholder ${extraCls}"></div>`;
    return `<div class="img-placeholder ${p.cls} ${extraCls}">
                <span class="img-placeholder__icon">${p.icon}</span>${p.label}
            </div>`;
}

function placeholder(sourceType) {
    const el = document.createElement('div');
    const p  = SOURCE_PLACEHOLDER[sourceType];
    el.className = p
        ? `news-card__img-placeholder img-placeholder ${p.cls}`
        : 'news-card__img-placeholder';
    if (p) el.innerHTML = `<span class="img-placeholder__icon">${p.icon}</span>${p.label}`;
    return el;
}

/* expose goPage globally (used in inline onclick) */
window.goPage = goPage;

/* ═══════════════════════════════════════
   MODAL
═══════════════════════════════════════ */
let currentArticle = null;
let userFavorites  = new Set();

// Завантажуємо улюблені при старті
document.addEventListener('DOMContentLoaded', () => {
    if (Auth.isLoggedIn()) {
        fetch('/api/user/favorites', { headers: Auth.getHeaders() })
            .then(r => r.ok ? r.json() : [])
            .then(ids => { userFavorites = new Set(ids); })
            .catch(() => {});
    }
});

function openModal(indexOrArticle) {
    // Приймає або індекс з _articles або об'єкт (виклик із my-dashboard)
    const article = typeof indexOrArticle === 'number'
        ? (window._articles || [])[indexOrArticle]
        : indexOrArticle;
    if (!article) return;
    currentArticle = article;

    // Заповнюємо дані
    document.getElementById('modalBadges').innerHTML =
        sourceBadge(article.sourceType) +
        (article.category ? `<span class="badge badge--category">${escHtml(article.category)}</span>` : '');

    const img = document.getElementById('modalImg');
    setModalImage(img, article.imageUrl, article.sourceType);

    document.getElementById('modalTitle').textContent    = article.title || 'Без заголовку';
    document.getElementById('modalSource').textContent   = article.source || '';
    document.getElementById('modalDate').textContent     = formatDate(article.publishedAt);
    document.getElementById('modalDesc').textContent     = article.description || '';
    document.getElementById('modalReadLink').href        = article.url || '#';
    document.getElementById('modalDetailLink').href      = `/article/${article.id}`;

    const fullEl = document.getElementById('modalFullText');
    if (article.fullContent && article.fullContent !== article.description) {
        const text = article.fullContent.length > 1200
            ? article.fullContent.substring(0, 1200) + '…'
            : article.fullContent;
        fullEl.textContent = text;
        fullEl.style.display = 'block';
    } else {
        fullEl.style.display = 'none';
    }

    // Зірочка
    updateStarBtn();

    // Скидаємо результати аналізу
    document.getElementById('analysisResults').style.display = 'none';
    document.getElementById('analyzeBtn').disabled = false;
    document.getElementById('analyzeBtn').textContent = 'Аналізувати';

    // Відкриваємо
    document.getElementById('articleModal').classList.add('open');
    document.body.style.overflow = 'hidden';
}

function closeModal() {
    document.getElementById('articleModal').classList.remove('open');
    document.body.style.overflow = '';
    currentArticle = null;
}

function modalOverlayClick(e) {
    if (e.target.id === 'articleModal') closeModal();
}

document.addEventListener('keydown', e => {
    if (e.key === 'Escape') closeModal();
});

/* ── Favorites ── */
function updateStarBtn() {
    const btn = document.getElementById('starBtn');
    if (!currentArticle) return;
    const isFav = userFavorites.has(currentArticle.id);
    btn.textContent = isFav ? '★' : '☆';
    btn.classList.toggle('star-btn--active', isFav);
}

function toggleFavorite() {
    if (!Auth.isLoggedIn()) {
        showAuthModal('analyze');
        return;
    }
    if (!currentArticle || !currentArticle.id) return;

    const btn = document.getElementById('starBtn');
    btn.disabled = true;

    fetch(`/api/user/favorites/${currentArticle.id}`, {
        method: 'POST',
        headers: Auth.getHeaders()
    })
    .then(r => {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    })
    .then(data => {
        if (data.added) userFavorites.add(currentArticle.id);
        else            userFavorites.delete(currentArticle.id);
        updateStarBtn();
    })
    .catch(err => console.error('Favorites error:', err))
    .finally(() => { btn.disabled = false; });
}

/* ── Layer sub-method toggling ── */
function toggleLayerSubs(layer, enabled) {
    const wrap = document.getElementById(`subs-${layer}`);
    if (!wrap) return;
    wrap.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.disabled = !enabled;
        cb.parentElement.style.opacity = enabled ? '1' : '0.4';
    });
}

/* ── Manual Analysis ── */
function runManualAnalysis() {
    if (!currentArticle) return;

    if (!Auth.isLoggedIn()) {
        showAuthModal('analyze');
        return;
    }

    const cb = id => { const el = document.getElementById(id); return el ? el.checked : true; };
    const options = {
        runLinguistic:  cb('optLinguistic'),
        linguistic: {
            hedgeWords:            cb('subHedge'),
            clickbait:             cb('subClickbait'),
            emotional:             cb('subEmotional'),
            manipulation:          cb('subManipulation'),
            conspiracy:            cb('subConspiracy'),
            anonymousSources:      cb('subAnonSources'),
            citationDensity:       cb('subCitationDensity'),
            headlineConsistency:   cb('subHeadlineConsistency'),
            betteridge:            cb('subBetteridge'),
        },
        runCrossSource: cb('optCrossSource'),
        runFactCheck:   cb('optFactCheck'),
        factCheck: {
            sourceReputation:    cb('subSourceRep'),
            claimBuster:         cb('subClaimBuster'),
            rssCheck:            cb('subRss'),
            extendedUrlAnalysis: cb('subExtendedUrl'),
            recycledNews:        cb('subRecycledNews'),
        },
        runSentiment:   cb('optSentiment'),
        runReadability: cb('optReadability'),
        runWikipedia:   cb('optWikipedia'),
    };

    const btn = document.getElementById('analyzeBtn');
    btn.disabled = true;
    btn.textContent = 'Аналізується...';

    const headers = Auth.isLoggedIn()
        ? Auth.getHeaders()
        : { 'Content-Type': 'application/json' };

    fetch(`/analysis/${currentArticle.id}/manual`, {
        method:  'POST',
        headers,
        body:    JSON.stringify(options),
    })
    .then(r => {
        if (!r.ok) throw new Error('HTTP ' + r.status);
        return r.json();
    })
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
    const container = document.getElementById('analysisResults');
    container.style.display = 'block';

    // Language disclaimer
    const disclaimer = document.getElementById('langDisclaimer');
    if (disclaimer) {
        if (r.languageNote) {
            disclaimer.textContent = r.languageNote;
            disclaimer.style.display = 'flex';
        } else {
            disclaimer.style.display = 'none';
        }
    }

    // Score circle
    const score = r.credibilityScore ?? 0;
    const circle = document.getElementById('scoreCircle');
    document.getElementById('scoreValue').textContent = score;
    circle.className = 'score-display__circle ' + scoreClass(score);

    const labels = ['Дуже низька', 'Низька', 'Помірна', 'Висока', 'Дуже висока'];
    document.getElementById('scoreLabel').textContent = labels[Math.floor(score / 21)] ?? 'Висока';

    // Layer bars
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

    // Extra metrics
    const sentiment  = r.sentimentLabel !== 'N/A' ? sentimentUk(r.sentimentLabel) + ` (${r.sentimentScore?.toFixed(2)})` : '—';
    const readability = r.readabilityLevel !== 'N/A' ? readabilityUk(r.readabilityLevel) + ` (${r.readabilityScore?.toFixed(0)})` : '—';

    // Details
    const details = [];
    if (r.hedgeWordsFound?.length)      details.push(`<li>Хедж-слова: <em>${escHtml(r.hedgeWordsFound.join(', '))}</em></li>`);
    if (r.clickbaitIndicators?.length)  details.push(`<li>Клікбейт: <em>${escHtml(r.clickbaitIndicators.join(', '))}</em></li>`);
    if (r.emotionalWordsFound?.length)  details.push(`<li>Емоційна лексика: <em>${escHtml(r.emotionalWordsFound.join(', '))}</em></li>`);
    if (r.manipulationIndicators?.length) details.push(`<li>Маніпуляція: <em>${escHtml(r.manipulationIndicators.join(', '))}</em></li>`);
    if (r.conspiracyIndicators?.length) details.push(`<li>Конспірологія: <em>${escHtml(r.conspiracyIndicators.join(', '))}</em></li>`);
    if (r.confirmedSources?.length)     details.push(`<li>Підтверджено: <em>${escHtml(r.confirmedSources.join(', '))}</em></li>`);
    if (r.sourceDomain)                 details.push(`<li>Домен: <em>${escHtml(r.sourceDomain)}</em> — ${escHtml(r.sourceReputationTier)}</li>`);

    document.getElementById('scoreDetails').innerHTML = `
        <div class="score-extras">
            <span>Тональність: <strong>${sentiment}</strong></span>
            <span>Читабельність: <strong>${readability}</strong></span>
            ${r.wordCount ? `<span>Слів: <strong>${r.wordCount}</strong></span>` : ''}
        </div>
        ${details.length ? `<ul class="score-indicators">${details.join('')}</ul>` : ''}`;

    document.getElementById('scoreReasoning').innerHTML =
        r.reasoning ? `<div class="score-reasoning__text">${escHtml(r.reasoning)}</div>` : '';
}

function scoreClass(s) {
    if (s <= 20) return 'score--very-low';
    if (s <= 40) return 'score--low';
    if (s <= 60) return 'score--medium';
    if (s <= 80) return 'score--high';
    return 'score--very-high';
}

function sentimentUk(l) {
    return { POSITIVE: 'Позитивна', NEUTRAL: 'Нейтральна', NEGATIVE: 'Негативна' }[l] || l;
}
function readabilityUk(l) {
    return { VERY_EASY: 'Дуже легко', EASY: 'Легко', STANDARD: 'Стандарт', DIFFICULT: 'Складно', VERY_DIFFICULT: 'Дуже складно' }[l] || l;
}

/* ── Auth modal ── */
const AUTH_MESSAGES = {
    analyze:    'Щоб аналізувати новини на достовірність, будь ласка, увійдіть або зареєструйтесь.',
    dashboard:  'Дашборд доступний лише для зареєстрованих користувачів.',
    addNews:    'Додавання новин доступне лише для зареєстрованих користувачів.',
    cabinet:    'Особистий кабінет доступний лише для зареєстрованих користувачів.',
};

function showAuthModal(msgKey = 'analyze') {
    document.getElementById('authModalMsg').textContent = AUTH_MESSAGES[msgKey] || AUTH_MESSAGES.analyze;
    document.getElementById('authModal').style.display = 'flex';
    document.body.style.overflow = 'hidden';
}
function closeAuthModal() {
    document.getElementById('authModal').style.display = 'none';
    document.body.style.overflow = '';
}

/* Перехоплює кліки на захищені nav-посилання для гостей */
function guardedNav(event, link) {
    if (Auth.isLoggedIn()) return; // залогінений — переходимо нормально
    event.preventDefault();
    const href = link.getAttribute('href');
    const key = href === '/dashboard'    ? 'dashboard'
              : href === '/add-news'     ? 'addNews'
              : href === '/my-dashboard' ? 'cabinet'
              : 'analyze';
    showAuthModal(key);
}

window.openModal        = openModal;
window.closeModal       = closeModal;
window.closeAuthModal   = closeAuthModal;
window.guardedNav       = guardedNav;
window.toggleFavorite   = toggleFavorite;
window.runManualAnalysis= runManualAnalysis;
window.modalOverlayClick= modalOverlayClick;
window.toggleLayerSubs  = toggleLayerSubs;
