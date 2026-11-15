import type { DimensionStatus } from '@/types/api';

const statusColors: Record<string, string> = {
  healthy: 'bg-green-100 text-green-800 border-green-200',
  'on-track': 'bg-green-100 text-green-800 border-green-200',
  'debt-free': 'bg-green-100 text-green-800 border-green-200',
  clear: 'bg-green-100 text-green-800 border-green-200',
  diversified: 'bg-green-100 text-green-800 border-green-200',
  manageable: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  review: 'bg-yellow-100 text-yellow-800 border-yellow-200',
  none: 'bg-gray-100 text-gray-600 border-gray-200',
  'not-set': 'bg-gray-100 text-gray-600 border-gray-200',
  concentrated: 'bg-orange-100 text-orange-800 border-orange-200',
  'at-risk': 'bg-red-100 text-red-800 border-red-200',
  critical: 'bg-red-100 text-red-800 border-red-200',
  high: 'bg-red-100 text-red-800 border-red-200',
  'attention-needed': 'bg-red-100 text-red-800 border-red-200',
};

const dimensionIcons: Record<number, string> = {
  1: '💧', 2: '🛡️', 3: '📉', 4: '🏖️', 5: '🎓',
  6: '⚖️', 7: '💰', 8: '🚨', 9: '🏠', 10: '⚠️',
};

export default function DimensionCard({ dim }: { dim: DimensionStatus }) {
  const colors = statusColors[dim.status] ?? 'bg-gray-100 text-gray-700 border-gray-200';
  const icon = dimensionIcons[dim.index] ?? '📋';

  return (
    <div className={`rounded-xl border p-5 ${colors} transition-all hover:shadow-md`}>
      <div className="flex items-start justify-between">
        <div className="flex items-center gap-2">
          <span className="text-xl">{icon}</span>
          <h3 className="font-semibold text-sm">{dim.name}</h3>
        </div>
        <span className="text-xs font-medium px-2 py-0.5 rounded-full bg-white/60 uppercase tracking-wide">
          {dim.status}
        </span>
      </div>
      <p className="mt-2 text-sm opacity-80">{dim.description}</p>
      {dim.actions.length > 0 && (
        <ul className="mt-3 space-y-1">
          {dim.actions.map((action, i) => (
            <li key={i} className="text-xs flex items-start gap-1.5">
              <span className="mt-0.5">→</span>
              <span>{action}</span>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
