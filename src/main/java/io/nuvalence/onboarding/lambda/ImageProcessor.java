package io.nuvalence.onboarding.lambda;

import com.amazonaws.services.textract.model.*;
import com.amazonaws.services.textract.AmazonTextract;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ImageProcessor {
    private DynamoWriter dynamoWriter;
    private AmazonTextract textractClient;
    private RedactionClient redactionClient;
    private NotificationClient notificationClient;

    public ImageProcessor( DynamoWriter dynamoWriter, AmazonTextract textractClient, RedactionClient redactionClient, NotificationClient notificationClient){
        this.dynamoWriter = dynamoWriter;
        this.textractClient = textractClient;
        this.redactionClient = redactionClient;
        this.notificationClient = notificationClient;
    }

    public void processImage(String bucket, String key) throws IOException {
        //file key is in the format users/{username}/{filename}
        String[] splitKey = key.split("/");
        String userName = splitKey[1];
        DetectDocumentTextRequest request = new DetectDocumentTextRequest()
                .withDocument(new Document()
                        .withS3Object(new S3Object()
                                .withName(key)
                                .withBucket(bucket)));

        DetectDocumentTextResult result = textractClient.detectDocumentText(request);
        List<Block> blocks = result.getBlocks();


        //Regex to find SSN
        String SSNregex = "^\\d{3}-\\d{2}-\\d{4}$";
        Pattern SSNpattern = Pattern.compile(SSNregex);
        Matcher SSNmatcher = SSNpattern.matcher("");

        Set<String> lines = new HashSet<String>();
        List<BoundingBox> boxes = new ArrayList<BoundingBox>();

        for(Block b : blocks){
            if (b.getBlockType().equals("LINE")){
               String text = b.getText();
               SSNmatcher.reset(text);
               if (SSNmatcher.matches()){
                   text = text.replaceAll(SSNregex, "***-**-****");
                   lines.add(text);
               }
               else if (b.getConfidence() >= 90){
                   lines.add(text);
               }
            }
            if(b.getBlockType().equals("WORD")){
                String text = b.getText();
                SSNmatcher.reset(text);
                if (SSNmatcher.matches()){
                    boxes.add(b.getGeometry().getBoundingBox());
                }
            }
        }
        System.out.println(blocks);
        String url = bucket+".s3.amazonaws.com/"+key;
        if(boxes.size() > 0) {
            notificationClient.notifyAdmin(bucket,key);
            notificationClient.notifyUploader(bucket,key, userName);
            redactionClient.startRedaction(bucket, key, boxes);
            url = bucket+".s3.amazonaws.com/redacted/"+splitKey[splitKey.length - 1];
        }
        this.dynamoWriter.createNewEntry(key, url, new ArrayList<String>(lines), boxes);
    }
}
