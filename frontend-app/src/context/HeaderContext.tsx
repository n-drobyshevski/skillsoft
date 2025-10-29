'use client';

import { createContext, useContext, useState, ReactNode } from 'react';

interface HeaderContextProps {
  title: string;
  subtitle: string;
  entityName: string;
  setTitle: (title: string) => void;
  setSubtitle: (subtitle: string) => void;
  setEntityName: (entityName: string) => void;
}

const HeaderContext = createContext<HeaderContextProps | undefined>(undefined);

export const HeaderProvider = ({ children }: { children: ReactNode }) => {
  const [title, setTitle] = useState('');
  const [subtitle, setSubtitle] = useState('');
  const [entityName, setEntityName] = useState('');

  return (
    <HeaderContext.Provider value={{ title, subtitle, entityName, setTitle, setSubtitle, setEntityName }}>
      {children}
    </HeaderContext.Provider>
  );
};

export const useHeader = () => {
  const context = useContext(HeaderContext);
  if (!context) {
    throw new Error('useHeader must be used within a HeaderProvider');
  }
  return context;
};
