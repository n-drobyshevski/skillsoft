"use client";
import React, { useState, useEffect } from "react";
import { Competency } from "../interfaces/domain-interfaces";
import CompetencyCard  from "./CompetencyCard";
import { levelToNumber } from "../utils";
import { Skeleton } from "@/components/ui/skeleton";
import {Card, CardHeader, CardContent, CardFooter} from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import {Button} from "@/components/ui/button";
import {Select, SelectContent, SelectItem, SelectTrigger, SelectValue} from "@/components/ui/select";
import {
  ChevronDown,
  Search,
  Filter,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
} from "lucide-react";

// Card Grid Component
export default function CompetencyCardGrid({
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
    const filtered = data.filter((comp: Competency) => {
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
