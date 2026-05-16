import type { AiInsights } from '../api/dashboard';

interface SectionProps {
  title: string;
  bullets: string[];
  accent: string;
}

function Section({ title, bullets, accent }: SectionProps) {
  return (
    <div>
      <h4 className={`mb-2 text-xs font-semibold uppercase tracking-wide ${accent}`}>{title}</h4>
      <ul className="space-y-1.5 text-sm text-slate-700">
        {bullets.length === 0 ? (
          <li className="italic text-slate-400">No data.</li>
        ) : (
          bullets.map((b, i) => (
            <li key={i} className="flex gap-2">
              <span className={`mt-1.5 h-1.5 w-1.5 shrink-0 rounded-full ${accent.replace('text-', 'bg-')}`} />
              <span>{b}</span>
            </li>
          ))
        )}
      </ul>
    </div>
  );
}

export function AiInsightsPanel({ insights }: { insights: AiInsights }) {
  return (
    <div className="rounded-xl border border-slate-200 bg-gradient-to-br from-indigo-50 to-white p-5 shadow-sm">
      <div className="mb-4 flex items-center gap-2">
        <span className="text-lg">✨</span>
        <h3 className="text-sm font-semibold uppercase tracking-wide text-slate-700">
          AI Insights
        </h3>
        <span className="ml-auto text-xs text-slate-400">drafted by Claude</span>
      </div>
      <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
        <Section title="Top Issues"          bullets={insights.topIssues}        accent="text-risk-700" />
        <Section title="Repeat Offenders"    bullets={insights.repeatOffenders}  accent="text-warn-700" />
        <Section title="High-Risk Projects"  bullets={insights.highRiskProjects} accent="text-info-700" />
      </div>
    </div>
  );
}
