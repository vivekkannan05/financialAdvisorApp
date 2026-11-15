interface ProgressRingProps {
  value: number;
  max: number;
  size?: number;
  strokeWidth?: number;
  color?: string;
  label?: string;
}

export default function ProgressRing({
  value,
  max,
  size = 80,
  strokeWidth = 8,
  color = '#6366f1',
  label,
}: ProgressRingProps) {
  const radius = (size - strokeWidth) / 2;
  const circumference = 2 * Math.PI * radius;
  const pct = Math.min(Math.max(value / max, 0), 1);
  const offset = circumference * (1 - pct);

  const displayPct = Math.round(pct * 100);
  let emoji = '🔴';
  if (displayPct >= 75) emoji = '🟢';
  else if (displayPct >= 50) emoji = '🟡';
  else if (displayPct >= 25) emoji = '🟠';

  return (
    <div className="flex flex-col items-center gap-1">
      <div className="relative" style={{ width: size, height: size }}>
        <svg width={size} height={size} className="-rotate-90">
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke="#e5e7eb"
            strokeWidth={strokeWidth}
          />
          <circle
            cx={size / 2}
            cy={size / 2}
            r={radius}
            fill="none"
            stroke={color}
            strokeWidth={strokeWidth}
            strokeDasharray={circumference}
            strokeDashoffset={offset}
            strokeLinecap="round"
            className="transition-all duration-700 ease-out"
          />
        </svg>
        <div className="absolute inset-0 flex items-center justify-center">
          <span className="text-sm font-bold text-gray-700">{emoji}</span>
        </div>
      </div>
      {label && <span className="text-xs text-gray-500 text-center leading-tight">{label}</span>}
      <span className="text-xs font-semibold text-gray-700">{displayPct}%</span>
    </div>
  );
}
