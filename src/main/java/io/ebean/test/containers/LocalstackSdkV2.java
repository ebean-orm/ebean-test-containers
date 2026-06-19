package io.ebean.test.containers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.profiles.ProfileFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

final class LocalstackSdkV2 implements AwsSDKv2 {

  /**
   * Empty profile file used to disable the AWS SDK default profile-file resolution.
   * We always supply explicit credentials, region and endpoint, so the SDK must not
   * attempt to read an ambient ~/.aws/config (which NPEs when that file/home is absent,
   * e.g. on CI).
   */
  private static final ProfileFile EMPTY_PROFILE_FILE = ProfileFile.aggregator().build();

  private static final ClientOverrideConfiguration OVERRIDE_CONFIGURATION =
    ClientOverrideConfiguration.builder()
      .defaultProfileFile(EMPTY_PROFILE_FILE)
      .build();

  private final String awsRegion;
  private final URI endpoint;
  private final AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create("localstack", "localstack");

  LocalstackSdkV2(String awsRegion, URI endpoint) {
    this.awsRegion = awsRegion;
    this.endpoint = endpoint;
  }

  @Override
  public DynamoDbClient dynamoDBClient() {
    return DynamoDbClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public KinesisClient kinesisClient() {
    return KinesisClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public SnsClient snsClient() {
    return SnsClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public SqsClient sqsClient() {
    return SqsClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public S3Client s3Client() {
    return S3Client.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .forcePathStyle(true)
      .build();
  }

  @Override
  public URI endpoint() {
    return endpoint;
  }

  @Override
  public KmsClient kmsClient() {
    return KmsClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public KmsAsyncClient kmsAsyncClient() {
    return KmsAsyncClient.builder()
      .overrideConfiguration(OVERRIDE_CONFIGURATION)
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public Region region() {
    return awsRegion == null ? null : Region.of(awsRegion);
  }

  @Override
  public AwsCredentialsProvider credentialsProvider() {
    return this::basicCredentials;
  }

  @Override
  public AwsBasicCredentials basicCredentials() {
    return awsBasicCredentials;
  }
}
