<!--
Thanks for contributing to Testcontainers. Please review the following notes before
submitting a pull request.

New Modules:

If you are contributing a new module, please add your module name to the following files:

* `./.github/ISSUE_TEMPLATE/bug_report.yaml`
* `./.github/ISSUE_TEMPLATE/enhancement.yaml`
* `./.github/ISSUE_TEMPLATE/feature.yaml`
* `./.github/dependabot.yml`
* `./.github/labeler.yml`

Also make sure that your new module has the appropriate documentation under `./docs/modules/`

Before committing any change, please run `./gradlew checkstyleMain checkstyleTest spotlessApply` and fix any issues that occur.

Dependency Upgrades:
Please do not open a pull request to update only a version dependency. Existing process will perform
the upgrade every week. However, if the upgrade involves more changes (changes for deprecated API) then
your pull request is very welcome.

Describing Your Changes:

If, after having reviewed the notes above, you're ready to submit your pull request, please
provide a brief description of the proposed changes and their context. If they fix a bug, please
describe the broken behaviour and how the changes fix it. If they make an enhancement,
please describe the new functionality and why you believe it's useful. If your pull
request relates to any existing issues, please reference them by using the issue number
prefixed with #.
-->
