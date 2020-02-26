package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.textract.AmazonTextract;
import com.amazonaws.services.textract.model.*;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;

public class ImageProcessorTest {
    private String key = "users/test/image.jpg";
    private String bucket = "test-bucket";
    private final DynamoWriter dynamoWriter = Mockito.mock(DynamoWriter.class);
    private final AmazonTextract textractClient = Mockito.mock(AmazonTextract.class);
    private final RedactionClient redactionClient = Mockito.mock(RedactionClient.class);
    private final NotificationClient notificationClient = Mockito.mock(NotificationClient.class);
    private final DetectDocumentTextRequest OCRRequest = new DetectDocumentTextRequest()
            .withDocument(new Document()
                    .withS3Object(new S3Object()
                            .withName(key)
                            .withBucket(bucket)));

    @Test
    public void processImage_givenNoTextResults_shouldCreateMinimumTableEntry() throws IOException {
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(new ArrayList<Block>());
        String expectedUrl = bucket+".s3.amazonaws.com/"+key;
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
    }

    @Test
    public void processImage_givenValidLineTextResults_shouldCreateTableEntryWithLines() throws IOException {
        ArrayList<Block> blocks = new ArrayList<Block>();
        blocks.add(new Block().withBlockType("LINE").withConfidence((float) 95).withText("TEST"));
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(blocks);
        String expectedUrl = bucket+".s3.amazonaws.com/"+key;
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();
        expectedLines.add("TEST");

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
    }
    @Test
    public void processImage_givenLowConfidenceLineTextResults_shouldCreateMinimumTableEntry() throws IOException {
        ArrayList<Block> blocks = new ArrayList<Block>();
        blocks.add(new Block().withBlockType("LINE").withConfidence((float) 80).withText("TEST"));
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(blocks);
        String expectedUrl = bucket+".s3.amazonaws.com/"+key;
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
    }

    @Test
    public void processImage_givenSSNLineTextResults_shouldCreateTableEntryWithLines() throws IOException {
        ArrayList<Block> blocks = new ArrayList<Block>();
        blocks.add(new Block().withBlockType("LINE").withText("123-45-6789"));
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(blocks);
        String expectedUrl = bucket+".s3.amazonaws.com/"+key;
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();
        expectedLines.add("***-**-****");

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
    }

    @Test
    public void processImage_givenValidWordTextResults_shouldCreateMinimumTableEntry() throws IOException {
        ArrayList<Block> blocks = new ArrayList<Block>();
        BoundingBox b = new BoundingBox();
        blocks.add(
                new Block().withBlockType("WORD").withText("TEST").withGeometry(
                        new Geometry().withBoundingBox(b)
                )
        );
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(blocks);
        String expectedUrl = bucket+".s3.amazonaws.com/"+key;
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
    }


    @Test
    public void processImage_givenSSNWordTextResults_shouldCreateTableEntryAndNotifyAndRedact() throws IOException {
        ArrayList<Block> blocks = new ArrayList<Block>();
        BoundingBox b = new BoundingBox();
        blocks.add(
                new Block().withBlockType("WORD").withText("123-45-6789").withGeometry(
                        new Geometry().withBoundingBox(b)
                )
        );
        DetectDocumentTextResult expectedResult = new DetectDocumentTextResult().withBlocks(blocks);
        String expectedUrl = bucket+".s3.amazonaws.com/redacted/image.jpg";
        ArrayList<String> expectedLines = new ArrayList<>();
        ArrayList<BoundingBox> expectedBoxes = new ArrayList<>();
        expectedBoxes.add(b);

        Mockito.when(textractClient.detectDocumentText(OCRRequest)).thenReturn(expectedResult);

        ImageProcessor processor = new ImageProcessor(dynamoWriter,textractClient,redactionClient,notificationClient);
        processor.processImage(bucket, key);

        Mockito.verify(dynamoWriter).createNewEntry(key, expectedUrl, expectedLines, expectedBoxes);
        Mockito.verify(notificationClient).notifyAdmin(bucket, key);
        Mockito.verify(notificationClient).notifyUploader(bucket, key, "test");
    }

}
