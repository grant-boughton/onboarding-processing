package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.textract.model.BoundingBox;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RedactionClient {
    private AWSLambda lambdaClient;

    public RedactionClient(AWSLambda lambdaClient){
            this.lambdaClient = lambdaClient;
    }

    public void startRedaction(String bucket, String key, List<BoundingBox> boxes) throws IOException {
        List<Map<String, Float>> boxData = new ArrayList<Map<String, Float>>();
        for(BoundingBox b : boxes) {
            Map<String, Float> box = new HashMap<>();
            box.put("top", b.getTop());
            box.put("bottom",b.getTop() + b.getHeight());
            box.put("left",b.getLeft());
            box.put("right",b.getLeft()+b.getWidth());
            boxData.add(box);
        }
        if (boxData.size() > 0){
            Map<String, String> dataPayload = new HashMap();
            ObjectMapper mapper = new ObjectMapper();
            dataPayload.put("data", mapper.writeValueAsString(boxData));
            dataPayload.put("key", key);
            dataPayload.put("bucket", bucket);

            InvokeRequest request = new InvokeRequest()
                    .withFunctionName("grant-onboarding-image-edit")
                    .withPayload(mapper.writeValueAsString(dataPayload));

            InvokeResult res = lambdaClient.invoke(request);
            String output = new String(res.getPayload().array());
            System.out.println("REDACTED: " + output);
        }

    }
}
