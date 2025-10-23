import React from 'react';
import { SearchBar } from './SearchBar';

interface HeaderProps {
  onSearch: (query: string) => void;
  competencyCount?: number;
  isLoading?: boolean;
}

export const Header: React.FC<HeaderProps> = ({ onSearch, competencyCount, isLoading }) => {
  return (
    <header className="bg-[#111113]/80 backdrop-blur-md border-b border-[#27272a] sticky top-0 z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex flex-col space-y-6 py-8">
          {/* Title Section */}
          <div className="text-center">
            <h1 className="text-4xl font-bold text-[#f4f4f5] mb-2">
              <span className="bg-linear-to-r from-[#3b82f6] to-[#2563eb] bg-clip-text text-transparent">
                Professional Competencies
              </span>
            </h1>
            <p className="text-lg text-[#a1a1aa] max-w-2xl mx-auto">
              Discover and explore our comprehensive library of professional competencies
            </p>
            
            {/* Stats */}
            <div className="flex justify-center items-center space-x-6 mt-4">
              {competencyCount !== undefined && !isLoading && (
                <div className="flex items-center space-x-2 text-[#a1a1aa]">
                  <svg className="w-5 h-5 text-[#3b82f6]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12l2 2 4-4M7.835 4.697a3.42 3.42 0 001.946-.806 3.42 3.42 0 014.438 0 3.42 3.42 0 001.946.806 3.42 3.42 0 013.138 3.138 3.42 3.42 0 00.806 1.946 3.42 3.42 0 010 4.438 3.42 3.42 0 00-.806 1.946 3.42 3.42 0 01-3.138 3.138 3.42 3.42 0 00-1.946.806 3.42 3.42 0 01-4.438 0 3.42 3.42 0 00-1.946-.806 3.42 3.42 0 01-3.138-3.138 3.42 3.42 0 00-.806-1.946 3.42 3.42 0 010-4.438 3.42 3.42 0 00.806-1.946 3.42 3.42 0 013.138-3.138z" />
                  </svg>
                  <span className="text-sm font-medium">
                    {competencyCount} {competencyCount === 1 ? 'competency' : 'competencies'} available
                  </span>
                </div>
              )}
              
              <div className="flex items-center space-x-2 text-[#a1a1aa]">
                <svg className="w-5 h-5 text-[#3b82f6]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                </svg>
                <span className="text-sm font-medium">Real-time updates</span>
              </div>
            </div>
          </div>

          {/* Search Section */}
          <div className="flex justify-center">
            <SearchBar onSearch={onSearch} />
          </div>
        </div>
      </div>
    </header>
  );
};