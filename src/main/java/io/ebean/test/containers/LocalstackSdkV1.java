package io.ebean.test.containers;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.kinesis.AmazonKinesis;
import com.amazonaws.services.kinesis.AmazonKinesisClientBuilder;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;


final class LocalstackSdkV1 implements AwsSDKv1 {

  private final String awsRegion;
  private final String endpointUri;

  LocalstackSdkV1(String awsRegion, String endpointUri) {
    this.awsRegion = awsRegion;
    this.endpointUri = endpointUri;
  }

  @Override
  public AWSStaticCredentialsProvider credentials() {
    return new AWSStaticCredentialsProvider(new BasicAWSCredentials("localstack", "localstack"));
  }

  @Override
  public AmazonDynamoDB dynamoDB() {
    return AmazonDynamoDBClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUri, awsRegion))
      .build();
  }

  @Override
  public AmazonKinesis kinesis() {
    return AmazonKinesisClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUri, awsRegion))
      .build();
  }

  @Override
  public AmazonSNS sns() {
    return AmazonSNSClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUri, awsRegion))
      .build();
  }

  @Override
  public AmazonSQS sqs() {
    return AmazonSQSClientBuilder.standard()
      .withCredentials(credentials())
      .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUri, awsRegion))
      .build();
  }
}
