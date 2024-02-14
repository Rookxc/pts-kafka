package um.si;


import io.confluent.kafka.streams.serdes.avro.GenericAvroDeserializer;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerde;
import io.confluent.kafka.streams.serdes.avro.GenericAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;

import java.util.*;
public class OrderStreamer {
    private static Properties setupApp() {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "racuni");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.Integer().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, GenericAvroSerde.class.getName());
        props.put("schema.registry.url", "http://0.0.0.0:8081");
        props.put("input.topic.name", "kafka-racuni");
        props.put("default.deserialization.exception.handler", "org.apache.kafka.streams.errors.LogAndContinueExceptionHandler");
        return props;
    }

    public static void main(String[] args) throws Exception {
        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
                "http://0.0.0.0:8081");

        final Serde<GenericRecord> valueGenericAvroSerde = new GenericAvroSerde();
        valueGenericAvroSerde.configure(serdeConfig, false);

        StreamsBuilder builder = new StreamsBuilder();
        KStream<Integer, GenericRecord> inputStream = builder.stream("kafka-racuni", Consumed.with(Serdes.Integer(), valueGenericAvroSerde));

        //sešteti zneski
        inputStream.map((k, v) -> new KeyValue<>(Integer.valueOf(v.get("id").toString()), v))
                .groupByKey(Grouped.with(Serdes.Integer(), valueGenericAvroSerde))
                .reduce((aggValue, newValue) -> {
                    double newZnesek = (double) newValue.get("znesek");
                    double aggZnesek = (double) aggValue.get("znesek");
                    aggValue.put("znesek", aggZnesek + newZnesek);
                    return aggValue;
                })
                .toStream()
                .to("kafka-aggregated-racuni", Produced.with(Serdes.Integer(), valueGenericAvroSerde));



        //st. orderjev na (po idju) stranko
        inputStream.map((k, v) -> new KeyValue<>(Integer.valueOf(v.get("id_stranke").toString()), 1))
                .groupByKey(Grouped.with(Serdes.Integer(), Serdes.Integer()))
                .count()
                .toStream()
                .to("kafka-order-count-by-id_stranke", Produced.with(Serdes.Integer(), Serdes.Long()));

        //Filter orders z znesekom ki je večji od 100
        inputStream.filter((k, v) -> (double) v.get("znesek") > 100.0)
                .to("kafka-high-value-orders", Produced.with(Serdes.Integer(), valueGenericAvroSerde));

        inputStream.print(Printed.toSysOut());

        Topology topology = builder.build();
        final KafkaStreams streams = new KafkaStreams(topology, setupApp());
        streams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));

    }
}
