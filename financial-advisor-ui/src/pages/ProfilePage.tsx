import { useEffect, useState, useRef } from 'react';
import { fetchProfile, updateProfile, createProfile } from '@/api/profile';
import InfoTooltip from '@/components/InfoTooltip';
import type { ProfileSnapshot } from '@/types/api';

/* ── Shared dynamic-row item ──────────────────────────────── */
interface DynamicItem {
  id: string;
  category: string;
  amount: number;
}

interface ProfileData {
  personal: { age: number; retirementAge: number; riskTolerance: string; city: string; dependents: number };
  income: { monthlySalary: number; otherIncome: number };
  expenses: DynamicItem[];
  assets: DynamicItem[];
  liabilities: {
    homeLoan: { outstanding: number; emi: number; rateOfInterest: number; remainingMonths: number };
    carLoan: { outstanding: number; emi: number; rateOfInterest: number; remainingMonths: number };
  };
  insurance: { termLifeCover: number; termLifePremium: number; healthCover: number; healthPremium: number };
  investments: { monthlySIP: number; expectedReturn: number };
  goals: Array<{ name: string; targetAmount: number; targetYear: number; priority: string }>;
}

/* ══════════════════════════════════════════════════════════════
   EXPENSE constants
   ══════════════════════════════════════════════════════════════ */
const EXPENSE_SUGGESTIONS = [
  'Rent / Housing', 'Groceries & Food', 'Bills & Utilities', 'Transportation',
  'Fun & Entertainment', 'Other Expenses', 'Education & Books', 'Medical / Health',
  'Clothing', 'Household Items', 'Gifts & Donations', 'Childcare',
  'Personal Care', 'Subscriptions', 'Eating Out', 'Travel',
  'Gym / Fitness', 'Maintenance & Repairs',
];

const EXPENSE_EMOJI: Record<string, string> = {
  'Rent / Housing': '🏠', 'Groceries & Food': '🍎', 'Bills & Utilities': '💡',
  'Transportation': '🚗', 'Fun & Entertainment': '🎬', 'Other Expenses': '📦',
  'Education & Books': '📚', 'Medical / Health': '🏥', 'Clothing': '👕',
  'Household Items': '🏡', 'Gifts & Donations': '🎁', 'Childcare': '👶',
  'Personal Care': '💇', 'Subscriptions': '📱', 'Eating Out': '🍕',
  'Travel': '✈️', 'Gym / Fitness': '🏋️', 'Maintenance & Repairs': '🔧',
};

const EXPENSE_TOOLTIPS: Record<string, string> = {
  'Rent / Housing': 'Monthly rent or housing maintenance charges. If you own your home, put 0.',
  'Groceries & Food': 'Monthly spending on groceries, vegetables, fruits, eating out.',
  'Bills & Utilities': 'Electricity, water, internet, phone bill, gas, DTH — all monthly bills.',
  'Transportation': 'Petrol/diesel, Uber/Ola rides, metro pass, bus tickets.',
  'Fun & Entertainment': 'Movies, Netflix, Spotify, shopping, hobbies, outings.',
  'Other Expenses': 'Anything that doesn\'t fit other categories.',
  'Education & Books': 'Tuition fees, books, online courses, coaching classes.',
  'Medical / Health': 'Doctor visits, medicines, tests (not insurance premiums).',
  'Clothing': 'Monthly average spending on clothes and accessories.',
  'Household Items': 'Cleaning supplies, kitchenware, furniture upkeep.',
  'Eating Out': 'Restaurants, ordering food, cafés, tea/coffee outside.',
};

const EXPENSE_KEY_MAP: Record<string, string> = {
  rent: 'Rent / Housing', groceries: 'Groceries & Food', utilities: 'Bills & Utilities',
  transportation: 'Transportation', entertainment: 'Fun & Entertainment', other: 'Other Expenses',
};

/* ══════════════════════════════════════════════════════════════
   ASSET / SAVINGS constants
   ══════════════════════════════════════════════════════════════ */
