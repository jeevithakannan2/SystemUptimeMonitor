// ── API helper ──
const API = {
    async request(url, method, params) {
        const opts = { method, credentials: 'same-origin' };
        if (params && (method === 'POST' || method === 'PUT' || method === 'DELETE')) {
            opts.headers = { 'Content-Type': 'application/x-www-form-urlencoded' };
            opts.body = new URLSearchParams(params).toString();
        } else if (params && method === 'GET') {
            url += '?' + new URLSearchParams(params).toString();
        }
        const resp = await fetch(url, opts);
        if (!resp.ok) {
            if (resp.status === 401 || resp.status === 403) {
                logout();
                throw new Error('Session expired');
            }
            const ct = resp.headers.get('content-type') || '';
            if (ct.includes('application/json')) {
                const json = await resp.json();
                throw new Error(json.error || resp.statusText);
            }
            const text = await resp.text();
            throw new Error(text || resp.statusText);
        }
        const ct = resp.headers.get('content-type') || '';
        if (ct.includes('application/json')) return resp.json();
        const text = await resp.text();
        return text;
    },
    login(email, password) { return this.request('login', 'GET', { email, password }); },
    register(email, password) { return this.request('register', 'POST', { email, password }); },
    createUserFromLink(email, password, code) { return this.request('create', 'POST', { email, password, code }); },
    getMonitors() { return this.request('monitors', 'GET'); },
    createMonitor(data) { return this.request('create_monitor', 'POST', data); },
    updateMonitor(data) { return this.request('update_monitor', 'PUT', data); },
    deleteMonitor(id) { return this.request('delete_monitor', 'DELETE', { id }); },
    getMonitorHistory(id) { return this.request('monitor_history', 'GET', { id }); },
    getIncidents() { return this.request('incidents', 'GET'); },
    createIncident(monitor_id, status_code) { return this.request('create_incident', 'POST', { monitor_id, status_code }); },
    resolveIncident(incident_id, notes) { return this.request('resolve_incident', 'PUT', { incident_id, notes }); },
    generateInviteLink(role) { return this.request('generate_invitelink', 'GET', { role }); },
    deleteUser(delete_email) { return this.request('delete_user', 'DELETE', { delete_email }); },
    getStatus() { return this.request('status', 'GET'); },
};

// ── Toast notifications ──
function showToast(message, type = 'success') {
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = message;
    container.appendChild(toast);
    setTimeout(() => { toast.remove(); }, 3500);
}

// ── Modal helpers ──
function openModal(id) { document.getElementById(id).classList.add('active'); }
function closeModal(id) { document.getElementById(id).classList.remove('active'); }

// ── Session ──
function getSession() {
    try { return JSON.parse(localStorage.getItem('session')); } catch { return null; }
}
function setSession(data) { localStorage.setItem('session', JSON.stringify(data)); }
function clearSession() { localStorage.removeItem('session'); }
function requireAuth() {
    if (!getSession()) { window.location.href = 'login.html'; return false; }
    return true;
}
function logout() {
    clearSession();
    document.cookie = 'token=; Max-Age=0; path=/';
    window.location.href = 'login.html';
}

// ── Time formatting ──
function formatTime(ts) {
    if (!ts || ts === 0) return '—';
    return new Date(typeof ts === 'string' ? ts : ts).toLocaleString();
}

function timeAgo(ts) {
    const diff = Date.now() - new Date(typeof ts === 'string' ? ts : ts).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'just now';
    if (mins < 60) return mins + 'm ago';
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return hrs + 'h ago';
    return Math.floor(hrs / 24) + 'd ago';
}
