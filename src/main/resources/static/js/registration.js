/* ═══════════════════════════════════════
   REGISTRATION WIZARD
   Step 1 → Email
   Step 2 → Username (live uniqueness check)
   Step 3 → Password + Confirm
═══════════════════════════════════════ */

let _currentStep    = 1;
let _usernameTimer  = null;
let _usernameOk     = false;   // стає true після успішної перевірки

/* ══════════════════════════════════════
   INIT
══════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {

    /* Enter → натискає активну кнопку "Далі" */
    document.addEventListener('keydown', e => {
        if (e.key !== 'Enter') return;
        if (_currentStep === 1) goStep2();
        else if (_currentStep === 2) goStep3();
        else if (_currentStep === 3) submitForm();
    });

    /* Email — live */
    document.getElementById('fEmail').addEventListener('input', () => {
        clearField('email');
        setError('');
    });

    /* Username — debounced check */
    document.getElementById('fUsername').addEventListener('input', () => {
        _usernameOk = false;
        clearTimeout(_usernameTimer);
        const val = document.getElementById('fUsername').value.trim();

        if (!val) { clearField('username'); return; }

        if (val.length < 3) {
            setField('username', 'error', 'Мінімум 3 символи', '✗'); return;
        }
        if (!/^[a-zA-Z0-9_.-]+$/.test(val)) {
            setField('username', 'error', 'Лише літери, цифри, _ . -', '✗'); return;
        }

        setField('username', 'loading', 'Перевіряємо…', 'spinner');
        _usernameTimer = setTimeout(() => checkUsername(val), 600);
    });

    /* Password — strength */
    document.getElementById('fPassword').addEventListener('input', () => {
        const val = document.getElementById('fPassword').value;
        if (!val) {
            clearField('password');
            document.getElementById('strengthBar').style.display = 'none';
            syncConfirm(); return;
        }
        document.getElementById('strengthBar').style.display = 'flex';
        const lvl = strength(val);
        renderStrength(lvl);
        if (val.length < 6)   setField('password', 'error',   'Мінімум 6 символів', '✗');
        else if (lvl === 1)   setField('password', 'warning', 'Слабкий пароль', '⚠');
        else if (lvl === 2)   setField('password', 'success', 'Середній пароль', '✓');
        else                  setField('password', 'success', 'Надійний пароль ✦', '✓');
        syncConfirm();
    });

    /* Confirm — live sync */
    document.getElementById('fConfirm').addEventListener('input', syncConfirm);
});

/* ══════════════════════════════════════
   STEP NAVIGATION
══════════════════════════════════════ */
async function goStep2() {
    if (!validateEmail()) return;
    showStep(2, 'forward');

    /* Підказка у крок 2 — "email" */
    const email = document.getElementById('fEmail').value.trim();
    document.getElementById('summaryEmail').innerHTML =
        `✉ <strong>${escHtml(email)}</strong>`;

    setTimeout(() => document.getElementById('fUsername').focus(), 320);
}

async function goStep3() {
    if (!validateUsername()) return;

    /* Якщо ще не завершилась перевірка — чекаємо */
    const val = document.getElementById('fUsername').value.trim();
    if (document.getElementById('usernameHint').classList.contains('field__hint--info')) {
        /* Іде запит — чекаємо трохи */
        setField('username', 'loading', 'Перевіряємо…', 'spinner');
        await new Promise(r => setTimeout(r, 700));
    }
    if (!_usernameOk) {
        setField('username', 'error', 'Перевірте username перед тим як продовжити', '✗');
        return;
    }

    showStep(3, 'forward');

    const uname = document.getElementById('fUsername').value.trim();
    document.getElementById('summaryUser').innerHTML =
        `👤 <strong>${escHtml(uname)}</strong>`;

    setTimeout(() => document.getElementById('fPassword').focus(), 320);
}

function goStep1() { showStep(1, 'back'); }
function goStep2back() { showStep(2, 'back'); }