const ASSET_SUGGESTIONS = [
  'Savings Account', 'Fixed Deposits (FD)', 'Mutual Funds', 'Stocks / Shares',
  'PPF', 'EPF / PF', 'Gold', 'NPS', 'Real Estate', 'Recurring Deposits (RD)',
  'Sovereign Gold Bonds', 'Corporate Bonds', 'Digital Gold', 'Cash in Hand',
  'ULIP', 'Post Office Savings', 'Cryptocurrency', 'Other Investments',
];

const ASSET_EMOJI: Record<string, string> = {
  'Savings Account': '🏦', 'Fixed Deposits (FD)': '🔒', 'Mutual Funds': '📊',
  'Stocks / Shares': '📈', 'PPF': '🏛️', 'EPF / PF': '🏢', 'Gold': '✨',
  'NPS': '🧓', 'Real Estate': '🏘️', 'Recurring Deposits (RD)': '🔄',
  'Sovereign Gold Bonds': '🥇', 'Corporate Bonds': '🏗️', 'Digital Gold': '💛',
  'Cash in Hand': '💵', 'ULIP': '📋', 'Post Office Savings': '📮',
  'Cryptocurrency': '₿', 'Other Investments': '📦',
};

const ASSET_TOOLTIPS: Record<string, string> = {
  'Savings Account': 'Total balance in all your savings bank accounts. This is your most accessible money.',
  'Fixed Deposits (FD)': 'Total value of all your FDs. FDs give guaranteed returns but money is locked for a period.',
  'Mutual Funds': 'Current value of all mutual fund investments (SIPs + lump sum). Check your fund app for today\'s value.',
  'Stocks / Shares': 'Current value of shares you directly own (Zerodha, Groww, etc). Check your demat account.',
  'PPF': 'Public Provident Fund — government savings scheme with tax benefits. Locked for 15 years, guaranteed returns.',
  'EPF / PF': 'Employee Provident Fund — salary deduction + employer contribution. Check EPFO passbook online.',
  'Gold': 'Value of gold you own — jewellery, coins, digital gold, Sovereign Gold Bonds. Use today\'s market price.',
  'NPS': 'National Pension System — retirement savings scheme. Partially locked until 60, gives market-linked returns.',
  'Real Estate': 'Current market value of property you own (excluding your primary home if you have a home loan listed).',
  'Recurring Deposits (RD)': 'Total value of your RDs. Like an FD but you deposit a fixed amount every month.',
  'Sovereign Gold Bonds': 'Government gold bonds — earn interest on gold! Check current NAV on RBI website.',
  'Cryptocurrency': 'Current value of crypto holdings (Bitcoin, Ethereum, etc). Very volatile — be careful!',
  'Cash in Hand': 'Physical cash you keep at home or in a locker.',
  'Other Investments': 'Any other savings or investments not listed above.',
};

const ASSET_KEY_MAP: Record<string, string> = {
  savingsAccount: 'Savings Account', fixedDeposits: 'Fixed Deposits (FD)',
  mutualFunds: 'Mutual Funds', stocks: 'Stocks / Shares',
  ppf: 'PPF', epf: 'EPF / PF', gold: 'Gold', nps: 'NPS',
  realEstate: 'Real Estate', recurringDeposits: 'Recurring Deposits (RD)',
};

/* ══════════════════════════════════════════════════════════════
   Shared helpers
   ══════════════════════════════════════════════════════════════ */
function uid(): string {
  return Math.random().toString(36).slice(2, 10);
}

function loadCustom(key: string): string[] {
  try { return JSON.parse(localStorage.getItem(key) || '[]'); }
  catch { return []; }
}

function saveCustom(key: string, defaults: string[], cat: string) {
  if (!cat || defaults.includes(cat)) return;
  const custom = loadCustom(key);
  if (!custom.includes(cat)) {
    localStorage.setItem(key, JSON.stringify([...custom, cat]));
  }
}

function objectToItems(obj: Record<string, unknown>, keyMap: Record<string, string>): DynamicItem[] {
  return Object.entries(obj)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([key, value]) => ({
      id: uid(),
      category: keyMap[key] || key,
      amount: Number(value) || 0,
    }));
}

function arrayToItems(arr: DynamicItem[]): DynamicItem[] {
  return arr.map((e) => ({
    id: e.id || uid(),
    category: String(e.category || ''),
    amount: Number(e.amount) || 0,
  }));
}

