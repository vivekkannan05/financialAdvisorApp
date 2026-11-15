import type { DimensionStatus } from '@/types/api';
import InfoTooltip from './InfoTooltip';

const statusConfig: Record<string, { bg: string; border: string; text: string; badge: string; emoji: string }> = {
  healthy:          { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', badge: 'bg-emerald-200 text-emerald-800', emoji: '✅' },
  'on-track':       { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', badge: 'bg-emerald-200 text-emerald-800', emoji: '✅' },
  'debt-free':      { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', badge: 'bg-emerald-200 text-emerald-800', emoji: '🎉' },
  clear:            { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', badge: 'bg-emerald-200 text-emerald-800', emoji: '✅' },
  diversified:      { bg: 'bg-emerald-50', border: 'border-emerald-200', text: 'text-emerald-800', badge: 'bg-emerald-200 text-emerald-800', emoji: '👍' },
  manageable:       { bg: 'bg-amber-50',   border: 'border-amber-200',   text: 'text-amber-800',   badge: 'bg-amber-200 text-amber-800',   emoji: '⚡' },
  review:           { bg: 'bg-amber-50',   border: 'border-amber-200',   text: 'text-amber-800',   badge: 'bg-amber-200 text-amber-800',   emoji: '👀' },
  'near-target':    { bg: 'bg-amber-50',   border: 'border-amber-200',   text: 'text-amber-800',   badge: 'bg-amber-200 text-amber-800',   emoji: '🏃' },
  none:             { bg: 'bg-gray-50',     border: 'border-gray-200',    text: 'text-gray-600',    badge: 'bg-gray-200 text-gray-700',     emoji: '➖' },
  'not-set':        { bg: 'bg-gray-50',     border: 'border-gray-200',    text: 'text-gray-600',    badge: 'bg-gray-200 text-gray-700',     emoji: '❓' },
  concentrated:     { bg: 'bg-orange-50',   border: 'border-orange-200',  text: 'text-orange-800',  badge: 'bg-orange-200 text-orange-800', emoji: '⚠️' },
  'at-risk':        { bg: 'bg-red-50',      border: 'border-red-200',     text: 'text-red-800',     badge: 'bg-red-200 text-red-800',       emoji: '🚨' },
  critical:         { bg: 'bg-red-50',      border: 'border-red-200',     text: 'text-red-800',     badge: 'bg-red-200 text-red-800',       emoji: '🔴' },
  high:             { bg: 'bg-red-50',      border: 'border-red-200',     text: 'text-red-800',     badge: 'bg-red-200 text-red-800',       emoji: '📛' },
  'attention-needed': { bg: 'bg-red-50',    border: 'border-red-200',     text: 'text-red-800',     badge: 'bg-red-200 text-red-800',       emoji: '⚠️' },
};

const dimensionMeta: Record<number, { icon: string; friendlyName: string; tooltip: string }> = {
  1:  { icon: '💧', friendlyName: 'Cash Available', tooltip: 'This shows how much money you can access quickly (like savings accounts, FDs) vs money locked in things like gold or property. More liquid = more flexible.' },
  2:  { icon: '🛡️', friendlyName: 'Insurance Cover', tooltip: 'Insurance protects your family if something bad happens. Term life insurance pays your family if you pass away. Health insurance pays hospital bills. Both are important!' },
  3:  { icon: '💳', friendlyName: 'Loans & Debt', tooltip: 'This shows your total loans (home loan, car loan, etc). Less debt = better. If your loans are more than half your savings, it\'s a warning sign.' },
  4:  { icon: '🏖️', friendlyName: 'Retirement Plan', tooltip: 'Are you saving enough to stop working someday? This tracks if your retirement savings are growing fast enough to reach your target amount.' },
  5:  { icon: '🎓', friendlyName: 'Education Fund', tooltip: 'If you\'re saving for a child\'s education (college, abroad studies), this shows if you\'re on track. Education costs go up every year, so start early!' },
  6:  { icon: '⚖️', friendlyName: 'Risk Level', tooltip: 'This is how much risk your investments should take. Young people with less debt can take more risk (stocks). More debt or short time = less risk (FDs, bonds).' },
  7:  { icon: '💰', friendlyName: 'Income Sources', tooltip: 'Having money coming from different places (salary + freelance + rent) is safer. If one source stops, you still have others. This is called diversification.' },
  8:  { icon: '🚨', friendlyName: 'Emergency Fund', tooltip: 'Emergency fund = money set aside for unexpected problems (job loss, medical emergency). You should have at least 6 months of expenses saved up!' },
  9:  { icon: '🏠', friendlyName: 'Home Loan Load', tooltip: 'Your home loan EMI compared to your salary. If it\'s more than 40% of your income, it\'s too much and leaves less money for other things.' },
  10: { icon: '📋', friendlyName: 'Action Items', tooltip: 'These are things you should do to improve your money situation. Think of them as homework for your finances! Start with the most important ones.' },
};

const statusLabels: Record<string, string> = {
  healthy: 'Looking good!',
  'on-track': 'On track!',
  'debt-free': 'No debt!',
  clear: 'All clear!',
  diversified: 'Well spread!',
  manageable: 'Okay for now',
  review: 'Needs a look',
  'near-target': 'Almost there!',
  none: 'Not applicable',
  'not-set': 'Not set up yet',
  concentrated: 'Too focused',
  'at-risk': 'Needs attention',
  critical: 'Urgent!',
  high: 'Too high',
  'attention-needed': 'Action needed',
  very_low: 'Very safe',
  low: 'Cautious',
  moderate: 'Balanced',
};

export default function DimensionCard({ dim }: { dim: DimensionStatus }) {
  const cfg = statusConfig[dim.status] ?? statusConfig.none;
  const meta = dimensionMeta[dim.index] ?? { icon: '📋', friendlyName: dim.name, tooltip: '' };
  const friendlyStatus = statusLabels[dim.status] ?? dim.status;

  return (
    <div className={`rounded-2xl border-2 ${cfg.border} ${cfg.bg} p-5 transition-all duration-300
                     hover:shadow-lg hover:-translate-y-0.5 group`}>
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <span className="text-2xl shrink-0">{meta.icon}</span>
          <InfoTooltip text={meta.tooltip}>
            <h3 className="font-bold text-sm text-gray-800 leading-tight">{meta.friendlyName}</h3>
          </InfoTooltip>
        </div>
        <span className={`shrink-0 text-[11px] font-bold px-2.5 py-1 rounded-full ${cfg.badge} flex items-center gap-1`}>
          {cfg.emoji} {friendlyStatus}
        </span>
      </div>

      {/* Description in simple words */}
      <p className={`mt-3 text-sm ${cfg.text} leading-relaxed`}>
        {dim.description}
      </p>

      {/* Action items */}
      {dim.actions.length > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-200/60 space-y-1.5">
          <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400">What to do:</p>
          {dim.actions.map((action, i) => (
            <div key={i} className="flex items-start gap-2 text-xs text-gray-600">
              <span className="mt-0.5 text-indigo-500">👉</span>
              <span>{action}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
