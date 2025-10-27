"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import React from "react";
import { LucideProps } from "lucide-react";

export default function StatsCard({
  title,
  value,
  icon: Icon,
  children,
}: {
  title: string;
  value: number | string;
  children?: React.ReactNode;
  icon: React.ForwardRefExoticComponent<
    Omit<LucideProps, "ref"> & React.RefAttributes<SVGSVGElement>
  >;
}) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">{title}</CardTitle>
        <Icon className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold">{value}</div>
        {children && (
          <div className="text-xs text-muted-foreground">{children}</div>
        )}
      </CardContent>
    </Card>
  );
}
