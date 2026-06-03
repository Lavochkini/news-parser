/* ═══════════════════════════════════════
   LOGIN PAGE
═══════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    const form     = document.getElementById('loginForm');
    const loginIn  = document.getElementById('fLogin');
    const passIn   = document.getElementById('fPassword');

    // Валідація при виході з поля
    loginIn.addEventListener('blur', () => validateLogin());
    passIn.addEventListener('blur',  () => validatePassword());

    // Прибираємо помилку при введенні
    loginIn.addEventListener('input', () => clearFieldState('fLogin', 'loginStatus', 'loginHint'));
    passIn.addEventListener('input',  () => clearFieldState('fPassword', null, 'passwordHint'));

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const okLogin = validateLogin();
        const okPass  = validatePassword();
        if (!okLogin || !okPass) return;

        const btn = document.getElementById('submitBtn');
        setLoading(btn, true);
        setError('');

        try {
            const res = await fetch('/auth/login', {
                method:  'POST',
                headers: { 'Content-Type': 'application/json' },
                body:    JSON.stringify({
                    login:    loginIn.value.trim(),
                    password: passIn.value,
                }),
            });
            const data = await res.json();

            if (res.ok && data.token) {
                Auth.save(data);
                window.location.href = '/main';
            } else {
                setError(data.error || data.message || 'Невірний логін або пароль');
                setFieldState('fLogin',    'loginStatus',    'loginHint',    'error', '');
                setFieldState('fPassword', null,             'passwordHint', 'error', '');
            }
        } catch {
            setError('Помилка сервера. Спробуйте пізніше.');
        } finally {
            setLoading(btn, false);
        }
    });
});

/* ── Validators ── */
function validateLogin() {
    const val = document.getElementById('fLogin').value.trim();
    if (!val) {
        setFieldState('fLogin', 'loginStatus', 'loginHint', 'error', 'Введіть email або username');
        return false;
    }
    clearFieldState('fLogin', 'loginStatus', 'loginHint');
    return true;
}

function validatePassword() {
    const val = document.getElementById('fPassword').value;
    if (!val) {
        setFieldState('fPassword', null, 'passwordHint', 'error', 'Введіть пароль');
        return false;
    }
    clearFieldState('fPassword', null, 'passwordHint');
    return true;
}

/* ── Helpers ── */
function setFieldState(inputId, statusId, hintId, state, msg) {
    const input  = document.getElementById(inputId);
    const hint   = document.getElementById(hintId);
    const status = statusId ? document.getElementById(statusId) : null;

    input.classList.remove('field__input--error', 'field__input--success', 'field__input--loading');
    if (state === 'error') {
        input.classList.add('field__input--error');
        if (status) status.textContent = '✗';
        if (hint)   { hint.textContent = msg; hint.className = 'field__hint field__hint--error'; }
    } else if (state === 'success') {
        input.classList.add('field__input--success');
        if (status) status.textContent = '✓';
        if (hint)   { hint.textContent = msg; hint.className = 'field__hint field__hint--success'; }
    } else if (state === 'loading') {
        input.classList.add('field__input--loading');
        if (status) status.innerHTML = '<span class="spinner"></span>';
        if (hint)   { hint.textContent = msg; hint.className = 'field__hint field__hint--info'; }
    }
}

function clearFieldState(inputId, statusId, hintId) {
    const input  = document.getElementById(inputId);
    const hint   = hintId   ? document.getElementById(hintId)   : null;
    const status = statusId ? document.getElementById(statusId) : null;
    input.classList.remove('field__input--error', 'field__input--success', 'field__input--loading');
    if (status) status.textContent = '';
    if (hint)   { hint.textContent = ''; hint.className = 'field__hint'; }
}

function setError(msg) {
    const el = document.getElementById('globalError');
    if (msg) { el.textContent = msg; el.classList.add('visible'); }
    else      { el.textContent = ''; el.classList.remove('visible'); }
}

function setLoading(btn, on) {
    btn.disabled    = on;
    btn.textContent = on ? 'Вхід…' : 'Увійти';
}

function togglePwd(inputId, btnId) {
    const input = document.getElementById(inputId);
    const btn   = document.getElementById(btnId);
    const show  = input.type === 'password';
    input.type      = show ? 'text' : 'password';
    btn.textContent = show ? 'Приховати' : 'Показати';
}
