import { useState, type FormEvent } from 'react';
import { useAuth } from '@/context/AuthContext';
import { useNavigate } from 'react-router-dom';
import ThemeToggle from '@/components/ThemeToggle';

export default function LoginPage() {
  const { login, loading, error } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    try {
      await login(username, password);
      navigate('/');
    } catch {
      // error is handled by AuthContext
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50
                    dark:from-gray-900 dark:via-gray-950 dark:to-indigo-950
                    flex items-center justify-center px-4 transition-colors duration-300">
      {/* Theme toggle — top right */}
      <div className="fixed top-4 right-4 z-50">
        <ThemeToggle size="sm" />
      </div>
      <div className="w-full max-w-md">
        {/* Welcome header */}
        <div className="text-center mb-8">
          <div className="w-20 h-20 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-2xl mx-auto
                          flex items-center justify-center text-4xl shadow-lg mb-5">
            💰
          </div>
          <h1 className="text-3xl font-bold text-gray-800 dark:text-gray-100">Welcome to Money Guide</h1>
          <p className="text-gray-500 dark:text-gray-400 mt-2 text-base leading-relaxed">
            Your personal assistant to understand,<br />manage, and grow your money 🌱
          </p>
        </div>

        {/* Login card */}
        <form
          onSubmit={handleSubmit}
          className="bg-white dark:bg-gray-800/80 rounded-2xl shadow-xl border border-gray-100
                     dark:border-gray-700 p-8 space-y-5 backdrop-blur"
        >
          <div className="text-center mb-2">
            <p className="text-sm text-gray-500 dark:text-gray-400">Sign in to see your financial dashboard</p>
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              👤 Username
            </label>
            <input
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl
                         focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none
                         text-sm bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all"
              placeholder="Type your username"
            />
          </div>

          <div>
            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1.5">
              🔒 Password
            </label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              className="w-full px-4 py-3 border border-gray-200 dark:border-gray-600 rounded-xl
                         focus:ring-2 focus:ring-indigo-400 focus:border-indigo-400 outline-none
                         text-sm bg-gray-50/50 dark:bg-gray-700/50 dark:text-gray-100 transition-all"
              placeholder="Type your password"
            />
          </div>

          {error && (
            <div className="flex items-start gap-2 text-sm text-red-600 dark:text-red-400 bg-red-50
                           dark:bg-red-950/40 px-4 py-3 rounded-xl">
              <span className="mt-0.5">❌</span>
              <span>{error}</span>
            </div>
          )}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-gradient-to-r from-indigo-600 to-purple-600 text-white
                       font-semibold rounded-xl hover:from-indigo-700 hover:to-purple-700
                       disabled:opacity-50 transition-all text-sm shadow-md hover:shadow-lg
                       active:scale-[0.98]"
          >
            {loading ? (
              <span className="flex items-center justify-center gap-2">
                <span className="animate-spin">⏳</span> Signing in...
              </span>
            ) : (
              'Sign In →'
            )}
          </button>
        </form>

        {/* Features preview */}
        <div className="mt-8 grid grid-cols-3 gap-3 text-center">
          {[
            { icon: '📊', label: 'Track Money' },
            { icon: '🎯', label: 'Set Goals' },
            { icon: '💬', label: 'Get Advice' },
          ].map((f) => (
            <div key={f.label} className="bg-white/60 dark:bg-gray-800/60 backdrop-blur rounded-xl p-3
                                        border border-gray-100 dark:border-gray-700">
              <span className="text-2xl">{f.icon}</span>
              <p className="text-[11px] text-gray-500 dark:text-gray-400 mt-1 font-medium">{f.label}</p>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
