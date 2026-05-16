// Mirrors com.timesheets.ai.dashboard.DashboardDtos. Keep in sync.

export interface Kpis {
  totalResources: number;
  totalDiscrepancies: number;
  criticalIssues: number;
  revenueAtRiskUsd: number;
  resolvedPercent: number;
}

export interface DiscrepancyBreakdown {
  label: string;
  count: number;
}

export interface WeeklyTrendPoint {
  weekLabel: string;
  discrepancies: number;
}

export interface ContractorRow {
  employeeCode: string;
  employeeName: string;
  reportsTo: string;
  manager: string;
  emailId: string;
  underReport: number;
  overReport: number;
  missing: number;
  total: number;
  impactUsd: number;
  status: string;
}

export interface PtoRow {
  employeeCode: string;
  employeeName: string;
  sowName: string;
  ptoDays: number;
  comments: string;
}

export interface AiInsights {
  topIssues: string[];
  repeatOffenders: string[];
  highRiskProjects: string[];
}

export interface ReportTab {
  label: string;
  description: string;
  rowCount: number;
}

export interface DashboardSummary {
  reportPath: string;
  defaultRecipient: string;
  kpis: Kpis;
  discrepancyBreakdown: DiscrepancyBreakdown[];
  weeklyTrend: WeeklyTrendPoint[];
  contractors: ContractorRow[];
  ptoMismatches: PtoRow[];
  aiInsights: AiInsights;
  reportTabs: ReportTab[];
}

export async function fetchDashboardSummary(): Promise<DashboardSummary> {
  const res = await fetch('/api/dashboard/summary');
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Dashboard API failed (${res.status}): ${text}`);
  }
  return res.json();
}

export interface SendEmailResult {
  recipient: string;
  totalDiscrepancyRows: number;
  totalContractorsFlagged: number;
  subject: string | null;
  body: string | null;
  sent: boolean;
  statusMessage: string;
}

export async function sendDiscrepancyEmail(recipient?: string): Promise<SendEmailResult> {
  const params = new URLSearchParams();
  if (recipient) params.set('recipient', recipient);
  const url = `/api/email/send-summary${params.toString() ? `?${params}` : ''}`;
  const res = await fetch(url, { method: 'POST' });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new Error(`Send API failed (${res.status}): ${text}`);
  }
  return res.json();
}
