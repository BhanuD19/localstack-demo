package com.example.localstack;

import com.example.localstack.config.AwsConfig;
import com.example.localstack.data.messageEntities.Message;
import com.example.localstack.service.MessagePublisher;
import com.example.localstack.service.S3Services;
import org.assertj.core.api.Assertions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.UUID;
import static org.awaitility.Awaitility.await;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MessageProcessingTest {

    @Autowired
    private MessagePublisher messagePublisher;
    @Autowired
    private AwsConfig awsConfig;
    @Autowired
    private S3Services s3Services;

//    @Test
//    void shouldHandleMessageSuccessfully() {
//        var message = new Message(UUID.randomUUID(), "Hello World!");
//        messagePublisher.publish(awsConfig.queueName(), message);
//
//        await().pollInterval(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
//            String msg = s3Services.downloadAsString(awsConfig.bucketName(), message.uuid().toString());
//            Assertions.assertThat(msg).isEqualTo(message.content());
//        });
//    }
}

