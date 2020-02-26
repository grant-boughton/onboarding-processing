package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;

public class NotificationClient {
    private String snsARN = "arn:aws:sns:us-east-1:116621101481:grant-onboarding-topic";
    private AmazonSNS snsClient;
    private AWSCognitoIdentityProvider cognitoClient;

    public NotificationClient(String topicARN, AmazonSNS snsClient, AWSCognitoIdentityProvider cognitoClient){
        this.snsARN = topicARN;
        this.snsClient = snsClient;
        this.cognitoClient = cognitoClient;
    }

    public void notifyAdmin(String bucket, String key){
        String message = "ALERT: uploaded file "+key+" in bucket "+bucket+" contains sensitive information";
        PublishRequest request = new PublishRequest(this.snsARN,message);
        final PublishResult publishResponse = snsClient.publish(request);

        System.out.println("Admin MessageId: " + publishResponse.getMessageId());
    }

    public void notifyUploader(String bucket, String key, String userName){

        AdminGetUserRequest request = new AdminGetUserRequest()
                .withUserPoolId("us-east-1_99kRWKlMB")
                .withUsername(userName);
        AdminGetUserResult result = cognitoClient.adminGetUser(request);
        java.util.List<AttributeType> attributes = result.getUserAttributes();
        AttributeType phoneNumberAttribute = attributes.stream()
                .filter(attribute -> attribute.getName().equals("phone_number"))
                .findAny()
                .orElse(null);
        if(phoneNumberAttribute != null) {
            String phoneNumber = phoneNumberAttribute.getValue();
            PublishRequest pubRequest = new PublishRequest()
                    .withMessage("ALERT: your uploaded file " + key + " in bucket "+bucket+" contains sensitive information")
                    .withPhoneNumber(phoneNumber);
            PublishResult pubResult = snsClient.publish(pubRequest);
            System.out.println("MessageId: " + pubResult.getMessageId());
        }
    }
}
