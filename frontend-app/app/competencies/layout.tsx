"use client";

import * as React from "react";
import {
	ThemeProvider as NextThemesProvider,
	ThemeProvider,
	useTheme,
} from "next-themes";

import {
	SidebarInset,
	SidebarProvider,
	SidebarTrigger,
} from "../../src/components/ui/sidebar";
import { AppSidebar } from "@/components/app-sidebar";

import { Button } from "../../src/components/ui/button";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";

import { Sun, Moon } from "lucide-react";
import { Metadata } from "next";

export default function CompetencyLayout({
	children,
}: Readonly<{
	children: React.ReactNode;
}>) {
	return <>{children}</>;
}
