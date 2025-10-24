"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
	type ColumnDef,
	type ColumnFiltersState,
	type SortingState,
	type VisibilityState,
	flexRender,
	getCoreRowModel,
	getFilteredRowModel,
	getPaginationRowModel,
	getSortedRowModel,
	useReactTable,
} from "@tanstack/react-table";
import {
	Table,
	TableBody,
	TableCell,
	TableHead,
	TableHeader,
	TableRow,
} from "./ui/table";
import {
	DropdownMenu,
	DropdownMenuCheckboxItem,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuLabel,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "./ui/dropdown-menu";
import { Button } from "./ui/button";
import { Input } from "./ui/input";
import { Badge } from "./ui/badge";
import { Card } from "./ui/card";

import {
	ArrowUpDown,
	ChevronDown,
	ChevronLeft,
	GraduationCap,
	Binary,
	ChevronRight,
	BrainCircuit,
	MessageSquare,
	Heart,
	Eye,
	LightbulbIcon,
	UsersRound,
	Search,
	Filter,
	MoreHorizontal,
	Award,
	BarChart3,
	Activity,
	Calendar,
	Settings2,
	Download,
	Pencil,
	Plus,
	Target,
	Clock,
	Users,
} from "lucide-react";

interface BehavioralIndicator {
	id: string;
	title: string;
	description: string;
	observabilityLevel: string;
	measurementType: string;
	weight: number;
	examples: string;
	counterExamples: string;
	isActive: boolean;
	approvalStatus: string;
	orderIndex: number;
}

enum CompetencyCategory {
	COGNITIVE = "COGNITIVE",
	INTERPERSONAL = "INTERPERSONAL",
	LEADERSHIP = "LEADERSHIP",
	ADAPTABILITY = "ADAPTABILITY",
	EMOTIONAL_INTELLIGENCE = "EMOTIONAL_INTELLIGENCE",
	COMMUNICATION = "COMMUNICATION",
	COLLABORATION = "COLLABORATION",
	CRITICAL_THINKING = "CRITICAL_THINKING",
	TIME_MANAGEMENT = "TIME_MANAGEMENT",
}

enum ProficiencyLevel {
	NOVICE = "NOVICE",
	DEVELOPING = "DEVELOPING",
	PROFICIENT = "PROFICIENT",
	ADVANCED = "ADVANCED",
	EXPERT = "EXPERT",
}

enum ApprovalStatus {
	DRAFT = "DRAFT",
	PENDING_REVIEW = "PENDING_REVIEW",
	APPROVED = "APPROVED",
	REJECTED = "REJECTED",
}

interface Competency {
	id: string;
	name: string;
	description: string;
	category: CompetencyCategory;
	level: ProficiencyLevel;
	isActive: boolean;
	approvalStatus: ApprovalStatus;
	version: number;
	createdAt: string;
	lastModified: string;
	behavioralIndicators?: BehavioralIndicator[];
}

const API_BASE_URL = "http://localhost:8080/api";

// Main component
const CompetenciesPage: React.FC = () => {
	const [competencies, setCompetencies] = useState<Competency[]>([]);
	const [loading, setLoading] = useState<boolean>(true);
	const [sorting, setSorting] = useState<SortingState>([]);
	const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
	const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
	const [rowSelection, setRowSelection] = useState({});

	// Column definitions
	const columns: ColumnDef<Competency>[] = [
		{
			accessorKey: "name",
			header: ({ column }) => {
				return (
					<Button
						variant="ghost"
						onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
					>
						Competency
						<ArrowUpDown className="ml-2 h-4 w-4" />
					</Button>
				);
			},
			cell: ({ row }) => {
				const category = row.original.category;
				return (
					<div className="flex items-start gap-3">
						<div className="shrink-0 w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center">
							<span className="text-base">{categoryToIcon(category)}</span>
						</div>
						<div className="flex-1 min-w-0">
							<Link
								href={`/competencies/${row.original.id}`}
								className="font-medium text-primary hover:underline block truncate"
							>
								{row.getValue("name")}
							</Link>
							<p className="text-xs text-muted-foreground truncate mt-0.5">
								{row.original.description}
							</p>
						</div>
					</div>
				);
			},
		},
		{
			accessorKey: "category",
			header: ({ column }) => (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Category
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => {
				const category = row.getValue("category") as CompetencyCategory;
				return (
					<div className="flex items-center gap-2">
						<span>{categoryToIcon(category)}</span>
						<span className="font-medium capitalize">
							{category.toLowerCase().replace("_", " ")}
						</span>
					</div>
				);
			},
		},
		{
			accessorKey: "level",
			header: ({ column }) => (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Level
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => {
				const level = row.getValue("level") as ProficiencyLevel;
				return (
					<Badge variant="outline" className={proficiencyLevelToColor(level)}>
						{level}
					</Badge>
				);
			},
		},
		{
			accessorKey: "behavioralIndicators",
			header: ({ column }) => (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Indicators
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => {
				const indicators = row.getValue(
					"behavioralIndicators",
				) as BehavioralIndicator[];
				return (
					<div className="flex items-center gap-2">
						<Target className="h-4 w-4 text-muted-foreground" />
						<span className="font-medium">{indicators?.length || 0}</span>
					</div>
				);
			},
		},
		{
			accessorKey: "version",
			header: ({ column }) => (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Version
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => (
				<div className="flex items-center gap-2">
					<Activity className="h-4 w-4 text-muted-foreground" />
					<span className="font-medium">v{row.getValue("version")}</span>
				</div>
			),
		},
		{
			accessorKey: "lastModified",
			header: ({ column }) => (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Updated
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			),
			cell: ({ row }) => {
				const date = new Date(row.getValue("lastModified"));
				return (
					<div className="flex items-center gap-2">
						<Clock className="h-4 w-4 text-muted-foreground" />
						<span className="font-medium">{date.toLocaleDateString()}</span>
					</div>
				);
			},
		},
		{
			accessorKey: "isActive",
			header: "Status",
			cell: ({ row }) => {
				const isActive = row.getValue("isActive") as boolean;
				const status = row.original.approvalStatus;
				return (
					<div className="flex items-center gap-2">
						<Badge variant={isActive ? "default" : "secondary"}>
							{isActive ? "Active" : "Inactive"}
						</Badge>
						<Badge variant="outline" className="text-xs">
							{status}
						</Badge>
					</div>
				);
			},
		},
		{
			id: "actions",
			cell: ({ row }) => {
				const competency = row.original;
				return (
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
							<DropdownMenuItem>
								<Eye className="mr-2 h-4 w-4" />
								View Details
							</DropdownMenuItem>
							<DropdownMenuItem>
								<Settings2 className="mr-2 h-4 w-4" />
								Edit Competency
							</DropdownMenuItem>
						</DropdownMenuContent>
					</DropdownMenu>
				);
			},
		},
	]; // Helper functions

	const categoryToIcon = (category: string) => {
		const icons = {
			COGNITIVE: BrainCircuit,
			INTERPERSONAL: Users,
			LEADERSHIP: GraduationCap,
			ADAPTABILITY: Binary,
			EMOTIONAL_INTELLIGENCE: Heart,
			COMMUNICATION: MessageSquare,
			COLLABORATION: UsersRound,
			CRITICAL_THINKING: LightbulbIcon,
			TIME_MANAGEMENT: Clock,
		};
		const IconComponent = icons[category as keyof typeof icons] || Award;
		return React.createElement(IconComponent, { className: "h-4 w-4" });
	};
	const proficiencyLevelToColor = (level: ProficiencyLevel): string => {
		const colors: { [key in ProficiencyLevel]: string } = {
			[ProficiencyLevel.NOVICE]:
				"border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
			[ProficiencyLevel.DEVELOPING]:
				"border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
			[ProficiencyLevel.PROFICIENT]:
				"border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
			[ProficiencyLevel.ADVANCED]:
				"border-blue-500/20 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
			[ProficiencyLevel.EXPERT]:
				"border-violet-500/20 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
		};
		return colors[level];
	};

	useEffect(() => {
		const fetchCompetencies = async () => {
			try {
				setLoading(true);
				const response = await fetch(`${API_BASE_URL}/competencies`, {
					headers: {
						"Content-Type": "application/json",
						Accept: "application/json",
					},
				});

				if (!response.ok) {
					throw new Error(`HTTP error! status: ${response.status}`);
				}

				const data = await response.json();
				setCompetencies(data);
			} catch (error) {
				console.error("Failed to fetch competencies:", error);
			} finally {
				setLoading(false);
			}
		};

		fetchCompetencies();
	}, []);

	// Initialize table
	const table = useReactTable({
		data: competencies,
		columns,
		onSortingChange: setSorting,
		onColumnFiltersChange: setColumnFilters,
		getCoreRowModel: getCoreRowModel(),
		getPaginationRowModel: getPaginationRowModel(),
		getSortedRowModel: getSortedRowModel(),
		getFilteredRowModel: getFilteredRowModel(),
		onColumnVisibilityChange: setColumnVisibility,
		onRowSelectionChange: setRowSelection,
		state: {
			sorting,
			columnFilters,
			columnVisibility,
			rowSelection,
		},
		initialState: {
			pagination: {
				pageSize: 10,
			},
		},
	});

	// Stats component
	const CompetencyStats: React.FC<{ competencies: Competency[] }> = ({
		competencies,
	}) => {
		const stats = React.useMemo(() => {
			const total = competencies.length;
			const active = competencies.filter((c) => c.isActive).length;
			const totalIndicators = competencies.reduce(
				(sum, c) => sum + (c.behavioralIndicators?.length || 0),
				0,
			);
			const categories = new Set(competencies.map((c) => c.category)).size;

			return { total, active, totalIndicators, categories };
		}, [competencies]);

		return (
			<div className="grid grid-cols-1 md:grid-cols-4 gap-4">
				<Card className="p-4 flex items-center space-x-4">
					<div className="p-3 bg-primary/10 rounded-lg">
						<Target className="h-5 w-5 text-primary" />
					</div>
					<div>
						<div className="text-2xl font-bold">{stats.total}</div>
						<div className="text-xs text-muted-foreground">
							Total Competencies
						</div>
					</div>
				</Card>
				<Card className="p-4 flex items-center space-x-4">
					<div className="p-3 bg-primary/10 rounded-lg">
						<Activity className="h-5 w-5 text-primary" />
					</div>
					<div>
						<div className="text-2xl font-bold">{stats.active}</div>
						<div className="text-xs text-muted-foreground">
							Active Competencies
						</div>
					</div>
				</Card>
				<Card className="p-4 flex items-center space-x-4">
					<div className="p-3 bg-primary/10 rounded-lg">
						<Users className="h-5 w-5 text-primary" />
					</div>
					<div>
						<div className="text-2xl font-bold">{stats.categories}</div>
						<div className="text-xs text-muted-foreground">Categories</div>
					</div>
				</Card>
				<Card className="p-4 flex items-center space-x-4">
					<div className="p-3 bg-primary/10 rounded-lg">
						<Target className="h-5 w-5 text-primary" />
					</div>
					<div>
						<div className="text-2xl font-bold">{stats.totalIndicators}</div>
						<div className="text-xs text-muted-foreground">
							Total Indicators
						</div>
					</div>
				</Card>
			</div>
		);
	};

	return (
		<div className="container mx-auto px-4 py-8 space-y-8 w-full">
			{/* Header */}
			<div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
				<div>
					<h1 className="text-3xl font-bold tracking-tight">Competencies</h1>
					<p className="text-muted-foreground">
						Manage and track organizational competencies and skills
					</p>
				</div>
				<div className="flex items-center gap-2">
					<Button variant="outline">
						<Download className="mr-2 h-4 w-4" />
						Export
					</Button>
					<Button>
						<Plus className="mr-2 h-4 w-4" />
						New Competency
					</Button>
				</div>
			</div>

			{/* Stats Cards */}
			<CompetencyStats competencies={competencies} />

			{/* Table Controls */}
			<div className="flex flex-1 items-center space-x-2">
				<div className="flex flex-1 items-center space-x-2">
					<div className="relative flex-1 max-w-sm">
						<Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
						<Input
							placeholder="Search competencies..."
							value={
								(table.getColumn("name")?.getFilterValue() as string) ?? ""
							}
							onChange={(event) =>
								table.getColumn("name")?.setFilterValue(event.target.value)
							}
							className="pl-9"
						/>
					</div>
					<DropdownMenu>
						<DropdownMenuTrigger asChild>
							<Button variant="outline" className="ml-auto">
								<Filter className="mr-2 h-4 w-4" />
								View
							</Button>
						</DropdownMenuTrigger>
						<DropdownMenuContent align="end">
							<DropdownMenuLabel>Toggle columns</DropdownMenuLabel>
							<DropdownMenuSeparator />
							{table
								.getAllColumns()
								.filter((column) => column.getCanHide())
								.map((column) => {
									return (
										<DropdownMenuCheckboxItem
											key={column.id}
											className="capitalize"
											checked={column.getIsVisible()}
											onCheckedChange={(value) =>
												column.toggleVisibility(!!value)
											}
										>
											{column.id}
										</DropdownMenuCheckboxItem>
									);
								})}
						</DropdownMenuContent>
					</DropdownMenu>
				</div>
			</div>

			{/* Loading State */}
			{loading && (
				<div className="flex items-center justify-center py-12">
					<div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-500"></div>
					<span className="ml-3 text-muted-foreground">
						Loading competencies...
					</span>
				</div>
			)}

			{/* Table */}
			{!loading && (
				<>
					<div className="rounded-md border">
						<Table>
							<TableHeader>
								{table.getHeaderGroups().map((headerGroup) => (
									<TableRow key={headerGroup.id}>
										{headerGroup.headers.map((header) => {
											return (
												<TableHead key={header.id}>
													{header.isPlaceholder
														? null
														: flexRender(
																header.column.columnDef.header,
																header.getContext(),
															)}
												</TableHead>
											);
										})}
									</TableRow>
								))}
							</TableHeader>
							<TableBody>
								{table.getRowModel().rows?.length ? (
									table.getRowModel().rows.map((row) => (
										<TableRow
											key={row.id}
											data-state={row.getIsSelected() && "selected"}
										>
											{row.getVisibleCells().map((cell) => (
												<TableCell key={cell.id}>
													{flexRender(
														cell.column.columnDef.cell,
														cell.getContext(),
													)}
												</TableCell>
											))}
										</TableRow>
									))
								) : (
									<TableRow>
										<TableCell
											colSpan={columns.length}
											className="h-24 text-center"
										>
											No competencies found.
										</TableCell>
									</TableRow>
								)}
							</TableBody>
						</Table>
					</div>

					{/* Pagination */}
					<div className="flex items-center justify-between space-x-2 py-4">
						<div className="flex-1 text-sm text-muted-foreground">
							{table.getFilteredSelectedRowModel().rows.length} of{" "}
							{table.getFilteredRowModel().rows.length} row(s) selected.
						</div>
						<div className="flex items-center space-x-6 lg:space-x-8">
							<div className="flex items-center space-x-2">
								<p className="text-sm font-medium">Rows per page</p>
								<select
									value={table.getState().pagination.pageSize}
									onChange={(e) => {
										table.setPageSize(Number(e.target.value));
									}}
									className="h-8 w-[70px] rounded-md border border-input bg-transparent"
								>
									{[10, 20, 30, 40, 50].map((pageSize) => (
										<option key={pageSize} value={pageSize}>
											{pageSize}
										</option>
									))}
								</select>
							</div>
							<div className="flex w-[100px] items-center justify-center text-sm font-medium">
								Page {table.getState().pagination.pageIndex + 1} of{" "}
								{table.getPageCount()}
							</div>
							<div className="flex items-center space-x-2">
								<Button
									variant="outline"
									className="hidden h-8 w-8 p-0 lg:flex"
									onClick={() => table.setPageIndex(0)}
									disabled={!table.getCanPreviousPage()}
								>
									<span className="sr-only">Go to first page</span>
									<ChevronLeft className="h-4 w-4" />
									<ChevronLeft className="h-4 w-4" />
								</Button>
								<Button
									variant="outline"
									className="h-8 w-8 p-0"
									onClick={() => table.previousPage()}
									disabled={!table.getCanPreviousPage()}
								>
									<span className="sr-only">Go to previous page</span>
									<ChevronLeft className="h-4 w-4" />
								</Button>
								<Button
									variant="outline"
									className="h-8 w-8 p-0"
									onClick={() => table.nextPage()}
									disabled={!table.getCanNextPage()}
								>
									<span className="sr-only">Go to next page</span>
									<ChevronRight className="h-4 w-4" />
								</Button>
								<Button
									variant="outline"
									className="hidden h-8 w-8 p-0 lg:flex"
									onClick={() => table.setPageIndex(table.getPageCount() - 1)}
									disabled={!table.getCanNextPage()}
								>
									<span className="sr-only">Go to last page</span>
									<ChevronRight className="h-4 w-4" />
									<ChevronRight className="h-4 w-4" />
								</Button>
							</div>
						</div>
					</div>
				</>
			)}
		</div>
	);
};

export default CompetenciesPage;