function parseItems(raw: unknown, keyMap: Record<string, string>): DynamicItem[] {
  if (!raw) return [];
  if (Array.isArray(raw)) return arrayToItems(raw as DynamicItem[]);
  return objectToItems(raw as Record<string, unknown>, keyMap);
}

function itemsToObject(items: DynamicItem[], storageKey: string, defaults: string[]): Record<string, number> {
  const obj: Record<string, number> = {};
  items.forEach((e) => {
    if (e.category.trim() && e.amount > 0) {
      obj[e.category.trim()] = e.amount;
      saveCustom(storageKey, defaults, e.category.trim());
    }
  });
  return obj;
}

/* ── Default data ─────────────────────────────────────────── */
const defaultData: ProfileData = {
  personal: { age: 30, retirementAge: 60, riskTolerance: 'MODERATE', city: '', dependents: 0 },
  income: { monthlySalary: 0, otherIncome: 0 },
  expenses: [],
  assets: [],
  liabilities: {
    homeLoan: { outstanding: 0, emi: 0, rateOfInterest: 0, remainingMonths: 0 },
    carLoan: { outstanding: 0, emi: 0, rateOfInterest: 0, remainingMonths: 0 },
  },
  insurance: { termLifeCover: 0, termLifePremium: 0, healthCover: 0, healthPremium: 0 },
  investments: { monthlySIP: 0, expectedReturn: 12 },
  goals: [],
};

