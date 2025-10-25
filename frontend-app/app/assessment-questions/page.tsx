"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
	Sparkles,
	Clock,
	MoreHorizontal,
	Eye,
	Settings2,
	Download,
	Search,
	Filter,
	Plus,
	ArrowUpDown,
	ChevronLeft,
	ChevronRight,
	BarChart3,
	ListFilter,
} from "lucide-react";
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
} from "../../src/components/ui/table";
import {
	DropdownMenu,
	DropdownMenuCheckboxItem,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuLabel,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "../../src/components/ui/dropdown-menu";
import { Button } from "../../src/components/ui/button";
import { Input } from "../../src/components/ui/input";
import { Badge } from "../../src/components/ui/badge";
import { Card } from "../../src/components/ui/card";

import { AssessmentQuestion } from "../interfaces/domain-interfaces";
import { questionTypeToIcon, questionDifficultyToColor } from "../utils";
import { assessmentQuestionsApi } from "@/services/api";

// Column definitions
const columns: ColumnDef<AssessmentQuestion>[] = [
	{
		accessorKey: "questionText",
		header: ({ column }) => {
			return (
				<Button
					variant="ghost"
					onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
				>
					Question
					<ArrowUpDown className="ml-2 h-4 w-4" />
				</Button>
			);
		},
		cell: ({ row }) => {
			const question = row.original;
			return (
				<div className="flex flex-col max-w-xl">
					<div className="font-medium">{row.getValue("questionText")}</div>
					<div className="text-sm text-muted-foreground">
						<Link
							href={`/behavioral-indicators/${question.behavioralIndicatorId}`}
							className="text-primary hover:underline"
						>
							{question.behavioralIndicatorId}
						</Link>
						{" → "}
						<span>{question.behavioralIndicatorId}</span>
					</div>
				</div>
			);
		},
	},
	{
		accessorKey: "questionType",
		header: ({ column }) => (
			<Button
				variant="ghost"
				onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
			>
				Type
				<ArrowUpDown className="ml-2 h-4 w-4" />
			</Button>
		),
		cell: ({ row }) => {
			const type = row.getValue("questionType") as string;
			return (
				<div className="flex items-center gap-2">
					<span>{questionTypeToIcon(type)}</span>
					<span className="font-medium">
						{type
							.split("_")
							.map((word) => word.charAt(0) + word.slice(1).toLowerCase())
							.join(" ")}
					</span>
				</div>
			);
		},
	},
	{
		accessorKey: "difficulty",
		header: ({ column }) => (
			<Button
				variant="ghost"
				onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
			>
				Difficulty
				<ArrowUpDown className="ml-2 h-4 w-4" />
			</Button>
		),
		cell: ({ row }) => {
			const difficulty = row.getValue("difficulty") as string;
			return (
				<Badge variant="outline" className={questionDifficultyToColor(difficulty)}>
					{difficulty}
				</Badge>
			);
		},
	},
	{
		accessorKey: "points",
		header: ({ column }) => (
			<Button
				variant="ghost"
				onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
			>
				Points
				<ArrowUpDown className="ml-2 h-4 w-4" />
			</Button>
		),
		cell: ({ row }) => {
			const points = row.getValue("points") as number;
			return (
				<div className="flex items-center gap-2">
					<Sparkles className="h-4 w-4 text-muted-foreground" />
					<span className="font-medium">{points}</span>
				</div>
			);
		},
	},
	{
		accessorKey: "timeEstimate",
		header: ({ column }) => (
			<Button
				variant="ghost"
				onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
			>
				Time
				<ArrowUpDown className="ml-2 h-4 w-4" />
			</Button>
		),
		cell: ({ row }) => {
			const time = row.getValue("timeEstimate") as number;
			return (
				<div className="flex items-center gap-2">
					<Clock className="h-4 w-4 text-muted-foreground" />
					<span className="font-medium">{Math.round(time / 60)} min</span>
				</div>
			);
		},
	},
	{
		accessorKey: "isActive",
		header: "Status",
		cell: ({ row }) => {
			const isActive = row.getValue("isActive") as boolean;
			return (
				<Badge variant={isActive ? "default" : "secondary"}>
					{isActive ? "Active" : "Inactive"}
				</Badge>
			);
		},
	},
	{
		id: "actions",
		cell: ({ row }) => {
			const question = row.original;
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
							onClick={() => navigator.clipboard.writeText(question.id)}
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
							Edit Question
						</DropdownMenuItem>
					</DropdownMenuContent>
				</DropdownMenu>
			);
		},
	},
];

