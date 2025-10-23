import React from 'react';

interface ErrorMessageProps {
  message: string;
  onRetry?: () => void;
}

export const ErrorMessage: React.FC<ErrorMessageProps> = ({ message, onRetry }) => {
  return (
    <div className="flex flex-col items-center justify-center py-12 px-6">
      <div className="bg-[#111113] border border-[#27272a] rounded-xl p-8 max-w-md text-center">
        <div className="w-16 h-16 mx-auto mb-4 bg-[#1a1a1d] rounded-full flex items-center justify-center">
          <svg className="w-8 h-8 text-[#71717a]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        </div>
        <h3 className="text-lg font-semibold text-[#f4f4f5] mb-2">Something went wrong</h3>
        <p className="text-[#a1a1aa] mb-6">{message}</p>
        {onRetry && (
          <button
            onClick={onRetry}
            className="px-6 py-2 bg-[#3b82f6] hover:bg-[#2563eb] text-white font-medium rounded-lg transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-[#3b82f6] focus:ring-offset-2 focus:ring-offset-[#0a0a0b]"
          >
            Try Again
          </button>
        )}
      </div>
    </div>
  );
};

export const EmptyState: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center py-16 px-6">
      <div className="w-24 h-24 mx-auto mb-6 bg-[#1a1a1d] rounded-full flex items-center justify-center">
        <svg className="w-12 h-12 text-[#71717a]" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M9.75 17L9 20l-1 1h8l-1-1-.75-3M3 13h18M5 17h14a2 2 0 002-2V5a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
        </svg>
      </div>
      <h3 className="text-xl font-semibold text-[#f4f4f5] mb-2">No competencies found</h3>
      <p className="text-[#a1a1aa] text-center max-w-md">
        It looks like there are no competencies available at the moment. Check back later or contact your administrator.
      </p>
    </div>
  );
};