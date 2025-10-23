import React, { useState, useEffect, useCallback } from 'react';
import { CompetencyService } from '../services/competencyService';
import type { Competency } from '../types/competency';
import { Header } from '../components/Header';
import { CompetencyCard } from '../components/CompetencyCard';
import { LoadingSkeleton, LoadingSpinner } from '../components/LoadingComponents';
import { ErrorMessage, EmptyState } from '../components/ErrorComponents';

export const CompetenciesPage: React.FC = () => {
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [filteredCompetencies, setFilteredCompetencies] = useState<Competency[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSearching, setIsSearching] = useState(false);

  // Load all competencies on component mount
  useEffect(() => {
    loadCompetencies();
  }, []);

  // Filter competencies based on search query
  useEffect(() => {
    if (!searchQuery.trim()) {
      setFilteredCompetencies(competencies);
      setIsSearching(false);
    } else {
      setIsSearching(true);
      const query = searchQuery.toLowerCase();
      const filtered = competencies.filter(competency =>
        competency.title.toLowerCase().includes(query) ||
        competency.description.toLowerCase().includes(query) ||
        competency.category?.toLowerCase().includes(query) ||
        competency.tags?.some(tag => tag.toLowerCase().includes(query)) ||
        competency.standardCodes?.some(code => 
          code.code.toLowerCase().includes(query) || 
          code.systemName.toLowerCase().includes(query)
        )
      );
      setFilteredCompetencies(filtered);
      setIsSearching(false);
    }
  }, [competencies, searchQuery]);

  const loadCompetencies = async () => {
    try {
      setIsLoading(true);
      setError(null);
      const data = await CompetencyService.getAllCompetencies();
      setCompetencies(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load competencies');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSearch = useCallback((query: string) => {
    setSearchQuery(query);
  }, []);

  const handleCompetencyClick = (competency: Competency) => {
    console.log('Competency clicked:', competency);
    // TODO: Navigate to competency detail page or open modal
  };

  const handleRetry = () => {
    loadCompetencies();
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0a0a0b]">
        <Header onSearch={handleSearch} isLoading={true} />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <LoadingSkeleton count={9} />
        </main>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-[#0a0a0b]">
        <Header onSearch={handleSearch} />
        <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
          <ErrorMessage message={error} onRetry={handleRetry} />
        </main>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0a0a0b]">
      <Header 
        onSearch={handleSearch} 
        competencyCount={competencies.length}
        isLoading={isLoading}
      />
      
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Search Status */}
        {searchQuery && (
          <div className="mb-6 flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <span className="text-[#a1a1aa]">Search results for:</span>
              <span className="px-3 py-1 bg-[#3b82f6]/10 text-[#3b82f6] rounded-full text-sm border border-[#3b82f6]/20">
                "{searchQuery}"
              </span>
            </div>
            <span className="text-[#71717a] text-sm">
              {filteredCompetencies.length} {filteredCompetencies.length === 1 ? 'result' : 'results'}
            </span>
          </div>
        )}

        {/* Loading indicator for search */}
        {isSearching && <LoadingSpinner />}

        {/* Content */}
        {!isSearching && (
          <>
            {filteredCompetencies.length === 0 ? (
              <EmptyState />
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {filteredCompetencies.map((competency) => (
                  <CompetencyCard
                    key={competency.id}
                    competency={competency}
                    onClick={handleCompetencyClick}
                  />
                ))}
              </div>
            )}
          </>
        )}

        {/* Back to top button */}
        {filteredCompetencies.length > 6 && (
          <div className="flex justify-center mt-12">
            <button
              onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
              className="px-6 py-3 bg-[#111113] hover:bg-[#1a1a1d] border border-[#27272a] hover:border-[#3b82f6]/30 
                         text-[#a1a1aa] hover:text-[#f4f4f5] rounded-xl transition-all duration-200 
                         flex items-center space-x-2 backdrop-blur-sm"
            >
              <svg className="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
              </svg>
              <span>Back to top</span>
            </button>
          </div>
        )}
      </main>
    </div>
  );
};