// Stats component
const AssessmentStats: React.FC<{ questions: AssessmentQuestion[] }> = ({
	questions,
}) => {
	const stats = React.useMemo(
		() => ({
			totalQuestions: questions.length,
			questionTypes: new Set(questions.map((q) => q.questionType)).size,
			totalMinutes: Math.round(
				questions.reduce((sum, q) => sum + q.timeLimit, 0) / 60,
			),
		}),
		[questions],
	);

	return (
		<div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
			<Card className="p-4 flex items-center space-x-4">
				<div className="p-3 bg-primary/10 rounded-lg">
					<BarChart3 className="h-5 w-5 text-primary" />
				</div>
				<div>
					<div className="text-2xl font-bold">{stats.totalQuestions}</div>
					<div className="text-xs text-muted-foreground">Total Questions</div>
				</div>
			</Card>
			<Card className="p-4 flex items-center space-x-4">
				<div className="p-3 bg-primary/10 rounded-lg">
					<ListFilter className="h-5 w-5 text-primary" />
				</div>
				<div>
					<div className="text-2xl font-bold">{stats.questionTypes}</div>
					<div className="text-xs text-muted-foreground">Question Types</div>
				</div>
			</Card>
			<Card className="p-4 flex items-center space-x-4">
				<div className="p-3 bg-primary/10 rounded-lg">
					<Clock className="h-5 w-5 text-primary" />
				</div>
				<div>
					<div className="text-2xl font-bold">{stats.totalMinutes}</div>
					<div className="text-xs text-muted-foreground">Est. Minutes</div>
				</div>
			</Card>
		</div>
	);
};

// Main component
export default function AssessmentQuestionsPage({}) {
	const [questions, setQuestions] = useState<AssessmentQuestion[]>([]);
	const [loading, setLoading] = useState<boolean>(true);
	const [sorting, setSorting] = useState<SortingState>([]);
	const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
	const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
	const [rowSelection, setRowSelection] = useState({});

	useEffect(() => {
		const fetchQuestions = async () => {
			try {
				setLoading(true);
				const data : AssessmentQuestion[] | null =  await assessmentQuestionsApi.getAllQuestions();
				if(!data){
					throw new Error("No data received for questions");
				}
				setQuestions(data);
			} catch (error) {
				console.error("Failed to fetch assessment questions:", error);
			} finally {
				setLoading(false);
			}
		};

		fetchQuestions();
	}, []);

	// Initialize table
	const table = useReactTable({
		data: questions,
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

	if (loading) {
		return (
			<div className="min-h-screen bg-background flex items-center justify-center">
				<div className="text-center">
					<div className="animate-spin rounded-full h-12 w-12 border-b-2 border-purple-500 mx-auto mb-4"></div>
					<p className="text-muted-foreground">
						Loading assessment questions...
					</p>
				</div>
			</div>
		);
	}

	return (
		<div className="container mx-auto px-4 py-8 space-y-8 w-full max-w-none">
			{/* Header */}
			<div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
				<div>
					<h1 className="text-3xl font-bold tracking-tight">
						Assessment Questions
					</h1>
					<p className="text-muted-foreground">
						Manage and organize assessment questions for competency evaluation
					</p>
				</div>
				<div className="flex items-center gap-2">
					<Button variant="outline">
						<Download className="mr-2 h-4 w-4" />
						Export
					</Button>
					<Button>
						<Plus className="mr-2 h-4 w-4" />
						New Question
					</Button>
				</div>
			</div>

			{/* Stats Cards */}
			<AssessmentStats questions={questions} />

			{/* Table Controls */}
			<div className="flex flex-1 items-center space-x-2">
				<div className="flex flex-1 items-center space-x-2">
					<div className="relative flex-1 max-w-sm">
						<Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
						<Input
							placeholder="Search questions..."
							value={
								(table.getColumn("questionText")?.getFilterValue() as string) ??
								""
							}
							onChange={(event) =>
								table
									.getColumn("questionText")
									?.setFilterValue(event.target.value)
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

			{/* Table */}
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
									No questions found.
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
		</div>
	);
};
