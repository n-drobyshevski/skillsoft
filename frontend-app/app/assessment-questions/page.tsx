"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import {
	Clock,
	MoreHorizontal,
	Eye,
	Settings2,
	Download,
	Plus,
	ArrowUpDown,
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
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuLabel,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "../../src/components/ui/dropdown-menu";
import { Button } from "../../src/components/ui/button";
import { Badge } from "../../src/components/ui/badge";

import { AssessmentQuestion } from "../interfaces/domain-interfaces";
import { questionTypeToIcon, questionDifficultyToColor } from "../utils";
import { assessmentQuestionsApi } from "@/services/api";
import StatsCard from "../components/StatsCard";
import EntitiesTable from "../components/Table";
import Header from "../components/Header";
import AssessmentQuestionDrawer from "./components/AssessmentQuestionDrawer";

// Stats component
const AssessmentStats: React.FC<{ questions: AssessmentQuestion[] }> = ({
	questions,
}) => {
	const stats = React.useMemo(
		() => ({
			totalQuestions: questions.length,
			questionTypes: new Set(questions.map((q) => q.questionType)).size,
			totalMinutes: Math.round(
				questions.reduce((sum, q) => sum + (q.timeLimit || 0), 0) / 60,
			),
		}),
		[questions],
	);

	return (
    <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-8">
      <StatsCard
        title="Total Questions"
        value={stats.totalQuestions}
        icon={BarChart3}
      />
      <StatsCard
        title="Question Types"
        value={stats.questionTypes}
        icon={ListFilter}
      />
      <StatsCard
        title="Est. Minutes"
        value={stats.totalMinutes}
        icon={Clock}
      />
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
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedQuestion, setSelectedQuestion] = useState<AssessmentQuestion | null>(null);

  const handleViewDetails = (question: AssessmentQuestion) => {
    setSelectedQuestion(question);
    setIsDrawerOpen(true);
  };

  // Column definitions
  const columns: ColumnDef<AssessmentQuestion>[] = [
    {
      accessorKey: "questionText",
      header: ({ column }) => {
        return (
          <div className="text-left">
            <Button
              variant="ghost"
              onClick={() => column.toggleSorting(column.getIsSorted() === "asc")}
            >
              Question
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </Button>
          </div>
        );
      },
      cell: ({ row }) => {
        const question = row.original;
        return (
          <div className="flex flex-col max-w-lg overflow-hidden text-ellipsis">
            <div className="font-medium ">{row.getValue("questionText")}</div>
            <div className="text-sm text-muted-foreground">
              <Link
                href={`/behavioral-indicators/${question.behavioralIndicatorId}`}
                className="text-primary hover:underline"
              >
                {question.behavioralIndicatorId}
              </Link>
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
        const question = row.original;
        return (
          <div className="flex items-center gap-2">
            <span>{questionTypeToIcon(question.questionType)}</span>
            <span className="font-medium">
              {question.questionType
                .split("_")
                .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
                .join(" ")}
            </span>
          </div>
        );
      },
    },
    {
      accessorKey: "difficultyLevel",
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
        const question = row.original;
        return (
          <Badge variant="outline" className={questionDifficultyToColor(question.difficultyLevel)}>
            {question.difficultyLevel}
          </Badge>
        );
      },
    },
    {
      accessorKey: "timeLimit",
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
        const question = row.original;
        const time = question.timeLimit;
        if (!time || time <= 0) {
          return <span className="text-muted-foreground">No limit</span>;
        }
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
              <DropdownMenuItem onClick={() => handleViewDetails(question)}>
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
      <Header
        title="Assessment Questions"
        subtitle="Manage and organize assessment questions for competency evaluation"
        entityName="Question"
      />
      

      {/* Stats Cards */}
      <AssessmentStats questions={questions} />

      <EntitiesTable data={questions} columns={columns} onRowClick={handleViewDetails} />

      {selectedQuestion && (
        <AssessmentQuestionDrawer
          open={isDrawerOpen}
          onOpenChange={setIsDrawerOpen}
          question={selectedQuestion}
        />
      )}
    </div>
  );
};