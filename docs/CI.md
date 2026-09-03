# Continuous Integration

TechStore uses two GitHub Actions workflows for pull requests targeting
`develop` or `main`:

- `Backend CI`: Java 21, Maven dependency cache, tests and application build.
- `Frontend CI`: Node.js 22, locked npm install, lint, tests and production build.

Both workflows can also be started manually with `workflow_dispatch`. They use
read-only repository permissions and cancel an older run when a newer commit is
pushed to the same pull request.

Backend Surefire reports and the Frontend JUnit report are retained as workflow
artifacts for seven days. Test and build output for both applications is also
available directly in the job logs.

## Required checks for `main`

The `main` branch is protected on GitHub. The active rule uses these settings:

1. Require a pull request before merging.
2. Require status checks to pass before merging.
3. Require branches to be up to date before merging.
4. Select `Backend CI` and `Frontend CI` as required checks.
5. Apply the rule to administrators; do not allow force-pushes or deletion.
6. Require pull-request conversations to be resolved before merging.

No approving reviewer is required because this is currently a single-developer
repository, but changes must still be delivered by Pull Request and pass both CI
checks. Increase the approval count when another reviewer joins the project.

The repository owner needs administration permission to change this protection.
Workflow files alone cannot change repository branch protection. Audit the live
setting under **Settings → Rules → Rulesets** or **Settings → Branches** after
changing repository ownership, plan, or CI job names.
