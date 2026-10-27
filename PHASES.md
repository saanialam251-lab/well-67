# Phases (from the project PDF)
- [x] Phase 0 - Skeleton that builds
- [x] Phase 1 - Home and splash (theme, animations)
- [x] Phase 2 - Board core: infinite grid, pan and zoom, tool bar, undo, minimise-able panel
- [x] Phase 3 - 2D shapes: labels, colour-coded angles, overlap decluttering, edit handles, double-tap menu with Details
- [x] Phase 4 - Graphs and derivatives: all functions, tangent lines, typing shortcuts
- [x] Phase 5 - Solve: pen, highlighter, eraser, sketch recognition
- [ ] Phase 6 - Trigonometry: unit circle, triangles, parallel lines
- [ ] Phase 7 - 3D solids: all solids plus prism and pyramid from your own shapes
- [ ] Phase 8 - Polish: layers, long-press tooltips, tablet sizes, saving boards

Reference for every port: web-reference/www/index.html (v0.11). Nothing in the acceptance checklist is dropped.

## Phase 3 - still to come inside later phases
- Triangle extras not yet ported: altitudes, incircle, circumcircle, orthocentre, bisectors (Phase 6, with the trig tools)
- Bisect a line (5 taps) and the parallel-lines angle tool: Phase 6
- 3D prism / pyramid buttons are in the edit panel but switch on in Phase 7
- Sketch (rough shape to neat figure): Phase 5

## Phase 4 - notes
- Graphs (Graph and Derivative tools), implicit equations (x^2+y^2=25), f' / f'', tap tangent, typing shortcuts: done
- Equations and Details use a built-in maths layout (stacked fractions, powers, roots), so no WebView/KaTeX is needed
- Still on the list: the Derivatives topic's own extras are all in; trig graphs of the unit circle come in Phase 6

## Phase 5 - notes
- Pen, highlighter (colours + width), eraser, sketch recognition (line, triangle, rectangle, quadrilateral, circle) in every topic that has the tools
- Fix in this version: two compile errors from earlier phases (an unsupported `catch (_: ...)` and a missing import)
- The build workflow now prints a "Build failed" box on the Actions run page and saves build.log

## Phase 6a - this version
- Partial eraser (only the part you touch) with size and whole-stroke mode
- Thin, crisp angle marks (blur removed)
- Quadrilateral measurements editable (4 sides + diagonal AC); pentagon and hexagon tools; Regular button
- Parallel lines: automatic detection, numbered angles 1-8, family chips, tap an angle, bisectors, facts in Details
- Lines that close up become a shape (auto-join, on/off with the link button); points/corners snap together
- Triangle: medians, altitudes, bisectors, centroid, orthocentre, incircle, circumcircle, angle at centre; cyclic quadrilaterals
- Undo / redo with named steps for every kind of change
- Still to do (Phase 6b): unit circle and the trig triangle
