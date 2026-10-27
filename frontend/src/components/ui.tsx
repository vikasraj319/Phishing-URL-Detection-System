import type { ReactNode } from 'react';
import { AlertCircle, Inbox, RefreshCw, Link2, Mail } from 'lucide-react';
import { CLASS_META } from '../lib/format';
import type { Classification, ScanType } from '../lib/types';
import { ApiError } from '../lib/api';

export function RiskBadge({ value }: { value: Classification }) {
  const m = CLASS_META[value];
  const Icon = m.icon;
  return (
    <span className={`inline-flex items-center gap-1 whitespace-nowrap rounded-full border px-2 py-0.5 text-xs font-medium ${m.bg} ${m.text}`}>
      <Icon className="h-3 w-3" aria-hidden /> {m.label}
    </span>
  );
}

export function ScoreChip({ score, classification }: { score: number; classification: Classification }) {
  const m = CLASS_META[classification];
  return <span className={`inline-block min-w-9 rounded-md px-2 py-0.5 text-center text-xs font-semibold tabular-nums ${m.bg} ${m.text}`} title={`Risk score ${score} of 100`}>{score}</span>;
}

export function TypeIcon({ type, className = 'h-4 w-4' }: { type: ScanType; className?: string }) {
  return type === 'URL'
    ? <Link2 className={className} aria-label="URL scan" role="img" />
    : <Mail className={className} aria-label="Message scan" role="img" />;
}

export function Skeleton({ className = 'h-4 w-full' }: { className?: string }) {
  return <div className={`skeleton ${className}`} aria-hidden />;
}

export function EmptyState({ title, hint, action }: { title: string; hint?: string; action?: ReactNode }) {
  return (
    <div className="flex flex-col items-center gap-2 px-4 py-8 text-center">
      <Inbox className="h-8 w-8 text-muted" aria-hidden />
      <p className="text-sm font-medium">{title}</p>
      {hint && <p className="max-w-xs text-xs text-muted">{hint}</p>}
      {action}
    </div>
  );
}

export function ErrorState({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  const msg = error instanceof ApiError ? error.message : 'Something went wrong while loading this data.';
  return (
    <div role="alert" className="flex flex-col items-center gap-2 px-4 py-8 text-center">
      <AlertCircle className="h-8 w-8 text-danger" aria-hidden />
      <p className="text-sm font-medium">Could not load data</p>
      <p className="max-w-xs text-xs text-muted">{msg}</p>
      {onRetry && <button className="btn-ghost mt-1" onClick={onRetry}><RefreshCw className="h-4 w-4" aria-hidden /> Retry</button>}
    </div>
  );
}

export function Card({ title, action, children, className = '' }: { title?: string; action?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`card p-5 ${className}`}>
      {(title || action) && (
        <header className="mb-4 flex items-center justify-between gap-3">
          {title && <h2 className="text-base font-semibold">{title}</h2>}
          {action}
        </header>
      )}
      {children}
    </section>
  );
}

export function RangeSelect({ value, onChange, label }: { value: '7d' | '30d'; onChange: (v: '7d' | '30d') => void; label: string }) {
  return (
    <select aria-label={label} value={value} onChange={(e) => onChange(e.target.value as '7d' | '30d')}
      className="rounded-md border border-line bg-surface px-2 py-1 text-xs text-fg">
      <option value="7d">Last 7 days</option>
      <option value="30d">Last 30 days</option>
    </select>
  );
}

export function Dialog({ title, children, onClose }: { title: string; children: ReactNode; onClose: () => void }) {
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4" onKeyDown={(e) => e.key === 'Escape' && onClose()}>
      <div role="dialog" aria-modal="true" aria-label={title} className="card w-full max-w-md p-6">
        <h2 className="mb-3 text-lg font-semibold">{title}</h2>
        {children}
      </div>
    </div>
  );
}
