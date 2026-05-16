import { useState } from 'react';
import type { ReportTab } from '../api/dashboard';

export function ReportsTabs({ tabs }: { tabs: ReportTab[] }) {
  const [active, setActive] = useState(0);
  const current = tabs[active];

  return (
    <div className="rounded-xl border border-slate-200 bg-white shadow-sm">
      <div className="flex flex-wrap gap-1 border-b border-slate-200 px-2 pt-2">
        {tabs.map((t, i) => (
          <button
            key={t.label}
            onClick={() => setActive(i)}
            className={
              'rounded-t-md px-4 py-2 text-sm font-medium transition ' +
              (i === active
                ? 'bg-slate-100 text-slate-900'
                : 'text-slate-500 hover:bg-slate-50 hover:text-slate-700')
            }
          >
            {t.label}
            <span className="ml-2 rounded-full bg-white px-2 py-0.5 text-xs text-slate-500 ring-1 ring-slate-200">
              {t.rowCount}
            </span>
          </button>
        ))}
      </div>
      <div className="px-5 py-6">
        {current ? (
          <>
            <div className="text-sm text-slate-700">{current.description}</div>
            <div className="mt-3 text-xs italic text-slate-400">
              Detailed rows for this tab will render once the upstream generator emits the data.
            </div>
          </>
        ) : (
          <div className="text-sm text-slate-400">No reports available.</div>
        )}
      </div>
    </div>
  );
}
