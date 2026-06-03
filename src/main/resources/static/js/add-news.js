/* ═══════════════════════════════════════
   ADD NEWS PAGE
═══════════════════════════════════════ */
let _parsedArticles = [];
let _uploadedFile   = null;

document.addEventListener('DOMContentLoaded', () => {
    if (!Auth.isLoggedIn()) {
        window.location.href = '/login';
        return;
    }
    renderNav();
    document.getElementById('fContent').addEventListener('input', updateWordCount);
});

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

/* ── Tabs ── */
function switchTab(name) {
    document.getElementById('panelForm').style.display = name === 'form' ? 'block' : 'none';
    document.getElementById('panelFile').style.display = name === 'file' ? 'block' : 'none';
    document.getElementById('tabForm').classList.toggle('an-tab--active', name === 'form');
    document.getElementById('tabFile').classList.toggle('an-tab--active', name === 'file');
}

/* ── Word count ── */
function updateWordCount() {
    const text  = document.getElementById('fContent').value.trim();
    const words = text ? text.split(/\s+/).length : 0;
    document.getElementById('wordCountHint').textContent = words + ' слів';
}

/* ── Form submit ── */
function submitForm() {
    const title   = document.getElementById('fTitle').value.trim();
    const content = document.getElementById('fContent').value.trim();

    if (!title)   { showToast('Введіть заголовок', 'error'); return; }
    if (!content) { showToast('Введіть текст статті', 'error'); return; }

    const btn = document.getElementById('submitBtn');
    btn.disabled = true;
    btn.textContent = 'Збереження…';

    const body = {
        title,
        description:  document.getElementById('fDesc').value.trim()     || null,
        fullContent:  content,
        url:          document.getElementById('fUrl').value.trim()       || null,
        imageUrl:     document.getElementById('fImageUrl').value.trim()  || null,
        source:       document.getElementById('fSource').value.trim()    || null,
        category:     document.getElementById('fCategory').value.trim()  || null,
    };

    fetch('/api/news/manual', {
        method:  'POST',
        headers: { ...Auth.getHeaders(), 'Content-Type': 'application/json' },
        body:    JSON.stringify(body),
    })
    .then(safeJson)
    .then(data => {
        btn.disabled = false;
        btn.textContent = 'Зберегти статтю';
        showFormResult(data.id, data.title);
        showToast('Статтю додано!');
    })
    .catch(err => {
        btn.disabled = false;
        btn.textContent = 'Зберегти статтю';
        showToast('Помилка: ' + err.message, 'error');
    });
}

function showFormResult(id, title) {
    const el = document.getElementById('formResult');
    el.style.display = 'block';
    el.innerHTML = `
        <div class="an-result">
            <div class="an-result__icon">✓</div>
            <div>
                <div style="font-weight:600;margin-bottom:4px">${escHtml(title)}</div>
                <div style="font-size:12px;color:var(--text-muted);margin-bottom:10px">ID: ${id}</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <a href="/main" class="btn btn--ghost" style="font-size:12px">Перейти до стрічки</a>
                    <button class="btn btn--primary" style="font-size:12px" onclick="analyzeNow('${id}')">Аналізувати зараз</button>
                </div>
            </div>
        </div>`;
}

function clearForm() {
    ['fTitle','fSource','fCategory','fUrl','fImageUrl','fDesc','fContent']
        .forEach(id => { document.getElementById(id).value = ''; });
    document.getElementById('wordCountHint').textContent = '0 слів';
    document.getElementById('formResult').style.display = 'none';
}

/* ── File handling ── */
function handleDrop(e) {
    e.preventDefault();
    document.getElementById('dropZone').classList.remove('an-upload-zone--over');
    const file = e.dataTransfer.files[0];
    if (file) processFile(file);
}

function handleFileSelect(input) {
    if (input.files[0]) processFile(input.files[0]);
}

function processFile(file) {
    if (!file.name.endsWith('.json')) {
        showToast('Підтримується тільки .json', 'error');
        return;
    }
    _uploadedFile = file;
    const reader = new FileReader();
    reader.onload = e => {
        try {
            _parsedArticles = JSON.parse(e.target.result);
            if (!Array.isArray(_parsedArticles)) _parsedArticles = [_parsedArticles];
            renderFilePreview(file.name, _parsedArticles);
        } catch (err) {
            showToast('Не вдалося розпарсити JSON: ' + err.message, 'error');
        }
    };
    reader.readAsText(file);
}

