import React, { useState, useEffect } from 'react';

interface SearchBarProps {
  onSearch: (query: string) => void;
  placeholder?: string;
  initialValue?: string;
  debounceMs?: number;
}

export const SearchBar: React.FC<SearchBarProps> = ({ 
  onSearch, 
  placeholder = "Search competencies...", 
  initialValue = "",
  debounceMs = 300 
}) => {
  const [query, setQuery] = useState(initialValue);
  const [isFocused, setIsFocused] = useState(false);

  // Debounced search effect
  useEffect(() => {
    const timer = setTimeout(() => {
      onSearch(query);
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [query, onSearch, debounceMs]);

  const handleClear = () => {
    setQuery('');
  };

  return (
    <div className="relative max-w-md mx-auto">
      <div className={`relative flex items-center transition-all duration-200 ${
        isFocused ? 'transform scale-105' : ''
      }`}>
        {/* Search Icon */}
        <div className="absolute left-3 top-1/2 transform -translate-y-1/2 z-10">
          <svg
            className={`w-5 h-5 transition-colors duration-200 ${
              isFocused ? 'text-[#3b82f6]' : 'text-[#71717a]'
            }`}
            fill="none"
            viewBox="0 0 24 24"
            stroke="currentColor"
          >
            <path
              strokeLinecap="round"
              strokeLinejoin="round"
              strokeWidth={2}
              d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
            />
          </svg>
        </div>

        {/* Input Field */}
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          placeholder={placeholder}
          className={`w-full pl-12 pr-12 py-3 bg-[#111113] backdrop-blur-sm border border-[#27272a] 
                     rounded-xl text-[#f4f4f5] placeholder-[#71717a] 
                     focus:outline-none focus:ring-2 focus:ring-[#3b82f6]/50 focus:border-[#3b82f6]
                     transition-all duration-200 hover:bg-[#1a1a1d] ${
                       isFocused ? 'bg-[#1a1a1d] border-[#3b82f6] shadow-lg shadow-[#3b82f6]/10' : ''
                     }`}
        />

        {/* Clear Button */}
        {query && (
          <button
            onClick={handleClear}
            className="absolute right-3 top-1/2 transform -translate-y-1/2 p-1 hover:bg-[#232326] rounded-full transition-colors duration-200 z-10"
            aria-label="Clear search"
          >
            <svg
              className="w-4 h-4 text-[#71717a] hover:text-[#a1a1aa]"
              fill="none"
              viewBox="0 0 24 24"
              stroke="currentColor"
            >
              <path
                strokeLinecap="round"
                strokeLinejoin="round"
                strokeWidth={2}
                d="M6 18L18 6M6 6l12 12"
              />
            </svg>
          </button>
        )}
      </div>

      {/* Search hint */}
      {isFocused && !query && (
        <div className="absolute top-full left-0 right-0 mt-2 p-3 bg-[#111113]/90 backdrop-blur-sm border border-[#27272a] rounded-lg text-sm text-[#71717a]">
          💡 Try searching by title, description, or tags
        </div>
      )}
    </div>
  );
};