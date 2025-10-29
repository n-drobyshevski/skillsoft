"use client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BarChart3 } from "lucide-react";
import { LineChart, Line, ResponsiveContainer } from "recharts";

interface AverageIndicatorsGaugeProps {
  value: number;
}

// Placeholder data for the sparkline
const data = [
  { value: 4.2 },
  { value: 4.5 },
  { value: 4.1 },
  { value: 4.8 },
  { value: 4.7 },
  { value: 5.1 },
  { value: 5.3 },
];

export default function AverageIndicatorsGauge({ value }: AverageIndicatorsGaugeProps) {
  return (
    <Card>
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2 sm:pb-3">
        <CardTitle className="text-sm sm:text-base font-medium">
          <span>Avg. Indicators per Competency</span>
          <span className="sr-only">, current value is {value.toFixed(1)}</span>
        </CardTitle>
        <BarChart3 className="h-4 w-4 sm:h-5 sm:w-5 text-muted-foreground" aria-hidden="true" />
      </CardHeader>
      <CardContent className="pt-2">
        <div className="flex items-baseline gap-2 sm:gap-3">
            <div className="text-3xl sm:text-4xl font-bold tracking-tight">{value.toFixed(1)}</div>
            <div className="h-[40px] w-[100px] sm:w-[120px]">
                <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data}>
                    <Line 
                      type="monotone" 
                      dataKey="value" 
                      stroke="hsl(var(--primary))" 
                      strokeWidth={2} 
                      dot={false} 
                      aria-label="Trend of average indicators"
                    />
                </LineChart>
                </ResponsiveContainer>
            </div>
        </div>
        <p className="text-xs text-muted-foreground">+2.1% from last month</p>
      </CardContent>
    </Card>
  );
}