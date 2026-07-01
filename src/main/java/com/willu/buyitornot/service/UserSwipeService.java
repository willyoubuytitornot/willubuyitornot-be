package com.willu.buyitornot.service;

import com.willu.buyitornot.web.dto.request.VoteRequest;
import com.willu.buyitornot.web.dto.response.PreferenceReportResponse;
import com.willu.buyitornot.web.dto.response.UserSwipeResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSwipeService {

    public UserSwipeResponse getResult(String userId, String swipeId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public UserSwipeResponse vote(String userId, String swipeId, List<VoteRequest> requests) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreferenceReportResponse getReport(String userId, String swipeId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
