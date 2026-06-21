package com.willu.buyitornot.service;

import com.willu.buyitornot.web.dto.request.VoteRequest;
import com.willu.buyitornot.web.dto.response.PreferenceReportResponse;
import com.willu.buyitornot.web.dto.response.UserSwipeResponse;
import org.springframework.stereotype.Service;

@Service
public class UserSwipeService {

    public UserSwipeResponse getResult(String userId, String swipeId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public UserSwipeResponse vote(String userId, String swipeId, VoteRequest request) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public UserSwipeResponse cancelVote(String userId, String swipeId, String gameId) {
        throw new UnsupportedOperationException("Not implemented");
    }

    public PreferenceReportResponse getReport(String userId, String swipeId) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
