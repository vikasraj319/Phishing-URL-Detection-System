import { Fragment, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import { ChevronDown, CircleHelp, CircleCheck, CircleX, Plug, Trash2, Clock, Hash } from 'lucide-react';
import { Card, Dialog, ErrorState, RiskBadge, Skeleton } from '../components/ui';
import { ScoreRing } from '../components/ScoreRing';
import { useToast } from '../components/Toast';
import { useDeleteScan, useScan } from '../lib/queries';
import { ApiError } from '../lib/api';
import { fullDate, SEVERITY_TEXT } from '../lib/format';
import type { Finding, ProviderState, ScanResult, Span } from '../lib/types';

function explain(r: ScanResult): string {
  if (r.findings.length === 0) return 'No sufficient threat indicators were detected. This does not prove the ' + (r.type === 'URL' ? 'site' : 'message') + ' is safe.';
  const top = [...r.findings].sort((a, b) => b.scoreContribution - a.scoreContribution)[0];
  const n = r.findings.length;
  return `${n} indicator${n === 1 ? ' was' : 's were'} found. The strongest signal: ${top.title.charAt(0).toLowerCase()}${top.title.slice(1)}.`;
}

export function recommendations(r: ScanResult): string[] {
  const ids = new Set(r.findings.map((f) => f.ruleId));
  const out: string[] = [];
  if (r.classification !== 'SAFE_LOOKING') out.push('Do not enter passwords, codes or card details on this page or in reply to this message.');
  if (ids.has('brand_impersonation') || ids.has('mixed_script_host') || ids.has('punycode_host') || ids.has('userinfo_in_url'))
    out.push('Find the organization’s official address yourself (a bookmark, a card, an official app) and use that instead.');
  if (ids.has('url_shortener')) out.push('Do not follow the short link. Ask the sender for the full address through another channel.');
  if (ids.has('otp_request')) out.push('Never share one-time codes. If you already did, contact the service immediately and change your password.');
  if (ids.has('credential_request') || ids.has('account_threat')) out.push('If you worry about the account, sign in by typing the official address yourself, not through this link.');
  if (ids.has('payment_request') || ids.has('prize_scam')) out.push('Do not pay or send gift cards, crypto or transfers to claim anything.');
  if (ids.has('bypass_verification')) out.push('Real organizations never ask you to hide a request from your bank or family. Stop and talk to someone you trust.');
  if (r.type === 'MESSAGE' && r.classification !== 'SAFE_LOOKING') out.push('Report the message through your email or phone provider, or the impersonated organization’s official channel.');
  if (out.length === 0) out.push('Stay cautious: verify unexpected requests with the sender through an independently obtained contact.');
  return out;
}

const PROVIDER_META: Record<ProviderState, { label: string; icon: typeof CircleCheck; cls: string }> = {
  CLEAN: { label: 'No match found', icon: CircleCheck, cls: 'text-safe' },
  FLAGGED: { label: 'Flagged', icon: CircleX, cls: 'text-danger' },
  UNKNOWN: { label: 'Unknown', icon: CircleHelp, cls: 'text-muted' },
  UNAVAILABLE: { label: 'Check failed (not a clean result)', icon: CircleHelp, cls: 'text-warn' },
  NOT_CONFIGURED: { label: 'Not configured', icon: Plug, cls: 'text-muted' },
};

function merge(spans: Span[]): Span[] {
  const s = [...spans].sort((a, b) => a.start - b.start);
  const out: Span[] = [];
  for (const x of s) {
    const last = out[out.length - 1];
    if (last && x.start <= last.end) last.end = Math.max(last.end, x.end); else out.push({ ...x });
  }
  return out;
}

export function HighlightedMessage({ text, findings }: { text: string; findings: Finding[] }) {
  const spans = merge(findings.flatMap((f) => f.spans ?? []).filter((s) => s.start >= 0 && s.end <= text.length && s.end > s.start));
  const parts: JSX.Element[] = [];
  let at = 0;
  spans.forEach((s, i) => {
    if (s.start > at) parts.push(<Fragment key={`t${i}`}>{text.slice(at, s.start)}</Fragment>);
    parts.push(
      <mark key={`m${i}`} className="rounded bg-warn/25 px-0.5 text-fg underline decoration-warn decoration-2 underline-offset-2">
        <span className="sr-only">[flagged phrase: </span>{text.slice(s.start, s.end)}<span className="sr-only">]</span>
      </mark>);
    at = s.end;
  });
  if (at < text.length) parts.push(<Fragment key="end">{text.slice(at)}</Fragment>);
  return <p className="whitespace-pre-wrap break-words rounded-lg bg-surface p-4 text-sm leading-relaxed">{parts}</p>;
}

function FindingRow({ f }: { f: Finding }) {
  const [open, setOpen] = useState(false);
  return (
    <li className="rounded-lg border border-line">
      <button className="flex w-full items-center gap-3 p-3 text-left" aria-expanded={open} onClick={() => setOpen((o) => !o)}>
        <span className={`w-16 shrink-0 text-xs font-semibold ${SEVERITY_TEXT[f.severity]}`}>{f.severity}</span>
        <span className="flex-1 text-sm font-medium">{f.title}</span>
        <span className="text-xs tabular-nums text-muted" title="Contribution to the total score">+{f.scoreContribution}</span>
        <ChevronDown className={`h-4 w-4 shrink-0 transition-transform ${open ? 'rotate-180' : ''}`} aria-hidden />
      </button>
      {open && (
        <div className="space-y-2 border-t border-line p-3 text-sm">
          <p>{f.explanation}</p>
          {f.evidence && <p className="break-words"><span className="text-muted">Evidence: </span><code className="rounded bg-surface px-1.5 py-0.5 text-xs">{f.evidence}</code></p>}
          <p className="text-xs text-muted">Rule: {f.ruleId}</p>
        </div>
      )}
    </li>
  );
}

export function ScanDetailsPage() {
  const { id = '' } = useParams();
  const nav = useNavigate();
  const { notify } = useToast();
  const location = useLocation();
  const messageText = (location.state as { messageText?: string } | null)?.messageText;
  const { data, isPending, error, refetch } = useScan(id);
  const del = useDeleteScan();
  const [confirm, setConfirm] = useState(false);

  if (isPending) return <div className="mx-auto max-w-4xl space-y-4"><Skeleton className="h-48" /><Skeleton className="h-64" /></div>;
  if (error) {
    const gone = error instanceof ApiError && error.status === 404;
    return <div className="card mx-auto max-w-lg">{gone
      ? <div className="p-8 text-center"><p className="font-medium">This scan no longer exists.</p><Link to="/history" className="btn-ghost mt-3">Back to history</Link></div>
      : <ErrorState error={error} onRetry={() => void refetch()} />}</div>;
  }
  const r = data;
  const text = messageText ?? r.messageText;
  const onDelete = () => del.mutate(r.id, {
    onSuccess: () => { notify('success', 'Scan deleted.'); nav('/history'); },
    onError: (e) => { setConfirm(false); notify('error', e instanceof ApiError ? e.message : 'Could not delete the scan.'); },
  });

  return (
    <div className="mx-auto max-w-4xl space-y-6">
      <Card>
        <div className="flex flex-wrap items-center gap-6">
          <ScoreRing score={r.score} classification={r.classification} />
          <div className="min-w-0 flex-1">
            <RiskBadge value={r.classification} />
            <h2 className="mt-2 break-words text-xl font-bold">{r.inputDisplay}</h2>
            <p className="mt-2 text-sm text-muted">{explain(r)}</p>
            <p className="mt-3 flex flex-wrap gap-x-5 gap-y-1 text-xs text-muted">
              <span className="flex items-center gap-1"><Clock className="h-3 w-3" aria-hidden />{fullDate(r.createdAt)}</span>
              <span className="flex items-center gap-1"><Hash className="h-3 w-3" aria-hidden /><span className="font-mono">{r.id}</span></span>
              <span>{r.type === 'URL' ? 'URL scan' : 'Message scan'}</span>
            </p>
          </div>
        </div>
      </Card>

      {r.type === 'MESSAGE' && (
        <Card title="Message">
          {text ? <HighlightedMessage text={text} findings={r.findings} />
            : <p className="text-sm text-muted">The message text is not stored, so phrases cannot be highlighted for saved scans. Findings below include the matched wording.</p>}
        </Card>
      )}

      <Card title="Findings">
        {r.findings.length === 0 ? <p className="text-sm text-muted">No detection rules fired.</p>
          : <ul className="space-y-2">{[...r.findings].sort((a, b) => b.scoreContribution - a.scoreContribution).map((f) => <FindingRow key={f.ruleId} f={f} />)}</ul>}
      </Card>

      {r.extractedUrls.length > 0 && (
        <Card title="Links found in the message">
          <ul className="space-y-1 text-sm">{r.extractedUrls.map((u) => <li key={u}><code className="break-all rounded bg-surface px-1.5 py-0.5 text-xs">{u}</code></li>)}</ul>
          <p className="mt-2 text-xs text-muted">Shown as plain text on purpose. Do not open these.</p>
        </Card>
      )}

      <Card title="Reputation checks">
        {r.providers.length === 0 ? <p className="text-sm text-muted">No threat-intelligence providers are configured, so no reputation check was made. This result comes from local rules only.</p>
          : <ul className="divide-y divide-line">{r.providers.map((p) => { const m = PROVIDER_META[p.status]; const Icon = m.icon; return (
              <li key={p.provider} className="flex items-center gap-3 py-2 text-sm"><Icon className={`h-4 w-4 ${m.cls}`} aria-hidden /><span className="flex-1 font-medium">{p.provider}</span><span className={m.cls}>{m.label}</span>{p.detail && <span className="text-xs text-muted">{p.detail}</span>}</li>); })}</ul>}
      </Card>

      <Card title="Recommended actions">
        <ul className="list-disc space-y-1.5 pl-5 text-sm">{recommendations(r).map((x) => <li key={x}>{x}</li>)}</ul>
      </Card>

      <div className="flex flex-wrap gap-3">
        <Link to={r.type === 'URL' ? '/url-scanner' : '/message-scanner'} className="btn-primary">Start another scan</Link>
        <Link to="/history" className="btn-ghost">View scan history</Link>
        <button className="btn-ghost ml-auto text-danger" onClick={() => setConfirm(true)}><Trash2 className="h-4 w-4" aria-hidden /> Delete scan</button>
      </div>

      {confirm && (
        <Dialog title="Delete this scan?" onClose={() => setConfirm(false)}>
          <p className="text-sm text-muted">The scan and its findings will be removed permanently.</p>
          <div className="mt-5 flex justify-end gap-2">
            <button className="btn-ghost" onClick={() => setConfirm(false)}>Cancel</button>
            <button className="btn-danger" onClick={onDelete} disabled={del.isPending}>Delete</button>
          </div>
        </Dialog>
      )}
    </div>
  );
}
