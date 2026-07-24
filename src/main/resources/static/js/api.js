/** Shared API helper: guest token when server security is enabled. */
(function (global) {
  const TOKEN_KEY = 'qvSessionToken';

  async function ensureGuestToken() {
    let token = sessionStorage.getItem(TOKEN_KEY);
    if (token) return token;
    const r = await fetch('/api/auth/guest', { method: 'POST' });
    if (!r.ok) {
      const body = await r.json().catch(() => ({}));
      throw new Error(body.message || ('guest login failed ' + r.status));
    }
    const body = await r.json();
    token = body.sessionToken;
    sessionStorage.setItem(TOKEN_KEY, token);
    return token;
  }

  async function apiFetch(url, options) {
    const opts = Object.assign({ headers: {} }, options || {});
    opts.headers = Object.assign({}, opts.headers);
    const token = await ensureGuestToken();
    opts.headers['Authorization'] = 'Bearer ' + token;
    let r = await fetch(url, opts);
    if (r.status === 401) {
      sessionStorage.removeItem(TOKEN_KEY);
      const retryToken = await ensureGuestToken();
      opts.headers['Authorization'] = 'Bearer ' + retryToken;
      r = await fetch(url, opts);
    }
    return r;
  }

  global.QuizApi = { ensureGuestToken, apiFetch };
})(window);
