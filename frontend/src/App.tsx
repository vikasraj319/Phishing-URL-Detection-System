import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { ThemeProvider } from './lib/theme';
import { ToastProvider } from './components/Toast';
import { AppShell } from './components/AppShell';
import { OverviewPage } from './pages/Overview';
import { MessageScannerPage, UrlScannerPage } from './pages/Scanners';
import { ScanDetailsPage } from './pages/ScanDetails';
import { HistoryPage } from './pages/History';
import { SettingsPage, ThreatIntelPage } from './pages/Other';
import { Link } from 'react-router-dom';

export const makeClient = (retry: number | false = 1) => new QueryClient({ defaultOptions: { queries: { retry, refetchOnWindowFocus: false, staleTime: 15_000 } } });

function NotFound() {
  return <div className="card mx-auto max-w-md p-8 text-center"><p className="font-medium">Page not found</p><Link to="/" className="btn-primary mt-4">Back to overview</Link></div>;
}

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<OverviewPage />} />
        <Route path="url-scanner" element={<UrlScannerPage />} />
        <Route path="message-scanner" element={<MessageScannerPage />} />
        <Route path="scans/:id" element={<ScanDetailsPage />} />
        <Route path="history" element={<HistoryPage />} />
        <Route path="threat-intelligence" element={<ThreatIntelPage />} />
        <Route path="settings" element={<SettingsPage />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  );
}

export default function App() {
  return (
    <QueryClientProvider client={makeClient()}>
      <ThemeProvider><ToastProvider><BrowserRouter><AppRoutes /></BrowserRouter></ToastProvider></ThemeProvider>
    </QueryClientProvider>
  );
}
