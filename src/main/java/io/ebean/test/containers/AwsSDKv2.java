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

import java.net.URI;

/**
 * AWS SDK v1 compatible helper API to provide clients like
 * DynamoDbClient, SnsClient, SqsClient, KinesisClient.
 *
 * @see Localstack2Container#sdk1()
 */
public interface AwsSDKv2 {

  /**
   * Return the endpoint URI for this container.
   */
  URI endpoint();

  /**
   * Return the DynamoDbClient (V2 SDK) that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  DynamoDbClient dynamoDBClient();

  /**
   * Return KinesisClient (V2 SDK) that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  KinesisClient kinesisClient();

  /**
   * Return the SnsClient (V2 SDK) client for this container.
   */
  SnsClient snsClient();

  /**
   * Return the SnsClient (V2 SDK) client for this container.
   */
  SqsClient sqsClient();

  /**
   * Return the S3Client (V2 SDK) for this container.
   */
  S3Client s3Client();

  /**
   * Return the KmsClient (V2 SDK) for this container.
   */
  KmsClient kmsClient();

  /**
   * Return the KmsAsyncClient (V2 SDK) for this container.
   */
  KmsAsyncClient kmsAsyncClient();

  /**
   * Return SDK 2 Region.
   */
  Region region();

  /**
   * Return SDK 2 AwsCredentialsProvider.
   */
  AwsCredentialsProvider credentialsProvider();

  /**
   * Return the basic credentials that can be used for this container.
   */
  AwsBasicCredentials basicCredentials();
}
