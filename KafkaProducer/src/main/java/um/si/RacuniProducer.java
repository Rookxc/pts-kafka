package um.si;


import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.avro.Schema;
import org.apache.avro.SchemaBuilder;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.Random;

public class RacuniProducer {

    private final static String TOPIC = "kafka-racuni";

    private static KafkaProducer createProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "AvroProducer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        return new KafkaProducer(props);
    }

    private static ProducerRecord<Object,Object> generateRecord(Schema schema) {
        Random rand = new Random();
        GenericRecord avroRecord = new GenericData.Record(schema);

        String id = String.valueOf((int)(Math.random()*(10000 + 1) + 1));
        avroRecord.put("id", id);
        avroRecord.put("id_stranke", String.valueOf((int)(Math.random()*(10000 + 1) + 1)));
        avroRecord.put("znesek", Integer.valueOf(rand.nextInt((9-1)) + 1));
        avroRecord.put("datum_izdaje", System.currentTimeMillis());
        avroRecord.put("placan", true);
        System.out.println(avroRecord);
        ProducerRecord<Object, Object> producerRecord = new ProducerRecord<>(TOPIC, id, avroRecord);
        return producerRecord;
    }


    public static void main(String[] args) throws Exception {
        Schema schema = SchemaBuilder.record("Racuni")
                .fields()
                .requiredString("id")
                .requiredString("id_stranke")
                .requiredInt("znesek")
                .requiredLong("datum_izdaje")
                .requiredBoolean("placan")
                .endRecord();

       KafkaProducer producer = createProducer();

        while(true){
            ProducerRecord record = generateRecord(schema);
            producer.send(record);

            System.out.println("[RECORD] Sent new order object." + record);
            Thread.sleep(10000);
        }
    }

}
