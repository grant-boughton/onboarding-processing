package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.textract.model.BoundingBox;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedactionClientTest {
    private final AWSLambda lambdaClient = Mockito.mock(AWSLambda.class);
    private final String key = "users/test/image.jpg";
    private final String bucket = "test-bucket";

    @Test
    public void startRedaction_givenValidBoxes_shouldInvokeLambda() throws IOException {
        RedactionClient redactionClient = new RedactionClient(lambdaClient);
        List<BoundingBox> boxes = new ArrayList<BoundingBox>(){{
           add(new BoundingBox()
                   .withTop((float) 0.5)
                   .withHeight((float) 0.5)
                   .withLeft((float) 0.5)
                   .withWidth((float) 0.5));
        }};
        List<Map<String,Float>> expectedBoxData = new ArrayList<Map<String, Float>>(){{
            add(new HashMap(){{
                put("top", (float) 0.5);
                put("bottom", (float) 1);
                put("left", (float) 0.5);
                put("right", (float) 1.0);
            }});
        }};
        ObjectMapper mapper = new ObjectMapper();
        Map<String,String> expectedPayload = new HashMap<String, String>(){{
            put("data", mapper.writeValueAsString(expectedBoxData));
            put("key", key);
            put("bucket", bucket);
        }};

        InvokeRequest expectedRequest = new InvokeRequest()
                .withFunctionName("grant-onboarding-image-edit")
                .withPayload(mapper.writeValueAsString(expectedPayload));
        String expectedResponsePayload = "test";
        Mockito.when(lambdaClient.invoke(expectedRequest)).thenReturn(
                new InvokeResult().withPayload(ByteBuffer.wrap(expectedResponsePayload.getBytes()))
        );

        redactionClient.startRedaction(bucket,key,boxes);

        Mockito.verify(lambdaClient).invoke(expectedRequest);

    }
}
