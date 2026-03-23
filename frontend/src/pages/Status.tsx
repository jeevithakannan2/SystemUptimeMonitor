import { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Activity, ArrowLeft, Clock, Globe, Loader2, RefreshCw } from 'lucide-react';
import { toast } from 'sonner';
import { useAuth } from '@/hooks/useAuth';
import { getStatus } from '@/services/api';
import type { StatusMonitor, StatusIncident, MonitorStatus, UptimeLevel } from '@/types';
import { GlassCard } from '@/components/GlassCard';
import { StatusDot } from '@/components/StatusDot';
import { PageHeader } from '@/components/PageHeader';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';

const REFRESH_INTERVAL = 30_000;

function deriveStatus(monitor: StatusMonitor): MonitorStatus {
  if (!monitor.enabled) return 'disabled';
  const hasUnresolved = monitor.incidents.some((i) => !i.resolved_time);
  return hasUnresolved ? 'down' : 'up';
}

function deriveUptimeLevel(uptime: number): UptimeLevel {
  if (uptime >= 99.9) return 'good';
  if (uptime >= 95) return 'warn';
  return 'bad';
}

const uptimeColor: Record<UptimeLevel, string> = {
  good: 'text-success',
  warn: 'text-warning',
  bad: 'text-destructive',
};

const statusLabel: Record<MonitorStatus, string> = {
  up: 'Operational',
  down: 'Incident detected',
  disabled: 'Disabled',
};

function formatTime(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });
}

