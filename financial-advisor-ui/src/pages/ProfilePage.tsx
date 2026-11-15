import { useEffect, useState } from 'react';
import { fetchProfile, updateProfile, createProfile } from '@/api/profile';
import type { ProfileSnapshot } from '@/types/api';

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileSnapshot | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [rawJson, setRawJson] = useState('');

  useEffect(() => {
    fetchProfile()
      .then((p) => {
        setProfile(p);
        setRawJson(JSON.stringify(p.data, null, 2));
      })
      .catch((err) => {
        if (err.response?.status === 404) {
          setRawJson(JSON.stringify(sampleProfile, null, 2));
        } else {
          setError(err.response?.data?.message ?? 'Failed to load profile');
        }
      })
      .finally(() => setLoading(false));
  }, []);

  async function handleSave() {
    setSaving(true);
    setError(null);
    setSuccess(null);
    try {
      const data = JSON.parse(rawJson);
      const saved = profile
        ? await updateProfile(data)
        : await createProfile(data);
      setProfile(saved);
      setSuccess(
        saved.snapshotId === profile?.snapshotId
          ? 'No changes detected — profile unchanged.'
          : 'Profile saved successfully!'
      );
    } catch (err: unknown) {
      if (err instanceof SyntaxError) {
        setError('Invalid JSON — please fix syntax errors');
      } else {
        const msg = err instanceof Error ? err.message : 'Save failed';
        setError(msg);
      }
    } finally {
      setSaving(false);
    }
  }

  if (loading) {
    return (
      <div className="p-8 text-gray-400 text-sm animate-pulse">Loading profile...</div>
    );
  }

  return (
    <div className="p-8 space-y-6">
      <div className="flex items-center justify-between">
        <div>
          <h2 className="text-2xl font-bold text-gray-900">Financial Profile</h2>
          {profile && (
            <p className="text-sm text-gray-500 mt-1">
              Snapshot: {profile.snapshotId} &middot; {profile.snapshotDate}
            </p>
          )}
          {!profile && (
            <p className="text-sm text-amber-600 mt-1">
              No profile yet — paste your financial data below and save.
            </p>
          )}
        </div>
        <button
          onClick={handleSave}
          disabled={saving}
          className="px-5 py-2 bg-indigo-600 text-white text-sm font-medium rounded-lg hover:bg-indigo-700 disabled:opacity-50 transition-colors"
        >
          {saving ? 'Saving...' : profile ? 'Update Profile' : 'Create Profile'}
        </button>
      </div>

      {error && (
        <div className="bg-red-50 border border-red-200 text-red-700 rounded-lg p-3 text-sm">
          {error}
        </div>
      )}
      {success && (
        <div className="bg-green-50 border border-green-200 text-green-700 rounded-lg p-3 text-sm">
          {success}
        </div>
      )}

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        <div className="bg-gray-50 px-4 py-2 border-b border-gray-200">
          <p className="text-xs text-gray-500 font-medium">
            Profile JSON — edit sections: income, expenses, assets, liabilities, insurance, goals, riskProfile
          </p>
        </div>
        <textarea
          value={rawJson}
          onChange={(e) => setRawJson(e.target.value)}
          className="w-full h-[60vh] p-4 font-mono text-sm text-gray-800 resize-none focus:outline-none"
          spellCheck={false}
        />
      </div>
    </div>
  );
}

const sampleProfile = {
  income: {
    monthlySalary: 200000,
    otherMonthly: 0,
  },
  expenses: {
    monthlyHousehold: 88000,
  },
  assets: {
    mutualFunds: { value: 344698, type: 'equity' },
    indianStocks: { value: 159611, type: 'equity' },
    emergencyFD: { value: 900000, goalTag: 'emergency', locked: true },
  },
  liabilities: {},
  insurance: {
    termLife: { type: 'term', cover: 10000000, premiumYearly: 18000 },
    healthFloater: { type: 'health', cover: 1000000, premiumYearly: 25000 },
  },
  goals: [
    { id: 'retirement', name: 'Retirement', targetAmountToday: 50000000, targetYear: 2050 },
    { id: 'education', name: 'Child Education', targetAmountToday: 5000000, targetYear: 2040 },
  ],
  riskProfile: {
    behaviouralTolerance: 'moderate',
    age: 32,
    retirementAge: 60,
  },
};
