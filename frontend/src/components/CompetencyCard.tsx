import React from 'react';
import type { Competency } from '../types/competency';

interface CompetencyCardProps {
  competency: Competency;
  onClick?: (competency: Competency) => void;
}

const getLevelColor = (level?: string) => {
  switch (level) {
    case 'BEGINNER':
      return 'bg-[#1a1a1d] text-[#71717a] border-[#27272a]';
    case 'INTERMEDIATE':
      return 'bg-[#232326] text-[#a1a1aa] border-[#3b82f6]/20';
    case 'ADVANCED':
      return 'bg-[#3b82f6]/10 text-[#3b82f6] border-[#3b82f6]/30';
    case 'EXPERT':
      return 'bg-[#2563eb]/10 text-[#2563eb] border-[#2563eb]/30';
    default:
      return 'bg-[#1a1a1d] text-[#71717a] border-[#27272a]';
  }
};

export const CompetencyCard: React.FC<CompetencyCardProps> = ({ competency, onClick }) => {
  return (
    <div
      className="group bg-[#111113] backdrop-blur-sm border border-[#27272a] rounded-xl p-6 
                 hover:bg-[#232326] hover:border-[#3b82f6]/30 hover:shadow-xl hover:shadow-[#3b82f6]/5
                 transition-all duration-300 cursor-pointer transform hover:-translate-y-1"
      onClick={() => onClick?.(competency)}
    >
      {/* Header */}
      <div className="flex items-start justify-between mb-4">
        <h3 className="text-lg font-semibold text-[#f4f4f5] group-hover:text-[#3b82f6] transition-colors duration-200 line-clamp-2">
          {competency.title}
        </h3>
        {competency.level && (
          <span className={`px-2 py-1 text-xs font-medium rounded-full border ${getLevelColor(competency.level)} ml-2 shrink-0`}>
            {competency.level}
          </span>
        )}
      </div>

      {/* Description */}
      <p className="text-[#a1a1aa] text-sm leading-relaxed mb-4 line-clamp-3">
        {competency.description}
      </p>

      {/* Tags */}
      {competency.tags && competency.tags.length > 0 && (
        <div className="flex flex-wrap gap-2 mb-4">
          {competency.tags.slice(0, 3).map((tag, index) => (
            <span
              key={index}
              className="px-2 py-1 bg-[#1a1a1d] text-[#a1a1aa] text-xs rounded-full border border-[#27272a]"
            >
              {tag}
            </span>
          ))}
          {competency.tags.length > 3 && (
            <span className="px-2 py-1 bg-[#1a1a1d] text-[#71717a] text-xs rounded-full border border-[#27272a]">
              +{competency.tags.length - 3} more
            </span>
          )}
        </div>
      )}

      {/* Progress bar (if available) */}
      {competency.progress !== undefined && (
        <div className="mb-4">
          <div className="flex justify-between items-center mb-1">
            <span className="text-xs text-[#71717a]">Progress</span>
            <span className="text-xs text-[#a1a1aa]">{competency.progress}%</span>
          </div>
          <div className="w-full bg-[#1a1a1d] rounded-full h-1.5 border border-[#27272a]">
            <div
              className="bg-linear-to-r from-[#3b82f6] to-[#2563eb] h-1.5 rounded-full transition-all duration-500"
              style={{ width: `${competency.progress}%` }}
            />
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="flex items-center justify-between pt-4 border-t border-[#27272a]">
        <div className="flex items-center space-x-4">
          {competency.category && (
            <span className="text-xs text-[#71717a]">
              📁 {competency.category}
            </span>
          )}
          {competency.skillCount !== undefined && (
            <span className="text-xs text-[#71717a]">
              🎯 {competency.skillCount} skills
            </span>
          )}
        </div>
        
        <div className="flex items-center space-x-2">
          {competency.standardCodes && competency.standardCodes.length > 0 && (
            <span className="text-xs text-[#3b82f6] bg-[#3b82f6]/10 px-2 py-1 rounded border border-[#3b82f6]/20">
              📋 {competency.standardCodes.length} standards
            </span>
          )}
          
          <svg 
            className="w-4 h-4 text-[#71717a] group-hover:text-[#3b82f6] transition-colors duration-200" 
            fill="none" 
            viewBox="0 0 24 24" 
            stroke="currentColor"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
          </svg>
        </div>
      </div>
    </div>
  );
};