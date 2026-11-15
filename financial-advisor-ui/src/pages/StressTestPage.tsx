import { useState, type FormEvent } from 'react';
import { runStressTest, compareScenarios } from '@/api/stressTest';
import type { StressTestResult, ScenarioComparison, StressType } from '@/types/api';

export default function StressTestPage() {
  return (
    <div className="p-8 space-y-10">
      <h2 className="text-2xl font-bold text-gray-900">Stress Test & Scenario Analysis</h2>
      <StressTestSection />
      <ScenarioSection />
    </div>
  );
}

function StressTestSection() {
  const [type, setType] = useState<StressType>('MARKET_FALL');
  const [param, setParam] = useState('0.30');
  const [result, setResult] = useState<StressTestResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleRun(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const data = await runStressTest(type, parseFloat(param));
      setResult(data);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Stress test failed');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-5">
      <h3 className="text-lg font-semibold text-gray-800">⚡ Stress Test</h3>
      <form onSubmit={handleRun} className="flex flex-wrap gap-4 items-end">
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">Scenario Type</label>
          <select
            value={type}
            onChange={(e) => setType(e.target.value as StressType)}
            className="px-3 py-2 border border-gray-300 rounded-lg text-sm"
          >
            <option value="MARKET_FALL">Market Fall</option>
            <option value="INCOME_LOSS">Income Loss</option>
            <option value="INTEREST_RATE_CHANGE">Interest Rate Change</option>
            <option value="LARGE_EXPENSE">Large Expense</option>
          </select>
        </div>
        <div>
          <label className="block text-xs font-medium text-gray-600 mb-1">
            Parameter
          </label>
          <input
            type="number"
            step="0.01"
            value={param}
            onChange={(e) => setParam(e.target.value)}
            className="w-32 px-3 py-2 border border-gray-300 rounded-lg text-sm"
          />
          <p className="text-[10px] text-gray-400 mt-0.5">
            {type === 'LARGE_EXPENSE' ? 'Amount in ₹' : 'Fraction (e.g. 0.30 = 30%)'}
          </p>
        </div>
        <button
          type="submit"
          disabled={loading}
          className="px-5 py-2 bg-red-600 text-white text-sm font-medium rounded-lg hover:bg-red-700 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Running...' : 'Run Stress Test'}
        </button>
      </form>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="space-y-4 mt-4">
          <div className="grid grid-cols-3 gap-4">
            <ResultCard label="Portfolio Before" value={`₹${Number(result.portfolioValueBefore).toLocaleString('en-IN')}`} />
            <ResultCard label="Portfolio After" value={`₹${Number(result.portfolioValueAfter).toLocaleString('en-IN')}`} />
            <ResultCard
              label="Impact"
              value={`₹${Number(result.portfolioImpact).toLocaleString('en-IN')}`}
              negative={Number(result.portfolioImpact) < 0}
            />
          </div>

          {result.warnings.length > 0 && (
            <div className="bg-amber-50 border border-amber-200 rounded-lg p-3 space-y-1">
              {result.warnings.map((w, i) => (
                <p key={i} className="text-sm text-amber-700">⚠️ {w}</p>
              ))}
            </div>
          )}

          {result.goalImpacts.length > 0 && (
            <div>
              <h4 className="text-sm font-medium text-gray-700 mb-2">Goal Impacts</h4>
              <div className="space-y-2">
                {result.goalImpacts.map((gi) => (
                  <div key={gi.goalId} className="flex items-center gap-3 text-sm">
                    <span className={`px-2 py-0.5 rounded text-xs font-medium ${
                      gi.status === 'at-risk' ? 'bg-red-100 text-red-700'
                        : gi.status === 'impacted' ? 'bg-amber-100 text-amber-700'
                        : 'bg-green-100 text-green-700'
                    }`}>
                      {gi.status}
                    </span>
                    <span className="text-gray-700">{gi.goalName}</span>
                    <span className="text-gray-400">
                      {Number(gi.fundingPercentBefore).toFixed(0)}% → {Number(gi.fundingPercentAfter).toFixed(0)}%
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
  const [prepayment, setPrepayment] = useState('500000');
  const [loanRate, setLoanRate] = useState('0.085');
  const [years, setYears] = useState('20');
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

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-6 space-y-5">
      <h3 className="text-lg font-semibold text-gray-800">📊 SIP vs Mortgage Prepayment</h3>
      <form onSubmit={handleCompare} className="flex flex-wrap gap-4 items-end">
        <InputField label="SIP Increase (₹/mo)" value={sipIncrease} onChange={setSipIncrease} />
        <InputField label="Prepayment (₹)" value={prepayment} onChange={setPrepayment} />
        <InputField label="Loan Rate" value={loanRate} onChange={setLoanRate} />
        <InputField label="Years Left" value={years} onChange={setYears} />
        <button
          type="submit"
          disabled={loading}
          className="px-5 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
        >
          {loading ? 'Comparing...' : 'Compare'}
        </button>
      </form>

      {error && <p className="text-sm text-red-600">{error}</p>}

      {result && (
        <div className="space-y-4 mt-4">
          <div className="grid grid-cols-2 gap-6">
            <ScenarioCard
              title={result.scenarioAName}
              outcome={result.scenarioA}
              color="indigo"
            />
            <ScenarioCard
              title={result.scenarioBName}
              outcome={result.scenarioB}
              color="emerald"
            />
          </div>

          <div className="bg-indigo-50 border border-indigo-200 rounded-lg p-4">
            <p className="text-sm font-medium text-indigo-800">💡 Recommendation</p>
            <p className="text-sm text-indigo-700 mt-1">{result.recommendation}</p>
          </div>

          <div>
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wider mb-2">Trade-offs</p>
            <ul className="space-y-1">
              {result.tradeoffs.map((t, i) => (
                <li key={i} className="text-sm text-gray-600 flex items-start gap-2">
                  <span className="text-gray-400 mt-0.5">•</span> {t}
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
}

function ScenarioCard({
  title, outcome, color,
}: {
  title: string;
  outcome: { advantages: string[]; disadvantages: string[] };
  color: string;
}) {
  const borderColor = color === 'indigo' ? 'border-indigo-200' : 'border-emerald-200';
  const headerBg = color === 'indigo' ? 'bg-indigo-50' : 'bg-emerald-50';

  return (
    <div className={`border ${borderColor} rounded-lg overflow-hidden`}>
      <div className={`${headerBg} px-4 py-2`}>
        <h4 className="text-sm font-semibold text-gray-800">{title}</h4>
      </div>
      <div className="p-4 space-y-3 text-sm">
        {outcome.advantages.length > 0 && (
          <div>
            <p className="text-xs font-medium text-green-600 mb-1">Advantages</p>
            {outcome.advantages.map((a, i) => (
              <p key={i} className="text-gray-600 text-xs">✅ {a}</p>
            ))}
          </div>
        )}
        {outcome.disadvantages.length > 0 && (
          <div>
            <p className="text-xs font-medium text-red-600 mb-1">Disadvantages</p>
            {outcome.disadvantages.map((d, i) => (
              <p key={i} className="text-gray-600 text-xs">⚠️ {d}</p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

function InputField({ label, value, onChange }: { label: string; value: string; onChange: (v: string) => void }) {
  return (
    <div>
      <label className="block text-xs font-medium text-gray-600 mb-1">{label}</label>
      <input
        type="number"
        step="any"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className="w-36 px-3 py-2 border border-gray-300 rounded-lg text-sm"
      />
    </div>
  );
}

function ResultCard({ label, value, negative }: { label: string; value: string; negative?: boolean }) {
  return (
    <div className={`rounded-lg border p-3 ${negative ? 'bg-red-50 border-red-200' : 'bg-gray-50 border-gray-200'}`}>
      <p className="text-xs text-gray-500">{label}</p>
      <p className={`text-sm font-bold mt-0.5 ${negative ? 'text-red-700' : 'text-gray-800'}`}>{value}</p>
    </div>
  );
}
