import { useEffect, useState } from 'react';
import { fetchDashboard } from '@/api/dashboard';
import DimensionCard from '@/components/DimensionCard';
import type { DashboardResponse } from '@/types/api';

function formatInr(amount: number): string {
  if (amount >= 10_000_000) return `₹${(amount / 10_000_000).toFixed(2)} Cr`;
  if (amount >= 100_000) return `₹${(amount / 100_000).toFixed(2)} L`;
  return `₹${amount.toLocaleString('en-IN')}`;
}

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetchDashboard()
      .then(setDashboard)
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="p-8 flex items-center justify-center h-full">
        <div className="text-gray-400 text-sm animate-pulse">Loading dashboard...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8">
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-xl p-6 text-sm">
          <p className="font-medium">Could not load dashboard</p>
          <p className="mt-1 opacity-80">{error}</p>
        </div>
      </div>
    );
  }

  if (!dashboard) return null;
  const s = dashboard.summary;

  return (
    <div className="p-8 space-y-8">
      {/* Header */}
      <div>
        <h2 className="text-2xl font-bold text-gray-900">Financial Health Dashboard</h2>
        <p className="text-sm text-gray-500 mt-1">
          Snapshot: {dashboard.snapshotDate} &middot; {dashboard.dimensions.length} dimensions
        </p>
      </div>

      {/* Summary cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <SummaryCard label="Net Worth" value={formatInr(s.netWorth)} color="indigo" />
        <SummaryCard
          label="Monthly Surplus"
          value={formatInr(s.monthlySurplus)}
          color={s.monthlySurplus >= 0 ? 'green' : 'red'}
        />
        <SummaryCard
          label="Emergency"
          value={`${Number(s.emergencyCoverageMonths).toFixed(1)} months`}
          color={Number(s.emergencyCoverageMonths) >= 6 ? 'green' : 'red'}
        />
        <SummaryCard
          label="Goals"
          value={`${s.goalsOnTrack}/${s.goalCount} on track`}
          color={s.goalsAtRisk === 0 ? 'green' : 'amber'}
        />
      </div>

      {/* 10-dimension grid */}
      <div>
        <h3 className="text-lg font-semibold text-gray-800 mb-4">Health Dimensions</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {dashboard.dimensions.map((dim) => (
            <DimensionCard key={dim.index} dim={dim} />
          ))}
        </div>
      </div>
    </div>
  );
}

function SummaryCard({ label, value, color }: { label: string; value: string; color: string }) {
  const colorMap: Record<string, string> = {
    indigo: 'bg-indigo-50 text-indigo-700 border-indigo-200',
    green: 'bg-green-50 text-green-700 border-green-200',
    red: 'bg-red-50 text-red-700 border-red-200',
    amber: 'bg-amber-50 text-amber-700 border-amber-200',
  };
  return (
    <div className={`rounded-xl border p-4 ${colorMap[color] ?? colorMap.indigo}`}>
      <p className="text-xs font-medium uppercase tracking-wider opacity-70">{label}</p>
      <p className="text-lg font-bold mt-1">{value}</p>
    </div>
  );
}
