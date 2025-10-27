"use client";

import * as React from "react";
import { Settings2, X } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  Sheet,
  SheetContent,
  SheetHeader,
  SheetTitle,
  SheetDescription,
  SheetFooter,
  SheetClose,
} from "@/components/ui/sheet";
import { biLevelToColor } from "../../utils";
import { useState, useEffect } from "react";
import { AssessmentQuestion, BehavioralIndicator } from "../../interfaces/domain-interfaces";
import { assessmentQuestionsApi } from "@/services/api";
import QuestionCard from "./QuestionCard";
import { Separator } from "@/components/ui/separator";

export default function IndicatorDrawer({
  open,
  onOpenChange,
  indicator,
}: {
  open: boolean;
  onOpenChange: (isOpen: boolean) => void;
  indicator: BehavioralIndicator;
}) {
  const [questions, setQuestions] = useState<AssessmentQuestion[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      const fetchQuestionsForIndicator = async () => {
        setLoading(true);
        setError(null);
        try {
          const questionsData = await assessmentQuestionsApi.getQuestions(
            indicator.competencyId,
            indicator.id
          );
          if (!questionsData) {
            throw new Error("Failed to fetch assessment questions");
          }
          setQuestions(questionsData);
        } catch (err) {
          setError("Failed to load questions.");
          console.error(err);
        } finally {
          setLoading(false);
        }
      };
      fetchQuestionsForIndicator();
    }
  }, [open, indicator]);

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-3xl p-0 flex flex-col">
        <SheetHeader className="p-6 pb-2">
          <div className="flex items-center justify-between">
            <SheetTitle className="text-2xl font-bold">
              {indicator.title}
            </SheetTitle>
          </div>
          <SheetDescription className="text-base">
            Details for the behavioral indicator.
          </SheetDescription>
            <div className="flex items-center justify-start gap-2 pt-4">
              <Badge variant={indicator.isActive ? "default" : "secondary"}>
                {indicator.isActive ? "Active" : "Inactive"}
              </Badge>
              <Badge
                variant="outline"
                className={biLevelToColor(indicator.observabilityLevel)}
              >
                {indicator.observabilityLevel}
              </Badge>
              <Badge variant="secondary">
                Weight: {indicator.weight.toFixed(2)}
              </Badge>
            </div>
        </SheetHeader>
        <Separator />
        <div className="flex-1 overflow-y-auto">
          <div className="p-6 space-y-6">
            <div className="space-y-2">
              <h3 className="text-lg font-medium">Description</h3>
              <p className="leading-relaxed text-muted-foreground">
                {indicator.description}
              </p>
            </div>
            <Separator />
            <div className="space-y-4">
              <h3 className="text-lg font-medium">Assessment Questions</h3>
              {loading && <p>Loading questions...</p>}
              {error && <p className="text-red-500">{error}</p>}
              {!loading && !error && (
                <div className="space-y-4">
                  {questions.map((question) => (
                    <QuestionCard question={question} key={question.id} />
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>
        <SheetFooter className="p-6 bg-muted/40 border-t">
          <div className="flex flex-row gap-2 w-full">
            <Button variant="outline" className="flex-grow">
              <Settings2 className="mr-2 h-4 w-4" />
              Edit Indicator
            </Button>
          </div>
        </SheetFooter>
      </SheetContent>
    </Sheet>
  );
}
