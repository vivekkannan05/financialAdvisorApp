import { useEffect, useState, useRef } from 'react';
import { fetchProfile, updateProfile, createProfile } from '@/api/profile';
import InfoTooltip from '@/components/InfoTooltip';
import type { ProfileSnapshot } from '@/types/api';

/* ── Shared models ────────────────────────────────────────── */
interface DynamicItem {
  id: string;
  category: string;
  amount: number;
}

interface LoanItem {
  id: string;
  loanType: string;
  outstanding: number;
  emi: number;
  rateOfInterest: number;
  remainingMonths: number;
}

interface InsuranceItem {
  id: string;
  policyType: string;
  coverAmount: number;
  premium: number;
  premiumFrequency: string;
}

interface InvestmentItem {
  id: string;
  instrument: string;
  name: string;
  amount: number;
  frequency: string;
  expectedReturn: number;
}

interface ProfileData {
  personal: { age: number; retirementAge: number; riskTolerance: string; city: string; dependents: number };
  income: { monthlySalary: number; otherIncome: number };
  expenses: DynamicItem[];
  assets: DynamicItem[];
  liabilities: LoanItem[];
  insurance: InsuranceItem[];
  investments: InvestmentItem[];
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
   ASSET constants
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
  'Savings Account': 'Total balance in all your savings bank accounts. Most accessible money.',
  'Fixed Deposits (FD)': 'Total value of all your FDs. Guaranteed returns but locked for a period.',
  'Mutual Funds': 'Current value of all mutual fund investments (SIPs + lump sum).',
  'Stocks / Shares': 'Current value of shares you directly own (Zerodha, Groww, etc).',
  'PPF': 'Public Provident Fund — government scheme with tax benefits. Locked 15 years.',
  'EPF / PF': 'Employee Provident Fund — salary deduction + employer contribution.',
  'Gold': 'Value of gold — jewellery, coins, digital gold, SGBs. Use today\'s price.',
  'NPS': 'National Pension System — retirement savings. Partially locked until 60.',
  'Real Estate': 'Market value of property you own (excluding home with active loan).',
  'Recurring Deposits (RD)': 'Total value of RDs. Like an FD but you deposit monthly.',
  'Sovereign Gold Bonds': 'Government gold bonds — earn interest on gold!',
  'Cryptocurrency': 'Current value of crypto holdings. Very volatile — be careful!',
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
   LOAN constants
   ══════════════════════════════════════════════════════════════ */
const LOAN_SUGGESTIONS = [
  'Home Loan', 'Car Loan', 'Personal Loan', 'Education Loan',
  'Gold Loan', 'Two-Wheeler Loan', 'Credit Card Debt',
  'Business Loan', 'Loan Against Property', 'Loan Against FD',
  'Consumer Durable Loan', 'Other Loan',
];

const LOAN_EMOJI: Record<string, string> = {
  'Home Loan': '🏠', 'Car Loan': '🚗', 'Personal Loan': '💳',
  'Education Loan': '🎓', 'Gold Loan': '✨', 'Two-Wheeler Loan': '🏍️',
  'Credit Card Debt': '💳', 'Business Loan': '💼', 'Loan Against Property': '🏘️',
  'Loan Against FD': '🔒', 'Consumer Durable Loan': '📺', 'Other Loan': '📋',
};

const LOAN_TOOLTIPS: Record<string, string> = {
  'Home Loan': 'Loan taken to buy or build a house. Usually 15-30 year tenure at 8-10% interest.',
  'Car Loan': 'Loan for buying a car. Usually 3-7 year tenure at 9-12% interest.',
  'Personal Loan': 'Unsecured loan for any purpose. Higher interest (10-18%) but no collateral needed.',
  'Education Loan': 'Loan for higher education (Indian or abroad). Tax benefit under Section 80E.',
  'Gold Loan': 'Loan taken by pledging your gold jewellery. Lower interest (7-12%).',
  'Two-Wheeler Loan': 'Loan for buying a bike or scooter. Usually 1-5 year tenure.',
  'Credit Card Debt': 'Outstanding balance on credit cards. Very high interest (24-42%)! Pay this off first.',
  'Business Loan': 'Loan taken for business purposes. Interest varies widely.',
  'Loan Against Property': 'Loan using your property as collateral. Lower interest than personal loan.',
  'Loan Against FD': 'Loan using your FD as collateral. Very low interest (1-2% above FD rate).',
  'Consumer Durable Loan': 'EMI for electronics, appliances, furniture purchased on credit.',
  'Other Loan': 'Any other loan not listed above.',
};

const LOAN_KEY_MAP: Record<string, string> = {
  homeLoan: 'Home Loan', carLoan: 'Car Loan', personalLoan: 'Personal Loan',
  educationLoan: 'Education Loan', goldLoan: 'Gold Loan',
  twoWheelerLoan: 'Two-Wheeler Loan', creditCardDebt: 'Credit Card Debt',
  businessLoan: 'Business Loan',
};

/* ══════════════════════════════════════════════════════════════
   INSURANCE constants
   ══════════════════════════════════════════════════════════════ */
const INSURANCE_SUGGESTIONS = [
  'Term Life Insurance', 'Health Insurance (Individual)', 'Health Insurance (Family Floater)',
  'Critical Illness Cover', 'Personal Accident Cover', 'Super Top-Up Health',
  'Motor Insurance (Car)', 'Motor Insurance (Two-Wheeler)', 'Home Insurance',
  'Travel Insurance', 'Endowment Plan', 'Child Plan (ULIP)', 'Other Insurance',
];

const INSURANCE_EMOJI: Record<string, string> = {
  'Term Life Insurance': '🛡️', 'Health Insurance (Individual)': '🏥',
  'Health Insurance (Family Floater)': '👨‍👩‍👧‍👦', 'Critical Illness Cover': '💊',
  'Personal Accident Cover': '🚑', 'Super Top-Up Health': '➕',
  'Motor Insurance (Car)': '🚗', 'Motor Insurance (Two-Wheeler)': '🏍️',
  'Home Insurance': '🏠', 'Travel Insurance': '✈️',
  'Endowment Plan': '📋', 'Child Plan (ULIP)': '👶', 'Other Insurance': '📦',
};

const INSURANCE_TOOLTIPS: Record<string, string> = {
  'Term Life Insurance': 'Pure life cover — pays a lump sum to your family if something happens. Experts say 10-15x annual income.',
  'Health Insurance (Individual)': 'Covers your hospital bills. Get at least ₹5-10 lakh cover.',
  'Health Insurance (Family Floater)': 'One policy covers the whole family. Shared cover amount — ₹10-20 lakh recommended.',
  'Critical Illness Cover': 'Pays a lump sum if you\'re diagnosed with cancer, heart attack, stroke, etc.',
  'Personal Accident Cover': 'Covers death or disability from accidents. Usually cheap — ₹100-200/month.',
  'Super Top-Up Health': 'Activates AFTER your base health plan limit is crossed. Cheap way to get ₹50L+ cover.',
  'Motor Insurance (Car)': 'Third-party is mandatory by law. Comprehensive also covers your car damage.',
  'Motor Insurance (Two-Wheeler)': 'Third-party mandatory. Comprehensive costs just ₹1000-3000/year.',
  'Home Insurance': 'Covers your home against fire, floods, earthquakes, burglary.',
  'Travel Insurance': 'Covers medical emergencies, trip cancellations, baggage loss when travelling.',
  'Endowment Plan': 'Insurance + savings combo. Low returns — consider term + mutual fund instead.',
  'Child Plan (ULIP)': 'Insurance + investment for child\'s future. Check charges carefully!',
  'Other Insurance': 'Any other insurance policy not listed above.',
};

const INSURANCE_KEY_MAP: Record<string, string> = {
  termLifeCover: 'Term Life Insurance', termLife: 'Term Life Insurance',
  healthCover: 'Health Insurance (Individual)', health: 'Health Insurance (Individual)',
  healthFamily: 'Health Insurance (Family Floater)',
  criticalIllness: 'Critical Illness Cover', personalAccident: 'Personal Accident Cover',
  superTopUp: 'Super Top-Up Health',
};

const PREMIUM_FREQUENCIES = ['Monthly', 'Quarterly', 'Half-Yearly', 'Yearly'] as const;

/* ══════════════════════════════════════════════════════════════
   INVESTMENT constants
   ══════════════════════════════════════════════════════════════ */
const INVESTMENT_SUGGESTIONS = [
  'SIP (Mutual Funds)', 'PPF Contribution', 'NPS Contribution', 'ELSS (Tax Saving MF)',
  'Sukanya Samriddhi (SSY)', 'Recurring Deposit (RD)', 'Direct Stocks',
  'Gold / SGB', 'EPF Voluntary (VPF)', 'NSC / KVP', 'ULIP Premium',
  'Chit Fund', 'Cryptocurrency', 'Other Investment',
];

const INVESTMENT_EMOJI: Record<string, string> = {
  'SIP (Mutual Funds)': '📊', 'PPF Contribution': '🏛️', 'NPS Contribution': '🧓',
  'ELSS (Tax Saving MF)': '💰', 'Sukanya Samriddhi (SSY)': '👧',
  'Recurring Deposit (RD)': '🔄', 'Direct Stocks': '📈',
  'Gold / SGB': '✨', 'EPF Voluntary (VPF)': '🏢', 'NSC / KVP': '📮',
  'ULIP Premium': '📋', 'Chit Fund': '🤝', 'Cryptocurrency': '₿',
  'Other Investment': '📦',
};

const INVESTMENT_TOOLTIPS: Record<string, string> = {
  'SIP (Mutual Funds)': 'Systematic Investment Plan — auto-invests in mutual funds. Great for wealth building! Expect 10-12% long-term.',
  'PPF Contribution': 'Public Provident Fund — govt scheme, tax-free returns ~7.1%. Max ₹1.5L/year.',
  'NPS Contribution': 'National Pension System — extra tax benefit of ₹50K under 80CCD(1B). Locked until 60.',
  'ELSS (Tax Saving MF)': 'Equity Linked Savings Scheme — 3-year lock-in, tax benefit under 80C. Best tax-saving option!',
  'Sukanya Samriddhi (SSY)': 'Govt scheme for girl child. High interest (~8%), 80C benefit. Max ₹1.5L/year.',
  'Recurring Deposit (RD)': 'Fixed monthly deposit with guaranteed returns. Like FD but you deposit monthly.',
  'Direct Stocks': 'Monthly amount you invest directly in stocks via Zerodha, Groww, etc.',
  'Gold / SGB': 'Monthly investment in gold — digital gold, gold ETF, or Sovereign Gold Bonds.',
  'EPF Voluntary (VPF)': 'Extra contribution to PF beyond the mandatory 12%. Same interest as EPF (~8.25%).',
  'NSC / KVP': 'National Savings Certificate / Kisan Vikas Patra — post office savings schemes.',
  'ULIP Premium': 'Unit Linked Insurance Plan premium. Combines insurance + investment.',
  'Chit Fund': 'Monthly chit fund contribution. Popular in South India.',
  'Cryptocurrency': 'Monthly crypto investment. Very volatile — keep this small!',
  'Other Investment': 'Any other regular investment not listed above.',
};

const INVESTMENT_KEY_MAP: Record<string, string> = {
  monthlySIP: 'SIP (Mutual Funds)', sip: 'SIP (Mutual Funds)',
  ppf: 'PPF Contribution', nps: 'NPS Contribution', elss: 'ELSS (Tax Saving MF)',
  rd: 'Recurring Deposit (RD)', stocks: 'Direct Stocks',
};

const INVESTMENT_FREQUENCIES = ['Monthly', 'Quarterly', 'Half-Yearly', 'Yearly'] as const;

/* ── Scheme / fund name suggestions per instrument type ─── */
const SCHEME_SUGGESTIONS: Record<string, string[]> = {
  'SIP (Mutual Funds)': [
    // Parag Parikh
    'Parag Parikh Flexi Cap Fund', 'Parag Parikh Conservative Hybrid Fund',
    'Parag Parikh Dynamic Asset Allocation Fund', 'Parag Parikh Liquid Fund',
    // Motilal Oswal
    'Motilal Oswal Flexi Cap Fund', 'Motilal Oswal Midcap Fund',
    'Motilal Oswal Large Cap Fund', 'Motilal Oswal S&P 500 Index Fund',
    'Motilal Oswal Nasdaq 100 FoF', 'Motilal Oswal Large & Midcap Fund',
    // Bandhan
    'Bandhan Flexi Cap Fund', 'Bandhan Small Cap Fund', 'Bandhan Core Equity Fund',
    'Bandhan Midcap Fund', 'Bandhan Balanced Advantage Fund',
    // Edelweiss
    'Edelweiss Flexi Cap Fund', 'Edelweiss Mid Cap Fund', 'Edelweiss Small Cap Fund',
    'Edelweiss Balanced Advantage Fund', 'Edelweiss Large Cap Fund',
    // HDFC
    'HDFC Flexi Cap Fund', 'HDFC Mid-Cap Opportunities Fund', 'HDFC Small Cap Fund',
    'HDFC Balanced Advantage Fund', 'HDFC Top 100 Fund', 'HDFC Hybrid Equity Fund',
    'HDFC Short Term Debt Fund', 'HDFC Nifty 50 Index Fund',
    // ICICI Prudential
    'ICICI Pru Bluechip Fund', 'ICICI Pru Value Discovery Fund',
    'ICICI Pru Equity & Debt Fund', 'ICICI Pru Technology Fund',
    'ICICI Pru Flexi Cap Fund',
    // SBI
    'SBI Bluechip Fund', 'SBI Flexi Cap Fund', 'SBI Small Cap Fund',
    'SBI Magnum Midcap Fund', 'SBI Equity Hybrid Fund', 'SBI Focused Equity Fund',
    'SBI Nifty Index Fund',
    // Axis
    'Axis Bluechip Fund', 'Axis Midcap Fund', 'Axis Small Cap Fund',
    'Axis Flexi Cap Fund',
    // Kotak
    'Kotak Flexi Cap Fund', 'Kotak Emerging Equity Fund', 'Kotak Small Cap Fund',
    'Kotak Equity Opportunities Fund',
    // Nippon India
    'Nippon India Growth Fund', 'Nippon India Small Cap Fund',
    'Nippon India Large Cap Fund', 'Nippon India Flexi Cap Fund',
    // DSP
    'DSP Flexi Cap Fund', 'DSP Midcap Fund', 'DSP Small Cap Fund',
    'DSP Equity Opportunities Fund',
    // Mirae Asset
    'Mirae Asset Large Cap Fund', 'Mirae Asset Flexi Cap Fund',
    'Mirae Asset Emerging Bluechip Fund', 'Mirae Asset Midcap Fund',
    // Tata
    'Tata Digital India Fund', 'Tata Mid Cap Growth Fund',
    'Tata Small Cap Fund', 'Tata Flexi Cap Fund',
    // UTI
    'UTI Flexi Cap Fund', 'UTI Nifty 50 Index Fund', 'UTI Mastershare',
    // Canara Robeco
    'Canara Robeco Flexi Cap Fund', 'Canara Robeco Bluechip Equity Fund',
    'Canara Robeco Small Cap Fund',
    // Quant
    'Quant Small Cap Fund', 'Quant Mid Cap Fund', 'Quant Flexi Cap Fund',
    'Quant Active Fund',
    // PPFAS
    'PPFAS Flexi Cap Fund',
    // Aditya Birla
    'Aditya Birla SL Flexi Cap Fund', 'Aditya Birla SL Frontline Equity Fund',
    'Aditya Birla SL Small Cap Fund',
    // Franklin Templeton
    'Franklin India Flexi Cap Fund', 'Franklin India Bluechip Fund',
    'Franklin India Smaller Companies Fund',
    // Sundaram
    'Sundaram Mid Cap Fund', 'Sundaram Small Cap Fund', 'Sundaram Flexi Cap Fund',
  ],
  'ELSS (Tax Saving MF)': [
    'Parag Parikh ELSS Tax Saver Fund', 'Motilal Oswal ELSS Tax Saver Fund',
    'Bandhan ELSS Tax Saver Fund', 'Edelweiss ELSS Tax Saver Fund',
    'HDFC ELSS Tax Saver Fund', 'ICICI Pru ELSS Tax Saver Fund',
    'SBI ELSS Tax Saver Fund', 'Axis ELSS Tax Saver Fund',
    'Kotak ELSS Tax Saver Fund', 'Nippon India ELSS Tax Saver Fund',
    'DSP Tax Saver Fund', 'Mirae Asset ELSS Tax Saver Fund',
    'Tata ELSS Tax Saver Fund', 'UTI ELSS Tax Saver Fund',
    'Canara Robeco ELSS Tax Saver Fund', 'Quant ELSS Tax Saver Fund',
    'Aditya Birla SL ELSS Tax Saver Fund', 'Franklin India ELSS Tax Saver Fund',
  ],
  'Direct Stocks': [
    // Nifty 50 top constituents
    'Reliance Industries', 'TCS', 'HDFC Bank', 'Infosys', 'ICICI Bank',
    'Bharti Airtel', 'State Bank of India', 'ITC', 'Kotak Mahindra Bank',
    'Hindustan Unilever', 'Larsen & Toubro', 'Axis Bank', 'Bajaj Finance',
    'Maruti Suzuki', 'Asian Paints', 'Titan Company', 'Sun Pharma',
    'HCL Technologies', 'Wipro', 'NTPC', 'Power Grid', 'UltraTech Cement',
    'Nestle India', 'Tata Motors', 'Tech Mahindra', 'Mahindra & Mahindra',
    'Bajaj Finserv', 'Adani Enterprises', 'Adani Ports', 'Coal India',
    'Tata Steel', 'JSW Steel', 'Cipla', 'Dr Reddy\'s', 'Divi\'s Labs',
    'Eicher Motors', 'Hero MotoCorp', 'Britannia', 'BPCL', 'ONGC',
    'Grasim Industries', 'IndusInd Bank', 'Tata Consumer', 'Apollo Hospitals',
    'SBI Life Insurance', 'HDFC Life Insurance', 'Zomato', 'Jio Financial',
  ],
  'NPS Contribution': [
    'SBI Pension Fund', 'LIC Pension Fund', 'UTI Retirement Solutions',
    'HDFC Pension Fund', 'ICICI Pru Pension Fund', 'Kotak Pension Fund',
    'Aditya Birla SL Pension Fund', 'Tata Pension Fund', 'Max Life Pension Fund',
  ],
};

const INSTRUMENT_DEFAULT_RETURN: Record<string, number> = {
  'SIP (Mutual Funds)': 12, 'PPF Contribution': 7.1, 'NPS Contribution': 9,
  'ELSS (Tax Saving MF)': 12, 'Sukanya Samriddhi (SSY)': 8,
  'Recurring Deposit (RD)': 6.5, 'Direct Stocks': 12,
  'Gold / SGB': 9, 'EPF Voluntary (VPF)': 8.25, 'NSC / KVP': 7.7,
  'ULIP Premium': 8, 'Chit Fund': 10, 'Cryptocurrency': 15,
  'Other Investment': 10,
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

function parseItems(raw: unknown, keyMap: Record<string, string>): DynamicItem[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return (raw as DynamicItem[]).map((e) => ({
      id: e.id || uid(), category: String(e.category || ''), amount: Number(e.amount) || 0,
    }));
  }
  return Object.entries(raw as Record<string, unknown>)
    .filter(([, v]) => v !== undefined && v !== null)
    .map(([key, value]) => ({ id: uid(), category: keyMap[key] || key, amount: Number(value) || 0 }));
}

function parseLoans(raw: unknown, keyMap: Record<string, string>): LoanItem[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return (raw as LoanItem[]).map((l) => ({
      id: l.id || uid(),
      loanType: String(l.loanType || ''),
      outstanding: Number(l.outstanding) || 0,
      emi: Number(l.emi) || 0,
      rateOfInterest: Number(l.rateOfInterest) || 0,
      remainingMonths: Number(l.remainingMonths) || 0,
    }));
  }
  const obj = raw as Record<string, unknown>;
  return Object.entries(obj)
    .filter(([, v]) => v && typeof v === 'object')
    .map(([key, value]) => {
      const loan = value as Record<string, unknown>;
      return {
        id: uid(),
        loanType: keyMap[key] || key,
        outstanding: Number(loan.outstanding ?? loan.principal ?? loan.value ?? 0) || 0,
        emi: Number(loan.emi ?? loan.emiAmount ?? 0) || 0,
        rateOfInterest: Number(loan.rateOfInterest ?? loan.interestRate ?? loan.rate ?? 0) || 0,
        remainingMonths: Number(loan.remainingMonths ?? loan.tenure ?? 0) || 0,
      };
    })
    .filter((l) => l.outstanding > 0 || l.emi > 0);
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

function loansToObject(loans: LoanItem[]): Record<string, Record<string, number>> {
  const obj: Record<string, Record<string, number>> = {};
  loans.forEach((l) => {
    if (l.loanType.trim() && (l.outstanding > 0 || l.emi > 0)) {
      obj[l.loanType.trim()] = {
        outstanding: l.outstanding,
        emi: l.emi,
        rateOfInterest: l.rateOfInterest,
        remainingMonths: l.remainingMonths,
      };
      saveCustom('fa-custom-loan-types', LOAN_SUGGESTIONS, l.loanType.trim());
    }
  });
  return obj;
}

function parseInsurance(raw: unknown, keyMap: Record<string, string>): InsuranceItem[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return (raw as InsuranceItem[]).map((p) => ({
      id: p.id || uid(),
      policyType: String(p.policyType || ''),
      coverAmount: Number(p.coverAmount) || 0,
      premium: Number(p.premium) || 0,
      premiumFrequency: String(p.premiumFrequency || 'Monthly'),
    }));
  }
  const obj = raw as Record<string, unknown>;
  const items: InsuranceItem[] = [];
  const coverKeys = new Set<string>();
  for (const [key, value] of Object.entries(obj)) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const pol = value as Record<string, unknown>;
      items.push({
        id: uid(),
        policyType: keyMap[key] || key,
        coverAmount: Number(pol.cover ?? pol.coverAmount ?? 0) || 0,
        premium: Number(pol.premium ?? 0) || 0,
        premiumFrequency: String(pol.premiumFrequency || 'Monthly'),
      });
      coverKeys.add(key);
    }
  }
  if (items.length === 0) {
    const termCover = Number(obj.termLifeCover ?? 0) || 0;
    const termPremium = Number(obj.termLifePremium ?? 0) || 0;
    if (termCover > 0 || termPremium > 0) {
      items.push({ id: uid(), policyType: 'Term Life Insurance', coverAmount: termCover, premium: termPremium, premiumFrequency: 'Monthly' });
    }
    const healthCover = Number(obj.healthCover ?? 0) || 0;
    const healthPremium = Number(obj.healthPremium ?? 0) || 0;
    if (healthCover > 0 || healthPremium > 0) {
      items.push({ id: uid(), policyType: 'Health Insurance (Family Floater)', coverAmount: healthCover, premium: healthPremium, premiumFrequency: 'Monthly' });
    }
  }
  return items;
}

