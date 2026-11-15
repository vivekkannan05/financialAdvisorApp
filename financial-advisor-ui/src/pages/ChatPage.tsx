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

export default function ChatPage() {
  const [messages, setMessages] = useState<Message[]>([
    {
      role: 'assistant',
      content:
        'Hello! I\'m your personal financial adviser. Ask me anything about your finances — ' +
        'goals, investments, risk assessment, or "what if" scenarios. I\'ll use your profile data ' +
        'to give you evidence-based recommendations.',
      source: 'ai',
    },
  ]);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  async function handleSend(e: FormEvent) {
    e.preventDefault();
    const text = input.trim();
    if (!text || sending) return;

    setInput('');
    setMessages((prev) => [...prev, { role: 'user', content: text }]);
    setSending(true);

    try {
      const response: ChatResponse = await sendMessage(text);
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
        { role: 'assistant', content: `⚠️ Error: ${msg}`, source: 'error' },
      ]);
    } finally {
      setSending(false);
    }
  }

  return (
    <div className="flex flex-col h-screen">
      {/* Header */}
      <div className="bg-white border-b border-gray-200 px-6 py-4">
        <h2 className="text-lg font-semibold text-gray-900">Chat with Your Adviser</h2>
        <p className="text-xs text-gray-400">
          AI-powered advice backed by deterministic financial calculations
        </p>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-6 py-4 space-y-4">
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
        {sending && (
          <div className="flex justify-start">
            <div className="bg-white border border-gray-200 rounded-2xl px-4 py-3 text-sm text-gray-400 animate-pulse">
              Thinking...
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <form
        onSubmit={handleSend}
        className="bg-white border-t border-gray-200 px-6 py-4 flex gap-3"
      >
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="Ask about your finances..."
          className="flex-1 px-4 py-2.5 border border-gray-300 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
          disabled={sending}
        />
        <button
          type="submit"
          disabled={sending || !input.trim()}
          className="px-5 py-2.5 bg-indigo-600 text-white text-sm font-medium rounded-xl hover:bg-indigo-700 disabled:opacity-50 transition-colors"
        >
          Send
        </button>
      </form>
    </div>
  );
}
