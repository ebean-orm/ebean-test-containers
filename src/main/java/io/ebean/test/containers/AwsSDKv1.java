package io.ebean.test.containers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sqs.AmazonSQS;

/**
 * AWS SDK v1 compatible helper API to provide clients like
 * AmazonDynamoDB, AmazonSNS, AmazonSQS, AmazonKinesis.
 *
 * @see Localstack2Container#sdk2()
 */
public interface AwsSDKv1 {

  /**
   * Return SDK 1 AWSStaticCredentialsProvider.
   */
  AWSStaticCredentialsProvider credentials();

  /**
   * Return the AmazonDynamoDB (V1 SDK) that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  AmazonDynamoDB dynamoDB();

  /**
   * Return AmazonKinesis (V1 SDK) that can be used for this container.
   * <p>
   * This should be used AFTER the container is started.
   */
  AmazonKinesis kinesis();

  /**
   * Return the AmazonSNS (V1 SDK) client for this container.
   */
  AmazonSNS sns();

  /**
   * Return the AmazonSQS (V1 SDK) client for this container.
   */
  AmazonSQS sqs();
}
