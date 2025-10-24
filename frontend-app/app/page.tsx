"use client"
import React, { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  CardFooter,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Progress } from "@/components/ui/progress";
import { Skeleton } from "@/components/ui/skeleton";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Search,
  Filter,
  MoreHorizontal,
  Award,
  BarChart3,
  Tag,
  TrendingUp,
  Activity,
  Calendar,
  Settings2,
  Download,
  Plus,
  ChevronDown,
  Eye,
  ArrowUpDown,
  ChevronLeft,
  ChevronRight,
  CheckCircle2,
  Layers,
  GraduationCap,
  BookOpen,
  Users,
  BrainCircuit,
  Heart,
  MessageSquare,
  UsersRound,
  LightbulbIcon,
  Clock,
  CircleDot,
  CircleSlash,
  CircleCheckBig,
  Binary,
  Medal,
  Trophy,
  Target,
} from "lucide-react";
import Link from "next/link";
// Types
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

interface Competency {
  id: string;
  name: string;
  description: string;
  category: string;
  level: string;
  isActive: boolean;
  approvalStatus: string;
  version: number;
  createdAt: string;
  lastModified: string;
  behavioralIndicators?: BehavioralIndicator[];
}

interface DashboardStats {
  totalCompetencies: number;
  totalBehavioralIndicators: number;
  totalAssessmentQuestions: number;
  competenciesByCategory: { [key: string]: number };
  competenciesByLevel: { [key: string]: number };
  averageIndicatorsPerCompetency: number;
}

// Helper functions
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
const levelToNumber = (level: string): number => {
  const levels: { [key: string]: number } = {
    NOVICE: 1,
    DEVELOPING: 2,
    PROFICIENT: 3,
    ADVANCED: 4,
    EXPERT: 5,
  };
  return levels[level] || 1;
};

const levelToColor = (level: string): string => {
  const colors: { [key: string]: string } = {
    NOVICE:
      "border-red-600/30 text-red-700 bg-red-50/90 dark:bg-red-950/90 dark:text-red-200 dark:border-red-400/30",
    DEVELOPING:
      "border-amber-600/30 text-amber-800 bg-amber-50/90 dark:bg-amber-950/90 dark:text-amber-200 dark:border-amber-400/30",
    PROFICIENT:
      "border-emerald-600/30 text-emerald-700 bg-emerald-50/90 dark:bg-emerald-950/90 dark:text-emerald-200 dark:border-emerald-400/30",
    ADVANCED:
      "border-blue-600/30 text-blue-700 bg-blue-50/90 dark:bg-blue-950/90 dark:text-blue-200 dark:border-blue-400/30",
    EXPERT:
      "border-violet-600/30 text-violet-700 bg-violet-50/90 dark:bg-violet-950/90 dark:text-violet-200 dark:border-violet-400/30",
  };
  return colors[level] || colors["NOVICE"];
};

