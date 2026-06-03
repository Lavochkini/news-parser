/* ═══════════════════════════════════════
   ARTICLE PAGE  —  article.js
═══════════════════════════════════════ */

/* ── State ── */
let _newsId      = null;
let _article     = null;
let _analysis    = null;
let _fullTextOpen = false;

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    renderNav();

    // Extract article id from URL: /article/{id}
    const parts = window.location.pathname.split('/');
    _newsId = parts[parts.length - 1];

    if (!_newsId) { showArticleError(); return; }

    loadArticle();
});

/* ════════════════════════════════════════
   NAVBAR
════════════════════════════════════════ */
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

/* ════════════════════════════════════════
   ARTICLE LOADING
════════════════════════════════════════ */
async function loadArticle() {
    try {
        const resp = await fetch(`/news/${_newsId}`);
        if (!resp.ok) { showArticleError(); return; }
        _article = await resp.json();
        renderArticle(_article);
    } catch (e) {
        showArticleError();
    }
}

function renderArticle(a) {
    document.title = `${a.title || 'Стаття'} — Fistserious`;

    // Meta badges
    const metaEl = document.getElementById('artMeta');
    metaEl.innerHTML = buildMetaBadges(a);

    // Title
    document.getElementById('artTitle').textContent = a.title || '';

    // Sub-meta: source, date
    const subEl = document.getElementById('artSubMeta');
    const dateStr = a.publishedAt
        ? new Date(a.publishedAt).toLocaleDateString('uk-UA', { day:'numeric', month:'long', year:'numeric' })
        : '';
    subEl.innerHTML = [
        a.source    ? `<span>${escHtml(a.source)}</span>` : '',
        a.category  ? `<span class="art-category-badge">${escHtml(a.category)}</span>` : '',
        dateStr     ? `<span>${dateStr}</span>` : ''
    ].filter(Boolean).join('<span class="art-dot">·</span>');

    // Image
    const imgWrap = document.getElementById('artImageWrap');
    if (a.imageUrl) {
        imgWrap.innerHTML = `<img class="art-hero-img" src="${escHtml(a.imageUrl)}" alt="" loading="lazy" onerror="this.parentElement.style.display='none'">`;
    } else {
        imgWrap.style.display = 'none';
    }

    // Description
    const descEl = document.getElementById('artDescription');
    if (a.description) {
        descEl.textContent = a.description;
    } else {
        descEl.style.display = 'none';
    }

    // Full text
    if (a.fullContent && a.fullContent.trim()) {
        document.getElementById('artFulltext').textContent = a.fullContent;
        document.getElementById('artFulltextWrap').style.display = 'none';
        document.getElementById('artFulltextBtn').style.display  = 'inline-flex';
    }

    // Source link
    const srcEl = document.getElementById('artSourceLink');
    if (a.url) {
        srcEl.innerHTML = `<a href="${escHtml(a.url)}" target="_blank" rel="noopener" class="btn btn--ghost">Відкрити оригінал →</a>`;
    }

    // Show article body, hide skeleton
    document.getElementById('artSkeleton').style.display = 'none';
    document.getElementById('artBody').style.display     = 'block';

    // Now load analysis
    loadAnalysis();
}

function buildMetaBadges(a) {
    const sourceColors = { NEWSAPI:'#7c3aed', GUARDIAN:'#dc2626', GNEWS:'#059669', REDDIT:'#ff4500', MANUAL:'#58a6ff' };
    const color = sourceColors[(a.sourceType || '').toUpperCase()] || '#58a6ff';
    const label = (a.sourceType || '').toLowerCase().replace('_',' ');
    return label ? `<span class="art-source-badge" style="background:${color}20;color:${color};border-color:${color}40">${escHtml(label)}</span>` : '';
}

function showArticleError() {
    document.getElementById('artSkeleton').style.display = 'none';
    document.getElementById('artError').style.display    = 'block';
    document.getElementById('apLoading').style.display   = 'none';
    document.getElementById('apNoAnalysis').style.display= 'block';
}

/* ════════════════════════════════════════
   FULL TEXT TOGGLE
════════════════════════════════════════ */
function toggleFullText() {
    _fullTextOpen = !_fullTextOpen;
    const wrap = document.getElementById('artFulltextWrap');
    const btn  = document.getElementById('artFulltextBtn');
    wrap.style.display  = _fullTextOpen ? 'block' : 'none';
    btn.textContent     = _fullTextOpen ? 'Сховати повний текст' : 'Показати повний текст';
}

