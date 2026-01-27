package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.ProficiencyLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatrixCell DTO.
 * Feature: 004-matrix-overview
 * Tests: T009 (factory methods), T010 (CSS class generation)
 */
class MatrixCellTest {

    // T009: Factory method tests
    @Test
    void withLevel_createsNonEmptyCell() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.BASIC);

        assertEquals(1L, cell.skillId());
        assertEquals(2, cell.roleId());
        assertEquals(ProficiencyLevel.BASIC, cell.requiredLevel());
        assertFalse(cell.isEmpty());
    }

    @Test
    void withLevel_throwsOnNullSkillId() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixCell.withLevel(null, 2, ProficiencyLevel.BASIC));
    }

    @Test
    void withLevel_throwsOnNullRoleId() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixCell.withLevel(1L, null, ProficiencyLevel.BASIC));
    }

    @Test
    void withLevel_throwsOnNullLevel() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixCell.withLevel(1L, 2, null));
    }

    @Test
    void empty_createsEmptyCell() {
        MatrixCell cell = MatrixCell.empty(1L, 2);

        assertEquals(1L, cell.skillId());
        assertEquals(2, cell.roleId());
        assertNull(cell.requiredLevel());
        assertTrue(cell.isEmpty());
    }

    @Test
    void empty_throwsOnNullSkillId() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixCell.empty(null, 2));
    }

    @Test
    void empty_throwsOnNullRoleId() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixCell.empty(1L, null));
    }

    @Test
    void canonicalConstructor_throwsWhenEmptyWithLevel() {
        assertThrows(IllegalArgumentException.class,
            () -> new MatrixCell(1L, 2, ProficiencyLevel.BASIC, true));
    }

    @Test
    void canonicalConstructor_throwsWhenNonEmptyWithoutLevel() {
        assertThrows(IllegalArgumentException.class,
            () -> new MatrixCell(1L, 2, null, false));
    }

    // T010: CSS class generation tests
    @Test
    void getLevelCssClass_returnsCorrectClassForBasic() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.BASIC);
        assertEquals("level-basic", cell.getLevelCssClass());
    }

    @Test
    void getLevelCssClass_returnsCorrectClassForDecent() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.DECENT);
        assertEquals("level-decent", cell.getLevelCssClass());
    }

    @Test
    void getLevelCssClass_returnsCorrectClassForGood() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.GOOD);
        assertEquals("level-good", cell.getLevelCssClass());
    }

    @Test
    void getLevelCssClass_returnsCorrectClassForExcellent() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.EXCELLENT);
        assertEquals("level-excellent", cell.getLevelCssClass());
    }

    @Test
    void getLevelCssClass_returnsEmptyForEmptyCell() {
        MatrixCell cell = MatrixCell.empty(1L, 2);
        assertEquals("", cell.getLevelCssClass());
    }

    @Test
    void getDisplayText_returnsLevelDisplayName() {
        MatrixCell cell = MatrixCell.withLevel(1L, 2, ProficiencyLevel.BASIC);
        assertEquals("Basic", cell.getDisplayText());
    }

    @Test
    void getDisplayText_returnsEmptyForEmptyCell() {
        MatrixCell cell = MatrixCell.empty(1L, 2);
        assertEquals("", cell.getDisplayText());
    }
}