// Competency Card Component
function CompetencyCard({ competency }: { competency: Competency }) {
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
              competency.level
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
                href={`/competency/${competency.id}`}
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
                    href={`/competency/${competency.id}`}
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
                href={`/competency/${competency.id}`}
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

// Card Grid Component
function CompetencyCardGrid({
  data,
  loading,
}: {
  data: Competency[];
  loading: boolean;
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedCategory, setSelectedCategory] = useState("all");
  const [sortBy, setSortBy] = useState<
    "name" | "level" | "indicators" | "modified"
  >("name");
  const [sortOrder, setSortOrder] = useState<"asc" | "desc">("asc");
  const [currentPage, setCurrentPage] = useState(1);
  const itemsPerPage = 9;

  // Filter and sort competencies
  const filteredAndSortedCompetencies = React.useMemo(() => {
    let filtered = data.filter((comp: Competency) => {
      const matchesCategory =
        selectedCategory === "all" || comp.category === selectedCategory;
      const matchesSearch =
        searchTerm === "" ||
        comp.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
        comp.description?.toLowerCase().includes(searchTerm.toLowerCase());
      return matchesCategory && matchesSearch;
    });

    return filtered.sort((a: Competency, b: Competency) => {
      let comparison = 0;
      switch (sortBy) {
        case "name":
          comparison = a.name.localeCompare(b.name);
          break;
        case "level":
          comparison = levelToNumber(a.level) - levelToNumber(b.level);
          break;
        case "indicators":
          comparison =
            (a.behavioralIndicators?.length || 0) -
            (b.behavioralIndicators?.length || 0);
          break;
        case "modified":
          comparison =
            new Date(a.lastModified).getTime() -
            new Date(b.lastModified).getTime();
          break;
      }
      return sortOrder === "asc" ? comparison : -comparison;
    });
  }, [data, selectedCategory, searchTerm, sortBy, sortOrder]);

  // Calculate pagination
  const totalPages = Math.ceil(
    filteredAndSortedCompetencies.length / itemsPerPage
  );
  const currentItems = filteredAndSortedCompetencies.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  );

  // Loading state
  if (loading) {
    return (
      <div className="space-y-6">
        <div className="flex flex-wrap justify-between gap-4">
          <Skeleton className="h-10 w-[300px]" />
          <div className="flex space-x-2">
            <Skeleton className="h-10 w-[120px]" />
            <Skeleton className="h-10 w-[120px]" />
          </div>
        </div>
        <div className="grid grid-cols-1 gap-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <Card key={i} className="relative overflow-hidden">
              <CardHeader className="flex flex-row items-start space-y-0 pb-2">
                <div className="flex space-x-4 w-full">
                  <Skeleton className="h-10 w-10 rounded-lg" />
                  <div className="space-y-2 flex-1">
                    <Skeleton className="h-6 w-3/4" />
                    <Skeleton className="h-4 w-20" />
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                <div className="space-y-2">
                  <Skeleton className="h-4 w-full" />
                  <Skeleton className="h-4 w-5/6" />
                </div>
                <div className="mt-4 space-y-2">
                  <Skeleton className="h-4 w-full" />
                </div>
              </CardContent>
              <CardFooter>
                <Skeleton className="h-9 w-full" />
              </CardFooter>
            </Card>
          ))}
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Controls */}
      <div className="flex flex-wrap justify-between gap-4">
        <div className="flex flex-wrap items-center gap-4">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search competencies..."
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setCurrentPage(1);
              }}
              className="pl-9 w-[300px]"
            />
          </div>
          <Select
            value={selectedCategory}
            onValueChange={(value) => {
              setSelectedCategory(value);
              setCurrentPage(1);
            }}
          >
            <SelectTrigger className="w-[180px]">
              <Filter className="mr-2 h-4 w-4" />
              <SelectValue placeholder="Category" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">All Categories</SelectItem>
              <SelectItem value="COGNITIVE">Cognitive</SelectItem>
              <SelectItem value="INTERPERSONAL">Interpersonal</SelectItem>
              <SelectItem value="LEADERSHIP">Leadership</SelectItem>
              <SelectItem value="ADAPTABILITY">Adaptability</SelectItem>
              <SelectItem value="EMOTIONAL_INTELLIGENCE">
                Emotional Intelligence
              </SelectItem>
              <SelectItem value="COMMUNICATION">Communication</SelectItem>
              <SelectItem value="COLLABORATION">Collaboration</SelectItem>
              <SelectItem value="CRITICAL_THINKING">
                Critical Thinking
              </SelectItem>
              <SelectItem value="TIME_MANAGEMENT">Time Management</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="flex items-center gap-2">
          <Select
            value={sortBy}
            onValueChange={(value: typeof sortBy) => setSortBy(value)}
          >
            <SelectTrigger className="w-[160px]">
              <ArrowUpDown className="mr-2 h-4 w-4" />
              <SelectValue placeholder="Sort by" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="name">Sort by Name</SelectItem>
              <SelectItem value="level">Sort by Level</SelectItem>
              <SelectItem value="indicators">Sort by Indicators</SelectItem>
              <SelectItem value="modified">Sort by Modified</SelectItem>
            </SelectContent>
          </Select>
          <Button
            variant="outline"
            size="icon"
            onClick={() =>
              setSortOrder((order) => (order === "asc" ? "desc" : "asc"))
            }
          >
            <ChevronDown
              className={`h-4 w-4 transition-transform ${
                sortOrder === "desc" ? "rotate-180" : ""
              }`}
            />
          </Button>
        </div>
      </div>

      {/* Cards Grid */}
      {currentItems.length > 0 ? (
        <div className="grid grid-cols-1 gap-4">
          {currentItems.map((competency: Competency) => (
            <CompetencyCard key={competency.id} competency={competency} />
          ))}
        </div>
      ) : (
        <Card className="p-12 text-center">
          <CardContent>
            <Search className="mx-auto h-12 w-12 text-muted-foreground/50" />
            <h3 className="mt-4 text-lg font-semibold">
              No competencies found
            </h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {searchTerm
                ? `No competencies match your search for "${searchTerm}"`
                : selectedCategory !== "all"
                ? `No competencies found in ${selectedCategory.replace(
                    "_",
                    " "
                  )} category`
                : "Try adjusting your search terms or filters"}
            </p>
            <div className="mt-6 flex gap-2 justify-center">
              {searchTerm && (
                <Button onClick={() => setSearchTerm("")} variant="default">
                  Clear Search
                </Button>
              )}
              {selectedCategory !== "all" && (
                <Button
                  onClick={() => setSelectedCategory("all")}
                  variant="outline"
                >
                  Show All Categories
                </Button>
              )}
            </div>
          </CardContent>
        </Card>
      )}

      {/* Pagination */}
      {filteredAndSortedCompetencies.length > itemsPerPage && (
        <div className="flex items-center justify-between pt-4">
          <p className="text-sm text-muted-foreground">
            Showing {(currentPage - 1) * itemsPerPage + 1} to{" "}
            {Math.min(
              currentPage * itemsPerPage,
              filteredAndSortedCompetencies.length
            )}{" "}
            of {filteredAndSortedCompetencies.length} competencies
          </p>
          <div className="flex items-center space-x-2">
            <Button
              variant="outline"
              size="icon"
              onClick={() => setCurrentPage(1)}
              disabled={currentPage === 1}
            >
              <ChevronLeft className="h-4 w-4 -rotate-180" />
              <ChevronLeft className="h-4 w-4 -rotate-180 -ml-2" />
              <span className="sr-only">First page</span>
            </Button>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
              disabled={currentPage === 1}
            >
              <ChevronLeft className="h-4 w-4" />
              <span className="sr-only">Previous page</span>
            </Button>
            <div className="flex items-center justify-center text-sm font-medium min-w-[100px]">
              Page {currentPage} of {totalPages}
            </div>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
              disabled={currentPage === totalPages}
            >
              <ChevronRight className="h-4 w-4" />
              <span className="sr-only">Next page</span>
            </Button>
            <Button
              variant="outline"
              size="icon"
              onClick={() => setCurrentPage(totalPages)}
              disabled={currentPage === totalPages}
            >
              <ChevronRight className="h-4 w-4 rotate-180" />
              <ChevronRight className="h-4 w-4 rotate-180 -ml-2" />
              <span className="sr-only">Last page</span>
            </Button>
          </div>
        </div>
      )}
    </div>
  );
}

