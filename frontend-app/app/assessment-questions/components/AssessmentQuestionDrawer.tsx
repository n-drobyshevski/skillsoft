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
import { AssessmentQuestion } from "../../interfaces/domain-interfaces";
import { questionDifficultyToColor, questionTypeToIcon } from "../../utils";
import { Separator } from "@/components/ui/separator";

export default function AssessmentQuestionDrawer({
  open,
  onOpenChange,
  question,
}: {
  open: boolean;
  onOpenChange: (isOpen: boolean) => void;
  question: AssessmentQuestion;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent className="sm:max-w-2xl p-0 flex flex-col">
        <SheetHeader className="p-6 pb-2">
          <SheetTitle className="text-2xl font-bold">
            {question.questionText}
          </SheetTitle>
          <SheetDescription className="text-base">
            Details for the assessment question.
          </SheetDescription>
          <div className="flex items-center justify-start gap-2 pt-4">
            <Badge variant={question.isActive ? "default" : "secondary"}>
              {question.isActive ? "Active" : "Inactive"}
            </Badge>
            <Badge
              variant="outline"
              className={questionDifficultyToColor(question.difficultyLevel)}
            >
              {question.difficultyLevel}
            </Badge>
            <div className="flex items-center gap-2">
              <span>{questionTypeToIcon(question.questionType)}</span>
              <span className="font-medium">
                {question.questionType
                  .split("_")
                  .map((word) => word.charAt(0) + word.slice(1).toLowerCase())
                  .join(" ")}
              </span>
            </div>
          </div>
        </SheetHeader>
        <Separator />
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          {question.answerOptions && question.answerOptions.length > 0 && (
            <div>
              <h3 className="text-lg font-medium">Answer Options</h3>
              <ul className="mt-2 space-y-2">
                {question.answerOptions.map((option, index) => (
                  <li key={index} className="text-sm text-muted-foreground">
                    - {option.text}
                  </li>
                ))}
              </ul>
            </div>
          )}
          <div>
            <h3 className="text-lg font-medium">Scoring Rubric</h3>
            <p className="mt-2 text-sm text-muted-foreground">
              {question.scoringRubric}
            </p>
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}