function showStep(n, direction = 'forward') {
    const current = document.getElementById('step' + _currentStep);
    current.classList.add('step-panel--hidden');
    current.classList.remove('step-panel--back');

    _currentStep = n;
    updateProgress(n);

    const next = document.getElementById('step' + n);
    next.classList.remove('step-panel--hidden', 'step-panel--back');
    if (direction === 'back') next.classList.add('step-panel--back');
    /* перезапустити анімацію */
    void next.offsetWidth;
    next.style.animation = 'none';
    requestAnimationFrame(() => { next.style.animation = ''; });
}

function updateProgress(step) {
    const pct = step === 1 ? '33.33%' : step === 2 ? '66.66%' : '100%';
    document.getElementById('stepsFill').style.width = pct;

    for (let i = 1; i <= 3; i++) {
        const lbl = document.getElementById('stepLabel' + i);
        lbl.classList.remove('active', 'done');
        if (i < step)  lbl.classList.add('done');
        if (i === step) lbl.classList.add('active');
    }
}

/* ══════════════════════════════════════
   SUBMIT
══════════════════════════════════════ */
function onTermsChange() {
    const checked = document.getElementById('termsCheck').checked;
    document.getElementById('btn3').disabled = !checked;
    document.getElementById('termsHint').style.display = 'none';
    document.getElementById('termsLabel').classList.toggle('terms-consent--checked', checked);
}

async function submitForm() {
    if (!validatePassword() || !validateConfirm()) return;

    // Перевірка прийняття умов
    const termsCheck = document.getElementById('termsCheck');
    if (!termsCheck.checked) {
        document.getElementById('termsHint').style.display = 'block';
        termsCheck.closest('.terms-consent').scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }

    const btn = document.getElementById('btn3');
    setLoading(btn, true, 'Реєстрація…', 'Зареєструватись');
    setError('');

    try {
        const res = await fetch('/auth/register', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                email:    document.getElementById('fEmail').value.trim(),
                username: document.getElementById('fUsername').value.trim(),
                password: document.getElementById('fPassword').value,
            }),
        });
        const data = await res.json();

        if (res.ok && data.token) {
            Auth.save(data);
            window.location.href = '/main';
        } else if (data.errors) {
            /* Серверні помилки — повертаємось до відповідного кроку */
            data.errors.forEach(err => {
                if (err.field === 'email') {
                    setField('email', 'error', err.message, '✗');
                    showStep(1, 'back');
                }
                if (err.field === 'username') {
                    setField('username', 'error', err.message, '✗');
                    _usernameOk = false;
                    showStep(2, 'back');
                }
            });
        } else {
            setError(data.error || data.message || 'Помилка реєстрації');
        }
    } catch {
        setError('Помилка сервера. Спробуйте пізніше.');
    } finally {
        setLoading(btn, false, 'Реєстрація…', 'Зареєструватись');
    }
}

/* ══════════════════════════════════════
   VALIDATORS
══════════════════════════════════════ */
function validateEmail() {
    const val = document.getElementById('fEmail').value.trim();
    if (!val) {
        setField('email', 'error', "Email обов'язковий", '✗'); return false;
    }
    if (!/^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(val)) {
        setField('email', 'error', 'Невірний формат email', '✗'); return false;
    }
    setField('email', 'success', 'Email виглядає коректно', '✓');
    return true;
}

function validateUsername() {
    const val = document.getElementById('fUsername').value.trim();
    if (!val)          { setField('username', 'error', "Username обов'язковий", '✗'); return false; }
    if (val.length < 3){ setField('username', 'error', 'Мінімум 3 символи', '✗');    return false; }
    if (!/^[a-zA-Z0-9_.-]+$/.test(val)) {
        setField('username', 'error', 'Лише літери, цифри, _ . -', '✗'); return false;
    }
    return true;
}

function validatePassword() {
    const val = document.getElementById('fPassword').value;
    if (!val || val.length < 6) {
        setField('password', 'error', 'Мінімум 6 символів', '✗'); return false;
    }
    return true;
}

function validateConfirm() {
    const p = document.getElementById('fPassword').value;
    const c = document.getElementById('fConfirm').value;
    if (!c) { setField('confirm', 'error', 'Підтвердіть пароль', '✗'); return false; }
    if (p !== c) { setField('confirm', 'error', 'Паролі не збігаються', '✗'); return false; }
    setField('confirm', 'success', 'Паролі збігаються', '✓');
    return true;
}

