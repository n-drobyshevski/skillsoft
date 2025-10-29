'use client';
import React, { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
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
	BarChart3,
	HelpCircle,
	CheckCircle,
	Tag,
	Zap,
	Info,
	ArrowLeft,
	AlertTriangle,
	Lightbulb,
	X,
	Pencil,
} from "lucide-react";
import type {
	BehavioralIndicator,
	AssessmentQuestion,
} from "../../interfaces/domain-interfaces";
import { behavioralIndicatorsApi, assessmentQuestionsApi } from "@/services/api";
import AssessmentQuestionDrawer from "../../assessment-questions/components/AssessmentQuestionDrawer";

interface IndicatorDetailPageProps {
	params: Promise<{ indicatorId: string; competencyId: string }>;
}

const approvalStatusToColor = (status: string): string => {
    const colors: { [key: string]: string } = {
        DRAFT: "border-gray-500/30 text-gray-400 bg-gray-500/8 dark:text-gray-300",
        PENDING_REVIEW: "border-yellow-500/30 text-yellow-500 bg-yellow-500/8 dark:text-yellow-300",
        APPROVED: "border-emerald-500/30 text-emerald-600 bg-emerald-500/8 dark:text-emerald-300",
        REJECTED: "border-red-500/30 text-red-400 bg-red-500/8 dark:text-red-300",
        ARCHIVED: "border-gray-500/30 text-gray-400 bg-gray-500/8 dark:text-gray-300",
        UNDER_REVISION: "border-blue-500/30 text-blue-600 bg-blue-500/8 dark:text-blue-300",
    };
    return colors[status] || colors["DRAFT"];
};

