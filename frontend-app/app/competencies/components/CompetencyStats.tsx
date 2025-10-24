"use client";


import { Card } from "@/components/ui/card";

import React, { useState, useEffect } from "react";

import {
  Activity,
  Target,
  Users,
} from "lucide-react";
import {
  Competency
} from "../../interfaces/domain-interfaces";

export default function CompetencyStats({ competencies }: { competencies: Competency[] }) {
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
      <Card className="p-4 flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-lg">
          <Target className="h-5 w-5 text-primary" />
        </div>
        <div>
          <div className="text-2xl font-bold">{stats.total}</div>
          <div className="text-xs text-muted-foreground">
            Total Competencies
          </div>
        </div>
      </Card>
      <Card className="p-4 flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-lg">
          <Activity className="h-5 w-5 text-primary" />
        </div>
        <div>
          <div className="text-2xl font-bold">{stats.active}</div>
          <div className="text-xs text-muted-foreground">
            Active Competencies
          </div>
        </div>
      </Card>
      <Card className="p-4 flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-lg">
          <Users className="h-5 w-5 text-primary" />
        </div>
        <div>
          <div className="text-2xl font-bold">{stats.categories}</div>
          <div className="text-xs text-muted-foreground">Categories</div>
        </div>
      </Card>
      <Card className="p-4 flex items-center space-x-4">
        <div className="p-3 bg-primary/10 rounded-lg">
          <Target className="h-5 w-5 text-primary" />
        </div>
        <div>
          <div className="text-2xl font-bold">{stats.totalIndicators}</div>
          <div className="text-xs text-muted-foreground">Total Indicators</div>
        </div>
      </Card>
    </div>
  );
};
