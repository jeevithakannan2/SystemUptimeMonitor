import { useEffect, useState, useCallback, useRef } from 'react';
import { toast } from 'sonner';
import {
  Activity,
  AlertTriangle,
  Clock,
  Plus,
  Pencil,
  Trash2,
  History,
  CheckCircle2,
  Loader2,
} from 'lucide-react';

import {
  getMonitors,
  getIncidents,
  createMonitor,
  updateMonitor,
  deleteMonitor,
  resolveIncident,
  getMonitorHistory,
  getStatus,
} from '@/services/api';
import { useAuth } from '@/hooks/useAuth';
import type { Monitor, Incident, MonitorAudit, MonitorStatus } from '@/types';

import { GlassCard } from '@/components/GlassCard';
import { StatCard } from '@/components/StatCard';
import { StatusDot } from '@/components/StatusDot';
import { PageHeader } from '@/components/PageHeader';

import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogClose,
} from '@/components/ui/dialog';
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { Switch } from '@/components/ui/switch';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';

/* ─── Helpers ──────────────────────────────────────────────────── */

function getMonitorStatus(monitor: Monitor, incidents: Incident[]): MonitorStatus {
  if (!monitor.enabled) return 'disabled';
  const hasUnresolved = incidents.some(
    (i) => i.monitor_id === monitor.id && !i.resolved,
  );
  return hasUnresolved ? 'down' : 'up';
}

function formatTime(ts: string | number | null | undefined): string {
  if (!ts) return '—';
  const d = new Date(ts);
  return isNaN(d.getTime()) ? '—' : d.toLocaleString();
}

/* ─── Default form values ──────────────────────────────────────── */

const EMPTY_FORM = {
  name: '',
  target_url: '',
  check_interval: '60',
  expected_status_codes: '200',
  failure_count: '3',
  enabled: 'true',
  is_public: 'false',
};

/* ─── Component ────────────────────────────────────────────────── */

