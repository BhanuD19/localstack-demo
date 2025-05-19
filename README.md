Spring boot application using Java 24.
Demonstration for Test Containers and Localstack to emulate cloud-ready development and performance checks locally.
Run the tests using ./mvnw test.

Current application: 
Simple application to publish messages to SQS message queue.
Contains listeners to the queue, which persists the message content in the S3 buckets.

Further development:
Creating a simple application which connects with DB for persistence but uses S3 as a backup storage. Also, help the DB servers to save their daily archives to S3.
