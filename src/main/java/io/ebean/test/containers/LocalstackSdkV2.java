package io.ebean.test.containers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.kms.KmsAsyncClient;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.io.File;
import java.io.IOException;
import java.net.URI;

final class LocalstackSdkV2 implements AwsSDKv2 {

  static {
    // AWS SDK v2 NPEs in ProfileFile$BuilderImpl.build() when the resolved config/credentials
    // file path is null (i.e. ~/.aws does not exist, as on CI runners).
    // ProfileFileLocation checks aws.configFile / aws.sharedCredentialsFile system properties
    // BEFORE falling back to the user.home-based path, so pointing them to a real (empty)
    // temp file makes the path non-null and prevents the NPE.
    ensureAwsProfileFilesExist();
  }

  private static void ensureAwsProfileFilesExist() {
    if (System.getProperty("aws.configFile") == null) {
      try {
        File emptyFile = File.createTempFile("aws-sdk-empty-config", ".properties");
        emptyFile.deleteOnExit();
        String path = emptyFile.getAbsolutePath();
        System.setProperty("aws.configFile", path);
        System.setProperty("aws.sharedCredentialsFile", path);
      } catch (IOException e) {
        // best effort
      }
    }
  }

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
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public KinesisClient kinesisClient() {
    return KinesisClient.builder()
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public SnsClient snsClient() {
    return SnsClient.builder()
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public SqsClient sqsClient() {
    return SqsClient.builder()
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public S3Client s3Client() {
    return S3Client.builder()
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
      .credentialsProvider(credentialsProvider())
      .endpointOverride(endpoint)
      .region(region())
      .build();
  }

  @Override
  public KmsAsyncClient kmsAsyncClient() {
    return KmsAsyncClient.builder()
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
