import { useState } from 'react';
import { Link } from 'react-router-dom';
import { CircleCheck, CircleHelp, Plug, Moon, Sun, Activity } from 'lucide-react';
import { Card, ErrorState, Skeleton } from '../components/ui';
import { useThreatIntel } from '../lib/queries';
import { useTheme } from '../lib/theme';
import { API_BASE, api, ApiError } from '../lib/api';
import { fullDate } from '../lib/format';
import type { ProviderState } from '../lib/types';

const META: Record<ProviderState, { label: string; icon: typeof CircleCheck; cls: string }> = {
  CLEAN: { label: 'Connected', icon: CircleCheck, cls: 'text-safe' },
  FLAGGED: { label: 'Connected', icon: CircleCheck, cls: 'text-safe' },
  UNKNOWN: { label: 'Status unknown', icon: CircleHelp, cls: 'text-muted' },
  UNAVAILABLE: { label: 'Unavailable', icon: CircleHelp, cls: 'text-warn' },
  NOT_CONFIGURED: { label: 'Not configured', icon: Plug, cls: 'text-muted' },
};

export function ThreatIntelPage() {
  const { data, isPending, error, refetch } = useThreatIntel();
  return (
    <div className="mx-auto max-w-4xl space-y-4">
      <p className="text-sm text-muted">Reputation providers are optional and configured on the server with environment variables. Local rule-based detection works without them. A failed or unavailable check is never treated as a clean result.</p>
      {error ? <div className="card"><ErrorState error={error} onRetry={() => void refetch()} /></div>
        : isPending ? <Skeleton className="h-40" />
        : data.providers.length === 0 ? <Card><p className="text-sm text-muted">The server reports no providers.</p></Card>
        : data.providers.map((p) => { const m = META[p.status]; const Icon = m.icon; return (
          <Card key={p.name}>
            <div className="flex flex-wrap items-center gap-3">
              <Icon className={`h-5 w-5 ${m.cls}`} aria-hidden />
              <h2 className="flex-1 text-base font-semibold">{p.name}</h2>
              <span className={`text-sm font-medium ${m.cls}`}>{m.label}</span>
            </div>
            <dl className="mt-3 grid gap-x-6 gap-y-1 text-sm sm:grid-cols-2">
              <div><dt className="inline text-muted">Configured: </dt><dd className="inline">{p.configured ? 'Yes' : 'No'}</dd></div>
              <div><dt className="inline text-muted">Last successful check: </dt><dd className="inline">{p.lastSuccessAt ? fullDate(p.lastSuccessAt) : 'None yet'}</dd></div>
            </dl>
            {p.detail && <p className="mt-2 text-sm text-muted">{p.detail}</p>}
          </Card>); })}
    </div>
  );
}

export function SettingsPage() {
  const { theme, setTheme } = useTheme();
  const [health, setHealth] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const check = async () => {
    setBusy(true);
    try { const h = await api.health(); setHealth(`Backend reachable (${h.status}).`); }
    catch (e) { setHealth(e instanceof ApiError ? e.message : 'Backend unreachable.'); }
    finally { setBusy(false); }
  };
  return (
    <div className="mx-auto max-w-3xl space-y-4">
      <Card title="Appearance">
        <div role="radiogroup" aria-label="Theme" className="flex gap-3">
          {(['dark', 'light'] as const).map((t) => (
            <button key={t} role="radio" aria-checked={theme === t} onClick={() => setTheme(t)}
              className={`btn flex-1 border ${theme === t ? 'border-primary bg-primary/15' : 'border-line bg-surface'}`}>
              {t === 'dark' ? <Moon className="h-4 w-4" aria-hidden /> : <Sun className="h-4 w-4" aria-hidden />}{t === 'dark' ? 'Dark' : 'Light'}
            </button>))}
        </div>
        <p className="mt-2 text-xs text-muted">Your choice is remembered in this browser.</p>
      </Card>
      <Card title="Connection">
        <p className="text-sm text-muted">API address: <code className="rounded bg-surface px-1.5 py-0.5 text-xs">{API_BASE}</code> (set with VITE_API_BASE_URL)</p>
        <button className="btn-ghost mt-3" onClick={check} disabled={busy}><Activity className="h-4 w-4" aria-hidden /> Test connection</button>
        {health && <p role="status" className="mt-2 text-sm">{health}</p>}
      </Card>
      <Card title="Privacy and integrations">
        <p className="text-sm text-muted">Message storage, retention and reputation providers are configured on the server. See <Link to="/threat-intelligence" className="text-primary hover:underline">Threat Intelligence</Link> for which providers are active. You can delete any scan from its details page or from Scan History.</p>
      </Card>
    </div>
  );
}
