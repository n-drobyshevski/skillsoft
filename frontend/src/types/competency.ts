// Types for competency data
export interface Competency {
  id: string;
  title: string;
  description: string;
  tags?: string[];
  level?: 'BEGINNER' | 'INTERMEDIATE' | 'ADVANCED' | 'EXPERT';
  category?: string;
  skillCount?: number;
  progress?: number;
  isActive?: boolean;
  standardCodes?: {
    code: string;
    systemName: string;
  }[];
}

export interface ApiResponse<T> {
  data: T;
  message?: string;
  error?: string;
}

export interface ErrorState {
  hasError: boolean;
  message: string;
}

export interface LoadingState {
  isLoading: boolean;
  message?: string;
}