export interface User {
  email: string;
  role: 'admin' | 'operator' | 'viewer';
  organization: string;
}

export interface Monitor {
  id: number;
  name: string;
  target_url: string;
  check_interval: number;
  created_time: string;
  failure_count: number;
  organization: string;
  status_codes: number[];
  enabled: boolean;
}

export interface Incident {
  id: number;
  monitor_id: number;
  monitor_run_id: number;
  down_time: string;
  resolved_time: string | number;
  status_code: number;
  expected_status_codes: string;
  resolved: boolean;
  notes?: string;
}

export interface MonitorAudit {
  id: number;
  monitor_id: number;
  operation: 'CREATE' | 'UPDATE' | 'DELETE';
  time: string;
}

export interface StatusMonitor extends Monitor {
  incidents: StatusIncident[];
  uptime: number;
}

export interface StatusIncident {
  id: number;
  monitor_run_id: number;
  down_time: string;
  resolved_time: string;
  status_code: number;
}

export type MonitorStatus = 'up' | 'down' | 'disabled';
export type UptimeLevel = 'good' | 'warn' | 'bad';
