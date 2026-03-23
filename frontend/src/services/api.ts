import axios from 'axios';
import type { Monitor, Incident, MonitorAudit, StatusMonitor, User } from '@/types';

const api = axios.create({
  baseURL: '/api',
  withCredentials: true,
  headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 || err.response?.status === 403) {
      localStorage.removeItem('session');
      document.cookie = 'token=; Max-Age=0; path=/';
      window.location.href = '/login';
    }
    const message = err.response?.data?.error || err.message || 'Request failed';
    return Promise.reject(new Error(message));
  },
);

function params(obj: Record<string, string | number | boolean | undefined>) {
  return new URLSearchParams(
    Object.entries(obj)
      .filter(([, v]) => v !== undefined && v !== '')
      .map(([k, v]) => [k, String(v)]),
  ).toString();
}

// Auth
export async function login(email: string, password: string): Promise<User> {
  const { data } = await api.get('/login', { params: { email, password } });
  return data;
}

export async function register(email: string, password: string): Promise<void> {
  await api.post('/register', params({ email, password }));
}

export async function createUserFromLink(email: string, password: string, code: string): Promise<void> {
  await api.post('/create', params({ email, password, code }));
}

// Monitors
export async function getMonitors(): Promise<{ monitors: Monitor[] }> {
  const { data } = await api.get('/monitors');
  return data;
}

export async function createMonitor(monitor: Record<string, string>): Promise<void> {
  await api.post('/create_monitor', params(monitor));
}

export async function updateMonitor(monitor: Record<string, string>): Promise<void> {
  await api.put('/update_monitor', params(monitor));
}

export async function deleteMonitor(id: number): Promise<void> {
  await api.delete('/delete_monitor', { data: params({ id }) });
}

export async function getMonitorHistory(id: number): Promise<{ history: MonitorAudit[] }> {
  const { data } = await api.get('/monitor_history', { params: { id } });
  return data;
}

// Incidents
export async function getIncidents(): Promise<{ incidents: Incident[] }> {
  const { data } = await api.get('/incidents');
  return data;
}

export async function createIncident(monitor_id: number, status_code: number): Promise<void> {
  await api.post('/create_incident', params({ monitor_id, status_code }));
}

export async function resolveIncident(incident_id: number, notes: string): Promise<void> {
  await api.put('/resolve_incident', params({ incident_id, notes }));
}

// Admin
export async function generateInviteLink(role: string): Promise<string> {
  const { data } = await api.get('/generate_invitelink', { params: { role } });
  return typeof data === 'string' ? data : String(data);
}

export async function deleteUser(delete_email: string): Promise<void> {
  await api.delete('/delete_user', { data: params({ delete_email }) });
}

export async function getUsers(): Promise<{ users: User[] }> {
  const { data } = await api.get('/users');
  return data;
}

export async function updateUserRole(user_id: number, role: string): Promise<void> {
  await api.put('/update_role', params({ user_id, role }));
}

// Public status
export async function getStatus(org: string): Promise<{ organization: string; monitors: StatusMonitor[] }> {
  const { data } = await api.get('/status', { params: { org } });
  return data;
}

export async function getOrganizations(): Promise<{ organizations: string[] }> {
  const { data } = await api.get('/organizations');
  return data;
}

// Notifications
export async function getSubscriptions(): Promise<{ email_notifications: boolean; subscribed_monitors: number[] }> {
  const { data } = await api.get('/subscriptions');
  return data;
}

export async function updateNotificationPreference(enabled: boolean): Promise<void> {
  await api.put('/notification_preference', params({ enabled }));
}

export async function subscribeMonitor(monitor_id: number): Promise<void> {
  await api.post('/subscribe_monitor', params({ monitor_id }));
}

export async function unsubscribeMonitor(monitor_id: number): Promise<void> {
  await api.delete('/unsubscribe_monitor', { data: params({ monitor_id }) });
}

export default api;