/* ════════════════════════════════════════
   ANALYSIS LOADING
════════════════════════════════════════ */
async function loadAnalysis() {
    // If guest — show auth prompt
    if (!Auth.isLoggedIn()) {
        showApState('apGuest');
        return;
    }

    // Check if analysis already exists (fast, no triggering)
    try {
        const token = Auth.getToken();
        const resp  = await fetch(`/analysis/${_newsId}/check`, {
            headers: token ? { 'Authorization': `Bearer ${token}` } : {}
        });

        if (resp.ok) {
            const exists = await resp.json();
            if (exists) {
                // Analysis exists — fetch and render it
                await fetchAndRenderAnalysis();
            } else {
                // No analysis yet — show button
                showApState('apNoAnalysis');
            }
        } else {
            showApState('apNoAnalysis');
        }
    } catch (e) {
        showApState('apNoAnalysis');
    }
}

async function fetchAndRenderAnalysis() {
    showApState('apLoading');
    try {
        const token = Auth.getToken();
        const resp  = await fetch(`/analysis/${_newsId}`, {
            headers: token ? { 'Authorization': `Bearer ${token}` } : {}
        });
        if (!resp.ok) {
            showApState('apNoAnalysis');
            return;
        }
        _analysis = await resp.json();
        renderAnalysis(_analysis);
    } catch (e) {
        showApState('apNoAnalysis');
    }
}

function buildAnalysisOptions() {
    const cb = id => { const el = document.getElementById(id); return el ? el.checked : true; };
    return {
        runLinguistic:  cb('artOptLinguistic'),
        linguistic: {
            hedgeWords:            cb('artSubHedge'),
            clickbait:             cb('artSubClickbait'),
            emotional:             cb('artSubEmotional'),
            manipulation:          cb('artSubManipulation'),
            conspiracy:            cb('artSubConspiracy'),
            anonymousSources:      cb('artSubAnonSources'),
            citationDensity:       cb('artSubCitationDensity'),
            headlineConsistency:   cb('artSubHeadlineConsistency'),
            betteridge:            cb('artSubBetteridge'),
        },
        runCrossSource: cb('artOptCrossSource'),
        runFactCheck:   cb('artOptFactCheck'),
        factCheck: {
            sourceReputation:    cb('artSubSourceRep'),
            claimBuster:         cb('artSubClaimBuster'),
            rssCheck:            cb('artSubRss'),
            extendedUrlAnalysis: cb('artSubExtendedUrl'),
            recycledNews:        cb('artSubRecycledNews'),
        },
        runSentiment:   cb('artOptSentiment'),
        runReadability: cb('artOptReadability'),
        runWikipedia:   cb('artOptWikipedia'),
    };
}

function toggleSharedConfig() {
    const cfg  = document.getElementById('sharedConfig');
    const open = cfg.style.display === 'none';
    cfg.style.display = open ? 'block' : 'none';
}

function toggleLayerSubsArt(layer, enabled) {
    const wrap = document.getElementById(`artSubs-${layer}`);
    if (!wrap) return;
    wrap.querySelectorAll('input[type="checkbox"]').forEach(cb => {
        cb.disabled = !enabled;
        cb.parentElement.style.opacity = enabled ? '1' : '0.4';
    });
}

async function runAnalysis() {
    showApState('apRunning');
    const options = buildAnalysisOptions();
    const token   = Auth.getToken();
    try {
        const resp = await fetch(`/analysis/${_newsId}/manual`, {
            method:  'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            },
            body: JSON.stringify(options),
        });
        if (!resp.ok) {
            showApState('apNoAnalysis');
            showToast('Помилка аналізу', 'error');
            return;
        }
        _analysis = await resp.json();
        renderAnalysis(_analysis);
    } catch (e) {
        showApState('apNoAnalysis');
        showToast('Помилка аналізу', 'error');
    }
}