/* ══════════════════════════════════════════════════════════════
   Main component
   ══════════════════════════════════════════════════════════════ */
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

  /* ── Data helpers ───────────────────────────────────────── */
  function coerceNumbers<T extends Record<string, unknown>>(base: T, incoming?: Record<string, unknown>): T {
    if (!incoming) return base;
    const result = { ...base };
    for (const key of Object.keys(result)) {
      if (key in incoming) {
        result[key as keyof T] = (typeof base[key] === 'number'
          ? Number(incoming[key]) || 0
          : incoming[key]) as T[keyof T];
      }
    }
    return result;
  }

  function mergeData(base: ProfileData, incoming: Partial<ProfileData>): ProfileData {
    return {
      personal: {
        ...coerceNumbers(base.personal, incoming?.personal as Record<string, unknown>),
        riskTolerance: (incoming?.personal?.riskTolerance as string) ?? base.personal.riskTolerance,
        city: (incoming?.personal?.city as string) ?? base.personal.city,
      },
      income: coerceNumbers(base.income, incoming?.income as Record<string, unknown>),
      expenses: parseItems(incoming?.expenses, EXPENSE_KEY_MAP),
      assets: parseItems(incoming?.assets, ASSET_KEY_MAP),
      liabilities: {
        homeLoan: coerceNumbers(base.liabilities.homeLoan, incoming?.liabilities?.homeLoan as Record<string, unknown>),
        carLoan: coerceNumbers(base.liabilities.carLoan, incoming?.liabilities?.carLoan as Record<string, unknown>),
      },
      insurance: coerceNumbers(base.insurance, incoming?.insurance as Record<string, unknown>),
      investments: coerceNumbers(base.investments, incoming?.investments as Record<string, unknown>),
      goals: incoming?.goals ?? base.goals,
    };
  }

  /* ── Save handler ───────────────────────────────────────── */
  async function handleSave() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const payload = {
        ...data,
        expenses: itemsToObject(data.expenses, 'fa-custom-expense-categories', EXPENSE_SUGGESTIONS),
        assets: itemsToObject(data.assets, 'fa-custom-asset-categories', ASSET_SUGGESTIONS),
      } as unknown as Record<string, unknown>;
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

  /* ── Field updaters ─────────────────────────────────────── */
  function updateField(section: keyof ProfileData, field: string, value: number | string, subSection?: string) {
    setData((prev) => {
      const updated = { ...prev };
      if (subSection && section === 'liabilities') {
        updated.liabilities = {
          ...prev.liabilities,
          [subSection]: { ...(prev.liabilities as Record<string, Record<string, number>>)[subSection], [field]: value },
        };
      } else if (section !== 'expenses' && section !== 'assets') {
        (updated[section] as Record<string, unknown>)[field] = value;
      }
      return updated;
    });
  }

  /* ── Dynamic-list mutators (shared for expenses & assets) ── */
  function addItem(list: 'expenses' | 'assets') {
    setData((prev) => ({
      ...prev,
      [list]: [...prev[list], { id: uid(), category: '', amount: 0 }],
    }));
  }

  function removeItem(list: 'expenses' | 'assets', id: string) {
    setData((prev) => ({
      ...prev,
      [list]: prev[list].filter((e) => e.id !== id),
    }));
  }

  function updateItemCategory(list: 'expenses' | 'assets', id: string, category: string) {
    setData((prev) => ({
      ...prev,
      [list]: prev[list].map((e) => (e.id === id ? { ...e, category } : e)),
    }));
  }

  function updateItemAmount(list: 'expenses' | 'assets', id: string, amount: number) {
    setData((prev) => ({
      ...prev,
      [list]: prev[list].map((e) => (e.id === id ? { ...e, amount } : e)),
    }));
  }

  /* ── Goal mutators ──────────────────────────────────────── */
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

  /* ── Loading state ──────────────────────────────────────── */
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

  const usedExpenseCategories = data.expenses.map((e) => e.category).filter(Boolean);
  const expenseTotal = data.expenses.reduce((sum, e) => sum + e.amount, 0);

  const usedAssetCategories = data.assets.map((a) => a.category).filter(Boolean);
  const assetTotal = data.assets.reduce((sum, a) => sum + a.amount, 0);

  /* ══════════════════════════════════════════════════════════
     Render
     ══════════════════════════════════════════════════════════ */
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

        {/* ────────── About You ────────── */}
        {activeSection === 'personal' && (
          <FormSection title="About You" hint="Basic info that helps us give better advice" icon="👤">
            <FormRow>
              <NumberField label="Your Age" value={data.personal.age} onChange={(v) => updateField('personal', 'age', v)}
                tooltip="How old are you? This helps us plan for retirement and set the right risk level for investments." />
              <NumberField label="Retirement Age" value={data.personal.retirementAge} onChange={(v) => updateField('personal', 'retirementAge', v)}
                tooltip="At what age do you want to stop working? In India, most people plan for 58-65." />
            </FormRow>
            <FormRow>
              <TextField label="City" value={data.personal.city} onChange={(v) => updateField('personal', 'city', v)}
                tooltip="Which city do you live in? Cost of living varies a lot between cities." placeholder="e.g. Bangalore" />
              <NumberField label="Dependents" value={data.personal.dependents} onChange={(v) => updateField('personal', 'dependents', v)}
                tooltip="How many people depend on your income? (spouse, children, parents)." />
            </FormRow>
            <div>
              <div className="flex items-center gap-1 mb-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Risk Comfort Level</label>
                <InfoTooltip text="How comfortable are you with your investments going up and down? Conservative = safe but slow growth. Aggressive = faster growth but can lose money temporarily.">
                  <span />
                </InfoTooltip>
              </div>
              <div className="flex gap-3 flex-wrap">
                {(['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'] as const).map((r) => (
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

        {/* ────────── Income ────────── */}
        {activeSection === 'income' && (
          <FormSection title="Monthly Income" hint="How much money comes in every month (before tax)" icon="💰">
            <FormRow>
              <NumberField label="Monthly Salary" value={data.income.monthlySalary} prefix="₹" onChange={(v) => updateField('income', 'monthlySalary', v)}
                tooltip="Your monthly take-home salary (after TDS/tax deduction)." />
              <NumberField label="Other Income" value={data.income.otherIncome} prefix="₹" onChange={(v) => updateField('income', 'otherIncome', v)}
                tooltip="Rent from property, freelance work, dividends, interest — any extra monthly income." />
            </FormRow>
            <div className="mt-4 p-4 bg-indigo-50 dark:bg-indigo-950/40 rounded-xl">
              <p className="text-sm text-indigo-700 dark:text-indigo-400">
                💡 <strong>Total Monthly Income:</strong> ₹{(data.income.monthlySalary + data.income.otherIncome).toLocaleString('en-IN')}
              </p>
            </div>
          </FormSection>
        )}

        {/* ────────── Expenses (dynamic) ────────── */}
        {activeSection === 'expenses' && (
          <FormSection title="Monthly Expenses" hint="Add the types of expenses you have. Remove any that don't apply." icon="🛒">
            <DynamicList
              items={data.expenses}
              usedCategories={usedExpenseCategories}
              suggestions={EXPENSE_SUGGESTIONS}
              emojiMap={EXPENSE_EMOJI}
              tooltipMap={EXPENSE_TOOLTIPS}
              storageKey="fa-custom-expense-categories"
              defaultEmoji="💰"
              categoryLabel="Category"
              amountLabel="Amount (₹)"
              emptyIcon="🛒"
              emptyText="No expenses added yet. Click the button below to add your spending categories."
              addLabel="➕ Add an Expense"
              onAdd={() => addItem('expenses')}
              onRemove={(id) => removeItem('expenses', id)}
              onCategoryChange={(id, cat) => updateItemCategory('expenses', id, cat)}
              onAmountChange={(id, amt) => updateItemAmount('expenses', id, amt)}
            />
            {data.expenses.length > 0 && (
              <div className="mt-4 p-4 bg-orange-50 dark:bg-orange-950/40 rounded-xl border border-orange-200 dark:border-orange-800">
                <p className="text-sm font-semibold text-orange-700 dark:text-orange-400">
                  💡 Total Monthly Expenses: ₹{expenseTotal.toLocaleString('en-IN')}
                </p>
              </div>
            )}
          </FormSection>
        )}

        {/* ────────── Savings / Assets (dynamic) ────────── */}
        {activeSection === 'assets' && (
          <FormSection title="Your Savings & Investments" hint="Add all your savings, investments, and assets. Remove any that don't apply." icon="🏦">
            <DynamicList
              items={data.assets}
              usedCategories={usedAssetCategories}
              suggestions={ASSET_SUGGESTIONS}
              emojiMap={ASSET_EMOJI}
              tooltipMap={ASSET_TOOLTIPS}
              storageKey="fa-custom-asset-categories"
              defaultEmoji="🏦"
              categoryLabel="Asset Type"
              amountLabel="Value (₹)"
              emptyIcon="🏦"
              emptyText="No savings added yet. Click the button below to add your savings and investments."
              addLabel="➕ Add a Saving / Investment"
              onAdd={() => addItem('assets')}
              onRemove={(id) => removeItem('assets', id)}
              onCategoryChange={(id, cat) => updateItemCategory('assets', id, cat)}
              onAmountChange={(id, amt) => updateItemAmount('assets', id, amt)}
            />
            {data.assets.length > 0 && (
              <div className="mt-4 p-4 bg-emerald-50 dark:bg-emerald-950/40 rounded-xl border border-emerald-200 dark:border-emerald-800">
                <p className="text-sm font-semibold text-emerald-700 dark:text-emerald-400">
                  💡 Total Savings: ₹{assetTotal.toLocaleString('en-IN')}
                </p>
              </div>
            )}
          </FormSection>
        )}

        {/* ────────── Loans ────────── */}
        {activeSection === 'liabilities' && (
          <FormSection title="Your Loans" hint="Any money you owe — EMIs, outstanding amounts. Leave 0 if you don't have a loan." icon="💳">
            <LoanSection title="🏠 Home Loan" data={data.liabilities.homeLoan}
              onChange={(field, val) => updateField('liabilities', field, val, 'homeLoan')} />
            <div className="border-t border-gray-100 dark:border-gray-700 my-6" />
            <LoanSection title="🚗 Car Loan" data={data.liabilities.carLoan}
              onChange={(field, val) => updateField('liabilities', field, val, 'carLoan')} />
          </FormSection>
        )}

        {/* ────────── Insurance ────────── */}
        {activeSection === 'insurance' && (
          <FormSection title="Insurance Policies" hint="Insurance protects you and your family. Enter what you have." icon="🛡️">
            <FormRow>
              <NumberField label="Term Life Cover Amount" value={data.insurance.termLifeCover} prefix="₹" onChange={(v) => updateField('insurance', 'termLifeCover', v)}
                tooltip="How much money your family gets if something happens to you. Experts say 10-15x annual income." />
              <NumberField label="Term Life Premium (monthly)" value={data.insurance.termLifePremium} prefix="₹" onChange={(v) => updateField('insurance', 'termLifePremium', v)}
                tooltip="How much you pay each month for your term life insurance. If you pay yearly, divide by 12." />
            </FormRow>
            <FormRow>
              <NumberField label="Health Insurance Cover" value={data.insurance.healthCover} prefix="₹" onChange={(v) => updateField('insurance', 'healthCover', v)}
                tooltip="Maximum your health insurance pays for hospital bills. Get at least ₹10-20 lakh for a family." />
              <NumberField label="Health Premium (monthly)" value={data.insurance.healthPremium} prefix="₹" onChange={(v) => updateField('insurance', 'healthPremium', v)}
                tooltip="Monthly cost of your health insurance. If yearly, divide by 12." />
            </FormRow>
          </FormSection>
        )}

        {/* ────────── Investments ────────── */}
        {activeSection === 'investments' && (
          <FormSection title="Monthly Investments" hint="Money you invest regularly every month" icon="📈">
            <FormRow>
              <NumberField label="Monthly SIP Amount" value={data.investments.monthlySIP} prefix="₹" onChange={(v) => updateField('investments', 'monthlySIP', v)}
                tooltip="SIP = Systematic Investment Plan. Money automatically invested in mutual funds every month." />
              <NumberField label="Expected Return (%)" value={data.investments.expectedReturn} onChange={(v) => updateField('investments', 'expectedReturn', v)}
                tooltip="What % return per year? Stocks/MFs: 10-12%. FDs: 6-7%. PPF: 7-8%." />
            </FormRow>
          </FormSection>
        )}

        {/* ────────── Goals ────────── */}
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

/* ══════════════════════════════════════════════════════════════
   DynamicList — renders rows + add button (shared by expenses
   and assets)
   ══════════════════════════════════════════════════════════════ */
interface DynamicListProps {
  items: DynamicItem[];
  usedCategories: string[];
  suggestions: string[];
  emojiMap: Record<string, string>;
  tooltipMap: Record<string, string>;
  storageKey: string;
  defaultEmoji: string;
  categoryLabel: string;
  amountLabel: string;
  emptyIcon: string;
  emptyText: string;
  addLabel: string;
  onAdd: () => void;
  onRemove: (id: string) => void;
  onCategoryChange: (id: string, cat: string) => void;
  onAmountChange: (id: string, amt: number) => void;
}

function DynamicList(props: DynamicListProps) {
  const { items, usedCategories, suggestions, emojiMap, tooltipMap, storageKey,
          defaultEmoji, categoryLabel, amountLabel,
          emptyIcon, emptyText, addLabel,
          onAdd, onRemove, onCategoryChange, onAmountChange } = props;

  return (
    <>
      <div className="space-y-3">
        {items.length === 0 && (
          <div className="text-center py-8 text-gray-400 dark:text-gray-500">
            <span className="text-4xl block mb-3">{emptyIcon}</span>
            <p className="text-sm">{emptyText}</p>
          </div>
        )}

        {items.map((item) => (
          <DynamicRow
            key={item.id}
            item={item}
            usedCategories={usedCategories}
            suggestions={suggestions}
            emojiMap={emojiMap}
            tooltipMap={tooltipMap}
            storageKey={storageKey}
            defaultEmoji={defaultEmoji}
            categoryLabel={categoryLabel}
            amountLabel={amountLabel}
            onCategoryChange={(cat) => onCategoryChange(item.id, cat)}
            onAmountChange={(amt) => onAmountChange(item.id, amt)}
            onRemove={() => onRemove(item.id)}
          />
        ))}
      </div>

      <button onClick={onAdd} type="button"
        className="w-full mt-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                   text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                   hover:text-indigo-600 dark:hover:text-indigo-400
                   hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30
                   transition-all font-medium">
        {addLabel}
      </button>
    </>
  );
}

/* ══════════════════════════════════════════════════════════════
   DynamicRow — autocomplete category + amount + delete
   ══════════════════════════════════════════════════════════════ */
function DynamicRow({ item, usedCategories, suggestions, emojiMap, tooltipMap,
                      storageKey, defaultEmoji, categoryLabel, amountLabel,
                      onCategoryChange, onAmountChange, onRemove }: {
  item: DynamicItem;
  usedCategories: string[];
  suggestions: string[];
  emojiMap: Record<string, string>;
  tooltipMap: Record<string, string>;
  storageKey: string;
  defaultEmoji: string;
  categoryLabel: string;
  amountLabel: string;
  onCategoryChange: (cat: string) => void;
  onAmountChange: (amt: number) => void;
  onRemove: () => void;
}) {
  const [search, setSearch] = useState(item.category);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const customCats = loadCustom(storageKey);
  const allSuggestions = [...suggestions, ...customCats.filter((c) => !suggestions.includes(c))];
  const filtered = allSuggestions.filter(
    (s) => s.toLowerCase().includes(search.toLowerCase()) && (s === item.category || !usedCategories.includes(s)),
  );

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        if (search !== item.category) onCategoryChange(search);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [search, item.category, onCategoryChange]);

  function selectCategory(cat: string) {
    setSearch(cat);
    onCategoryChange(cat);
    setOpen(false);
  }

  const emoji = emojiMap[item.category] || defaultEmoji;
  const tooltip = tooltipMap[item.category];

  return (
    <div className="flex items-start gap-2 sm:gap-3 animate-fade-in">
      {/* Category autocomplete */}
      <div className="relative flex-1 min-w-0" ref={ref}>
        <div className="flex items-center gap-1 mb-1">
          <span className="text-sm">{emoji}</span>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">{categoryLabel}</label>
          {tooltip && <InfoTooltip text={tooltip}><span /></InfoTooltip>}
        </div>
        <input
          type="text"
          value={search}
          onChange={(e) => { setSearch(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              if (filtered.length > 0 && search) selectCategory(filtered[0]);
              else { onCategoryChange(search); setOpen(false); }
            }
          }}
          placeholder="Type to search..."
          className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                     bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                     focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all"
        />
        {open && filtered.length > 0 && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          max-h-48 overflow-y-auto animate-fade-in">
            {filtered.map((s) => (
              <button
                key={s}
                type="button"
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => selectCategory(s)}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30
                           text-gray-700 dark:text-gray-300 flex items-center gap-2 transition-colors
                           first:rounded-t-xl last:rounded-b-xl"
              >
                <span>{emojiMap[s] || defaultEmoji}</span>
                <span>{s}</span>
              </button>
            ))}
          </div>
        )}
        {open && filtered.length === 0 && search.trim() && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
            Press <strong>Enter</strong> to use "<em>{search}</em>" as a new type
          </div>
        )}
      </div>

      {/* Amount */}
      <div className="w-36 sm:w-44 shrink-0">
        <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">{amountLabel}</label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-400 dark:text-gray-500 font-medium">₹</span>
          <input
            type="number"
            value={item.amount || ''}
            onChange={(e) => onAmountChange(Number(e.target.value) || 0)}
            placeholder="0"
            className="w-full pl-8 pr-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                       bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                       focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all"
          />
        </div>
      </div>

      {/* Delete */}
      <div className="pt-6">
        <button
          type="button"
          onClick={onRemove}
          className="w-8 h-8 rounded-full bg-red-100 dark:bg-red-900/40 text-red-500 dark:text-red-400
                     text-xs font-bold flex items-center justify-center
                     hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors
                     focus:outline-none focus:ring-2 focus:ring-red-400"
          title="Remove this item"
        >
          ✕
        </button>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   Reusable form helpers
   ══════════════════════════════════════════════════════════════ */
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
          tooltip="How much you still owe. Check your loan statement or banking app." />
        <NumberField label="Monthly EMI" value={data.emi} prefix="₹" onChange={(v) => onChange('emi', v)}
          tooltip="EMI = Equated Monthly Installment. The fixed amount you pay every month." />
        <NumberField label="Interest Rate (%)" value={data.rateOfInterest} onChange={(v) => onChange('rateOfInterest', v)}
          tooltip="Annual interest rate. Home loans usually 8-10%, car loans 9-12%." />
        <NumberField label="Months Remaining" value={data.remainingMonths} onChange={(v) => onChange('remainingMonths', v)}
          tooltip="How many more monthly EMI payments until fully paid off." />
      </div>
    </div>
  );
}
