"use client";
import React from "react";
import { Card } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import Link from "next/link";
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuItem,
    DropdownMenuLabel,
    DropdownMenuSeparator,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"; 
import { Competency } from "../interfaces/domain-interfaces";
import { levelToColor } from "../utils";
import { categoryToIcon } from "../utils";

import {
  MoreHorizontal,
  BarChart3,
  Tag,
  Calendar,
  Settings2,
  Download,
  Eye
} from "lucide-react";
// Competency Card Component
export default function CompetencyCard({ competency }: { competency: Competency }) {
	return (
		<Card className="group transition-all hover:shadow-md dark:hover:shadow-accent/5">
			<div className="flex flex-col md:flex-row md:items-center gap-4 p-6">
				{/* Left Section - Icon and Primary Info */}
				<div className="flex md:flex-col items-center justify-center md:w-24 shrink-0">
					<div className="h-16 w-16 rounded-xl bg-muted flex items-center justify-center">
						{React.cloneElement(categoryToIcon(competency.category), {
							className: "h-8 w-8",
						})}
					</div>
					<Badge
						variant="outline"
						className={`${levelToColor(
							competency.level,
						)} mt-2 hidden md:inline-flex`}
					>
						{competency.level}
					</Badge>
				</div>

				{/* Middle Section - Main Content */}
				<div className="flex-grow min-w-0">
					<div className="flex items-start justify-between mb-2">
						<div>
							<Link
								href={`/competencies/${competency.id}`}
								className="font-semibold text-xl hover:text-primary transition-colors line-clamp-1"
							>
								{competency.name}
							</Link>
							<div className="flex items-center gap-2 mt-1 md:mt-2">
								<Badge
									variant="outline"
									className={`${levelToColor(competency.level)} md:hidden`}
								>
									{competency.level}
								</Badge>
								<Badge
									variant={competency.isActive ? "default" : "secondary"}
									className="text-xs"
								>
									{competency.isActive ? "Active" : "Inactive"}
								</Badge>
								<div className="flex items-center text-muted-foreground text-sm">
									<Tag className="mr-1 h-4 w-4" />
									<span>
										{competency.category.toLowerCase().replace("_", " ")}
									</span>
								</div>
							</div>
						</div>
						<DropdownMenu>
							<DropdownMenuTrigger asChild>
								<Button variant="ghost" className="h-8 w-8 p-0">
									<span className="sr-only">Open menu</span>
									<MoreHorizontal className="h-4 w-4" />
								</Button>
							</DropdownMenuTrigger>
							<DropdownMenuContent align="end">
								<DropdownMenuLabel>Actions</DropdownMenuLabel>
								<DropdownMenuItem
									onClick={() => navigator.clipboard.writeText(competency.id)}
								>
									Copy ID
								</DropdownMenuItem>
								<DropdownMenuSeparator />
								<DropdownMenuItem asChild>
									<Link
										href={`/competencies/${competency.id}`}
										className="flex items-center"
									>
										<Eye className="mr-2 h-4 w-4" />
										View Details
									</Link>
								</DropdownMenuItem>
								<DropdownMenuItem>
									<Settings2 className="mr-2 h-4 w-4" />
									Edit Competency
								</DropdownMenuItem>
								<DropdownMenuItem>
									<Download className="mr-2 h-4 w-4" />
									Export
								</DropdownMenuItem>
							</DropdownMenuContent>
						</DropdownMenu>
					</div>

					<p className="text-muted-foreground text-sm leading-relaxed line-clamp-2 mb-3">
						{competency.description}
					</p>

					<div className="flex flex-wrap items-center gap-4 text-sm text-muted-foreground">
						<div className="flex items-center">
							<BarChart3 className="mr-1 h-4 w-4" />
							<span>
								{competency.behavioralIndicators?.length || 0} indicators
							</span>
						</div>
						<div className="flex items-center">
							<Calendar className="mr-1 h-4 w-4" />
							<span>
								{new Date(competency.lastModified).toLocaleDateString()}
							</span>
						</div>
						<div className="flex-grow" />
						<Button asChild variant="outline" className="ml-auto">
							<Link
								href={`/competencies/${competency.id}`}
								className="flex items-center"
							>
								View Details
								<Eye className="ml-2 h-4 w-4" />
							</Link>
						</Button>
					</div>
				</div>
			</div>
		</Card>
	);
}
