import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from './api';
import type { HistoryParams, Range } from './types';

export const useStats = (r: Range) => useQuery({ queryKey: ['dashboard', 'stats', r], queryFn: () => api.stats(r) });
export const useDistribution = (r: Range) => useQuery({ queryKey: ['dashboard', 'distribution', r], queryFn: () => api.distribution(r) });
export const useActivity = (r: Range) => useQuery({ queryKey: ['dashboard', 'activity', r], queryFn: () => api.activity(r) });
export const useIndicators = (r: Range) => useQuery({ queryKey: ['dashboard', 'indicators', r], queryFn: () => api.indicators(r) });
export const useScans = (p: HistoryParams) =>
  useQuery({ queryKey: ['scans', p], queryFn: () => api.listScans(p), placeholderData: keepPreviousData });
export const useScan = (id: string) => useQuery({ queryKey: ['scan', id], queryFn: () => api.getScan(id), retry: false });
export const useThreatIntel = () => useQuery({ queryKey: ['threat-intel'], queryFn: api.threatIntel });

export function useScanMutation(kind: 'URL' | 'MESSAGE') {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: string) => (kind === 'URL' ? api.scanUrl(input) : api.scanMessage(input)),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['dashboard'] });
      void qc.invalidateQueries({ queryKey: ['scans'] });
    },
  });
}

export function useDeleteScan() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.deleteScan(id),
    onSuccess: () => {
      void qc.invalidateQueries({ queryKey: ['dashboard'] });
      void qc.invalidateQueries({ queryKey: ['scans'] });
    },
  });
}
