
'use client';

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { BehavioralIndicator } from "../../interfaces/domain-interfaces";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";

export default function IndicatorPreview({ indicator }: { indicator: BehavioralIndicator }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-2xl font-bold">
          {indicator.title}
        </CardTitle>
        <CardDescription className="text-base">
          Live preview of the behavioral indicator.
        </CardDescription>
        <div className="flex items-center justify-start gap-2 pt-4">
          <Badge variant={indicator.isActive ? "default" : "secondary"}>
            {indicator.isActive ? "Active" : "Inactive"}
          </Badge>
          <Badge
            variant="outline"
          >
            {indicator.observabilityLevel}
          </Badge>
        </div>
      </CardHeader>
      <Separator />
      <CardContent className="p-6 space-y-6">
        <div>
          <h3 className="text-lg font-medium">Description</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {indicator.description}
          </p>
        </div>
        <div>
          <h3 className="text-lg font-medium">Examples</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {indicator.examples}
          </p>
        </div>
        <div>
          <h3 className="text-lg font-medium">Counter Examples</h3>
          <p className="mt-2 text-sm text-muted-foreground">
            {indicator.counterExamples}
          </p>
        </div>
      </CardContent>
    </Card>
  );
}
