# Release process

Testcontainers' release process is semi-automated through GitHub Actions. This describes the basic steps for a project member to perform a release.

## Steps

1. Ensure that the master branch is building and that tests are passing.
1. Create a new release on GitHub. **The tag name is used as the version**, so please keep the tag name plain (e.g. 1.2.3).
1. The release triggers a GitHub Action workflow.
1. Log in to [Sonatype](https://oss.sonatype.org/) to check the staging repository.
    * Getting access to Sonatype requires a Sonatype JIRA account and [raising an issue](https://issues.sonatype.org/browse/OSSRH-74229), requesting access. 
3. Get the staging URL from Sonatype after GitHub Action workflow finished. The general URL format should be `https://oss.sonatype.org/service/local/repositories/$staging-repo-id/content/`
4. Manually test the release with the staging URL as maven repository URL (e.g. critical issues and features).
5. Run [TinSalver](https://github.com/bsideup/tinsalver) from GitHub using `npx` to sign artifact (see [TinSalver README](https://github.com/bsideup/tinsalver/blob/main/README.md)).
    * For TinSalver to correctly work with keybase on WSL on Windows, you might need to disable pinentry: `keybase config set -b pinentry.disabled true`.
7. Close the release in Sonatype. This will evaluate the release based on given Sonatype rules.
8. After successful closing, the release button needs to be clicked and afterwards it is automatically synced to Maven Central.
9. Handcraft and polish some of the release notes (e.g. substitute combinded dependency PRs and highlight certain features).
10. When available through Maven Central, poke [Richard North](https://github.com/rnorth) to announce the release on Twitter!

## Internal details

* The process is done with GitHub Actions, TinSalver and Sonatype.
* Sonatype will automatically promote the staging release to Maven Central.
* Keybase needs to be installed on the developer machine.
* GPG key of signing developer needs to be uplodaed to the [Ubuntu keyserver](https://keyserver.ubuntu.com/) (or other server supported by Sonatype).
