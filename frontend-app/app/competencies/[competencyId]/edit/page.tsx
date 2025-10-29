import { competenciesApi } from "@/services/api";
import { EditCompetencyForm } from "../../components/EditCompetencyForm";
import { notFound } from "next/navigation";

export default async function EditCompetencyPage({ params }: { params: { competencyId: string } }) {
  const competency = await competenciesApi.getCompetencyById(params.competencyId);

  if (!competency) {
    notFound();
  }

  return (
    <div className="container mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Edit Competency</h1>
      <EditCompetencyForm competency={competency} />
    </div>
  );
}