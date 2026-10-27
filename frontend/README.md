# PhishGuard AI frontend

React + Vite + TypeScript + Tailwind. Talks only to the Java API; there is no bundled sample data.

```
copy .env.example .env      # VITE_API_BASE_URL=http://localhost:8080
npm install
npm run dev                 # http://localhost:5173
npm run typecheck && npm test && npm run build
```

## API contract the backend must implement
Types live in `src/lib/types.ts`; the client is `src/lib/api.ts`.

| Endpoint | Notes |
|---|---|
| `POST /api/v1/scan/url` `{url}` | returns `ScanResult` |
| `POST /api/v1/scan/message` `{message}` | returns `ScanResult`; may include transient `messageText` |
| `GET /api/v1/scans` | `q,type,classification,from,to,sort,dir,page,size` -> `Page<ScanListItem>` |
| `GET/DELETE /api/v1/scans/{id}` | `ScanResult` / 204 |
| `GET /api/v1/dashboard/stats?range=7d\|30d` | `flaggedRate` = (suspicious+highRisk)/total or null; `previous` = prior period counts or null |
| `GET /api/v1/dashboard/risk-distribution` `scan-activity` `top-indicators` | same `range` param |
| `GET /api/v1/threat-intelligence/status` | provider list; unavailable is never "clean" |
| `GET /api/v1/health` | `{status}` |

Errors: `{status, code, message, fieldErrors?}`. CORS must allow the frontend origin.
