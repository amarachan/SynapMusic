# Contributing to SynapMusic

First off, thank you for considering contributing to SynapMusic! It's people like you that make this project a great tool for the community.

## 🛠 Project Architecture
SynapMusic uses a **Multi-Module** architecture to ensure performance and clean separation of concerns.

*   **`:app`**: The main UI (Jetpack Compose), Player Service, and Database.
*   **`:pulse-engine`**: Our custom engine for talking to YouTube Music.
*   **`:lyric-flow`**: The logic for fetching and rendering synchronized lyrics.
*   **`:compose-routing` / `:compose-persist` / `:compose-reordering`**: Internal libraries for navigation, state persistence, and drag-and-drop reordering.
*   **`:ktor-client-brotli`**: Brotli HTTP compression interceptor module.

## 🚀 Getting Started
1.  Fork the repository and clone it to your machine.
2.  Open the project in **Android Studio (Ladybug or newer)**.
3.  Build the project using the `assembleDebug` task to ensure all native binaries are linked correctly.

## 🐛 Reporting Bugs
If you find a bug, please open an issue and include:
*   Your device model and Android version.
*   Steps to reproduce the bug.
*   If possible, a screen recording or logs from Logcat.

## 💡 Suggesting Features
We love new ideas! Please check the existing issues to see if your feature has already been suggested. If not, open a new issue with the "Enhancement" label.

## 🌍 Translations
You don't need to be a developer to contribute! We want SynapMusic to be accessible to everyone.
*   To contribute a translation, please edit the `strings.xml` files in `app/src/main/res/values-XX/`.
*   If your language is missing, create a new folder following the Android locale naming convention.
*   *Note*: If English is not your native language, you are welcome to use AI or machine translation tools for translating communications or string resources.

## 📝 Coding Style
*   **Custom Design System:** We do **not** use `androidx.compose.material` or `material3`. All UI components must use our custom primitives (`com.synap.music.ui.styling.*`) to maintain rendering speed and the dynamic Palette theming.
*   **UI-First:** All UI is built using Jetpack Compose Foundation. Follow modern best practices (state hoisting, stable models, etc.).
*   **Fidelity Matters:** Always keep audio quality in mind. If you're adding a feature that touches the audio path, ensure it respects lossless integrity.
*   **Clean Architecture:** Respect the module boundaries. Keep API logic in `:pulse-engine` and UI logic in `:app`.
*   **Asset Management:** Complex vectors (like `icon-image.svg`) must be rendered to PNG (`ic_banner_image.png`) using `rsvg-convert` before inclusion in the project. This is due to Android TV limitations with parsing complex SVGs natively in `res/drawable` XML wrappers. Check the README for details on the TV banner structure.

## 📥 Submitting Changes
1.  Create a new branch for your feature or fix.
2.  Submit a Pull Request (PR) to the `main` branch.
3.  Link the PR to the relevant issue if applicable.

## 🤝 Community & Respect
We want SynapMusic to be a welcoming place for everyone. Please **be respectful** to all contributors and community members. We have a zero-tolerance policy for abuse, harassment, or any form of toxicity. Let's keep the focus on building a great all-in-one music player together.

## ⚖️ License
By contributing, you agree that your contributions will be licensed under the **GPL v3 License**.

Let's improve the SynapMusic! 🎶
