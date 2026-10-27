import { useState } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight, ArrowDown, ArrowUp, Link2, ShieldAlert, AlertTriangle, ShieldCheck, ChevronRight } from 'lucide-react';
import { Area, AreaChart, CartesianGrid, Cell, Pie, PieChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts';
import { QuickScan } from '../components/ScanForm';
import { Card, EmptyState, ErrorState, RangeSelect, RiskBadge, ScoreChip, Skeleton, TypeIcon } from '../components/ui';
import { ShieldArt } from '../components/ShieldArt';
import { useActivity, useDistribution, useIndicators, useScans, useStats } from '../lib/queries';
import { fullDate, pct, relativeTime, trend } from '../lib/format';
import type { Range } from '../lib/types';
import type { LucideIcon } from 'lucide-react';

function StatCard({ icon: Icon, tone, label, value, note, delta }: { icon: LucideIcon; tone: string; label: string; value: string; note: string; delta?: number | null }) {
  return (
    <div className="card flex items-start gap-3 p-4">
      <span className={`flex h-11 w-11 shrink-0 items-center justify-center rounded-full ${tone}`}><Icon className="h-5 w-5" aria-hidden /></span>
      <div className="min-w-0">
        <div className="text-xs text-muted">{label}</div>
        <div className="text-2xl font-bold tabular-nums">{value}</div>
        {delta !== undefined && delta !== null
          ? <div className={`flex items-center gap-1 text-xs ${delta >= 0 ? 'text-safe' : 'text-warn'}`}>
              {delta >= 0 ? <ArrowUp className="h-3 w-3" aria-hidden /> : <ArrowDown className="h-3 w-3" aria-hidden />}
              <span>{Math.abs(delta)}% {delta >= 0 ? 'more' : 'fewer'} than the previous 7 days</span>
            </div>
          : <div className="text-xs text-muted">{note}</div>}
      </div>
    </div>
  );
}

function Stats() {
  const { data, isPending, error, refetch } = useStats('7d');
  if (error) return <div className="card sm:col-span-2 xl:col-span-4"><ErrorState error={error} onRetry={() => void refetch()} /></div>;
  if (isPending) return <>{[0, 1, 2, 3].map((i) => <div key={i} className="card p-4"><Skeleton className="h-16" /></div>)}</>;
  const empty = data.totalScans === 0;
  const p = data.previous;
  return (
    <>
      <StatCard icon={Link2} tone="bg-primary/20 text-primary" label="Total Scans" value={String(data.totalScans)} note={empty ? 'No scans yet' : 'All scans on record'} delta={p ? trend(data.totalScans, p.total) : null} />
      <StatCard icon={ShieldAlert} tone="bg-danger/20 text-danger" label="High Risk Detections" value={String(data.highRisk)} note={empty ? 'No scans yet' : 'Classified high risk'} delta={p ? trend(data.highRisk, p.highRisk) : null} />
      <StatCard icon={AlertTriangle} tone="bg-warn/20 text-warn" label="Suspicious Results" value={String(data.suspicious)} note={empty ? 'No scans yet' : 'Classified suspicious'} delta={p ? trend(data.suspicious, p.suspicious) : null} />
      <StatCard icon={ShieldCheck} tone="bg-safe/20 text-safe" label="Detection Rate" value={data.flaggedRate === null ? '—' : `${(data.flaggedRate * 100).toFixed(1)}%`}
        note={data.flaggedRate === null ? 'No scans yet' : 'Share of scans flagged suspicious or high risk. Not an accuracy measure.'} />
    </>
  );
}

const COLORS = { safe: 'rgb(var(--safe))', warn: 'rgb(var(--warn))', danger: 'rgb(var(--danger))' };

function Distribution() {
  const [range, setRange] = useState<Range>('7d');
  const { data, isPending, error, refetch } = useDistribution(range);
  const rows = data ? [
    { key: 'Safe-looking', n: data.safeLooking, color: COLORS.safe },
    { key: 'Suspicious', n: data.suspicious, color: COLORS.warn },
    { key: 'High risk', n: data.highRisk, color: COLORS.danger },
  ] : [];
  return (
    <Card title="Risk Distribution" action={<RangeSelect label="Risk distribution range" value={range} onChange={setRange} />}>
      {error ? <ErrorState error={error} onRetry={() => void refetch()} />
        : isPending ? <Skeleton className="h-44" />
        : data.total === 0 ? <EmptyState title="No scans in this period" hint="Run a scan and the distribution will appear here." />
        : (
          <div className="flex flex-wrap items-center gap-6">
            <div className="relative h-44 w-44 shrink-0" role="img" aria-label={`Risk distribution of ${data.total} scans: ${rows.map((r) => `${r.key} ${r.n}`).join(', ')}`}>
              <ResponsiveContainer>
                <PieChart>
                  <Pie data={rows.filter((r) => r.n > 0)} dataKey="n" nameKey="key" innerRadius={56} outerRadius={80} paddingAngle={2} stroke="none">
                    {rows.filter((r) => r.n > 0).map((r) => <Cell key={r.key} fill={r.color} />)}
                  </Pie>
                  <Tooltip contentStyle={{ background: 'rgb(var(--card))', border: '1px solid rgb(var(--line))', borderRadius: 8, color: 'rgb(var(--fg))' }} />
                </PieChart>
              </ResponsiveContainer>
              <div className="pointer-events-none absolute inset-0 flex flex-col items-center justify-center"><span className="text-2xl font-bold tabular-nums">{data.total}</span><span className="text-xs text-muted">Total Scans</span></div>
            </div>
            <ul className="min-w-[200px] flex-1 space-y-2 text-sm">
              {rows.map((r) => (
                <li key={r.key} className="flex items-center gap-2">
                  <span className="h-3 w-3 rounded-full" style={{ background: r.color }} aria-hidden />
                  <span className="flex-1">{r.key}</span><span className="tabular-nums">{r.n}</span><span className="w-14 text-right tabular-nums text-muted">{pct(r.n, data.total)}</span>
                </li>
              ))}
            </ul>
          </div>
        )}
    </Card>
  );
}

function Activity() {
  const [range, setRange] = useState<Range>('7d');
  const { data, isPending, error, refetch } = useActivity(range);
  const tick = { fill: 'rgb(var(--muted))', fontSize: 11 };
  const short = (d: string) => new Intl.DateTimeFormat('en', { month: 'short', day: 'numeric' }).format(new Date(d + 'T00:00:00'));
  return (
    <Card title="Scan Activity" action={<RangeSelect label="Scan activity range" value={range} onChange={setRange} />}>
      {error ? <ErrorState error={error} onRetry={() => void refetch()} />
        : isPending ? <Skeleton className="h-44" />
        : data.every((p) => p.count === 0) ? <EmptyState title="No scan activity in this period" hint="Scans you run will be plotted by day." />
        : (
          <div className="h-48" role="img" aria-label={`Scans per day over the last ${range === '7d' ? 7 : 30} days`}>
            <ResponsiveContainer>
              <AreaChart data={data} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                <defs><linearGradient id="act" x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor="rgb(var(--primary))" stopOpacity={0.35} /><stop offset="100%" stopColor="rgb(var(--primary))" stopOpacity={0} /></linearGradient></defs>
                <CartesianGrid stroke="rgb(var(--line))" strokeOpacity={0.6} vertical={false} />
                <XAxis dataKey="date" tickFormatter={short} tick={tick} stroke="rgb(var(--line))" interval="preserveStartEnd" />
                <YAxis allowDecimals={false} tick={tick} stroke="rgb(var(--line))" />
                <Tooltip labelFormatter={(l) => short(String(l))} formatter={(v) => [`${v} scans`, 'Scans']} contentStyle={{ background: 'rgb(var(--card))', border: '1px solid rgb(var(--line))', borderRadius: 8, color: 'rgb(var(--fg))' }} />
                <Area type="monotone" dataKey="count" stroke="rgb(var(--primary))" strokeWidth={2} fill="url(#act)" dot={{ r: 3 }} />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        )}
    </Card>
  );
}

function RecentTable() {
  const { data, isPending, error, refetch } = useScans({ size: 5, sort: 'createdAt', dir: 'desc' });
  return (
    <Card title="Recent Scans" action={<Link to="/history" className="flex items-center gap-1 text-sm text-primary hover:underline">View all <ArrowRight className="h-4 w-4" aria-hidden /></Link>}>
      {error ? <ErrorState error={error} onRetry={() => void refetch()} />
        : isPending ? <Skeleton className="h-40" />
        : data.items.length === 0 ? <EmptyState title="No scans yet" hint="Your most recent scans will appear here." />
        : (
          <div className="-mx-2 overflow-x-auto">
            <table className="w-full min-w-[560px]">
              <thead><tr><th className="th">Type</th><th className="th">Input</th><th className="th">Risk score</th><th className="th">Classification</th><th className="th">Time</th><th className="th"><span className="sr-only">Open</span></th></tr></thead>
              <tbody className="divide-y divide-line">
                {data.items.map((s) => (
                  <tr key={s.id} className="hover:bg-surface/60">
                    <td className="td"><span className="flex items-center gap-2"><TypeIcon type={s.type} /><span className="hidden text-xs text-muted xl:inline">{s.type === 'URL' ? 'URL Scan' : 'Message'}</span></span></td>
                    <td className="td max-w-[200px] truncate" title={s.inputDisplay}>{s.inputDisplay}</td>
                    <td className="td"><ScoreChip score={s.score} classification={s.classification} /></td>
                    <td className="td"><RiskBadge value={s.classification} /></td>
                    <td className="td whitespace-nowrap text-xs text-muted">{fullDate(s.createdAt)}</td>
                    <td className="td"><Link to={`/scans/${s.id}`} aria-label={`Open scan of ${s.inputDisplay}`}><ChevronRight className="h-4 w-4 text-muted" aria-hidden /></Link></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
    </Card>
  );
}

function Indicators() {
  const [range, setRange] = useState<Range>('7d');
  const { data, isPending, error, refetch } = useIndicators(range);
  const max = data?.reduce((m, i) => Math.max(m, i.count), 0) ?? 0;
  return (
    <Card title="Top Detection Indicators" action={<RangeSelect label="Indicators range" value={range} onChange={setRange} />}>
      {error ? <ErrorState error={error} onRetry={() => void refetch()} />
        : isPending ? <Skeleton className="h-40" />
        : data.length === 0 ? <EmptyState title="No indicators recorded" hint="Rules that fire during scans are counted here." />
        : (
          <ul className="space-y-4">
            {data.slice(0, 6).map((i) => (
              <li key={i.ruleId}>
                <div className="mb-1 flex justify-between text-sm"><span>{i.title}</span><span className="tabular-nums text-muted">{i.count}</span></div>
                <div className="h-1.5 rounded-full bg-surface" role="presentation"><div className="h-full rounded-full bg-gradient-to-r from-violet to-primary" style={{ width: `${max ? (i.count / max) * 100 : 0}%` }} /></div>
              </li>
            ))}
          </ul>
        )}
    </Card>
  );
}

function RecentPanel() {
  const { data, isPending, error, refetch } = useScans({ size: 5, sort: 'createdAt', dir: 'desc' });
  return (
    <Card title="Recent Scans" action={<Link to="/history" className="flex items-center gap-1 text-sm text-primary hover:underline">View all <ArrowRight className="h-4 w-4" aria-hidden /></Link>}>
      {error ? <ErrorState error={error} onRetry={() => void refetch()} />
        : isPending ? <Skeleton className="h-48" />
        : data.items.length === 0 ? <EmptyState title="Nothing scanned yet" />
        : (
          <ul className="divide-y divide-line">
            {data.items.map((s) => (
              <li key={s.id}>
                <Link to={`/scans/${s.id}`} className="flex items-center gap-3 py-3 hover:bg-surface/50">
                  <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full bg-surface text-muted"><TypeIcon type={s.type} className="h-5 w-5" /></span>
                  <span className="min-w-0 flex-1"><span className="block truncate text-sm font-medium" title={s.inputDisplay}>{s.inputDisplay}</span><span className="text-xs text-muted">{s.type === 'URL' ? 'URL Scan' : 'Message Scan'}</span></span>
                  <span className="flex flex-col items-end gap-1"><RiskBadge value={s.classification} /><span className="text-xs text-muted">{relativeTime(s.createdAt)}</span></span>
                </Link>
              </li>
            ))}
          </ul>
        )}
    </Card>
  );
}

export function OverviewPage() {
  return (
    <div className="mx-auto grid max-w-[1500px] gap-6 xl:grid-cols-[minmax(0,1fr)_380px]">
      <div className="min-w-0 space-y-6">
        <section className="flex items-center justify-between gap-4">
          <div>
            <h2 className="text-3xl font-bold">Your Security Overview</h2>
            <p className="mt-2 max-w-md text-sm text-muted">Analyze suspicious links and messages before they put your data at risk.</p>
            <Link to="/url-scanner" className="btn-primary mt-4 w-fit">Start a scan <ArrowRight className="h-4 w-4" aria-hidden /></Link>
          </div>
          <div className="hidden shrink-0 items-center sm:flex"><ShieldArt className="h-36 w-52" /><div className="-ml-2 text-xs"><div className="font-semibold text-cyan">AI-Powered Phishing Detection</div></div></div>
        </section>
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4"><Stats /></div>
        <div className="grid gap-6 lg:grid-cols-2"><Distribution /><Activity /></div>
        <div className="grid gap-6 lg:grid-cols-[minmax(0,1.4fr)_minmax(0,1fr)]"><RecentTable /><Indicators /></div>
      </div>
      <div className="min-w-0 space-y-6"><QuickScan /><RecentPanel /></div>
    </div>
  );
}
