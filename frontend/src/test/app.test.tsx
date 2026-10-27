import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClientProvider } from '@tanstack/react-query';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { AppRoutes, makeClient } from '../App';
import { ThemeProvider } from '../lib/theme';
import { ToastProvider } from '../components/Toast';
import { validate } from '../components/ScanForm';
import { HighlightedMessage, recommendations } from '../pages/ScanDetails';
import { relativeTime, trend } from '../lib/format';
import type { ScanResult } from '../lib/types';

type Handler = (url: URL, init?: RequestInit) => { status?: number; body?: unknown };
let handlers: Record<string, Handler> = {};
const calls: { url: URL; init?: RequestInit }[] = [];

const result = (over: Partial<ScanResult> = {}): ScanResult => ({
  id: 'abc-123', type: 'URL', inputDisplay: 'paypal-security-login.xyz', score: 66, classification: 'HIGH_RISK',
  findings: [{ ruleId: 'brand_impersonation', title: 'Brand name on an unrelated domain', severity: 'HIGH', explanation: 'Why.', evidence: 'Brand: paypal', scoreContribution: 50 }],
  providers: [], extractedUrls: [], createdAt: '2026-10-24T10:00:00Z', ...over,
});
const emptyDashboard: Record<string, Handler> = {
  '/api/v1/dashboard/stats': () => ({ body: { totalScans: 0, highRisk: 0, suspicious: 0, safeLooking: 0, flaggedRate: null, previous: null } }),
  '/api/v1/dashboard/risk-distribution': () => ({ body: { total: 0, safeLooking: 0, suspicious: 0, highRisk: 0 } }),
  '/api/v1/dashboard/scan-activity': () => ({ body: [{ date: '2026-10-23', count: 0 }] }),
  '/api/v1/dashboard/top-indicators': () => ({ body: [] }),
  '/api/v1/scans': () => ({ body: { items: [], page: 0, size: 5, totalItems: 0, totalPages: 0 } }),
};

beforeEach(() => {
  calls.length = 0; handlers = { ...emptyDashboard };
  vi.stubGlobal('fetch', vi.fn(async (input: string, init?: RequestInit) => {
    const url = new URL(input);
    calls.push({ url, init });
    const h = handlers[url.pathname];
    const r = h ? h(url, init) : { status: 404, body: { status: 404, code: 'NOT_FOUND', message: 'Not found' } };
    const status = r.status ?? 200;
    return { ok: status < 400, status, json: async () => r.body } as Response;
  }));
  localStorage.clear();
});
afterEach(() => vi.unstubAllGlobals());

function renderAt(path: string) {
  return render(
    <QueryClientProvider client={makeClient(false)}><ThemeProvider><ToastProvider>
      <MemoryRouter initialEntries={[path]}><AppRoutes /></MemoryRouter>
    </ToastProvider></ThemeProvider></QueryClientProvider>);
}

describe('helpers', () => {
  it('validates URL and message input', () => {
    expect(validate('URL', '')).toMatch(/Enter a URL/);
    expect(validate('URL', 'exa mple.com')).toMatch(/spaces/);
    expect(validate('URL', 'javascript:alert(1)')).toMatch(/http/);
    expect(validate('URL', 'https://example.com')).toBeNull();
    expect(validate('MESSAGE', 'x'.repeat(10001))).toMatch(/10,000/);
    expect(validate('MESSAGE', 'hello')).toBeNull();
  });
  it('only reports a trend when a baseline exists', () => {
    expect(trend(10, null)).toBeNull();
    expect(trend(10, 0)).toBeNull();
    expect(trend(12, 10)).toBe(20);
  });
  it('formats relative time', () => {
    const now = Date.parse('2026-10-24T12:00:00Z');
    expect(relativeTime('2026-10-24T11:58:00Z', now)).toBe('2 minutes ago');
  });
  it('gives contextual recommendations', () => {
    const recs = recommendations(result({ findings: [{ ...result().findings[0], ruleId: 'otp_request' }] }));
    expect(recs.join(' ')).toMatch(/one-time codes/);
    expect(recommendations(result({ classification: 'SAFE_LOOKING', findings: [] })).length).toBe(1);
  });
  it('highlights flagged phrases with a non-color marker', () => {
    const text = 'Verify your password now';
    const { container } = render(<HighlightedMessage text={text} findings={[{ ...result().findings[0], spans: [{ start: 0, end: 20 }, { start: 10, end: 20 }] }]} />);
    const marks = container.querySelectorAll('mark');
    expect(marks).toHaveLength(1);
    expect(marks[0].textContent).toContain('Verify your password');
    expect(within(marks[0] as HTMLElement).getByText(/flagged phrase/)).toBeInTheDocument();
  });
});

