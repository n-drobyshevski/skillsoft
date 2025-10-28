"use client";

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
} from "@/components/ui/sheet";
import { Competency } from "../../interfaces/domain-interfaces";
import { approvalStatusToColor, competencyCategoryToIcon, competencyProficiencyLevelToColor } from "../../utils";
import { Separator } from "@/components/ui/separator";
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion";

export default function CompetencyDrawer({
  open,
  onOpenChange,
  competency,
}: {
  open: boolean;
  onOpenChange: (isOpen: boolean) => void;
  competency: Competency;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-2xl p-0 flex flex-col">
        <SheetHeader className="p-6 pb-2">
          <SheetTitle className="text-2xl font-bold flex items-center gap-2">
            {competencyCategoryToIcon(competency.category)}
            {competency.name}
          </SheetTitle>
          <SheetDescription className="text-base">
            Details for the competency.
          </SheetDescription>
          <div className="flex items-center justify-start gap-2 pt-4">
            <Badge variant={competency.isActive ? "default" : "secondary"}>
              {competency.isActive ? "Active" : "Inactive"}
            </Badge>
            <Badge
              variant="outline"
              className={approvalStatusToColor(competency.approvalStatus)}
            >
              {competency.approvalStatus}
            </Badge>
            <Badge
              variant="outline"
              className={competencyProficiencyLevelToColor(competency.level)}
            >
              {competency.level}
            </Badge>
          </div>
        </SheetHeader>
        <Separator />
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          <div>
            <h3 className="text-lg font-medium">Description</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {competency.description}
            </p>
          </div>
          {competency.behavioralIndicators && competency.behavioralIndicators.length > 0 && (
            <div>
              <h3 className="text-lg font-medium">Behavioral Indicators</h3>
              <Accordion type="single" collapsible className="w-full mt-2">
                {competency.behavioralIndicators.map((indicator) => (
                  <AccordionItem value={indicator.id} key={indicator.id}>
                    <AccordionTrigger>{indicator.title}</AccordionTrigger>
                    <AccordionContent>
                      {indicator.description}
                    </AccordionContent>
                  </AccordionItem>
                ))}
              </Accordion>
            </div>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}
