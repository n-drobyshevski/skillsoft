'use client';

import * as React from "react";
import { Badge } from "@/components/ui/badge";
import { AssessmentQuestion } from "../../interfaces/domain-interfaces";
import { questionDifficultyToColor, questionTypeToIcon } from "../../utils";
import { Separator } from "@/components/ui/separator";
import { Card, CardContent, CardHeader, CardTitle, CardDescription } from "@/components/ui/card";

export default function QuestionPreview({ question }: { question: AssessmentQuestion }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-2xl font-bold">
          {question.questionText}
        </CardTitle>
        <CardDescription className="text-base">
          Live preview of the assessment question.
        </CardDescription>
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
      </CardHeader>
      <Separator />
      <CardContent className="p-6 space-y-6">
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
      </CardContent>
    </Card>
  );
}
