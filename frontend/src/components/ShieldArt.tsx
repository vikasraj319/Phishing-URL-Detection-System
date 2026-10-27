export function ShieldArt({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 220 150" className={className} aria-hidden>
      <g stroke="rgb(var(--cyan))" strokeOpacity="0.45" fill="none" strokeWidth="1">
        <path d="M10 100h45l15-20h30" /><path d="M210 40h-40l-12 18" /><path d="M20 30h35l12 16" /><path d="M205 112h-38l-14-16" />
      </g>
      <g fill="rgb(var(--cyan))" fillOpacity="0.8"><circle cx="10" cy="100" r="2.500" /><circle cx="210" cy="40" r="2.500" /><circle cx="20" cy="30" r="2.500" /><circle cx="205" cy="112" r="2.500" /></g>
      <path d="M110 12 66 30v38c0 30 20 52 44 62 24-10 44-32 44-62V30z" fill="rgb(var(--primary))" fillOpacity="0.22" stroke="rgb(var(--primary))" strokeWidth="3" />
      <rect x="94" y="64" width="32" height="26" rx="4" fill="rgb(var(--fg))" />
      <path d="M100 64v-8a10 10 0 0 1 20 0v8" fill="none" stroke="rgb(var(--fg))" strokeWidth="5" />
    </svg>
  );
}