export default function Dashboard() {

  const { user } = useAuth();

  const [monitors, setMonitors] = useState<Monitor[]>([]);
  const [incidents, setIncidents] = useState<Incident[]>([]);
  const [loading, setLoading] = useState(true);
  const [avgUptime, setAvgUptime] = useState<number | null>(null);

  /* Modal state */
  const [createOpen, setCreateOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [resolveOpen, setResolveOpen] = useState(false);
  const [historyOpen, setHistoryOpen] = useState(false);
  const [deleteConfirmId, setDeleteConfirmId] = useState<number | null>(null);

  /* Form state */
  const [form, setForm] = useState(EMPTY_FORM);
  const [editId, setEditId] = useState<number | null>(null);
  const [resolveId, setResolveId] = useState<number | null>(null);
  const [resolveNotes, setResolveNotes] = useState('');
  const [historyData, setHistoryData] = useState<MonitorAudit[]>([]);
  const [historyMonitorName, setHistoryMonitorName] = useState('');
  const [submitting, setSubmitting] = useState(false);

  /* ── Data fetching ───────────────────────────────────────────── */

  const fetchMonitors = useCallback(async () => {
    try {
      const res = await getMonitors();
      setMonitors(res.monitors);
    } catch {
      toast.error('Failed to load monitors');
    }
  }, []);

  const fetchIncidents = useCallback(async () => {
    try {
      const res = await getIncidents();
      setIncidents(res.incidents);
    } catch {
      /* silent — polling */
    }
  }, []);

  const fetchUptime = useCallback(async () => {
    if (!user?.organization) return;
    try {
      const res = await getStatus(user.organization);
      if (res.monitors.length > 0) {
        const sum = res.monitors.reduce((acc, m) => acc + m.uptime, 0);
        setAvgUptime(sum / res.monitors.length);
      } else {
        setAvgUptime(null);
      }
    } catch {
      /* silent */
    }
  }, [user?.organization]);

  useEffect(() => {
    async function init() {
      setLoading(true);
      await Promise.all([fetchMonitors(), fetchIncidents(), fetchUptime()]);
      setLoading(false);
    }
    init();
  }, [fetchMonitors, fetchIncidents, fetchUptime]);

  // Poll incidents every 5 s
  const pollRef = useRef<ReturnType<typeof setInterval>>(undefined);
  useEffect(() => {
    pollRef.current = setInterval(fetchIncidents, 5000);
    return () => clearInterval(pollRef.current);
  }, [fetchIncidents]);

  /* ── Stats ───────────────────────────────────────────────────── */

  const activeIncidents = incidents.filter((i) => !i.resolved).length;
  const enabledMonitors = monitors.filter((m) => m.enabled).length;

  /* ── CRUD handlers ───────────────────────────────────────────── */

  const handleCreate = async () => {
    setSubmitting(true);
    try {
      await createMonitor({
        name: form.name,
        target_url: form.target_url,
        check_interval: form.check_interval,
        expected_status_codes: form.expected_status_codes,
        failure_count: form.failure_count,
        enabled: form.enabled,
        is_public: form.is_public,
      });
      toast.success('Monitor created');
      setCreateOpen(false);
      setForm(EMPTY_FORM);
      await fetchMonitors();
    } catch {
      toast.error('Failed to create monitor');
    } finally {
      setSubmitting(false);
    }
  };

  const handleUpdate = async () => {
    if (editId === null) return;
    setSubmitting(true);
    try {
      await updateMonitor({
        id: String(editId),
        name: form.name,
        target_url: form.target_url,
        check_interval: form.check_interval,
        expected_status_codes: form.expected_status_codes,
        failure_count: form.failure_count,
        enabled: form.enabled,
        is_public: form.is_public,
      });
      toast.success('Monitor updated');
      setEditOpen(false);
      await fetchMonitors();
    } catch {
      toast.error('Failed to update monitor');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    setSubmitting(true);
    try {
      await deleteMonitor(id);
      toast.success('Monitor deleted');
      setDeleteConfirmId(null);
      await fetchMonitors();
    } catch {
      toast.error('Failed to delete monitor');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolve = async () => {
    if (resolveId === null) return;
    setSubmitting(true);
    try {
      await resolveIncident(resolveId, resolveNotes);
      toast.success('Incident resolved');
      setResolveOpen(false);
      setResolveNotes('');
      await fetchIncidents();
    } catch {
      toast.error('Failed to resolve incident');
    } finally {
      setSubmitting(false);
    }
  };

  const openEdit = (m: Monitor) => {
    setEditId(m.id);
    setForm({
      name: m.name,
      target_url: m.target_url,
      check_interval: String(m.check_interval),
      expected_status_codes: m.status_codes.join(','),
      failure_count: String(m.failure_count),
      enabled: String(m.enabled),
      is_public: String(m.is_public),
    });
    setEditOpen(true);
  };

  const openHistory = async (m: Monitor) => {
    setHistoryMonitorName(m.name);
    setHistoryOpen(true);
    try {
      const res = await getMonitorHistory(m.id);
      setHistoryData(res.history);
    } catch {
      toast.error('Failed to load history');
      setHistoryData([]);
    }
  };

  /* ── Render ──────────────────────────────────────────────────── */

  if (loading) {
    return (
      <div className="flex h-[60vh] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="mx-auto max-w-screen-xl px-8 py-8 space-y-8">
      {/* Header */}
      <PageHeader title="Dashboard" description="Monitor your services" />

      {/* Stats Row */}
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard icon={Activity} label="Total Monitors" value={monitors.length} />
        <StatCard
          icon={AlertTriangle}
          label="Active Incidents"
          value={activeIncidents}
          accent="destructive"
        />
        <StatCard icon={Clock} label="Avg Uptime" value={avgUptime !== null ? `${avgUptime.toFixed(1)}%` : '—'} />
        <StatCard
          icon={CheckCircle2}
          label="Monitors Enabled"
          value={enabledMonitors}
          accent="success"
        />
      </div>

      {/* ── Monitors Section ───────────────────────────────────── */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="font-heading text-xl font-semibold">Monitors</h2>
          <Button
            size="sm"
            onClick={() => {
              setForm(EMPTY_FORM);
              setCreateOpen(true);
            }}
          >
            <Plus className="mr-1.5 h-4 w-4" />
            Add Monitor
          </Button>
        </div>

        <GlassCard className="overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-10">Status</TableHead>
                <TableHead>Name</TableHead>
                <TableHead>URL</TableHead>
                <TableHead className="text-center">Interval</TableHead>
                <TableHead className="text-center">Expected Codes</TableHead>
                <TableHead className="text-center">Failures</TableHead>
                <TableHead>Created</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {monitors.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-muted-foreground py-8">
                    No monitors yet — create one to get started.
                  </TableCell>
                </TableRow>
              ) : (
                monitors.map((m) => (
                  <TableRow key={m.id}>
                    <TableCell>
                      <StatusDot status={getMonitorStatus(m, incidents)} />
                    </TableCell>
                    <TableCell className="font-semibold">
                      {m.name}
                      {m.is_public && <Badge variant="secondary" className="ml-2 text-xs">Public</Badge>}
                    </TableCell>
                    <TableCell className="max-w-[200px] truncate text-muted-foreground">
                      {m.target_url}
                    </TableCell>
                    <TableCell className="text-center tabular-nums">{m.check_interval}s</TableCell>
                    <TableCell className="text-center tabular-nums">
                      {m.status_codes.join(', ')}
                    </TableCell>
                    <TableCell className="text-center tabular-nums">{m.failure_count}</TableCell>
                    <TableCell className="text-muted-foreground text-sm">
                      {formatTime(m.created_time)}
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end gap-1">
                        <Button variant="ghost" size="icon" onClick={() => openEdit(m)}>
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button variant="ghost" size="icon" onClick={() => openHistory(m)}>
                          <History className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          onClick={() => setDeleteConfirmId(m.id)}
                        >
                          <Trash2 className="h-4 w-4 text-destructive" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </GlassCard>
      </section>

      {/* ── Incidents Section ──────────────────────────────────── */}
      <section className="space-y-4">
        <h2 className="font-heading text-xl font-semibold">Incidents</h2>

        <GlassCard className="overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-16">ID</TableHead>
                <TableHead>Monitor</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-center">Status Code</TableHead>
                <TableHead className="text-center">Expected Codes</TableHead>
                <TableHead>Down Since</TableHead>
                <TableHead>Resolved</TableHead>
                <TableHead className="text-right">Actions</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {incidents.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center text-muted-foreground py-8">
                    No incidents recorded.
                  </TableCell>
                </TableRow>
              ) : (
                incidents.map((inc) => {
                  const monitorName =
                    monitors.find((m) => m.id === inc.monitor_id)?.name ?? `#${inc.monitor_id}`;

                  return (
                    <TableRow key={inc.id}>
                      <TableCell className="tabular-nums">{inc.id}</TableCell>
                      <TableCell className="font-medium">{monitorName}</TableCell>
                      <TableCell>
                        {inc.resolved ? (
                          <Badge className="bg-success/15 text-success border-success/25 hover:bg-success/20">
                            Resolved
                          </Badge>
                        ) : (
                          <Badge className="bg-destructive/15 text-destructive border-destructive/25 hover:bg-destructive/20">
                            Active
                          </Badge>
                        )}
                      </TableCell>
                      <TableCell className="text-center tabular-nums">{inc.status_code}</TableCell>
                      <TableCell className="text-center tabular-nums">
                        {inc.expected_status_codes}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatTime(inc.down_time)}
                      </TableCell>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatTime(inc.resolved_time)}
                      </TableCell>
                      <TableCell className="text-right">
                        {!inc.resolved && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => {
                              setResolveId(inc.id);
                              setResolveNotes('');
                              setResolveOpen(true);
                            }}
                          >
                            <CheckCircle2 className="mr-1.5 h-3.5 w-3.5" />
                            Resolve
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </GlassCard>
      </section>

      {/* ── Create Monitor Dialog ──────────────────────────────── */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Create Monitor</DialogTitle>
          </DialogHeader>
          <MonitorForm form={form} setForm={setForm} />
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleCreate} disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Create
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Edit Monitor Dialog ────────────────────────────────── */}
      <Dialog open={editOpen} onOpenChange={setEditOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Edit Monitor</DialogTitle>
          </DialogHeader>
          <MonitorForm form={form} setForm={setForm} />
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleUpdate} disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Save Changes
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Resolve Incident Dialog ────────────────────────────── */}
      <Dialog open={resolveOpen} onOpenChange={setResolveOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Resolve Incident</DialogTitle>
          </DialogHeader>
          <div className="space-y-3 py-2">
            <div className="space-y-1.5">
              <Label htmlFor="resolve-notes">Notes (optional)</Label>
              <textarea
                id="resolve-notes"
                className="flex min-h-[80px] w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50"
                placeholder="Resolution notes…"
                maxLength={256}
                value={resolveNotes}
                onChange={(e) => setResolveNotes(e.target.value)}
              />
              <p className="text-xs text-muted-foreground text-right">
                {resolveNotes.length}/256
              </p>
            </div>
          </div>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button onClick={handleResolve} disabled={submitting}>
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Resolve
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Monitor History Dialog ─────────────────────────────── */}
      <Dialog open={historyOpen} onOpenChange={setHistoryOpen}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>History — {historyMonitorName}</DialogTitle>
          </DialogHeader>
          {historyData.length === 0 ? (
            <p className="py-6 text-center text-muted-foreground text-sm">No history available.</p>
          ) : (
            <div className="max-h-[300px] overflow-y-auto">
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead>Time</TableHead>
                    <TableHead>Operation</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {historyData.map((entry) => (
                    <TableRow key={entry.id}>
                      <TableCell className="text-sm text-muted-foreground">
                        {formatTime(entry.time)}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline">{entry.operation}</Badge>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Close</Button>
            </DialogClose>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* ── Delete Confirm Dialog ──────────────────────────────── */}
      <Dialog
        open={deleteConfirmId !== null}
        onOpenChange={(open) => {
          if (!open) setDeleteConfirmId(null);
        }}
      >
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Delete Monitor</DialogTitle>
          </DialogHeader>
          <p className="text-sm text-muted-foreground">
            Are you sure you want to delete this monitor? This action cannot be undone.
          </p>
          <DialogFooter>
            <DialogClose asChild>
              <Button variant="outline">Cancel</Button>
            </DialogClose>
            <Button
              variant="destructive"
              onClick={() => deleteConfirmId !== null && handleDelete(deleteConfirmId)}
              disabled={submitting}
            >
              {submitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              Delete
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}

/* ─── Monitor Form (shared between Create & Edit) ──────────────── */

interface MonitorFormProps {
  form: typeof EMPTY_FORM;
  setForm: React.Dispatch<React.SetStateAction<typeof EMPTY_FORM>>;
}

function MonitorForm({ form, setForm }: MonitorFormProps) {
  const update = (field: keyof typeof EMPTY_FORM, value: string) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  return (
    <div className="grid gap-4 py-2">
      <div className="space-y-1.5">
        <Label htmlFor="mon-name">Name</Label>
        <Input
          id="mon-name"
          placeholder="My API"
          value={form.name}
          onChange={(e) => update('name', e.target.value)}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="mon-url">Target URL</Label>
        <Input
          id="mon-url"
          placeholder="https://example.com/health"
          value={form.target_url}
          onChange={(e) => update('target_url', e.target.value)}
        />
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="mon-interval">Check Interval (s)</Label>
          <Input
            id="mon-interval"
            type="number"
            min={10}
            value={form.check_interval}
            onChange={(e) => update('check_interval', e.target.value)}
          />
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="mon-codes">Expected Status Codes</Label>
          <Input
            id="mon-codes"
            placeholder="200"
            value={form.expected_status_codes}
            onChange={(e) => update('expected_status_codes', e.target.value)}
          />
        </div>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="mon-failures">Failure Threshold</Label>
          <Input
            id="mon-failures"
            type="number"
            min={1}
            value={form.failure_count}
            onChange={(e) => update('failure_count', e.target.value)}
          />
        </div>

        <div className="space-y-1.5">
          <Label>Enabled</Label>
          <Select
            value={form.enabled}
            onValueChange={(v) => update('enabled', v)}
          >
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="true">Yes</SelectItem>
              <SelectItem value="false">No</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Switch
          id="mon-public"
          checked={form.is_public === 'true'}
          onCheckedChange={(checked) => update('is_public', String(checked))}
        />
        <Label htmlFor="mon-public">Public</Label>
      </div>
    </div>
  );
}
