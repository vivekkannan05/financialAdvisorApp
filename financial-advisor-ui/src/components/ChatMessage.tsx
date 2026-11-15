interface ChatMessageProps {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  warnings?: string[];
  timestamp?: string;
}

export default function ChatMessage({ role, content, source, warnings, timestamp }: ChatMessageProps) {
  const isUser = role === 'user';

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'}`}>
      <div
        className={`max-w-[80%] rounded-2xl px-4 py-3 ${
          isUser
            ? 'bg-indigo-600 text-white'
            : 'bg-white border border-gray-200 text-gray-800'
        }`}
      >
        {!isUser && source && (
          <div className="flex items-center gap-2 mb-1.5">
            <span
              className={`text-[10px] font-medium uppercase tracking-wider px-1.5 py-0.5 rounded ${
                source === 'ai'
                  ? 'bg-green-100 text-green-700'
                  : source === 'rule-engine'
                  ? 'bg-amber-100 text-amber-700'
                  : 'bg-red-100 text-red-700'
              }`}
            >
              {source}
            </span>
            {timestamp && (
              <span className="text-[10px] text-gray-400">
                {new Date(timestamp).toLocaleTimeString()}
              </span>
            )}
          </div>
        )}
        <div className="text-sm whitespace-pre-wrap leading-relaxed">{content}</div>
        {warnings && warnings.length > 0 && (
          <div className="mt-2 pt-2 border-t border-amber-200">
            {warnings.map((w, i) => (
              <p key={i} className="text-xs text-amber-600 flex items-start gap-1">
                <span>⚠️</span> {w}
              </p>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
