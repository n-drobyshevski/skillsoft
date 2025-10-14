package app.skillsoft.assessmentbackend.domain.entities;

public enum ApprovalStatus {
    DRAFT("Черновик", "Начальное создание"),
    PENDING_REVIEW("На рассмотрении", "Ожидает валидации"),
    APPROVED("Утверждено", "Проверено и активно"),
    REJECTED("Отклонено", "Не соответствует стандартам"),
    ARCHIVED("Архивировано", "Больше не используется"),
    UNDER_REVISION("На доработке", "Обновляется");

    ApprovalStatus(String черновик, String s) {
    }
}