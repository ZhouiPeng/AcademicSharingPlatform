package com.academic.system.service.impl;

import com.academic.system.service.SystemService;
import org.springframework.stereotype.Service;

@Service
public class SystemServiceImpl implements SystemService {

    @Override
    public void createCategory(String body) {
        System.out.println("createCategory stub: " + body);
    }
}