function syncConfirm() {
    const c = document.getElementById('fConfirm').value;
    if (!c) return;
    validateConfirm();
}

/* ══════════════════════════════════════
   USERNAME UNIQUENESS CHECK
══════════════════════════════════════ */
async function checkUsername(val) {
    try {
        const res  = await fetch('/auth/check-username?username=' + encodeURIComponent(val));
        const data = await res.json();
        if (data.taken) {
            setField('username', 'error', 'Цей username вже зайнятий', '✗');
            _usernameOk = false;
        } else {
            setField('username', 'success', 'Username доступний', '✓');
            _usernameOk = true;
        }
    } catch {
        clearField('username');
    }
}

/* ══════════════════════════════════════
   PASSWORD STRENGTH
══════════════════════════════════════ */
function strength(pwd) {
    let s = 0;
    if (pwd.length >= 8)           s++;
    if (pwd.length >= 12)          s++;
    if (/[A-Z]/.test(pwd))         s++;
    if (/[0-9]/.test(pwd))         s++;
    if (/[^A-Za-z0-9]/.test(pwd))  s++;
    return s <= 1 ? 1 : s <= 3 ? 2 : 3;
}

function renderStrength(lvl) {
    const cls = ['', 'weak', 'medium', 'strong'][lvl];
    for (let i = 1; i <= 4; i++) {
        const el = document.getElementById('seg' + i);
        el.className = 'strength-bar__seg';
        const fill = (lvl === 1 && i <= 1) || (lvl === 2 && i <= 2) || (lvl === 3 && i <= 4);
        if (fill) el.classList.add('strength-bar__seg--' + cls);
    }
}

/* ══════════════════════════════════════
   FIELD STATE HELPERS
══════════════════════════════════════ */
const FIELD_IDS = {
    email:    { input:'fEmail',    status:'emailStatus',    hint:'emailHint'    },
    username: { input:'fUsername', status:'usernameStatus', hint:'usernameHint' },
    password: { input:'fPassword', status: null,            hint:'passwordHint' },
    confirm:  { input:'fConfirm',  status: null,            hint:'confirmHint'  },
};

function setField(name, state, msg, statusContent) {
    const ids = FIELD_IDS[name];
    const input  = document.getElementById(ids.input);
    const hint   = ids.hint   ? document.getElementById(ids.hint)   : null;
    const status = ids.status ? document.getElementById(ids.status) : null;

    input.classList.remove('field__input--error','field__input--success','field__input--loading');

    if (state === 'error')   input.classList.add('field__input--error');
    if (state === 'success') input.classList.add('field__input--success');
    if (state === 'loading' || state === 'warning') input.classList.add('field__input--loading');

    const hintCls = { error:'error', success:'success', loading:'info', warning:'warning' }[state] || 'info';
    if (hint) { hint.innerHTML = msg; hint.className = 'field__hint field__hint--' + hintCls; }
    if (status) {
        if (statusContent === 'spinner') status.innerHTML = '<span class="spinner"></span>';
        else status.textContent = statusContent || '';
    }
}

function clearField(name) {
    const ids = FIELD_IDS[name];
    const input  = document.getElementById(ids.input);
    const hint   = ids.hint   ? document.getElementById(ids.hint)   : null;
    const status = ids.status ? document.getElementById(ids.status) : null;
    input.classList.remove('field__input--error','field__input--success','field__input--loading');
    if (hint)   { hint.textContent = ''; hint.className = 'field__hint'; }
    if (status) status.innerHTML = '';
}

function setError(msg) {
    const el = document.getElementById('globalError');
    if (msg) { el.textContent = msg; el.classList.add('visible'); }
    else      { el.textContent = ''; el.classList.remove('visible'); }
}

function setLoading(btn, on, loadingText, normalText) {
    btn.disabled    = on;
    btn.textContent = on ? loadingText : normalText;
}

/* ══════════════════════════════════════
   UTILS
══════════════════════════════════════ */
function togglePwd(inputId, btnId) {
    const input = document.getElementById(inputId);
    const btn   = document.getElementById(btnId);
    const show  = input.type === 'password';
    input.type      = show ? 'text' : 'password';
    btn.textContent = show ? 'Приховати' : 'Показати';
}

function escHtml(str) {
    return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
}
