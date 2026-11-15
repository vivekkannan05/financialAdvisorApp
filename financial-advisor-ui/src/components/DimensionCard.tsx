import { useNavigate } from 'react-router-dom';
import type { DimensionStatus } from '@/types/api';
import { useChatPrefill } from '@/context/ChatPrefillContext';
import InfoTooltip from './InfoTooltip';

const statusConfig: Record<string, { bg: string; border: string; text: string; badge: string; emoji: string }> = {
  healthy:          { bg: 'bg-emerald-50 dark:bg-emerald-950/40', border: 'border-emerald-200 dark:border-emerald-800', text: 'text-emerald-800 dark:text-emerald-300', badge: 'bg-emerald-200 dark:bg-emerald-900 text-emerald-800 dark:text-emerald-300', emoji: '✅' },
  'on-track':       { bg: 'bg-emerald-50 dark:bg-emerald-950/40', border: 'border-emerald-200 dark:border-emerald-800', text: 'text-emerald-800 dark:text-emerald-300', badge: 'bg-emerald-200 dark:bg-emerald-900 text-emerald-800 dark:text-emerald-300', emoji: '✅' },
  'debt-free':      { bg: 'bg-emerald-50 dark:bg-emerald-950/40', border: 'border-emerald-200 dark:border-emerald-800', text: 'text-emerald-800 dark:text-emerald-300', badge: 'bg-emerald-200 dark:bg-emerald-900 text-emerald-800 dark:text-emerald-300', emoji: '🎉' },
  clear:            { bg: 'bg-emerald-50 dark:bg-emerald-950/40', border: 'border-emerald-200 dark:border-emerald-800', text: 'text-emerald-800 dark:text-emerald-300', badge: 'bg-emerald-200 dark:bg-emerald-900 text-emerald-800 dark:text-emerald-300', emoji: '✅' },
  diversified:      { bg: 'bg-emerald-50 dark:bg-emerald-950/40', border: 'border-emerald-200 dark:border-emerald-800', text: 'text-emerald-800 dark:text-emerald-300', badge: 'bg-emerald-200 dark:bg-emerald-900 text-emerald-800 dark:text-emerald-300', emoji: '👍' },
  manageable:       { bg: 'bg-amber-50 dark:bg-amber-950/40',     border: 'border-amber-200 dark:border-amber-800',     text: 'text-amber-800 dark:text-amber-300',     badge: 'bg-amber-200 dark:bg-amber-900 text-amber-800 dark:text-amber-300',     emoji: '⚡' },
  review:           { bg: 'bg-amber-50 dark:bg-amber-950/40',     border: 'border-amber-200 dark:border-amber-800',     text: 'text-amber-800 dark:text-amber-300',     badge: 'bg-amber-200 dark:bg-amber-900 text-amber-800 dark:text-amber-300',     emoji: '👀' },
  'near-target':    { bg: 'bg-amber-50 dark:bg-amber-950/40',     border: 'border-amber-200 dark:border-amber-800',     text: 'text-amber-800 dark:text-amber-300',     badge: 'bg-amber-200 dark:bg-amber-900 text-amber-800 dark:text-amber-300',     emoji: '🏃' },
  none:             { bg: 'bg-gray-50 dark:bg-gray-800/40',        border: 'border-gray-200 dark:border-gray-700',       text: 'text-gray-600 dark:text-gray-400',       badge: 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300',       emoji: '➖' },
  'not-set':        { bg: 'bg-gray-50 dark:bg-gray-800/40',        border: 'border-gray-200 dark:border-gray-700',       text: 'text-gray-600 dark:text-gray-400',       badge: 'bg-gray-200 dark:bg-gray-700 text-gray-700 dark:text-gray-300',       emoji: '❓' },
  concentrated:     { bg: 'bg-orange-50 dark:bg-orange-950/40',    border: 'border-orange-200 dark:border-orange-800',   text: 'text-orange-800 dark:text-orange-300',   badge: 'bg-orange-200 dark:bg-orange-900 text-orange-800 dark:text-orange-300', emoji: '⚠️' },
  'at-risk':        { bg: 'bg-red-50 dark:bg-red-950/40',          border: 'border-red-200 dark:border-red-800',         text: 'text-red-800 dark:text-red-300',         badge: 'bg-red-200 dark:bg-red-900 text-red-800 dark:text-red-300',         emoji: '🚨' },
  critical:         { bg: 'bg-red-50 dark:bg-red-950/40',          border: 'border-red-200 dark:border-red-800',         text: 'text-red-800 dark:text-red-300',         badge: 'bg-red-200 dark:bg-red-900 text-red-800 dark:text-red-300',         emoji: '🔴' },
  high:             { bg: 'bg-red-50 dark:bg-red-950/40',          border: 'border-red-200 dark:border-red-800',         text: 'text-red-800 dark:text-red-300',         badge: 'bg-red-200 dark:bg-red-900 text-red-800 dark:text-red-300',         emoji: '📛' },
  'attention-needed': { bg: 'bg-red-50 dark:bg-red-950/40',        border: 'border-red-200 dark:border-red-800',         text: 'text-red-800 dark:text-red-300',         badge: 'bg-red-200 dark:bg-red-900 text-red-800 dark:text-red-300',         emoji: '⚠️' },
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

function buildAdvisorPrompt(actionText: string): string {
  const lower = actionText.toLowerCase();

  if (lower.includes('emergency fund')) {
    return `My dashboard says my emergency fund is low. ${actionText} — What's the best strategy to build this up without hurting my other goals?`;
  }
  if (lower.includes('loans') || lower.includes('debt')) {
    return `My dashboard flagged my debt level: ${actionText} — Should I focus on paying off debt first, or keep investing for my goals? What's the right balance?`;
  }
  if (lower.includes('spending more than')) {
    return `My dashboard says I have a cash flow deficit — I'm spending more than I earn. How should I fix this? Where should I cut back?`;
  }
  if (lower.includes('no money set aside')) {
    return `${actionText} — I haven't started saving for this yet. What type of investment should I start with, and how much per month?`;
  }
  if (lower.includes('% funded') || lower.includes('underfunded')) {
    return `${actionText} — Is this realistic? What changes should I make to get on track?`;
  }

  return `My dashboard flagged this: "${actionText}" — What should I do about it?`;
}

export default function DimensionCard({ dim }: { dim: DimensionStatus }) {
  const cfg = statusConfig[dim.status] ?? statusConfig.none;
  const meta = dimensionMeta[dim.index] ?? { icon: '📋', friendlyName: dim.name, tooltip: '' };
  const friendlyStatus = statusLabels[dim.status] ?? dim.status;
  const navigate = useNavigate();
  const { setPrefill } = useChatPrefill();

  function discussWithAdvisor(actionText: string) {
    setPrefill(buildAdvisorPrompt(actionText));
    navigate('/chat');
  }

  return (
    <div className={`rounded-2xl border-2 ${cfg.border} ${cfg.bg} p-5 transition-all duration-300
                     hover:shadow-lg hover:-translate-y-0.5 group`}>
      {/* Header */}
      <div className="flex items-start justify-between gap-2">
        <div className="flex items-center gap-2.5 min-w-0">
          <span className="text-2xl shrink-0">{meta.icon}</span>
          <InfoTooltip text={meta.tooltip}>
            <h3 className="font-bold text-sm text-gray-800 dark:text-gray-100 leading-tight">{meta.friendlyName}</h3>
          </InfoTooltip>
        </div>
        <span className={`shrink-0 text-[11px] font-bold px-2.5 py-1 rounded-full ${cfg.badge} flex items-center gap-1`}>
          {cfg.emoji} {friendlyStatus}
        </span>
      </div>

      {/* Description */}
      <p className={`mt-3 text-sm ${cfg.text} leading-relaxed`}>
        {dim.description}
      </p>

      {/* Action items */}
      {dim.actions.length > 0 && (
        <div className="mt-3 pt-3 border-t border-gray-200/60 dark:border-gray-700/60 space-y-2">
          <p className="text-[10px] font-bold uppercase tracking-wider text-gray-400 dark:text-gray-500">What to do:</p>
          {dim.actions.map((action, i) => (
            <div key={i} className="flex items-start gap-2 text-xs text-gray-600 dark:text-gray-400">
              <span className="mt-0.5 text-indigo-500 dark:text-indigo-400 shrink-0">👉</span>
              <div className="flex-1 space-y-1">
                <span>{action}</span>
                <button
                  onClick={() => discussWithAdvisor(action)}
                  className="flex items-center gap-1 text-[11px] font-medium
                             text-indigo-500 dark:text-indigo-400
                             hover:text-indigo-700 dark:hover:text-indigo-300
                             hover:underline transition-colors mt-0.5"
                >
                  <span>💬</span> Discuss with Advisor
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
