import { cn } from '@/lib/utils';
import type { HTMLAttributes } from 'react';

interface GlassCardProps extends HTMLAttributes<HTMLDivElement> {
  subtle?: boolean;
}

export function GlassCard({ className, subtle = false, children, ...props }: GlassCardProps) {
  return (
    <div className={cn('rounded-2xl', subtle ? 'glass-subtle' : 'glass', className)} {...props}>
      {children}
    </div>
  );
}
