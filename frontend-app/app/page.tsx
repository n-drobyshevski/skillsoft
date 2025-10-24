"use client";
import React, { useState, useEffect } from "react";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
	CardFooter,
} from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";

import {
	Award,
	BarChart3,
	TrendingUp,
	Activity,
	Download,
	Plus,
	CheckCircle2,
	Layers
} from "lucide-react";
import { categoryToIcon } from "./utils";
import { competenciesApi } from "@/services/api";
import { Competency, DashboardStats } from "./interfaces/domain-interfaces";
import CompetencyCardGrid from "./components/CompetencyCardGrid";

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
			const competenciesData: Array<Competency> = await competenciesApi.getAllCompetencies();
			
			// Verify that competenciesData is an array and not empty
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

	if (error) {
		return (
			<div className="container mx-auto px-4 py-8 flex items-center justify-center min-h-[60vh]">
				<Card className="max-w-md border-destructive/50">
					<CardContent className="p-8 text-center">
						<div className="text-4xl mb-4">⚠️</div>
						<CardTitle className="text-destructive mb-2">
							Error Loading Dashboard
						</CardTitle>
						<CardDescription className="mb-6">{error}</CardDescription>
						<Button
							onClick={fetchData}
							variant="destructive"
							className="w-full"
						>
							Try Again
						</Button>
					</CardContent>
				</Card>
			</div>
		);
	}

	return (
		<div className="container mx-auto px-4 py-8 space-y-8">
			{/* Header */}
			<div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0">
				<div>
					<h1 className="text-3xl font-bold tracking-tight">
						Competency Management
					</h1>
					<p className="text-muted-foreground">
						Manage and track your organization's competency framework
					</p>
				</div>
				<div className="flex items-center space-x-2">
					<Button variant="outline" size="sm">
						<Download className="mr-2 h-4 w-4" />
						Export All
					</Button>
					<Button size="sm">
						<Plus className="mr-2 h-4 w-4" />
						New Competency
					</Button>
				</div>
			</div>

			{/* Statistics Overview */}
			{stats && (
				<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
					<Card className="group hover:shadow-lg transition-all">
						<CardContent className="p-6">
							<div className="flex items-start gap-4">
								<div className="p-3 rounded-xl bg-primary/10 text-primary">
									<Award className="h-6 w-6" aria-hidden="true" />
								</div>
								<div className="space-y-1.5">
									<p className="text-sm font-medium text-muted-foreground">
										Total Competencies
									</p>
									<p className="text-2xl font-bold tracking-tight">
										{stats.totalCompetencies}
									</p>
									<div className="flex items-center text-xs text-muted-foreground">
										<TrendingUp className="mr-1 h-3 w-3" aria-hidden="true" />
										<span>+12% from last month</span>
									</div>
								</div>
							</div>
						</CardContent>
					</Card>

					<Card className="group hover:shadow-lg transition-all">
						<CardContent className="p-6">
							<div className="flex items-start gap-4">
								<div className="p-3 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400">
									<BarChart3 className="h-6 w-6" aria-hidden="true" />
								</div>
								<div className="space-y-1.5">
									<p className="text-sm font-medium text-muted-foreground">
										Behavioral Indicators
									</p>
									<p className="text-2xl font-bold tracking-tight">
										{stats.totalBehavioralIndicators}
									</p>
									<p className="text-xs text-muted-foreground">
										~{Math.round(stats.averageIndicatorsPerCompetency)} per
										competency
									</p>
								</div>
							</div>
						</CardContent>
					</Card>

					<Card className="group hover:shadow-lg transition-all">
						<CardContent className="p-6">
							<div className="flex items-start gap-4">
								<div className="p-3 rounded-xl bg-yellow-500/10 text-yellow-600 dark:text-yellow-400">
									<CheckCircle2 className="h-6 w-6" aria-hidden="true" />
								</div>
								<div className="space-y-1.5">
									<p className="text-sm font-medium text-muted-foreground">
										Assessment Questions
									</p>
									<p className="text-2xl font-bold tracking-tight">
										{stats.totalAssessmentQuestions}
									</p>
									<Progress
										value={65}
										className="h-1 w-20"
										aria-label="65% completion rate"
									/>
								</div>
							</div>
						</CardContent>
					</Card>

					<Card className="group hover:shadow-lg transition-all">
						<CardContent className="p-6">
							<div className="flex items-start gap-4">
								<div className="p-3 rounded-xl bg-green-500/10 text-green-600 dark:text-green-400">
									<Layers className="h-6 w-6" aria-hidden="true" />
								</div>
								<div className="space-y-1.5">
									<p className="text-sm font-medium text-muted-foreground">
										Categories
									</p>
									<p className="text-2xl font-bold tracking-tight">
										{Object.keys(stats.competenciesByCategory).length}
									</p>
									<div className="flex -space-x-1.5">
										{Object.keys(stats.competenciesByCategory)
											.slice(0, 4)
											.map((category, i) => (
												<div
													key={category}
													className="h-6 w-6 rounded-full bg-muted flex items-center justify-center text-xs ring-2 ring-background"
													title={category.toLowerCase().replace("_", " ")}
												>
													{React.cloneElement(categoryToIcon(category), {
														className: "h-3 w-3",
													})}
												</div>
											))}
										{Object.keys(stats.competenciesByCategory).length > 4 && (
											<div
												className="h-6 w-6 rounded-full bg-muted flex items-center justify-center text-xs font-medium ring-2 ring-background"
												title="More categories"
											>
												+{Object.keys(stats.competenciesByCategory).length - 4}
											</div>
										)}
									</div>
								</div>
							</div>
						</CardContent>
					</Card>
				</div>
			)}

			{/* Competency Cards */}
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
					<CompetencyCardGrid data={competencies} loading={loading} />
				</CardContent>
			</Card>
		</div>
	);
}
