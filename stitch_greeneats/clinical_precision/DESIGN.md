---
name: Clinical Precision
colors:
  surface: '#f8fafb'
  surface-dim: '#d8dadb'
  surface-bright: '#f8fafb'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f2f4f5'
  surface-container: '#eceeef'
  surface-container-high: '#e6e8e9'
  surface-container-highest: '#e1e3e4'
  on-surface: '#191c1d'
  on-surface-variant: '#444748'
  inverse-surface: '#2e3132'
  inverse-on-surface: '#eff1f2'
  outline: '#747878'
  outline-variant: '#c4c7c7'
  surface-tint: '#5f5e5e'
  primary: '#000000'
  on-primary: '#ffffff'
  primary-container: '#1c1b1b'
  on-primary-container: '#858383'
  inverse-primary: '#c8c6c5'
  secondary: '#006c46'
  on-secondary: '#ffffff'
  secondary-container: '#7bfabb'
  on-secondary-container: '#00734b'
  tertiary: '#000000'
  on-tertiary: '#ffffff'
  tertiary-container: '#1c1b1a'
  on-tertiary-container: '#868381'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#e5e2e1'
  primary-fixed-dim: '#c8c6c5'
  on-primary-fixed: '#1c1b1b'
  on-primary-fixed-variant: '#474646'
  secondary-fixed: '#7bfabb'
  secondary-fixed-dim: '#5ddda1'
  on-secondary-fixed: '#002112'
  on-secondary-fixed-variant: '#005234'
  tertiary-fixed: '#e6e1df'
  tertiary-fixed-dim: '#cac6c3'
  on-tertiary-fixed: '#1c1b1a'
  on-tertiary-fixed-variant: '#484645'
  background: '#f8fafb'
  on-background: '#191c1d'
  surface-variant: '#e1e3e4'
  border-subtle: '#E2E8F0'
  border-strong: '#121212'
  surgical-green: '#00A36C'
  clinical-charcoal: '#121212'
  pure-white: '#FFFFFF'
typography:
  display-lg:
    fontFamily: Inter
    fontSize: 48px
    fontWeight: '700'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '600'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '500'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.6'
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.05em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1'
rounded:
  sm: 0.125rem
  DEFAULT: 0.25rem
  md: 0.375rem
  lg: 0.5rem
  xl: 0.75rem
  full: 9999px
spacing:
  base: 8px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  gutter: 16px
  margin-mobile: 20px
  margin-desktop: 64px
---

## Brand & Style

This design system is defined by a "Clinical Precision" aesthetic—a fusion of extreme minimalism and medical-grade utility. The brand personality is disciplined, intellectual, and transparent, aimed at users who value data accuracy and mental clarity in their health journey. 

The visual style is **Minimalist** with a **Wireframe-Inspired** execution. It rejects the "softness" of typical consumer health apps in favor of a structured, high-contrast environment. By utilizing thin line work and expansive whitespace, the interface mimics the clarity of a high-end diagnostic report. The emotional response should be one of calm control, professionalism, and absolute focus.

## Colors

The palette is strictly functional. The foundation is **Pure White (#FFFFFF)**, ensuring the interface feels sterile and open. **Clinical Charcoal (#121212)** provides the primary contrast for typography and structural borders. 

**Surgical Green (#00A36C)** is our singular chromatic accent. It must be used with surgical precision: reserved exclusively for "Success" states, positive trend indicators, and the final "Primary Action" in a flow. A secondary **Slate Gray** is used for non-interactive borders and secondary information to prevent visual clutter.

## Typography

Using **Inter**, the typography system relies on weight and scale rather than color to create hierarchy. 

- **Asymmetry:** Large display headers should be left-aligned with significant top-padding to create an editorial, modern feel.
- **Micro-copy:** Labels use all-caps with increased letter spacing to emulate technical blueprints.
- **Vertical Rhythm:** All line-heights are set to increments of 4px to align with the 8px baseline grid.

## Layout & Spacing

The system follows a **Strict 8px Baseline Grid**. All components, padding, and margins must be multiples of 8.

- **Grid Model:** A 12-column fixed grid for desktop, and a 4-column fluid grid for mobile.
- **Structural Alignment:** Use "Wireframe" lines (1px width, #E2E8F0) to separate sections rather than margins alone. This maintains the clinical, organized aesthetic.
- **Negative Space:** Headers should have 2x the standard padding (40px+) to isolate them from data density below.

## Elevation & Depth

This design system avoids traditional shadows and depth. It is a **flat, layered system** that uses **Low-contrast Outlines** for separation.

- **Stacking:** Depth is conveyed through 1px strokes. 
- **Active State:** To show interaction, an element does not "lift" (no shadow); instead, its border weight increases or changes color to #121212.
- **Modals:** Use a solid 1px border with a pure white background. Background dimming should be a very light, low-opacity gray (#121212 at 10%) to keep the UI bright.

## Shapes

The shape language is **Technical and Sharp**. 

A standard border radius of **4px (Soft)** is the maximum allowed for components like buttons and inputs. Large containers and cards should use **0px (Sharp)** corners to emphasize the architectural and clinical nature of the design. Icons must use a consistent 1.5px stroke weight with square caps.

## Components

### Buttons
- **Primary:** Solid #121212 background with #FFFFFF text. 4px radius. No shadow.
- **Success/Action:** Solid #00A36C background. Used only for "Confirm" or "Complete."
- **Ghost:** 1px border (#E2E8F0) with #121212 text. Border becomes #121212 on hover.

### Input Fields
- Underline style or 4-sided 1px stroke (#E2E8F0). 
- Focused state: Border color changes to #121212. 
- Label sits above the field in `label-md` style (uppercase).

### Cards
- No shadows. Use a 1px stroke (#E2E8F0) for the container.
- For data-heavy cards, use a vertical "accent line" (2px) of Surgical Green on the left edge to indicate active or healthy metrics.

### Chips & Tags
- Rectangular with 2px radius. 
- Light gray background (#F8FAFB) with `label-sm` text.

### Iconography
- All icons must be **1.5px stroke weight**.
- No filled icons; use "outline-only" to maintain the wireframe look.