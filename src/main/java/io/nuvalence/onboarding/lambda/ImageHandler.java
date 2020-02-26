package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProviderClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.S3Event;
import com.amazonaws.services.s3.event.S3EventNotification;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.AmazonTextractClientBuilder;

import java.io.IOException;

public class ImageHandler implements RequestHandler<S3Event, String> {
    private ImageProcessor imageProcessor;

    public ImageHandler(){
        String snsARN = "arn:aws:sns:us-east-1:116621101481:grant-onboarding-topic";
        String tableName = "grant-onboarding";

        AmazonDynamoDB dynamo = AmazonDynamoDBClientBuilder.defaultClient();
        AmazonTextract textractClient = AmazonTextractClientBuilder.defaultClient();
        AmazonSNS snsClient = AmazonSNSClientBuilder.defaultClient();
        AWSCognitoIdentityProvider cognitoClient = AWSCognitoIdentityProviderClientBuilder.defaultClient();
        AWSLambda lambdaClient = AWSLambdaClientBuilder.defaultClient();

        DynamoWriter dynamoWriter = new DynamoWriter(dynamo, tableName);
        RedactionClient redactionClient = new RedactionClient(lambdaClient);
        NotificationClient notificationClient = new NotificationClient(snsARN, snsClient, cognitoClient);

        this.imageProcessor = new ImageProcessor(dynamoWriter, textractClient, redactionClient, notificationClient);
    }

    public String handleRequest(S3Event event, Context context){
        S3EventNotification.S3EventNotificationRecord record = event.getRecords().get(0);

        String srcBucket = record.getS3().getBucket().getName();
        // Object key may have spaces or unicode non-ASCII characters.
        String srcKey = record.getS3().getObject().getUrlDecodedKey();

        try {
            this.imageProcessor.processImage(srcBucket,srcKey);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "PLACEHOLDER";
    }
}
