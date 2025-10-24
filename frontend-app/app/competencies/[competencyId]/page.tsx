'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../../../src/components/ui/card';
import WeightDistributionPie from '@/components/charts/WeightDistributionPie';
import { Badge } from '../../../src/components/ui/badge';
import { Button } from '../../../src/components/ui/button';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '../../../src/components/ui/tabs';
import { Skeleton } from '../../../src/components/ui/skeleton';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from '../../../src/components/ui/collapsible';
import { 
  FileText, 
  BarChart3, 
  HelpCircle, 
  Clock, 
  Calendar, 
  CheckCircle, 
  Tag, 
  Zap,
  Info,
  ChevronDown,
  ArrowLeft,
  AlertTriangle,
  Lightbulb,
  X,
  Target
} from 'lucide-react';
import type { Competency, BehavioralIndicator, AssessmentQuestion } from '../../types/competency';

interface CompetencyDetailPageProps {
    params: { competencyId: string };
}

const CompetencyDetailPage: React.FC<CompetencyDetailPageProps> = ({ params }) => {
  const { competencyId } = params;
  const router = useRouter();

  const [competency, setCompetency] = useState<Competency | null>(null);
  const [assessmentQuestions, setAssessmentQuestions] = useState<AssessmentQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState<'overview' | 'indicators' | 'assessment'>('overview');
  const [expandedIndicators, setExpandedIndicators] = useState<Set<string>>(new Set());

  useEffect(() => {
    if (!competencyId) {
      setError('No competency ID provided');
      setLoading(false);
      return;
    }

    const fetchCompetencyData = async () => {
      try {
        setLoading(true);
        
        // Fetch competency details
        const competencyResponse = await fetch(`http://localhost:8080/api/competencies/${competencyId}`);
        if (!competencyResponse.ok) {
          throw new Error(`Failed to fetch competency: ${competencyResponse.status}`);
        }
        const competencyData = await competencyResponse.json();
        setCompetency(competencyData);

        // Fetch assessment questions for all behavioral indicators
        if (competencyData.behavioralIndicators?.length > 0) {
          const questionsPromises = competencyData.behavioralIndicators.map(async (indicator: BehavioralIndicator) => {
            try {
              const response = await fetch(`http://localhost:8080/api/competencies/${competencyId}/bi/${indicator.id}/questions`);
              if (response.ok) {
                return await response.json();
              }
              return [];
            } catch (error) {
              console.warn(`Failed to fetch questions for indicator ${indicator.id}:`, error);
              return [];
            }
          });

          const questionsArrays = await Promise.all(questionsPromises);
          const allQuestions = questionsArrays.flat();
          setAssessmentQuestions(allQuestions);
        }

      } catch (error) {
        console.error('Error fetching competency data:', error);
        setError(error instanceof Error ? error.message : 'Failed to load competency data');
      } finally {
        setLoading(false);
      }
    };

    fetchCompetencyData();
  }, [competencyId]);

  const toggleIndicatorExpansion = (indicatorId: string) => {
    const newExpanded = new Set(expandedIndicators);
    if (newExpanded.has(indicatorId)) {
      newExpanded.delete(indicatorId);
    } else {
      newExpanded.add(indicatorId);
    }
    setExpandedIndicators(newExpanded);
  };

  const formatProficiencyLevel = (level: string) => {
    return level.charAt(0) + level.slice(1).toLowerCase().replace('_', ' ');
  };

  const levelToColor = (level: string): string => {
    const colors: { [key: string]: string } = {
      'NOVICE': 'border-red-500/30 text-red-400 bg-red-500/8 dark:text-red-300',
      'DEVELOPING': 'border-orange-500/30 text-orange-500 bg-orange-500/8 dark:text-orange-300',
      'PROFICIENT': 'border-yellow-500/30 text-yellow-600 bg-yellow-500/8 dark:text-yellow-300',
      'ADVANCED': 'border-emerald-500/30 text-emerald-600 bg-emerald-500/8 dark:text-emerald-300',
      'EXPERT': 'border-blue-500/30 text-blue-600 bg-blue-500/8 dark:text-blue-300'
    };
    return colors[level] || colors['NOVICE'];
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
            <Button onClick={() => router.push('/')} variant="destructive" className="w-full">
              <ArrowLeft className="w-4 h-4 mr-2" />
              Go Back
            </Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  if (!competency) {
    return (
      <div className="container mx-auto px-6 py-8 flex items-center justify-center min-h-[60vh]">
        <Card className="max-w-md w-full">
          <CardContent className="p-12 text-center">
            <FileText className="w-20 h-20 text-muted-foreground mx-auto mb-6" />
            <CardTitle className="mb-3">Competency Not Found</CardTitle>
            <CardDescription className="mb-6">
              The requested competency could not be found.
            </CardDescription>
            <Button onClick={() => router.push('/')} className="w-full">
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
          <Button variant="ghost" size="icon" onClick={() => router.push('/')}>
            <ArrowLeft className="w-4 h-4" />
            <span className="sr-only">Go back</span>
          </Button>
          <div>
            <h1 className="text-3xl font-bold tracking-tight">{competency.name}</h1>
            <div className="flex items-center gap-2 mt-2">
              <Badge variant="outline" className={levelToColor(competency.level)}>
                {formatProficiencyLevel(competency.level)}
              </Badge>
              <Badge variant={competency.isActive ? 'default' : 'secondary'}>
                {competency.isActive ? 'Active' : 'Inactive'}
              </Badge>
              <Badge variant="outline">
                <Tag className="w-3 h-3 mr-1" />
                {competency.category.replace('_', ' ')}
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <Tabs value={activeTab} onValueChange={(value) => setActiveTab(value as typeof activeTab)} className="w-full">
        <TabsList className="grid w-full grid-cols-3">
          <TabsTrigger value="overview" className="flex items-center gap-2">
            <FileText className="w-4 h-4" />
            Overview
          </TabsTrigger>
          <TabsTrigger value="indicators" className="flex items-center gap-2">
            <BarChart3 className="w-4 h-4" />
            Indicators ({competency.behavioralIndicators?.length || 0})
          </TabsTrigger>
          <TabsTrigger value="assessment" className="flex items-center gap-2">
            <HelpCircle className="w-4 h-4" />
            Questions ({assessmentQuestions.length})
          </TabsTrigger>
        </TabsList>

        <TabsContent value="overview" className="mt-6 space-y-6">
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
                {competency.description || 'No description available for this competency.'}
              </p>
            </CardContent>
          </Card>

          {/* Standard Codes Card */}
          {competency.standardCodes && Object.keys(competency.standardCodes).length > 0 && (
            <Card>
              <CardHeader>
                <CardTitle className="flex items-center gap-2">
                  <Tag className="w-5 h-5" />
                  Standard Codes
                </CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                  {Object.entries(competency.standardCodes).map(([standard, mapping]) => (
                    mapping && (
                      <Card key={standard} className="border-muted hover:border-muted-foreground/20 transition-colors">
                        <CardContent className="p-4">
                          <div className="flex items-center justify-between mb-3">
                            <h3 className="font-semibold">{standard}</h3>
                            <Badge variant="outline" className={
                              mapping.confidence === 'VERIFIED' ? 'border-emerald-500/30 text-emerald-600 bg-emerald-500/8' :
                              mapping.confidence === 'HIGH' ? 'border-blue-500/30 text-blue-600 bg-blue-500/8' :
                              mapping.confidence === 'MODERATE' ? 'border-yellow-500/30 text-yellow-600 bg-yellow-500/8' :
                              'border-muted text-muted-foreground'
                            }>
                              {mapping.confidence}
                            </Badge>
                          </div>
                          <p className="text-sm text-muted-foreground mb-2 font-mono">{mapping.code}</p>
                          <p className="text-sm text-muted-foreground">{mapping.name}</p>
                        </CardContent>
                      </Card>
                    )
                  ))}
                </div>
              </CardContent>
            </Card>
          )}

          {/* Metadata Card */}
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Info className="w-5 h-5" />
                Metadata
              </CardTitle>
            </CardHeader>
            <CardContent>
              <dl className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="flex items-center gap-3">
                  <Clock className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <dt className="text-sm font-medium text-muted-foreground">Created</dt>
                    <dd className="text-sm font-medium">{new Date(competency.createdAt).toLocaleDateString()}</dd>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Calendar className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <dt className="text-sm font-medium text-muted-foreground">Last Modified</dt>
                    <dd className="text-sm font-medium">{new Date(competency.lastModified).toLocaleDateString()}</dd>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <CheckCircle className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <dt className="text-sm font-medium text-muted-foreground">Status</dt>
                    <dd className="text-sm font-medium">
                      <Badge variant={competency.isActive ? 'default' : 'secondary'}>
                        {competency.isActive ? 'Active' : 'Inactive'}
                      </Badge>
                    </dd>
                  </div>
                </div>
                <div className="flex items-center gap-3">
                  <Tag className="w-5 h-5 text-muted-foreground" />
                  <div>
                    <dt className="text-sm font-medium text-muted-foreground">Version</dt>
                    <dd className="text-sm font-medium">{competency.version}</dd>
                  </div>
                </div>
              </dl>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="indicators" className="mt-6 space-y-6">
          {competency.behavioralIndicators && competency.behavioralIndicators.length > 0 ? (
            <>
              <WeightDistributionPie indicators={competency.behavioralIndicators} />
              <div className="space-y-6">
                {competency.behavioralIndicators
              .sort((a, b) => a.orderIndex - b.orderIndex)
              .map((indicator) => (
                <Collapsible 
                  key={indicator.id}
                  open={expandedIndicators.has(indicator.id)}
                  onOpenChange={() => toggleIndicatorExpansion(indicator.id)}
                >
                  <Card>
                    <CollapsibleTrigger className="w-full p-6 hover:bg-accent/50 transition-colors text-left">
                      <div className="flex items-start justify-between">
                        <div className="flex-1">
                          <div className="flex items-center space-x-3 mb-3">
                            <h3 className="text-lg font-semibold">{indicator.title}</h3>
                            <Badge variant="outline" className={levelToColor(indicator.observabilityLevel)}>
                              {formatProficiencyLevel(indicator.observabilityLevel)}
                            </Badge>
                          </div>
                          {indicator.description && (
                            <p className="text-muted-foreground text-sm leading-relaxed">{indicator.description}</p>
                          )}
                          <div className="flex items-center gap-4 mt-3 text-xs text-muted-foreground">
                            <span className="flex items-center gap-1">
                              <Zap className="w-3 h-3" />
                              Weight: {indicator.weight}
                            </span>
                            <span className="flex items-center gap-1">
                              <BarChart3 className="w-3 h-3" />
                              {indicator.measurementType.replace('_', ' ')}
                            </span>
                            <Badge variant={indicator.isActive ? 'default' : 'secondary'} className="text-xs">
                              {indicator.isActive ? 'Active' : 'Inactive'}
                            </Badge>
                          </div>
                        </div>
                        <ChevronDown 
                          className={`w-5 h-5 text-muted-foreground transition-transform duration-200 ${
                            expandedIndicators.has(indicator.id) ? 'rotate-180' : 'rotate-0'
                          }`}
                        />
                      </div>
                    </CollapsibleTrigger>

                    <CollapsibleContent>
                      <div className="border-t px-6 pb-6 pt-4">
                        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
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
                        
                        {/* Assessment Questions for this indicator */}
                        {assessmentQuestions.filter(q => q.behavioralIndicatorId === indicator.id).length > 0 && (
                          <div className="mt-8">
                            <h4 className="font-semibold mb-4 flex items-center gap-2">
                              <HelpCircle className="w-4 h-4" />
                              Assessment Questions ({assessmentQuestions.filter(q => q.behavioralIndicatorId === indicator.id).length})
                            </h4>
                            <div className="space-y-3">
                              {assessmentQuestions
                                .filter(q => q.behavioralIndicatorId === indicator.id)
                                .sort((a, b) => a.orderIndex - b.orderIndex)
                                .map((question) => (
                                  <Card key={question.id} className="border-muted">
                                    <CardContent className="p-4">
                                      <div className="flex items-start justify-between mb-3">
                                        <p className="text-sm flex-1 leading-relaxed">{question.questionText}</p>
                                        <Badge variant="outline" className={
                                          question.difficultyLevel === 'BASIC' ? 'border-emerald-500/30 text-emerald-600 bg-emerald-500/8' :
                                          question.difficultyLevel === 'INTERMEDIATE' ? 'border-yellow-500/30 text-yellow-600 bg-yellow-500/8' :
                                          question.difficultyLevel === 'ADVANCED' ? 'border-orange-500/30 text-orange-600 bg-orange-500/8' :
                                          'border-red-500/30 text-red-600 bg-red-500/8'
                                        }>
                                          {question.difficultyLevel}
                                        </Badge>
                                      </div>
                                      <div className="text-xs text-muted-foreground font-medium">
                                        {question.questionType.replace('_', ' ')}
                                      </div>
                                    </CardContent>
                                  </Card>
                                ))}
                            </div>
                          </div>
                        )}
                      </div>
                    </CollapsibleContent>
                  </Card>
                </Collapsible>
              ))}
              </div>
            </>
          ) : (
            <Card>
              <CardContent className="p-12 text-center">
                <Target className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
                <CardTitle className="mb-2">No Behavioral Indicators</CardTitle>
                <CardDescription>
                  This competency doesn't have any behavioral indicators defined yet.
                </CardDescription>
              </CardContent>
            </Card>
          )}
        </TabsContent>

        <TabsContent value="assessment" className="mt-6 space-y-6">
          {assessmentQuestions.length > 0 ? (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {assessmentQuestions
                .sort((a, b) => a.orderIndex - b.orderIndex)
                .map((question) => {
                  const indicator = competency.behavioralIndicators?.find(i => i.id === question.behavioralIndicatorId);
                  return (
                    <Card key={question.id}>
                      <CardHeader>
                        <div className="flex items-start justify-between">
                          <Badge variant="outline" className={
                            question.difficultyLevel === 'BASIC' ? 'border-emerald-500/30 text-emerald-600 bg-emerald-500/8' :
                            question.difficultyLevel === 'INTERMEDIATE' ? 'border-yellow-500/30 text-yellow-600 bg-yellow-500/8' :
                            question.difficultyLevel === 'ADVANCED' ? 'border-orange-500/30 text-orange-600 bg-orange-500/8' :
                            'border-red-500/30 text-red-600 bg-red-500/8'
                          }>
                            {question.difficultyLevel}
                          </Badge>
                        </div>
                        <CardTitle className="text-lg leading-relaxed">{question.questionText}</CardTitle>
                        {indicator && (
                          <CardDescription className="flex items-center gap-2">
                            <Info className="w-4 h-4" />
                            {indicator.title}
                          </CardDescription>
                        )}
                      </CardHeader>
                      <CardContent>
                        <div className="text-sm text-muted-foreground mb-6 flex items-center gap-2">
                          <HelpCircle className="w-4 h-4" />
                          {question.questionType.replace('_', ' ')}
                        </div>

                        {question.answerOptions && question.answerOptions.length > 0 && (
                          <div>
                            <h4 className="text-sm font-semibold mb-3 flex items-center gap-2">
                              <CheckCircle className="w-4 h-4" />
                              Answer Options:
                            </h4>
                            <div className="space-y-2">
                              {question.answerOptions.slice(0, 3).map((option, idx) => (
                                <div 
                                  key={idx} 
                                  className={`text-sm p-3 rounded-lg border ${
                                    option.correct 
                                      ? 'bg-emerald-500/5 border-emerald-500/20 text-emerald-700 dark:text-emerald-300' 
                                      : option.score && option.score >= 4
                                      ? 'bg-blue-500/5 border-blue-500/20 text-blue-700 dark:text-blue-300'
                                      : 'bg-muted border-muted text-muted-foreground'
                                  }`}
                                >
                                  <div className="flex items-center justify-between">
                                    <span className="flex items-center gap-2">
                                      {option.correct && (
                                        <CheckCircle className="w-4 h-4 text-emerald-500" />
                                      )}
                                      {option.text || option.label}
                                    </span>
                                    {option.score && (
                                      <span className="text-xs opacity-75 font-medium">({option.score}pt)</span>
                                    )}
                                  </div>
                                </div>
                              ))}
                              {question.answerOptions.length > 3 && (
                                <div className="text-xs text-muted-foreground text-center py-2 font-medium">
                                  +{question.answerOptions.length - 3} more options
                                </div>
                              )}
                            </div>
                          </div>
                        )}
                      </CardContent>
                    </Card>
                  );
                })}
            </div>
          ) : (
            <Card>
              <CardContent className="p-12 text-center">
                <HelpCircle className="w-16 h-16 text-muted-foreground mx-auto mb-4" />
                <CardTitle className="mb-2">No Assessment Questions</CardTitle>
                <CardDescription>
                  No assessment questions have been created for this competency's behavioral indicators yet.
                </CardDescription>
              </CardContent>
            </Card>
          )}
        </TabsContent>
      </Tabs>
    </div>
  );
};

export default CompetencyDetailPage;
