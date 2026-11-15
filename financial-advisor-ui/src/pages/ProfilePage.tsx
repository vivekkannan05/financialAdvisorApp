import { useEffect, useState } from 'react';
import { fetchProfile, updateProfile, createProfile } from '@/api/profile';
import InfoTooltip from '@/components/InfoTooltip';
import type { ProfileSnapshot } from '@/types/api';

interface ProfileData {
  personal: { age: number; retirementAge: number; riskTolerance: string; city: string; dependents: number };
  income: { monthlySalary: number; otherIncome: number };
  expenses: { rent: number; groceries: number; utilities: number; transportation: number; entertainment: number; other: number };
  assets: { savingsAccount: number; fixedDeposits: number; mutualFunds: number; stocks: number; ppf: number; epf: number; gold: number };
  liabilities: {
    homeLoan: { outstanding: number; emi: number; rateOfInterest: number; remainingMonths: number };
    carLoan: { outstanding: number; emi: number; rateOfInterest: number; remainingMonths: number };
  };
  insurance: { termLifeCover: number; termLifePremium: number; healthCover: number; healthPremium: number };
  investments: { monthlySIP: number; expectedReturn: number };
  goals: Array<{ name: string; targetAmount: number; targetYear: number; priority: string }>;
}

const defaultData: ProfileData = {
  personal: { age: 30, retirementAge: 60, riskTolerance: 'MODERATE', city: '', dependents: 0 },
  income: { monthlySalary: 0, otherIncome: 0 },
  expenses: { rent: 0, groceries: 0, utilities: 0, transportation: 0, entertainment: 0, other: 0 },
  assets: { savingsAccount: 0, fixedDeposits: 0, mutualFunds: 0, stocks: 0, ppf: 0, epf: 0, gold: 0 },
  liabilities: {
    homeLoan: { outstanding: 0, emi: 0, rateOfInterest: 0, remainingMonths: 0 },
    carLoan: { outstanding: 0, emi: 0, rateOfInterest: 0, remainingMonths: 0 },
  },
  insurance: { termLifeCover: 0, termLifePremium: 0, healthCover: 0, healthPremium: 0 },
  investments: { monthlySIP: 0, expectedReturn: 12 },
  goals: [],
};

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileSnapshot | null>(null);
  const [data, setData] = useState<ProfileData>(defaultData);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [activeSection, setActiveSection] = useState('personal');

  useEffect(() => {
    fetchProfile()
      .then((p) => {
        setProfile(p);
        setData(mergeData(defaultData, p.data as Partial<ProfileData>));
      })
      .catch(() => {})
      .finally(() => setLoading(false));
  }, []);

  function mergeData(base: ProfileData, incoming: Partial<ProfileData>): ProfileData {
    return {
      personal: { ...base.personal, ...incoming?.personal },
      income: { ...base.income, ...incoming?.income },
      expenses: { ...base.expenses, ...incoming?.expenses },
      assets: { ...base.assets, ...incoming?.assets },
      liabilities: {
        homeLoan: { ...base.liabilities.homeLoan, ...incoming?.liabilities?.homeLoan },
        carLoan: { ...base.liabilities.carLoan, ...incoming?.liabilities?.carLoan },
      },
      insurance: { ...base.insurance, ...incoming?.insurance },
      investments: { ...base.investments, ...incoming?.investments },
      goals: incoming?.goals ?? base.goals,
    };
  }

  async function handleSave() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const payload = data as unknown as Record<string, unknown>;
      const saved = profile ? await updateProfile(payload) : await createProfile(payload);
      setProfile(saved);
      setSuccess('Saved successfully! Your dashboard will now show updated numbers. 🎉');
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Save failed';
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  function updateField(section: keyof ProfileData, field: string, value: number | string, subSection?: string) {
    setData((prev) => {
      const updated = { ...prev };
      if (subSection && section === 'liabilities') {
        updated.liabilities = {
          ...prev.liabilities,
          [subSection]: { ...(prev.liabilities as Record<string, Record<string, number>>)[subSection], [field]: value },
        };
      } else {
        (updated[section] as Record<string, unknown>)[field] = value;
      }
      return updated;
    });
  }

  function addGoal() {
    setData((prev) => ({
      ...prev,
      goals: [...prev.goals, { name: '', targetAmount: 0, targetYear: new Date().getFullYear() + 5, priority: 'MEDIUM' }],
    }));
  }

  function removeGoal(index: number) {
    setData((prev) => ({ ...prev, goals: prev.goals.filter((_, i) => i !== index) }));
  }

  function updateGoal(index: number, field: string, value: string | number) {
    setData((prev) => ({
      ...prev,
      goals: prev.goals.map((g, i) => (i === index ? { ...g, [field]: value } : g)),
    }));
  }

  if (loading) {
    return (
      <div className="p-8 flex flex-col items-center justify-center h-full gap-4">
        <div className="text-5xl animate-bounce">📝</div>
        <p className="text-gray-400 dark:text-gray-500 text-sm animate-pulse">Loading your profile...</p>
      </div>
    );
  }

  const sections = [
    { id: 'personal', icon: '👤', label: 'About You' },
    { id: 'income', icon: '💰', label: 'Income' },
    { id: 'expenses', icon: '🛒', label: 'Expenses' },
    { id: 'assets', icon: '🏦', label: 'Savings' },
    { id: 'liabilities', icon: '💳', label: 'Loans' },
    { id: 'insurance', icon: '🛡️', label: 'Insurance' },
    { id: 'investments', icon: '📈', label: 'Investments' },
    { id: 'goals', icon: '🎯', label: 'Goals' },
  ];

  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
            📝 My Money Information
          </h2>
          <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">
            Fill in your details below. Don't worry — you can always change them later!
          </p>
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white
                     font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700
                     disabled:opacity-50 transition-all shadow-md hover:shadow-lg
                     active:scale-[0.98] text-sm"
        >
          {saving ? '⏳ Saving...' : profile ? '💾 Update Profile' : '✨ Create Profile'}
        </button>
      </div>

      {error && (
        <div className="flex items-start gap-2 bg-red-50 dark:bg-red-950/40 border border-red-200
                        dark:border-red-800 text-red-700 dark:text-red-400 rounded-xl px-4 py-3 text-sm">
          <span>❌</span> {error}
        </div>
      )}
      {success && (
        <div className="flex items-start gap-2 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200
                        dark:border-emerald-800 text-emerald-700 dark:text-emerald-400 rounded-xl px-4 py-3 text-sm">
          <span>✅</span> {success}
        </div>
      )}

      {/* Section tabs */}
      <div className="flex flex-wrap gap-2">
        {sections.map((s) => (
          <button
            key={s.id}
            onClick={() => setActiveSection(s.id)}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeSection === s.id
                ? 'bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-400 border-2 border-indigo-300 dark:border-indigo-700 shadow-sm'
                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700'
            }`}
          >
            {s.icon} {s.label}
          </button>
        ))}
      </div>

      {/* Form sections */}
      <div className="bg-white dark:bg-gray-800/80 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 md:p-8">
        {activeSection === 'personal' && (
          <FormSection title="About You" hint="Basic info that helps us give better advice" icon="👤">
            <FormRow>
              <NumberField label="Your Age" value={data.personal.age} onChange={(v) => updateField('personal', 'age', v)}
                tooltip="How old are you? This helps us plan for retirement and set the right risk level for investments." />
              <NumberField label="Retirement Age" value={data.personal.retirementAge} onChange={(v) => updateField('personal', 'retirementAge', v)}
                tooltip="At what age do you want to stop working? In India, most people plan for 58-65. Earlier retirement needs more savings!" />
            </FormRow>
            <FormRow>
              <TextField label="City" value={data.personal.city} onChange={(v) => updateField('personal', 'city', v)}
                tooltip="Which city do you live in? Cost of living varies a lot between cities like Mumbai, Bangalore, and smaller towns." placeholder="e.g. Bangalore" />
              <NumberField label="Dependents" value={data.personal.dependents} onChange={(v) => updateField('personal', 'dependents', v)}
                tooltip="How many people depend on your income? (spouse, children, parents). More dependents = need more insurance and emergency fund." />
            </FormRow>
            <div>
              <div className="flex items-center gap-1 mb-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Risk Comfort Level</label>
                <InfoTooltip text="How comfortable are you with your investments going up and down? Conservative = safe but slow growth. Aggressive = faster growth but can lose money temporarily.">
                  <span />
                </InfoTooltip>
              </div>
              <div className="flex gap-3 flex-wrap">
                {['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'].map((r) => (
                  <button key={r} type="button"
                    onClick={() => updateField('personal', 'riskTolerance', r)}
                    className={`px-4 py-2.5 rounded-xl text-sm font-medium transition-all ${
                      data.personal.riskTolerance === r
                        ? r === 'CONSERVATIVE' ? 'bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-400 border-2 border-blue-300 dark:border-blue-700'
                          : r === 'MODERATE' ? 'bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-400 border-2 border-amber-300 dark:border-amber-700'
                          : 'bg-red-100 dark:bg-red-900/50 text-red-700 dark:text-red-400 border-2 border-red-300 dark:border-red-700'
                        : 'bg-gray-50 dark:bg-gray-700 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-600 hover:bg-gray-100 dark:hover:bg-gray-600'
                    }`}>
                    {r === 'CONSERVATIVE' ? '🐢 Safe & Slow' : r === 'MODERATE' ? '⚖️ Balanced' : '🚀 Risky & Fast'}
                  </button>
                ))}
              </div>
            </div>
          </FormSection>
        )}

        {activeSection === 'income' && (
          <FormSection title="Monthly Income" hint="How much money comes in every month (before tax)" icon="💰">
            <FormRow>
              <NumberField label="Monthly Salary" value={data.income.monthlySalary} prefix="₹" onChange={(v) => updateField('income', 'monthlySalary', v)}
                tooltip="Your monthly take-home salary (after TDS/tax deduction). This is the amount that hits your bank account." />
              <NumberField label="Other Income" value={data.income.otherIncome} prefix="₹" onChange={(v) => updateField('income', 'otherIncome', v)}
                tooltip="Rent from property, freelance work, dividends, interest — any extra money you earn regularly each month." />
            </FormRow>
            <div className="mt-4 p-4 bg-indigo-50 dark:bg-indigo-950/40 rounded-xl">
              <p className="text-sm text-indigo-700 dark:text-indigo-400">
                💡 <strong>Total Monthly Income:</strong> ₹{(data.income.monthlySalary + data.income.otherIncome).toLocaleString('en-IN')}
              </p>
            </div>
          </FormSection>
        )}

        {activeSection === 'expenses' && (
          <FormSection title="Monthly Expenses" hint="How much you spend each month (don't count EMIs or insurance — those are separate)" icon="🛒">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <NumberField label="🏠 Rent / Housing" value={data.expenses.rent} prefix="₹" onChange={(v) => updateField('expenses', 'rent', v)}
                tooltip="Monthly rent or housing maintenance charges. If you own your home and have no rent, put 0." />
              <NumberField label="🍎 Groceries & Food" value={data.expenses.groceries} prefix="₹" onChange={(v) => updateField('expenses', 'groceries', v)}
                tooltip="Monthly spending on groceries, vegetables, fruits, eating out, ordering food. Include everything food-related!" />
              <NumberField label="💡 Bills & Utilities" value={data.expenses.utilities} prefix="₹" onChange={(v) => updateField('expenses', 'utilities', v)}
                tooltip="Electricity, water, internet, phone bill, gas, DTH — all your monthly utility bills combined." />
              <NumberField label="🚗 Transportation" value={data.expenses.transportation} prefix="₹" onChange={(v) => updateField('expenses', 'transportation', v)}
                tooltip="Petrol/diesel, Uber/Ola rides, metro pass, bus tickets — how you get around each month." />
              <NumberField label="🎬 Fun & Entertainment" value={data.expenses.entertainment} prefix="₹" onChange={(v) => updateField('expenses', 'entertainment', v)}
                tooltip="Movies, subscriptions (Netflix, Spotify), shopping, hobbies, outings — money spent on having fun!" />
              <NumberField label="📦 Other Expenses" value={data.expenses.other} prefix="₹" onChange={(v) => updateField('expenses', 'other', v)}
                tooltip="Anything else — medical, education, clothes, gifts, personal care. If it doesn't fit above, put it here." />
            </div>
            <div className="mt-4 p-4 bg-orange-50 dark:bg-orange-950/40 rounded-xl">
              <p className="text-sm text-orange-700 dark:text-orange-400">
                💡 <strong>Total Monthly Expenses:</strong> ₹{Object.values(data.expenses).reduce((a, b) => a + b, 0).toLocaleString('en-IN')}
              </p>
            </div>
          </FormSection>
        )}

        {activeSection === 'assets' && (
          <FormSection title="Your Savings & Investments" hint="Everything you've saved or invested — the total value today" icon="🏦">
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <NumberField label="🏦 Savings Account" value={data.assets.savingsAccount} prefix="₹" onChange={(v) => updateField('assets', 'savingsAccount', v)}
                tooltip="Total balance in all your savings bank accounts combined. This is your most accessible money." />
              <NumberField label="🔒 Fixed Deposits (FD)" value={data.assets.fixedDeposits} prefix="₹" onChange={(v) => updateField('assets', 'fixedDeposits', v)}
                tooltip="Total value of all your FDs in banks. FDs give guaranteed returns but money is locked for a period." />
              <NumberField label="📊 Mutual Funds" value={data.assets.mutualFunds} prefix="₹" onChange={(v) => updateField('assets', 'mutualFunds', v)}
                tooltip="Current value of all your mutual fund investments (SIPs + lump sum). Check your fund app for today's value." />
              <NumberField label="📈 Stocks / Shares" value={data.assets.stocks} prefix="₹" onChange={(v) => updateField('assets', 'stocks', v)}
                tooltip="Current value of shares you directly own on the stock market (Zerodha, Groww, etc). Check your demat account." />
              <NumberField label="🏛️ PPF" value={data.assets.ppf} prefix="₹" onChange={(v) => updateField('assets', 'ppf', v)}
                tooltip="Public Provident Fund — a government savings scheme with tax benefits. Money is locked for 15 years but gives guaranteed returns." />
              <NumberField label="🏢 EPF / PF" value={data.assets.epf} prefix="₹" onChange={(v) => updateField('assets', 'epf', v)}
                tooltip="Employee Provident Fund — money your company deducts from salary + their contribution. Check your EPFO passbook online." />
              <NumberField label="✨ Gold" value={data.assets.gold} prefix="₹" onChange={(v) => updateField('assets', 'gold', v)}
                tooltip="Value of gold you own — jewellery, coins, digital gold, Sovereign Gold Bonds. Use today's market price." />
            </div>
            <div className="mt-4 p-4 bg-emerald-50 dark:bg-emerald-950/40 rounded-xl">
              <p className="text-sm text-emerald-700 dark:text-emerald-400">
                💡 <strong>Total Savings:</strong> ₹{Object.values(data.assets).reduce((a, b) => a + b, 0).toLocaleString('en-IN')}
              </p>
            </div>
          </FormSection>
        )}

        {activeSection === 'liabilities' && (
          <FormSection title="Your Loans" hint="Any money you owe — EMIs, outstanding amounts. Leave 0 if you don't have a loan." icon="💳">
            <LoanSection title="🏠 Home Loan" data={data.liabilities.homeLoan}
              onChange={(field, val) => updateField('liabilities', field, val, 'homeLoan')} />
            <div className="border-t border-gray-100 dark:border-gray-700 my-6" />
            <LoanSection title="🚗 Car Loan" data={data.liabilities.carLoan}
              onChange={(field, val) => updateField('liabilities', field, val, 'carLoan')} />
          </FormSection>
        )}

        {activeSection === 'insurance' && (
          <FormSection title="Insurance Policies" hint="Insurance protects you and your family. Enter what you have." icon="🛡️">
            <FormRow>
              <NumberField label="Term Life Cover Amount" value={data.insurance.termLifeCover} prefix="₹" onChange={(v) => updateField('insurance', 'termLifeCover', v)}
                tooltip="How much money your family gets if something happens to you. Experts say you need 10-15x your annual income. E.g., if you earn ₹20L/year, get ₹2-3 Cr cover." />
              <NumberField label="Term Life Premium (monthly)" value={data.insurance.termLifePremium} prefix="₹" onChange={(v) => updateField('insurance', 'termLifePremium', v)}
                tooltip="How much you pay each month for your term life insurance. If you pay yearly, divide by 12." />
            </FormRow>
            <FormRow>
              <NumberField label="Health Insurance Cover" value={data.insurance.healthCover} prefix="₹" onChange={(v) => updateField('insurance', 'healthCover', v)}
                tooltip="Maximum amount your health insurance will pay for hospital bills. Get at least ₹10-20 lakh for a family. Hospital costs are rising fast!" />
              <NumberField label="Health Premium (monthly)" value={data.insurance.healthPremium} prefix="₹" onChange={(v) => updateField('insurance', 'healthPremium', v)}
                tooltip="Monthly cost of your health insurance. If you pay yearly, divide by 12." />
            </FormRow>
          </FormSection>
        )}

        {activeSection === 'investments' && (
          <FormSection title="Monthly Investments" hint="Money you invest regularly every month" icon="📈">
            <FormRow>
              <NumberField label="Monthly SIP Amount" value={data.investments.monthlySIP} prefix="₹" onChange={(v) => updateField('investments', 'monthlySIP', v)}
                tooltip="SIP = Systematic Investment Plan. It's money automatically invested in mutual funds every month. This is the best way to build wealth over time!" />
              <NumberField label="Expected Return (%)" value={data.investments.expectedReturn} onChange={(v) => updateField('investments', 'expectedReturn', v)}
                tooltip="What % return do you expect per year? Stocks/MFs: 10-12%. FDs: 6-7%. PPF: 7-8%. Don't be too optimistic — use realistic numbers." />
            </FormRow>
          </FormSection>
        )}

        {activeSection === 'goals' && (
          <FormSection title="Your Money Goals" hint="What are you saving for? Add all your dreams here!" icon="🎯">
            {data.goals.length === 0 && (
              <div className="text-center py-8 text-gray-400 dark:text-gray-500">
                <span className="text-4xl block mb-3">🎯</span>
                <p className="text-sm">No goals yet! Add what you're saving for.</p>
              </div>
            )}
            {data.goals.map((goal, i) => (
              <div key={i} className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl mb-4 relative">
                <button onClick={() => removeGoal(i)}
                  className="absolute top-3 right-3 w-7 h-7 rounded-full bg-red-100 dark:bg-red-900/50
                             text-red-600 dark:text-red-400 text-xs font-bold flex items-center justify-center
                             hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors">
                  ✕
                </button>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Goal Name</label>
                    <input type="text" value={goal.name} onChange={(e) => updateGoal(i, 'name', e.target.value)}
                      placeholder="e.g. Buy a House, Child's Education"
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                                 bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Target Amount (₹)</label>
                    <input type="number" value={goal.targetAmount} onChange={(e) => updateGoal(i, 'targetAmount', Number(e.target.value))}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                                 bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Target Year</label>
                    <input type="number" value={goal.targetYear} onChange={(e) => updateGoal(i, 'targetYear', Number(e.target.value))}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                                 bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Priority</label>
                    <select value={goal.priority} onChange={(e) => updateGoal(i, 'priority', e.target.value)}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                                 bg-white dark:bg-gray-800 dark:text-gray-100">
                      <option value="HIGH">🔴 High — Must achieve</option>
                      <option value="MEDIUM">🟡 Medium — Important</option>
                      <option value="LOW">🟢 Low — Nice to have</option>
                    </select>
                  </div>
                </div>
              </div>
            ))}
            <button onClick={addGoal} type="button"
              className="w-full py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                         text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                         hover:text-indigo-600 dark:hover:text-indigo-400
                         hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30
                         transition-all font-medium">
              ➕ Add a New Goal
            </button>
          </FormSection>
        )}
      </div>
    </div>
  );
}

function FormSection({ title, hint, icon, children }: { title: string; hint: string; icon: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="mb-6">
        <h3 className="text-lg font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">
          {icon} {title}
        </h3>
        <p className="text-sm text-gray-400 dark:text-gray-500 mt-0.5">{hint}</p>
      </div>
      <div className="space-y-4">{children}</div>
    </div>
  );
}

function FormRow({ children }: { children: React.ReactNode }) {
  return <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">{children}</div>;
}

function NumberField({ label, value, onChange, tooltip, prefix, placeholder }: {
  label: string; value: number; onChange: (v: number) => void; tooltip: string;
  prefix?: string; placeholder?: string;
}) {
  return (
    <div>
      <div className="flex items-center gap-1 mb-1.5">
        <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
        <InfoTooltip text={tooltip}><span /></InfoTooltip>
      </div>
      <div className="relative">
        {prefix && (
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-400 dark:text-gray-500 font-medium">{prefix}</span>
        )}
        <input
          type="number"
          value={value || ''}
          onChange={(e) => onChange(Number(e.target.value) || 0)}
          placeholder={placeholder || '0'}
          className={`w-full ${prefix ? 'pl-8' : 'pl-3'} pr-3 py-2.5 border border-gray-200 dark:border-gray-600
                     rounded-xl text-sm focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none
                     bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all`}
        />
      </div>
    </div>
  );
}

function TextField({ label, value, onChange, tooltip, placeholder }: {
  label: string; value: string; onChange: (v: string) => void; tooltip: string; placeholder?: string;
}) {
  return (
    <div>
      <div className="flex items-center gap-1 mb-1.5">
        <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
        <InfoTooltip text={tooltip}><span /></InfoTooltip>
      </div>
      <input
        type="text"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                   focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none
                   bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all"
      />
    </div>
  );
}

function LoanSection({ title, data, onChange }: {
  title: string;
  data: { outstanding: number; emi: number; rateOfInterest: number; remainingMonths: number };
  onChange: (field: string, value: number) => void;
}) {
  return (
    <div>
      <h4 className="text-sm font-bold text-gray-700 dark:text-gray-300 mb-3">{title}</h4>
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <NumberField label="Outstanding Amount" value={data.outstanding} prefix="₹" onChange={(v) => onChange('outstanding', v)}
          tooltip="How much you still owe on this loan. Check your loan statement or banking app for the current outstanding principal." />
        <NumberField label="Monthly EMI" value={data.emi} prefix="₹" onChange={(v) => onChange('emi', v)}
          tooltip="EMI = Equated Monthly Installment. The fixed amount you pay every month for this loan." />
        <NumberField label="Interest Rate (%)" value={data.rateOfInterest} onChange={(v) => onChange('rateOfInterest', v)}
          tooltip="The annual interest rate on your loan. Home loans are usually 8-10%, car loans 9-12%." />
        <NumberField label="Months Remaining" value={data.remainingMonths} onChange={(v) => onChange('remainingMonths', v)}
          tooltip="How many more monthly EMI payments until the loan is fully paid off." />
      </div>
    </div>
  );
}
