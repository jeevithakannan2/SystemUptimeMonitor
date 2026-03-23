import { cn } from '@/lib/utils';
import type { MonitorStatus } from '@/types';

const config: Record<MonitorStatus, string> = {
  up: 'bg-success/12 text-success border-success/30',
  down: 'bg-destructive/12 text-destructive border-destructive/30 animate-status-pulse',
  disabled: 'bg-muted text-muted-foreground border-border/60',
};

const labels: Record<MonitorStatus, string> = {
  up: 'Operational',
  down: 'Down',
  disabled: 'Disabled',
};

export function StatusBadge({ status }: { status: MonitorStatus }) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold uppercase tracking-wider font-heading',
        config[status],
      )}
    >
      {status === 'down' && <span className="mr-1.5 h-1.5 w-1.5 rounded-full bg-current animate-pulse" />}
      {labels[status]}
    </span>
  );
}
