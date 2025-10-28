"use client";

import { Badge } from "@/components/ui/badge";
import React from "react";
import { AssessmentQuestion } from "../../interfaces/domain-interfaces";
import { CheckCircle, HelpCircle } from "lucide-react";
import Link from "next/link";

export default function QuestionCard({ question }: { question: AssessmentQuestion }) {
  return (
    <Link href={`/assessment-questions/${question.id}`}>
      <div
        key={question.id}
        className="border rounded-lg p-3 space-y-2 hover:bg-muted/50 transition-colors cursor-pointer"
      >
        <div className="flex items-start justify-between">
          <Badge
            variant="outline"
            className={
              `text-xs px-2 py-0.5 rounded-full font-medium ` +
              (question.difficultyLevel === "BASIC"
                ? "border-emerald-500/30 text-emerald-600 bg-emerald-500/8"
                : question.difficultyLevel === "INTERMEDIATE"
                ? "border-yellow-500/30 text-yellow-600 bg-yellow-500/8"
                : question.difficultyLevel === "ADVANCED"
                ? "border-orange-500/30 text-orange-600 bg-orange-500/8"
                : "border-red-500/30 text-red-600 bg-red-500/8")
            }
          >
            {question.difficultyLevel}
          </Badge>
        </div>
        <p className="text-sm font-normal">{question.questionText}</p>
        <div className="text-xs text-muted-foreground flex items-center gap-1.5">
          <HelpCircle className="w-3.5 h-3.5" />
          {question.questionType.replace("_", " ")}
        </div>

        {question.answerOptions && question.answerOptions.length > 0 && (
          <div>
            <h4 className="text-xs font-semibold mb-2 flex items-center gap-1.5">
              <CheckCircle className="w-3.5 h-3.5" />
              Answer Options:
            </h4>
            <div className="flex flex-wrap gap-2">
              {question.answerOptions.map((option, idx) => (
                <Badge
                  key={idx}
                  variant={option.correct ? "default" : "secondary"}
                  className="text-xs font-normal"
                >
                  {option.text || option.label}
                  {option.score && ` (${option.score}pt)`}
                </Badge>
              ))}
            </div>
          </div>
        )}
      </div>
    </Link>
  );
}