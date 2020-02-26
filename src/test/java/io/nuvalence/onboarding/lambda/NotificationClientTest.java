package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.cognitoidp.AWSCognitoIdentityProvider;
import com.amazonaws.services.cognitoidp.model.AdminGetUserRequest;
import com.amazonaws.services.cognitoidp.model.AdminGetUserResult;
import com.amazonaws.services.cognitoidp.model.AttributeType;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.PublishResult;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

public class NotificationClientTest {
    private final String snsARN = "arn:aws:sns:region:aws-id:topic";
    private final AmazonSNS snsClient = Mockito.mock(AmazonSNS.class);
    private final AWSCognitoIdentityProvider cognitoClient = Mockito.mock(AWSCognitoIdentityProvider.class);
    private final String bucket = "test-bucket";
    private final String key  = "users/test/image.jpg";
    private final String cognitoPool = "us-east-1_99kRWKlMB";
    private final String userName = "test";

    @Test
    public void notifyAdminTest(){
        NotificationClient notificationClient = new NotificationClient(snsARN, snsClient, cognitoClient);
        PublishRequest expectedRequest = new PublishRequest(snsARN,"ALERT: uploaded file "+key+" in bucket "+bucket+" contains sensitive information");
        Mockito.when(snsClient.publish(expectedRequest)).thenReturn(new PublishResult().withMessageId("1"));

        notificationClient.notifyAdmin(bucket, key);

        Mockito.verify(snsClient).publish(expectedRequest);
    }

    @Test
    public void notifyUploader_givenUserWithPhoneNumber_shouldContactUser(){
        NotificationClient notificationClient = new NotificationClient(snsARN, snsClient, cognitoClient);
        String phoneNumber = "+11111111111";
        AdminGetUserRequest expectedUserRequest = new AdminGetUserRequest()
                .withUserPoolId(cognitoPool)
                .withUsername(userName);
        AdminGetUserResult expectedUserResult = new AdminGetUserResult().withUserAttributes(
                new ArrayList<AttributeType>() {{
                    add(new AttributeType().withName("phone_number").withValue(phoneNumber));
                }}
        );
        PublishRequest expectedPublishRequest = new PublishRequest()
                .withMessage("ALERT: your uploaded file " + key + " in bucket "+bucket+" contains sensitive information")
                .withPhoneNumber(phoneNumber);

        Mockito.when(cognitoClient.adminGetUser(expectedUserRequest)).thenReturn(expectedUserResult);
        Mockito.when(snsClient.publish(expectedPublishRequest)).thenReturn(new PublishResult());

        notificationClient.notifyUploader(bucket, key, userName);

        Mockito.verify(cognitoClient).adminGetUser(expectedUserRequest);
        Mockito.verify(snsClient).publish(expectedPublishRequest);
    }

    @Test
    public void notifyUploader_givenUserWithoutPhoneNumber_shouldNotContactUser(){
        NotificationClient notificationClient = new NotificationClient(snsARN, snsClient, cognitoClient);
        String phoneNumber = "+11111111111";
        AdminGetUserRequest expectedUserRequest = new AdminGetUserRequest()
                .withUserPoolId(cognitoPool)
                .withUsername(userName);
        AdminGetUserResult expectedUserResult = new AdminGetUserResult().withUserAttributes(
                new ArrayList<AttributeType>() {{
                    add(new AttributeType().withName("placeholder").withValue("test"));
                }}
        );
        PublishRequest expectedPublishRequest = new PublishRequest()
                .withMessage("ALERT: your uploaded file " + key + " in bucket "+bucket+" contains sensitive information")
                .withPhoneNumber(phoneNumber);

        Mockito.when(cognitoClient.adminGetUser(expectedUserRequest)).thenReturn(expectedUserResult);

        notificationClient.notifyUploader(bucket, key, userName);

        Mockito.verify(cognitoClient).adminGetUser(expectedUserRequest);
        Mockito.verify(snsClient, Mockito.never()).publish(expectedPublishRequest);
    }
}
