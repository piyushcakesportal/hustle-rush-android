The previous GitHub upload mixed file contents and filenames.
For example, build.gradle.kts received Android icon XML and README.md received app/build.gradle.kts content.

Recommended fix:
1. Extract this ZIP completely.
2. Open the extracted hustle-rush-android folder.
3. Double-click PUSH_TO_GITHUB.bat.
4. Sign in to GitHub if a browser window opens.
5. The script force-replaces the broken main branch with the complete project structure.
6. Open GitHub > Actions > Build Android.

Do not manually create or copy individual project files in GitHub.
