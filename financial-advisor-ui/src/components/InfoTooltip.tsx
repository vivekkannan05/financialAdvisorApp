import { useState, useRef, useEffect } from 'react';

interface InfoTooltipProps {
  text: string;
  children?: React.ReactNode;
}

export default function InfoTooltip({ text, children }: InfoTooltipProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    if (open) document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open]);

  return (
    <div className="relative inline-flex items-center" ref={ref}>
      {children}
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="ml-1.5 w-5 h-5 rounded-full bg-blue-100 dark:bg-blue-900/60 text-blue-600
                   dark:text-blue-400 text-xs font-bold flex items-center justify-center
                   hover:bg-blue-200 dark:hover:bg-blue-800/60 transition-colors
                   focus:outline-none focus:ring-2 focus:ring-blue-400 shrink-0"
        aria-label="More info"
      >
        ?
      </button>
      {open && (
        <div className="info-tooltip-popup absolute z-50 bottom-full left-1/2 -translate-x-1/2 mb-2
                        w-64 bg-gray-900 dark:bg-gray-100 text-white dark:text-gray-900
                        text-xs leading-relaxed rounded-xl px-4 py-3 shadow-xl animate-fade-in">
          {text}
          <div className="absolute top-full left-1/2 -translate-x-1/2 -mt-1
                          border-[6px] border-transparent border-t-gray-900 dark:border-t-gray-100" />
        </div>
      )}
    </div>
  );
}
