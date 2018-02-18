package com.trading.forex.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Repository
public class ESRepositoryImpl implements  ESRepository{


    private final String index;
    private RestHighLevelClient restHighLevelClient;
    private ObjectMapper objectMapper;

    @Autowired
    public ESRepositoryImpl(ObjectMapper objectMapper, RestHighLevelClient restHighLevelClient, @org.springframework.beans.factory.annotation.Value("${elasticsearch.index}") String index) {
        this.objectMapper = objectMapper;
        this.restHighLevelClient = restHighLevelClient;
        this.index = index;
    }

    @Override
    public void push(final Map<String, Object> data) {
        try {
            data.put("@timestamp",new Date());
            final IndexRequest indexRequest = new IndexRequest(index, "svc", UUID.randomUUID().toString())
                    .source(objectMapper.writeValueAsString(data), XContentType.JSON);
            //restHighLevelClient.index(indexRequest);
        } catch (ElasticsearchException | IOException e) {
            log.error(e.getMessage(), e);
        }
    }
}
