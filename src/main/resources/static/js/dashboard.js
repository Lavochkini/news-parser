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
        { key: 'linguistic',  label: 'Linguistic',    max: 35, cls: 'linguistic'  },
        { key: 'crossSource', label: 'Cross-Source',  max: 35, cls: 'crosssource' },
        { key: 'factCheck',   label: 'Fact Check',    max: 30, cls: 'factcheck'   },
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
    const colors  = [COLORS.green, COLORS.accent, COLORS.red];
    const labels  = order.filter(k => data[k] != null);
    const values  = labels.map(k => data[k]);
    const bgColors = labels.map(k => colors[order.indexOf(k)]);

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
    const labels = order.filter(k => data[k] != null);
    const values = labels.map(k => data[k]);

    new Chart(document.getElementById('readabilityChart'), {
        type: 'bar',
        data: {
            labels: labels.map(l => l.replace('_', ' ')),
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
    const colors = [COLORS.green, COLORS.teal, COLORS.gray, COLORS.red];
    const labels = order.filter(k => data[k] != null);
    const values = labels.map(k => data[k]);
    const bgColors = labels.map(k => colors[order.indexOf(k)]);

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
   UTILS
═══════════════════════════════════════ */
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
