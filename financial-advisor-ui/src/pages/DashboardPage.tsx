import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { fetchDashboard } from '@/api/dashboard';
import DimensionCard from '@/components/DimensionCard';
import InfoTooltip from '@/components/InfoTooltip';
import type { DashboardResponse } from '@/types/api';

function formatInr(amount: number): string {
  const abs = Math.abs(amount);
  const sign = amount < 0 ? '-' : '';
  if (abs >= 10_000_000) return `${sign}₹${(abs / 10_000_000).toFixed(2)} Cr`;
  if (abs >= 100_000) return `${sign}₹${(abs / 100_000).toFixed(2)} L`;
  return `${sign}₹${abs.toLocaleString('en-IN')}`;
}

function getHealthEmoji(score: number): string {
  if (score >= 80) return '🌟';
  if (score >= 60) return '😊';
  if (score >= 40) return '😐';
  if (score >= 20) return '😟';
  return '😰';
}

function calculateHealthScore(s: DashboardResponse['summary']): number {
  let score = 50;
  if (s.netWorth > 0) score += 10;
  if (s.monthlySurplus > 0) score += 10;
  if (Number(s.emergencyCoverageMonths) >= 6) score += 15;
  else if (Number(s.emergencyCoverageMonths) >= 3) score += 5;
  else score -= 10;
  if (s.goalsOnTrack > 0) score += 5 * Math.min(s.goalsOnTrack, 3);
  if (s.goalsAtRisk > 0) score -= 5 * Math.min(s.goalsAtRisk, 3);
  if (s.totalLiabilities > s.liquidAssets) score -= 10;
  return Math.max(0, Math.min(100, score));
}

