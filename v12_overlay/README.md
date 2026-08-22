# LightArt Studio v1.2

LightArt Studio is a lightweight Android player/editor for mathematical generative art.

## v1.2 UI changes
- Simplified home: create, search, play, edit. Advanced actions moved into menus.
- Project overflow menu: favorite, duplicate, export, validate, delete.
- Simplified editor with Save and Save & Play as the primary actions.
- Playback controls reduced to Back / Pause / Speed / Display Only / Menu.
- New **Display Only mode** hides all app controls plus Android status/navigation bars. Tap the artwork once to restore controls.
- Playback keeps the screen awake automatically.
- Speed cycles from the main playback bar or can be selected precisely from the menu.
- Added Organic Ribbon template based on the second p5.js-style formula supplied in the project conversation.
- Existing JSON import/export remains compatible; format version is now 3.

## Engine
- Java Android UI
- C/JNI expression engine
- OpenGL ES 2.0 point renderer
- Supported ABIs: arm64-v8a, armeabi-v7a, x86_64
