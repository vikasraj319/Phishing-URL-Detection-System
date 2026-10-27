import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router-dom';
import { ArrowRight, ClipboardPaste, Link2, Loader2, Lock, X } from 'lucide-react';
import { useScanMutation } from '../lib/queries';
import { ApiError } from '../lib/api';
import { useToast } from './Toast';
import type { ScanType } from '../lib/types';

export const MAX_URL = 2048;
export const MAX_MESSAGE = 10000;
export const EXAMPLE_URL = 'https://www.wikipedia.org';
export const EXAMPLE_MESSAGES = [
  { label: 'Phishing-style example', text: 'URGENT: Your account has been suspended. Verify your password immediately at http://secure-paypa1-login.top/verify or it will be closed within 24 hours.' },
  { label: 'Legitimate example', text: 'Hi, your appointment is confirmed for Tuesday at 10:30. Reply STOP to opt out.' },
];

export function validate(kind: ScanType, value: string): string | null {
  const v = value.trim();
  if (!v) return kind === 'URL' ? 'Enter a URL to analyze.' : 'Enter a message to analyze.';
  if (kind === 'URL') {
    if (v.length > MAX_URL) return `URL must be ${MAX_URL} characters or fewer.`;
    if (/\s/.test(v)) return 'URLs cannot contain spaces.';
    if (/^(javascript|data|vbscript|file|mailto):/i.test(v)) return 'Only http and https links can be analyzed.';
  } else if (value.length > MAX_MESSAGE) return `Message must be ${MAX_MESSAGE.toLocaleString()} characters or fewer.`;
  return null;
}

export function PrivacyNote({ kind }: { kind: ScanType }) {
  return (
    <p className="flex items-start gap-2 text-xs text-muted">
      <Lock className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />
      <span>
        {kind === 'URL'
          ? 'The link is analyzed on the PhishGuard server and is never opened. If reputation providers are configured, the URL is sent to them; see Threat Intelligence for which.'
          : 'The message is analyzed on the PhishGuard server and is not sent to external services. Only a short redacted summary and the findings are saved to history.'}
      </span>
    </p>
  );
}