function insuranceToObject(policies: InsuranceItem[]): Record<string, Record<string, unknown>> {
  const obj: Record<string, Record<string, unknown>> = {};
  policies.forEach((p) => {
    if (p.policyType.trim() && (p.coverAmount > 0 || p.premium > 0)) {
      obj[p.policyType.trim()] = {
        cover: p.coverAmount,
        premium: p.premium,
        premiumFrequency: p.premiumFrequency.toLowerCase(),
      };
      saveCustom('fa-custom-insurance-types', INSURANCE_SUGGESTIONS, p.policyType.trim());
    }
  });
  return obj;
}

function resolveReturn(instrument: string, explicit?: number): number {
  if (explicit && explicit > 0) return explicit;
  return INSTRUMENT_DEFAULT_RETURN[instrument] ?? 10;
}

function parseInvestments(raw: unknown, keyMap: Record<string, string>): InvestmentItem[] {
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return (raw as InvestmentItem[]).map((inv) => {
      const inst = String(inv.instrument || '');
      return {
        id: inv.id || uid(),
        instrument: inst,
        name: String(inv.name || ''),
        amount: Number(inv.amount) || 0,
        frequency: String(inv.frequency || 'Monthly'),
        expectedReturn: resolveReturn(inst, Number(inv.expectedReturn)),
      };
    });
  }
  const obj = raw as Record<string, unknown>;
  const items: InvestmentItem[] = [];
  for (const [key, value] of Object.entries(obj)) {
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      const inv = value as Record<string, unknown>;
      const inst = String(inv.instrument || '') || keyMap[key] || key;
      const schemeName = String(inv.name || '');
      items.push({
        id: uid(),
        instrument: inst,
        name: schemeName || (keyMap[key] ? '' : (inst === key ? '' : key)),
        amount: Number(inv.amount ?? 0) || 0,
        frequency: String(inv.frequency || 'Monthly'),
        expectedReturn: resolveReturn(inst, Number(inv.expectedReturn ?? inv.expectedReturnRate ?? 0)),
      });
    }
  }
  if (items.length === 0) {
    const sip = Number(obj.monthlySIP ?? 0) || 0;
    if (sip > 0) {
      items.push({ id: uid(), instrument: 'SIP (Mutual Funds)', name: '', amount: sip, frequency: 'Monthly', expectedReturn: 12 });
    }
  }
  return items;
}

