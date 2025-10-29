"use client";

import React from "react";
import StatsCard from "./StatsCard";
import {
  BarChart3,
  TrendingUp,
  CheckCircle2,
  Layers,
} from "lucide-react";
import { DashboardStats } from "../interfaces/domain-interfaces";
import { competencyCategoryToIcon } from "../utils";

export default function DashboardStatsCards({ stats }: { stats: DashboardStats }) {
  return (
    <div className="grid grid-cols-2 lg:grid-cols-4 gap-3 md:gap-4 lg:gap-6">
      <StatsCard
        title="Total Competencies"
        value={stats.totalCompetencies}
        icon={TrendingUp}
      >
        +12% from last month
      </StatsCard>
      <StatsCard
        title="Total Behavioral Indicators"
        value={stats.totalBehavioralIndicators}
        icon={BarChart3}
      >
        ~{Math.round(stats.averageIndicatorsPerCompetency)} per competency
      </StatsCard>
      <StatsCard
        title="Total Assessment Questions"
        value={stats.totalAssessmentQuestions}
        icon={CheckCircle2}
      >
        65% completion rate
      </StatsCard>
      <StatsCard
        title="Categories"
        value={Object.keys(stats.competenciesByCategory).length}
        icon={Layers}
      >
        <div className="flex items-center -space-x-2">
          {Object.keys(stats.competenciesByCategory)
            .slice(0, 4)
            .map((category) => (
              <div
                key={category}
                className="h-6 w-6 rounded-full bg-muted flex items-center justify-center ring-2 ring-background"
                title={category.toLowerCase().replace("_", " ")}
              >
                {React.cloneElement(competencyCategoryToIcon(category), {
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
      </StatsCard>
    </div>
  );
}