async function reanalyze() {
    const btn = document.getElementById('reanalyzeBtn');
    if (btn) { btn.disabled = true; btn.textContent = '...'; }
    const options = buildAnalysisOptions();
    const token   = Auth.getToken();
    try {
        const resp = await fetch(`/analysis/${_newsId}/manual`, {
            method:  'POST',
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            },
            body: JSON.stringify(options),
        });
        if (!resp.ok) { showToast('Помилка перезапуску', 'error'); return; }
        _analysis = await resp.json();
        renderAnalysis(_analysis);
        showToast('Аналіз оновлено', 'success');
    } catch (e) {
        showToast('Помилка перезапуску', 'error');
    } finally {
        if (btn) { btn.disabled = false; btn.textContent = 'Оновити'; }
    }
}

/* ════════════════════════════════════════
   RENDER ANALYSIS
════════════════════════════════════════ */
function renderAnalysis(a) {
    showApState('apResult');

    // ── Gauge ──
    animateGauge(a.credibilityScore || 0);

    // ── Score bars ──
    animateBar('barLinguistic', 'valLinguistic', a.linguisticScore || 0, 35);
    animateBar('barCross',      'valCross',      a.crossSourceScore || 0, 35);
    animateBar('barFact',       'valFact',       a.factCheckScore   || 0, 30);

    // ── Meta ──
    const apMeta = document.getElementById('apMeta');
    const dateStr = a.analyzedAt
        ? new Date(a.analyzedAt).toLocaleDateString('uk-UA', { day:'numeric', month:'short', year:'numeric' })
        : '';
    const typeLabel = a.manual ? 'Ручний аналіз' : 'Авто-аналіз';
    const typeColor = a.manual ? 'var(--accent)' : 'var(--text-muted)';
    apMeta.innerHTML = `
        <span style="color:${typeColor}">${typeLabel}</span>
        ${a.analyzedByUsername ? `<span>• ${escHtml(a.analyzedByUsername)}</span>` : ''}
        ${dateStr ? `<span>• ${dateStr}</span>` : ''}`;

    // ── Language disclaimer ──
    const disclaimer = document.getElementById('langDisclaimer');
    if (disclaimer) {
        if (a.languageNote) {
            disclaimer.textContent = a.languageNote;
            disclaimer.style.display = 'flex';
        } else {
            disclaimer.style.display = 'none';
        }
    }

    // ── Tabs ──
    document.getElementById('artTabsSection').style.display = 'block';
    renderTabOverview(a);
    renderTabLinguistic(a);
    renderTabCross(a);
    renderTabFact(a);
    renderTabReadability(a);
}

/* ── Gauge animation ── */
function animateGauge(score) {
    const fill      = document.getElementById('gaugeFill');
    const scoreEl   = document.getElementById('gaugeScore');
    const labelEl   = document.getElementById('gaugeLabel');
    const circ      = 314.16; // 2π×50

    // Color based on score
    const color = scoreColor(score);
    fill.style.stroke = color;

    // Animate score counter
    let current = 0;
    const step  = Math.ceil(score / 40);
    const timer = setInterval(() => {
        current = Math.min(current + step, score);
        scoreEl.textContent = current;
        // dashoffset: full = circ (0%), zero = 0 (100%)
        const offset = circ - (current / 100) * circ;
        fill.style.strokeDashoffset = offset;
        if (current >= score) clearInterval(timer);
    }, 30);

    scoreEl.style.color = color;
    labelEl.textContent = credibilityLabel(score);
    labelEl.style.color = color;
}

function animateBar(barId, valId, score, max) {
    const bar = document.getElementById(barId);
    const val = document.getElementById(valId);
    if (!bar || !val) return;
    const pct = Math.round((score / max) * 100);
    val.textContent = score + '/' + max;
    // Trigger after a tiny delay so CSS transition kicks in
    requestAnimationFrame(() => {
        setTimeout(() => { bar.style.width = pct + '%'; }, 50);
    });
}

/* ── Credibility helpers ── */
function credibilityLabel(s) {
    if (s >= 80) return 'Надійно';
    if (s >= 60) return 'Загалом надійно';
    if (s >= 40) return 'Сумнівно';
    if (s >= 20) return 'Ненадійно';
    return 'Неправдива інформація';
}

function scoreColor(s) {
    if (s >= 80) return '#3fb950';
    if (s >= 60) return '#58a6ff';
    if (s >= 40) return '#d29922';
    if (s >= 20) return '#f0883e';
    return '#f85149';
}