describe('overview', () => {
  it('shows honest empty states, no invented numbers', async () => {
    renderAt('/');
    expect(await screen.findByText('No scans in this period')).toBeInTheDocument();
    expect(screen.getAllByText('No scans yet').length).toBeGreaterThan(0);
    expect(screen.getByText('—')).toBeInTheDocument(); // detection rate unavailable
  });
  it('renders backend counts and only shows a trend when a previous period exists', async () => {
    handlers['/api/v1/dashboard/stats'] = () => ({ body: { totalScans: 10, highRisk: 2, suspicious: 3, safeLooking: 5, flaggedRate: 0.5, previous: { total: 8, highRisk: 0, suspicious: 3 } } });
    handlers['/api/v1/dashboard/risk-distribution'] = () => ({ body: { total: 10, safeLooking: 5, suspicious: 3, highRisk: 2 } });
    renderAt('/');
    expect(await screen.findByText('50.0%')).toBeInTheDocument();
    expect(screen.getByText('25% more than the previous 7 days')).toBeInTheDocument();
    expect(screen.getByText('50.0%', { selector: 'span' })).toBeTruthy();
    await screen.findByText('Safe-looking');
    expect(screen.getAllByText('30.0%').length).toBe(1); // suspicious share from the same response
  });
  it('shows an error state with retry when the API is down', async () => {
    handlers['/api/v1/dashboard/stats'] = () => ({ status: 500, body: { status: 500, code: 'ERR', message: 'Server exploded' } });
    renderAt('/');
    expect((await screen.findAllByText('Server exploded')).length).toBeGreaterThan(0);
    expect(screen.getAllByRole('button', { name: /retry/i }).length).toBeGreaterThan(0);
  });
});

describe('quick scan', () => {
  it('blocks empty input, switches tabs, posts to the Java endpoint and opens the result', async () => {
    handlers['/api/v1/scan/url'] = () => ({ body: result() });
    handlers['/api/v1/scans/abc-123'] = () => ({ body: result() });
    const user = userEvent.setup();
    renderAt('/');
    const panel = (await screen.findByText('Quick Scan')).closest('section') as HTMLElement;
    await user.click(within(panel).getByRole('button', { name: 'Analyze URL' }));
    expect(await within(panel).findByRole('alert')).toHaveTextContent('Enter a URL to analyze.');
    expect(calls.some((c) => c.url.pathname === '/api/v1/scan/url')).toBe(false);

    await user.click(within(panel).getByRole('tab', { name: 'Message Scanner' }));
    expect(within(panel).getByRole('button', { name: 'Analyze Message' })).toBeInTheDocument();
    await user.click(within(panel).getByRole('tab', { name: 'URL Scanner' }));

    await user.type(within(panel).getByLabelText('Link to analyze'), ' https://paypal-security-login.xyz/verify ');
    await user.click(within(panel).getByRole('button', { name: 'Analyze URL' }));
    await screen.findByRole('heading', { name: 'paypal-security-login.xyz' });
    const post = calls.find((c) => c.url.pathname === '/api/v1/scan/url')!;
    expect(post.init?.method).toBe('POST');
    expect(JSON.parse(post.init!.body as string)).toEqual({ url: 'https://paypal-security-login.xyz/verify' });
    expect(screen.getByRole('img', { name: /Risk score 66 out of 100, High risk/ })).toBeInTheDocument();
    expect(screen.getByText(/Brand name on an unrelated domain/)).toBeInTheDocument();
    expect(screen.getByText(/No threat-intelligence providers are configured/)).toBeInTheDocument();
  });
  it('shows the server error honestly when a scan fails', async () => {
    handlers['/api/v1/scan/url'] = () => ({ status: 400, body: { status: 400, code: 'INVALID', message: 'Host name is not valid.' } });
    const user = userEvent.setup();
    renderAt('/url-scanner');
    await user.type(await screen.findByLabelText('Link to analyze'), 'https://bad_host');
    await user.click(screen.getByRole('button', { name: 'Analyze URL' }));
    expect((await screen.findAllByText('Host name is not valid.')).length).toBeGreaterThan(0);
  });
});

