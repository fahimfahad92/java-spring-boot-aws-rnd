package rnd.fahim.javaspringbootawsrnd.sqs;

import io.awspring.cloud.sqs.annotation.SqsListener;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

@Service
public class SqsService {

  private final SqsClient sqsClient;

  @Value("${aws.sqs.queue-url}")
  private String queueUrl;

  @Value("${aws.sqs.fifo-queue-url}")
  private String fifoQueueUrl;

  public SqsService(SqsClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  public void sendMessage(String messageBody) {
    SendMessageRequest request =
        SendMessageRequest.builder().queueUrl(queueUrl).messageBody(messageBody).build();

    sqsClient.sendMessage(request);
    System.out.println(request.toString());
  }

  public void sendOrderedMessage(String messageId, String messageGroupId, String messageBody) {
    SendMessageRequest request =
        SendMessageRequest.builder()
            .queueUrl(fifoQueueUrl)
            .messageBody(messageBody)
            .messageGroupId(messageGroupId)
            .messageDeduplicationId(messageId)
            .build();

    sqsClient.sendMessage(request);
    System.out.println("Sent message for " + messageId + " messageGroupId: " + messageGroupId);
  }

  /**
   * Polls messages from an Amazon SQS queue at regular intervals. Retrieves up to 10 messages at a
   * time using a long polling mechanism to reduce empty responses. Once messages are retrieved,
   * they are processed and deleted from the queue.
   *
   * <p>The method uses the configured queue URL and deletes each processed message to ensure it is
   * not reprocessed.
   *
   * <p>Note: - Long polling is enabled by setting the `waitTimeSeconds` parameter to 10 seconds. -
   * The method processes each message and automatically deletes it from the queue post-processing.
   * - The number of retrieved messages per poll is limited to 10.
   */
  @Scheduled(fixedDelay = 5000)
  public void pollMessages() {
    ReceiveMessageRequest receiveRequest =
        ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(10) // Long polling
            .build();

    List<Message> messages = sqsClient.receiveMessage(receiveRequest).messages();

    System.out.println("Running pollMessages for " + messages.size() + " messages");

    for (Message msg : messages) {
      System.out.println("Received: " + msg.body());

      sqsClient.deleteMessage(
          DeleteMessageRequest.builder()
              .queueUrl(queueUrl)
              .receiptHandle(msg.receiptHandle())
              .build());
    }
  }

  /**
   * Handles incoming messages from the Amazon SQS standard queue.
   *
   * @param message the content of the message received from the standard queue
   */
  @SqsListener("#{ '${standardQueueName}' }")
  public void listener(String message) {
    System.out.println("Received from listener 1: " + message);
  }

  /**
   * Handles incoming messages from an Amazon SQS FIFO queue.
   *
   * @param messageBody the content of the message received from the FIFO queue
   */
  @SqsListener("#{ '${fifoQueueName}' }")
  public void processor(String messageBody) {
    System.out.println("FIFO Message Received: " + messageBody);
  }
}