export function ScanForm({ kind, large = false }: { kind: ScanType; large?: boolean }) {
  const [value, setValue] = useState('');
  const [touched, setTouched] = useState(false);
  const nav = useNavigate();
  const { notify } = useToast();
  const m = useScanMutation(kind);
  const error = touched ? validate(kind, value) : null;
  const serverError = m.error instanceof ApiError ? (m.error.fieldErrors ? Object.values(m.error.fieldErrors)[0] : m.error.message) : m.error ? 'Scan failed.' : null;

  const submit = (e: FormEvent) => {
    e.preventDefault();
    setTouched(true);
    if (validate(kind, value) || m.isPending) return;
    m.mutate(kind === 'URL' ? value.trim() : value, {
      onSuccess: (res) => nav(`/scans/${res.id}`, { state: { messageText: kind === 'MESSAGE' ? value : undefined } }),
      onError: (err) => notify('error', err instanceof ApiError ? err.message : 'Scan failed.'),
    });
  };
  const paste = async () => {
    try { setValue((await navigator.clipboard.readText()).slice(0, kind === 'URL' ? MAX_URL + 1 : MAX_MESSAGE + 1)); setTouched(true); }
    catch { notify('error', 'Clipboard access was blocked. Paste with Ctrl+V instead.'); }
  };
  const id = `scan-input-${kind}-${large ? 'l' : 's'}`;
  const label = kind === 'URL' ? 'Link to analyze' : 'Message to analyze';

  return (
    <form onSubmit={submit} noValidate className="flex flex-col gap-3">
      <label htmlFor={id} className={large ? 'text-sm font-medium' : 'sr-only'}>{label}</label>
      {kind === 'URL' ? (
        <div className="relative">
          <Link2 className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" aria-hidden />
          <input id={id} className={`field pl-9 pr-16 ${large ? 'py-3.5 text-base' : ''}`} placeholder="Enter URL (e.g. https://example.com)"
            value={value} onChange={(e) => setValue(e.target.value)} onBlur={() => setTouched(true)}
            aria-invalid={!!error} aria-describedby={`${id}-help`} autoComplete="off" spellCheck={false} inputMode="url" />
          <div className="absolute right-1.5 top-1/2 flex -translate-y-1/2 gap-0.5">
            {value && <button type="button" className="rounded p-1.5 text-muted hover:text-fg" onClick={() => setValue('')} aria-label="Clear input"><X className="h-4 w-4" aria-hidden /></button>}
            <button type="button" className="rounded p-1.5 text-muted hover:text-fg" onClick={paste} aria-label="Paste from clipboard"><ClipboardPaste className="h-4 w-4" aria-hidden /></button>
          </div>
        </div>
      ) : (
        <div>
          <textarea id={id} className={`field resize-y ${large ? 'min-h-56' : 'min-h-28'}`} placeholder="Paste a suspicious email, SMS or chat message…"
            value={value} onChange={(e) => setValue(e.target.value)} onBlur={() => setTouched(true)}
            aria-invalid={!!error} aria-describedby={`${id}-help`} />
          <div className="mt-1 flex items-center justify-between text-xs text-muted">
            <span className={value.length > MAX_MESSAGE ? 'text-danger' : ''} aria-live="polite">{value.length.toLocaleString()} / {MAX_MESSAGE.toLocaleString()} characters</span>
            <span className="flex gap-3">
              <button type="button" className="hover:text-fg" onClick={paste}>Paste</button>
              {value && <button type="button" className="hover:text-fg" onClick={() => setValue('')}>Clear</button>}
            </span>
          </div>
        </div>
      )}
      <p id={`${id}-help`} className={`text-xs ${error ? 'text-danger' : 'text-muted'}`} role={error ? 'alert' : undefined}>
        {error ?? (kind === 'URL' ? 'Paste the full link. It will not be opened.' : 'Include the whole message so wording and links can be checked.')}
      </p>
      <button type="submit" className="btn-primary w-full py-3" disabled={m.isPending}>
        {m.isPending ? <><Loader2 className="h-4 w-4 animate-spin" aria-hidden /> Analyzing…</> : <>{kind === 'URL' ? 'Analyze URL' : 'Analyze Message'} <ArrowRight className="h-4 w-4" aria-hidden /></>}
      </button>
      {serverError && <p role="alert" className="text-sm text-danger">{serverError}</p>}
      {kind === 'URL' ? (
        <div className="rounded-lg border border-line bg-surface/60 p-3 text-xs">
          <span className="text-muted">Test example (not a safety verdict): </span>
          <button type="button" className="font-mono text-cyan underline-offset-2 hover:underline" onClick={() => { setValue(EXAMPLE_URL); setTouched(true); }}>{EXAMPLE_URL}</button>
        </div>
      ) : (
        <div className="flex flex-wrap gap-2 text-xs">
          {EXAMPLE_MESSAGES.map((x) => <button key={x.label} type="button" className="btn-ghost px-2.5 py-1.5 text-xs" onClick={() => { setValue(x.text); setTouched(true); }}>{x.label}</button>)}
        </div>
      )}
      <PrivacyNote kind={kind} />
    </form>
  );
}

export function QuickScan() {
  const [tab, setTab] = useState<ScanType>('URL');
  const tabs: { k: ScanType; label: string }[] = [{ k: 'URL', label: 'URL Scanner' }, { k: 'MESSAGE', label: 'Message Scanner' }];
  return (
    <section className="card p-5" aria-labelledby="quick-scan-title">
      <h2 id="quick-scan-title" className="mb-4 text-lg font-semibold">Quick Scan</h2>
      <div role="tablist" aria-label="Scan type" className="mb-4 flex border-b border-line">
        {tabs.map((t) => (
          <button key={t.k} role="tab" id={`qs-tab-${t.k}`} aria-selected={tab === t.k} aria-controls="qs-panel" tabIndex={tab === t.k ? 0 : -1}
            onClick={() => setTab(t.k)}
            onKeyDown={(e) => { if (e.key === 'ArrowRight' || e.key === 'ArrowLeft') setTab(tab === 'URL' ? 'MESSAGE' : 'URL'); }}
            className={`-mb-px flex-1 border-b-2 px-3 py-2 text-sm font-medium ${tab === t.k ? 'border-primary text-primary' : 'border-transparent text-muted hover:text-fg'}`}>{t.label}</button>
        ))}
      </div>
      <div role="tabpanel" id="qs-panel" aria-labelledby={`qs-tab-${tab}`}><ScanForm key={tab} kind={tab} /></div>
    </section>
  );
}
