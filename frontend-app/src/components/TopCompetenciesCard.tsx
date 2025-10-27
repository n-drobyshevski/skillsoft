"use client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { List } from "lucide-react";
import { Competency } from "@/app/interfaces/domain-interfaces";
import Link from "next/link";

interface TopCompetenciesCardProps {
  competencies: Competency[];
}

export default function TopCompetenciesCard({ competencies }: TopCompetenciesCardProps) {
  const topCompetencies = competencies
    .sort((a, b) => (b.behavioralIndicators?.length || 0) - (a.behavioralIndicators?.length || 0))
    .slice(0, 5);

  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <List className="w-5 h-5" />
          Top Competencies
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-4">
          {topCompetencies.map((competency) => (
            <div key={competency.id} className="flex justify-between items-center">
              <Link href={`/competencies/${competency.id}`} className="text-sm font-medium text-primary hover:underline">
                {competency.name}
              </Link>
              <div className="text-sm text-muted-foreground">
                {competency.behavioralIndicators?.length || 0} indicators
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
