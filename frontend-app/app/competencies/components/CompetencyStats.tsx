"use client";

import React from "react";

import { Activity, Target, Users } from "lucide-react";
import { Competency } from "../../interfaces/domain-interfaces";
import StatsCard from "../../components/StatsCard";

export default function CompetencyStats({
  competencies,
}: {
  competencies: Competency[];
}) {
  competencies = competencies || [];

  const stats = React.useMemo(() => {
    const total = competencies.length;
    const active = competencies.filter((c) => c.isActive).length;
    const totalIndicators = competencies.reduce(
      (sum, c) => sum + (c.behavioralIndicators?.length || 0),
      0
    );
    const categories = new Set(competencies.map((c) => c.category)).size;

    return { total, active, totalIndicators, categories };
  }, [competencies]);

  return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
      <StatsCard
        title="Total Competencies"
        value={stats.total}
        icon={Target}
      />
      <StatsCard
        title="Active Competencies"
        value={stats.active}
        icon={Activity}
      />
      <StatsCard
        title="Categories"
        value={stats.categories}
        icon={Users}
      />
      <StatsCard
        title="Total Indicators"
        value={stats.totalIndicators}
        icon={Target}
      />
    </div>
  );
}
