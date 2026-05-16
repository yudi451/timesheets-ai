import type { PtoRow } from '../api/dashboard';

export function PtoMismatchPanel({ rows }: { rows: PtoRow[] }) {
  if (rows.length === 0) {
    return (
      <div className="rounded-xl border border-slate-200 bg-white p-5 shadow-sm">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          PTO Mismatches
        </h3>
        <p className="mt-2 text-sm text-slate-400 italic">
          No PTO rows flagged in the latest report.
        </p>
      </div>
    );
  }

  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-200 px-5 py-3">
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-500">
          PTO Mismatches
        </h3>
        <p className="text-xs text-slate-400">
          Rows where the PTO Report sheet has a Comments note that needs attention
        </p>
      </div>
      <div className="overflow-x-auto">
        <table className="min-w-full divide-y divide-slate-200 text-sm">
          <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
            <tr>
              <th className="px-4 py-2 text-left font-medium">Resource</th>
              <th className="px-4 py-2 text-left font-medium">SOW</th>
              <th className="px-4 py-2 text-right font-medium">PTO days</th>
              <th className="px-4 py-2 text-left font-medium">Comment</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {rows.map((r) => (
              <tr key={r.employeeCode} className="hover:bg-slate-50">
                <td className="px-4 py-2">
                  <div className="font-medium text-slate-800">{r.employeeName}</div>
                  <div className="text-xs text-slate-400">#{r.employeeCode}</div>
                </td>
                <td className="px-4 py-2 text-xs text-slate-600">{r.sowName || '—'}</td>
                <td className="px-4 py-2 text-right tabular-nums">{r.ptoDays}</td>
                <td className="px-4 py-2 text-xs text-slate-700">{r.comments}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