export default function DashboardPage() {
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const navigate = useNavigate();

  useEffect(() => {
    fetchDashboard()
      .then(setDashboard)
      .catch((err) => setError(err.response?.data?.message ?? 'Failed to load dashboard'))
      .finally(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="p-8 flex flex-col items-center justify-center h-full gap-4">
        <div className="text-5xl animate-bounce">💰</div>
        <p className="text-gray-400 text-sm animate-pulse">Loading your financial dashboard...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-8 max-w-lg mx-auto mt-12">
        <div className="bg-amber-50 border-2 border-amber-200 rounded-2xl p-8 text-center">
          <span className="text-5xl mb-4 block">📝</span>
          <h3 className="text-lg font-bold text-gray-800 mb-2">Let's get started!</h3>
          <p className="text-sm text-gray-600 leading-relaxed mb-6">
            You haven't added your money information yet.
            Tell us about your income, expenses, savings, and goals — and we'll show you
            a clear picture of your financial health!
          </p>
          <button
            onClick={() => navigate('/profile')}
            className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white
                       font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700
                       transition-all shadow-md hover:shadow-lg active:scale-[0.98]"
          >
            📝 Add My Money Info
          </button>
        </div>
      </div>
    );
  }

  if (!dashboard) return null;
  const s = dashboard.summary;
  const healthScore = calculateHealthScore(s);
  const healthEmoji = getHealthEmoji(healthScore);

  return (
    <div className="p-6 md:p-8 space-y-8 max-w-6xl mx-auto">
      {/* Welcome header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 flex items-center gap-2">
            {healthEmoji} Your Money Dashboard
          </h2>
          <p className="text-sm text-gray-400 mt-1">
            Here's how your finances look right now — let's make them better! 💪
          </p>
        </div>
        <div className="flex items-center gap-3">
          <div className={`px-4 py-2 rounded-xl text-sm font-bold ${
            healthScore >= 60 ? 'bg-emerald-100 text-emerald-700' :
            healthScore >= 40 ? 'bg-amber-100 text-amber-700' :
            'bg-red-100 text-red-700'
          }`}>
            Health Score: {healthScore}/100
          </div>
          <InfoTooltip text="Your overall financial health score. It considers your savings, debts, emergency fund, and goals. Higher is better! 80+ is great, 60+ is okay, below 40 needs work.">
            <span />
          </InfoTooltip>
        </div>
      </div>

      {/* Summary cards — the big picture */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <SummaryCard
          icon="🏦"
          label="Net Worth"
          value={formatInr(s.netWorth)}
          hint="Everything you own minus everything you owe"
          tooltip="Net worth = Total savings + investments + property - all loans. If this is positive, you own more than you owe. Negative means your loans are bigger than your savings."
          color={s.netWorth >= 0 ? 'emerald' : 'red'}
        />
        <SummaryCard
          icon="📈"
          label="Monthly Savings"
          value={formatInr(s.monthlySurplus)}
          hint="Money left after all spending each month"
          tooltip="This is what's left from your salary after paying for rent, food, EMIs, insurance, and investments (SIPs). More savings = more flexibility!"
          color={s.monthlySurplus >= 0 ? 'emerald' : 'red'}
        />
        <SummaryCard
          icon="🚨"
          label="Emergency Fund"
          value={`${Number(s.emergencyCoverageMonths).toFixed(1)} months`}
          hint="How long you can survive without income"
          tooltip="If you lost your job today, how many months could you pay bills? You need at least 6 months. This money should be in a savings account or FD, not stocks."
          color={Number(s.emergencyCoverageMonths) >= 6 ? 'emerald' : Number(s.emergencyCoverageMonths) >= 3 ? 'amber' : 'red'}
        />
        <SummaryCard
          icon="🎯"
          label="Goals Progress"
          value={`${s.goalsOnTrack} of ${s.goalCount} on track`}
          hint="How many of your money goals are going well"
          tooltip="These are the financial goals you set (retirement, education, vacation, etc). 'On track' means you're saving enough to reach them on time."
          color={s.goalsAtRisk === 0 ? 'emerald' : s.goalsOnTrack > 0 ? 'amber' : 'red'}
        />
      </div>

      {/* Quick numbers bar */}
      <div className="bg-white rounded-2xl border border-gray-200 p-5">
        <h3 className="text-sm font-bold text-gray-700 mb-4 flex items-center gap-2">
          📊 Quick Numbers
          <InfoTooltip text="A snapshot of your money in different buckets. Liquid means easy to access. Illiquid means locked or hard to sell quickly (like gold, property).">
            <span />
          </InfoTooltip>
        </h3>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-4 text-sm">
          <QuickStat label="💧 Easy-to-access money" value={formatInr(s.liquidAssets)} />
          <QuickStat label="🔒 Locked money" value={formatInr(s.illiquidAssets)} />
          <QuickStat label="💳 Total loans" value={formatInr(s.totalLiabilities)} negative={s.totalLiabilities > 0} />
          <QuickStat label="⚖️ Risk appetite" value={s.riskLevel.replace('_', ' ')} />
        </div>
      </div>

      {/* 10-dimension grid */}
      <div>
        <h3 className="text-lg font-bold text-gray-800 mb-2 flex items-center gap-2">
          🔍 Detailed Health Check
          <InfoTooltip text="We check 10 different areas of your finances. Green = good, Yellow = okay but could be better, Red = needs attention. Click the ? on any card to learn more!">
            <span />
          </InfoTooltip>
        </h3>
        <p className="text-sm text-gray-400 mb-5">
          Each card shows one part of your financial health. Look for the <span className="text-blue-500 font-semibold">?</span> button to learn what it means!
        </p>
        <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4">
          {dashboard.dimensions.map((dim) => (
            <DimensionCard key={dim.index} dim={dim} />
          ))}
        </div>
      </div>
    </div>
  );
}

function SummaryCard({
  icon, label, value, hint, tooltip, color,
}: {
  icon: string; label: string; value: string; hint: string; tooltip: string; color: string;
}) {
  const colorMap: Record<string, string> = {
    emerald: 'bg-emerald-50 border-emerald-200',
    red: 'bg-red-50 border-red-200',
    amber: 'bg-amber-50 border-amber-200',
    indigo: 'bg-indigo-50 border-indigo-200',
  };
  const textMap: Record<string, string> = {
    emerald: 'text-emerald-700',
    red: 'text-red-700',
    amber: 'text-amber-700',
    indigo: 'text-indigo-700',
  };

  return (
    <div className={`rounded-2xl border-2 p-5 ${colorMap[color] ?? colorMap.indigo} transition-all hover:shadow-md`}>
      <div className="flex items-center justify-between mb-1">
        <span className="text-2xl">{icon}</span>
        <InfoTooltip text={tooltip}>
          <span />
        </InfoTooltip>
      </div>
      <p className={`text-xl font-bold mt-2 ${textMap[color] ?? textMap.indigo}`}>{value}</p>
      <p className="text-xs font-semibold text-gray-700 mt-1">{label}</p>
      <p className="text-[11px] text-gray-400 mt-0.5 leading-snug">{hint}</p>
    </div>
  );
}

function QuickStat({ label, value, negative }: { label: string; value: string; negative?: boolean }) {
  return (
    <div className="text-center p-3 rounded-xl bg-gray-50">
      <p className="text-[11px] text-gray-500 mb-1">{label}</p>
      <p className={`text-sm font-bold ${negative ? 'text-red-600' : 'text-gray-800'}`}>{value}</p>
    </div>
  );
}
