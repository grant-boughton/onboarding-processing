package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.PutItemResult;
import com.amazonaws.services.textract.model.BoundingBox;
import net.bytebuddy.asm.Advice;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DynamoWriterTest {
    private final AmazonDynamoDB mockDynamoClient = Mockito.mock(AmazonDynamoDB.class);
    private final String table = "testTable";
    private final String url = "testURL";
    private final String key = "text/image.jpg";
    private final List<String> lines = new ArrayList<String>() {{
        add("TEXT");
        add("WORD");
        add("TEST");
    }};
    private final List<BoundingBox> boxes = new ArrayList<BoundingBox>() {{
       add(new BoundingBox());
    }};

    @Test
    public void createNewEntry_givenAllData_dbShouldContainAllFields(){
        DynamoWriter writer = new DynamoWriter(mockDynamoClient, table);

        HashMap<String, AttributeValue> expectedInput = new HashMap<String, AttributeValue>() {{
            put("fileKey", new AttributeValue(key));
            put("url", new AttributeValue(url));
            put("foundText", new AttributeValue(lines));
            put("redactedBoxes", new AttributeValue(String.valueOf(boxes)));
        }};
        Mockito.when(mockDynamoClient.putItem(this.table, expectedInput)).thenReturn(new PutItemResult());
        writer.createNewEntry(key, url, lines, boxes);
        Mockito.verify(mockDynamoClient).putItem(table, expectedInput);
    }

    @Test
    public void createNewEntry_notGivenLines_dbShouldNotContainLinesField(){
        DynamoWriter writer = new DynamoWriter(mockDynamoClient, table);

        HashMap<String, AttributeValue> expectedInput = new HashMap<String, AttributeValue>() {{
            put("fileKey", new AttributeValue(key));
            put("url", new AttributeValue(url));
            put("redactedBoxes", new AttributeValue(String.valueOf(boxes)));
        }};
        Mockito.when(mockDynamoClient.putItem(this.table, expectedInput)).thenReturn(new PutItemResult());
        writer.createNewEntry(key, url, new ArrayList<String>(), boxes);
        Mockito.verify(mockDynamoClient).putItem(table, expectedInput);
    }

    @Test
    public void createNewEntry_notGivenBoxes_dbShouldNotContainBoxesField(){
        DynamoWriter writer = new DynamoWriter(mockDynamoClient, table);

        HashMap<String, AttributeValue> expectedInput = new HashMap<String, AttributeValue>() {{
            put("fileKey", new AttributeValue(key));
            put("url", new AttributeValue(url));
            put("foundText", new AttributeValue(lines));
        }};
        Mockito.when(mockDynamoClient.putItem(this.table, expectedInput)).thenReturn(new PutItemResult());
        writer.createNewEntry(key, url, lines, new ArrayList<BoundingBox>());
        Mockito.verify(mockDynamoClient).putItem(table, expectedInput);
    }
}
