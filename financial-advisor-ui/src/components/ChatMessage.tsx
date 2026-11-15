interface ChatMessageProps {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  warnings?: string[];
  timestamp?: string;
}

const sourceConfig: Record<string, { label: string; icon: string; bg: string; text: string }> = {
  ai: { label: 'AI Advisor', icon: '🤖', bg: 'bg-emerald-100 dark:bg-emerald-900/50', text: 'text-emerald-700 dark:text-emerald-400' },
  'rule-engine': { label: 'Calculator', icon: '🧮', bg: 'bg-amber-100 dark:bg-amber-900/50', text: 'text-amber-700 dark:text-amber-400' },
  guard: { label: 'Safety Check', icon: '🛡️', bg: 'bg-red-100 dark:bg-red-900/50', text: 'text-red-700 dark:text-red-400' },
  error: { label: 'Error', icon: '❌', bg: 'bg-red-100 dark:bg-red-900/50', text: 'text-red-700 dark:text-red-400' },
};

export default function ChatMessage({ role, content, source, warnings, timestamp }: ChatMessageProps) {
  const isUser = role === 'user';
  const srcCfg = source ? sourceConfig[source] ?? sourceConfig.ai : null;

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] sm:max-w-[75%] ${isUser ? '' : 'flex items-start gap-3'}`}>
        {!isUser && (
          <div className="w-8 h-8 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl
                          flex items-center justify-center text-white text-sm shrink-0 mt-1 shadow-sm">
            💬
          </div>
        )}

        <div
          className={`rounded-2xl px-5 py-3.5 ${
            isUser
              ? 'bg-gradient-to-r from-indigo-600 to-purple-600 text-white shadow-md'
              : 'bg-white dark:bg-gray-800 border border-gray-200 dark:border-gray-700 text-gray-800 dark:text-gray-200 shadow-sm'
          }`}
        >
          {!isUser && srcCfg && (
            <div className="flex items-center gap-2 mb-2">
              <span
                className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full
                            ${srcCfg.bg} ${srcCfg.text} flex items-center gap-1`}
              >
                {srcCfg.icon} {srcCfg.label}
              </span>
              {timestamp && (
                <span className="text-[10px] text-gray-300 dark:text-gray-500">
                  {new Date(timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                </span>
              )}
            </div>
          )}

          <div className="text-sm whitespace-pre-wrap leading-relaxed">{content}</div>

          {warnings && warnings.length > 0 && (
            <div className="mt-3 pt-3 border-t border-gray-200/40 dark:border-gray-700/40">
              <p className="text-[11px] text-gray-400 dark:text-gray-500 italic">
                💡 Some numbers above are AI-interpreted. Always cross-check important figures on your Dashboard.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
