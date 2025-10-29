import { behavioralIndicatorsApi } from "@/services/api";
import { EditIndicatorForm } from "../../components/EditIndicatorForm";
import { notFound } from "next/navigation";
import IndicatorPreview from "../../components/IndicatorPreview";

export default async function EditIndicatorPage({ params }: { params: { indicatorId: string } }) {
  const indicator = await behavioralIndicatorsApi.getIndicatorById((await params).indicatorId);

  if (!indicator) {
    notFound();
  }

  return (
    <div className="container mx-auto p-4">
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2">
          <EditIndicatorForm indicator={indicator} competencyId={indicator.competencyId} />
        </div>
        <div className="hidden lg:block">
          <IndicatorPreview indicator={indicator} />
        </div>
      </div>
    </div>
  );
}