# 🎬 Film Festival Management System - Design System & UI Guide

## Overview
This document describes the comprehensive modern, cinematic design system implemented for the Film Festival Management System frontend.

## Color Palette
```
PRIMARY:        #0a0e27 (Deep Black)
SECONDARY:      #1a1f3a (Dark Blue)
TERTIARY:       #2d3748 (Slate Gray)

ACCENT:         #fbbf24 (Gold)
ACCENT-NEON:    #f97316 (Orange)

SUCCESS:        #10b981 (Green)
DANGER:         #ef4444 (Red)
WARNING:        #f59e0b (Amber)
INFO:           #06b6d4 (Cyan)

TEXT-PRIMARY:   #f8fafc (Light)
TEXT-SECONDARY: #cbd5e1 (Medium)
TEXT-TERTIARY:  #94a3b8 (Dark)
TEXT-MUTED:     #64748b (Very Dark)
```

## Typography
- **Display Font**: Playfair Display (serif) - for headings
- **Body Font**: Inter (sans-serif) - for content
- **Font Weights**: 400 (Regular), 500 (Medium), 600 (Semibold), 700 (Bold), 800 (ExtraBold)

## Spacing System
- XS: 0.25rem (4px)
- SM: 0.5rem (8px)
- MD: 1rem (16px)
- LG: 1.5rem (24px)
- XL: 2rem (32px)
- 2XL: 3rem (48px)

## Border Radius
- SM: 0.375rem (6px)
- MD: 0.5rem (8px)
- LG: 0.75rem (12px)
- XL: 1rem (16px)
- 2XL: 1.5rem (24px)

## Components

### Buttons
- **Variants**: primary, secondary, success, danger, warning, info, outline, ghost
- **Sizes**: sm, md, lg
- **States**: hover, active, disabled, loading
- **Features**: Gradient backgrounds, smooth transitions, glow effects

### Cards
- **Types**: Default, Gradient, Neon, Hoverable, StatCard
- **Features**: Glassmorphism, smooth shadows, hover animations

### Input Fields
- **Types**: Text, Email, Password, Number, Select, Textarea, Checkbox, Radio
- **Features**: Focus states, error handling, icon support, validation

### Modals
- **Features**: Backdrop blur, smooth animations, responsive sizing
- **Sizes**: sm (400px), md (600px), lg (900px)

### Toast Notifications
- **Types**: success, error, warning, info
- **Position**: Bottom-right, stacks vertically
- **Auto-dismiss**: 4 seconds

### Sidebar
- **Position**: Fixed left
- **Features**: Icon-based navigation, active indicators, collapsible on mobile
- **Width**: 280px (desktop), full-width collapsed menu (mobile)

### Navbar
- **Position**: Fixed top
- **Features**: Search bar, notifications, user profile, responsive design
- **Height**: 80px

## Animation & Transitions
- **Fast**: 0.15s ease-in-out (micro interactions)
- **Base**: 0.3s ease-in-out (standard transitions)
- **Slow**: 0.5s ease-in-out (major animations)

### Key Animations
- fadeIn, slideInUp, slideInDown, slideInLeft, slideInRight
- scaleIn, pulse, shimmer, glow

## Effects
- **Glassmorphism**: 10-20px blur with semi-transparent backgrounds
- **Shadows**: Multiple depth levels from subtle to prominent
- **Glow Effects**: Accent colors create glowing halos on interactive elements

## Responsive Breakpoints
- **Desktop**: > 1024px
- **Tablet**: 768px - 1024px
- **Mobile**: < 768px

## Layout Structure
```
┌─────────────────────────────────────────┐
│           NAVBAR (Fixed)                │
├─────────────────────────────────────────┤
│ SIDEBAR │                               │
│         │     PAGE CONTENT               │
│ (Fixed) │     (Main Area)                │
│         │                               │
│         └───────────────────────────────┘
```

## Page Templates

