import { useEffect, useState } from 'react';
import { CLASS_META } from '../lib/format';
import type { Classification } from '../lib/types';

/** Animates only after mount (i.e. after a real result exists); CSS disables the transition for reduced motion. */
export function ScoreRing({ score, classification, size = 140 }: { score: number; classification: Classification; size?: number }) {
  const [shown, setShown] = useState(0);
  useEffect(() => { const t = requestAnimationFrame(() => setShown(score)); return () => cancelAnimationFrame(t); }, [score]);
  const r = (size - 16) / 2;
  const c = 2 * Math.PI * r;
  const m = CLASS_META[classification];
  return (
    <div className="relative shrink-0" style={{ width: size, height: size }} role="img" aria-label={`Risk score ${score} out of 100, ${m.label}`}>
      <svg width={size} height={size} className="-rotate-90">
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="rgb(var(--line))" strokeWidth="10" />
        <circle cx={size / 2} cy={size / 2} r={r} fill="none" stroke="currentColor" strokeWidth="10" strokeLinecap="round"
          className={m.text} strokeDasharray={c} strokeDashoffset={c * (1 - shown / 100)}
          style={{ transition: 'stroke-dashoffset 0.9s ease-out' }} />
      </svg>
      <div className="absolute inset-0 flex flex-col items-center justify-center">
        <span className="text-3xl font-bold tabular-nums">{score}</span>
        <span className="text-xs text-muted">of 100</span>
      </div>
    </div>
  );
}
