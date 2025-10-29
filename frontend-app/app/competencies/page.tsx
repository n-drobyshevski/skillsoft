"use client";

import React, { useState, useEffect } from "react";
import Link from "next/link";
import { type ColumnDef } from "@tanstack/react-table";

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
import {
  CompetencyCategory,
  ProficiencyLevel,
  ApprovalStatus,
} from "../enums/domain_enums";
import {
  Competency,
  BehavioralIndicator,
} from "../interfaces/domain-interfaces";
import {
  ArrowUpDown,
  Eye,
  Search,
  Filter,
  MoreHorizontal,
  Activity,
  Calendar,
  Settings2,
  Download,
  Plus,
  Target,
  Clock,
} from "lucide-react";
import { competenciesApi } from "@/services/api";
import CompetencyStats from "./components/CompetencyStats";
import {
  approvalStatusToColor,
  competencyCategoryToIcon,
  competencyProficiencyLevelToColor,
} from "../utils";
import Header from "../components/Header";
import EntitiesTable from "../components/Table";
import CompetencyDrawer from "./components/CompetencyDrawer";

import { useHeader } from "@/context/HeaderContext";

// Main component
export default function CompetenciesPage() {
  const { setTitle, setSubtitle, setEntityName } = useHeader();

  useEffect(() => {
    setTitle("Competencies");
    setSubtitle("Manage and explore competencies");
    setEntityName("Competency");
  }, [setTitle, setSubtitle, setEntityName]);
  const [competencies, setCompetencies] = useState<Competency[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [isDrawerOpen, setIsDrawerOpen] = useState(false);
  const [selectedCompetency, setSelectedCompetency] =
    useState<Competency | null>(null);

  const handleViewDetails = (competency: Competency) => {
    setSelectedCompetency(competency);
    setIsDrawerOpen(true);
  };

  // Column definitions
  const columns: ColumnDef<Competency>[] = [
    {
      accessorKey: "name",
      header: ({ column }) => {
        return (
          <div className="text-left">
            <Button
              variant="ghost"
              onClick={() =>
                column.toggleSorting(column.getIsSorted() === "asc")
              }
            >
              Competency
              <ArrowUpDown className="ml-2 h-4 w-4" />
            </Button>
          </div>
        );
      },
      cell: ({ row }) => {
        const category = row.original.category;
        return (
          <div className="flex items-start gap-3">
            <div className="shrink-0 w-8 h-8 bg-primary/10 rounded-lg flex items-center justify-center">
              <span className="text-base">
                {competencyCategoryToIcon(category)}
              </span>
            </div>
            <div className="flex-1 min-w-0">
              <Link
                href={`/competencies/${row.original.id}`}
                className="font-medium text-primary hover:underline block truncate"
              >
                {row.getValue("name")}
              </Link>
              <p className="max-w-xs text-xs text-muted-foreground truncate mt-0.5">
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
          <Badge
            variant="outline"
            className={competencyProficiencyLevelToColor(level)}
          >
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
          "behavioralIndicators"
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
        const isActive = row.original.isActive;
        return (
          <div className="flex items-center gap-2">
            <Badge variant={isActive ? "default" : "secondary"}>
              {isActive ? "Active" : "Inactive"}
            </Badge>
          </div>
        );
      },
    },
    {
      accessorKey: "ApprovalStatus",
      header: "Approval Status",
      cell: ({ row }) => {
        const status = row.original.approvalStatus;
        return (
          <div className="flex items-center gap-2">
            <Badge variant="outline" className={approvalStatusToColor(status)}>
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
              <DropdownMenuItem onClick={() => handleViewDetails(competency)}>
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

  useEffect(() => {
    const fetchCompetencies = async () => {
      try {
        setLoading(true);
        const data: Competency[] | null =
          await competenciesApi.getAllCompetencies();
        if (!data) {
          throw new Error("No competencies found");
        }
        setCompetencies(data);
      } catch (error) {
        console.error("Failed to fetch competencies:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchCompetencies();
  }, []);

  return (
    <div className="container mx-auto py-8 space-y-8 w-full">
      {/* Header */}

      {/* Stats Cards */}
      <CompetencyStats competencies={competencies} />

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
        <EntitiesTable
          columns={columns}
          data={competencies}
          onRowClick={handleViewDetails}
        />
      )}

      {selectedCompetency && (
        <CompetencyDrawer
          open={isDrawerOpen}
          onOpenChange={setIsDrawerOpen}
          competency={selectedCompetency}
        />
      )}
    </div>
  );
}
