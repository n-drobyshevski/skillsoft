"use client";

import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { cn } from "@/lib/utils";
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
    <Card className={cn(
      "relative overflow-hidden group transition-all duration-200 h-full",
      "hover:shadow-lg active:scale-[0.98]",
      "before:absolute before:inset-0 before:-translate-x-full hover:before:translate-x-0",
      "before:bg-linear-to-r before:from-transparent before:via-white/10 before:to-transparent",
      "before:transition-transform before:duration-500"
    )}>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 p-3 md:p-6 pb-2 md:pb-3">
        <CardTitle className="text-[13px] md:text-sm lg:text-base font-medium line-clamp-2">
          {title}
          <span className="sr-only">, value is {value}</span>
        </CardTitle>
        <Icon className="h-4 w-4 md:h-5 md:w-5 text-muted-foreground shrink-0 ml-2" aria-hidden="true" />
      </CardHeader>
      <CardContent className="p-3 md:p-6 pt-0 md:pt-0">
        <div className="text-xl md:text-2xl lg:text-3xl font-bold tracking-tight">{value}</div>
        {children && (
          <div className="text-[11px] md:text-xs lg:text-sm text-muted-foreground mt-1.5 line-clamp-2">
            {children}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
