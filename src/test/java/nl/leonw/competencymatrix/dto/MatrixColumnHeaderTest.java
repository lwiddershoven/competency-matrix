package nl.leonw.competencymatrix.dto;

import nl.leonw.competencymatrix.model.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MatrixColumnHeader DTO.
 * Feature: 004-matrix-overview
 * Tests: T011 (abbreviation generation)
 */
class MatrixColumnHeaderTest {

    // T011: Abbreviation generation tests

    @Test
    void from_createsHeaderWithRole() {
        Role role = new Role(1, "Developer", "A developer", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals(role, header.role());
        assertNotNull(header.abbreviation());
    }

    @Test
    void from_throwsOnNullRole() {
        assertThrows(IllegalArgumentException.class,
            () -> MatrixColumnHeader.from(null));
    }

    @Test
    void abbreviation_keepsShortNamesUnchanged() {
        // Names ≤15 chars should be kept as-is
        Role role = new Role(1, "Junior Dev", "Junior Developer", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("Junior Dev", header.abbreviation());
    }

    @Test
    void abbreviation_keepsExactly15CharsUnchanged() {
        // Exactly 15 chars should be kept as-is
        Role role = new Role(1, "A".repeat(15), "Description", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("A".repeat(15), header.abbreviation());
    }

    @Test
    void abbreviation_extractsUppercaseLettersForLongNames() {
        // Names >15 chars should extract uppercase letters
        Role role = new Role(1, "Software Architect", "An architect", "Architect", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("SA", header.abbreviation());
    }

    @Test
    void abbreviation_limitsToThreeUppercaseLetters() {
        // Should extract max 3 uppercase letters
        Role role = new Role(1, "Lead Developer / Software Architect", "Lead dev and architect", "Architect", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("LDS", header.abbreviation());
    }

    @Test
    void abbreviation_fallsBackToFirstThreeCharsWhenNoUppercase() {
        // If no uppercase letters, take first 3 chars
        Role role = new Role(1, "very long lowercase name here", "Description", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("ver", header.abbreviation());
    }

    @Test
    void abbreviation_handlesEmptyName() {
        // Empty name should throw exception due to validation
        Role role = new Role(1, "", "Description", "Developer", 1);
        assertThrows(IllegalArgumentException.class, () -> MatrixColumnHeader.from(role));
    }

    @Test
    void abbreviation_handlesSingleUppercaseLetter() {
        // Name longer than 15 chars with only one uppercase letter should still take first 3 chars as fallback
        Role role = new Role(1, "architect with one uppercase", "Description", "Architect", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertEquals("arc", header.abbreviation());
    }

    @Test
    void needsTooltip_returnsTrueForLongNames() {
        Role role = new Role(1, "Software Architect", "An architect", "Architect", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertTrue(header.needsTooltip());
    }

    @Test
    void needsTooltip_returnsFalseForShortNames() {
        Role role = new Role(1, "Junior Dev", "Junior Developer", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertFalse(header.needsTooltip());
    }

    @Test
    void needsTooltip_returnsFalseForExactly15Chars() {
        Role role = new Role(1, "A".repeat(15), "Description", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertFalse(header.needsTooltip());
    }

    @Test
    void needsTooltip_returnsTrueFor16Chars() {
        Role role = new Role(1, "A".repeat(16), "Description", "Developer", 1);
        MatrixColumnHeader header = MatrixColumnHeader.from(role);

        assertTrue(header.needsTooltip());
    }

    @Test
    void canonicalConstructor_throwsOnNullRole() {
        assertThrows(IllegalArgumentException.class,
            () -> new MatrixColumnHeader(null, "Abbrev"));
    }

    @Test
    void canonicalConstructor_throwsOnNullAbbreviation() {
        Role role = new Role(1, "Developer", "A developer", "Developer", 1);
        assertThrows(IllegalArgumentException.class,
            () -> new MatrixColumnHeader(role, null));
    }

    @Test
    void canonicalConstructor_throwsOnEmptyAbbreviation() {
        Role role = new Role(1, "Developer", "A developer", "Developer", 1);
        assertThrows(IllegalArgumentException.class,
            () -> new MatrixColumnHeader(role, ""));
    }
}
