"use client";
import React, { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import {
	Card,
	CardContent,
	CardDescription,
	CardHeader,
	CardTitle,
} from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Skeleton } from "@/components/ui/skeleton";
import {
	FileText,
	HelpCircle,
	ArrowLeft,
	AlertTriangle,
	Clock,
	BarChart,
	Info,
	Type,
	Pencil,
} from "lucide-react";
import type { AssessmentQuestion } from "../../interfaces/domain-interfaces";
import { assessmentQuestionsApi } from "@/services/api";
import Link from "next/link";

interface QuestionDetailPageProps {
	params: Promise<{ questionId: string }>;
}

const difficultyLevelToColor = (level: string): string => {
	const colors: { [key: string]: string } = {
		FOUNDATIONAL:
			"border-emerald-500/30 text-emerald-600 bg-emerald-500/8 dark:text-emerald-300",
		INTERMEDIATE:
			"border-yellow-500/30 text-yellow-600 bg-yellow-500/8 dark:text-yellow-300",
		ADVANCED:
			"border-orange-500/30 text-orange-600 bg-orange-500/8 dark:text-orange-300",
		EXPERT: "border-red-500/30 text-red-600 bg-red-500/8 dark:text-red-300",
	};
	return colors[level] || "border-gray-500/30 text-gray-400 bg-gray-500/8";
};

export default function Page({ params }: QuestionDetailPageProps) {
	const { questionId } = use(params);
	const router = useRouter();

	const [question, setQuestion] = useState<AssessmentQuestion | null>(null);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);

	useEffect(() => {
		if (!questionId) {
			setError("No question ID provided");
			setLoading(false);
			return;
		}

		const fetchQuestionData = async () => {
			try {
				setLoading(true);
				const questionData: AssessmentQuestion | null =
					await assessmentQuestionsApi.getQuestionById(questionId);
				if (!questionData) {
					setError("Question not found");
				} else {
					setQuestion(questionData);
				}
			} catch (error) {
				console.error("Error fetching question data:", error);
				setError(
					error instanceof Error
						? error.message
						: "Failed to load question data",
				);
			} finally {
				setLoading(false);
			}
		};

		fetchQuestionData();
	}, [questionId]);

	if (loading) {
		return (
			<div className="container mx-auto px-6 py-8 space-y-6">
				<div className="flex items-center gap-4 mb-6">
					<Skeleton className="h-10 w-10" />
					<div className="space-y-2">
						<Skeleton className="h-8 w-96" />
						<Skeleton className="h-4 w-64" />
					</div>
				</div>
				<Skeleton className="h-32 w-full" />
				<div className="grid grid-cols-1 md:grid-cols-2 gap-6">
					<Skeleton className="h-48 w-full" />
					<Skeleton className="h-48 w-full" />
				</div>
			</div>
		);
	}

	if (error) {
		return (
			<div className="container mx-auto px-6 py-8 flex items-center justify-center min-h-[60vh]">
				<Card className="max-w-md w-full border-destructive/50">
					<CardContent className="p-8 text-center">
						<AlertTriangle className="w-16 h-16 text-destructive mx-auto mb-4" />
						<CardTitle className="text-destructive mb-2">
							An Error Occurred
						</CardTitle>
						<CardDescription className="mb-6">{error}</CardDescription>
						<Button
							onClick={() => router.back()}
							variant="destructive"
							className="w-full"
						>
							<ArrowLeft className="w-4 h-4 mr-2" />
							Go Back
						</Button>
					</CardContent>
				</Card>
			</div>
		);
	}

	if (!question) {
		return (
			<div className="container mx-auto px-6 py-8 flex items-center justify-center min-h-[60vh]">
				<Card className="max-w-md w-full">
					<CardContent className="p-12 text-center">
						<HelpCircle className="w-20 h-20 text-muted-foreground mx-auto mb-6" />
						<CardTitle className="mb-3">Question Not Found</CardTitle>
						<CardDescription className="mb-6">
							The requested assessment question could not be found.
						</CardDescription>
						<Button onClick={() => router.back()} className="w-full">
							<ArrowLeft className="w-4 h-4 mr-2" />
							Go Back
						</Button>
					</CardContent>
				</Card>
			</div>
		);
	}

	return (
		<div className="container mx-auto px-6 py-8">
			<div className="flex items-center justify-between mb-8">
				<div className="flex items-center gap-4">
					<Button variant="ghost" size="icon" onClick={() => router.back()}>
						<ArrowLeft className="w-4 h-4" />
						<span className="sr-only">Go back</span>
					</Button>
					<div>
						<h1 className="text-3xl font-bold tracking-tight">
							Assessment Question
						</h1>
						<div className="flex items-center gap-2 mt-2">
							<Badge
								variant="outline"
								className={difficultyLevelToColor(question.difficultyLevel)}
							>
								{question.difficultyLevel}
							</Badge>
							<Badge variant={question.isActive ? "default" : "secondary"}>
								{question.isActive ? "Active" : "Inactive"}
							</Badge>
						</div>
					</div>
				</div>
				<Link href={`/assessment-questions/${questionId}/edit`} passHref>
					<Button>
						<Pencil className="mr-2 h-4 w-4" />
						Edit
					</Button>
				</Link>
			</div>

			<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
				<div className="lg:col-span-2 space-y-6">
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<FileText className="w-5 h-5" />
								Question Details
							</CardTitle>
						</CardHeader>
						<CardContent>
							<p className="text-muted-foreground leading-relaxed">
								{question.questionText}
							</p>
						</CardContent>
					</Card>

					{question.answerOptions && question.answerOptions.length > 0 && (
						<Card>
							<CardHeader>
								<CardTitle className="flex items-center gap-2">
									<HelpCircle className="w-5 h-5" />
									Answer Options
								</CardTitle>
							</CardHeader>
							<CardContent>
								<div className="space-y-4">
									{question.answerOptions.map((option, index) => (
										<div
											key={index}
											className="p-4 bg-muted/50 rounded-lg border"
										>
											<p className="font-semibold">{option.text as string}</p>
											<div className="flex items-center justify-between mt-2">
												<Badge variant="secondary">Score: {option.score as number}</Badge>
												<p className="text-sm text-muted-foreground">{option.explanation as string}</p>
											</div>
										</div>
									))}
								</div>
							</CardContent>
						</Card>
					)}
				</div>

				<div className="space-y-6">
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<Info className="w-5 h-5" />
								Metadata
							</CardTitle>
						</CardHeader>
						<CardContent className="space-y-4">
							<div className="flex items-center gap-3">
								<Type className="w-5 h-5 text-muted-foreground" />
								<div>
									<dt className="text-sm font-medium text-muted-foreground">
										Question Type
									</dt>
									<dd className="text-sm font-medium">
										{question.questionType.replace("_", " ")}
									</dd>
								</div>
							</div>
							<div className="flex items-center gap-3">
								<BarChart className="w-5 h-5 text-muted-foreground" />
								<div>
									<dt className="text-sm font-medium text-muted-foreground">
										Difficulty Level
									</dt>
									<dd className="text-sm font-medium">
										{question.difficultyLevel}
									</dd>
								</div>
							</div>
							{question.timeLimit && (
								<div className="flex items-center gap-3">
									<Clock className="w-5 h-5 text-muted-foreground" />
									<div>
										<dt className="text-sm font-medium text-muted-foreground">
											Time Limit
										</dt>
										<dd className="text-sm font-medium">
											{question.timeLimit} seconds
										</dd>
									</div>
								</div>
							)}
						</CardContent>
					</Card>
				</div>
			</div>
		</div>
	);
}
