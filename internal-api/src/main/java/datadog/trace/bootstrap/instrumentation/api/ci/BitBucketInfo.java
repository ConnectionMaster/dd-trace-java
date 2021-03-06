package datadog.trace.bootstrap.instrumentation.api.ci;

class BitBucketInfo extends CIProviderInfo {

  // https://support.atlassian.com/bitbucket-cloud/docs/variables-and-secrets/
  public static final String BITBUCKET = "BITBUCKET_COMMIT";
  public static final String BITBUCKET_PROVIDER_NAME = "bitbucket";
  public static final String BITBUCKET_PIPELINE_ID = "BITBUCKET_PIPELINE_UUID";
  public static final String BITBUCKET_REPO_FULL_NAME = "BITBUCKET_REPO_FULL_NAME";
  public static final String BITBUCKET_BUILD_NUMBER = "BITBUCKET_BUILD_NUMBER";
  public static final String BITBUCKET_WORKSPACE_PATH = "BITBUCKET_CLONE_DIR";
  public static final String BITBUCKET_GIT_REPOSITORY_URL = "BITBUCKET_GIT_SSH_ORIGIN";
  public static final String BITBUCKET_GIT_COMMIT = "BITBUCKET_COMMIT";
  public static final String BITBUCKET_GIT_BRANCH = "BITBUCKET_BRANCH";
  public static final String BITBUCKET_GIT_TAG = "BITBUCKET_TAG";

  BitBucketInfo() {
    final String repo = System.getenv(BITBUCKET_REPO_FULL_NAME);
    final String number = System.getenv(BITBUCKET_BUILD_NUMBER);
    final String url = buildPipelineUrl(repo, number);

    this.ciTags =
        new CITagsBuilder()
            .withCiProviderName(BITBUCKET_PROVIDER_NAME)
            .withCiPipelineId(buildPipelineId())
            .withCiPipelineName(repo)
            .withCiPipelineNumber(number)
            .withCiPipelineUrl(url)
            .withCiJorUrl(url)
            .withCiWorkspacePath(expandTilde(System.getenv(BITBUCKET_WORKSPACE_PATH)))
            .withGitRepositoryUrl(filterSensitiveInfo(System.getenv(BITBUCKET_GIT_REPOSITORY_URL)))
            .withGitCommit(System.getenv(BITBUCKET_GIT_COMMIT))
            .withGitBranch(normalizeRef(System.getenv(BITBUCKET_GIT_BRANCH)))
            .withGitTag(normalizeRef(System.getenv(BITBUCKET_GIT_TAG)))
            .build();
  }

  private String buildPipelineUrl(final String repo, final String number) {
    return String.format(
        "https://bitbucket.org/%s/addon/pipelines/home#!/results/%s", repo, number);
  }

  private String buildPipelineId() {
    String id = System.getenv(BITBUCKET_PIPELINE_ID);
    if (id != null) {
      id = id.replaceAll("}", "").replaceAll("\\{", "");
    }
    return id;
  }
}
