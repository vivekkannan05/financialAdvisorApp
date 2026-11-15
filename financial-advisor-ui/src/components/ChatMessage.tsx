interface ChatMessageProps {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  warnings?: string[];
  timestamp?: string;
}

const sourceConfig: Record<string, { label: string; icon: string; bg: string; text: string }> = {
  ai: { label: 'AI Advisor', icon: '🤖', bg: 'bg-emerald-100', text: 'text-emerald-700' },
  'rule-engine': { label: 'Calculator', icon: '🧮', bg: 'bg-amber-100', text: 'text-amber-700' },
  guard: { label: 'Safety Check', icon: '🛡️', bg: 'bg-red-100', text: 'text-red-700' },
  error: { label: 'Error', icon: '❌', bg: 'bg-red-100', text: 'text-red-700' },
};

export default function ChatMessage({ role, content, source, warnings, timestamp }: ChatMessageProps) {
  const isUser = role === 'user';
  const srcCfg = source ? sourceConfig[source] ?? sourceConfig.ai : null;

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div className={`max-w-[85%] sm:max-w-[75%] ${isUser ? '' : 'flex items-start gap-3'}`}>
        {/* Avatar for assistant */}
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
              : 'bg-white border border-gray-200 text-gray-800 shadow-sm'
          }`}
        >
          {/* Source badge */}
          {!isUser && srcCfg && (
            <div className="flex items-center gap-2 mb-2">
              <span
                className={`text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded-full
                            ${srcCfg.bg} ${srcCfg.text} flex items-center gap-1`}
              >
                {srcCfg.icon} {srcCfg.label}
              </span>
              {timestamp && (
                <span className="text-[10px] text-gray-300">
                  {new Date(timestamp).toLocaleTimeString('en-IN', { hour: '2-digit', minute: '2-digit' })}
                </span>
              )}
            </div>
          )}

          {/* Message content */}
          <div className="text-sm whitespace-pre-wrap leading-relaxed">{content}</div>

          {/* Warnings */}
          {warnings && warnings.length > 0 && (
            <div className="mt-3 pt-3 border-t border-amber-200/60 space-y-1.5">
              <p className="text-[10px] font-bold text-amber-600 uppercase tracking-wider">⚠️ Please note:</p>
              {warnings.map((w, i) => (
                <p key={i} className="text-xs text-amber-600 flex items-start gap-1.5">
                  <span className="shrink-0 mt-0.5">•</span>
                  <span>{w}</span>
                </p>
              ))}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
