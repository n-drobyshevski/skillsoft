import { assessmentQuestionsApi, behavioralIndicatorsApi } from "@/services/api";
import { EditQuestionForm } from "../../components/EditQuestionForm";
import { notFound } from "next/navigation";
import Header from "../../../components/Header";
import QuestionPreview from "../../components/QuestionPreview";

export default async function EditQuestionPage({ params }: { params: { questionId: string } }) {
  const question = await assessmentQuestionsApi.getQuestionById((await params).questionId);

  if (!question) {
    notFound();
  }

  const indicator = await behavioralIndicatorsApi.getIndicatorById(question.behavioralIndicatorId);

  if (!indicator) {
    // Or handle this case gracefully
    notFound();
  }

  return (
    <div className="container mx-auto p-4">
      <Header
        title="Edit Assessment Question"
        subtitle="Make changes to your question and see a live preview."
        breadcrumbs={[
          { label: "Questions", href: "/assessment-questions" },
          { label: question.questionText, href: `/assessment-questions/${question.id}` },
          { label: "Edit", href: `/assessment-questions/${question.id}/edit` },
        ]}
      />
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <EditQuestionForm question={question} competencyId={indicator.competencyId} />
        </div>
        <div className="hidden lg:block">
          <QuestionPreview question={question} />
        </div>
      </div>
    </div>
  );
}