import { cn } from '@/lib/utils';
import type { MonitorStatus } from '@/types';

const dotColors: Record<MonitorStatus, string> = {
  up: 'bg-success',
  down: 'bg-destructive animate-pulse',
  disabled: 'bg-muted-foreground',
};

export function StatusDot({ status }: { status: MonitorStatus }) {
  return <span className={cn('inline-block h-2.5 w-2.5 rounded-full shrink-0', dotColors[status])} />;
}
