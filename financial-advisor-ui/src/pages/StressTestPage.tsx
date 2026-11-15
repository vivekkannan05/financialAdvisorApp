import { useState, type FormEvent } from 'react';
import { runStressTest, compareScenarios } from '@/api/stressTest';
import InfoTooltip from '@/components/InfoTooltip';
import type { StressTestResult, ScenarioComparison, StressType } from '@/types/api';

const stressTypes: { value: StressType; icon: string; label: string; hint: string; paramLabel: string; paramHint: string; defaultParam: string }[] = [
  {
    value: 'MARKET_FALL', icon: '📉', label: 'Stock Market Crash',
    hint: 'What if the stock market falls suddenly?',
    paramLabel: 'How much does it fall? (%)',
    paramHint: 'E.g. 30 means stocks drop 30%', defaultParam: '30',
  },
  {
    value: 'INCOME_LOSS', icon: '😰', label: 'Job Loss / Income Drop',
    hint: 'What if you lose your income?',
    paramLabel: 'How much income do you lose? (%)',
    paramHint: 'E.g. 100 = total job loss, 50 = half salary cut', defaultParam: '100',
  },
  {
    value: 'INTEREST_RATE_CHANGE', icon: '🏦', label: 'Loan Rates Go Up',
    hint: 'What if banks increase your loan interest?',
    paramLabel: 'How much do rates go up? (%)',
    paramHint: 'E.g. 2 means rates increase by 2%', defaultParam: '2',
  },
  {
    value: 'LARGE_EXPENSE', icon: '🏥', label: 'Big Emergency Expense',
    hint: 'What if you have an unexpected large expense?',
    paramLabel: 'How much do you need? (₹)',
    paramHint: 'E.g. 500000 for a medical emergency', defaultParam: '500000',
  },
];

export default function StressTestPage() {
  return (
    <div className="p-6 md:p-8 max-w-5xl mx-auto space-y-10">
      <div>
        <h2 className="text-2xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
          🔮 What If? — Stress Test Your Finances
        </h2>
        <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
          Life is unpredictable. Let's see how your finances would handle different tough situations.
        </p>
      </div>
      <StressTestSection />
      <ScenarioSection />
    </div>
  );
}