/* ════════════════════════════════════════
   TAB: ОГЛЯД
════════════════════════════════════════ */
function renderTabOverview(a) {
    const grid = document.getElementById('overviewGrid');

    const cards = [
        {
            title: 'Загальний рейтинг',
            value: `<span style="font-size:2rem;font-weight:700;color:${scoreColor(a.credibilityScore)}">${a.credibilityScore}</span><span style="color:var(--text-muted)">/100</span>`,
            sub: credibilityLabel(a.credibilityScore)
        },
        {
            title: 'Лінгвістичний аналіз',
            value: `<span style="font-size:1.6rem;font-weight:600;">${a.linguisticScore}</span><span style="color:var(--text-muted)">/35</span>`,
            sub: a.linguisticScore >= 28 ? 'Хороший' : a.linguisticScore >= 18 ? 'Задовільний' : 'Слабкий'
        },
        {
            title: 'Крос-перевірка',
            value: `<span style="font-size:1.6rem;font-weight:600;">${a.crossSourceScore}</span><span style="color:var(--text-muted)">/35</span>`,
            sub: `${a.sourcesConfirmed || 0} підтверджуючих джерел`
        },
        {
            title: 'Факт-чек',
            value: `<span style="font-size:1.6rem;font-weight:600;">${a.factCheckScore}</span><span style="color:var(--text-muted)">/30</span>`,
            sub: a.factCheckVerdict ? escHtml(a.factCheckVerdict) : '—'
        }
    ];

    grid.innerHTML = cards.map(c => `
        <div class="overview-card">
            <div class="overview-card__title">${c.title}</div>
            <div class="overview-card__value">${c.value}</div>
            <div class="overview-card__sub">${c.sub}</div>
        </div>`).join('');

    // Reasoning block
    if (a.reasoning) {
        grid.innerHTML += `
            <div class="overview-reasoning">
                <div class="overview-reasoning__title">Висновок аналізу</div>
                <p>${escHtml(a.reasoning)}</p>
            </div>`;
    }
}

/* ════════════════════════════════════════
   TAB: ЛІНГВІСТИКА
════════════════════════════════════════ */
function renderTabLinguistic(a) {
    const grid = document.getElementById('lingGrid');

    const sections = [
        { title: 'Маніпулятивна лексика',  words: a.manipulationIndicators,  color: '#f85149' },
        { title: 'Клікбейт-індикатори',     words: a.clickbaitIndicators,     color: '#f85149' },
        { title: 'Конспірологічні маркери', words: a.conspiracyIndicators,    color: '#f85149' },
        { title: 'Анонімна атрибуція',      words: a.anonymousSourcesFound,   color: '#d29922' },
        { title: 'Емоційна лексика',         words: a.emotionalWordsFound,     color: '#f0883e' },
        { title: 'Хедж-слова',              words: a.hedgeWordsFound,         color: '#8b949e' },
    ];

    const existing = sections.filter(s => s.words && s.words.length > 0);

    let html = existing.length === 0
        ? emptyState('Тривожна лексика відсутня. Текст виглядає нейтрально.')
        : existing.map(s => {
            const bg = hexToRgba(s.color, 0.12);
            const bd = hexToRgba(s.color, 0.3);
            return `
            <div class="ling-section">
                <div class="ling-section__title" style="color:${s.color}">${s.title}</div>
                <div class="tag-cloud">
                    ${s.words.map(w => `<span class="tag" style="background:${bg};color:${s.color};border-color:${bd}">${escHtml(w)}</span>`).join('')}
                </div>
            </div>`;
        }).join('');

    // ── Нові метрики ──
    const citColor = (a.citationCount || 0) >= 3 ? '#3fb950' : (a.citationCount || 0) >= 1 ? '#d29922' : '#f85149';
    const overlapPct = Math.round((a.headlineBodyOverlap || 0) * 100);
    const overlapColor = overlapPct >= 40 ? '#3fb950' : overlapPct >= 20 ? '#d29922' : '#f85149';

    html += `
        <div class="ling-section ling-section--meta">
            <div class="ling-section__title">Додаткові лінгвістичні метрики</div>
            <div class="ling-meta-grid">
                <div class="ling-meta-item">
                    <span class="ling-meta__label">Цитат знайдено</span>
                    <span class="ling-meta__value" style="color:${citColor}">${a.citationCount || 0}</span>
                    <span class="ling-meta__hint">${(a.citationCount || 0) >= 3 ? '✓ достатньо' : (a.citationCount || 0) >= 1 ? '⚠ мало' : '✗ відсутні'}</span>
                </div>
                <div class="ling-meta-item">
                    <span class="ling-meta__label">Заголовок / тіло</span>
                    <span class="ling-meta__value" style="color:${overlapColor}">${overlapPct}%</span>
                    <span class="ling-meta__hint">спільних ключових слів</span>
                </div>
                ${a.betteridgeDetected ? `
                <div class="ling-meta-item ling-meta-item--warn">
                    <span class="ling-meta__label">Закон Беттерідж</span>
                    <span class="ling-meta__value" style="color:#f85149">Так</span>
                    <span class="ling-meta__hint">заголовок-питання (відповідь — «Ні»)</span>
                </div>` : ''}
            </div>
        </div>`;

    grid.innerHTML = html;

    // Sentiment block
    if (a.sentimentLabel) {
        const sentColor = a.sentimentLabel === 'POSITIVE' ? '#3fb950' : a.sentimentLabel === 'NEGATIVE' ? '#f85149' : '#8b949e';
        const sentLabelUa = a.sentimentLabel === 'POSITIVE' ? 'Позитивний' : a.sentimentLabel === 'NEGATIVE' ? 'Негативний' : 'Нейтральний';
        grid.innerHTML += `
            <div class="ling-section ling-section--sentiment">
                <div class="ling-section__title">Тональність тексту</div>
                <div class="sentiment-row">
                    <span class="sentiment-label" style="color:${sentColor}">${sentLabelUa}</span>
                    <div class="sentiment-bar-wrap">
                        <div class="sentiment-bar" style="background:${sentColor};width:${Math.round(Math.abs(a.sentimentScore || 0) * 100)}%"></div>
                    </div>
                    <span class="sentiment-val">${((a.sentimentScore || 0) >= 0 ? '+' : '') + (a.sentimentScore || 0).toFixed(2)}</span>
                </div>
                <div class="sentiment-meta">Слів знайдено: ${a.sentimentWordsMatched || 0}</div>
            </div>`;
    }
}

