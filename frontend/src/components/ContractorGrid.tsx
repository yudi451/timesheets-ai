import type { ContractorRow } from '../api/dashboard';

const FMT_USD = new Intl.NumberFormat('en-US', {
  style: 'currency',
  currency: 'USD',
  maximumFractionDigits: 0,
});

function statusBadgeClasses(status: string): string {
  if (status === 'At Risk') return 'bg-risk-50 text-risk-700 ring-1 ring-risk-500/30';
  if (status === 'Action Needed') return 'bg-warn-50 text-warn-700 ring-1 ring-warn-500/30';
  return 'bg-slate-100 text-slate-700 ring-1 ring-slate-300';
}

export function ContractorGrid({ rows }: { rows: ContractorRow[] }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-5 py-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          Contractor Breakdown
        </h3>
        <p className="text-xs text-slate-400">Reports To → Manager → Resource</p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-2 text-left font-medium">Reports To</th>
              <th className="px-4 py-2 text-left font-medium">Manager</th>
              <th className="px-4 py-2 text-left font-medium">Resource</th>
              <th className="px-4 py-2 text-right font-medium">Under</th>
              <th className="px-4 py-2 text-right font-medium">Over</th>
              <th className="px-4 py-2 text-right font-medium">Missing</th>
              <th className="px-4 py-2 text-right font-medium">Total</th>
              <th className="px-4 py-2 text-right font-medium">Impact</th>
              <th className="px-4 py-2 text-left font-medium">Status</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.map((r) => (
              <tr key={r.employeeCode} className="hover:bg-slate-50">
                <td className="px-4 py-2 text-slate-600">{r.reportsTo}</td>
                <td className="px-4 py-2 text-slate-700">{r.manager}</td>
                <td className="px-4 py-2">
                  <div className="font-medium text-slate-800">{r.employeeName}</div>
                  <div className="text-xs text-slate-400">#{r.employeeCode}</div>
                </td>
                <td className="px-4 py-2 text-right tabular-nums">{r.underReport}</td>
                <td className="px-4 py-2 text-right tabular-nums">{r.overReport}</td>
                <td className="px-4 py-2 text-right tabular-nums">{r.missing}</td>
                <td className="px-4 py-2 text-right font-semibold tabular-nums">{r.total}</td>
                <td className="px-4 py-2 text-right tabular-nums text-slate-700">
                  {FMT_USD.format(r.impactUsd)}
                </td>
                <td className="px-4 py-2">
                  <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${statusBadgeClasses(r.status)}`}>
                    {r.status}
                  </span>
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan={9} className="px-4 py-8 text-center text-slate-400">
                  No contractors flagged in the latest report.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
