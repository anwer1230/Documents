import React, { useState } from 'react';
import { ShieldCheck, ExternalLink, ChevronDown, ChevronUp, AlertCircle } from 'lucide-react';
import { MessageFactCheck } from '../../types';

interface FactCheckCardProps {
  factCheck: MessageFactCheck;
  isArabic?: boolean;
}

export const FactCheckCard: React.FC<FactCheckCardProps> = ({ factCheck, isArabic = false }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  return (
    <div className="mt-2 bg-[#0e1621]/90 border border-emerald-500/40 rounded-xl p-2.5 text-xs text-white shadow-sm space-y-1.5 backdrop-blur-xs">
      <div className="flex items-center justify-between gap-2">
        <div className="flex items-center gap-1.5">
          <ShieldCheck className="w-4 h-4 text-emerald-400 shrink-0" />
          <span className="font-bold text-[11px] text-emerald-400 uppercase tracking-wide">
            {factCheck.organization}
          </span>
        </div>

        <button
          onClick={() => setIsExpanded(!isExpanded)}
          className="text-gray-400 hover:text-white p-0.5"
        >
          {isExpanded ? <ChevronUp className="w-3.5 h-3.5" /> : <ChevronDown className="w-3.5 h-3.5" />}
        </button>
      </div>

      <p className="text-gray-200 text-[11px] leading-relaxed">
        {factCheck.text}
      </p>

      {isExpanded && factCheck.sourceUrl && (
        <div className="pt-1 border-t border-white/10 flex items-center justify-between text-[10px]">
          <span className="text-gray-400 font-mono">Date: {factCheck.checkedAt}</span>
          <a
            href={factCheck.sourceUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="text-[#5288c1] hover:underline flex items-center gap-1 font-semibold"
          >
            <span>{isArabic ? 'المصدر المعتمد' : 'Verified Source'}</span>
            <ExternalLink className="w-3 h-3" />
          </a>
        </div>
      )}
    </div>
  );
};
