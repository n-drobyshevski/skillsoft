"use client";

import React, { useState } from "react";
import StatsCard from "./StatsCard";
import {
  Award,
  BarChart3,
  TrendingUp,
  Activity,
  Download,
  Plus,
  CheckCircle2,
  Layers,
} from "lucide-react";
import { DashboardStats } from "../interfaces/domain-interfaces";
import { Progress } from "@/components/ui/progress";
import { competencyCategoryToIcon } from "../utils";

export default function DashboardStatsCards({stats}: {stats: DashboardStats}) {
    return  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <StatsCard
            title="Total Competencies"
            value={stats.totalCompetencies}
            icon={TrendingUp}
          >
            <span>+12% from last month</span>
          </StatsCard>
          <StatsCard
            title="Total Behavioral Indicators"
            value={stats.totalBehavioralIndicators}
            icon={BarChart3}
            style="p-3 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400"
          >
            <TrendingUp className="mr-1 h-3 w-3" aria-hidden="true" />~
            <span>
              {Math.round(stats.averageIndicatorsPerCompetency)} per competency
            </span>
          </StatsCard>
          <StatsCard
            title="Total Assessment Questions"
            value={stats.totalAssessmentQuestions}
            icon={CheckCircle2}
            style="p-3 rounded-xl bg-yellow-500/10 text-yellow-600 dark:text-yellow-400"
          >
            <Progress
              value={65}
              className="h-1 w-20"
              aria-label="65% completion rate"
            />
          </StatsCard>
          <StatsCard
            title="Categories"
            value={Object.keys(stats.competenciesByCategory).length}
            icon={Layers}
            style="p-3 rounded-xl bg-green-500/10 text-green-600 dark:text-green-400"
          >
            {" "}
            <div className="flex -space-x-1.5">
              {Object.keys(stats.competenciesByCategory)
                .slice(0, 4)
                .map((category, i) => (
                  <div
                    key={category}
                    className="h-6 w-6 rounded-full bg-muted flex items-center justify-center text-xs ring-2 ring-background"
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
        </div>;
}