const FREQ_MONTHLY_DIVISOR: Record<string, number> = {
  monthly: 1, quarterly: 3, 'half-yearly': 6, yearly: 12,
};

function monthlyEquivalent(amount: number, frequency: string): number {
  const divisor = FREQ_MONTHLY_DIVISOR[frequency.toLowerCase()] ?? 1;
  return amount / divisor;
}

function investmentsToObject(investments: InvestmentItem[]): Record<string, unknown> {
  const obj: Record<string, unknown> = {};
  investments.forEach((inv) => {
    if (inv.instrument.trim() && inv.amount > 0) {
      const key = inv.name.trim() || inv.instrument.trim();
      let finalKey = key;
      let counter = 2;
      while (obj[finalKey]) { finalKey = `${key} (${counter})`; counter++; }
      obj[finalKey] = {
        instrument: inv.instrument.trim(),
        name: inv.name.trim(),
        amount: inv.amount,
        frequency: inv.frequency.toLowerCase(),
        expectedReturn: inv.expectedReturn,
      };
      saveCustom('fa-custom-investment-instruments', INVESTMENT_SUGGESTIONS, inv.instrument.trim());
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
  liabilities: [],
  insurance: [],
  investments: [],
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
  const successTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

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
      liabilities: parseLoans(incoming?.liabilities, LOAN_KEY_MAP),
      insurance: parseInsurance(incoming?.insurance, INSURANCE_KEY_MAP),
      investments: parseInvestments(incoming?.investments, INVESTMENT_KEY_MAP),
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
        liabilities: loansToObject(data.liabilities),
        insurance: insuranceToObject(data.insurance),
        investments: investmentsToObject(data.investments),
      } as unknown as Record<string, unknown>;
      const saved = profile ? await updateProfile(payload) : await createProfile(payload);
      setProfile(saved);
      setSuccess('Saved successfully! Your dashboard will now show updated numbers. 🎉');
      if (successTimerRef.current) clearTimeout(successTimerRef.current);
      successTimerRef.current = setTimeout(() => setSuccess(null), 5000);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Save failed';
      setError(msg);
    } finally {
      setSaving(false);
    }
  }

  /* ── Field updaters ─────────────────────────────────────── */
  function updateField(section: keyof ProfileData, field: string, value: number | string) {
    setData((prev) => {
      const updated = { ...prev };
      if (section !== 'expenses' && section !== 'assets' && section !== 'liabilities'
          && section !== 'insurance' && section !== 'investments') {
        (updated[section] as Record<string, unknown>)[field] = value;
      }
      return updated;
    });
  }

  /* ── Dynamic-list mutators (expenses & assets) ──────────── */
  function addItem(list: 'expenses' | 'assets') {
    setData((prev) => ({ ...prev, [list]: [...prev[list], { id: uid(), category: '', amount: 0 }] }));
  }
  function removeItem(list: 'expenses' | 'assets', id: string) {
    setData((prev) => ({ ...prev, [list]: prev[list].filter((e) => e.id !== id) }));
  }
  function updateItemCategory(list: 'expenses' | 'assets', id: string, category: string) {
    setData((prev) => ({ ...prev, [list]: prev[list].map((e) => (e.id === id ? { ...e, category } : e)) }));
  }
  function updateItemAmount(list: 'expenses' | 'assets', id: string, amount: number) {
    setData((prev) => ({ ...prev, [list]: prev[list].map((e) => (e.id === id ? { ...e, amount } : e)) }));
  }

  /* ── Loan mutators ──────────────────────────────────────── */
  function addLoan() {
    setData((prev) => ({
      ...prev,
      liabilities: [...prev.liabilities, { id: uid(), loanType: '', outstanding: 0, emi: 0, rateOfInterest: 0, remainingMonths: 0 }],
    }));
  }
  function removeLoan(id: string) {
    setData((prev) => ({ ...prev, liabilities: prev.liabilities.filter((l) => l.id !== id) }));
  }
  function updateLoanType(id: string, loanType: string) {
    setData((prev) => ({ ...prev, liabilities: prev.liabilities.map((l) => (l.id === id ? { ...l, loanType } : l)) }));
  }
  function updateLoanField(id: string, field: string, value: number) {
    setData((prev) => ({
      ...prev,
      liabilities: prev.liabilities.map((l) => (l.id === id ? { ...l, [field]: value } : l)),
    }));
  }

  /* ── Insurance mutators ──────────────────────────────────── */
  function addInsurance() {
    setData((prev) => ({
      ...prev,
      insurance: [...prev.insurance, { id: uid(), policyType: '', coverAmount: 0, premium: 0, premiumFrequency: 'Monthly' }],
    }));
  }
  function removeInsurance(id: string) {
    setData((prev) => ({ ...prev, insurance: prev.insurance.filter((p) => p.id !== id) }));
  }
  function updateInsuranceType(id: string, policyType: string) {
    setData((prev) => ({ ...prev, insurance: prev.insurance.map((p) => (p.id === id ? { ...p, policyType } : p)) }));
  }
  function updateInsuranceField(id: string, field: string, value: number | string) {
    setData((prev) => ({
      ...prev,
      insurance: prev.insurance.map((p) => (p.id === id ? { ...p, [field]: value } : p)),
    }));
  }

  /* ── Investment mutators ────────────────────────────────── */
  function addInvestment() {
    setData((prev) => ({
      ...prev,
      investments: [...prev.investments, { id: uid(), instrument: '', name: '', amount: 0, frequency: 'Monthly', expectedReturn: 0 }],
    }));
  }
  function removeInvestment(id: string) {
    setData((prev) => ({ ...prev, investments: prev.investments.filter((inv) => inv.id !== id) }));
  }
  function updateInvestmentInstrument(id: string, instrument: string) {
    const autoReturn = INSTRUMENT_DEFAULT_RETURN[instrument] ?? 10;
    setData((prev) => ({
      ...prev,
      investments: prev.investments.map((inv) =>
        inv.id === id ? { ...inv, instrument, expectedReturn: autoReturn } : inv,
      ),
    }));
  }
  function updateInvestmentField(id: string, field: string, value: number | string) {
    setData((prev) => ({
      ...prev,
      investments: prev.investments.map((inv) => (inv.id === id ? { ...inv, [field]: value } : inv)),
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
    setData((prev) => ({ ...prev, goals: prev.goals.map((g, i) => (i === index ? { ...g, [field]: value } : g)) }));
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
  const usedLoanTypes = data.liabilities.map((l) => l.loanType).filter(Boolean);
  const totalEmi = data.liabilities.reduce((sum, l) => sum + l.emi, 0);
  const totalOutstanding = data.liabilities.reduce((sum, l) => sum + l.outstanding, 0);
  const usedInsuranceTypes = data.insurance.map((p) => p.policyType).filter(Boolean);
  const totalPremiumMonthly = data.insurance.reduce((sum, p) => sum + monthlyEquivalent(p.premium, p.premiumFrequency), 0);
  const totalCover = data.insurance.reduce((sum, p) => sum + p.coverAmount, 0);
  const totalInvestmentMonthly = data.investments.reduce((sum, inv) => sum + monthlyEquivalent(inv.amount, inv.frequency), 0);

  /* ══════════════════════════════════════════════════════════
     Render
     ══════════════════════════════════════════════════════════ */
  return (
    <div className="p-6 md:p-8 max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">📝 My Money Information</h2>
          <p className="text-sm text-gray-400 dark:text-gray-500 mt-1">Fill in your details below. Don't worry — you can always change them later!</p>
        </div>
        <button onClick={handleSave} disabled={saving}
          className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white font-semibold rounded-xl
                     hover:from-indigo-700 hover:to-purple-700 disabled:opacity-50 transition-all shadow-md
                     hover:shadow-lg active:scale-[0.98] text-sm">
          {saving ? '⏳ Saving...' : profile ? '💾 Update Profile' : '✨ Create Profile'}
        </button>
      </div>

      {error && (
        <div className="flex items-start gap-2 bg-red-50 dark:bg-red-950/40 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 rounded-xl px-4 py-3 text-sm">
          <span>❌</span> {error}
        </div>
      )}
      {success && (
        <div className="flex items-start gap-2 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 text-emerald-700 dark:text-emerald-400 rounded-xl px-4 py-3 text-sm">
          <span>✅</span> {success}
        </div>
      )}

      {/* Section tabs */}
      <div className="flex flex-wrap gap-2">
        {sections.map((s) => (
          <button key={s.id} onClick={() => { setActiveSection(s.id); setSuccess(null); setError(null); }}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all ${
              activeSection === s.id
                ? 'bg-indigo-100 dark:bg-indigo-900/50 text-indigo-700 dark:text-indigo-400 border-2 border-indigo-300 dark:border-indigo-700 shadow-sm'
                : 'bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-gray-700 hover:bg-gray-50 dark:hover:bg-gray-700'
            }`}>
            {s.icon} {s.label}
          </button>
        ))}
      </div>

      {/* Form sections */}
      <div className="bg-white dark:bg-gray-800/80 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 md:p-8">

        {/* ── About You ── */}
        {activeSection === 'personal' && (
          <FormSection title="About You" hint="Basic info that helps us give better advice" icon="👤">
            <FormRow>
              <NumberField label="Your Age" value={data.personal.age} onChange={(v) => updateField('personal', 'age', v)}
                tooltip="How old are you? This helps plan for retirement and set risk level." />
              <NumberField label="Retirement Age" value={data.personal.retirementAge} onChange={(v) => updateField('personal', 'retirementAge', v)}
                tooltip="At what age do you want to stop working? Most Indians plan for 58-65." />
            </FormRow>
            <FormRow>
              <TextField label="City" value={data.personal.city} onChange={(v) => updateField('personal', 'city', v)}
                tooltip="Which city? Cost of living varies between Mumbai, Bangalore, smaller towns." placeholder="e.g. Bangalore" />
              <NumberField label="Dependents" value={data.personal.dependents} onChange={(v) => updateField('personal', 'dependents', v)}
                tooltip="How many depend on your income? (spouse, children, parents)." />
            </FormRow>
            <div>
              <div className="flex items-center gap-1 mb-1.5">
                <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Risk Comfort Level</label>
                <InfoTooltip text="How comfortable are you with investments going up and down? Conservative = safe but slow. Aggressive = faster but can lose money temporarily."><span /></InfoTooltip>
              </div>
              <div className="flex gap-3 flex-wrap">
                {(['CONSERVATIVE', 'MODERATE', 'AGGRESSIVE'] as const).map((r) => (
                  <button key={r} type="button" onClick={() => updateField('personal', 'riskTolerance', r)}
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

        {/* ── Income ── */}
        {activeSection === 'income' && (
          <FormSection title="Monthly Income" hint="How much money comes in every month (before tax)" icon="💰">
            <FormRow>
              <NumberField label="Monthly Salary" value={data.income.monthlySalary} prefix="₹" onChange={(v) => updateField('income', 'monthlySalary', v)}
                tooltip="Your monthly take-home salary (after TDS/tax deduction)." />
              <NumberField label="Other Income" value={data.income.otherIncome} prefix="₹" onChange={(v) => updateField('income', 'otherIncome', v)}
                tooltip="Rent from property, freelance work, dividends, interest — any extra monthly income." />
            </FormRow>
            <TotalBar color="indigo" label="Total Monthly Income" value={data.income.monthlySalary + data.income.otherIncome} />
          </FormSection>
        )}

        {/* ── Expenses (dynamic) ── */}
        {activeSection === 'expenses' && (
          <FormSection title="Monthly Expenses" hint="Add the types of expenses you have. Remove any that don't apply." icon="🛒">
            <DynamicList items={data.expenses} usedCategories={usedExpenseCategories}
              suggestions={EXPENSE_SUGGESTIONS} emojiMap={EXPENSE_EMOJI} tooltipMap={EXPENSE_TOOLTIPS}
              storageKey="fa-custom-expense-categories" defaultEmoji="💰"
              categoryLabel="Category" amountLabel="Amount (₹)"
              emptyIcon="🛒" emptyText="No expenses added yet. Click below to add your spending."
              addLabel="➕ Add an Expense"
              onAdd={() => addItem('expenses')} onRemove={(id) => removeItem('expenses', id)}
              onCategoryChange={(id, cat) => updateItemCategory('expenses', id, cat)}
              onAmountChange={(id, amt) => updateItemAmount('expenses', id, amt)} />
            {data.expenses.length > 0 && <TotalBar color="orange" label="Total Monthly Expenses" value={expenseTotal} />}
          </FormSection>
        )}

        {/* ── Savings / Assets (dynamic) ── */}
        {activeSection === 'assets' && (
          <FormSection title="Your Savings & Investments" hint="Add all your savings, investments, and assets." icon="🏦">
            <DynamicList items={data.assets} usedCategories={usedAssetCategories}
              suggestions={ASSET_SUGGESTIONS} emojiMap={ASSET_EMOJI} tooltipMap={ASSET_TOOLTIPS}
              storageKey="fa-custom-asset-categories" defaultEmoji="🏦"
              categoryLabel="Asset Type" amountLabel="Value (₹)"
              emptyIcon="🏦" emptyText="No savings added yet. Click below to add your savings and investments."
              addLabel="➕ Add a Saving / Investment"
              onAdd={() => addItem('assets')} onRemove={(id) => removeItem('assets', id)}
              onCategoryChange={(id, cat) => updateItemCategory('assets', id, cat)}
              onAmountChange={(id, amt) => updateItemAmount('assets', id, amt)} />
            {data.assets.length > 0 && <TotalBar color="emerald" label="Total Savings" value={assetTotal} />}
          </FormSection>
        )}

        {/* ── Loans (dynamic) ── */}
        {activeSection === 'liabilities' && (
          <FormSection title="Your Loans" hint="Add each loan you have. Remove any that don't apply." icon="💳">
            <div className="space-y-4">
              {data.liabilities.length === 0 && (
                <div className="text-center py-8 text-gray-400 dark:text-gray-500">
                  <span className="text-4xl block mb-3">🎉</span>
                  <p className="text-sm">No loans added — that's great! If you do have one, add it below.</p>
                </div>
              )}

              {data.liabilities.map((loan) => (
                <LoanCard
                  key={loan.id}
                  loan={loan}
                  usedTypes={usedLoanTypes}
                  onTypeChange={(t) => updateLoanType(loan.id, t)}
                  onFieldChange={(f, v) => updateLoanField(loan.id, f, v)}
                  onRemove={() => removeLoan(loan.id)}
                />
              ))}
            </div>

            <button onClick={addLoan} type="button"
              className="w-full mt-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                         text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                         hover:text-indigo-600 dark:hover:text-indigo-400
                         hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 transition-all font-medium">
              ➕ Add a Loan
            </button>

            {data.liabilities.length > 0 && (
              <div className="mt-4 p-4 bg-red-50 dark:bg-red-950/40 rounded-xl border border-red-200 dark:border-red-800">
                <div className="flex flex-wrap gap-x-8 gap-y-1">
                  <p className="text-sm font-semibold text-red-700 dark:text-red-400">
                    💳 Total Outstanding: ₹{totalOutstanding.toLocaleString('en-IN')}
                  </p>
                  <p className="text-sm font-semibold text-red-700 dark:text-red-400">
                    📅 Total EMI/month: ₹{totalEmi.toLocaleString('en-IN')}
                  </p>
                </div>
              </div>
            )}
          </FormSection>
        )}

        {/* ── Insurance (dynamic) ── */}
        {activeSection === 'insurance' && (
          <FormSection title="Insurance Policies" hint="Add each insurance policy you have. Premium can be monthly, quarterly, or yearly." icon="🛡️">
            <div className="space-y-4">
              {data.insurance.length === 0 && (
                <div className="text-center py-8 text-gray-400 dark:text-gray-500">
                  <span className="text-4xl block mb-3">🛡️</span>
                  <p className="text-sm">No insurance policies added yet. Every family needs at least term life + health insurance!</p>
                </div>
              )}
              {data.insurance.map((policy) => (
                <InsuranceCard
                  key={policy.id}
                  policy={policy}
                  usedTypes={usedInsuranceTypes}
                  onTypeChange={(t) => updateInsuranceType(policy.id, t)}
                  onFieldChange={(f, v) => updateInsuranceField(policy.id, f, v)}
                  onRemove={() => removeInsurance(policy.id)}
                />
              ))}
            </div>
            <button onClick={addInsurance} type="button"
              className="w-full mt-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                         text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                         hover:text-indigo-600 dark:hover:text-indigo-400
                         hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 transition-all font-medium">
              ➕ Add an Insurance Policy
            </button>
            {data.insurance.length > 0 && (
              <div className="mt-4 p-4 bg-blue-50 dark:bg-blue-950/40 rounded-xl border border-blue-200 dark:border-blue-800">
                <div className="flex flex-wrap gap-x-8 gap-y-1">
                  <p className="text-sm font-semibold text-blue-700 dark:text-blue-400">
                    🛡️ Total Cover: ₹{totalCover.toLocaleString('en-IN')}
                  </p>
                  <p className="text-sm font-semibold text-blue-700 dark:text-blue-400">
                    📅 Total Premium/month: ₹{Math.round(totalPremiumMonthly).toLocaleString('en-IN')}
                  </p>
                </div>
              </div>
            )}
          </FormSection>
        )}

        {/* ── Investments (dynamic) ── */}
        {activeSection === 'investments' && (
          <FormSection title="Regular Investments" hint="Add each investment you make regularly — SIPs, PPF, NPS, RDs, and more." icon="📈">
            <div className="space-y-4">
              {data.investments.length === 0 && (
                <div className="text-center py-8 text-gray-400 dark:text-gray-500">
                  <span className="text-4xl block mb-3">📈</span>
                  <p className="text-sm">No investments added yet. Start with a SIP — even ₹500/month makes a difference!</p>
                </div>
              )}
              {data.investments.map((inv) => (
                <InvestmentCard
                  key={inv.id}
                  investment={inv}
                  onInstrumentChange={(t) => updateInvestmentInstrument(inv.id, t)}
                  onFieldChange={(f, v) => updateInvestmentField(inv.id, f, v)}
                  onRemove={() => removeInvestment(inv.id)}
                />
              ))}
            </div>
            <button onClick={addInvestment} type="button"
              className="w-full mt-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                         text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                         hover:text-indigo-600 dark:hover:text-indigo-400
                         hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 transition-all font-medium">
              ➕ Add an Investment
            </button>
            {data.investments.length > 0 && (
              <TotalBar color="emerald" label="Total Monthly Investment" value={Math.round(totalInvestmentMonthly)} />
            )}
          </FormSection>
        )}

        {/* ── Goals ── */}
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
                  className="absolute top-3 right-3 w-7 h-7 rounded-full bg-red-100 dark:bg-red-900/50 text-red-600 dark:text-red-400 text-xs font-bold flex items-center justify-center hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors">✕</button>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Goal Name</label>
                    <input type="text" value={goal.name} onChange={(e) => updateGoal(i, 'name', e.target.value)}
                      placeholder="e.g. Buy a House, Child's Education"
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Target Amount (₹)</label>
                    <input type="number" value={goal.targetAmount} onChange={(e) => updateGoal(i, 'targetAmount', Number(e.target.value))}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Target Year</label>
                    <input type="number" value={goal.targetYear} onChange={(e) => updateGoal(i, 'targetYear', Number(e.target.value))}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-white dark:bg-gray-800 dark:text-gray-100" />
                  </div>
                  <div>
                    <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">Priority</label>
                    <select value={goal.priority} onChange={(e) => updateGoal(i, 'priority', e.target.value)}
                      className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-white dark:bg-gray-800 dark:text-gray-100">
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
                         hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 transition-all font-medium">
              ➕ Add a New Goal
            </button>
          </FormSection>
        )}
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   LoanCard — autocomplete loan type + 4 detail fields + delete
   ══════════════════════════════════════════════════════════════ */
function LoanCard({ loan, usedTypes, onTypeChange, onFieldChange, onRemove }: {
  loan: LoanItem;
  usedTypes: string[];
  onTypeChange: (t: string) => void;
  onFieldChange: (field: string, value: number) => void;
  onRemove: () => void;
}) {
  const [search, setSearch] = useState(loan.loanType);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const customTypes = loadCustom('fa-custom-loan-types');
  const allSuggestions = [...LOAN_SUGGESTIONS, ...customTypes.filter((c) => !LOAN_SUGGESTIONS.includes(c))];
  const filtered = allSuggestions.filter(
    (s) => s.toLowerCase().includes(search.toLowerCase()) && (s === loan.loanType || !usedTypes.includes(s)),
  );

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        if (search !== loan.loanType) onTypeChange(search);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [search, loan.loanType, onTypeChange]);

  function selectType(t: string) {
    setSearch(t);
    onTypeChange(t);
    setOpen(false);
  }

  const emoji = LOAN_EMOJI[loan.loanType] || '💳';
  const tooltip = LOAN_TOOLTIPS[loan.loanType];

  return (
    <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl relative animate-fade-in">
      {/* Delete button */}
      <button type="button" onClick={onRemove}
        className="absolute top-3 right-3 w-7 h-7 rounded-full bg-red-100 dark:bg-red-900/50 text-red-600 dark:text-red-400
                   text-xs font-bold flex items-center justify-center hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors"
        title="Remove this loan">✕</button>

      {/* Loan type autocomplete */}
      <div className="relative mb-4 max-w-sm" ref={ref}>
        <div className="flex items-center gap-1.5 mb-1">
          <span className="text-lg">{emoji}</span>
          <label className="text-sm font-bold text-gray-700 dark:text-gray-300">Loan Type</label>
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
              if (filtered.length > 0 && search) selectType(filtered[0]);
              else { onTypeChange(search); setOpen(false); }
            }
          }}
          placeholder="Type to search loan type..."
          className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                     bg-white dark:bg-gray-800 dark:text-gray-100
                     focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all"
        />
        {open && filtered.length > 0 && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          max-h-48 overflow-y-auto animate-fade-in">
            {filtered.map((s) => (
              <button key={s} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => selectType(s)}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30
                           text-gray-700 dark:text-gray-300 flex items-center gap-2 transition-colors
                           first:rounded-t-xl last:rounded-b-xl">
                <span>{LOAN_EMOJI[s] || '💳'}</span><span>{s}</span>
              </button>
            ))}
          </div>
        )}
        {open && filtered.length === 0 && search.trim() && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
            Press <strong>Enter</strong> to use "<em>{search}</em>" as a new loan type
          </div>
        )}
      </div>

      {/* Loan detail fields */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <NumberField label="Outstanding Amount" value={loan.outstanding} prefix="₹"
          onChange={(v) => onFieldChange('outstanding', v)}
          tooltip="How much you still owe. Check your loan statement or banking app." />
        <NumberField label="Monthly EMI" value={loan.emi} prefix="₹"
          onChange={(v) => onFieldChange('emi', v)}
          tooltip="EMI = Equated Monthly Installment. The fixed amount you pay every month." />
        <NumberField label="Interest Rate (%)" value={loan.rateOfInterest}
          onChange={(v) => onFieldChange('rateOfInterest', v)}
          tooltip="Annual interest rate on this loan. Home: 8-10%, Car: 9-12%, Personal: 10-18%." />
        <NumberField label="Months Remaining" value={loan.remainingMonths}
          onChange={(v) => onFieldChange('remainingMonths', v)}
          tooltip="How many more monthly EMI payments until fully paid off." />
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   InsuranceCard — autocomplete policy type + cover/premium/frequency + delete
   ══════════════════════════════════════════════════════════════ */
function InsuranceCard({ policy, usedTypes, onTypeChange, onFieldChange, onRemove }: {
  policy: InsuranceItem;
  usedTypes: string[];
  onTypeChange: (t: string) => void;
  onFieldChange: (field: string, value: number | string) => void;
  onRemove: () => void;
}) {
  const [search, setSearch] = useState(policy.policyType);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const customTypes = loadCustom('fa-custom-insurance-types');
  const allSuggestions = [...INSURANCE_SUGGESTIONS, ...customTypes.filter((c) => !INSURANCE_SUGGESTIONS.includes(c))];
  const filtered = allSuggestions.filter(
    (s) => s.toLowerCase().includes(search.toLowerCase()) && (s === policy.policyType || !usedTypes.includes(s)),
  );

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        if (search !== policy.policyType) onTypeChange(search);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [search, policy.policyType, onTypeChange]);

  function selectType(t: string) { setSearch(t); onTypeChange(t); setOpen(false); }

  const emoji = INSURANCE_EMOJI[policy.policyType] || '🛡️';
  const tooltip = INSURANCE_TOOLTIPS[policy.policyType];

  return (
    <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl relative animate-fade-in">
      <button type="button" onClick={onRemove}
        className="absolute top-3 right-3 w-7 h-7 rounded-full bg-red-100 dark:bg-red-900/50 text-red-600 dark:text-red-400
                   text-xs font-bold flex items-center justify-center hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors"
        title="Remove this policy">✕</button>

      <div className="relative mb-4 max-w-sm" ref={ref}>
        <div className="flex items-center gap-1.5 mb-1">
          <span className="text-lg">{emoji}</span>
          <label className="text-sm font-bold text-gray-700 dark:text-gray-300">Policy Type</label>
          {tooltip && <InfoTooltip text={tooltip}><span /></InfoTooltip>}
        </div>
        <input type="text" value={search}
          onChange={(e) => { setSearch(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault();
              if (filtered.length > 0 && search) selectType(filtered[0]);
              else { onTypeChange(search); setOpen(false); }
            }
          }}
          placeholder="Type to search policy type..."
          className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                     bg-white dark:bg-gray-800 dark:text-gray-100
                     focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all" />
        {open && filtered.length > 0 && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          max-h-48 overflow-y-auto animate-fade-in">
            {filtered.map((s) => (
              <button key={s} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => selectType(s)}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30
                           text-gray-700 dark:text-gray-300 flex items-center gap-2 transition-colors
                           first:rounded-t-xl last:rounded-b-xl">
                <span>{INSURANCE_EMOJI[s] || '🛡️'}</span><span>{s}</span>
              </button>
            ))}
          </div>
        )}
        {open && filtered.length === 0 && search.trim() && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                          border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                          px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
            Press <strong>Enter</strong> to use "<em>{search}</em>" as a new policy type
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <NumberField label="Cover Amount" value={policy.coverAmount} prefix="₹"
          onChange={(v) => onFieldChange('coverAmount', v)}
          tooltip="How much the policy pays out (sum insured). Term life: 10-15x annual income. Health: ₹10-20 lakh." />
        <NumberField label="Premium Amount" value={policy.premium} prefix="₹"
          onChange={(v) => onFieldChange('premium', v)}
          tooltip="How much you pay for this policy. Enter the amount per frequency selected." />
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Premium Frequency</label>
            <InfoTooltip text="How often do you pay the premium? Monthly, quarterly, half-yearly, or yearly."><span /></InfoTooltip>
          </div>
          <select value={policy.premiumFrequency} onChange={(e) => onFieldChange('premiumFrequency', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                       bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                       focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all">
            {PREMIUM_FREQUENCIES.map((f) => <option key={f} value={f}>{f}</option>)}
          </select>
        </div>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   InvestmentCard — instrument + fund name + amount/frequency + delete
   ══════════════════════════════════════════════════════════════ */
function InvestmentCard({ investment, onInstrumentChange, onFieldChange, onRemove }: {
  investment: InvestmentItem;
  onInstrumentChange: (t: string) => void;
  onFieldChange: (field: string, value: number | string) => void;
  onRemove: () => void;
}) {
  const [search, setSearch] = useState(investment.instrument);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const customInstruments = loadCustom('fa-custom-investment-instruments');
  const allSuggestions = [...INVESTMENT_SUGGESTIONS, ...customInstruments.filter((c) => !INVESTMENT_SUGGESTIONS.includes(c))];
  const filtered = allSuggestions.filter(
    (s) => s.toLowerCase().includes(search.toLowerCase()),
  );

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        if (search !== investment.instrument) onInstrumentChange(search);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [search, investment.instrument, onInstrumentChange]);

  function selectInstrument(t: string) { setSearch(t); onInstrumentChange(t); setOpen(false); }

  const emoji = INVESTMENT_EMOJI[investment.instrument] || '📈';
  const tooltip = INVESTMENT_TOOLTIPS[investment.instrument];

  return (
    <div className="p-4 bg-gray-50 dark:bg-gray-700/50 rounded-xl relative animate-fade-in">
      <button type="button" onClick={onRemove}
        className="absolute top-3 right-3 w-7 h-7 rounded-full bg-red-100 dark:bg-red-900/50 text-red-600 dark:text-red-400
                   text-xs font-bold flex items-center justify-center hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors"
        title="Remove this investment">✕</button>

      {/* Instrument type + fund name row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 mb-4">
        <div className="relative" ref={ref}>
          <div className="flex items-center gap-1.5 mb-1">
            <span className="text-lg">{emoji}</span>
            <label className="text-sm font-bold text-gray-700 dark:text-gray-300">Instrument Type</label>
            {tooltip && <InfoTooltip text={tooltip}><span /></InfoTooltip>}
          </div>
          <input type="text" value={search}
            onChange={(e) => { setSearch(e.target.value); setOpen(true); }}
            onFocus={() => setOpen(true)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') {
                e.preventDefault();
                if (filtered.length > 0 && search) selectInstrument(filtered[0]);
                else { onInstrumentChange(search); setOpen(false); }
              }
            }}
            placeholder="Type to search..."
            className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                       bg-white dark:bg-gray-800 dark:text-gray-100
                       focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all" />
          {open && filtered.length > 0 && (
            <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                            border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                            max-h-48 overflow-y-auto animate-fade-in">
              {filtered.map((s) => (
                <button key={s} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => selectInstrument(s)}
                  className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30
                             text-gray-700 dark:text-gray-300 flex items-center gap-2 transition-colors
                             first:rounded-t-xl last:rounded-b-xl">
                  <span>{INVESTMENT_EMOJI[s] || '📈'}</span><span>{s}</span>
                </button>
              ))}
            </div>
          )}
          {open && filtered.length === 0 && search.trim() && (
            <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                            border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                            px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
              Press <strong>Enter</strong> to use "<em>{search}</em>" as a new instrument
            </div>
          )}
        </div>
        <SchemeNameField
          instrument={investment.instrument}
          value={investment.name}
          onChange={(v) => onFieldChange('name', v)}
        />
      </div>

      {/* Amount + frequency row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <NumberField label="Investment Amount" value={investment.amount} prefix="₹"
          onChange={(v) => onFieldChange('amount', v)}
          tooltip="How much you invest per cycle. Enter the amount per frequency selected." />
        <div>
          <div className="flex items-center gap-1 mb-1.5">
            <label className="text-sm font-medium text-gray-700 dark:text-gray-300">Frequency</label>
            <InfoTooltip text="How often do you invest? Most SIPs are monthly. PPF can be yearly."><span /></InfoTooltip>
          </div>
          <select value={investment.frequency} onChange={(e) => onFieldChange('frequency', e.target.value)}
            className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                       bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                       focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all">
            {INVESTMENT_FREQUENCIES.map((f) => <option key={f} value={f}>{f}</option>)}
          </select>
        </div>
      </div>
      {investment.instrument && (
        <p className="mt-2 text-xs text-gray-400 dark:text-gray-500">
          📈 Assumed return: ~{INSTRUMENT_DEFAULT_RETURN[investment.instrument] ?? 10}% p.a.
        </p>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   SchemeNameField — autocomplete fund/scheme name based on instrument
   ══════════════════════════════════════════════════════════════ */
function SchemeNameField({ instrument, value, onChange }: {
  instrument: string;
  value: string;
  onChange: (v: string) => void;
}) {
  const [search, setSearch] = useState(value);
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  const schemes = SCHEME_SUGGESTIONS[instrument] || [];
  const hasSchemes = schemes.length > 0;
  const filtered = hasSchemes && search.length >= 1
    ? schemes.filter((s) => s.toLowerCase().includes(search.toLowerCase())).slice(0, 15)
    : [];

  useEffect(() => { setSearch(value); }, [value]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
        if (search !== value) onChange(search);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [search, value, onChange]);

  function selectScheme(s: string) { setSearch(s); onChange(s); setOpen(false); }

  const placeholder = hasSchemes
    ? `Type to search ${schemes.length}+ schemes...`
    : 'e.g. Fund or scheme name';

  return (
    <div className="relative" ref={ref}>
      <div className="flex items-center gap-1 mb-1.5">
        <label className="text-sm font-medium text-gray-700 dark:text-gray-300">
          {hasSchemes ? 'Fund / Scheme Name' : 'Name (optional)'}
        </label>
        <InfoTooltip text={hasSchemes
          ? `Start typing to search from ${schemes.length}+ popular schemes. You can also type a custom name.`
          : 'Optional: Name of the specific investment.'}><span /></InfoTooltip>
      </div>
      <input type="text" value={search}
        onChange={(e) => { setSearch(e.target.value); if (hasSchemes) setOpen(true); }}
        onFocus={() => { if (hasSchemes && search.length >= 1) setOpen(true); }}
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            e.preventDefault();
            if (filtered.length > 0 && search) selectScheme(filtered[0]);
            else { onChange(search); setOpen(false); }
          }
        }}
        placeholder={placeholder}
        className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm
                   bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100
                   focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all" />
      {open && filtered.length > 0 && (
        <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                        border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                        max-h-48 overflow-y-auto animate-fade-in">
          {filtered.map((s) => (
            <button key={s} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => selectScheme(s)}
              className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30
                         text-gray-700 dark:text-gray-300 transition-colors
                         first:rounded-t-xl last:rounded-b-xl">
              {s}
            </button>
          ))}
        </div>
      )}
      {open && search.length >= 1 && filtered.length === 0 && hasSchemes && (
        <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700
                        border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl
                        px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
          No match — press <strong>Enter</strong> to use "<em>{search}</em>"
        </div>
      )}
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   DynamicList — renders rows + add button (expenses & assets)
   ══════════════════════════════════════════════════════════════ */
interface DynamicListProps {
  items: DynamicItem[]; usedCategories: string[]; suggestions: string[];
  emojiMap: Record<string, string>; tooltipMap: Record<string, string>;
  storageKey: string; defaultEmoji: string; categoryLabel: string; amountLabel: string;
  emptyIcon: string; emptyText: string; addLabel: string;
  onAdd: () => void; onRemove: (id: string) => void;
  onCategoryChange: (id: string, cat: string) => void; onAmountChange: (id: string, amt: number) => void;
}

function DynamicList(p: DynamicListProps) {
  return (
    <>
      <div className="space-y-3">
        {p.items.length === 0 && (
          <div className="text-center py-8 text-gray-400 dark:text-gray-500">
            <span className="text-4xl block mb-3">{p.emptyIcon}</span>
            <p className="text-sm">{p.emptyText}</p>
          </div>
        )}
        {p.items.map((item) => (
          <DynamicRow key={item.id} item={item} usedCategories={p.usedCategories}
            suggestions={p.suggestions} emojiMap={p.emojiMap} tooltipMap={p.tooltipMap}
            storageKey={p.storageKey} defaultEmoji={p.defaultEmoji}
            categoryLabel={p.categoryLabel} amountLabel={p.amountLabel}
            onCategoryChange={(cat) => p.onCategoryChange(item.id, cat)}
            onAmountChange={(amt) => p.onAmountChange(item.id, amt)}
            onRemove={() => p.onRemove(item.id)} />
        ))}
      </div>
      <button onClick={p.onAdd} type="button"
        className="w-full mt-4 py-3 border-2 border-dashed border-gray-300 dark:border-gray-600 rounded-xl text-sm
                   text-gray-500 dark:text-gray-400 hover:border-indigo-400 dark:hover:border-indigo-600
                   hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50/50 dark:hover:bg-indigo-950/30 transition-all font-medium">
        {p.addLabel}
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
  item: DynamicItem; usedCategories: string[]; suggestions: string[];
  emojiMap: Record<string, string>; tooltipMap: Record<string, string>;
  storageKey: string; defaultEmoji: string; categoryLabel: string; amountLabel: string;
  onCategoryChange: (cat: string) => void; onAmountChange: (amt: number) => void; onRemove: () => void;
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

  function selectCategory(cat: string) { setSearch(cat); onCategoryChange(cat); setOpen(false); }

  const emoji = emojiMap[item.category] || defaultEmoji;
  const tooltip = tooltipMap[item.category];

  return (
    <div className="flex items-start gap-2 sm:gap-3 animate-fade-in">
      <div className="relative flex-1 min-w-0" ref={ref}>
        <div className="flex items-center gap-1 mb-1">
          <span className="text-sm">{emoji}</span>
          <label className="text-xs font-medium text-gray-600 dark:text-gray-400">{categoryLabel}</label>
          {tooltip && <InfoTooltip text={tooltip}><span /></InfoTooltip>}
        </div>
        <input type="text" value={search}
          onChange={(e) => { setSearch(e.target.value); setOpen(true); }}
          onFocus={() => setOpen(true)}
          onKeyDown={(e) => {
            if (e.key === 'Enter') { e.preventDefault(); if (filtered.length > 0 && search) selectCategory(filtered[0]); else { onCategoryChange(search); setOpen(false); } }
          }}
          placeholder="Type to search..."
          className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all" />
        {open && filtered.length > 0 && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl max-h-48 overflow-y-auto animate-fade-in">
            {filtered.map((s) => (
              <button key={s} type="button" onMouseDown={(e) => e.preventDefault()} onClick={() => selectCategory(s)}
                className="w-full text-left px-3 py-2.5 text-sm hover:bg-indigo-50 dark:hover:bg-indigo-900/30 text-gray-700 dark:text-gray-300 flex items-center gap-2 transition-colors first:rounded-t-xl last:rounded-b-xl">
                <span>{emojiMap[s] || defaultEmoji}</span><span>{s}</span>
              </button>
            ))}
          </div>
        )}
        {open && filtered.length === 0 && search.trim() && (
          <div className="absolute z-50 top-full left-0 right-0 mt-1 bg-white dark:bg-gray-700 border border-gray-200 dark:border-gray-600 rounded-xl shadow-xl px-3 py-2.5 text-sm text-gray-500 dark:text-gray-400 animate-fade-in">
            Press <strong>Enter</strong> to use "<em>{search}</em>" as a new type
          </div>
        )}
      </div>
      <div className="w-36 sm:w-44 shrink-0">
        <label className="text-xs font-medium text-gray-600 dark:text-gray-400 mb-1 block">{amountLabel}</label>
        <div className="relative">
          <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-400 dark:text-gray-500 font-medium">₹</span>
          <input type="number" value={item.amount || ''} onChange={(e) => onAmountChange(Number(e.target.value) || 0)} placeholder="0"
            className="w-full pl-8 pr-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none transition-all" />
        </div>
      </div>
      <div className="pt-6">
        <button type="button" onClick={onRemove}
          className="w-8 h-8 rounded-full bg-red-100 dark:bg-red-900/40 text-red-500 dark:text-red-400 text-xs font-bold flex items-center justify-center hover:bg-red-200 dark:hover:bg-red-800/50 transition-colors focus:outline-none focus:ring-2 focus:ring-red-400"
          title="Remove this item">✕</button>
      </div>
    </div>
  );
}

/* ══════════════════════════════════════════════════════════════
   Reusable form helpers
   ══════════════════════════════════════════════════════════════ */
function TotalBar({ color, label, value }: { color: string; label: string; value: number }) {
  const colors: Record<string, string> = {
    indigo: 'bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-400',
    orange: 'bg-orange-50 dark:bg-orange-950/40 text-orange-700 dark:text-orange-400 border border-orange-200 dark:border-orange-800',
    emerald: 'bg-emerald-50 dark:bg-emerald-950/40 text-emerald-700 dark:text-emerald-400 border border-emerald-200 dark:border-emerald-800',
  };
  return (
    <div className={`mt-4 p-4 rounded-xl ${colors[color] || colors.indigo}`}>
      <p className="text-sm font-semibold">💡 {label}: ₹{value.toLocaleString('en-IN')}</p>
    </div>
  );
}

function FormSection({ title, hint, icon, children }: { title: string; hint: string; icon: string; children: React.ReactNode }) {
  return (
    <div>
      <div className="mb-6">
        <h3 className="text-lg font-bold text-gray-800 dark:text-gray-100 flex items-center gap-2">{icon} {title}</h3>
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
  label: string; value: number; onChange: (v: number) => void; tooltip: string; prefix?: string; placeholder?: string;
}) {
  return (
    <div>
      <div className="flex items-center gap-1 mb-1.5">
        <label className="text-sm font-medium text-gray-700 dark:text-gray-300">{label}</label>
        <InfoTooltip text={tooltip}><span /></InfoTooltip>
      </div>
      <div className="relative">
        {prefix && <span className="absolute left-3 top-1/2 -translate-y-1/2 text-sm text-gray-400 dark:text-gray-500 font-medium">{prefix}</span>}
        <input type="number" value={value || ''} onChange={(e) => onChange(Number(e.target.value) || 0)} placeholder={placeholder || '0'}
          className={`w-full ${prefix ? 'pl-8' : 'pl-3'} pr-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all`} />
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
      <input type="text" value={value} onChange={(e) => onChange(e.target.value)} placeholder={placeholder}
        className="w-full px-3 py-2.5 border border-gray-200 dark:border-gray-600 rounded-xl text-sm focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all" />
    </div>
  );
}
