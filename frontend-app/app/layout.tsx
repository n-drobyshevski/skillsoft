import "./globals.css";

import * as React from "react";
import { ThemeProvider } from "next-themes";
import { Metadata } from "next";

import { LayoutProvider } from "@/components/layout-provider";

export const metadata: Metadata = {
  title: "SkillSoft - Competency Management",
  description: "A platform for managing skills and competencies.",
};
export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head />
      <body>
        <LayoutProvider>{children}</LayoutProvider>
      </body>
    </html>
  );
}
