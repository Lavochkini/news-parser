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
        nav.innerHTML = `
            <span class="navbar__username">${escHtml(Auth.getUsername())}</span>
            <button class="btn btn--ghost" onclick="Auth.logout()">Logout</button>`;
    } else {
        nav.innerHTML = `
            <a href="/login"    class="btn btn--ghost">Login</a>
            <a href="/register" class="btn btn--outline">Register</a>`;
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

    newsGrid.innerHTML = articles.map((a, i) => `
        <article class="news-card" style="animation-delay:${i * 40}ms" onclick="window.open('${escHtml(a.url)}','_blank')">
            ${a.imageUrl
                ? `<img class="news-card__img" src="${escHtml(a.imageUrl)}" alt="" loading="lazy" onerror="this.replaceWith(placeholder())">`
                : `<div class="news-card__img-placeholder"></div>`
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
                    <a class="news-card__link" href="${escHtml(a.url)}" target="_blank" onclick="event.stopPropagation()">
                        Read more →
                    </a>
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

function placeholder() {
    const el = document.createElement('div');
    el.className = 'news-card__img-placeholder';
    el.textContent = '';
    return el;
}

/* expose goPage globally (used in inline onclick) */
window.goPage = goPage;