/* ════════════════════════════════════════
   TAB: КРОС-ПЕРЕВІРКА
════════════════════════════════════════ */
function renderTabCross(a) {
    const grid = document.getElementById('crossGrid');

    let html = `
        <div class="cross-summary">
            <div class="cross-summary__score" style="color:${scoreColor(a.crossSourceScore * 100 / 35)}">
                ${a.sourcesConfirmed || 0}
            </div>
            <div class="cross-summary__label">підтверджуючих джерел</div>
            <div class="cross-summary__sub">Бал крос-перевірки: ${a.crossSourceScore}/35</div>
        </div>`;

    if (a.keywordsUsed) {
        html += `
            <div class="cross-keywords">
                <div class="cross-keywords__title">Ключові слова пошуку</div>
                <div class="tag-cloud">
                    ${a.keywordsUsed.split(',').map(k => `<span class="tag">${escHtml(k.trim())}</span>`).join('')}
                </div>
            </div>`;
    }

    if (a.confirmedSources && a.confirmedSources.length > 0) {
        html += `
            <div class="cross-sources">
                <div class="cross-sources__title">Джерела підтвердження</div>
                <ul class="cross-sources__list">
                    ${a.confirmedSources.map(s => `<li>${escHtml(s)}</li>`).join('')}
                </ul>
            </div>`;
    } else {
        html += emptyState('Підтверджуючих джерел не знайдено.');
    }

    grid.innerHTML = html;
}

