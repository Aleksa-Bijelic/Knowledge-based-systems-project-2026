const API_BASE = '/api';

export async function request(path, options = {}) {
  const response = await fetch(`${API_BASE}${path}`, options);
  const data = await response.text();
  if (!response.ok) {
    throw new Error(data || response.statusText);
  }
  try {
    return JSON.parse(data);
  } catch {
    return data;
  }
}

export function authHeader(token) {
  return token ? { Authorization: `Bearer ${token}` } : {};
}