// Main Dashboard Component
export default function Page() {
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  // Fetch competencies and stats
  const fetchData = async () => {
    try {
      setLoading(true);
      const competenciesResponse = await fetch(
        "http://localhost:8080/api/competencies",
        {
          headers: {
            "Content-Type": "application/json",
            Accept: "application/json",
          },
        }
      );

      if (!competenciesResponse.ok) {
        throw new Error(`HTTP error! status: ${competenciesResponse.status}`);
      }

      const competenciesData: Competency[] = await competenciesResponse.json();
      setCompetencies(competenciesData);

      // Calculate stats
      const stats: DashboardStats = {
        totalCompetencies: competenciesData.length,
        totalBehavioralIndicators: competenciesData.reduce(
          (sum, comp) => sum + (comp.behavioralIndicators?.length || 0),
          0
        ),
        totalAssessmentQuestions: competenciesData.length * 8, // Estimate
        competenciesByCategory: {},
        competenciesByLevel: {},
        averageIndicatorsPerCompetency: 0,
      };

      // Calculate distributions
      competenciesData.forEach((comp) => {
        stats.competenciesByCategory[comp.category] =
          (stats.competenciesByCategory[comp.category] || 0) + 1;
        stats.competenciesByLevel[comp.level] =
          (stats.competenciesByLevel[comp.level] || 0) + 1;
      });

      stats.averageIndicatorsPerCompetency =
        stats.totalBehavioralIndicators / stats.totalCompetencies;
      setStats(stats);
    } catch (error) {
      console.error("Failed to fetch data:", error);
      setError("Failed to load competencies. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  if (error) {
    return (
      <div className="container mx-auto px-4 py-8 flex items-center justify-center min-h-[60vh]">
        <Card className="max-w-md border-destructive/50">
          <CardContent className="p-8 text-center">
            <div className="text-4xl mb-4">⚠️</div>
            <CardTitle className="text-destructive mb-2">
              Error Loading Dashboard
            </CardTitle>
            <CardDescription className="mb-6">{error}</CardDescription>
            <Button
              onClick={fetchData}
              variant="destructive"
              className="w-full"
            >
              Try Again
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto px-4 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-4 lg:space-y-0">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">
            Competency Management
          </h1>
          <p className="text-muted-foreground">
            Manage and track your organization's competency framework
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button variant="outline" size="sm">
            <Download className="mr-2 h-4 w-4" />
            Export All
          </Button>
          <Button size="sm">
            <Plus className="mr-2 h-4 w-4" />
            New Competency
          </Button>
        </div>
      </div>

      {/* Statistics Overview */}
      {stats && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Card className="group hover:shadow-lg transition-all">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="p-3 rounded-xl bg-primary/10 text-primary">
                  <Award className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="space-y-1.5">
                  <p className="text-sm font-medium text-muted-foreground">
                    Total Competencies
                  </p>
                  <p className="text-2xl font-bold tracking-tight">
                    {stats.totalCompetencies}
                  </p>
                  <div className="flex items-center text-xs text-muted-foreground">
                    <TrendingUp className="mr-1 h-3 w-3" aria-hidden="true" />
                    <span>+12% from last month</span>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="p-3 rounded-xl bg-blue-500/10 text-blue-600 dark:text-blue-400">
                  <BarChart3 className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="space-y-1.5">
                  <p className="text-sm font-medium text-muted-foreground">
                    Behavioral Indicators
                  </p>
                  <p className="text-2xl font-bold tracking-tight">
                    {stats.totalBehavioralIndicators}
                  </p>
                  <p className="text-xs text-muted-foreground">
                    ~{Math.round(stats.averageIndicatorsPerCompetency)} per
                    competency
                  </p>
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="p-3 rounded-xl bg-yellow-500/10 text-yellow-600 dark:text-yellow-400">
                  <CheckCircle2 className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="space-y-1.5">
                  <p className="text-sm font-medium text-muted-foreground">
                    Assessment Questions
                  </p>
                  <p className="text-2xl font-bold tracking-tight">
                    {stats.totalAssessmentQuestions}
                  </p>
                  <Progress
                    value={65}
                    className="h-1 w-20"
                    aria-label="65% completion rate"
                  />
                </div>
              </div>
            </CardContent>
          </Card>

          <Card className="group hover:shadow-lg transition-all">
            <CardContent className="p-6">
              <div className="flex items-start gap-4">
                <div className="p-3 rounded-xl bg-green-500/10 text-green-600 dark:text-green-400">
                  <Layers className="h-6 w-6" aria-hidden="true" />
                </div>
                <div className="space-y-1.5">
                  <p className="text-sm font-medium text-muted-foreground">
                    Categories
                  </p>
                  <p className="text-2xl font-bold tracking-tight">
                    {Object.keys(stats.competenciesByCategory).length}
                  </p>
                  <div className="flex -space-x-1.5">
                    {Object.keys(stats.competenciesByCategory)
                      .slice(0, 4)
                      .map((category, i) => (
                        <div
                          key={category}
                          className="h-6 w-6 rounded-full bg-muted flex items-center justify-center text-xs ring-2 ring-background"
                          title={category.toLowerCase().replace("_", " ")}
                        >
                          {React.cloneElement(categoryToIcon(category), {
                            className: "h-3 w-3",
                          })}
                        </div>
                      ))}
                    {Object.keys(stats.competenciesByCategory).length > 4 && (
                      <div
                        className="h-6 w-6 rounded-full bg-muted flex items-center justify-center text-xs font-medium ring-2 ring-background"
                        title="More categories"
                      >
                        +{Object.keys(stats.competenciesByCategory).length - 4}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </div>
      )}

      {/* Competency Cards */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Activity className="h-5 w-5" />
            Competencies Overview
          </CardTitle>
          <CardDescription>
            Browse, search, and manage all competencies in your system
          </CardDescription>
        </CardHeader>
        <CardContent>
          <CompetencyCardGrid data={competencies} loading={loading} />
        </CardContent>
      </Card>
    </div>
  );
};
