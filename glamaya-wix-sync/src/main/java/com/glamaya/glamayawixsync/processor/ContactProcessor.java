package com.glamaya.glamayawixsync.processor;

import com.glamaya.datacontracts.ecommerce.mapper.ContactMapperFactory;
import com.glamaya.datacontracts.wix.Contact;
import com.glamaya.datacontracts.wix.ContactQuery;
import com.glamaya.datacontracts.wix.ContactQueryRequest;
import com.glamaya.datacontracts.wix.ContactQueryResponse;
import com.glamaya.datacontracts.wix.Paging;
import com.glamaya.datacontracts.wix.Sort;
import com.glamaya.datacontracts.wix.SortOrder;
import com.glamaya.glamayawixsync.config.kafka.KafkaProducer;
import com.glamaya.glamayawixsync.repository.ProcessorStatusTrackerRepository;
import com.glamaya.glamayawixsync.repository.entity.ProcessorStatusTracker;
import com.glamaya.glamayawixsync.repository.entity.ProcessorType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContactProcessor implements GlamWixProcessor<List<Contact>, ContactQueryRequest, ContactQueryResponse> {

    private final WebClient webClient;
    private final KafkaProducer<Object> producer;
    @Getter
    private final PollerMetadata poller;
    private final ProcessorStatusTrackerRepository repository;
    private final ContactMapperFactory<Contact> contactMapperFactory;

    @Value("${application.wix.entities.contacts.query-url}")
    private final String queryContactUrl;
    @Value("${application.wix.entities.contacts.enable}")
    private final boolean enable;
    @Value("${application.wix.entities.contacts.fetch-limit}")
    private final long fetchLimit;
    @Value("${application.wix.entities.contacts.reset-on-startup: false}")
    private boolean resetOnStartup;
    @Value("${external.wix.api.account-name}")
    private final String sourceAccountName;
    @Value("${application.kafka.topic.contact-events}")
    private final String wixContactEventsTopic;
    @Value("${application.kafka.topic.ecom-contact-events}")
    private final String ecomContactEventsTopic;
    @Value("${application.wix.entities.contacts.fetch-duration-in-millis.active-mode}")
    private int fetchDurationInMillisActiveMode;
    @Value("${application.wix.entities.contacts.fetch-duration-in-millis.passive-mode}")
    private int fetchDurationInMillisPassiveMode;

    @Override
    public MessageSource<List<Contact>> receive() {
        return () -> {

            if (!enable) {
                log.info("Contact sync is disabled");
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode * 60);
                return null;
            }

            ProcessorStatusTracker statusTracker = getOrCreateStatusTracker(resetOnStartup, fetchLimit);
            resetOnStartup = false; // Reset only once on startup

            var request = buildContactQueryRequest(statusTracker);

            var response = fetchEntities(webClient, queryContactUrl, request, new ParameterizedTypeReference<>() {
            });

            var isResponseNullOrEmpty = response == null || response.getContacts() == null || response.getContacts().isEmpty();
            if (isResponseNullOrEmpty) {
                log.info("No new wix contacts found, resetting status tracker and switching to passive mode");
                resetStatusTracker(statusTracker);
                modifyPollerDuration(poller, fetchDurationInMillisPassiveMode);
            } else {
                log.info("Fetched {} wix contacts", statusTracker.getCount() + response.getContacts().size());
                updateStatusTracker(statusTracker, response.getContacts(), null, c -> ((Contact) c).getUpdatedDate());
                modifyPollerDuration(poller, fetchDurationInMillisActiveMode);
            }
            repository.save(statusTracker).block();
            return isResponseNullOrEmpty ? null : MessageBuilder.withPayload(response.getContacts()).build();
        };
    }

    private ContactQueryRequest buildContactQueryRequest(ProcessorStatusTracker statusTracker) {
        var contactQuery = ContactQuery.builder()
                .withSort(List.of(Sort.builder().withFieldName("updatedDate").withOrder(SortOrder.ASC).build()))
                .withPaging(Paging.builder().withLimit(fetchLimit).withOffset(statusTracker.getOffset()).build())
                .withFieldSets(List.of("FULL"));

        if (statusTracker.getOffset() == 0 && statusTracker.getLastUpdatedDate() != null) {
            Map<String, Map<String, String>> filterMap = new HashMap<>();
            Map<String, String> dateMap = new HashMap<>();
            dateMap.put("$gt", statusTracker.getLastUpdatedDate().toString());
            filterMap.put("updatedDate", dateMap);
            contactQuery.withFilter(filterMap);
        }
        return ContactQueryRequest.builder().withQuery(contactQuery.build()).build();
    }

    @Override
    public ProcessorStatusTrackerRepository getStatusTrackerRepository() {
        return repository;
    }

    @Override
    public ProcessorType getProcessorType() {
        return ProcessorType.WIX_CONTACT;
    }

    @Override
    public Object handle(List<Contact> payload, MessageHeaders headers) {
        payload.forEach(contact -> {
            producer.send(wixContactEventsTopic, contact.getId(), contact);
            try {
                var ecomContact = contactMapperFactory.toGlamayaContact(contact, sourceAccountName);
                producer.send(ecomContactEventsTopic, ecomContact.getId(), ecomContact);
            } catch (IllegalArgumentException e) {
                log.error("Failed to map contact {}: {}", contact.getId(), e.getMessage());
            }
        });
        return null;
    }
}