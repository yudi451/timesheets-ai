import type { Kpis } from '../api/dashboard';

const FMT_USD = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
});

interface CardProps {
  label: string;
  value: string;
  tone: 'risk' | 'warn' | 'ok' | 'info' | 'neutral';
}

function Card({ label, value, tone }: CardProps) {
  const toneClasses: Record<CardProps['tone'], string> = {
    risk:    'bg-risk-50 text-risk-700 border-risk-500/30',
    warn:    'bg-warn-50 text-warn-700 border-warn-500/30',
    ok:      'bg-ok-50 text-ok-700 border-ok-500/30',
    info:    'bg-info-50 text-info-700 border-info-500/30',
    neutral: 'bg-white text-slate-700 border-slate-200',
  };
  return (
    <div className={`rounded-xl border p-5 shadow-sm ${toneClasses[tone]}`}>
      <div className="text-xs uppercase tracking-wide opacity-70">{label}</div>
      <div className="mt-2 text-3xl font-semibold">{value}</div>
    </div>
  );
}

export function KpiCards({ kpis }: { kpis: Kpis }) {
  return (
    <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-5">
      <Card label="Total Resources"  value={String(kpis.totalResources)}      tone="neutral" />
      <Card label="Discrepancies"    value={String(kpis.totalDiscrepancies)}  tone="info" />
      <Card label="Critical Issues"  value={String(kpis.criticalIssues)}      tone="risk" />
      <Card label="Revenue at Risk"  value={FMT_USD.format(kpis.revenueAtRiskUsd)} tone="warn" />
      <Card label="Resolved %"       value={`${kpis.resolvedPercent}%`}       tone="ok" />
    </div>
  );
}