describe('history', () => {
  const item = { id: 's1', type: 'URL', inputDisplay: 'bit.ly/3Xk9a7', score: 34, classification: 'SUSPICIOUS', createdAt: '2026-10-24T10:00:00Z' };
  it('filters through the API, paginates and deletes after confirmation', async () => {
    let items = [item];
    handlers['/api/v1/scans'] = (url) => ({ body: { items, page: Number(url.searchParams.get('page') ?? 0), size: 10, totalItems: items.length, totalPages: items.length ? 1 : 0 } });
    handlers['/api/v1/scans/s1'] = (_u, init) => { if (init?.method === 'DELETE') { items = []; return { status: 204, body: undefined }; } return { body: result() }; };
    const user = userEvent.setup();
    renderAt('/history');
    expect(await screen.findByText('bit.ly/3Xk9a7')).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Classification'), 'HIGH_RISK');
    await waitFor(() => expect(calls.some((c) => c.url.pathname === '/api/v1/scans' && c.url.searchParams.get('classification') === 'HIGH_RISK')).toBe(true));
    await user.click(screen.getByRole('button', { name: 'Risk score' }));
    await waitFor(() => expect(calls.some((c) => c.url.searchParams.get('sort') === 'score')).toBe(true));
    await user.click(await screen.findByRole('button', { name: /Delete scan of bit.ly/ }));
    const dlg = screen.getByRole('dialog', { name: 'Delete this scan?' });
    await user.click(within(dlg).getByRole('button', { name: 'Delete' }));
    await waitFor(() => expect(calls.some((c) => c.init?.method === 'DELETE')).toBe(true));
    expect(await screen.findByText('No scans match these filters')).toBeInTheDocument();
  });
});

describe('shell', () => {
  it('toggles and persists the theme, and global search routes to history', async () => {
    handlers['/api/v1/scans'] = () => ({ body: { items: [], page: 0, size: 10, totalItems: 0, totalPages: 0 } });
    const user = userEvent.setup();
    renderAt('/settings');
    await user.click(await screen.findByRole('button', { name: 'Switch to light theme' }));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    expect(localStorage.getItem('pg-theme')).toBe('light');
    await user.type(screen.getByLabelText('Search scans'), 'paypal{enter}');
    expect(await screen.findByLabelText('Search history')).toHaveValue('paypal');
    await waitFor(() => expect(calls.some((c) => c.url.searchParams.get('q') === 'paypal')).toBe(true));
  });
  it('lists every navigation destination', async () => {
    renderAt('/threat-intelligence');
    handlers['/api/v1/threat-intelligence/status'] = () => ({ body: { providers: [] } });
    const nav = screen.getAllByRole('navigation', { name: 'Main' })[0];
    ['Overview', 'URL Scanner', 'Message Scanner', 'Scan History', 'Threat Intelligence', 'Settings'].forEach((n) => expect(within(nav).getByRole('link', { name: n })).toBeInTheDocument());
  });
});
