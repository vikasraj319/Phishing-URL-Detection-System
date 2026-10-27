import { AlertTriangle, CheckCircle2, ShieldAlert, type LucideIcon } from 'lucide-react';
import type { Classification, Severity } from './types';

export const CLASS_META: Record<Classification, { label: string; icon: LucideIcon; text: string; bg: string }> = {
  SAFE_LOOKING: { label: 'Safe-looking', icon: CheckCircle2, text: 'text-safe', bg: 'bg-safe/15 border-safe/40' },
  SUSPICIOUS: { label: 'Suspicious', icon: AlertTriangle, text: 'text-warn', bg: 'bg-warn/15 border-warn/40' },
  HIGH_RISK: { label: 'High risk', icon: ShieldAlert, text: 'text-danger', bg: 'bg-danger/15 border-danger/40' },
};

export const SEVERITY_TEXT: Record<Severity, string> = {
  INFO: 'text-muted', LOW: 'text-cyan', MEDIUM: 'text-warn', HIGH: 'text-danger', CRITICAL: 'text-danger',
};

export function relativeTime(iso: string, now = Date.now()): string {
  const diff = (new Date(iso).getTime() - now) / 1000;
  const rtf = new Intl.RelativeTimeFormat('en', { numeric: 'auto' });
  const abs = Math.abs(diff);
  if (abs < 60) return 'just now';
  if (abs < 3600) return rtf.format(Math.round(diff / 60), 'minute');
  if (abs < 86400) return rtf.format(Math.round(diff / 3600), 'hour');
  return rtf.format(Math.round(diff / 86400), 'day');
}

export const fullDate = (iso: string) =>
  new Intl.DateTimeFormat('en', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(iso));

export const pct = (part: number, total: number) => (total > 0 ? `${((part / total) * 100).toFixed(1)}%` : '0%');

/** Percent change vs. the previous period; null when there is no baseline to compare with. */
export function trend(current: number, previous: number | undefined | null): number | null {
  if (previous === undefined || previous === null || previous <= 0) return null;
  return Math.round(((current - previous) / previous) * 100);
}
