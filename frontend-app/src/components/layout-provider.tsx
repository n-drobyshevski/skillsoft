"use client";

import { SidebarInset, SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";
import { ThemeProvider } from "next-themes";

import { HeaderProvider } from "../context/HeaderContext";
import Header from "../../app/components/Header";

export function LayoutProvider({ children }: { children: React.ReactNode }) {
  return (
    <ThemeProvider
      attribute="class"
      defaultTheme="system"
      enableSystem
      disableTransitionOnChange
    >
      <SidebarProvider defaultOpen={false}>
        <AppSidebar />
        <SidebarInset>
          <HeaderProvider>
            <Header />
            <div className="flex flex-1 flex-col gap-4 pt-0">{children}</div>
          </HeaderProvider>
        </SidebarInset>
      </SidebarProvider>
    </ThemeProvider>
  );
}
