import React from 'react';

interface LoadingSkeletonProps {
  count?: number;
}

export const LoadingSkeleton: React.FC<LoadingSkeletonProps> = ({ count = 6 }) => {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
      {Array.from({ length: count }).map((_, index) => (
        <div
          key={index}
          className="bg-[#111113] border border-[#27272a] rounded-xl p-6 animate-pulse"
        >
          <div className="h-6 bg-[#1a1a1d] rounded-md mb-4 w-3/4"></div>
          <div className="space-y-3">
            <div className="h-4 bg-[#1a1a1d] rounded w-full"></div>
            <div className="h-4 bg-[#1a1a1d] rounded w-5/6"></div>
            <div className="h-4 bg-[#1a1a1d] rounded w-4/6"></div>
          </div>
          <div className="flex gap-2 mt-4">
            <div className="h-6 bg-[#1a1a1d] rounded-full w-16"></div>
            <div className="h-6 bg-[#1a1a1d] rounded-full w-20"></div>
          </div>
        </div>
      ))}
    </div>
  );
};

export const LoadingSpinner: React.FC = () => {
  return (
    <div className="flex justify-center items-center py-12">
      <div className="relative">
        <div className="w-12 h-12 border-4 border-[#27272a] border-t-[#3b82f6] rounded-full animate-spin"></div>
        <div className="absolute inset-0 w-12 h-12 border-4 border-transparent border-t-[#3b82f6]/40 rounded-full animate-ping"></div>
      </div>
    </div>
  );
};