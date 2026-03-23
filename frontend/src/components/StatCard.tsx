import type { ReactNode } from 'react';
import type { LucideIcon } from 'lucide-react';
import { cn } from '@/lib/utils';
import { GlassCard } from '@/components/GlassCard';

interface StatCardProps {
  icon: LucideIcon;
  label: string;
  value: number | string;
  accent?: 'info' | 'success' | 'destructive' | 'warning';
  children?: ReactNode;
}

const accentClasses: Record<string, string> = {
  info: 'text-info',
  success: 'text-success',
  destructive: 'text-destructive',
  warning: 'text-warning',
};

const accentBgClasses: Record<string, string> = {
  info: 'bg-info/10',
  success: 'bg-success/10',
  destructive: 'bg-destructive/10',
  warning: 'bg-warning/10',
};

export function StatCard({ icon: Icon, label, value, accent, children }: StatCardProps) {
  return (
    <GlassCard className="p-5 transition-all hover:shadow-warm">
      <div className="flex items-center justify-between mb-3">
        <span className="text-xs font-heading uppercase tracking-wider text-muted-foreground font-medium">
          {label}
        </span>
        <div
          className={cn(
            'flex h-8 w-8 items-center justify-center rounded-xl',
            accent ? accentBgClasses[accent] : 'bg-muted',
          )}
        >
          <Icon className={cn('h-4 w-4', accent ? accentClasses[accent] : 'text-muted-foreground')} />
        </div>
      </div>
      <div className={cn('text-2xl font-bold font-heading tabular-nums', accent ? accentClasses[accent] : '')}>
        {value}
      </div>
      {children && <div className="mt-1">{children}</div>}
    </GlassCard>
  );
}
