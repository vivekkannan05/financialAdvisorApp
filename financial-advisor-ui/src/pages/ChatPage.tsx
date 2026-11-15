import { useState, useRef, useEffect, type FormEvent } from 'react';
import { sendMessage } from '@/api/chat';
import ChatMessage from '@/components/ChatMessage';
import type { ChatResponse } from '@/types/api';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  source?: string;
  warnings?: string[];
  timestamp?: string;
}

const suggestedQuestions = [
  { icon: '🏥', text: 'Am I financially healthy?', category: 'Health Check' },
  { icon: '🎯', text: 'Am I saving enough for retirement?', category: 'Goals' },
  { icon: '🚨', text: 'Do I have enough emergency fund?', category: 'Safety' },
  { icon: '💳', text: 'Should I pay off loans faster or invest more?', category: 'Strategy' },
  { icon: '📊', text: 'How risky are my investments?', category: 'Risk' },
  { icon: '🎓', text: 'Am I on track for my child\'s education fund?', category: 'Goals' },
];

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        '👋 Hi there! I\'m your personal money advisor.\n\n' +
        'You can ask me anything about your finances in plain, simple language. For example:\n\n' +
        '• "Am I saving enough?"\n' +
        '• "What happens if I lose my job?"\n' +
        '• "Should I invest more or pay off my loan?"\n\n' +
        'I\'ll look at your actual financial data and give you honest, evidence-based advice. No guessing! 🎯',
      source: 'ai',
    },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [showSuggestions, setShowSuggestions] = useState(true);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSend(text?: string) {
    const msgText = (text ?? input).trim();
    if (!msgText || sending) return;

    setInput('');
    setShowSuggestions(false);
    setMessages((prev) => [...prev, { role: 'user', content: msgText }]);
    setSending(true);

    try {
      const response: ChatResponse = await sendMessage(msgText);
      setMessages((prev) => [
        ...prev,
        {
          role: 'assistant',
          content: response.message,
          source: response.source,
          warnings: response.validationWarnings ?? undefined,
          timestamp: response.timestamp,
        },
      ]);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Something went wrong';
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: `😞 Sorry, something went wrong: ${msg}`, source: 'error' },
      ]);
    } finally {
      setSending(false);
    }
  }

  function handleSubmit(e: FormEvent) {
    e.preventDefault();
    handleSend();
  }

  return (
    <div className="flex flex-col h-screen">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl
                          flex items-center justify-center text-lg shadow-md">
            💬
          </div>
          <div>
            <h2 className="text-lg font-bold text-gray-800">Ask Your Money Advisor</h2>
            <p className="text-xs text-gray-400">
              Ask in plain English · Answers backed by your real financial data · No jargon
            </p>
          </div>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4 bg-gradient-to-b from-gray-50 to-white">
        {messages.map((msg, i) => (
          <ChatMessage
            key={i}
            role={msg.role}
            content={msg.content}
            source={msg.source}
            warnings={msg.warnings}
            timestamp={msg.timestamp}
          />
        ))}

        {/* Suggested questions */}
        {showSuggestions && (
          <div className="py-4">
            <p className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-3 text-center">
              💡 Try asking one of these:
            </p>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 max-w-xl mx-auto">
              {suggestedQuestions.map((q) => (
                <button
                  key={q.text}
                  onClick={() => handleSend(q.text)}
                  className="text-left px-4 py-3 bg-white border border-gray-200 rounded-xl text-sm
                             hover:border-indigo-300 hover:bg-indigo-50/50 transition-all
                             group active:scale-[0.98]"
                >
                  <span className="text-lg mr-2">{q.icon}</span>
                  <span className="text-gray-700 group-hover:text-indigo-700">{q.text}</span>
                </button>
              ))}
            </div>
          </div>
        )}

        {sending && (
          <div className="flex justify-start">
            <div className="bg-white border border-gray-200 rounded-2xl px-5 py-3 shadow-sm">
              <div className="flex items-center gap-2 text-sm text-gray-400">
                <span className="animate-bounce">🤔</span>
                <span className="animate-pulse">Analyzing your finances...</span>
              </div>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form
        onSubmit={handleSubmit}
        className="bg-white border-t border-gray-200 px-4 sm:px-6 py-4"
      >
        <div className="flex gap-3 max-w-3xl mx-auto">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Type your question here... (e.g. 'Am I saving enough?')"
            className="flex-1 px-4 py-3 border border-gray-200 rounded-xl text-sm
                       focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none
                       bg-gray-50/50 transition-all"
            disabled={sending}
          />
          <button
            type="submit"
            disabled={sending || !input.trim()}
            className="px-6 py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white
                       text-sm font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700
                       disabled:opacity-50 transition-all shadow-md active:scale-[0.98]
                       flex items-center gap-2"
          >
            <span>Ask</span>
            <span>→</span>
          </button>
        </div>
      </form>
    </div>
  );
}