### Dashboard/Home
- Header with date/time
- Stats grid (4 columns)
- Quick actions grid
- Activity feed
- Featured section
- Upcoming screenings

### Films
- Toolbar (search, view toggle, add button)
- Filters (genre, language)
- Grid or list view
- Film cards with metadata
- Edit/Delete actions
- Add/Edit modal

### Venues
- Similar structure to Films
- Venue cards with capacity
- Location information
- Add/Edit/Delete operations

### Screenings
- Calendar view (or list view)
- Drag-and-drop scheduling
- Conflict detection (red highlighting)
- Film-Venue-Time linkage

### Tickets (Premium Feature)
- Cinema-style seat selection
- Real-time availability
- Interactive seat map
- Ticket summary panel
- Booking confirmation

### Attendees
- Search and filters
- Table or card view
- Contact information
- Registration status
- Add/Edit/Delete operations

### Awards (Premium Feature)
- Toggle Film/Crew awards
- Dynamic dropdowns
- Award recipient display
- Award cards with trophy icons

### Film Crew
- Crew grouped by role
- Profile cards
- Contact information
- Link to films
- Add/Edit/Delete operations

## Best Practices

### Color Usage
- Use accent colors for interactive elements
- Use danger red for destructive actions
- Use success green for positive confirmations
- Use warning amber for cautions
- Use info cyan for informational content

### Spacing
- Maintain consistent padding/margins
- Use the spacing scale consistently
- Don't mix different spacing scales

### Typography
- Use Playfair Display for all headings
- Use Inter for body text
- Maintain proper font weight hierarchy
- Ensure sufficient line-height for readability

### Interactions
- Provide visual feedback on hover
- Use smooth transitions for state changes
- Show loading states for async operations
- Display error states clearly

### Accessibility
- Maintain sufficient color contrast
- Support keyboard navigation
- Include focus indicators
- Use semantic HTML

## File Structure
```
src/
├── styles/
│   ├── global.css       (Design system variables, base styles)
│   └── pages.css        (Legacy component styling)
├── components/
│   ├── ui/              (Reusable UI components)
│   │   ├── Button.jsx/css
│   │   ├── Card.jsx/css
│   │   ├── Input.jsx/css
│   │   ├── Modal.jsx/css
│   │   └── Toast.jsx/css
│   └── layout/          (Layout components)
│       ├── Sidebar.jsx/css
│       └── Layout.jsx/css
└── pages/               (Page components with CSS)
    ├── Home.jsx/css
    ├── Films.jsx/css
    ├── Venues.jsx
    ├── Tickets.jsx
    ├── Attendees.jsx
    ├── Awards.jsx
    ├── FilmCrew.jsx
    └── Screenings.jsx
```

## Usage Examples

### Using a Button
```jsx
import { Button } from '../components/ui';

<Button variant="primary" size="lg" onClick={handleClick} icon="➕">
  Add Film
</Button>
```

### Using a Card
```jsx
import { Card, CardBody, CardHeader } from '../components/ui';

<Card hoverable gradient>
  <CardHeader>
    <h3>Title</h3>
  </CardHeader>
  <CardBody>
    Content goes here
  </CardBody>
</Card>
```

### Using Modals
```jsx
import { Modal, useModal } from '../components/ui';

const modal = useModal();

<Modal isOpen={modal.isOpen} onClose={modal.close} title="Edit">
  Form content
</Modal>

<Button onClick={modal.open}>Open</Button>
```

### Showing Toasts
```jsx
import { showToast } from '../components/ui';

showToast('Success!', 'success');
showToast('Error occurred', 'error');
```

## Future Enhancements
1. Dark/Light theme toggle
2. Custom color schemes
3. Animation preference settings
4. Additional component variants
5. Storybook integration
6. Accessibility audit
7. Performance optimization
8. Internationalization (i18n)

---

**Last Updated**: April 2024
**Version**: 1.0
**Design Lead**: AI Assistant
