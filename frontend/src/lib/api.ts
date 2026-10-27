import type {
  ActivityPoint, ApiErrorBody, DashboardStats, HistoryParams, IndicatorCount, Page, Range,
  RiskDistribution, ScanListItem, ScanResult, ThreatIntelStatus,
} from './types';

export const API_BASE: string = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080';

export class ApiError extends Error {
  constructor(public status: number, public code: string, message: string, public fieldErrors?: Record<string, string>) {
    super(message);
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    res = await fetch(`${API_BASE}${path}`, {
      ...init,
      headers: { 'Content-Type': 'application/json', Accept: 'application/json', ...(init?.headers ?? {}) },
    });
  } catch {
    throw new ApiError(0, 'NETWORK', 'Cannot reach the PhishGuard API. Check that the backend is running.');
  }
  if (!res.ok) {
    let body: Partial<ApiErrorBody> = {};
    try { body = (await res.json()) as Partial<ApiErrorBody>; } catch { /* non-JSON error */ }
    throw new ApiError(res.status, body.code ?? 'ERROR', body.message ?? `Request failed (${res.status}).`, body.fieldErrors);
  }
  if (res.status === 204) return undefined as T;
  return (await res.json()) as T;
}

const qs = (o: Record<string, string | number | undefined | null>) => {
  const p = new URLSearchParams();
  Object.entries(o).forEach(([k, v]) => { if (v !== undefined && v !== null && v !== '') p.set(k, String(v)); });
  const s = p.toString();
  return s ? `?${s}` : '';
};

export const api = {
  scanUrl: (url: string) => request<ScanResult>('/api/v1/scan/url', { method: 'POST', body: JSON.stringify({ url }) }),
  scanMessage: (message: string) =>
    request<ScanResult>('/api/v1/scan/message', { method: 'POST', body: JSON.stringify({ message }) }),
  listScans: (p: HistoryParams) => request<Page<ScanListItem>>(`/api/v1/scans${qs({ ...p })}`),
  getScan: (id: string) => request<ScanResult>(`/api/v1/scans/${encodeURIComponent(id)}`),
  deleteScan: (id: string) => request<void>(`/api/v1/scans/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  stats: (range: Range) => request<DashboardStats>(`/api/v1/dashboard/stats${qs({ range })}`),
  distribution: (range: Range) => request<RiskDistribution>(`/api/v1/dashboard/risk-distribution${qs({ range })}`),
  activity: (range: Range) => request<ActivityPoint[]>(`/api/v1/dashboard/scan-activity${qs({ range })}`),
  indicators: (range: Range) => request<IndicatorCount[]>(`/api/v1/dashboard/top-indicators${qs({ range })}`),
  threatIntel: () => request<ThreatIntelStatus>('/api/v1/threat-intelligence/status'),
  health: () => request<{ status: string }>('/api/v1/health'),
};
