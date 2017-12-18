package cz.csas.smartdlk;

import cz.csas.smartdlk.service.EventProcessor;
import org.apache.avro.generic.GenericRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;

@SpringBootApplication
@EnableCaching
@EnableKafka
public class Application {

	@Autowired
	private EventProcessor eventProcessor;

	@KafkaListener(topics = "SMART.monitorEvents")
	public void listen(GenericRecord record) throws Exception {
		eventProcessor.handle(record);
	}

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