function StressTestSection() {
  const [selectedType, setSelectedType] = useState<StressType>('MARKET_FALL');
  const [param, setParam] = useState('30');
  const [result, setResult] = useState<StressTestResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const selected = stressTypes.find((t) => t.value === selectedType)!;

  async function handleRun(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const paramValue = selected.value === 'LARGE_EXPENSE'
        ? parseFloat(param)
        : parseFloat(param) / 100;
      const data = await runStressTest(selectedType, paramValue);
      setResult(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Stress test failed');
    } finally {
      setLoading(false);
    }
  }

  function formatInr(n: number): string {
    const abs = Math.abs(n);
    const sign = n < 0 ? '-' : '';
    if (abs >= 10_000_000) return `${sign}₹${(abs / 10_000_000).toFixed(2)} Cr`;
    if (abs >= 100_000) return `${sign}₹${(abs / 100_000).toFixed(2)} L`;
    return `${sign}₹${abs.toLocaleString('en-IN')}`;
  }

  return (
    <div className="bg-white dark:bg-gray-800/80 rounded-2xl border border-gray-200 dark:border-gray-700
                    p-6 md:p-8 space-y-6">
      <div className="flex items-center gap-2">
        <h3 className="text-lg font-bold text-gray-800 dark:text-gray-100">⚡ Run a Stress Test</h3>
        <InfoTooltip text="A stress test shows what would happen to your money if something bad happens. It doesn't change anything — it just simulates the scenario so you can be prepared!">
          <span />
        </InfoTooltip>
      </div>

      {/* Scenario selector */}
      <div>
        <p className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-3">Pick a scenario:</p>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {stressTypes.map((t) => (
            <button
              key={t.value}
              type="button"
              onClick={() => { setSelectedType(t.value); setParam(t.defaultParam); setResult(null); }}
              className={`text-left p-4 rounded-xl border-2 transition-all ${
                selectedType === t.value
                  ? 'border-red-300 dark:border-red-700 bg-red-50 dark:bg-red-950/40 shadow-sm'
                  : 'border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700'
              }`}
            >
              <span className="text-2xl">{t.icon}</span>
              <p className="font-semibold text-sm text-gray-800 dark:text-gray-100 mt-1">{t.label}</p>
              <p className="text-xs text-gray-400 dark:text-gray-500 mt-0.5">{t.hint}</p>
            </button>
          ))}
        </div>
      </div>

      {/* Parameter input */}
      <form onSubmit={handleRun} className="flex flex-wrap items-end gap-4">
        <div className="flex-1 min-w-48">
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{selected.paramLabel}</label>
            <InfoTooltip text={selected.paramHint}><span /></InfoTooltip>
          </div>
          <input
            type="number"
            step="any"
            value={param}
            onChange={(e) => setParam(e.target.value)}
            className="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                       bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                       focus:ring-2 focus:ring-red-400 focus:border-red-400 outline-none transition-all"
          />
        </div>
        <button
          type="submit"
          disabled={loading}
          className="px-6 py-3 bg-gradient-to-r from-red-500 to-orange-500 text-white
                     font-semibold rounded-xl hover:from-red-600 hover:to-orange-600
                     disabled:opacity-50 transition-all shadow-md active:scale-[0.98] text-sm"
        >
          {loading ? '⏳ Running...' : '🔮 See What Happens'}
        </button>
      </form>

      {error && (
        <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 px-4 py-3 rounded-xl">
          ❌ {error}
        </p>
      )}

      {/* Results */}
      {result && (
        <div className="space-y-5 pt-4 border-t border-gray-100 dark:border-gray-700">
          <h4 className="text-sm font-bold text-gray-700 dark:text-gray-300">📊 Results:</h4>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
            <ResultCard emoji="💰" label="Your savings BEFORE" value={formatInr(result.portfolioValueBefore)} />
            <ResultCard emoji="📉" label="Your savings AFTER" value={formatInr(result.portfolioValueAfter)}
              alert={result.portfolioValueAfter < result.portfolioValueBefore} />
            <ResultCard emoji={Number(result.portfolioImpact) < 0 ? '🔴' : '🟢'} label="Impact on savings"
              value={formatInr(result.portfolioImpact)} alert={Number(result.portfolioImpact) < 0} />
          </div>

          {result.warnings.length > 0 && (
            <div className="bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800
                            rounded-xl p-4 space-y-2">
              <p className="text-sm font-bold text-amber-800 dark:text-amber-400">⚠️ Warnings</p>
              {result.warnings.map((w, i) => (
                <p key={i} className="text-sm text-amber-700 dark:text-amber-400">• {w}</p>
              ))}
            </div>
          )}

          {result.goalImpacts.length > 0 && (
            <div>
              <h4 className="text-sm font-bold text-gray-700 dark:text-gray-300 mb-3">🎯 How it affects your goals:</h4>
              <div className="space-y-2">
                {result.goalImpacts.map((gi) => (
                  <div key={gi.goalId} className="flex items-center gap-3 p-3 bg-gray-50 dark:bg-gray-700/50
                                                  rounded-xl text-sm">
                    <span className={`shrink-0 text-lg ${
                      gi.status.includes('risk') ? '🔴' : gi.status.includes('impact') ? '🟡' : '🟢'
                    }`}>
                      {gi.status.includes('risk') ? '🔴' : gi.status.includes('impact') ? '🟡' : '🟢'}
                    </span>
                    <div className="flex-1">
                      <span className="font-medium text-gray-800 dark:text-gray-200">{gi.goalName}</span>
                      <span className="text-gray-400 dark:text-gray-500 ml-2">
                        {Number(gi.fundingPercentBefore).toFixed(0)}% → {Number(gi.fundingPercentAfter).toFixed(0)}%
                      </span>
                    </div>
                    <span className={`text-xs font-bold px-2 py-1 rounded-full ${
                      gi.status.includes('risk') ? 'bg-red-100 dark:bg-red-900/50 text-red-700 dark:text-red-400'
                      : gi.status.includes('impact') ? 'bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-400'
                      : 'bg-emerald-100 dark:bg-emerald-900/50 text-emerald-700 dark:text-emerald-400'
                    }`}>
                      {gi.status.replace(/-/g, ' ')}
                    </span>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
}

function ScenarioSection() {
  const [sipIncrease, setSipIncrease] = useState('10000');
  const [prepayment, setPrepayment] = useState('10000');
  const [loanRate, setLoanRate] = useState('8.5');
  const [years, setYears] = useState('10');
  const [result, setResult] = useState<ScenarioComparison | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleCompare(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await compareScenarios(
        parseFloat(sipIncrease), parseFloat(prepayment),
        parseFloat(loanRate), parseInt(years)
      );
      setResult(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Comparison failed');
    } finally {
      setLoading(false);
    }
  }

  const inputCls = `w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                    bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                    focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none`;

  return (
    <div className="bg-white dark:bg-gray-800/80 rounded-2xl border border-gray-200 dark:border-gray-700
                    p-6 md:p-8 space-y-6">
      <div>
        <div className="flex items-center gap-2">
          <h3 className="text-lg font-bold text-gray-800 dark:text-gray-100">📊 Should I Invest More or Pay Off Loans?</h3>
          <InfoTooltip text="This is one of the biggest financial decisions! Should you put extra money into investments (SIP) for growth, or pay off your home loan faster to save on interest? Let's compare both options with your real numbers.">
            <span />
          </InfoTooltip>
        </div>
        <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
          Compare: Investing more (SIP) vs. Paying off your home loan faster
        </p>
      </div>

      <form onSubmit={handleCompare} className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Extra SIP per month (₹)</label>
            <InfoTooltip text="How much extra money can you invest per month through SIP? Even ₹5,000 extra grows a lot over 10+ years!">
              <span />
            </InfoTooltip>
          </div>
          <input type="number" value={sipIncrease} onChange={(e) => setSipIncrease(e.target.value)} className={inputCls} />
        </div>
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Loan prepayment (₹)</label>
            <InfoTooltip text="How much extra can you pay towards your home loan? Even a small prepayment can save lakhs in interest over the loan period!">
              <span />
            </InfoTooltip>
          </div>
          <input type="number" value={prepayment} onChange={(e) => setPrepayment(e.target.value)} className={inputCls} />
        </div>
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Home loan rate (%)</label>
            <InfoTooltip text="What's your current home loan interest rate? Usually 8-10% in India. Higher rate = prepayment saves more money.">
              <span />
            </InfoTooltip>
          </div>
          <input type="number" step="0.1" value={loanRate} onChange={(e) => setLoanRate(e.target.value)} className={inputCls} />
        </div>
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Years remaining</label>
            <InfoTooltip text="How many more years until your home loan is fully paid off? Check your loan statement.">
              <span />
            </InfoTooltip>
          </div>
          <input type="number" value={years} onChange={(e) => setYears(e.target.value)} className={inputCls} />
        </div>
        <div className="sm:col-span-2">
          <button type="submit" disabled={loading}
            className="w-full py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white
                       font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700
                       disabled:opacity-50 transition-all shadow-md active:scale-[0.98] text-sm">
            {loading ? '⏳ Comparing...' : '📊 Compare Options'}
          </button>
        </div>
      </form>

      {error && (
        <p className="text-sm text-red-600 dark:text-red-400 bg-red-50 dark:bg-red-950/40 px-4 py-3 rounded-xl">
          ❌ {error}
        </p>
      )}

      {result && (
        <div className="space-y-5 pt-4 border-t border-gray-100 dark:border-gray-700">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <ScenarioCard title={result.scenarioAName} outcome={result.scenarioA}
              icon="📈" color="indigo" />
            <ScenarioCard title={result.scenarioBName} outcome={result.scenarioB}
              icon="🏠" color="emerald" />
          </div>

          <div className="bg-gradient-to-r from-indigo-50 to-purple-50
                          dark:from-indigo-950/40 dark:to-purple-950/40
                          border border-indigo-200 dark:border-indigo-800 rounded-xl p-5">
            <p className="text-sm font-bold text-indigo-800 dark:text-indigo-400 flex items-center gap-2">💡 Our Recommendation</p>
            <p className="text-sm text-indigo-700 dark:text-indigo-300 mt-2 leading-relaxed">{result.recommendation}</p>
          </div>

          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-xl p-5">
            <p className="text-xs font-bold text-gray-500 dark:text-gray-400 uppercase tracking-wider mb-3">⚖️ Things to Consider</p>
            <div className="space-y-2">
              {result.tradeoffs.map((t, i) => (
                <div key={i} className="flex items-start gap-2 text-sm text-gray-600 dark:text-gray-400">
                  <span className="text-gray-400 dark:text-gray-500 mt-0.5 shrink-0">•</span>
                  <span>{t}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function ScenarioCard({ title, outcome, icon, color }: {
  title: string; outcome: { advantages: string[]; disadvantages: string[] }; icon: string; color: string;
}) {
  const borderColor = color === 'indigo'
    ? 'border-indigo-200 dark:border-indigo-800'
    : 'border-emerald-200 dark:border-emerald-800';
  const headerBg = color === 'indigo'
    ? 'bg-indigo-50 dark:bg-indigo-950/50'
    : 'bg-emerald-50 dark:bg-emerald-950/50';
  const headerText = color === 'indigo'
    ? 'text-indigo-800 dark:text-indigo-400'
    : 'text-emerald-800 dark:text-emerald-400';

  return (
    <div className={`border-2 ${borderColor} rounded-xl overflow-hidden`}>
      <div className={`${headerBg} px-5 py-3`}>
        <h4 className={`text-sm font-bold ${headerText} flex items-center gap-2`}>
          {icon} {title}
        </h4>
      </div>
      <div className="p-5 space-y-4">
        {outcome.advantages.length > 0 && (
          <div>
            <p className="text-xs font-bold text-emerald-600 dark:text-emerald-400 mb-2">👍 Advantages</p>
            {outcome.advantages.map((a, i) => (
              <p key={i} className="text-xs text-gray-600 dark:text-gray-400 flex items-start gap-2 mb-1">
                <span className="text-emerald-500 dark:text-emerald-400 shrink-0">✅</span> {a}
              </p>
            ))}
          </div>
        )}
        {outcome.disadvantages.length > 0 && (
          <div>
            <p className="text-xs font-bold text-red-600 dark:text-red-400 mb-2">👎 Disadvantages</p>
            {outcome.disadvantages.map((d, i) => (
              <p key={i} className="text-xs text-gray-600 dark:text-gray-400 flex items-start gap-2 mb-1">
                <span className="text-red-400 dark:text-red-500 shrink-0">⚠️</span> {d}
              </p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function ResultCard({ emoji, label, value, alert }: { emoji: string; label: string; value: string; alert?: boolean }) {
  return (
    <div className={`rounded-xl border-2 p-4 text-center ${
      alert
        ? 'bg-red-50 dark:bg-red-950/40 border-red-200 dark:border-red-800'
        : 'bg-gray-50 dark:bg-gray-700/50 border-gray-200 dark:border-gray-700'
    }`}>
      <span className="text-2xl">{emoji}</span>
      <p className="text-xs text-gray-500 dark:text-gray-400 mt-1">{label}</p>
      <p className={`text-lg font-bold mt-1 ${alert ? 'text-red-700 dark:text-red-400' : 'text-gray-800 dark:text-gray-200'}`}>{value}</p>
    </div>
  );
}
