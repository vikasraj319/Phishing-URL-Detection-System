import { useEffect, useState, type FormEvent } from 'react';
import { NavLink, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { Home, Link2, Mail, History, ShieldCheck, Settings, ChevronsLeft, ChevronsRight, Menu, Search, Sun, Moon, X, Shield } from 'lucide-react';
import { useTheme } from '../lib/theme';

const NAV = [
  { to: '/', label: 'Overview', icon: Home, end: true },
  { to: '/url-scanner', label: 'URL Scanner', icon: Link2 },
  { to: '/message-scanner', label: 'Message Scanner', icon: Mail },
  { to: '/history', label: 'Scan History', icon: History },
  { to: '/threat-intelligence', label: 'Threat Intelligence', icon: ShieldCheck },
  { to: '/settings', label: 'Settings', icon: Settings },
];
const TITLES: Record<string, string> = {
  '/': 'Overview', '/url-scanner': 'URL Scanner', '/message-scanner': 'Message Scanner', '/history': 'Scan History',
  '/threat-intelligence': 'Threat Intelligence', '/settings': 'Settings',
};

function Logo({ compact }: { compact: boolean }) {
  return (
    <div className="flex items-center gap-3 px-2">
      <Shield className="h-8 w-8 shrink-0 text-primary" aria-hidden />
      {!compact && (
        <div className="leading-tight">
          <div className="text-lg font-bold">PhishGuard <span className="text-primary">AI</span></div>
          <div className="text-[11px] text-muted">Detect · Analyze · Stay Safe</div>
        </div>
      )}
    </div>
  );
}

function SideNav({ compact, onNavigate }: { compact: boolean; onNavigate?: () => void }) {
  return (
    <nav aria-label="Main" className="mt-6 flex flex-col gap-1">
      {NAV.map(({ to, label, icon: Icon, end }) => (
        <NavLink key={to} to={to} end={end} onClick={onNavigate} title={compact ? label : undefined}
          className={({ isActive }) => `flex items-center gap-3 rounded-lg px-3 py-2.5 text-sm font-medium transition-colors ${isActive ? 'bg-primary/20 text-fg ring-1 ring-primary/50' : 'text-muted hover:bg-surface hover:text-fg'} ${compact ? 'justify-center' : ''}`}>
          <Icon className="h-5 w-5 shrink-0" aria-hidden />
          {compact ? <span className="sr-only">{label}</span> : label}
        </NavLink>
      ))}
    </nav>
  );
}

function PromoCard() {
  return (
    <div className="card bg-surface p-4">
      <ShieldCheck className="mb-2 h-8 w-8 text-safe" aria-hidden />
      <h3 className="text-sm font-semibold">Smarter Protection with AI</h3>
      <p className="mt-1 text-xs text-muted">Explainable detection rules with optional reputation checks. Every result shows why it was scored.</p>
    </div>
  );
}

export function AppShell() {
  const { theme, toggle } = useTheme();
  const navigate = useNavigate();
  const { pathname } = useLocation();
  const [collapsed, setCollapsed] = useState(() => { try { return localStorage.getItem('pg-collapsed') === '1'; } catch { return false; } });
  const [drawer, setDrawer] = useState(false);
  const [q, setQ] = useState('');
  useEffect(() => { try { localStorage.setItem('pg-collapsed', collapsed ? '1' : '0'); } catch { /* ignore */ } }, [collapsed]);
  useEffect(() => { setDrawer(false); }, [pathname]);

  const title = TITLES[pathname] ?? (pathname.startsWith('/scans/') ? 'Scan details' : 'PhishGuard AI');
  const onSearch = (e: FormEvent) => {
    e.preventDefault();
    const v = q.trim();
    if (v) navigate(`/history?q=${encodeURIComponent(v)}`);
  };

  return (
    <div className="flex min-h-full">
      <aside className={`sticky top-0 hidden h-screen shrink-0 flex-col border-r border-line bg-sidebar p-4 transition-[width] duration-200 lg:flex ${collapsed ? 'w-[76px]' : 'w-[252px]'}`}>
        <Logo compact={collapsed} />
        <SideNav compact={collapsed} />
        <div className="mt-auto flex flex-col gap-3">
          {!collapsed && <PromoCard />}
          <button className="btn-ghost justify-start" onClick={() => setCollapsed((c) => !c)} aria-label={collapsed ? 'Expand sidebar' : 'Collapse sidebar'}>
            {collapsed ? <ChevronsRight className="h-4 w-4" aria-hidden /> : <><ChevronsLeft className="h-4 w-4" aria-hidden /> Collapse Sidebar</>}
          </button>
        </div>
      </aside>

      {drawer && (
        <div className="fixed inset-0 z-40 lg:hidden" onKeyDown={(e) => e.key === 'Escape' && setDrawer(false)}>
          <div className="absolute inset-0 bg-black/60" onClick={() => setDrawer(false)} aria-hidden />
          <aside role="dialog" aria-modal="true" aria-label="Navigation" className="absolute left-0 top-0 flex h-full w-72 max-w-[85vw] flex-col bg-sidebar p-4">
            <div className="flex items-center justify-between"><Logo compact={false} />
              <button className="btn-ghost px-2" onClick={() => setDrawer(false)} aria-label="Close navigation"><X className="h-4 w-4" aria-hidden /></button>
            </div>
            <SideNav compact={false} onNavigate={() => setDrawer(false)} />
            <div className="mt-auto"><PromoCard /></div>
          </aside>
        </div>
      )}

      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-30 flex items-center gap-3 border-b border-line bg-bg/90 px-4 py-3 backdrop-blur sm:px-6">
          <button className="btn-ghost px-2 lg:hidden" onClick={() => setDrawer(true)} aria-label="Open navigation"><Menu className="h-5 w-5" aria-hidden /></button>
          <h1 className="hidden text-sm font-semibold sm:block lg:hidden">{title}</h1>
          <form onSubmit={onSearch} role="search" className="relative max-w-xl flex-1">
            <label htmlFor="global-search" className="sr-only">Search scans</label>
            <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted" aria-hidden />
            <input id="global-search" className="field pl-9" placeholder="Search scans by URL, domain or text…" value={q} onChange={(e) => setQ(e.target.value)} maxLength={200} />
          </form>
          <button className="btn-ghost ml-auto px-2.5" onClick={toggle} aria-label={theme === 'dark' ? 'Switch to light theme' : 'Switch to dark theme'}>
            {theme === 'dark' ? <Sun className="h-4 w-4" aria-hidden /> : <Moon className="h-4 w-4" aria-hidden />}
          </button>
        </header>
        <main className="min-w-0 flex-1 p-4 sm:p-6"><Outlet /></main>
        <footer className="flex items-center justify-between border-t border-line px-6 py-3 text-xs text-muted">
          <span>PhishGuard AI · v1.0.0</span>
          <span>Heuristic risk scores are not guarantees of safety.</span>
        </footer>
      </div>
    </div>
  );
}
