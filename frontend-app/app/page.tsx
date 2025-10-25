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

import {
	Activity
} from "lucide-react";
import { competenciesApi } from "@/services/api";
import { Competency, DashboardStats } from "./interfaces/domain-interfaces";
import CompetencyCardGrid from "./components/CompetencyCardGrid";
import Header from "./components/Header";
import DashboardStatsCards from "./components/DasboardStatsCards";
import ErrorCard from "./components/ErrorCard";

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
      <Header
        title="Dashboard"
        subtitle="Overview of competencies and statistics"
      />

      {/* Statistics Overview */}
      {stats && (
       <DashboardStatsCards stats={stats} />
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
