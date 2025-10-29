"use client";
import { useEffect, useState } from "react";
import { Bar, BarChart, ResponsiveContainer, XAxis, YAxis, Tooltip, Legend, Cell } from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { BrainCircuit } from "lucide-react";

interface CompetencyByCategoryBarChartProps {
  data: { name: string; value: number }[];
}

export default function CompetencyByCategoryBarChart({ data }: CompetencyByCategoryBarChartProps) {
  const [chartColors, setChartColors] = useState<string[]>([]);

  useEffect(() => {
    const root = getComputedStyle(document.documentElement);
    const colors = [
      root.getPropertyValue('--chart-1').trim(),
      root.getPropertyValue('--chart-2').trim(),
      root.getPropertyValue('--chart-3').trim(),
      root.getPropertyValue('--chart-4').trim(),
      root.getPropertyValue('--chart-5').trim(),
    ];
    setChartColors(colors);
  }, []);

  return (
    <Card className="min-h-[300px] md:min-h-[400px]">
      <CardHeader className="space-y-0 pb-2">
        <div className="flex items-center gap-2">
          <BrainCircuit className="h-4 w-4 md:h-5 md:w-5" />
          <CardTitle className="text-base md:text-lg">Competencies by Category</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        <div className="h-[250px] md:h-[350px]">
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 20, right: 20, left: 20, bottom: 20 }}>
              <XAxis
                dataKey="name"
                stroke="#888888"
                fontSize={10}
                tickLine={false}
                axisLine={false}
                interval={0}
                tick={{ transform: 'translate(0, 8)' }}
                height={50}
              />
              <YAxis
                stroke="#888888"
                fontSize={10}
                tickLine={false}
                axisLine={false}
                tickFormatter={(value) => `${value}`}
                width={30}
              />
              <Tooltip
                cursor={{ fill: "hsl(var(--muted))" }}
                content={({ active, payload, label }) => {
                  if (active && payload && payload.length) {
                    return (
                      <div className="p-2 md:p-3 bg-background border rounded-lg shadow-lg">
                        <p className="text-xs md:text-sm font-medium truncate max-w-[200px]">{label}</p>
                        <p className="text-[10px] md:text-xs text-muted-foreground mt-0.5">
                          {`Competencies: ${payload[0].value}`}
                        </p>
                      </div>
                    );
                  }
                  return null;
                }}
              />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={chartColors[index % chartColors.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </div>
      </CardContent>
    </Card>
  );
}
