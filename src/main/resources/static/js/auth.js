const Auth = {
    save(data) {
        localStorage.setItem('token',    data.token);
        localStorage.setItem('userId',   data.userId);
        localStorage.setItem('username', data.username);
        localStorage.setItem('role',     data.role);
    },
    clear() {
        ['token', 'userId', 'username', 'role'].forEach(k => localStorage.removeItem(k));
    },
    getToken()    { return localStorage.getItem('token'); },
    getUsername() { return localStorage.getItem('username'); },
    getRole()     { return localStorage.getItem('role'); },
    isLoggedIn()  { return !!localStorage.getItem('token'); },
    getHeaders(extra = {}) {
        const h = { 'Content-Type': 'application/json', ...extra };
        const t = this.getToken();
        if (t) h['Authorization'] = 'Bearer ' + t;
        return h;
    },
    logout() {
        this.clear();
        window.location.href = '/main';
    },
};
