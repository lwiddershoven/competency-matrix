/**
 * Matrix Overview JavaScript
 * Feature: 004-matrix-overview
 * Tasks: T038-T040 - Tooltip positioning and Popover API integration
 */

/**
 * T038-T039: Popover API event listeners for tooltip positioning
 * Listens for popover toggle events and positions tooltips correctly
 */
document.addEventListener('DOMContentLoaded', function() {
    const showTimeouts = new Map();
    const hideTimeouts = new Map();
    const tooltipPortal = document.createElement('div');
    tooltipPortal.id = 'matrix-tooltip-portal';
    document.body.appendChild(tooltipPortal);
    /**
     * T040: Viewport boundary detection for tooltips
     * Ensures tooltips stay within viewport bounds
     */
    document.addEventListener('beforetoggle', function(event) {
        // Only handle tooltip popovers
        if (!event.target.matches('[popover].tooltip-popover') || event.newState !== 'open') {
            return;
        }

        const tooltip = event.target;
        if (tooltip.parentElement !== tooltipPortal) {
            tooltipPortal.appendChild(tooltip);
        }
        const trigger = document.querySelector(`[popovertarget="${tooltip.id}"]`);

        if (!trigger) {
            return;
        }

        const triggerRect = trigger.getBoundingClientRect();
        const tooltipRect = tooltip.getBoundingClientRect();

        // Calculate initial position (below trigger with 8px gap)
        let top = triggerRect.bottom + 8;
        let left = triggerRect.left;

        // Viewport boundary detection
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        const padding = 8;

        // Flip vertically if doesn't fit below
        if (top + tooltipRect.height > viewportHeight - padding) {
            // Try positioning above
            const topAbove = triggerRect.top - tooltipRect.height - padding;
            if (topAbove >= padding) {
                top = topAbove;
            } else {
                // If doesn't fit above or below, position at top with padding
                top = padding;
            }
        }

        // Shift horizontally to stay in viewport
        if (left + tooltipRect.width > viewportWidth - padding) {
            left = viewportWidth - tooltipRect.width - padding;
        }
        if (left < padding) {
            left = padding;
        }

        // Apply positioning
        tooltip.style.position = 'fixed';
        tooltip.style.top = `${top}px`;
        tooltip.style.left = `${left}px`;
    });

    /**
     * Show popover on hover for better UX
     * The Popover API with popovertarget attribute handles show/hide automatically,
     * but we enhance it with manual control for better hover experience
     */
    document.querySelectorAll('.level-badge-button').forEach(function(button) {
        const tooltipId = button.getAttribute('popovertarget');
        const tooltip = tooltipId ? document.getElementById(tooltipId) : null;

        if (!tooltip) {
            return;
        }

        button.addEventListener('mouseenter', function() {
            clearTimeout(hideTimeouts.get(tooltipId));
            // Delay showing tooltip by 300ms (WCAG compliance)
            const showTimeout = setTimeout(function() {
                try {
                    tooltip.showPopover();
                } catch (e) {
                    // Fallback for browsers without Popover API support
                    console.log('Popover API not supported');
                }
            }, 300);
            showTimeouts.set(tooltipId, showTimeout);
        });

        button.addEventListener('mouseleave', function() {
            clearTimeout(showTimeouts.get(tooltipId));
            // Hide after a short delay to allow moving to tooltip
            const hideTimeout = setTimeout(function() {
                try {
                    tooltip.hidePopover();
                } catch (e) {
                    // Fallback for browsers without Popover API support
                }
            }, 100);
            hideTimeouts.set(tooltipId, hideTimeout);
        });

        tooltip.addEventListener('mouseenter', function() {
            clearTimeout(hideTimeouts.get(tooltipId));
        });

        tooltip.addEventListener('mouseleave', function() {
            const hideTimeout = setTimeout(function() {
                try {
                    tooltip.hidePopover();
                } catch (e) {
                    // Fallback for browsers without Popover API support
                }
            }, 100);
            hideTimeouts.set(tooltipId, hideTimeout);
        });
    });

    /**
     * Keep tooltip visible when hovering over it
     */
    document.querySelectorAll('.tooltip-popover').forEach(function(tooltip) {
        tooltip.addEventListener('beforetoggle', function(event) {
            if (event.newState === 'open') {
                const tooltipId = tooltip.id;
                clearTimeout(hideTimeouts.get(tooltipId));
                clearTimeout(showTimeouts.get(tooltipId));
            }
        });
    });

    /**
     * Database reload with category filter preservation
     */
    const reloadButton = document.getElementById('reload-button');
    if (reloadButton) {
        // Handle click to capture category name and show confirmation
        reloadButton.addEventListener('click', function(event) {
            // Show confirmation dialog
            if (!confirm('This will delete all current data and reload from seed files. Continue?')) {
                event.preventDefault();
                return;
            }

            // Capture selected category name (display text, not ID)
            const categorySelect = document.getElementById('categoryFilter');
            let selectedCategoryName = null;

            if (categorySelect && categorySelect.value) {
                // Get the display text of selected option (e.g., "Programming")
                selectedCategoryName = categorySelect.selectedOptions[0]?.textContent.trim();
            }

            // Store in button dataset for hx-vals to access
            reloadButton.dataset.categoryName = selectedCategoryName || '';
        });

        // Handle response and redirect to URL from backend
        document.body.addEventListener('htmx:afterRequest', function(event) {
            // Only handle reload button responses
            if (event.detail.target !== reloadButton) return;

            if (event.detail.xhr.status === 200) {
                try {
                    const response = JSON.parse(event.detail.xhr.responseText);

                    if (response.success && response.redirectUrl) {
                        // Use redirect URL from backend (preserves category filter)
                        window.location.href = response.redirectUrl;
                    } else {
                        // Fallback to default matrix page
                        console.warn('Reload response missing redirectUrl, falling back to /matrix');
                        window.location.href = '/matrix';
                    }
                } catch (e) {
                    // JSON parse error - fallback to default
                    console.error('Failed to parse reload response:', e);
                    window.location.href = '/matrix';
                }
            } else {
                // HTTP error - show error and stay on page
                console.error('Reload failed with status:', event.detail.xhr.status);
                alert('Database reload failed. Please check the logs and try again.');
            }
        });
    }
});