export default function Page({ params }: IndicatorDetailPageProps) {
	const { indicatorId, competencyId } = use(params);
	const router = useRouter();

	const [indicator, setIndicator] = useState<BehavioralIndicator | null>(null);
	const [assessmentQuestions, setAssessmentQuestions] = useState<
		AssessmentQuestion[]
	>([]);
	const [loading, setLoading] = useState(true);
	const [error, setError] = useState<string | null>(null);
  const [selectedQuestion, setSelectedQuestion] = useState<AssessmentQuestion | null>(null);

	useEffect(() => {
		if (!indicatorId) {
			setError("No indicator ID provided");
			setLoading(false);
			return;
		}

		const fetchIndicatorData = async () => {
			try {
				setLoading(true);

				// Fetch indicator details
				const indicatorData: BehavioralIndicator | null =
					await behavioralIndicatorsApi.getIndicatorById(indicatorId);
				if (!indicatorData) {
					setError("Indicator not found");
					setLoading(false);
					return;
				}
				setIndicator(indicatorData);

				// Fetch assessment questions for the indicator
				const questions = await assessmentQuestionsApi.getIndicatorQuestions(
					indicatorData.competencyId,
					indicatorId,
				);
				setAssessmentQuestions(questions);
			} catch (error) {
				console.error("Error fetching indicator data:", error);
				setError(
					error instanceof Error
						? error.message
						: "Failed to load indicator data",
				);
			} finally {
				setLoading(false);
			}
		};

		fetchIndicatorData();
	}, [indicatorId]);

	const formatProficiencyLevel = (level: string) => {
		return level.charAt(0) + level.slice(1).toLowerCase().replace("_", " ");
	};

	const levelToColor = (level: string): string => {
		const colors: { [key: string]: string } = {
			NOVICE: "border-red-500/30 text-red-400 bg-red-500/8 dark:text-red-300",
			DEVELOPING:
				"border-orange-500/30 text-orange-500 bg-orange-500/8 dark:text-orange-300",
			PROFICIENT:
				"border-yellow-500/30 text-yellow-600 bg-yellow-500/8 dark:text-yellow-300",
			ADVANCED:
				"border-emerald-500/30 text-emerald-600 bg-emerald-500/8 dark:text-emerald-300",
			EXPERT:
				"border-blue-500/30 text-blue-600 bg-blue-500/8 dark:text-blue-300",
		};
		return colors[level] || colors["NOVICE"];
	};

	if (loading) {
		return (
			<div className="container mx-auto px-6 py-8 space-y-6">
				<div className="flex items-center gap-4 mb-6">
					<Skeleton className="h-10 w-10" />
					<div className="space-y-2">
						<Skeleton className="h-8 w-64" />
						<Skeleton className="h-4 w-48" />
					</div>
				</div>

				<div className="space-y-4">
					<Skeleton className="h-12 w-full" />
					<div className="grid grid-cols-1 md:grid-cols-2 gap-6">
						{Array.from({ length: 4 }).map((_, i) => (
							<Card key={i}>
								<CardHeader>
									<Skeleton className="h-6 w-32" />
								</CardHeader>
								<CardContent className="space-y-2">
									<Skeleton className="h-4 w-full" />
									<Skeleton className="h-4 w-3/4" />
									<Skeleton className="h-4 w-1/2" />
								</CardContent>
							</Card>
						))}
					</div>
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
						<CardTitle className="text-destructive mb-2">{error}</CardTitle>
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

	if (!indicator) {
		return (
			<div className="container mx-auto px-6 py-8 flex items-center justify-center min-h-[60vh]">
				<Card className="max-w-md w-full">
					<CardContent className="p-12 text-center">
						<FileText className="w-20 h-20 text-muted-foreground mx-auto mb-6" />
						<CardTitle className="mb-3">Indicator Not Found</CardTitle>
						<CardDescription className="mb-6">
							The requested behavioral indicator could not be found.
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
			{/* Header */}
			<div className="flex items-center justify-between mb-8">
				<div className="flex items-center gap-4">
					<Button variant="ghost" size="icon" onClick={() => router.back()}>
						<ArrowLeft className="w-4 h-4" />
						<span className="sr-only">Go back</span>
					</Button>
					<div>
						<h1 className="text-3xl font-bold tracking-tight">
							{indicator.title}
						</h1>
						<div className="flex items-center gap-2 mt-2">
							<Badge
								variant="outline"
								className={levelToColor(indicator.observabilityLevel)}
							>
								{formatProficiencyLevel(indicator.observabilityLevel)}
							</Badge>
							<Badge variant={indicator.isActive ? "default" : "secondary"}>
								{indicator.isActive ? "Active" : "Inactive"}
							</Badge>
                            <Badge
                                variant="outline"
                                className={approvalStatusToColor(indicator.approvalStatus)}
                            >
                                {indicator.approvalStatus.replace("_", " ")}
                            </Badge>
						</div>
					</div>
				</div>
				<Link href={`/behavioral-indicators/${indicatorId}/edit`} passHref>
					<Button>
						<Pencil className="mr-2 h-4 w-4" />
						Edit
					</Button>
				</Link>
			</div>

			<div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
				<div className="lg:col-span-2 space-y-6">
					{/* Description Card */}
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<FileText className="w-5 h-5" />
								Description
							</CardTitle>
						</CardHeader>
						<CardContent>
							<p className="text-muted-foreground leading-relaxed">
								{indicator.description ||
									"No description available for this indicator."}
							</p>
						</CardContent>
					</Card>

					{/* Examples and Counter Examples */}
					<div className="grid grid-cols-1 md:grid-cols-2 gap-6">
						{indicator.examples && (
							<div>
								<h4 className="font-semibold mb-3 flex items-center gap-2">
									<Lightbulb className="w-4 h-4 text-emerald-500" />
									Examples
								</h4>
								<Card className="bg-emerald-500/5 border-emerald-500/20">
									<CardContent className="p-4">
										<p className="text-sm text-emerald-700 dark:text-emerald-300 leading-relaxed">
											{indicator.examples}
										</p>
									</CardContent>
								</Card>
							</div>
						)}

						{indicator.counterExamples && (
							<div>
								<h4 className="font-semibold mb-3 flex items-center gap-2">
									<X className="w-4 h-4 text-red-500" />
									Counter Examples
								</h4>
								<Card className="bg-red-500/5 border-red-500/20">
									<CardContent className="p-4">
										<p className="text-sm text-red-700 dark:text-red-300 leading-relaxed">
											{indicator.counterExamples}
										</p>
									</CardContent>
								</Card>
							</div>
						)}
					</div>

					{/* Assessment Questions */}
          {assessmentQuestions.length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <HelpCircle className="w-5 h-5" />
                  Assessment Questions ({assessmentQuestions.length})
                </CardTitle>
              </CardHeader>
              <CardContent className="space-y-4">
                {assessmentQuestions
                  .sort((a, b) => a.orderIndex - b.orderIndex)
                  .map((question) => (
                    <Card
                      key={question.id}
                      className="border-muted hover:border-primary/80 transition-colors cursor-pointer"
                      onClick={() => setSelectedQuestion(question)}
                    >
                      <CardContent className="p-4">
                        <div className="flex items-start justify-between mb-3">
                          <p className="text-sm flex-1 leading-relaxed">
                            {question.questionText}
                          </p>
                          <Badge
                            variant="outline"
                            className={
                              question.difficultyLevel === "BASIC"
                                ? "border-emerald-500/30 text-emerald-600 bg-emerald-500/8"
                                : question.difficultyLevel === "INTERMEDIATE"
                                ? "border-yellow-500/30 text-yellow-600 bg-yellow-500/8"
                                : question.difficultyLevel === "ADVANCED"
                                ? "border-orange-500/30 text-orange-600 bg-orange-500/8"
                                : "border-red-500/30 text-red-600 bg-red-500/8"
                            }
                          >
                            {question.difficultyLevel}
                          </Badge>
                        </div>
                        <div className="text-xs text-muted-foreground font-medium">
                          {question.questionType.replace("_", " ")}
                        </div>
                      </CardContent>
                    </Card>
                  ))}
              </CardContent>
            </Card>
          )}
				</div>

				{/* Details Card */}
				<div className="space-y-6">
					<Card>
						<CardHeader>
							<CardTitle className="flex items-center gap-2">
								<Info className="w-5 h-5" />
								Details
							</CardTitle>
						</CardHeader>
						<CardContent className="grid gap-4 sm:grid-cols-2">
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Weight</div>
								<div>{indicator.weight}</div>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Measurement Type</div>
								<div>{indicator.measurementType.replace("_", " ")}</div>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Order Index</div>
								<div>{indicator.orderIndex}</div>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Observability Level</div>
								<Badge variant="outline" className={levelToColor(indicator.observabilityLevel)}>
									{formatProficiencyLevel(indicator.observabilityLevel)}
								</Badge>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Approval Status</div>
								<Badge variant="outline" className={approvalStatusToColor(indicator.approvalStatus)}>
									{indicator.approvalStatus.replace("_", " ")}
								</Badge>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Status</div>
								<Badge variant={indicator.isActive ? "default" : "secondary"}>
									{indicator.isActive ? "Active" : "Inactive"}
								</Badge>
							</div>
							<div className="grid gap-1">
								<div className="font-semibold text-muted-foreground">Competency</div>
								<Link href={`/competencies/${indicator.competencyId}`} className="text-primary hover:underline">
									View Competency
								</Link>
							</div>
						</CardContent>
					</Card>
				</div>
			</div>
      {selectedQuestion && (
        <AssessmentQuestionDrawer
          open={!!selectedQuestion}
          onOpenChange={(isOpen) => {
            if (!isOpen) {
              setSelectedQuestion(null);
            }
          }}
          question={selectedQuestion}
        />
      )}
		</div>
	);
}
