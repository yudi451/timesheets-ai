import { Cell, Legend, Pie, PieChart, ResponsiveContainer, Tooltip } from 'recharts';
import type { DiscrepancyBreakdown } from '../api/dashboard';

// Stable color per slice label so the legend doesn't reshuffle between renders.
const COLORS: Record<string, string> = {
  Missing:            '#ef4444',
  'Under-report':     '#f59e0b',
  'Over-report':      '#3b82f6',
  'PTO Mismatch':     '#a855f7',
  'Billing Mismatch': '#14b8a6',
};

export function DiscrepancyPie({ data }: { data: DiscrepancyBreakdown[] }) {
  const filtered = data.filter((d) => d.count > 0);
  return (
    <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
      <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-slate-500">
        Discrepancy Overview
      </h3>
      <div className="h-72">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={filtered}
              dataKey="count"
              nameKey="label"
              cx="50%"
              cy="50%"
              outerRadius={90}
              label={(entry) => `${entry.label}: ${entry.count}`}
            >
              {filtered.map((entry) => (
                <Cell key={entry.label} fill={COLORS[entry.label] ?? '#94a3b8'} />
              ))}
            </Pie>
            <Tooltip />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}
