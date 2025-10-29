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
    <Card className="hover:shadow-lg transition-all duration-200">
      <CardHeader className="pb-2 sm:pb-3">
        <CardTitle className="flex items-center gap-2 text-base sm:text-lg">
          <List className="w-4 h-4 sm:w-5 sm:h-5" aria-hidden="true" />
          Top Competencies
          <span className="sr-only">List of top {topCompetencies.length} competencies by number of indicators</span>
        </CardTitle>
      </CardHeader>
      <CardContent>
        <div className="space-y-3 sm:space-y-4">
          {topCompetencies.map((competency) => (
            <div key={competency.id} className="flex justify-between items-center group">
              <Link 
                href={`/competencies/${competency.id}`} 
                className="text-sm sm:text-base font-medium text-primary hover:underline flex-1 min-w-0 mr-4"
              >
                <span className="block truncate">{competency.name}</span>
              </Link>
              <div className="text-sm text-muted-foreground whitespace-nowrap">
                {competency.behavioralIndicators?.length || 0} indicators
              </div>
            </div>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
