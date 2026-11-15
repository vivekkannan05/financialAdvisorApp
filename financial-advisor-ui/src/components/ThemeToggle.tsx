import { useTheme } from '@/context/ThemeContext';

interface ThemeToggleProps {
  size?: 'sm' | 'md';
}

export default function ThemeToggle({ size = 'md' }: ThemeToggleProps) {
  const { theme, toggle } = useTheme();

  const sizes = {
    sm: { track: 'w-12 h-6', knob: 'w-5 h-5', translate: 'translate-x-6', icon: 'text-xs' },
    md: { track: 'w-14 h-7', knob: 'w-6 h-6', translate: 'translate-x-7', icon: 'text-sm' },
  };
  const s = sizes[size];

  return (
    <button
      onClick={toggle}
      className={`theme-toggle relative ${s.track} rounded-full transition-colors duration-300 focus:outline-none
                  focus:ring-2 focus:ring-indigo-400 focus:ring-offset-2 dark:focus:ring-offset-gray-800
                  ${theme === 'dark'
                    ? 'bg-indigo-600'
                    : 'bg-gray-300'
                  }`}
      aria-label={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
      title={`Switch to ${theme === 'dark' ? 'light' : 'dark'} mode`}
    >
      <span
        className={`absolute top-0.5 left-0.5 ${s.knob} rounded-full bg-white shadow-md
                    flex items-center justify-center transition-transform duration-300
                    ${theme === 'dark' ? s.translate : 'translate-x-0'}`}
      >
        <span className={s.icon}>{theme === 'dark' ? '🌙' : '☀️'}</span>
      </span>
    </button>
  );
}
