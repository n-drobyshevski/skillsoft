"use client";
import React, { useState, useEffect } from "react";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Activity, ArrowUpDown, MoreHorizontal } from "lucide-react";
import { competenciesApi } from "@/services/api";
import { Competency, DashboardStats } from "./interfaces/domain-interfaces";
import Header from "./components/Header";
import DashboardStatsCards from "./components/DasboardStatsCards";
import ErrorCard from "./components/ErrorCard";
import CompetencyByCategoryBarChart from "@/components/charts/CompetencyByCategoryBarChart";
import CompetencyByLevelBarChart from "@/components/charts/CompetencyByLevelBarChart";
import AverageIndicatorsGauge from "@/components/charts/AverageIndicatorsGauge";
import RecentActivityCard from "@/components/RecentActivityCard";
import QuickActionsCard from "@/components/QuickActionsCard";
import EntityTable from "./components/Table";
import { ColumnDef } from "@tanstack/react-table";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import TopCompetenciesCard from "@/components/TopCompetenciesCard";

// Main Dashboard Component
export default function Page() {
	const [competencies, setCompetencies] = useState<Competency[]>([]);
	const [stats, setStats] = useState<DashboardStats | null>(null);
	const [loading, setLoading] = useState<boolean>(true);
	const [error, setError] = useState<string | null>(null);

	// Fetch competencies and stats
	const fetchData = async () => {
		try {
			setLoading(true);
			const competenciesData: Array<Competency> | null = await competenciesApi.getAllCompetencies();
			
			if (!Array.isArray(competenciesData)) {
				throw new Error('Invalid response from server: data is not an array');
			}
			
			setCompetencies(competenciesData);

			// Calculate stats
			const stats: DashboardStats = {
				totalCompetencies: competenciesData.length,
				totalBehavioralIndicators: competenciesData.reduce(
					(sum: number, comp: Competency) => sum + (comp.behavioralIndicators?.length || 0),
					0,
				),
				totalAssessmentQuestions: competenciesData.length * 8, // Estimate
				competenciesByCategory: {},
				competenciesByLevel: {},
				averageIndicatorsPerCompetency: 0,
			};

			// Calculate distributions
			competenciesData.forEach((comp: Competency) => {
				stats.competenciesByCategory[comp.category] =
					(stats.competenciesByCategory[comp.category] || 0) + 1;
				stats.competenciesByLevel[comp.level] =
					(stats.competenciesByLevel[comp.level] || 0) + 1;
			});

			stats.averageIndicatorsPerCompetency =
				stats.totalBehavioralIndicators / stats.totalCompetencies;
			setStats(stats);
		} catch (error) {
			console.error("Failed to fetch data:", error);
			setError("Failed to load competencies. Please try again.");
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchData();
	}, []);

	const competencyColumns: ColumnDef<Competency>[] = [
		{
			accessorKey: "name",
			header: ({ column }) => (
				<Button
					variant="link"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
					className="-ml-4 text-muted-foreground"
				>
					Name
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => (
				<Link href={`/competencies/${row.original.id}`} className="font-medium text-primary hover:underline">
					{row.getValue("name")}
				</Link>
			),
		},
		{
			accessorKey: "category",
			header: "Category",
		},
		{
			accessorKey: "level",
			header: "Level",
		},
		{
			accessorKey: "behavioralIndicators",
			header: "Indicators",
			cell: ({ row }) => row.original.behavioralIndicators?.length || 0,
		},
		{
			accessorKey: "isActive",
			header: "Status",
			cell: ({ row }) => {
				const isActive = row.getValue("isActive") as boolean;
				return (
					<Badge variant={isActive ? "default" : "secondary"}>
						{isActive ? "Active" : "Inactive"}
					</Badge>
				);
			},
		},
	];

	if (error) {
		return <ErrorCard error={error} onRetry={fetchData} />;
	}

	return (
		<div className="container mx-auto px-4 py-8 space-y-8">
			<Header title="Dashboard" subtitle="Overview of competencies and statistics" />

			{stats && <DashboardStatsCards stats={stats} />}

			<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
				<div className="lg:col-span-2 space-y-8">
					<div className="grid grid-cols-1 md:grid-cols-2 gap-8">
						{stats && (
							<CompetencyByCategoryBarChart
								data={Object.entries(stats.competenciesByCategory).map(([name, value]) => ({ name, value }))}
							/>
						)}
						{stats && (
							<CompetencyByLevelBarChart
								data={Object.entries(stats.competenciesByLevel).map(([name, value]) => ({ name, value }))}
							/>
						)}
					</div>
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<Activity className="h-5 w-5" />
								Competencies Overview
							</CardTitle>
							<CardDescription>
								Browse, search, and manage all competencies in your system
							</CardDescription>
						</CardHeader>
						<CardContent>
							<EntityTable columns={competencyColumns} data={competencies} />
						</CardContent>
					</Card>
				</div>
				<div className="space-y-8">
					<QuickActionsCard />
					{stats && <AverageIndicatorsGauge value={stats.averageIndicatorsPerCompetency} />}
					<TopCompetenciesCard competencies={competencies} />
				</div>
			</div>
		</div>
	);
}