export function Status() {
  const { user, isAuthenticated, isOperator, logout } = useAuth();
  const navigate = useNavigate();

  const [org, setOrg] = useState(user?.organization ?? '');
  const [orgInput, setOrgInput] = useState('');
  const [monitors, setMonitors] = useState<StatusMonitor[]>([]);
  const [loading, setLoading] = useState(false);
  const [lastUpdated, setLastUpdated] = useState<Date | null>(null);
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null);

  const fetchStatus = useCallback(
    async (organization: string, silent = false) => {
      if (!organization) return;
      if (!silent) setLoading(true);
      try {
        const data = await getStatus(organization);
        setMonitors(data.monitors);
        setLastUpdated(new Date());
      } catch (err) {
        toast.error(err instanceof Error ? err.message : 'Failed to load status');
      } finally {
        setLoading(false);
      }
    },
    [],
  );

  // Auto-fetch for authenticated users
  useEffect(() => {
    if (user?.organization) {
      setOrg(user.organization);
      fetchStatus(user.organization);
    }
  }, [user?.organization, fetchStatus]);

  // Auto-refresh every 30s
  useEffect(() => {
    if (!org) return;
    intervalRef.current = setInterval(() => fetchStatus(org, true), REFRESH_INTERVAL);
    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current);
    };
  }, [org, fetchStatus]);

  function handleViewStatus() {
    const trimmed = orgInput.trim();
    if (!trimmed) {
      toast.error('Please enter an organization name');
      return;
    }
    setOrg(trimmed);
    fetchStatus(trimmed);
  }

  return (
    <div className="min-h-screen bg-background">
      {/* ── Top nav bar ── */}
      <nav className="glass-subtle sticky top-0 z-40">
        <div className="mx-auto flex max-w-screen-xl items-center justify-between px-8 py-3">
          <Link to="/status" className="flex items-center gap-2 font-heading text-lg font-bold tracking-tight">
            <Activity className="h-5 w-5 text-primary" />
            UPTIMEMON
          </Link>

          <div className="flex items-center gap-3">
            {isOperator && (
              <Button variant="ghost" size="sm" asChild>
                <Link to="/">Dashboard</Link>
              </Button>
            )}
            {isAuthenticated ? (
              <Button
                variant="ghost"
                size="sm"
                onClick={() => {
                  logout();
                  navigate('/login');
                }}
              >
                Logout
              </Button>
            ) : (
              <Button variant="ghost" size="sm" asChild>
                <Link to="/login">Login</Link>
              </Button>
            )}
          </div>
        </div>
      </nav>

      {/* ── Main content ── */}
      <div className="mx-auto max-w-screen-xl px-8 py-8 space-y-6">
        {isOperator && (
          <Link to="/" className="inline-flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground transition-colors">
            <ArrowLeft className="h-4 w-4" />
            Dashboard
          </Link>
        )}

        <PageHeader title="Service Status" description={org ? `Organization: ${org}` : 'Check the live status of monitored services'}>
          {lastUpdated && (
            <span className="flex items-center gap-1.5 text-xs text-muted-foreground">
              <Clock className="h-3.5 w-3.5" />
              Updated {lastUpdated.toLocaleTimeString()}
            </span>
          )}
          {org && (
            <Button
              variant="ghost"
              size="sm"
              disabled={loading}
              onClick={() => fetchStatus(org)}
            >
              <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            </Button>
          )}
        </PageHeader>

        {/* ── Org input for unauthenticated users ── */}
        {!isAuthenticated && !org && (
          <GlassCard className="p-6">
            <p className="text-sm text-muted-foreground mb-3">Enter an organization name to view its service status.</p>
            <form
              className="flex gap-3"
              onSubmit={(e) => {
                e.preventDefault();
                handleViewStatus();
              }}
            >
              <Input
                placeholder="Organization name"
                value={orgInput}
                onChange={(e) => setOrgInput(e.target.value)}
                className="max-w-sm"
              />
              <Button type="submit" disabled={loading}>
                {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <Globe className="h-4 w-4" />}
                <span className="ml-2">View Status</span>
              </Button>
            </form>
          </GlassCard>
        )}

        {/* ── Loading state ── */}
        {loading && monitors.length === 0 && (
          <div className="flex items-center justify-center py-20">
            <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
          </div>
        )}

        {/* ── Monitor list ── */}
        {monitors.length > 0 && (
          <div className="space-y-3">
            {monitors.map((monitor) => {
              const status = deriveStatus(monitor);
              const level = deriveUptimeLevel(monitor.uptime);
              const recentIncidents = monitor.incidents.slice(0, 3);

              return (
                <GlassCard key={monitor.id} className="p-5 transition-all hover:shadow-warm">
                  <div className="flex items-center justify-between gap-4">
                    {/* Left side */}
                    <div className="flex items-center gap-3 min-w-0">
                      <StatusDot status={status} />
                      <div className="min-w-0">
                        <p className="font-heading font-bold truncate">{monitor.name}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {monitor.target_url} · {statusLabel[status]}
                        </p>
                      </div>
                    </div>

                    {/* Right side — uptime */}
                    <span className={`font-heading text-lg font-bold tabular-nums shrink-0 ${uptimeColor[level]}`}>
                      {monitor.uptime.toFixed(1)}%
                    </span>
                  </div>

                  {/* Recent incidents */}
                  {recentIncidents.length > 0 && (
                    <div className="mt-3 border-t border-border/50 pt-3 space-y-1.5">
                      {recentIncidents.map((incident: StatusIncident) => (
                        <div key={incident.id} className="flex items-center justify-between text-xs text-muted-foreground">
                          <span>
                            HTTP {incident.status_code} · {formatTime(incident.down_time)}
                          </span>
                          <span className={incident.resolved_time ? 'text-success' : 'text-destructive'}>
                            {incident.resolved_time ? 'Resolved' : 'Ongoing'}
                          </span>
                        </div>
                      ))}
                    </div>
                  )}
                </GlassCard>
              );
            })}
          </div>
        )}

        {/* ── Empty state ── */}
        {!loading && org && monitors.length === 0 && (
          <GlassCard className="flex flex-col items-center justify-center py-16 text-center">
            <Globe className="h-10 w-10 text-muted-foreground mb-3" />
            <p className="font-heading font-semibold">No monitors found</p>
            <p className="text-sm text-muted-foreground mt-1">This organization has no monitored services yet.</p>
          </GlassCard>
        )}
      </div>
    </div>
  );
}
