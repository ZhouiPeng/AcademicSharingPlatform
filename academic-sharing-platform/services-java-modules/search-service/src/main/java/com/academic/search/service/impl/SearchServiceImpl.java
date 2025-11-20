package com.academic.search.service.impl;

import org.springframework.stereotype.Service;

import com.academic.search.service.SearchService;

@Service
public class SearchServiceImpl implements SearchService {

    @Override
    public String search(String q) {
        return "results for: " + q;
    }
}
