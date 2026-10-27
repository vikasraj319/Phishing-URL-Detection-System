import { ScanForm } from '../components/ScanForm';
import { Card } from '../components/ui';
import { Link2, Mail, Info } from 'lucide-react';

export function UrlScannerPage() {
  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[1fr_320px]">
      <Card title="Analyze a link">
        <div className="mb-4 flex items-center gap-2 text-sm text-muted"><Link2 className="h-4 w-4" aria-hidden /> Check a suspicious URL before you open it.</div>
        <ScanForm kind="URL" large />
      </Card>
      <Card title="What gets checked">
        <ul className="space-y-2 text-sm text-muted">
          <li>Host structure: IP addresses, look-alike characters, nested subdomains</li>
          <li>Brand names on domains that do not belong to the brand</li>
          <li>Link shorteners, unusual ports and embedded redirects</li>
          <li>Reputation providers, when configured on the server</li>
        </ul>
        <p className="mt-4 flex gap-2 rounded-lg bg-surface p-3 text-xs text-muted"><Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />A safe-looking result means no strong indicators were found. It does not prove a site is safe.</p>
      </Card>
    </div>
  );
}

export function MessageScannerPage() {
  return (
    <div className="mx-auto grid max-w-5xl gap-6 lg:grid-cols-[1fr_320px]">
      <Card title="Analyze a message">
        <div className="mb-4 flex items-center gap-2 text-sm text-muted"><Mail className="h-4 w-4" aria-hidden /> Paste an email, SMS or chat message.</div>
        <ScanForm kind="MESSAGE" large />
      </Card>
      <Card title="What gets checked">
        <ul className="space-y-2 text-sm text-muted">
          <li>Urgency, threats and pressure to act</li>
          <li>Requests for passwords, card details or one-time codes</li>
          <li>Prize claims and unusual payment demands</li>
          <li>Every link in the message, scored on its own</li>
        </ul>
        <p className="mt-4 flex gap-2 rounded-lg bg-surface p-3 text-xs text-muted"><Info className="mt-0.5 h-4 w-4 shrink-0" aria-hidden />Urgent wording alone is not treated as phishing. Real reminders often sound urgent.</p>
      </Card>
    </div>
  );
}