/* ════════════════════════════════════════
   TAB: ФАКТ-ЧЕК
════════════════════════════════════════ */
function renderTabFact(a) {
    const grid = document.getElementById('factGrid');

    const urlTier  = a.urlAnalysisTier  || 'CLEAN';
    const urlColor = urlTier === 'FAKE' ? '#f85149' : urlTier === 'SUSPICIOUS' ? '#d29922' : '#3fb950';

    let html = `
        <div class="fact-layers">
            <div class="fact-layer">
                <div class="fact-layer__name">Репутація домену</div>
                <div class="fact-layer__score">${a.sourceReputationScore || 0}/12</div>
                <div class="fact-layer__detail">
                    ${a.sourceDomain ? escHtml(a.sourceDomain) : '—'}
                    ${a.sourceReputationTier ? `<span class="fact-tier">${escHtml(a.sourceReputationTier)}</span>` : ''}
                </div>
            </div>
            <div class="fact-layer">
                <div class="fact-layer__name">URL-аналіз</div>
                <div class="fact-layer__score" style="color:${urlColor}">${urlTier}</div>
                <div class="fact-layer__detail">
                    ${a.urlPenalty ? `Штраф: ${a.urlPenalty}` : 'Без штрафу'}
                    ${a.urlWarnings && a.urlWarnings.length ? `<br><span style="color:${urlColor};font-size:11px">${a.urlWarnings.map(escHtml).join('; ')}</span>` : ''}
                </div>
            </div>
            <div class="fact-layer">
                <div class="fact-layer__name">ClaimBuster</div>
                <div class="fact-layer__score">${a.claimBusterScore || 0}/10</div>
                <div class="fact-layer__detail">Сирий бал: ${(a.claimBusterRaw || 0).toFixed(3)}</div>
            </div>
            <div class="fact-layer">
                <div class="fact-layer__name">RSS перевірка</div>
                <div class="fact-layer__score">${a.rssCheckScore || 0}/8</div>
                <div class="fact-layer__detail">
                    ${a.rssMatchedTitle ? escHtml(a.rssMatchedTitle) : 'Збігів не знайдено'}
                    ${a.rssMatchSource ? `<span class="fact-tier">${escHtml(a.rssMatchSource)}</span>` : ''}
                </div>
            </div>
            ${a.recycledNewsDetected ? `
            <div class="fact-layer fact-layer--warn">
                <div class="fact-layer__name" style="color:#f85149">⚠ Перевидана новина</div>
                <div class="fact-layer__score" style="color:#f85149">${a.recycledNewsPenalty || 0}</div>
                <div class="fact-layer__detail">${escHtml(a.recycledNewsReason || '')}</div>
            </div>` : ''}
        </div>`;

    if (a.factCheckClaims && a.factCheckClaims.length > 0) {
        html += `
            <div class="fact-claims">
                <div class="fact-claims__title">Перевірені твердження (${a.factCheckClaims.length})</div>
                ${a.factCheckClaims.map(c => `
                    <div class="fact-claim">
                        <span class="fact-claim__text">${escHtml(c.text || c.claim || '')}</span>
                        <span class="fact-claim__score">${c.score !== undefined ? c.score.toFixed(2) : ''}</span>
                    </div>`).join('')}
            </div>`;
    }

    // ── Wikipedia entity block ──
    if (a.wikiEntitiesTotal > 0) {
        const found = a.wikiEntitiesFound || [];
        const checked = a.wikiEntitiesChecked || [];
        const notFound = checked.filter(e => !found.includes(e));
        html += `
            <div class="fact-wiki">
                <div class="fact-wiki__title">Wikipedia-верифікація сутностей</div>
                <div class="fact-wiki__stats">Перевірено: ${a.wikiEntitiesTotal} · Підтверджено: ${found.length}</div>
                ${found.length ? `<div class="tag-cloud">${found.map(e => `<span class="tag tag--good">${escHtml(e)}</span>`).join('')}</div>` : ''}
                ${notFound.length ? `<div class="tag-cloud" style="margin-top:4px">${notFound.map(e => `<span class="tag tag--neutral">${escHtml(e)}</span>`).join('')}</div>` : ''}
            </div>`;
    }

    grid.innerHTML = html;
}

/* ════════════════════════════════════════
   TAB: ЧИТАБЕЛЬНІСТЬ
════════════════════════════════════════ */
function renderTabReadability(a) {
    const grid = document.getElementById('readGrid');

    const readScore = a.readabilityScore || 0;
    const readLabel = a.readabilityLevel
        ? readabilityLabelUa(a.readabilityLevel)
        : '—';

    const metrics = [
        { label: 'Flesch Score',              value: readScore.toFixed(1), sub: readLabel },
        { label: 'Кількість слів',            value: a.wordCount || 0,     sub: '' },
        { label: 'Слів у реченні (сер.)',     value: (a.avgWordsPerSentence || 0).toFixed(1), sub: '' },
        { label: 'Складів у слові (сер.)',    value: (a.avgSyllablesPerWord || 0).toFixed(2), sub: '' },
    ];

    // Readability gauge bar
    const pct = Math.min(100, readScore);
    const barColor = pct >= 70 ? '#3fb950' : pct >= 50 ? '#58a6ff' : pct >= 30 ? '#d29922' : '#f85149';

    grid.innerHTML = `
        <div class="read-gauge-wrap">
            <div class="read-gauge-label">Індекс читабельності (Flesch)</div>
            <div class="read-gauge-bar-track">
                <div class="read-gauge-bar-fill" id="readGaugeFill" style="width:0%;background:${barColor}"></div>
            </div>
            <div class="read-gauge-value" style="color:${barColor}">${readScore.toFixed(1)} — ${readLabel}</div>
        </div>
        <div class="read-metrics">
            ${metrics.map(m => `
                <div class="read-metric">
                    <div class="read-metric__label">${m.label}</div>
                    <div class="read-metric__value">${m.value}</div>
                    ${m.sub ? `<div class="read-metric__sub">${m.sub}</div>` : ''}
                </div>`).join('')}
        </div>`;

    // Animate the readability bar
    requestAnimationFrame(() => {
        setTimeout(() => {
            const fill = document.getElementById('readGaugeFill');
            if (fill) fill.style.width = pct + '%';
        }, 100);
    });
}

