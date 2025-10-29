'use client';

import { zodResolver } from '@hookform/resolvers/zod';
import { useForm, useFieldArray } from 'react-hook-form';
import { z } from 'zod';
import { questionSchema } from '../validation';
import { AssessmentQuestion } from '../../interfaces/domain-interfaces';
import { Button } from '@/components/ui/button';
import {
  Form,
  FormControl,
  FormDescription,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form';
import { Input } from '@/components/ui/input';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { Switch } from '@/components/ui/switch';
import { DifficultyLevel } from '../../enums/domain_enums';
import { assessmentQuestionsApi } from '@/services/api';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { TrashIcon } from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';

type QuestionFormValues = z.infer<typeof questionSchema>;

const questionTypes = [
  'MULTIPLE_CHOICE',
  'SINGLE_CHOICE',
  'TRUE_FALSE',
  'OPEN_ENDED',
  'SCENARIO_BASED',
  'LIKERT_SCALE',
  'SITUATIONAL_JUDGMENT',
] as const;

export function EditQuestionForm({ question, competencyId }: { question: AssessmentQuestion, competencyId: string }) {
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const form = useForm<QuestionFormValues>({
    resolver: zodResolver(questionSchema),
    defaultValues: {
      questionText: question.questionText,
      questionType: question.questionType,
      scoringRubric: question.scoringRubric,
      difficultyLevel: question.difficultyLevel,
      isActive: question.isActive,
      orderIndex: question.orderIndex,
      timeLimit: question.timeLimit,
      answerOptions: question.answerOptions || [],
    },
  });

  const { fields, append, remove } = useFieldArray({
    control: form.control,
    name: "answerOptions",
  });

  async function onSubmit(data: QuestionFormValues) {
    setIsLoading(true);
    setError(null);
    try {
      await assessmentQuestionsApi.updateQuestion(question.id, data, competencyId, question.behavioralIndicatorId);
      router.push(`/assessment-questions/${question.id}`);
    } catch (e: any) {
      setError(e.message || "An error occurred.");
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
        <Card>
          <CardHeader>
            <CardTitle>Basic Information</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            <FormField
              control={form.control}
              name="questionText"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Question Text</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g., How do you handle tight deadlines?" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="questionType"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Question Type</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a question type" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {questionTypes.map((type) => (
                        <SelectItem key={type} value={type}>
                          {type.replace(/_/g, ' ')}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Details</CardTitle>
          </CardHeader>
          <CardContent className="grid md:grid-cols-2 gap-6">
            <FormField
              control={form.control}
              name="scoringRubric"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Scoring Rubric</FormLabel>
                  <FormControl>
                    <Input placeholder="e.g., Based on clarity and feasibility" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="difficultyLevel"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Difficulty Level</FormLabel>
                  <Select onValueChange={field.onChange} defaultValue={field.value}>
                    <FormControl>
                      <SelectTrigger>
                        <SelectValue placeholder="Select a difficulty level" />
                      </SelectTrigger>
                    </FormControl>
                    <SelectContent>
                      {Object.values(DifficultyLevel).map((level) => (
                        <SelectItem key={level} value={level}>
                          {level}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="orderIndex"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Order Index</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <FormField
              control={form.control}
              name="timeLimit"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Time Limit (seconds)</FormLabel>
                  <FormControl>
                    <Input type="number" {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle>Answer Options</CardTitle>
          </CardHeader>
          <CardContent className="space-y-4">
            {fields.map((field, index) => (
              <div key={field.id} className="flex items-end space-x-4 p-4 border rounded-lg bg-muted/50">
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 flex-grow">
                    <FormField
                      control={form.control}
                      name={`answerOptions.${index}.text`}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Option {index + 1} Text</FormLabel>
                          <FormControl>
                            <Input {...field} placeholder="e.g., Prioritize tasks" />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name={`answerOptions.${index}.score`}
                      render={({ field }) => (
                        <FormItem>
                          <FormLabel>Score</FormLabel>
                          <FormControl>
                            <Input type="number" {...field} placeholder="e.g., 10" />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                    <FormField
                      control={form.control}
                      name={`answerOptions.${index}.correct`}
                      render={({ field }) => (
                        <FormItem className="flex flex-col items-start space-y-2 pt-2">
                          <FormLabel>Correct?</FormLabel>
                          <FormControl>
                            <Switch checked={field.value} onCheckedChange={field.onChange} />
                          </FormControl>
                        </FormItem>
                      )}
                    />
                </div>
                <Button type="button" variant="ghost" size="icon" onClick={() => remove(index)} className="hover:bg-destructive/80 hover:text-destructive-foreground">
                  <TrashIcon className="h-4 w-4" />
                </Button>
              </div>
            ))}
            <Button
              type="button"
              variant="outline"
              size="sm"
              className="mt-4"
              onClick={() => append({ text: "", score: 0, correct: false })}
            >
              Add Answer Option
            </Button>
          </CardContent>
        </Card>

        <Card>
            <CardContent className="pt-6">
                <FormField
                control={form.control}
                name="isActive"
                render={({ field }) => (
                    <FormItem className="flex flex-row items-center justify-between rounded-lg border p-4">
                    <div className="space-y-0.5">
                        <FormLabel className="text-base">Active Status</FormLabel>
                        <FormDescription>
                        Set whether this question is currently active and available.
                        </FormDescription>
                    </div>
                    <FormControl>
                        <Switch
                        checked={field.value}
                        onCheckedChange={field.onChange}
                        />
                    </FormControl>
                    </FormItem>
                )}
                />
            </CardContent>
        </Card>

        {error && <p className="text-sm font-medium text-destructive text-center">{error}</p>}
        <div className="flex justify-end space-x-4 pt-4">
          <Button type="button" variant="outline" onClick={() => router.back()} disabled={isLoading}>
            Cancel
          </Button>
          <Button type="submit" disabled={isLoading}>
            {isLoading ? "Saving..." : "Save Changes"}
          </Button>
        </div>
      </form>
    </Form>
  );
}
