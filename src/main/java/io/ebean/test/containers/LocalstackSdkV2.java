package io.ebean.test.containers;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.kinesis.KinesisClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URI;

final class LocalstackSdkV2 implements AwsSDKv2 {

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
