"use client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, Upload, Zap } from "lucide-react";

export default function QuickActionsCard() {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="flex items-center gap-2">
          <Zap className="w-5 h-5" />
          Quick Actions
        </CardTitle>
      </CardHeader>
      <CardContent className="space-y-2">
        <Button className="w-full justify-start">
          <Plus className="mr-2 h-4 w-4" />
          Add New Competency
        </Button>
        <Button variant="secondary" className="w-full justify-start">
          <Upload className="mr-2 h-4 w-4" />
          Import Competencies
        </Button>
      </CardContent>
    </Card>
  );
}