function renderFilePreview(filename, articles) {
    document.getElementById('fileInfoText').textContent =
        `Файл: ${filename} · ${articles.length} статей`;
    document.getElementById('fileInfo').style.display = 'block';

    document.getElementById('filePreview').innerHTML = articles.slice(0, 5).map((a, i) => `
        <div class="an-preview-item">
            <span class="an-preview-num">${i + 1}</span>
            <div>
                <div style="font-size:13px;font-weight:500;color:var(--text)">${escHtml(a.title || '—')}</div>
                <div style="font-size:11px;color:var(--text-muted)">
                    ${a.source ? escHtml(a.source) + ' · ' : ''}
                    ${a.fullContent ? Math.ceil(a.fullContent.split(/\s+/).length) + ' слів' : 'без тексту'}
                </div>
            </div>
        </div>`).join('') +
        (articles.length > 5
            ? `<div style="font-size:12px;color:var(--text-muted);padding:8px 0">… і ще ${articles.length - 5} статей</div>`
            : '');
}

function clearFile() {
    _parsedArticles = [];
    _uploadedFile   = null;
    document.getElementById('fileInput').value = '';
    document.getElementById('fileInfo').style.display   = 'none';
    document.getElementById('uploadResult').style.display = 'none';
}

function uploadFile() {
    if (!_uploadedFile) return;
    const btn = document.getElementById('uploadBtn');
    btn.disabled = true;
    btn.textContent = 'Імпорт…';

    const form = new FormData();
    form.append('file', _uploadedFile);

    // Для multipart НЕ передаємо Content-Type — браузер сам встановлює multipart/form-data з boundary
    const uploadHeaders = {};
    const token = Auth.getToken();
    if (token) uploadHeaders['Authorization'] = 'Bearer ' + token;

    fetch('/api/news/manual/upload', { method: 'POST', headers: uploadHeaders, body: form })
        .then(safeJson)
        .then(data => {
            btn.disabled = false;
            btn.textContent = 'Імпортувати';
            showUploadResult(data);
            showToast('Імпорт завершено!');
        })
        .catch(err => {
            btn.disabled = false;
            btn.textContent = 'Імпортувати';
            showToast('Помилка: ' + err.message, 'error');
        });
}

function showUploadResult(data) {
    const el = document.getElementById('uploadResult');
    el.style.display = 'block';
    el.innerHTML = `
        <div class="an-result">
            <div class="an-result__icon">✓</div>
            <div>
                <div style="font-weight:600;margin-bottom:6px">${data.message}</div>
                <div style="display:flex;gap:8px;flex-wrap:wrap">
                    <a href="/main" class="btn btn--ghost" style="font-size:12px">Перейти до стрічки</a>
                    ${data.ids?.length === 1
                        ? `<button class="btn btn--primary" style="font-size:12px" onclick="analyzeNow('${data.ids[0]}')">Аналізувати зараз</button>`
                        : data.ids?.length > 1
                        ? `<button class="btn btn--primary" style="font-size:12px" onclick="analyzeBatch(${JSON.stringify(data.ids)})">Аналізувати всі (${data.ids.length})</button>`
                        : ''}
                </div>
            </div>
        </div>`;
}

/* ── Analyze ── */
function analyzeNow(id) {
    window.location.href = `/main?analyzeId=${id}`;
}

function analyzeBatch(ids) {
    const btn = event.currentTarget;
    btn.disabled = true;
    btn.textContent = 'Аналізується…';

    const chain = ids.reduce((p, id) =>
        p.then(() => fetch(`/analysis/${id}`, { method: 'POST', headers: { 'Content-Type': 'application/json' } })),
        Promise.resolve());

    chain
        .then(() => { btn.textContent = `✓ Проаналізовано ${ids.length}`; })
        .catch(() => { btn.textContent = 'Помилка'; btn.disabled = false; });
}

/* ── Toast ── */
function showToast(msg, type = 'info') {
    const t = document.getElementById('toast');
    t.className = `toast${type === 'error' ? ' toast--error' : ''}`;
    t.textContent = msg;
    t.classList.add('toast--visible');
    setTimeout(() => t.classList.remove('toast--visible'), 3000);
}

/** Парсить JSON або кидає Error з текстом помилки від сервера */
function safeJson(r) {
    if (r.ok) return r.json();
    return r.text().then(text => {
        let msg = 'HTTP ' + r.status;
        try { msg = JSON.parse(text).error || msg; } catch {}
        throw new Error(msg);
    });
}

function escHtml(str) {
    if (!str) return '';
    return String(str).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}
