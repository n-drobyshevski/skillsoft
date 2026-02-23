package app.skillsoft.assessmentbackend.services.selection;

import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning;
import app.skillsoft.assessmentbackend.domain.dto.simulation.InventoryWarning.WarningLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SelectionWarningCollector Tests")
class SelectionWarningCollectorTest {

    @AfterEach
    void cleanup() {
        SelectionWarningCollector.clear();
    }

    @Test
    @DisplayName("drain returns empty list when no collector is active")
    void drainReturnsEmptyWhenNotActive() {
        List<InventoryWarning> result = SelectionWarningCollector.drain();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("begin and drain returns collected warnings")
    void beginAndDrainReturnsCollectedWarnings() {
        SelectionWarningCollector.begin();
        SelectionWarningCollector.addWarning(WarningLevel.WARNING, "test warning");

        List<InventoryWarning> result = SelectionWarningCollector.drain();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).message()).isEqualTo("test warning");
        assertThat(result.get(0).level()).isEqualTo(WarningLevel.WARNING);
    }

    @Test
    @DisplayName("addWarning is no-op when no collector is active")
    void addWarningIsNoOpWhenNotActive() {
        // Should not throw
        SelectionWarningCollector.addWarning(WarningLevel.ERROR, "orphaned warning");

        List<InventoryWarning> result = SelectionWarningCollector.drain();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("clear removes thread-local state")
    void clearRemovesState() {
        SelectionWarningCollector.begin();
        SelectionWarningCollector.addWarning(WarningLevel.WARNING, "will be cleared");

        SelectionWarningCollector.clear();

        assertThat(SelectionWarningCollector.isActive()).isFalse();
        assertThat(SelectionWarningCollector.drain()).isEmpty();
    }

    @Test
    @DisplayName("multiple warnings are all collected")
    void multipleWarningsAllCollected() {
        SelectionWarningCollector.begin();
        SelectionWarningCollector.addWarning(WarningLevel.WARNING, "first");
        SelectionWarningCollector.addWarning(WarningLevel.ERROR, "second");
        SelectionWarningCollector.addWarning(WarningLevel.WARNING, "third");

        List<InventoryWarning> result = SelectionWarningCollector.drain();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(InventoryWarning::message)
                .containsExactly("first", "second", "third");
    }

    @Test
    @DisplayName("drain clears state so second drain is empty")
    void drainClearsState() {
        SelectionWarningCollector.begin();
        SelectionWarningCollector.addWarning(WarningLevel.WARNING, "once");

        assertThat(SelectionWarningCollector.drain()).hasSize(1);
        assertThat(SelectionWarningCollector.drain()).isEmpty();
    }
}
