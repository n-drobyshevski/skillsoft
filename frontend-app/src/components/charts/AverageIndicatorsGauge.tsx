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
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium">Avg. Indicators per Competency</CardTitle>
        <BarChart3 className="h-4 w-4 text-muted-foreground" />
      </CardHeader>
      <CardContent>
        <div className="flex items-baseline gap-2">
            <div className="text-4xl font-bold">{value.toFixed(1)}</div>
            <div className="h-[40px] w-[100px]">
                <ResponsiveContainer width="100%" height="100%">
                <LineChart data={data}>
                    <Line type="monotone" dataKey="value" stroke="hsl(var(--primary))" strokeWidth={2} dot={false} />
                </LineChart>
                </ResponsiveContainer>
            </div>
        </div>
        <p className="text-xs text-muted-foreground">+2.1% from last month</p>
      </CardContent>
    </Card>
  );
}