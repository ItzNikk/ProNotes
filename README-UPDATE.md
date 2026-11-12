# Pro Notes v9.5.1 — minor maths update

A focused maths and presentation update based on the supplied Pro Notes source archive. The original six files are retained, and the update adds the expanded on-device evaluator plus a guarded integration script for `EditorViewModel.kt`. Other project files are not replaced.

## What's new

- Better candidate selection: handwriting alternatives are checked against the maths evaluator before an answer is inserted.
- More than basic arithmetic: powers, implicit multiplication, percentages, factorials, square/cube roots, `pi` and `e`, trigonometric functions, logarithms, and common numeric functions.
- One-variable linear equations, for example `2x + 3 = 7`.
- Completed equalities are checked. An incorrect equality is rejected instead of being presented as a valid answer.
- An Apple Notes-inspired light sweep, colour-shifting trail and subtle answer halo run alongside the existing handwritten, character-by-character answer reveal.
- GitHub Actions builds the Android **release** APK and creates a uniquely tagged GitHub Release after a successful push to `main`.

## Apply the update

1. Make a backup or commit your current work. Extract this ZIP into the root of your existing `ProNotes` repository and keep the folder structure. Let the included files replace files at the same paths.
2. From the repository root, run the guarded integration script:

   ```bash
   python scripts/apply-viewmodel-update.py
   ```

   The script changes only the required lines in `app/src/main/java/com/pronotes/app/editor/EditorViewModel.kt`. It checks that every expected source fragment occurs exactly once and stops without writing if the base file has drifted. It is safe to run again.
3. Review the change set and optionally build locally:

   ```bash
   git status --short
   ./gradlew assembleRelease
   ```

   On Windows, use `gradlew.bat assembleRelease`.
4. Commit and push the update to `main`:

   ```bash
   git add .
   git commit -m "Release v9.5.1: improved maths and answer animation"
   git push origin main
   ```

## Automatic GitHub release

A successful push to `main` triggers `.github/workflows/build.yml`. It builds `app`'s release variant, uploads the APK as a workflow artifact, and publishes a GitHub Release named `Pro Notes 9.5.1.<run-number>` with tag `v9.5.1.<run-number>` and asset `ProNotes.apk`. The workflow intentionally does not trigger on the tag it creates, preventing a second build/release loop. Failed builds do not publish a release.

The workflow retains the project's existing optional signing-secret names: `KEYSTORE_B64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`, and `SUPPORT_UPI`. Configure the production release-keystore secrets for stable, install-over-existing-app signing. The workflow caches the debug keystore only as a fallback; that is not a substitute for your production signing key.

## Validation and limits

The standalone Kotlin maths engine compiled and passed smoke checks for arithmetic, powers, roots, trigonometry, logarithms, percentages, factorials, implicit multiplication, linear equations and rejection of incorrect equalities. The workflow YAML was parsed and its main-push/release conditions were checked. A full Android Gradle build was not run here, so the APK and GitHub Release are **not** yet built or published. They are created after the update is applied, committed and pushed, and GitHub Actions succeeds.
