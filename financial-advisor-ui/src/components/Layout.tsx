import { NavLink, Outlet } from 'react-router-dom';
import { useAuth } from '@/context/AuthContext';
import { useState } from 'react';

const navItems = [
  {
    to: '/',
    icon: '🏠',
    label: 'My Dashboard',
    hint: 'See how healthy your money situation is at a glance',
  },
  {
    to: '/chat',
    icon: '💬',
    label: 'Ask Advisor',
    hint: 'Ask questions about your money in plain English',
  },
  {
    to: '/profile',
    icon: '📝',
    label: 'My Money Info',
    hint: 'Enter your income, expenses, savings & goals',
  },
  {
    to: '/stress-test',
    icon: '🔮',
    label: 'What If?',
    hint: 'See what happens if things go wrong',
  },
];

export default function Layout() {
  const { logout } = useAuth();
  const [sidebarOpen, setSidebarOpen] = useState(false);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-blue-50/30 flex">
      {/* Mobile hamburger */}
      <button
        onClick={() => setSidebarOpen(!sidebarOpen)}
        className="fixed top-4 left-4 z-50 md:hidden w-10 h-10 bg-white shadow-lg rounded-xl
                   flex items-center justify-center text-lg border border-gray-200"
      >
        {sidebarOpen ? '✕' : '☰'}
      </button>

      {/* Overlay */}
      {sidebarOpen && (
        <div
          className="fixed inset-0 bg-black/20 z-30 md:hidden"
          onClick={() => setSidebarOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={`fixed md:static z-40 w-72 bg-white border-r border-gray-200/80 flex flex-col
                    h-screen transition-transform duration-300 ease-in-out
                    ${sidebarOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'}`}
      >
        <div className="px-6 py-5 border-b border-gray-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-indigo-500 to-purple-600 rounded-xl
                            flex items-center justify-center text-white text-lg shadow-md">
              💰
            </div>
            <div>
              <h1 className="text-lg font-bold text-gray-800">Money Guide</h1>
              <p className="text-[11px] text-gray-400">Your Personal Financial Advisor</p>
            </div>
          </div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
          {navItems.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.to === '/'}
              onClick={() => setSidebarOpen(false)}
              className={({ isActive }) =>
                `group block px-4 py-3 rounded-xl text-sm transition-all duration-200 ${
                  isActive
                    ? 'bg-indigo-50 border border-indigo-200/60 shadow-sm'
                    : 'hover:bg-gray-50 border border-transparent'
                }`
              }
            >
              {({ isActive }) => (
                <div className="flex items-start gap-3">
                  <span className="text-xl mt-0.5">{item.icon}</span>
                  <div>
                    <span
                      className={`font-semibold block ${
                        isActive ? 'text-indigo-700' : 'text-gray-700 group-hover:text-gray-900'
                      }`}
                    >
                      {item.label}
                    </span>
                    <span className="text-[11px] text-gray-400 leading-tight block mt-0.5">
                      {item.hint}
                    </span>
                  </div>
                </div>
              )}
            </NavLink>
          ))}
        </nav>

        {/* Help box */}
        <div className="mx-3 mb-3 p-4 bg-gradient-to-br from-blue-50 to-indigo-50 rounded-xl border border-blue-100">
          <p className="text-xs font-semibold text-blue-800">💡 Need Help?</p>
          <p className="text-[11px] text-blue-600 mt-1 leading-relaxed">
            Look for the <span className="inline-flex items-center justify-center w-4 h-4 rounded-full bg-blue-200 text-blue-700 text-[9px] font-bold mx-0.5">?</span> button
            next to anything confusing. It'll explain in simple words!
          </p>
        </div>

        <div className="px-3 pb-4 border-t border-gray-100 pt-3">
          <button
            onClick={logout}
            className="w-full px-4 py-2.5 text-sm text-gray-500 hover:text-red-600
                       hover:bg-red-50 rounded-xl transition-all duration-200 flex items-center gap-2"
          >
            <span>👋</span> Sign Out
          </button>
        </div>
      </aside>

      {/* Main content */}
      <main className="flex-1 overflow-auto md:ml-0">
        <Outlet />
      </main>
    </div>
  );
}
