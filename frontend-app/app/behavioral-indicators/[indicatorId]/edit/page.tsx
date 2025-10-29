import { behavioralIndicatorsApi } from "@/services/api";
import { EditIndicatorForm } from "../../components/EditIndicatorForm";
import { notFound } from "next/navigation";

export default async function EditIndicatorPage({ params }: { params: { indicatorId: string } }) {
  const indicator = await behavioralIndicatorsApi.getIndicatorById((await params).indicatorId);

  if (!indicator) {
    notFound();
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Edit Behavioral Indicator</h1>
      <EditIndicatorForm indicator={indicator} />
    </div>
  );
}
