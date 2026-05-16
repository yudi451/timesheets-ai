import { useEffect, useState } from 'react';
import { fetchDashboardSummary, type DashboardSummary } from './api/dashboard';
import { KpiCards } from './components/KpiCards';
import { DiscrepancyPie } from './components/DiscrepancyPie';
import { WeeklyTrendLine } from './components/WeeklyTrendLine';
import { ContractorGrid } from './components/ContractorGrid';
import { AiInsightsPanel } from './components/AiInsightsPanel';
import { ReportsTabs } from './components/ReportsTabs';
import { PtoMismatchPanel } from './components/PtoMismatchPanel';
import { SendEmailButton } from './components/SendEmailButton';

// Default recipient — matches app.email.default-recipient in application.yml.
// If you wire this in dynamically later, hoist it onto DashboardSummary.
const DEFAULT_RECIPIENT = 'uday.rajpurohit@gmail.com';

export default function App() {
  const [data, setData] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    setLoading(true);
    fetchDashboardSummary()
      .then((d) => setData(d))
      .catch((e) => setError(String(e)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="flex h-full items-center justify-center text-slate-500">
        Loading dashboard…
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="mx-auto max-w-2xl p-8">
        <div className="rounded-xl border border-risk-500/30 bg-risk-50 p-6 text-risk-700">
          <h2 className="text-lg font-semibold">Couldn't load dashboard</h2>
          <p className="mt-1 text-sm">
            Is the Spring Boot backend running on <code>localhost:8080</code>?
          </p>
          {error && <pre className="mt-3 whitespace-pre-wrap text-xs">{error}</pre>}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-full">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-start justify-between gap-4 px-6 py-4">
          <div>
            <h1 className="text-xl font-semibold text-slate-900">Timesheets AI Dashboard</h1>
            <p className="text-xs text-slate-500">Source: {data.reportPath}</p>
          </div>
          <SendEmailButton defaultRecipient={DEFAULT_RECIPIENT} />
        </div>
      </header>

      <main className="mx-auto max-w-7xl space-y-6 px-6 py-6">
        <KpiCards kpis={data.kpis} />

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-2">
          <DiscrepancyPie data={data.discrepancyBreakdown} />
          <WeeklyTrendLine data={data.weeklyTrend} />
        </div>

        <AiInsightsPanel insights={data.aiInsights} />

        <ContractorGrid rows={data.contractors} />

        <PtoMismatchPanel rows={data.ptoMismatches} />

        <ReportsTabs tabs={data.reportTabs} />
      </main>
    </div>
  );
}
