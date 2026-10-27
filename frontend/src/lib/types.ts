export type Classification = 'SAFE_LOOKING' | 'SUSPICIOUS' | 'HIGH_RISK';
export type ScanType = 'URL' | 'MESSAGE';
export type Severity = 'INFO' | 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
export type Range = '7d' | '30d';

export interface Span { start: number; end: number }
export interface Finding {
  ruleId: string; title: string; severity: Severity; explanation: string;
  evidence: string | null; scoreContribution: number; spans?: Span[];
}
export type ProviderState = 'CLEAN' | 'FLAGGED' | 'UNKNOWN' | 'UNAVAILABLE' | 'NOT_CONFIGURED';
export interface ProviderResult { provider: string; status: ProviderState; detail?: string | null }

export interface ScanResult {
  id: string; type: ScanType; inputDisplay: string; score: number; classification: Classification;
  findings: Finding[]; providers: ProviderResult[]; extractedUrls: string[]; createdAt: string;
  /** Present only in the POST response of a message scan; never persisted by default. */
  messageText?: string;
}
export interface ScanListItem {
  id: string; type: ScanType; inputDisplay: string; score: number; classification: Classification; createdAt: string;
}
export interface Page<T> { items: T[]; page: number; size: number; totalItems: number; totalPages: number }

export interface PeriodCounts { total: number; highRisk: number; suspicious: number }
export interface DashboardStats {
  totalScans: number; highRisk: number; suspicious: number; safeLooking: number;
  /** (suspicious + highRisk) / total, 0..1, or null when there are no scans. Not an accuracy metric. */
  flaggedRate: number | null;
  /** Counts for the preceding period of equal length, or null when unavailable. */
  previous: PeriodCounts | null;
}
export interface RiskDistribution { total: number; safeLooking: number; suspicious: number; highRisk: number }
export interface ActivityPoint { date: string; count: number }
export interface IndicatorCount { ruleId: string; title: string; count: number }
export interface ProviderStatus {
  name: string; configured: boolean; status: ProviderState; lastSuccessAt: string | null; detail: string | null;
}
export interface ThreatIntelStatus { providers: ProviderStatus[] }
export interface ApiErrorBody { status: number; code: string; message: string; fieldErrors?: Record<string, string> }

export interface HistoryParams {
  q?: string; type?: ScanType | ''; classification?: Classification | ''; from?: string; to?: string;
  sort?: 'createdAt' | 'score' | 'type' | 'classification'; dir?: 'asc' | 'desc'; page?: number; size?: number;
}
