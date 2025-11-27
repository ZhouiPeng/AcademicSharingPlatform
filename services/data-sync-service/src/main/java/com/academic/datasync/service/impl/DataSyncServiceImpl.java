package com.academic.datasync.service.impl;

import com.academic.datasync.service.DataSyncService;
import org.springframework.stereotype.Service;

@Service
public class DataSyncServiceImpl implements DataSyncService {


    @Override
    public void pullFromPublicDb() {
        System.out.println("pullFromPublicDb stub");
    }
}