function readabilityLabelUa(level) {
    const map = {
        'VERY_EASY':   'Дуже легко',
        'EASY':        'Легко',
        'FAIRLY_EASY': 'Досить легко',
        'STANDARD':    'Стандартний',
        'FAIRLY_DIFFICULT': 'Досить складно',
        'DIFFICULT':   'Складно',
        'VERY_DIFFICULT': 'Дуже складно',
    };
    return map[level] || level;
}

/* ════════════════════════════════════════
   ANALYSIS TABS SWITCHING
════════════════════════════════════════ */
function switchAnalysisTab(name, btn) {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('tab-btn--active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('tab-panel--active'));
    if (btn) btn.classList.add('tab-btn--active');
    const panel = document.getElementById(`tab-${name}`);
    if (panel) panel.classList.add('tab-panel--active');
}

/* ════════════════════════════════════════
   STATE HELPERS
════════════════════════════════════════ */
function showApState(stateId) {
    const flexStates = new Set(['apLoading', 'apGuest', 'apRunning']);
    const states = ['apLoading', 'apGuest', 'apNoAnalysis', 'apRunning', 'apResult'];
    states.forEach(id => {
        const el = document.getElementById(id);
        if (!el) return;
        if (id === stateId) {
            el.style.display = flexStates.has(id) ? 'flex' : 'block';
        } else {
            el.style.display = 'none';
        }
    });
    // Hide shared config when switching to non-result states
    if (stateId !== 'apResult' && stateId !== 'apNoAnalysis') {
        const cfg = document.getElementById('sharedConfig');
        if (cfg) cfg.style.display = 'none';
    }
}

function emptyState(msg) {
    return `<div class="tab-empty">${msg}</div>`;
}

/* ════════════════════════════════════════
   UTILS
════════════════════════════════════════ */
function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g,'&amp;')
        .replace(/</g,'&lt;')
        .replace(/>/g,'&gt;')
        .replace(/"/g,'&quot;');
}

let _toastTimer = null;
function showToast(msg, type = 'info') {
    let t = document.getElementById('artToast');
    if (!t) {
        t = document.createElement('div');
        t.id = 'artToast';
        t.style.cssText = 'position:fixed;bottom:24px;right:24px;padding:12px 20px;border-radius:8px;color:#fff;font-size:14px;z-index:9999;transition:opacity .3s;pointer-events:none;';
        document.body.appendChild(t);
    }
    t.textContent = msg;
    t.style.background = type === 'success' ? '#3fb950' : type === 'error' ? '#f85149' : '#58a6ff';
    t.style.opacity = '1';
    clearTimeout(_toastTimer);
    _toastTimer = setTimeout(() => { t.style.opacity = '0'; }, 3000);
}

function hexToRgba(hex, alpha) {
    const r = parseInt(hex.slice(1,3), 16);
    const g = parseInt(hex.slice(3,5), 16);
    const b = parseInt(hex.slice(5,7), 16);
    return `rgba(${r},${g},${b},${alpha})`;
}

/* ── Expose for HTML onclick ── */
window.toggleFullText       = toggleFullText;
window.runAnalysis          = runAnalysis;
window.reanalyze            = reanalyze;
window.switchAnalysisTab    = switchAnalysisTab;
window.toggleSharedConfig   = toggleSharedConfig;
window.toggleLayerSubsArt   = toggleLayerSubsArt;
