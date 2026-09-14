import React, { useMemo } from 'react';
import { Check, X, ShieldAlert, ShieldCheck, Shield } from 'lucide-react';

interface PasswordStrengthIndicatorProps {
  password: string;
  lang?: 'ar' | 'en';
  showDetails?: boolean;
}

export interface PasswordAnalysis {
  score: number; // 0 to 4
  level: 'empty' | 'very-weak' | 'weak' | 'fair' | 'strong' | 'very-strong';
  label: string;
  colorClass: string;
  barColor: string;
  checks: {
    length: boolean;
    mixedCase: boolean;
    number: boolean;
    symbol: boolean;
  };
  tip: string;
}

export function evaluatePassword(password: string, isAr: boolean): PasswordAnalysis {
  if (!password) {
    return {
      score: 0,
      level: 'empty',
      label: isAr ? 'أدخل كلمة المرور' : 'Enter password',
      colorClass: 'text-gray-400',
      barColor: 'bg-gray-600',
      checks: {
        length: false,
        mixedCase: false,
        number: false,
        symbol: false,
      },
      tip: isAr
        ? 'استخدم 8 أحرف على الأقل مع مزيج من الحروف والأرقام والرموز'
        : 'Use at least 8 characters with a mix of letters, numbers & symbols',
    };
  }

  const checks = {
    length: password.length >= 8,
    mixedCase: /[a-z]/.test(password) && /[A-Z]/.test(password),
    number: /\d/.test(password),
    symbol: /[^A-Za-z0-9]/.test(password),
  };

  // Detect common weak patterns
  const isCommonPattern = /^(password|123456|12345678|qwerty|admin|telegram|111111|123123)$/i.test(password);

  let rawScore = 0;
  if (checks.length) rawScore++;
  if (password.length >= 12) rawScore += 0.5; // Bonus for good length
  if (checks.mixedCase) rawScore++;
  if (checks.number) rawScore++;
  if (checks.symbol) rawScore++;

  if (isCommonPattern || password.length < 6) {
    rawScore = 0.5;
  }

  let level: PasswordAnalysis['level'] = 'very-weak';
  let label = isAr ? 'ضعيفة جداً' : 'Very Weak';
  let colorClass = 'text-red-400';
  let barColor = 'bg-red-500';
  let tip = isAr
    ? 'كلمة المرور سهلة التخمين، أضف رموزاً وأرقاماً وحروفاً كبيرة'
    : 'Easy to guess, add symbols, numbers, and capital letters';

  if (rawScore >= 4) {
    level = 'very-strong';
    label = isAr ? 'ممتازة وقوية جداً' : 'Very Strong';
    colorClass = 'text-emerald-400';
    barColor = 'bg-emerald-500';
    tip = isAr
      ? 'كلمة مرور محصنة وآمنة للغاية ضد هجمات القوة الغاشمة'
      : 'Highly resilient against brute-force attacks';
  } else if (rawScore >= 3) {
    level = 'strong';
    label = isAr ? 'قوية' : 'Strong';
    colorClass = 'text-green-400';
    barColor = 'bg-green-500';
    tip = isAr
      ? 'مستوى حماية ممتاز لحسابك في تليجرام'
      : 'Great level of protection for your account';
  } else if (rawScore >= 2) {
    level = 'fair';
    label = isAr ? 'متوسطة' : 'Fair';
    colorClass = 'text-amber-400';
    barColor = 'bg-amber-500';
    tip = isAr
      ? 'جيدة، ولكن يمكنك تقويتها بإضافة رموز خاصة أو زيادة الطول'
      : 'Good, but add special symbols or lengthen for higher safety';
  } else if (rawScore > 1) {
    level = 'weak';
    label = isAr ? 'ضعيفة' : 'Weak';
    colorClass = 'text-orange-400';
    barColor = 'bg-orange-500';
    tip = isAr
      ? 'تحتاج إلى تنويع أكبر بين الحروف والأرقام'
      : 'Needs more diversity in characters';
  }

  return {
    score: Math.min(4, Math.floor(rawScore)),
    level,
    label,
    colorClass,
    barColor,
    checks,
    tip,
  };
}

