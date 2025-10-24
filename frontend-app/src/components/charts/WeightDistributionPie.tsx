import type React from "react";
import {
	PieChart,
	Pie,
	Cell,
	ResponsiveContainer,
	Legend,
	Tooltip,
} from "recharts";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import type { BehavioralIndicator } from "../../types/competency";
import { BarChart3 } from "lucide-react";

interface WeightDistributionPieProps {
	indicators: BehavioralIndicator[];
}

const WeightDistributionPie: React.FC<WeightDistributionPieProps> = ({
	indicators,
}) => {
	// Calculate total weight for percentage calculation
	const totalWeight = indicators.reduce((sum, i) => sum + i.weight, 0);

	// Prepare data for pie chart with percentage values
	const data = indicators.map((indicator) => {
		const percentage = (indicator.weight / totalWeight) * 100;
		return {
			name: indicator.title,
			value: percentage,
			displayValue: `${percentage.toFixed(1)}%`,
			weight: indicator.weight,
			description: indicator.description,
		};
	});

	// Colors for different weights
	const COLORS = [
		"rgb(132 204 22)", // lime-500
		"rgb(34 197 94)", // green-500
		"rgb(16 185 129)", // emerald-500
		"rgb(6 182 212)", // cyan-500
		"rgb(59 130 246)", // blue-500
	];

	return (
		<Card>
			<CardHeader>
				<CardTitle className="flex items-center gap-2">
					<BarChart3 className="w-5 h-5" />
					Indicators Weight Distribution (%)
				</CardTitle>
			</CardHeader>
			<CardContent>
				<div className="h-[300px]">
					<ResponsiveContainer width="100%" height="100%">
						<PieChart>
							<Pie
								data={data}
								cx="50%"
								cy="50%"
								innerRadius={70}
								outerRadius={90}
								paddingAngle={2}
								dataKey="value"
								nameKey="name"
								label={false}
							>
								{data.map((entry, index) => (
									<Cell
										key={`cell-${index}`}
										fill={COLORS[index % COLORS.length]}
										className="stroke-background hover:opacity-80 transition-opacity"
									/>
								))}
							</Pie>
							<Tooltip
								content={({ active, payload }) => {
									if (active && payload && payload.length) {
										const data = payload[0].payload;
										return (
											<div className="p-3 bg-background border rounded-lg shadow-lg max-w-[300px]">
												<p className="text-sm font-medium mb-1">{data.name}</p>
												{data.description && (
													<p className="text-xs text-muted-foreground mb-2">
														{data.description}
													</p>
												)}
												<div className="text-xs space-y-1">
													<p className="font-medium">
														Contribution: {data.displayValue}
													</p>
													<p className="text-muted-foreground">
														Weight: {data.weight}
													</p>
												</div>
											</div>
										);
									}
									return null;
								}}
							/>
							<Legend
								layout="vertical"
								align="right"
								verticalAlign="middle"
								formatter={(value: string, entry: any) => {
									const data = entry.payload;
									return `${data.name} (${data.displayValue})`;
								}}
								wrapperStyle={{
									paddingLeft: "20px",
									maxHeight: "240px",
									overflowY: "auto",
								}}
							/>
						</PieChart>
					</ResponsiveContainer>
				</div>
			</CardContent>
		</Card>
	);
};

export default WeightDistributionPie;
