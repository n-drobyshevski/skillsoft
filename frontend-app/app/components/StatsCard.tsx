"use client";

import { Card, CardContent, CardTitle } from "@/components/ui/card";
import React, { useState } from "react";

import {
  Award,
  BarChart3,
  TrendingUp,
  Activity,
  Download,
  Plus,
  CheckCircle2,
  Layers,
  LucideProps,
} from "lucide-react";

export enum StatsCardVariant {
  NORMAL = "normal",
  CONTAINED = "contained",
}

export default function StatsCard({
  title,
  value,
  variant,
  icon: Icon,
  children,
  style,
}: {
  title: string;
  value: number;
  variant?: StatsCardVariant;
  children?: React.ReactNode;
  icon: React.ForwardRefExoticComponent<
    Omit<LucideProps, "ref"> & React.RefAttributes<SVGSVGElement>
  >;
  
  style?: string;
}) {
    return (
      <Card className="group hover:shadow-lg transition-all gap-4">
        {variant !== StatsCardVariant.CONTAINED ? (
          <CardTitle className="px-6 text-sm font-medium text-muted-foreground">
            {title}
          </CardTitle>
        ) : null}
        <CardContent className="px-6">
          <div className="flex items-center gap-4">
            <div
              className={
                style ? style : "p-3 rounded-xl bg-primary/10 text-primary"
              }
            >
              <Icon className="h-6 w-6" aria-hidden="true" />
            </div>
            <div className="space-y-1.5 flex flex-col justify-start">
              {variant === StatsCardVariant.CONTAINED ? (
                <p className="text-sm font-medium text-muted-foreground">
                  {title}
                </p>
              ) : null}
              <p className="text-2xl font-bold tracking-tight">{value}</p>
              <div className="flex items-center text-xs text-muted-foreground">
                {children}
              </div>
            </div>
          </div>
        </CardContent>
      </Card>
    );
}