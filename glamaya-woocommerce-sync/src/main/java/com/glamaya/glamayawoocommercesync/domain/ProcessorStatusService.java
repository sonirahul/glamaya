package com.glamaya.glamayawoocommercesync.domain;

import java.util.List;
import java.util.function.Function;

import static com.glamaya.datacontracts.commons.constant.Constants.STRING_DATE_TO_INSTANT_FUNCTION;

public class ProcessorStatusService {

    public void resetAfterEmptyPage(ProcessorStatus status) {
        if (status == null) return;
        status.setPage(1);
        status.setUseLastUpdatedDateInQuery(true);
    }

    public void advanceAfterBatch(ProcessorStatus status, List<?> response, Function<Object, String> dateExtractor) {
        if (status == null || response == null || response.isEmpty()) {
            return;
        }

        long newCount = status.getCount() + response.size();
        status.setCount(newCount);
        status.setPage(status.getPage() + 1);

        String lastDateModified = dateExtractor.apply(response.getLast());
        if (lastDateModified != null) {
            status.setLastUpdatedDate(STRING_DATE_TO_INSTANT_FUNCTION.apply(lastDateModified));
        }
    }
}
