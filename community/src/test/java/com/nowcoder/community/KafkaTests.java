package com.nowcoder.community;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/*/
1开启zookeeper
cd D:\Project\kafka_2.12-3.2.0    bin\windows\zookeeper-server-start.bat config\zookeeper.properties

2. 开启kafka
cd D:\Project\kafka_2.12-3.2.0    bin\windows\kafka-server-start.bat config\server.properties

3. 创建topic
D:\Project\kafka_2.12-3.2.0\bin\windows    kafka-topics.bat --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test

Created topic test.

4. 生产者
D:\Project\kafka_2.12-3.2.0\bin\windows    kafka-console-producer.bat --broker-list localhost:9092 --topic test

5. 消费者
d:\Project\kafka_2.12-3.2.0\bin\windows    kafka-console-consumer.bat --bootstrap-server localhost:9092 --topic test --from-beginning
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class KafkaTests {

    @Autowired
    private KafkaProducer kafkaProducer;

    @Test
    public void testKafka() {
        kafkaProducer.sendMessage("test", "你好");
        kafkaProducer.sendMessage("test", "在吗");

        try {
            Thread.sleep(1000 * 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}

@Component
class KafkaProducer {

    @Autowired
    private KafkaTemplate kafkaTemplate;

    public void sendMessage(String topic, String content) {
        kafkaTemplate.send(topic, content);
    }

}

@Component
class KafkaConsumer {

    @KafkaListener(topics = {"test"})
    public void handleMessage(ConsumerRecord record) {
        System.out.println(record.value());
    }


}