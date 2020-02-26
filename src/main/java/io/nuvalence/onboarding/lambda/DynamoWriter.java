package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.textract.model.Block;
import com.amazonaws.services.textract.model.BoundingBox;
import com.amazonaws.services.textract.model.DetectDocumentTextResult;

import java.util.HashMap;
import java.util.List;

public class DynamoWriter {
    private final AmazonDynamoDB dynamo;
    private final String tableName;

    DynamoWriter(AmazonDynamoDB dynamoConnection, String table) {
        this.dynamo = dynamoConnection;
        this.tableName = table;
    }

    public void createNewEntry(String fileKey, String url, List<String> lines, List<BoundingBox> boxes){
        HashMap<String, AttributeValue> item = new HashMap<String, AttributeValue>();
        //Insert values into item
        item.put("fileKey", new AttributeValue(fileKey));
        item.put("url", new AttributeValue(url));
        if(lines.size() > 0) {
            item.put("foundText", new AttributeValue(lines));
        }
        if(boxes.size() > 0) {
            item.put("redactedBoxes", new AttributeValue(String.valueOf(boxes)));
        }
        System.out.println("Creating new table entry for: "+fileKey);
        this.dynamo.putItem(this.tableName, item);
    }
}
