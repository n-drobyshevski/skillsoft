"use client";
import { Button } from "@/components/ui/button";
import { Download, Plus } from "lucide-react";
import React from "react";



export default function Header({ title, subtitle , entityName}: { title: string; subtitle?: string; entityName?: string }) {
    return <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">{title}</h1>
          <p className="text-muted-foreground">
            {subtitle}
          </p>
        </div>
        {entityName && (
        <div className="flex items-center gap-2">
          <Button variant="outline">
            <Download className="mr-2 h-4 w-4" />
            Export
          </Button>
          <Button>
            <Plus className="mr-2 h-4 w-4" />
            New {entityName}
          </Button>
        </div>)}
      </div>;
}