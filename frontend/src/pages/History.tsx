import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowDown, ArrowUp, ChevronLeft, ChevronRight, Search, Trash2 } from 'lucide-react';
import { Card, Dialog, EmptyState, ErrorState, RiskBadge, ScoreChip, Skeleton, TypeIcon } from '../components/ui';
import { useToast } from '../components/Toast';
import { useDeleteScan, useScans } from '../lib/queries';
import { ApiError } from '../lib/api';
import { fullDate } from '../lib/format';
import type { Classification, HistoryParams, ScanListItem, ScanType } from '../lib/types';

type SortKey = NonNullable<HistoryParams['sort']>;
const COLS: { key: SortKey; label: string }[] = [
  { key: 'type', label: 'Type' }, { key: 'score', label: 'Risk score' }, { key: 'classification', label: 'Classification' }, { key: 'createdAt', label: 'Scanned' },
];

export function HistoryPage() {
  const [sp, setSp] = useSearchParams();
  const { notify } = useToast();
  const del = useDeleteScan();
  const [target, setTarget] = useState<ScanListItem | null>(null);
  const q = sp.get('q') ?? '';
  const [text, setText] = useState(q);
  useEffect(() => setText(q), [q]);

  const params: HistoryParams = {
    q, type: (sp.get('type') ?? '') as ScanType | '', classification: (sp.get('classification') ?? '') as Classification | '',
    from: sp.get('from') ?? '', to: sp.get('to') ?? '', sort: (sp.get('sort') as SortKey) || 'createdAt',
    dir: sp.get('dir') === 'asc' ? 'asc' : 'desc', page: Number(sp.get('page') ?? 0) || 0, size: 10,
  };
  const { data, isPending, isFetching, error, refetch } = useScans(params);

  const set = (patch: Record<string, string>) => {
    const n = new URLSearchParams(sp);
    Object.entries(patch).forEach(([k, v]) => (v ? n.set(k, v) : n.delete(k)));
    if (!('page' in patch)) n.delete('page');
    setSp(n, { replace: true });
  };
  useEffect(() => { const t = setTimeout(() => { if (text !== q) set({ q: text.trim() }); }, 350); return () => clearTimeout(t); }, [text]); // eslint-disable-line react-hooks/exhaustive-deps
  const sortBy = (k: SortKey) => set({ sort: k, dir: params.sort === k && params.dir === 'desc' ? 'asc' : 'desc' });
  const filtered = !!(q || params.type || params.classification || params.from || params.to);

  const confirmDelete = () => {
    if (!target) return;
    del.mutate(target.id, {
      onSuccess: () => { notify('success', 'Scan deleted.'); setTarget(null); },
      onError: (e) => { setTarget(null); notify('error', e instanceof ApiError ? e.message : 'Could not delete the scan.'); },
    });
  };

  return (
    <div className="mx-auto max-w-6xl space-y-4">
      <Card>
        <form role="search" onSubmit={(e) => { e.preventDefault(); set({ q: text.trim() }); }} className="grid gap-3 sm:grid-cols-2 lg:grid-cols-[1fr_140px_160px_150px_150px]">
          <div className="relative"><label htmlFor="h-q" className="sr-only">Search history</label><Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" aria-hidden />
            <input id="h-q" className="field pl-9" placeholder="Search scans…" value={text} onChange={(e) => setText(e.target.value)} maxLength={200} /></div>
          <div><label htmlFor="h-type" className="sr-only">Scan type</label><select id="h-type" className="field" value={params.type} onChange={(e) => set({ type: e.target.value })}><option value="">All types</option><option value="URL">URL</option><option value="MESSAGE">Message</option></select></div>
          <div><label htmlFor="h-class" className="sr-only">Classification</label><select id="h-class" className="field" value={params.classification} onChange={(e) => set({ classification: e.target.value })}><option value="">All classifications</option><option value="SAFE_LOOKING">Safe-looking</option><option value="SUSPICIOUS">Suspicious</option><option value="HIGH_RISK">High risk</option></select></div>
          <div><label htmlFor="h-from" className="sr-only">From date</label><input id="h-from" type="date" className="field" value={params.from} max={params.to || undefined} onChange={(e) => set({ from: e.target.value })} /></div>
          <div><label htmlFor="h-to" className="sr-only">To date</label><input id="h-to" type="date" className="field" value={params.to} min={params.from || undefined} onChange={(e) => set({ to: e.target.value })} /></div>
        </form>
      </Card>

      <section className="card overflow-hidden" aria-busy={isFetching}>
        {error ? <ErrorState error={error} onRetry={() => void refetch()} />
          : isPending ? <div className="space-y-2 p-5">{[0, 1, 2, 3, 4].map((i) => <Skeleton key={i} className="h-10" />)}</div>
          : data.items.length === 0 ? <EmptyState title={filtered ? 'No scans match these filters' : 'No scans yet'} hint={filtered ? 'Try clearing a filter.' : 'Scan a URL or message and it will be listed here.'}
              action={filtered ? <button className="btn-ghost" onClick={() => { setText(''); setSp({}, { replace: true }); }}>Clear filters</button> : <Link to="/url-scanner" className="btn-primary">Scan a URL</Link>} />
          : (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[720px]">
                <thead><tr className="border-b border-line">
                  {COLS.slice(0, 1).map((c) => <SortTh key={c.key} c={c} p={params} onSort={sortBy} />)}
                  <th className="th">Input</th>
                  {COLS.slice(1).map((c) => <SortTh key={c.key} c={c} p={params} onSort={sortBy} />)}
                  <th className="th"><span className="sr-only">Actions</span></th>
                </tr></thead>
                <tbody className="divide-y divide-line">
                  {data.items.map((s) => (
                    <tr key={s.id} className="hover:bg-surface/60">
                      <td className="td"><span className="flex items-center gap-2"><TypeIcon type={s.type} />{s.type === 'URL' ? 'URL' : 'Message'}</span></td>
                      <td className="td max-w-[320px] truncate" title={s.inputDisplay}><Link to={`/scans/${s.id}`} className="hover:underline">{s.inputDisplay}</Link></td>
                      <td className="td"><ScoreChip score={s.score} classification={s.classification} /></td>
                      <td className="td"><RiskBadge value={s.classification} /></td>
                      <td className="td whitespace-nowrap text-xs text-muted">{fullDate(s.createdAt)}</td>
                      <td className="td"><div className="flex justify-end gap-1">
                        <Link to={`/scans/${s.id}`} className="btn-ghost px-2.5 py-1.5 text-xs">Details</Link>
                        <button className="btn-ghost px-2 py-1.5 text-danger" aria-label={`Delete scan of ${s.inputDisplay}`} onClick={() => setTarget(s)}><Trash2 className="h-4 w-4" aria-hidden /></button></div></td>
                    </tr>
                  ))}
                </tbody>
              </table>
              <nav aria-label="Pagination" className="flex items-center justify-between border-t border-line px-4 py-3 text-sm">
                <span className="text-muted">{data.totalItems} scan{data.totalItems === 1 ? '' : 's'} · page {data.page + 1} of {Math.max(1, data.totalPages)}</span>
                <span className="flex gap-2">
                  <button className="btn-ghost px-2.5 py-1.5" disabled={data.page <= 0} onClick={() => set({ page: String(data.page - 1) })} aria-label="Previous page"><ChevronLeft className="h-4 w-4" aria-hidden /></button>
                  <button className="btn-ghost px-2.5 py-1.5" disabled={data.page + 1 >= data.totalPages} onClick={() => set({ page: String(data.page + 1) })} aria-label="Next page"><ChevronRight className="h-4 w-4" aria-hidden /></button>
                </span>
              </nav>
            </div>
          )}
      </section>

      {target && (
        <Dialog title="Delete this scan?" onClose={() => setTarget(null)}>
          <p className="break-all text-sm text-muted">“{target.inputDisplay}” and its findings will be removed permanently.</p>
          <div className="mt-5 flex justify-end gap-2"><button className="btn-ghost" onClick={() => setTarget(null)}>Cancel</button><button className="btn-danger" onClick={confirmDelete} disabled={del.isPending}>Delete</button></div>
        </Dialog>
      )}
    </div>
  );
}

function SortTh({ c, p, onSort }: { c: { key: SortKey; label: string }; p: HistoryParams; onSort: (k: SortKey) => void }) {
  const active = p.sort === c.key;
  return (
    <th className="th" aria-sort={active ? (p.dir === 'asc' ? 'ascending' : 'descending') : 'none'}>
      <button className="inline-flex items-center gap-1 hover:text-fg" onClick={() => onSort(c.key)}>
        {c.label}{active && (p.dir === 'asc' ? <ArrowUp className="h-3 w-3" aria-hidden /> : <ArrowDown className="h-3 w-3" aria-hidden />)}
      </button>
    </th>
  );
}
