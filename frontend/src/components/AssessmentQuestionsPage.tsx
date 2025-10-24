import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
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
  ListTodo ,
  Gavel,
  ChartColumn,
  Pencil ,
  BadgeQuestionMark,
  BarChart3,
  ListFilter
} from 'lucide-react';
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
} from "@/components/ui/table";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import { Card } from "@/components/ui/card";


interface AssessmentQuestion {
  id: string;
  questionText: string;
  questionType: string;
  category: string;
  difficulty: string;
  points: number;
  timeEstimate: number;
  isActive: boolean;
  behavioralIndicator: {
    id: string;
    title: string;
    competency: {
      id: string;
      name: string;
      category: string;
    };
  };
  answerOptions?: {
    id: string;
    optionText: string;
    isCorrect: boolean;
    points: number;
  }[];
}

const typeToIcon = (category: string) => {
  const icons = {
    'MULTIPLE_CHOICE': ListTodo,
    'SITUATIONAL_JUDGMENT': Gavel,
    'LIKERT_SCALE': ChartColumn,
    'OPEN_ENDED': Pencil,
    'TRUE_FALSE': BadgeQuestionMark,
  };
  const IconComponent =
    icons[category as keyof typeof icons] || BadgeQuestionMark;
  return React.createElement(IconComponent, { className: "h-4 w-4" });
};
const typeToIcon2 = (type: string): string => {
  const icons: { [key: string]: string } = {
    'MULTIPLE_CHOICE': '☑️',
    'SITUATIONAL_JUDGMENT': '🎭',
    'LIKERT_SCALE': '📊',
    'OPEN_ENDED': '✏️',
    'TRUE_FALSE': '❓'
  };
  return icons[type] || '❓';
};

const difficultyToColor = (difficulty: string): string => {
  const colors: { [key: string]: string } = {
    'BEGINNER': 'border-emerald-500/20 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30',
    'INTERMEDIATE': 'border-amber-500/20 text-amber-700 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30',
    'ADVANCED': 'border-red-500/20 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30'
  };
  return colors[difficulty] || colors['BEGINNER'];
};

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
      const indicator = row.original.behavioralIndicator;
      return (
        <div className="flex flex-col max-w-xl">
          <div className="font-medium">{row.getValue("questionText")}</div>
          <div className="text-sm text-muted-foreground">
            <Link
              to={`/competency/${indicator.competency.id}`}
              className="text-primary hover:underline"
            >
              {indicator.competency.name}
            </Link>
            {' → '}
            <span>{indicator.title}</span>
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
          <span>{typeToIcon(type)}</span>
          <span className="font-medium">
            {type.split('_').map(word => word.charAt(0) + word.slice(1).toLowerCase()).join(' ')}
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
        <Badge variant="outline" className={difficultyToColor(difficulty)}>
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
const AssessmentStats: React.FC<{ questions: AssessmentQuestion[] }> = ({ questions }) => {
  const stats = React.useMemo(() => ({
    totalQuestions: questions.length,
    questionTypes: new Set(questions.map(q => q.questionType)).size,
    totalPoints: questions.reduce((sum, q) => sum + q.points, 0),
    totalMinutes: Math.round(questions.reduce((sum, q) => sum + q.timeEstimate, 0) / 60)
  }), [questions]);

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
          <Sparkles className="h-5 w-5 text-primary" />
        </div>
        <div>
          <div className="text-2xl font-bold">{stats.totalPoints}</div>
          <div className="text-xs text-muted-foreground">Total Points</div>
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
const AssessmentQuestionsPageNew: React.FC = () => {
  const [questions, setQuestions] = useState<AssessmentQuestion[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [sorting, setSorting] = useState<SortingState>([]);
  const [columnFilters, setColumnFilters] = useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = useState<VisibilityState>({});
  const [rowSelection, setRowSelection] = useState({});

  useEffect(() => {
    // Simulate API call - replace with actual endpoint
    const mockQuestions: AssessmentQuestion[] = [
      {
        id: '1',
        questionText: 'When faced with a complex problem, what is your first step?',
        questionType: 'MULTIPLE_CHOICE',
        category: 'PROBLEM_SOLVING',
        difficulty: 'INTERMEDIATE',
        points: 10,
        timeEstimate: 120,
        isActive: true,
        behavioralIndicator: {
          id: '1',
          title: 'Problem Analysis',
          competency: {
            id: '1',
            name: 'Critical Thinking',
            category: 'COGNITIVE'
          }
        },
        answerOptions: [
          { id: '1a', optionText: 'Break down the problem into smaller parts', isCorrect: true, points: 10 },
          { id: '1b', optionText: 'Immediately start implementing solutions', isCorrect: false, points: 0 },
          { id: '1c', optionText: 'Ask someone else to solve it', isCorrect: false, points: 0 },
          { id: '1d', optionText: 'Ignore it and hope it goes away', isCorrect: false, points: 0 }
        ]
      },
      {
        id: '2',
        questionText: 'Describe a situation where you had to adapt your communication style.',
        questionType: 'SITUATIONAL_JUDGMENT',
        category: 'COMMUNICATION',
        difficulty: 'ADVANCED',
        points: 15,
        timeEstimate: 300,
        isActive: true,
        behavioralIndicator: {
          id: '2',
          title: 'Active Listening',
          competency: {
            id: '2',
            name: 'Communication',
            category: 'INTERPERSONAL'
          }
        }
      }
    ];
    
    setTimeout(() => {
      setQuestions(mockQuestions);
      setLoading(false);
    }, 1000);
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

  return (
    <div className="container mx-auto px-4 py-8 space-y-8 w-full max-w-none">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">Assessment Questions</h1>
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
              value={(table.getColumn("questionText")?.getFilterValue() as string) ?? ""}
              onChange={(event) =>
                table.getColumn("questionText")?.setFilterValue(event.target.value)
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
                            header.getContext()
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
                      {flexRender(cell.column.columnDef.cell, cell.getContext())}
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

export default AssessmentQuestionsPageNew;