export const PasswordStrengthIndicator: React.FC<PasswordStrengthIndicatorProps> = ({
  password,
  lang = 'ar',
  showDetails = true,
}) => {
  const isAr = lang === 'ar';
  const analysis = useMemo(() => evaluatePassword(password, isAr), [password, isAr]);

  if (!password) {
    return null;
  }

  // 4 segments calculation
  const segments = [1, 2, 3, 4];

  return (
    <div className="mt-2 space-y-2 select-none animate-fadeIn transition-all duration-200">
      {/* Top row: Strength Label & Icon */}
      <div className="flex items-center justify-between text-xs">
        <div className="flex items-center gap-1.5 font-medium">
          {analysis.score >= 3 ? (
            <ShieldCheck className={`w-4 h-4 ${analysis.colorClass}`} />
          ) : analysis.score >= 2 ? (
            <Shield className={`w-4 h-4 ${analysis.colorClass}`} />
          ) : (
            <ShieldAlert className={`w-4 h-4 ${analysis.colorClass}`} />
          )}
          <span className="text-gray-400 text-[11px]">{isAr ? 'قوة كلمة المرور:' : 'Password Strength:'}</span>
          <span className={`font-semibold text-xs ${analysis.colorClass}`}>{analysis.label}</span>
        </div>
        <span className="text-[10px] font-mono text-gray-400">
          {password.length} {isAr ? 'حرف' : 'chars'}
        </span>
      </div>

      {/* 4 Segmented Progress Bars */}
      <div className="grid grid-cols-4 gap-1.5 h-1.5">
        {segments.map((segIndex) => {
          const isActive = analysis.score >= segIndex;
          return (
            <div
              key={segIndex}
              className={`h-full rounded-full transition-all duration-300 ${
                isActive ? analysis.barColor : 'bg-gray-700/60'
              }`}
            />
          );
        })}
      </div>

      {/* Requirement Criteria Chips (Real-time checks) */}
      {showDetails && (
        <div className="grid grid-cols-2 gap-1.5 pt-1 text-[10.5px]">
          <div
            className={`flex items-center gap-1.5 px-2 py-1 rounded-md transition-colors ${
              analysis.checks.length
                ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                : 'bg-[#1b2633] text-gray-400 border border-gray-700/40'
            }`}
          >
            {analysis.checks.length ? (
              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
            ) : (
              <X className="w-3 h-3 text-gray-500 shrink-0" />
            )}
            <span className="truncate">{isAr ? '8 أحرف أو أكثر' : '8+ characters'}</span>
          </div>

          <div
            className={`flex items-center gap-1.5 px-2 py-1 rounded-md transition-colors ${
              analysis.checks.mixedCase
                ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                : 'bg-[#1b2633] text-gray-400 border border-gray-700/40'
            }`}
          >
            {analysis.checks.mixedCase ? (
              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
            ) : (
              <X className="w-3 h-3 text-gray-500 shrink-0" />
            )}
            <span className="truncate">{isAr ? 'حروف كبيرة وصغيرة' : 'Upper & lower'}</span>
          </div>

          <div
            className={`flex items-center gap-1.5 px-2 py-1 rounded-md transition-colors ${
              analysis.checks.number
                ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                : 'bg-[#1b2633] text-gray-400 border border-gray-700/40'
            }`}
          >
            {analysis.checks.number ? (
              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
            ) : (
              <X className="w-3 h-3 text-gray-500 shrink-0" />
            )}
            <span className="truncate">{isAr ? 'أرقام (0-9)' : 'Numbers (0-9)'}</span>
          </div>

          <div
            className={`flex items-center gap-1.5 px-2 py-1 rounded-md transition-colors ${
              analysis.checks.symbol
                ? 'bg-emerald-500/10 text-emerald-300 border border-emerald-500/20'
                : 'bg-[#1b2633] text-gray-400 border border-gray-700/40'
            }`}
          >
            {analysis.checks.symbol ? (
              <Check className="w-3 h-3 text-emerald-400 shrink-0" />
            ) : (
              <X className="w-3 h-3 text-gray-500 shrink-0" />
            )}
            <span className="truncate">{isAr ? 'رموز خاصة (!@#$)' : 'Symbols (!@#$)'}</span>
          </div>
        </div>
      )}

      {/* Helpful context tip */}
      <p className="text-[10px] text-gray-400 leading-normal pt-0.5">{analysis.tip}</p>
    </div>
  );
};
